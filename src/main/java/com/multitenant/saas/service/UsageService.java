package com.multitenant.saas.service;

import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.dto.UsageDTO;
import com.multitenant.saas.model.Usage;
import com.multitenant.saas.repository.UsageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsageService {

    private static final Logger log = LoggerFactory.getLogger(UsageService.class);
    private final UsageRepository usageRepository;

    public UsageService(UsageRepository usageRepository) {
        this.usageRepository = usageRepository;
    }

    public UsageDTO updateUsage(UsageDTO dto) {
        String tenantId = TenantContext.getTenantId();
        String month = dto.getMonth() != null ? dto.getMonth()
                : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        log.info("Updating usage for tenant: {} month: {}", tenantId, month);

        Usage usage = usageRepository.findByTenantIdAndMonth(tenantId, month)
                .orElse(Usage.builder()
                        .month(month)
                        .apiCalls(0)
                        .storageUsed(0)
                        .build());

        usage.setTenantId(tenantId);
        usage.setApiCalls(usage.getApiCalls() + dto.getApiCalls());
        usage.setStorageUsed(usage.getStorageUsed() + dto.getStorageUsed());

        usage = usageRepository.save(usage);
        log.info("Usage updated for tenant: {} month: {} — apiCalls: {}, storage: {}",
                tenantId, month, usage.getApiCalls(), usage.getStorageUsed());

        return toDTO(usage);
    }

    public List<UsageDTO> getUsageByTenant() {
        String tenantId = TenantContext.getTenantId();
        return usageRepository.findAllByTenantId(tenantId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public void incrementApiCall(String tenantId) {
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Usage usage = usageRepository.findByTenantIdAndMonth(tenantId, month)
                .orElse(Usage.builder()
                        .month(month)
                        .apiCalls(0)
                        .storageUsed(0)
                        .build());
        usage.setTenantId(tenantId);
        usage.setApiCalls(usage.getApiCalls() + 1);
        usageRepository.save(usage);
    }

    private UsageDTO toDTO(Usage usage) {
        return UsageDTO.builder()
                .tenantId(usage.getTenantId())
                .apiCalls(usage.getApiCalls())
                .storageUsed(usage.getStorageUsed())
                .month(usage.getMonth())
                .build();
    }
}


