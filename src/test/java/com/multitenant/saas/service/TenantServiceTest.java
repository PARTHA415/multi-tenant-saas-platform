package com.multitenant.saas.service;

import com.multitenant.saas.dto.TenantDTO;
import com.multitenant.saas.exception.DuplicateResourceException;
import com.multitenant.saas.exception.ResourceNotFoundException;
import com.multitenant.saas.model.Tenant;
import com.multitenant.saas.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    private Tenant sampleTenant;
    private TenantDTO sampleDTO;

    @BeforeEach
    void setUp() {
        sampleTenant = Tenant.builder()
                .id("tenant-1")
                .name("Test Corp")
                .subscriptionPlan("ENTERPRISE")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        sampleDTO = TenantDTO.builder()
                .name("Test Corp")
                .subscriptionPlan("ENTERPRISE")
                .build();
    }

    @Test
    @DisplayName("Should create a new tenant")
    void shouldCreateTenant() {
        when(tenantRepository.existsByName("Test Corp")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(sampleTenant);

        TenantDTO result = tenantService.createTenant(sampleDTO);

        assertNotNull(result);
        assertEquals("Test Corp", result.getName());
        assertEquals("ENTERPRISE", result.getSubscriptionPlan());
        assertTrue(result.isActive());
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should throw exception for duplicate tenant name")
    void shouldThrowForDuplicateTenant() {
        when(tenantRepository.existsByName("Test Corp")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> tenantService.createTenant(sampleDTO));
        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return all tenants")
    void shouldGetAllTenants() {
        when(tenantRepository.findAll()).thenReturn(List.of(sampleTenant));

        List<TenantDTO> result = tenantService.getAllTenants();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Corp", result.get(0).getName());
    }

    @Test
    @DisplayName("Should get tenant by ID")
    void shouldGetTenantById() {
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(sampleTenant));

        TenantDTO result = tenantService.getTenantById("tenant-1");

        assertNotNull(result);
        assertEquals("tenant-1", result.getId());
        assertEquals("Test Corp", result.getName());
    }

    @Test
    @DisplayName("Should throw exception when tenant not found")
    void shouldThrowWhenTenantNotFound() {
        when(tenantRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> tenantService.getTenantById("nonexistent"));
    }

    @Test
    @DisplayName("Should default to FREE plan when not specified")
    void shouldDefaultToFreePlan() {
        TenantDTO dto = TenantDTO.builder()
                .name("Free Tenant")
                .build();

        Tenant savedTenant = Tenant.builder()
                .id("tenant-2")
                .name("Free Tenant")
                .subscriptionPlan("FREE")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(tenantRepository.existsByName("Free Tenant")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);

        TenantDTO result = tenantService.createTenant(dto);

        assertEquals("FREE", result.getSubscriptionPlan());
    }
}

