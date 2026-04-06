package com.multitenant.saas.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Map;

public class ProductDTO {
    private String id;
    @NotBlank(message = "Product name is required")
    private String name;
    private String description;
    private BigDecimal price;
    private boolean active;
    private int productCount;
    private Map<String, Object> attributes;

    public ProductDTO() {}
    public ProductDTO(String id, String name, String description, BigDecimal price, boolean active, int productCount, Map<String, Object> attributes) {
        this.id = id; this.name = name; this.description = description; this.price = price; this.active = active; this.productCount = productCount; this.attributes = attributes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getProductCount() { return productCount; }
    public void setProductCount(int productCount) { this.productCount = productCount; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }

    public static ProductDTOBuilder builder() { return new ProductDTOBuilder(); }
    public static class ProductDTOBuilder {
        private String id, name, description;
        private BigDecimal price;
        private boolean active;
        private int productCount;
        private Map<String, Object> attributes;
        public ProductDTOBuilder id(String id) { this.id = id; return this; }
        public ProductDTOBuilder name(String name) { this.name = name; return this; }
        public ProductDTOBuilder description(String description) { this.description = description; return this; }
        public ProductDTOBuilder price(BigDecimal price) { this.price = price; return this; }
        public ProductDTOBuilder active(boolean active) { this.active = active; return this; }
        public ProductDTOBuilder productCount(int productCount) { this.productCount = productCount; return this; }
        public ProductDTOBuilder attributes(Map<String, Object> attributes) { this.attributes = attributes; return this; }
        public ProductDTO build() { return new ProductDTO(id, name, description, price, active, productCount, attributes); }
    }
}
