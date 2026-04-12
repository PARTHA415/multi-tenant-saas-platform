package com.multitenant.saas.controller;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.dto.ApiResponse;
import com.multitenant.saas.service.S3StorageService;
import com.multitenant.saas.service.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/storage")
@Tag(name = "S3 Storage", description = "APIs for tenant-isolated file storage (AWS S3)")
public class S3Controller {

    private final S3StorageService s3StorageService;
    private final UsageService usageService;

    public S3Controller(S3StorageService s3StorageService, UsageService usageService) {
        this.s3StorageService = s3StorageService;
        this.usageService = usageService;
    }

    @PostMapping("/products/{productId}/image")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Upload product image to S3")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProductImage(
            @PathVariable String productId,
            @RequestParam("file") MultipartFile file) {
        try {
            String tenantId = TenantContext.getTenantId();
            String url = s3StorageService.uploadProductImage(
                    tenantId, productId,
                    file.getInputStream(), file.getSize(),
                    file.getContentType());

            // Automatically track storage usage (bytes → MB)
            usageService.incrementStorage(tenantId, file.getSize() / (1024.0 * 1024.0));

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Product image uploaded successfully",
                            Map.of("url", url, "productId", productId)));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload image: " + e.getMessage()));
        }
    }

    @PostMapping("/files")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Upload a file to tenant's S3 storage")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        try {
            String tenantId = TenantContext.getTenantId();
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
            String url = s3StorageService.uploadTenantFile(
                    tenantId, filename,
                    file.getInputStream(), file.getSize(),
                    file.getContentType());

            // Automatically track storage usage (bytes → MB)
            usageService.incrementStorage(tenantId, file.getSize() / (1024.0 * 1024.0));

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("File uploaded successfully",
                            Map.of("url", url, "filename", filename)));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/files")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "List all files in tenant's S3 storage")
    public ResponseEntity<ApiResponse<List<String>>> listFiles() {
        String tenantId = TenantContext.getTenantId();
        List<String> files = s3StorageService.listTenantFiles(tenantId);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    @DeleteMapping("/products/{productId}/assets")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN')")
    @Operation(summary = "Delete all S3 assets for a product")
    public ResponseEntity<ApiResponse<Void>> deleteProductAssets(@PathVariable String productId) {
        String tenantId = TenantContext.getTenantId();
        s3StorageService.deleteProductAssets(tenantId, productId);
        return ResponseEntity.ok(ApiResponse.success("Product assets deleted successfully", null));
    }

    @GetMapping("/products/{productId}/image-url")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TENANT_ADMIN', 'USER')")
    @Operation(summary = "Get product image URL from S3")
    public ResponseEntity<ApiResponse<Map<String, String>>> getProductImageUrl(@PathVariable String productId) {
        String tenantId = TenantContext.getTenantId();
        String url = s3StorageService.getProductImageUrl(tenantId, productId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url, "productId", productId)));
    }
}


