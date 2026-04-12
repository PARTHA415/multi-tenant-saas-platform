package com.multitenant.saas.service;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.dto.InvoiceDTO;
import com.multitenant.saas.model.Invoice;
import com.multitenant.saas.model.Tenant;
import com.multitenant.saas.model.Usage;
import com.multitenant.saas.repository.InvoiceRepository;
import com.multitenant.saas.repository.TenantRepository;
import com.multitenant.saas.repository.UsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private final InvoiceRepository invoiceRepository;
    private final UsageRepository usageRepository;
    private final TenantRepository tenantRepository;
    private final LambdaInvoiceService lambdaInvoiceService;
    private final S3StorageService s3StorageService;

    public BillingService(InvoiceRepository invoiceRepository, UsageRepository usageRepository,
                          TenantRepository tenantRepository, LambdaInvoiceService lambdaInvoiceService,
                          S3StorageService s3StorageService) {
        this.invoiceRepository = invoiceRepository;
        this.usageRepository = usageRepository;
        this.tenantRepository = tenantRepository;
        this.lambdaInvoiceService = lambdaInvoiceService;
        this.s3StorageService = s3StorageService;
    }

    @Value("${app.billing.cost-per-api-call:0.01}")
    private double costPerApiCall;

    @Value("${app.billing.cost-per-storage-unit:0.001}")
    private double costPerStorageUnit;

    public List<InvoiceDTO> getInvoices() {
        String tenantId = TenantContext.getTenantId();
        log.debug("Fetching invoices for tenant: {}", tenantId);
        return invoiceRepository.findAllByTenantId(tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public InvoiceDTO getInvoiceForMonth(String month) {
        String tenantId = TenantContext.getTenantId();
        Invoice invoice = invoiceRepository.findByTenantIdAndMonth(tenantId, month)
                .orElse(null);
        return invoice != null ? toDTO(invoice) : null;
    }

    /**
     * Scheduled monthly invoice generation — runs on 1st of every month at midnight.
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void generateMonthlyInvoices() {
        String previousMonth = LocalDate.now().minusMonths(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("Generating invoices for month: {}", previousMonth);

        List<Usage> allUsages = usageRepository.findAllByMonth(previousMonth);

        for (Usage usage : allUsages) {
            generateInvoiceForUsage(usage, previousMonth);
        }

        log.info("Generated {} invoices for month: {}", allUsages.size(), previousMonth);
    }

    /**
     * Manual invoice generation trigger for a specific tenant.
     */
    public InvoiceDTO generateInvoice(String month) {
        String tenantId = TenantContext.getTenantId();
        log.info("Manually generating invoice for tenant: {} month: {}", tenantId, month);

        Usage usage = usageRepository.findByTenantIdAndMonth(tenantId, month)
                .orElseGet(() -> {
                    Usage u = Usage.builder()
                            .month(month)
                            .apiCalls(0)
                            .storageUsed(0)
                            .build();
                    u.setTenantId(tenantId);
                    return u;
                });

        return generateInvoiceForUsage(usage, month);
    }

    private InvoiceDTO generateInvoiceForUsage(Usage usage, String month) {
        BigDecimal amount;
        String pdfS3Key = null;

        String subscriptionPlan = tenantRepository.findById(usage.getTenantId())
                .map(Tenant::getSubscriptionPlan)
                .orElse("FREE");

        // AWS Lambda: generate invoice PDF (calculates billing, creates PDF, uploads to S3, returns S3 key)
        Map<String, Object> pdfResult = lambdaInvoiceService.generateInvoicePdf(
                usage.getTenantId(), subscriptionPlan, month,
                usage.getApiCalls(), usage.getStorageUsed());

        if (pdfResult.containsKey("pdfS3Key")) {
            pdfS3Key = pdfResult.get("pdfS3Key").toString();
            log.info("Lambda generated PDF for tenant {}: s3Key={}", usage.getTenantId(), pdfS3Key);
        } else {
            log.warn("Lambda PDF generation returned no S3 key for tenant {}", usage.getTenantId());
        }

        if (pdfResult.containsKey("amount")) {
            amount = new BigDecimal(pdfResult.get("amount").toString())
                    .setScale(2, RoundingMode.HALF_UP);
            log.info("Lambda billing for tenant {}: amount={}", usage.getTenantId(), amount);
        } else {
            log.warn("Lambda returned no amount for tenant {}, falling back to local calc",
                    usage.getTenantId());
            amount = calculateLocally(usage);
        }

        // Check if invoice already exists
        Invoice invoice = invoiceRepository.findByTenantIdAndMonth(usage.getTenantId(), month)
                .orElse(new Invoice());

        invoice.setTenantId(usage.getTenantId());
        invoice.setAmount(amount);
        invoice.setMonth(month);
        invoice.setStatus(pdfS3Key != null ? "GENERATED" : "PENDING");
        invoice.setGeneratedAt(LocalDateTime.now());
        invoice.setApiCalls(usage.getApiCalls());
        invoice.setStorageUsed(usage.getStorageUsed());
        invoice.setPdfS3Key(pdfS3Key);

        invoice = invoiceRepository.save(invoice);
        log.info("Invoice generated for tenant: {} month: {} amount: {} pdfS3Key: {}",
                usage.getTenantId(), month, amount, pdfS3Key);

        return toDTO(invoice);
    }

    /**
     * Local billing calculation fallback (when Lambda is not available).
     * cost = apiCalls * costPerApiCall + storageUsed * costPerStorageUnit
     */
    private BigDecimal calculateLocally(Usage usage) {
        double cost = usage.getApiCalls() * costPerApiCall + usage.getStorageUsed() * costPerStorageUnit;
        return BigDecimal.valueOf(cost).setScale(2, RoundingMode.HALF_UP);
    }

    private InvoiceDTO toDTO(Invoice invoice) {
        String downloadUrl = null;

        // Generate pre-signed S3 URL if PDF exists
        if (invoice.getPdfS3Key() != null) {
            try {
                downloadUrl = s3StorageService.generatePresignedUrl(invoice.getPdfS3Key());
            } catch (Exception e) {
                log.warn("Failed to generate pre-signed URL for invoice {}: {}", invoice.getId(), e.getMessage());
            }
        }

        return InvoiceDTO.builder()
                .id(invoice.getId())
                .tenantId(invoice.getTenantId())
                .amount(invoice.getAmount())
                .month(invoice.getMonth())
                .status(invoice.getStatus())
                .generatedAt(invoice.getGeneratedAt())
                .apiCalls(invoice.getApiCalls())
                .storageUsed(invoice.getStorageUsed())
                .pdfS3Key(invoice.getPdfS3Key())
                .pdfDownloadUrl(downloadUrl)
                .build();
    }

    /**
     * Get a fresh pre-signed download URL for an existing invoice PDF.
     * Used when the previous URL has expired (URLs expire after 15 minutes).
     */
    public String getInvoiceDownloadUrl(String month) {
        String tenantId = TenantContext.getTenantId();
        Invoice invoice = invoiceRepository.findByTenantIdAndMonth(tenantId, month)
                .orElse(null);

        if (invoice == null || invoice.getPdfS3Key() == null) {
            return null;
        }

        return s3StorageService.generatePresignedUrl(invoice.getPdfS3Key());
    }
}



