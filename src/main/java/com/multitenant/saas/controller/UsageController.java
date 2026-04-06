package com.multitenant.saas.controller;

import com.multitenant.saas.dto.ApiResponse;
import com.multitenant.saas.dto.UsageDTO;
import com.multitenant.saas.service.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usage")
@Tag(name = "Usage Tracking", description = "APIs for tracking API usage")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    @PostMapping("/update")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Update usage metrics for current tenant")
    public ResponseEntity<ApiResponse<UsageDTO>> updateUsage(@RequestBody UsageDTO usageDTO) {
        UsageDTO updated = usageService.updateUsage(usageDTO);
        return ResponseEntity.ok(ApiResponse.success("Usage updated successfully", updated));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Get usage data for current tenant")
    public ResponseEntity<ApiResponse<List<UsageDTO>>> getUsage() {
        List<UsageDTO> usages = usageService.getUsageByTenant();
        return ResponseEntity.ok(ApiResponse.success(usages));
    }
}


