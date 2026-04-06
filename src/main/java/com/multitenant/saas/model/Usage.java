package com.multitenant.saas.model;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "usages")
@CompoundIndex(name = "tenant_month_idx", def = "{'tenantId': 1, 'month': 1}", unique = true)
public class Usage extends BaseEntity {

    private long apiCalls;
    private double storageUsed;
    private String month;

    public Usage() {}

    public Usage(long apiCalls, double storageUsed, String month) {
        this.apiCalls = apiCalls;
        this.storageUsed = storageUsed;
        this.month = month;
    }

    public long getApiCalls() { return apiCalls; }
    public void setApiCalls(long apiCalls) { this.apiCalls = apiCalls; }

    public double getStorageUsed() { return storageUsed; }
    public void setStorageUsed(double storageUsed) { this.storageUsed = storageUsed; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public static UsageBuilder builder() { return new UsageBuilder(); }

    public static class UsageBuilder {
        private long apiCalls;
        private double storageUsed;
        private String month;

        public UsageBuilder apiCalls(long apiCalls) { this.apiCalls = apiCalls; return this; }
        public UsageBuilder storageUsed(double storageUsed) { this.storageUsed = storageUsed; return this; }
        public UsageBuilder month(String month) { this.month = month; return this; }

        public Usage build() {
            return new Usage(apiCalls, storageUsed, month);
        }
    }
}
