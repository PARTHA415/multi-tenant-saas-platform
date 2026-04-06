package com.multitenant.saas.service;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.dto.InvoiceDTO;
import com.multitenant.saas.model.Invoice;
import com.multitenant.saas.model.Usage;
import com.multitenant.saas.repository.InvoiceRepository;
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
import java.util.stream.Collectors;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private final InvoiceRepository invoiceRepository;
    private final UsageRepository usageRepository;

    public BillingService(InvoiceRepository invoiceRepository, UsageRepository usageRepository) {
        this.invoiceRepository = invoiceRepository;
        this.usageRepository = usageRepository;
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
        // cost = apiCalls * 0.01 + storageUsed * 0.001
        double cost = usage.getApiCalls() * costPerApiCall + usage.getStorageUsed() * costPerStorageUnit;
        BigDecimal amount = BigDecimal.valueOf(cost).setScale(2, RoundingMode.HALF_UP);

        // Check if invoice already exists
        Invoice invoice = invoiceRepository.findByTenantIdAndMonth(usage.getTenantId(), month)
                .orElse(new Invoice());

        invoice.setTenantId(usage.getTenantId());
        invoice.setAmount(amount);
        invoice.setMonth(month);
        invoice.setStatus("PENDING");
        invoice.setGeneratedAt(LocalDateTime.now());
        invoice.setApiCalls(usage.getApiCalls());
        invoice.setStorageUsed(usage.getStorageUsed());

        invoice = invoiceRepository.save(invoice);
        log.info("Invoice generated for tenant: {} month: {} amount: {}", usage.getTenantId(), month, amount);

        return toDTO(invoice);
    }

    private InvoiceDTO toDTO(Invoice invoice) {
        return InvoiceDTO.builder()
                .id(invoice.getId())
                .tenantId(invoice.getTenantId())
                .amount(invoice.getAmount())
                .month(invoice.getMonth())
                .status(invoice.getStatus())
                .generatedAt(invoice.getGeneratedAt())
                .apiCalls(invoice.getApiCalls())
                .storageUsed(invoice.getStorageUsed())
                .build();
    }
}



