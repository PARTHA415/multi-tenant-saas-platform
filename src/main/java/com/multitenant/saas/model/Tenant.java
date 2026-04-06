package com.multitenant.saas.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "tenants")
public class Tenant {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String subscriptionPlan;
    private boolean active;
    private Map<String, Object> config;

    @CreatedDate
    private LocalDateTime createdAt;

    public Tenant() {}

    public Tenant(String id, String name, String subscriptionPlan, boolean active, Map<String, Object> config, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.subscriptionPlan = subscriptionPlan;
        this.active = active;
        this.config = config;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static TenantBuilder builder() { return new TenantBuilder(); }

    public static class TenantBuilder {
        private String id;
        private String name;
        private String subscriptionPlan;
        private boolean active;
        private Map<String, Object> config;
        private LocalDateTime createdAt;

        public TenantBuilder id(String id) { this.id = id; return this; }
        public TenantBuilder name(String name) { this.name = name; return this; }
        public TenantBuilder subscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; return this; }
        public TenantBuilder active(boolean active) { this.active = active; return this; }
        public TenantBuilder config(Map<String, Object> config) { this.config = config; return this; }
        public TenantBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Tenant build() {
            return new Tenant(id, name, subscriptionPlan, active, config, createdAt);
        }
    }
}
