package com.multitenant.saas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class S3StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name:multi-tenant-saas-common-assets}")
    private String bucketName;

    public S3StorageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Upload a product image. Files are stored in tenant-isolated paths:
     * tenants/{tenantId}/products/{productId}/image
     */
    public String uploadProductImage(String tenantId, String productId,
                                     InputStream inputStream, long contentLength,
                                     String contentType) {
        String key = buildProductImageKey(tenantId, productId);
        log.info("Uploading product image: {} for tenant: {}", key, tenantId);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

        String url = String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);
        log.info("Product image uploaded: {}", url);
        return url;
    }

    /**
     * Upload any tenant file (e.g., invoice PDF, report).
     * tenants/{tenantId}/files/{filename}
     */
    public String uploadTenantFile(String tenantId, String filename,
                                   InputStream inputStream, long contentLength,
                                   String contentType) {
        String key = String.format("tenants/%s/files/%s", tenantId, filename);
        log.info("Uploading tenant file: {} for tenant: {}", key, tenantId);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));

        String url = String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);
        log.info("Tenant file uploaded: {}", url);
        return url;
    }

    /**
     * Delete all assets for a specific product (when product is deleted).
     */
    public void deleteProductAssets(String tenantId, String productId) {
        String prefix = String.format("tenants/%s/products/%s/", tenantId, productId);
        log.info("Deleting product assets with prefix: {}", prefix);

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

        for (S3Object obj : listResponse.contents()) {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(obj.key())
                    .build());
            log.debug("Deleted S3 object: {}", obj.key());
        }

        log.info("Deleted {} product assets for product: {}", listResponse.contents().size(), productId);
    }

    /**
     * List all files for a tenant.
     */
    public List<String> listTenantFiles(String tenantId) {
        String prefix = String.format("tenants/%s/", tenantId);
        log.debug("Listing files for tenant: {} with prefix: {}", tenantId, prefix);

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

        return listResponse.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    /**
     * Get the S3 URL for a product image.
     */
    public String getProductImageUrl(String tenantId, String productId) {
        String key = buildProductImageKey(tenantId, productId);
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);
    }

    /**
     * Generate a pre-signed URL for downloading an S3 object.
     * URL expires after the specified duration (default: 15 minutes).
     */
    public String generatePresignedUrl(String s3Key, Duration expiration) {
        log.info("Generating pre-signed URL for key: {} (expires in {})", s3Key, expiration);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        String url = presignedRequest.url().toString();
        log.info("Pre-signed URL generated for key: {}", s3Key);
        return url;
    }

    /**
     * Generate a pre-signed URL with default 15-minute expiration.
     */
    public String generatePresignedUrl(String s3Key) {
        return generatePresignedUrl(s3Key, Duration.ofMinutes(15));
    }

    private String buildProductImageKey(String tenantId, String productId) {
        return String.format("tenants/%s/products/%s/image", tenantId, productId);
    }
}

