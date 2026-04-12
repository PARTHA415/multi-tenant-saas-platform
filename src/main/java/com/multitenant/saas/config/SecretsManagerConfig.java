package com.multitenant.saas.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * Retrieves sensitive configuration from AWS Secrets Manager.
 * Replaces hardcoded secrets in application.yml in production.
 *
 * Secrets stored in AWS:
 *   - multitenant-saas/jwt-secret → JWT signing key
 *   - multitenant-saas/mongodb-uri → MongoDB connection URI
 */
@Component
public class SecretsManagerConfig {

    private static final Logger log = LoggerFactory.getLogger(SecretsManagerConfig.class);
    private final SecretsManagerClient secretsManagerClient;

    public SecretsManagerConfig(SecretsManagerClient secretsManagerClient) {
        this.secretsManagerClient = secretsManagerClient;
    }

    /**
     * Fetch the JWT signing secret from Secrets Manager.
     */
    public String getJwtSecret() {
        return getSecret("multitenant-saas/jwt-secret");
    }

    /**
     * Fetch the MongoDB connection URI from Secrets Manager.
     */
    public String getMongoUri() {
        return getSecret("multitenant-saas/mongodb-uri");
    }

    /**
     * Generic secret retrieval by name.
     */
    public String getSecret(String secretName) {
        try {
            log.info("Fetching secret from AWS Secrets Manager: {}", secretName);

            GetSecretValueResponse response = secretsManagerClient.getSecretValue(
                    GetSecretValueRequest.builder()
                            .secretId(secretName)
                            .build()
            );

            log.info("Secret retrieved successfully: {}", secretName);
            return response.secretString();

        } catch (Exception e) {
            log.error("Failed to retrieve secret '{}': {}", secretName, e.getMessage());
            throw new RuntimeException("Could not retrieve secret: " + secretName, e);
        }
    }
}

