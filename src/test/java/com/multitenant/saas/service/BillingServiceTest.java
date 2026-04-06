package com.multitenant.saas.service;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.dto.InvoiceDTO;
import com.multitenant.saas.model.Invoice;
import com.multitenant.saas.model.Usage;
import com.multitenant.saas.repository.InvoiceRepository;
import com.multitenant.saas.repository.UsageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private UsageRepository usageRepository;

    @InjectMocks
    private BillingService billingService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-1");
        ReflectionTestUtils.setField(billingService, "costPerApiCall", 0.01);
        ReflectionTestUtils.setField(billingService, "costPerStorageUnit", 0.001);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should generate invoice from usage data")
    void shouldGenerateInvoice() {
        Usage usage = Usage.builder()
                .apiCalls(1000)
                .storageUsed(500.0)
                .month("2026-03")
                .build();
        usage.setTenantId("tenant-1");

        when(usageRepository.findByTenantIdAndMonth("tenant-1", "2026-03"))
                .thenReturn(Optional.of(usage));
        when(invoiceRepository.findByTenantIdAndMonth("tenant-1", "2026-03"))
                .thenReturn(Optional.empty());

        Invoice savedInvoice = new Invoice();
        savedInvoice.setId("invoice-1");
        savedInvoice.setTenantId("tenant-1");
        savedInvoice.setAmount(BigDecimal.valueOf(10.50));
        savedInvoice.setMonth("2026-03");
        savedInvoice.setStatus("PENDING");
        savedInvoice.setGeneratedAt(LocalDateTime.now());
        savedInvoice.setApiCalls(1000);
        savedInvoice.setStorageUsed(500.0);

        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);

        InvoiceDTO result = billingService.generateInvoice("2026-03");

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals("2026-03", result.getMonth());
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    @DisplayName("Should calculate cost correctly: apiCalls * 0.01 + storageUsed * 0.001")
    void shouldCalculateCostCorrectly() {
        // 1000 * 0.01 = 10.00 + 500 * 0.001 = 0.50 = total 10.50
        Usage usage = Usage.builder()
                .apiCalls(1000)
                .storageUsed(500.0)
                .month("2026-03")
                .build();
        usage.setTenantId("tenant-1");

        when(usageRepository.findByTenantIdAndMonth("tenant-1", "2026-03"))
                .thenReturn(Optional.of(usage));
        when(invoiceRepository.findByTenantIdAndMonth("tenant-1", "2026-03"))
                .thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId("invoice-1");
            return inv;
        });

        InvoiceDTO result = billingService.generateInvoice("2026-03");

        assertNotNull(result);
        assertEquals(new BigDecimal("10.50"), result.getAmount());
    }

    @Test
    @DisplayName("Should return all invoices for tenant")
    void shouldGetAllInvoices() {
        Invoice invoice = new Invoice();
        invoice.setId("invoice-1");
        invoice.setTenantId("tenant-1");
        invoice.setAmount(BigDecimal.valueOf(10.50));
        invoice.setMonth("2026-03");
        invoice.setStatus("PENDING");
        invoice.setGeneratedAt(LocalDateTime.now());

        when(invoiceRepository.findAllByTenantId("tenant-1")).thenReturn(List.of(invoice));

        List<InvoiceDTO> results = billingService.getInvoices();

        assertEquals(1, results.size());
        assertEquals("invoice-1", results.get(0).getId());
    }

    @Test
    @DisplayName("Should generate invoice with zero usage")
    void shouldGenerateInvoiceWithZeroUsage() {
        when(usageRepository.findByTenantIdAndMonth("tenant-1", "2026-03"))
                .thenReturn(Optional.empty());
        when(invoiceRepository.findByTenantIdAndMonth("tenant-1", "2026-03"))
                .thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice inv = invocation.getArgument(0);
            inv.setId("invoice-2");
            return inv;
        });

        InvoiceDTO result = billingService.generateInvoice("2026-03");

        assertNotNull(result);
        assertEquals(new BigDecimal("0.00"), result.getAmount());
    }
}

