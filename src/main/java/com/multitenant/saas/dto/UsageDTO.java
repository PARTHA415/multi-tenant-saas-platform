package com.multitenant.saas.dto;

public class UsageDTO {
    private String tenantId;
    private long apiCalls;
    private double storageUsed;
    private String month;

    public UsageDTO() {}
    public UsageDTO(String tenantId, long apiCalls, double storageUsed, String month) {
        this.tenantId = tenantId; this.apiCalls = apiCalls; this.storageUsed = storageUsed; this.month = month;
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public long getApiCalls() { return apiCalls; }
    public void setApiCalls(long apiCalls) { this.apiCalls = apiCalls; }
    public double getStorageUsed() { return storageUsed; }
    public void setStorageUsed(double storageUsed) { this.storageUsed = storageUsed; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public static UsageDTOBuilder builder() { return new UsageDTOBuilder(); }
    public static class UsageDTOBuilder {
        private String tenantId, month;
        private long apiCalls;
        private double storageUsed;
        public UsageDTOBuilder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public UsageDTOBuilder apiCalls(long apiCalls) { this.apiCalls = apiCalls; return this; }
        public UsageDTOBuilder storageUsed(double storageUsed) { this.storageUsed = storageUsed; return this; }
        public UsageDTOBuilder month(String month) { this.month = month; return this; }
        public UsageDTO build() { return new UsageDTO(tenantId, apiCalls, storageUsed, month); }
    }
}
