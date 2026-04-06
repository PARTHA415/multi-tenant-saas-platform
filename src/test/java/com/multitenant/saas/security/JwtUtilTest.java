package com.multitenant.saas.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // Base64 encoded secret (at least 256 bits)
        String secret = "Y2hhbmdlLXRoaXMtdG8tYS12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3ItcHJvZHVjdGlvbi11c2UtYXQtbGVhc3QtMjU2LWJpdHM=";
        jwtUtil = new JwtUtil(secret, 86400000L);
    }

    @Test
    @DisplayName("Should generate a valid JWT token")
    void shouldGenerateToken() {
        String token = jwtUtil.generateToken("test@example.com", "tenant-1", Set.of("USER"));

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Should extract email from token")
    void shouldExtractEmail() {
        String token = jwtUtil.generateToken("test@example.com", "tenant-1", Set.of("USER"));

        String email = jwtUtil.extractEmail(token);

        assertEquals("test@example.com", email);
    }

    @Test
    @DisplayName("Should extract tenant ID from token")
    void shouldExtractTenantId() {
        String token = jwtUtil.generateToken("test@example.com", "tenant-1", Set.of("USER"));

        String tenantId = jwtUtil.extractTenantId(token);

        assertEquals("tenant-1", tenantId);
    }

    @Test
    @DisplayName("Should extract roles from token")
    void shouldExtractRoles() {
        String token = jwtUtil.generateToken("test@example.com", "tenant-1", Set.of("TENANT_ADMIN", "USER"));

        List<String> roles = jwtUtil.extractRoles(token);

        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("TENANT_ADMIN"));
        assertTrue(roles.contains("USER"));
    }

    @Test
    @DisplayName("Should validate a valid token")
    void shouldValidateValidToken() {
        String token = jwtUtil.generateToken("test@example.com", "tenant-1", Set.of("USER"));

        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("Should reject an invalid token")
    void shouldRejectInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    @DisplayName("Should reject an expired token")
    void shouldRejectExpiredToken() {
        // Create JwtUtil with 0ms expiration
        JwtUtil expiredJwtUtil = new JwtUtil(
                "Y2hhbmdlLXRoaXMtdG8tYS12ZXJ5LWxvbmctc2VjcmV0LWtleS1mb3ItcHJvZHVjdGlvbi11c2UtYXQtbGVhc3QtMjU2LWJpdHM=",
                0L
        );
        String token = expiredJwtUtil.generateToken("test@example.com", "tenant-1", Set.of("USER"));

        // Token should be expired immediately
        assertFalse(expiredJwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("Should reject null token")
    void shouldRejectNullToken() {
        assertFalse(jwtUtil.validateToken(null));
    }
}

