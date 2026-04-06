package com.multitenant.saas.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.Map;

public class TenantDTO {
    private String id;
    @NotBlank(message = "Tenant name is required")
    private String name;
    @NotBlank(message = "Subscription plan is required")
    private String subscriptionPlan;
    private boolean active;
    private Map<String, Object> config;
    private LocalDateTime createdAt;

    public TenantDTO() {
    }

    public TenantDTO(String id, String name, String subscriptionPlan, boolean active, Map<String, Object> config, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.subscriptionPlan = subscriptionPlan;
        this.active = active;
        this.config = config;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(String subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static TenantDTOBuilder builder() {
        return new TenantDTOBuilder();
    }

    public static class TenantDTOBuilder {
        private String id, name, subscriptionPlan;
        private boolean active;
        private Map<String, Object> config;
        private LocalDateTime createdAt;

        public TenantDTOBuilder id(String id) {
            this.id = id;
            return this;
        }

        public TenantDTOBuilder name(String name) {
            this.name = name;
            return this;
        }

        public TenantDTOBuilder subscriptionPlan(String sp) {
            this.subscriptionPlan = sp;
            return this;
        }

        public TenantDTOBuilder active(boolean active) {
            this.active = active;
            return this;
        }

        public TenantDTOBuilder config(Map<String, Object> config) {
            this.config = config;
            return this;
        }

        public TenantDTOBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TenantDTO build() {
            return new TenantDTO(id, name, subscriptionPlan, active, config, createdAt);
        }
    }
}
