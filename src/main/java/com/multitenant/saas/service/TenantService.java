package com.multitenant.saas.service;

import com.multitenant.saas.dto.TenantDTO;
import com.multitenant.saas.exception.DuplicateResourceException;
import com.multitenant.saas.exception.ResourceNotFoundException;
import com.multitenant.saas.model.Tenant;
import com.multitenant.saas.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);
    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @CacheEvict(value = "tenants", allEntries = true)
    public TenantDTO createTenant(TenantDTO dto) {
        log.info("Creating tenant: {}", dto.getName());

        if (tenantRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Tenant with name '" + dto.getName() + "' already exists");
        }

        Tenant tenant = Tenant.builder()
                .name(dto.getName())
                .subscriptionPlan(dto.getSubscriptionPlan() != null ? dto.getSubscriptionPlan() : "FREE")
                .active(true)
                .config(dto.getConfig())
                .createdAt(LocalDateTime.now())
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("Tenant created with ID: {}", tenant.getId());

        return toDTO(tenant);
    }

    @Cacheable(value = "tenants")
    public List<TenantDTO> getAllTenants() {
        log.debug("Fetching all tenants");
        return tenantRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "tenants", key = "#id")
    public TenantDTO getTenantById(String id) {
        log.debug("Fetching tenant by ID: {}", id);
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + id));
        return toDTO(tenant);
    }

    public Tenant getTenantEntityById(String id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + id));
    }

    private TenantDTO toDTO(Tenant tenant) {
        return TenantDTO.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .subscriptionPlan(tenant.getSubscriptionPlan())
                .active(tenant.isActive())
                .config(tenant.getConfig())
                .createdAt(tenant.getCreatedAt())
                .build();
    }
}


