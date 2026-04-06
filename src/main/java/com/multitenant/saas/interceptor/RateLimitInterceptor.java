package com.multitenant.saas.interceptor;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.model.Tenant;
import com.multitenant.saas.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private final TenantRepository tenantRepository;

    public RateLimitInterceptor(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    private static final Map<String, RateLimitBucket> BUCKETS = new ConcurrentHashMap<>();
    private static final long WINDOW_MS = 60_000; // 1 minute window

    private static final Map<String, Integer> PLAN_LIMITS = Map.of(
            "FREE", 20,
            "STARTER", 60,
            "ENTERPRISE", 200
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return true; // Let tenant interceptor handle missing tenant
        }

        int limit = getLimit(tenantId);
        RateLimitBucket bucket = BUCKETS.computeIfAbsent(tenantId, k -> new RateLimitBucket());

        if (!bucket.tryConsume(limit)) {
            log.warn("Rate limit exceeded for tenant: {}", tenantId);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded. Try again later.\"}");
            return false;
        }

        return true;
    }

    private int getLimit(String tenantId) {
        try {
            return tenantRepository.findById(tenantId)
                    .map(Tenant::getSubscriptionPlan)
                    .map(plan -> PLAN_LIMITS.getOrDefault(plan.toUpperCase(), 20))
                    .orElse(20);
        } catch (Exception e) {
            log.warn("Failed to resolve tenant plan for rate limiting, using default: {}", e.getMessage());
            return 20;
        }
    }

    private static class RateLimitBucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());

        boolean tryConsume(int limit) {
            long now = System.currentTimeMillis();
            long start = windowStart.get();

            if (now - start > WINDOW_MS) {
                // Reset window
                windowStart.set(now);
                count.set(1);
                return true;
            }

            return count.incrementAndGet() <= limit;
        }
    }
}
