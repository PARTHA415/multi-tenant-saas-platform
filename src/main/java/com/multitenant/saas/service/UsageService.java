package com.multitenant.saas.service;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.dto.UsageDTO;
import com.multitenant.saas.model.Invoice;
import com.multitenant.saas.model.Tenant;
import com.multitenant.saas.model.Usage;
import com.multitenant.saas.repository.InvoiceRepository;
import com.multitenant.saas.repository.TenantRepository;
import com.multitenant.saas.repository.UsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UsageService {

    private static final Logger log = LoggerFactory.getLogger(UsageService.class);
    private final UsageRepository usageRepository;
    private final InvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;
    private final CloudWatchMetricsService cloudWatchMetricsService;
    private final LambdaInvoiceService lambdaInvoiceService;

    @Value("${app.billing.cost-per-api-call:0.01}")
    private double costPerApiCall;

    @Value("${app.billing.cost-per-storage-unit:0.001}")
    private double costPerStorageUnit;

    public UsageService(UsageRepository usageRepository, InvoiceRepository invoiceRepository,
                        TenantRepository tenantRepository, CloudWatchMetricsService cloudWatchMetricsService,
                        LambdaInvoiceService lambdaInvoiceService) {
        this.usageRepository = usageRepository;
        this.invoiceRepository = invoiceRepository;
        this.tenantRepository = tenantRepository;
        this.cloudWatchMetricsService = cloudWatchMetricsService;
        this.lambdaInvoiceService = lambdaInvoiceService;
    }

    /**
     * Internal method — called by system services only (e.g., S3 upload), NOT exposed to tenants.
     * Tenants cannot self-report usage; all metrics are tracked automatically.
     */
    public UsageDTO updateUsageInternal(String tenantId, long apiCallsIncrement, double storageIncrement, String month) {
        if (month == null) {
            month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }

        log.info("System updating usage for tenant: {} month: {}", tenantId, month);

        Usage usage = usageRepository.findByTenantIdAndMonth(tenantId, month)
                .orElse(Usage.builder()
                        .month(month)
                        .apiCalls(0)
                        .storageUsed(0)
                        .build());

        usage.setTenantId(tenantId);
        usage.setApiCalls(usage.getApiCalls() + apiCallsIncrement);
        usage.setStorageUsed(usage.getStorageUsed() + storageIncrement);

        usage = usageRepository.save(usage);
        log.info("Usage updated for tenant: {} month: {} — apiCalls: {}, storage: {}",
                tenantId, month, usage.getApiCalls(), usage.getStorageUsed());

        // AWS CloudWatch: record usage metrics
        cloudWatchMetricsService.recordUsageUpdate(tenantId, usage.getApiCalls(), usage.getStorageUsed());

        // AWS Lambda: generate invoice PDF (calculates billing, creates PDF, uploads to S3, returns S3 key)
        String subscriptionPlan = tenantRepository.findById(tenantId)
                .map(Tenant::getSubscriptionPlan)
                .orElse("FREE");

        Map<String, Object> pdfResult = lambdaInvoiceService.generateInvoicePdf(
                tenantId, subscriptionPlan, month,
                usage.getApiCalls(), usage.getStorageUsed());

        // Store invoice with S3 key from Lambda
        BigDecimal amount;
        String pdfS3Key = null;

        if (pdfResult.containsKey("pdfS3Key")) {
            pdfS3Key = pdfResult.get("pdfS3Key").toString();
            log.info("Lambda generated PDF for tenant {}: s3Key={}", tenantId, pdfS3Key);
        }

        if (pdfResult.containsKey("amount")) {
            amount = new BigDecimal(pdfResult.get("amount").toString())
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            amount = calculateLocally(usage);
        }

        Invoice invoice = invoiceRepository.findByTenantIdAndMonth(tenantId, month)
                .orElse(new Invoice());
        invoice.setTenantId(tenantId);
        invoice.setAmount(amount);
        invoice.setMonth(month);
        invoice.setStatus(pdfS3Key != null ? "GENERATED" : "PENDING");
        invoice.setGeneratedAt(LocalDateTime.now());
        invoice.setApiCalls(usage.getApiCalls());
        invoice.setStorageUsed(usage.getStorageUsed());
        invoice.setPdfS3Key(pdfS3Key);
        invoiceRepository.save(invoice);

        log.info("Invoice saved for tenant: {} month: {} amount: {} pdfS3Key: {}",
                tenantId, month, amount, pdfS3Key);

        return toDTO(usage);
    }

    public List<UsageDTO> getUsageByTenant() {
        String tenantId = TenantContext.getTenantId();
        return usageRepository.findAllByTenantId(tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Called automatically by UsageTrackingFilter after each successful API request.
     * Not exposed via any REST endpoint — completely server-side.
     */
    public void incrementApiCall(String tenantId) {
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Usage usage = usageRepository.findByTenantIdAndMonth(tenantId, month)
                .orElse(Usage.builder()
                        .month(month)
                        .apiCalls(0)
                        .storageUsed(0)
                        .build());
        usage.setTenantId(tenantId);
        usage.setApiCalls(usage.getApiCalls() + 1);
        usageRepository.save(usage);
    }

    /**
     * Called automatically when tenant uploads a file (S3 or any storage).
     * Tracks storage in MB. Not exposed via any REST endpoint.
     */
    public void incrementStorage(String tenantId, double storageMB) {
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Usage usage = usageRepository.findByTenantIdAndMonth(tenantId, month)
                .orElse(Usage.builder()
                        .month(month)
                        .apiCalls(0)
                        .storageUsed(0)
                        .build());
        usage.setTenantId(tenantId);
        usage.setStorageUsed(usage.getStorageUsed() + storageMB);
        usageRepository.save(usage);
        log.info("Storage tracked for tenant: {} — +{} MB (total this month: {} MB)", tenantId, storageMB, usage.getStorageUsed());
    }

    private UsageDTO toDTO(Usage usage) {
        return UsageDTO.builder()
                .tenantId(usage.getTenantId())
                .apiCalls(usage.getApiCalls())
                .storageUsed(usage.getStorageUsed())
                .month(usage.getMonth())
                .build();
    }

    /**
     * Local billing calculation fallback (when Lambda is not available or returns no amount).
     */
    private BigDecimal calculateLocally(Usage usage) {
        double cost = usage.getApiCalls() * costPerApiCall + usage.getStorageUsed() * costPerStorageUnit;
        return BigDecimal.valueOf(cost).setScale(2, RoundingMode.HALF_UP);
    }
}



