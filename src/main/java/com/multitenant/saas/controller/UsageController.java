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
@Tag(name = "Usage Tracking", description = "APIs for viewing API usage (tracked automatically by the system)")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    /**
     * Read-only — tenants can view their automatically tracked usage.
     * API calls are counted by UsageTrackingFilter; storage is counted on file uploads.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Get usage data for current tenant (auto-tracked by system)")
    public ResponseEntity<ApiResponse<List<UsageDTO>>> getUsage() {
        List<UsageDTO> usages = usageService.getUsageByTenant();
        return ResponseEntity.ok(ApiResponse.success(usages));
    }

    /**
     * Internal admin endpoint — for manually adjusting usage if needed (e.g., corrections).
     * NOT intended for regular tenant use. Usage is normally tracked automatically.
     */
    @PostMapping("/admin/adjust")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Manually adjust usage for any tenant (admin only, for corrections)")
    public ResponseEntity<ApiResponse<UsageDTO>> adjustUsage(
            @RequestParam String tenantId,
            @RequestBody UsageDTO usageDTO) {
        UsageDTO updated = usageService.updateUsageInternal(
                tenantId,
                usageDTO.getApiCalls(),
                usageDTO.getStorageUsed(),
                usageDTO.getMonth()
        );
        return ResponseEntity.ok(ApiResponse.success("Usage adjusted successfully", updated));
    }
}


