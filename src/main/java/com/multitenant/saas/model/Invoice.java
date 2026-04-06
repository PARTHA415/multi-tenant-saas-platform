package com.multitenant.saas.model;

import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "invoices")
public class Invoice extends BaseEntity {

    private BigDecimal amount;
    private String month;
    private String status;
    private LocalDateTime generatedAt;
    private long apiCalls;
    private double storageUsed;

    public Invoice() {}

    public Invoice(BigDecimal amount, String month, String status, LocalDateTime generatedAt, long apiCalls, double storageUsed) {
        this.amount = amount;
        this.month = month;
        this.status = status;
        this.generatedAt = generatedAt;
        this.apiCalls = apiCalls;
        this.storageUsed = storageUsed;
    }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public long getApiCalls() { return apiCalls; }
    public void setApiCalls(long apiCalls) { this.apiCalls = apiCalls; }

    public double getStorageUsed() { return storageUsed; }
    public void setStorageUsed(double storageUsed) { this.storageUsed = storageUsed; }
}
