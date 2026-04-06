package com.multitenant.saas.service;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.dto.ProductDTO;
import com.multitenant.saas.exception.ResourceNotFoundException;
import com.multitenant.saas.model.Product;
import com.multitenant.saas.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
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

        Product product = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

        if (!product.isActive()) {
            throw new IllegalStateException("Product is not active and cannot be sold");
        }

        if (product.getProductCount() < quantity) {
            throw new IllegalStateException("Insufficient stock. Available: " + product.getProductCount() + ", Requested: " + quantity);
        }

        int newCount = product.getProductCount() - quantity;
        product.setProductCount(newCount);

        // Auto-deactivate if product count reaches 0
        if (newCount <= 0) {
            product.setActive(false);
            log.info("Product {} auto-deactivated: stock reached 0", id);
        }

        product = productRepository.save(product);
        log.info("Product sold: {} unit(s) of {}, remaining: {}", quantity, id, newCount);

        return toDTO(product);
    }

    public void deleteProduct(String id) {
        String tenantId = TenantContext.getTenantId();
        log.info("Deleting product: {} for tenant: {}", id, tenantId);

        Product product = productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));

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


