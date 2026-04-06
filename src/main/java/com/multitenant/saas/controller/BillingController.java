package com.multitenant.saas.controller;

import com.multitenant.saas.dto.ApiResponse;
import com.multitenant.saas.dto.InvoiceDTO;
import com.multitenant.saas.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/billing")
@Tag(name = "Billing", description = "APIs for billing and invoices")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/invoice")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Get all invoices for current tenant")
    public ResponseEntity<ApiResponse<List<InvoiceDTO>>> getInvoices() {
        List<InvoiceDTO> invoices = billingService.getInvoices();
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    @GetMapping("/invoice/{month}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Get invoice for a specific month")
    public ResponseEntity<ApiResponse<InvoiceDTO>> getInvoiceForMonth(@PathVariable String month) {
        InvoiceDTO invoice = billingService.getInvoiceForMonth(month);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @PostMapping("/generate/{month}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Manually generate invoice for a specific month")
    public ResponseEntity<ApiResponse<InvoiceDTO>> generateInvoice(@PathVariable String month) {
        InvoiceDTO invoice = billingService.generateInvoice(month);
        return ResponseEntity.ok(ApiResponse.success("Invoice generated successfully", invoice));
    }
}


