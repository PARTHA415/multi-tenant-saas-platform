package com.multitenant.saas.controller;

import com.multitenant.saas.dto.ApiResponse;
import com.multitenant.saas.dto.TenantDTO;
import com.multitenant.saas.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Tenant Management", description = "APIs for managing tenants — SUPER_ADMIN only")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @Operation(summary = "Create a new tenant")
    public ResponseEntity<ApiResponse<TenantDTO>> createTenant(@Valid @RequestBody TenantDTO tenantDTO) {
        TenantDTO created = tenantService.createTenant(tenantDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tenant created successfully", created));
    }

    @GetMapping
    @Operation(summary = "Get all tenants")
    public ResponseEntity<ApiResponse<List<TenantDTO>>> getAllTenants() {
        List<TenantDTO> tenants = tenantService.getAllTenants();
        return ResponseEntity.ok(ApiResponse.success(tenants));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tenant by ID")
    public ResponseEntity<ApiResponse<TenantDTO>> getTenantById(@PathVariable String id) {
        TenantDTO tenant = tenantService.getTenantById(id);
        return ResponseEntity.ok(ApiResponse.success(tenant));
    }
}
