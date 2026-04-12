package com.multitenant.saas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;

@Service
public class CloudWatchMetricsService {

    private static final Logger log = LoggerFactory.getLogger(CloudWatchMetricsService.class);
    private static final String NAMESPACE = "MultiTenantSaaS";

    private final CloudWatchClient cloudWatchClient;

    public CloudWatchMetricsService(CloudWatchClient cloudWatchClient) {
        this.cloudWatchClient = cloudWatchClient;
    }

    /**
     * Record an API call metric per tenant and endpoint.
     * Called on every authenticated API request (via AuditAspect or interceptor).
     */
    public void recordApiCall(String tenantId, String endpoint) {
        try {
            putMetric("ApiCalls", 1.0, StandardUnit.COUNT,
                    Dimension.builder().name("TenantId").value(tenantId).build(),
                    Dimension.builder().name("Endpoint").value(endpoint).build());
            log.debug("CloudWatch: recorded API call — tenant={}, endpoint={}", tenantId, endpoint);
        } catch (Exception e) {
            log.warn("CloudWatch: failed to record API call metric — {}", e.getMessage());
        }
    }

    /**
     * Record an out-of-stock event when productCount reaches 0.
     */
    public void recordOutOfStockEvent(String tenantId, String productId) {
        try {
            putMetric("OutOfStockEvents", 1.0, StandardUnit.COUNT,
                    Dimension.builder().name("TenantId").value(tenantId).build(),
                    Dimension.builder().name("ProductId").value(productId).build());
            log.info("CloudWatch: recorded out-of-stock event — tenant={}, product={}", tenantId, productId);
        } catch (Exception e) {
            log.warn("CloudWatch: failed to record out-of-stock metric — {}", e.getMessage());
        }
    }

    /**
     * Record a product sell event with quantity.
     */
    public void recordSellEvent(String tenantId, String productId, int quantity) {
        try {
            putMetric("ProductsSold", (double) quantity, StandardUnit.COUNT,
                    Dimension.builder().name("TenantId").value(tenantId).build(),
                    Dimension.builder().name("ProductId").value(productId).build());
            log.debug("CloudWatch: recorded sell event — tenant={}, product={}, qty={}", tenantId, productId, quantity);
        } catch (Exception e) {
            log.warn("CloudWatch: failed to record sell metric — {}", e.getMessage());
        }
    }

    /**
     * Record usage update metrics.
     */
    public void recordUsageUpdate(String tenantId, long apiCalls, double storageUsed) {
        try {
            Dimension tenantDim = Dimension.builder().name("TenantId").value(tenantId).build();

            putMetric("TenantApiCalls", (double) apiCalls, StandardUnit.COUNT, tenantDim);
            putMetric("TenantStorageUsed", storageUsed, StandardUnit.MEGABYTES, tenantDim);

            log.debug("CloudWatch: recorded usage update — tenant={}, apiCalls={}, storage={}",
                    tenantId, apiCalls, storageUsed);
        } catch (Exception e) {
            log.warn("CloudWatch: failed to record usage metric — {}", e.getMessage());
        }
    }

    /**
     * Record login events (success/failure).
     */
    public void recordLoginEvent(String tenantId, String email, boolean success) {
        try {
            String metricName = success ? "LoginSuccess" : "LoginFailure";
            putMetric(metricName, 1.0, StandardUnit.COUNT,
                    Dimension.builder().name("TenantId").value(tenantId).build());
            log.debug("CloudWatch: recorded {} — tenant={}, email={}", metricName, tenantId, email);
        } catch (Exception e) {
            log.warn("CloudWatch: failed to record login metric — {}", e.getMessage());
        }
    }

    private void putMetric(String metricName, double value, StandardUnit unit, Dimension... dimensions) {
        MetricDatum.Builder datumBuilder = MetricDatum.builder()
                .metricName(metricName)
                .value(value)
                .unit(unit)
                .timestamp(Instant.now());

        if (dimensions.length > 0) {
            datumBuilder.dimensions(dimensions);
        }

        cloudWatchClient.putMetricData(PutMetricDataRequest.builder()
                .namespace(NAMESPACE)
                .metricData(datumBuilder.build())
                .build());
    }
}

