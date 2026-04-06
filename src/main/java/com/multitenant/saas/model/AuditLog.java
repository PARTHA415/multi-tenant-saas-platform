package com.multitenant.saas.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;
    private String tenantId;
    private String userId;
    private String action;
    private String entity;
    private String entityId;
    private String details;

    @CreatedDate
    private LocalDateTime timestamp;

    public AuditLog() {}

    public AuditLog(String id, String tenantId, String userId, String action, String entity, String entityId, String details, LocalDateTime timestamp) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.action = action;
        this.entity = entity;
        this.entityId = entityId;
        this.details = details;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public static AuditLogBuilder builder() { return new AuditLogBuilder(); }

    public static class AuditLogBuilder {
        private String id;
        private String tenantId;
        private String userId;
        private String action;
        private String entity;
        private String entityId;
        private String details;
        private LocalDateTime timestamp;

        public AuditLogBuilder id(String id) { this.id = id; return this; }
        public AuditLogBuilder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public AuditLogBuilder userId(String userId) { this.userId = userId; return this; }
        public AuditLogBuilder action(String action) { this.action = action; return this; }
        public AuditLogBuilder entity(String entity) { this.entity = entity; return this; }
        public AuditLogBuilder entityId(String entityId) { this.entityId = entityId; return this; }
        public AuditLogBuilder details(String details) { this.details = details; return this; }
        public AuditLogBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public AuditLog build() {
            return new AuditLog(id, tenantId, userId, action, entity, entityId, details, timestamp);
        }
    }
}
