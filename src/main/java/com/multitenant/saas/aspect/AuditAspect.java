package com.multitenant.saas.aspect;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.model.AuditLog;
import com.multitenant.saas.repository.AuditLogRepository;
import com.multitenant.saas.service.CloudWatchMetricsService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);
    private final AuditLogRepository auditLogRepository;
    private final CloudWatchMetricsService cloudWatchMetricsService;

    public AuditAspect(AuditLogRepository auditLogRepository, CloudWatchMetricsService cloudWatchMetricsService) {
        this.auditLogRepository = auditLogRepository;
        this.cloudWatchMetricsService = cloudWatchMetricsService;
    }

    @Around("execution(* com.multitenant.saas.service.*.*(..)) && !execution(* com.multitenant.saas.service.CloudWatchMetricsService.*(..)) && !execution(* com.multitenant.saas.service.LambdaInvoiceService.*(..))")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // AWS CloudWatch: record every API call per tenant (all operations)
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            cloudWatchMetricsService.recordApiCall(tenantId, className + "." + methodName);
        }

        // Only audit write operations
        if (!isWriteOperation(methodName)) {
            return joinPoint.proceed();
        }

        String userId = getCurrentUserId();

        long startTime = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - startTime;

        try {
            AuditLog auditLog = AuditLog.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .action(methodName)
                    .entity(className.replace("Service", ""))
                    .details(String.format("Method: %s.%s, Duration: %dms", className, methodName, duration))
                    .timestamp(LocalDateTime.now())
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} -> {}.{} ({}ms)", tenantId, className, methodName, duration);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
            // Don't fail the original operation if audit logging fails
        }

        return result;
    }

    private boolean isWriteOperation(String methodName) {
        return methodName.startsWith("create") ||
               methodName.startsWith("update") ||
               methodName.startsWith("delete") ||
               methodName.startsWith("register") ||
               methodName.startsWith("login") ||
               methodName.startsWith("generate") ||
               methodName.startsWith("increment") ||
               methodName.startsWith("sell");
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
}


