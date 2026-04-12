package com.multitenant.saas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvoiceDTO {
    private String id;
    private String tenantId;
    private BigDecimal amount;
    private String month;
    private String status;
    private LocalDateTime generatedAt;
    private long apiCalls;
    private double storageUsed;
    private String pdfS3Key;
    private String pdfDownloadUrl;

    public InvoiceDTO() {}
    public InvoiceDTO(String id, String tenantId, BigDecimal amount, String month, String status, LocalDateTime generatedAt, long apiCalls, double storageUsed, String pdfS3Key, String pdfDownloadUrl) {
        this.id = id; this.tenantId = tenantId; this.amount = amount; this.month = month; this.status = status; this.generatedAt = generatedAt; this.apiCalls = apiCalls; this.storageUsed = storageUsed; this.pdfS3Key = pdfS3Key; this.pdfDownloadUrl = pdfDownloadUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
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
    public String getPdfS3Key() { return pdfS3Key; }
    public void setPdfS3Key(String pdfS3Key) { this.pdfS3Key = pdfS3Key; }
    public String getPdfDownloadUrl() { return pdfDownloadUrl; }
    public void setPdfDownloadUrl(String pdfDownloadUrl) { this.pdfDownloadUrl = pdfDownloadUrl; }

    public static InvoiceDTOBuilder builder() { return new InvoiceDTOBuilder(); }
    public static class InvoiceDTOBuilder {
        private String id, tenantId, month, status, pdfS3Key, pdfDownloadUrl;
        private BigDecimal amount;
        private LocalDateTime generatedAt;
        private long apiCalls;
        private double storageUsed;
        public InvoiceDTOBuilder id(String id) { this.id = id; return this; }
        public InvoiceDTOBuilder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public InvoiceDTOBuilder amount(BigDecimal amount) { this.amount = amount; return this; }
        public InvoiceDTOBuilder month(String month) { this.month = month; return this; }
        public InvoiceDTOBuilder status(String status) { this.status = status; return this; }
        public InvoiceDTOBuilder generatedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; return this; }
        public InvoiceDTOBuilder apiCalls(long apiCalls) { this.apiCalls = apiCalls; return this; }
        public InvoiceDTOBuilder storageUsed(double storageUsed) { this.storageUsed = storageUsed; return this; }
        public InvoiceDTOBuilder pdfS3Key(String pdfS3Key) { this.pdfS3Key = pdfS3Key; return this; }
        public InvoiceDTOBuilder pdfDownloadUrl(String pdfDownloadUrl) { this.pdfDownloadUrl = pdfDownloadUrl; return this; }
        public InvoiceDTO build() { return new InvoiceDTO(id, tenantId, amount, month, status, generatedAt, apiCalls, storageUsed, pdfS3Key, pdfDownloadUrl); }
    }
}
