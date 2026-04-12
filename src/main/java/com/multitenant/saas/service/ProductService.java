package com.multitenant.saas.service;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.dto.ProductDTO;
import com.multitenant.saas.exception.ResourceNotFoundException;
import com.multitenant.saas.model.Product;
import com.multitenant.saas.repository.ProductRepository;
import com.mongodb.client.result.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;
    private final CloudWatchMetricsService cloudWatchMetricsService;
    private final S3StorageService s3StorageService;

    public ProductService(ProductRepository productRepository, MongoTemplate mongoTemplate,
                          CloudWatchMetricsService cloudWatchMetricsService, S3StorageService s3StorageService) {
        this.productRepository = productRepository;
        this.mongoTemplate = mongoTemplate;
        this.cloudWatchMetricsService = cloudWatchMetricsService;
        this.s3StorageService = s3StorageService;
    }

    public ProductDTO createProduct(ProductDTO dto) {
        String tenantId = TenantContext.getTenantId();
        log.info("Creating product: {} for tenant: {}", dto.getName(), tenantId);

        Product product = Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .active(true)
                .productCount(dto.getProductCount())
                .attributes(dto.getAttributes())
                .build();
        product.setTenantId(tenantId);

        product = productRepository.save(product);
        log.info("Product created with ID: {} for tenant: {}", product.getId(), tenantId);

        return toDTO(product);
    }

    public List<ProductDTO> getAllProducts() {
        String tenantId = TenantContext.getTenantId();
        log.debug("Fetching all products for tenant: {}", tenantId);
        return productRepository.findAllByTenantId(tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProductDTO getProductById(String id) {
        String tenantId = TenantContext.getTenantId();
        Product product = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return toDTO(product);
    }

    public ProductDTO updateProduct(String id, ProductDTO dto) {
        String tenantId = TenantContext.getTenantId();
        log.info("Updating product: {} for tenant: {}", id, tenantId);

        Product product = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice());
        if (dto.getAttributes() != null) product.setAttributes(dto.getAttributes());
        if (dto.getProductCount() > 0) product.setProductCount(dto.getProductCount());
        product.setActive(dto.isActive());

        // Auto-deactivate if product count is 0
        if (product.getProductCount() <= 0) {
            product.setActive(false);
        }

        product = productRepository.save(product);
        log.info("Product updated: {} for tenant: {}", id, tenantId);

        return toDTO(product);
    }

    public ProductDTO sellProduct(String id, int quantity) {
        String tenantId = TenantContext.getTenantId();
        log.info("Selling {} unit(s) of product: {} for tenant: {}", quantity, id, tenantId);

        // Atomic decrement: only succeeds if product exists, is active, and has enough stock
        Query query = new Query(Criteria.where("_id").is(id)
                .and("tenantId").is(tenantId)
                .and("active").is(true)
                .and("productCount").gte(quantity));

        Update update = new Update().inc("productCount", -quantity);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Product.class);

        if (result.getModifiedCount() == 0) {
            // Determine the specific failure reason for a clear error message
            Product product = productRepository.findByIdAndTenantId(id, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

            if (!product.isActive()) {
                throw new IllegalStateException("Product is not active and cannot be sold");
            }
            throw new IllegalStateException("Insufficient stock. Available: " + product.getProductCount() + ", Requested: " + quantity);
        }

        // Re-read the updated product to check if auto-deactivation is needed
        Product product = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        // Auto-deactivate if product count reaches 0
        if (product.getProductCount() <= 0) {
            product.setActive(false);
            product = productRepository.save(product);
            log.info("Product {} auto-deactivated: stock reached 0", id);

            // AWS CloudWatch: record out-of-stock event
            cloudWatchMetricsService.recordOutOfStockEvent(tenantId, id);
        }

        // AWS CloudWatch: record sell event
        cloudWatchMetricsService.recordSellEvent(tenantId, id, quantity);

        log.info("Product sold atomically: {} unit(s) of {}, remaining: {}", quantity, id, product.getProductCount());

        return toDTO(product);
    }

    public void deleteProduct(String id) {
        String tenantId = TenantContext.getTenantId();
        log.info("Deleting product: {} for tenant: {}", id, tenantId);

        Product product = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        // AWS S3: delete all product assets (images, files)
        try {
            s3StorageService.deleteProductAssets(tenantId, id);
            log.info("S3 assets deleted for product: {}", id);
        } catch (Exception e) {
            log.warn("Failed to delete S3 assets for product {}: {}", id, e.getMessage());
        }

        productRepository.delete(product);
        log.info("Product deleted: {} for tenant: {}", id, tenantId);
    }

    private ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .active(product.isActive())
                .productCount(product.getProductCount())
                .attributes(product.getAttributes())
                .build();
    }
}


