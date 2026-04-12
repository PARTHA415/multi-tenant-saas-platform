package com.multitenant.saas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.Map;

@Service
public class LambdaInvoiceService {

    private static final Logger log = LoggerFactory.getLogger(LambdaInvoiceService.class);
    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.lambda.invoice-function-name:generate-tenant-invoice}")
    private String invoiceFunctionName;

    public LambdaInvoiceService(LambdaClient lambdaClient, ObjectMapper objectMapper) {
        this.lambdaClient = lambdaClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Trigger async invoice generation via Lambda.
     * Called internally by UsageService when usage is recorded (auto-tracked by system).
     * Uses "Event" invocation type — fire-and-forget.
     */
    public void triggerInvoiceGeneration(String tenantId, String month,
                                          long apiCalls, double storageUsed) {
        try {
            Map<String, Object> payload = Map.of(
                    "tenantId", tenantId,
                    "month", month,
                    "apiCalls", apiCalls,
                    "storageUsed", storageUsed
            );

            String payloadJson = objectMapper.writeValueAsString(payload);
            log.info("Triggering Lambda invoice generation for tenant: {} month: {}", tenantId, month);

            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName(invoiceFunctionName)
                    .invocationType("Event") // async — no wait for response
                    .payload(SdkBytes.fromUtf8String(payloadJson))
                    .build();

            lambdaClient.invoke(invokeRequest);
            log.info("Lambda invoice trigger sent for tenant: {} month: {}", tenantId, month);

        } catch (Exception e) {
            // Log but don't fail the usage update
            log.error("Lambda invoice trigger failed for tenant: {} — {}", tenantId, e.getMessage());
        }
    }

    /**
     * Synchronous call to Lambda to calculate billing amount.
     * Uses "RequestResponse" invocation type — waits for result.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> calculateBilling(String tenantId, String subscriptionPlan,
                                                  long apiCalls, double storageUsed) {
        try {
            Map<String, Object> payload = Map.of(
                    "tenantId", tenantId,
                    "subscriptionPlan", subscriptionPlan,
                    "apiCalls", apiCalls,
                    "storageUsed", storageUsed
            );

            String payloadJson = objectMapper.writeValueAsString(payload);
            log.info("Calling Lambda for billing calculation: tenant={}", tenantId);

            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName(invoiceFunctionName)
                    .invocationType("RequestResponse") // synchronous
                    .payload(SdkBytes.fromUtf8String(payloadJson))
                    .build();

            InvokeResponse response = lambdaClient.invoke(invokeRequest);
            String responseJson = response.payload().asUtf8String();

            log.info("Lambda billing response for tenant {}: {}", tenantId, responseJson);
            return objectMapper.readValue(responseJson, Map.class);

        } catch (Exception e) {
            log.error("Lambda billing calculation failed for tenant: {} — {}", tenantId, e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Synchronous call to Lambda to generate an invoice PDF.
     * Lambda calculates billing, generates PDF, uploads it to S3, and returns the S3 key.
     *
     * Expected Lambda response:
     * {
     *   "amount": 10.50,
     *   "pdfS3Key": "tenants/{tenantId}/invoices/{month}/invoice.pdf",
     *   "status": "GENERATED"
     * }
     *
     * Uses "RequestResponse" invocation type — waits for result.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateInvoicePdf(String tenantId, String subscriptionPlan,
                                                   String month, long apiCalls, double storageUsed) {
        try {
            Map<String, Object> payload = Map.of(
                    "action", "GENERATE_PDF",
                    "tenantId", tenantId,
                    "subscriptionPlan", subscriptionPlan,
                    "month", month,
                    "apiCalls", apiCalls,
                    "storageUsed", storageUsed
            );

            String payloadJson = objectMapper.writeValueAsString(payload);
            log.info("Calling Lambda for invoice PDF generation: tenant={} month={}", tenantId, month);

            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName(invoiceFunctionName)
                    .invocationType("RequestResponse") // synchronous — wait for PDF to be generated
                    .payload(SdkBytes.fromUtf8String(payloadJson))
                    .build();

            InvokeResponse response = lambdaClient.invoke(invokeRequest);
            String responseJson = response.payload().asUtf8String();

            log.info("Lambda PDF generation response for tenant {}: {}", tenantId, responseJson);

            Map<String, Object> result = objectMapper.readValue(responseJson, Map.class);

            if (response.statusCode() != 200 || result.containsKey("error")) {
                log.error("Lambda PDF generation returned error for tenant {}: {}", tenantId, result);
                return Map.of("error", result.getOrDefault("error", "Lambda invocation failed"));
            }

            return result;

        } catch (Exception e) {
            log.error("Lambda invoice PDF generation failed for tenant: {} — {}", tenantId, e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}

