package com.multitenant.saas.model;

import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "products")
public class Product extends BaseEntity {

    private String name;
    private String description;
    private BigDecimal price;
    private boolean active;
    private int productCount;
    private Map<String, Object> attributes = new HashMap<>();

    public Product() {}

    public Product(String name, String description, BigDecimal price, boolean active, int productCount, Map<String, Object> attributes) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.active = active;
        this.productCount = productCount;
        this.attributes = attributes != null ? attributes : new HashMap<>();
    }

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

    public static ProductBuilder builder() { return new ProductBuilder(); }

    public static class ProductBuilder {
        private String name;
        private String description;
        private BigDecimal price;
        private boolean active;
        private int productCount;
        private Map<String, Object> attributes = new HashMap<>();

        public ProductBuilder name(String name) { this.name = name; return this; }
        public ProductBuilder description(String description) { this.description = description; return this; }
        public ProductBuilder price(BigDecimal price) { this.price = price; return this; }
        public ProductBuilder active(boolean active) { this.active = active; return this; }
        public ProductBuilder productCount(int productCount) { this.productCount = productCount; return this; }
        public ProductBuilder attributes(Map<String, Object> attributes) { this.attributes = attributes; return this; }

        public Product build() {
            return new Product(name, description, price, active, productCount, attributes);
        }
    }
}
