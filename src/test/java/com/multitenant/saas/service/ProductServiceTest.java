package com.multitenant.saas.service;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.dto.ProductDTO;
import com.multitenant.saas.exception.ResourceNotFoundException;
import com.multitenant.saas.model.Product;
import com.multitenant.saas.repository.ProductRepository;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private CloudWatchMetricsService cloudWatchMetricsService;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-1");

        sampleProduct = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(19.99))
                .active(true)
                .productCount(10)
                .attributes(Map.of("color", "red"))
                .build();
        sampleProduct.setId("product-1");
        sampleProduct.setTenantId("tenant-1");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should create a product")
    void shouldCreateProduct() {
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductDTO dto = ProductDTO.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(19.99))
                .attributes(Map.of("color", "red"))
                .build();

        ProductDTO result = productService.createProduct(dto);

        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        assertEquals(BigDecimal.valueOf(19.99), result.getPrice());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should get all products for current tenant")
    void shouldGetAllProducts() {
        when(productRepository.findAllByTenantId("tenant-1")).thenReturn(List.of(sampleProduct));

        List<ProductDTO> result = productService.getAllProducts();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Product", result.get(0).getName());
    }

    @Test
    @DisplayName("Should get product by ID and tenant")
    void shouldGetProductById() {
        when(productRepository.findByIdAndTenantId("product-1", "tenant-1"))
                .thenReturn(Optional.of(sampleProduct));

        ProductDTO result = productService.getProductById("product-1");

        assertNotNull(result);
        assertEquals("product-1", result.getId());
    }

    @Test
    @DisplayName("Should throw when product not found")
    void shouldThrowWhenProductNotFound() {
        when(productRepository.findByIdAndTenantId("nonexistent", "tenant-1"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductById("nonexistent"));
    }

    @Test
    @DisplayName("Should update product")
    void shouldUpdateProduct() {
        when(productRepository.findByIdAndTenantId("product-1", "tenant-1"))
                .thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductDTO updateDTO = ProductDTO.builder()
                .name("Updated Product")
                .price(BigDecimal.valueOf(29.99))
                .active(true)
                .build();

        ProductDTO result = productService.updateProduct("product-1", updateDTO);

        assertNotNull(result);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should delete product")
    void shouldDeleteProduct() {
        when(productRepository.findByIdAndTenantId("product-1", "tenant-1"))
                .thenReturn(Optional.of(sampleProduct));

        productService.deleteProduct("product-1");

        verify(productRepository).delete(sampleProduct);
    }

    @Test
    @DisplayName("Should not allow cross-tenant product access")
    void shouldNotAllowCrossTenantAccess() {
        // Product belongs to tenant-1 but we query as tenant-2
        TenantContext.setTenantId("tenant-2");

        when(productRepository.findByIdAndTenantId("product-1", "tenant-2"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.getProductById("product-1"));
    }

    @Test
    @DisplayName("Should sell product and decrease count (atomic)")
    void shouldSellProductAndDecreaseCount() {
        // Mock atomic update success
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Product.class)))
                .thenReturn(updateResult);

        // After atomic update, product is re-read with decremented count
        Product updatedProduct = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(19.99))
                .active(true)
                .productCount(7)
                .attributes(Map.of("color", "red"))
                .build();
        updatedProduct.setId("product-1");
        updatedProduct.setTenantId("tenant-1");

        when(productRepository.findByIdAndTenantId("product-1", "tenant-1"))
                .thenReturn(Optional.of(updatedProduct));

        ProductDTO result = productService.sellProduct("product-1", 3);

        assertNotNull(result);
        assertEquals(7, result.getProductCount());
        assertTrue(result.isActive());
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(Product.class));
    }

    @Test
    @DisplayName("Should auto-deactivate product when count reaches 0 (atomic)")
    void shouldAutoDeactivateWhenCountReachesZero() {
        // Mock atomic update success
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(1L);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Product.class)))
                .thenReturn(updateResult);

        // After atomic decrement, product has count 0
        Product updatedProduct = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(19.99))
                .active(true)
                .productCount(0)
                .attributes(Map.of("color", "red"))
                .build();
        updatedProduct.setId("product-1");
        updatedProduct.setTenantId("tenant-1");

        when(productRepository.findByIdAndTenantId("product-1", "tenant-1"))
                .thenReturn(Optional.of(updatedProduct));

        // After auto-deactivation, save is called
        Product deactivatedProduct = Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(19.99))
                .active(false)
                .productCount(0)
                .attributes(Map.of("color", "red"))
                .build();
        deactivatedProduct.setId("product-1");
        deactivatedProduct.setTenantId("tenant-1");

        when(productRepository.save(any(Product.class))).thenReturn(deactivatedProduct);

        ProductDTO result = productService.sellProduct("product-1", 5);

        assertNotNull(result);
        assertEquals(0, result.getProductCount());
        assertFalse(result.isActive());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw when selling inactive product (atomic)")
    void shouldThrowWhenSellingInactiveProduct() {
        // Atomic update fails (product not active)
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(0L);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Product.class)))
                .thenReturn(updateResult);

        // When checking failure reason, product is found but inactive
        sampleProduct.setActive(false);
        when(productRepository.findByIdAndTenantId("product-1", "tenant-1"))
                .thenReturn(Optional.of(sampleProduct));

        assertThrows(IllegalStateException.class,
                () -> productService.sellProduct("product-1", 1));
    }

    @Test
    @DisplayName("Should throw when selling more than available stock (atomic)")
    void shouldThrowWhenSellingMoreThanStock() {
        // Atomic update fails (insufficient stock)
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(0L);
        when(mongoTemplate.updateFirst(any(Query.class), any(Update.class), eq(Product.class)))
                .thenReturn(updateResult);

        // When checking failure reason, product has only 2 units
        sampleProduct.setProductCount(2);
        when(productRepository.findByIdAndTenantId("product-1", "tenant-1"))
                .thenReturn(Optional.of(sampleProduct));

        assertThrows(IllegalStateException.class,
                () -> productService.sellProduct("product-1", 5));
    }
}

