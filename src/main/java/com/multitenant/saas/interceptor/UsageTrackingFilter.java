package com.multitenant.saas.interceptor;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.service.UsageService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Automatically tracks API calls per tenant.
 * Runs after each successful request and increments the tenant's apiCalls counter.
 * This replaces the manual POST /usage/update endpoint for API call tracking —
 * usage is now metered server-side, not self-reported by tenants.
 */
@Component
public class UsageTrackingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UsageTrackingFilter.class);
    private final UsageService usageService;

    public UsageTrackingFilter(UsageService usageService) {
        this.usageService = usageService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Let the request proceed first
        filterChain.doFilter(request, response);

        // After request completes, track API call if tenant context exists and response was successful
        try {
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null && response.getStatus() < 400) {
                usageService.incrementApiCall(tenantId);
                log.debug("API call tracked for tenant: {} — {} {} → {}",
                        tenantId, request.getMethod(), request.getRequestURI(), response.getStatus());
            }
        } catch (Exception e) {
            // Never fail the original request due to usage tracking errors
            log.warn("Failed to track API usage: {}", e.getMessage());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Don't track public/non-tenant endpoints
        return path.startsWith("/tenants") ||
               path.startsWith("/auth") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/actuator") ||
               path.equals("/error");
    }
}

