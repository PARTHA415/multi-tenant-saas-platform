package com.multitenant.saas.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.multitenant.saas.config.TenantContext;
import com.multitenant.saas.dto.AuthRequest;
import com.multitenant.saas.dto.ProductDTO;
import com.multitenant.saas.model.Tenant;
import com.multitenant.saas.model.User;
import com.multitenant.saas.repository.*;
import com.multitenant.saas.security.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test to verify strict tenant isolation.
 * Tests that data from one tenant is never accessible to another.
 *
 * NOTE: Requires a running MongoDB instance.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TenantIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private static String tenant1Id;
    private static String tenant2Id;
    private static String tenant1Token;
    private static String tenant2Token;

    @BeforeEach
    void setUp() {
        // Clean up
        productRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // Create tenant 1
        Tenant t1 = tenantRepository.save(Tenant.builder()
                .name("Tenant One")
                .subscriptionPlan("ENTERPRISE")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        tenant1Id = t1.getId();

        // Create tenant 2
        Tenant t2 = tenantRepository.save(Tenant.builder()
                .name("Tenant Two")
                .subscriptionPlan("STARTER")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build());
        tenant2Id = t2.getId();

        // Create admin for tenant 1
        User user1 = User.builder()
                .email("admin@tenant1.com")
                .password(passwordEncoder.encode("password"))
                .roles(Set.of("TENANT_ADMIN"))
                .active(true)
                .build();
        user1.setTenantId(tenant1Id);
        userRepository.save(user1);

        // Create admin for tenant 2
        User user2 = User.builder()
                .email("admin@tenant2.com")
                .password(passwordEncoder.encode("password"))
                .roles(Set.of("TENANT_ADMIN"))
                .active(true)
                .build();
        user2.setTenantId(tenant2Id);
        userRepository.save(user2);

        // Generate tokens
        tenant1Token = jwtUtil.generateToken("admin@tenant1.com", tenant1Id, Set.of("TENANT_ADMIN"));
        tenant2Token = jwtUtil.generateToken("admin@tenant2.com", tenant2Id, Set.of("TENANT_ADMIN"));
    }

    @Test
    @Order(1)
    @DisplayName("Tenant 1 should only see their own products")
    void tenant1ShouldOnlySeeOwnProducts() throws Exception {
        // Create product as tenant 1
        ProductDTO product = ProductDTO.builder()
                .name("Tenant1 Product")
                .description("Belongs to tenant 1")
                .price(BigDecimal.valueOf(10.00))
                .active(true)
                .attributes(Map.of("owner", "tenant1"))
                .build();

        mockMvc.perform(post("/products")
                        .header("X-Tenant-ID", tenant1Id)
                        .header("Authorization", "Bearer " + tenant1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Tenant1 Product"));

        // Tenant 1 should see the product
        mockMvc.perform(get("/products")
                        .header("X-Tenant-ID", tenant1Id)
                        .header("Authorization", "Bearer " + tenant1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Tenant1 Product"));

        // Tenant 2 should NOT see tenant 1's product
        mockMvc.perform(get("/products")
                        .header("X-Tenant-ID", tenant2Id)
                        .header("Authorization", "Bearer " + tenant2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @Order(2)
    @DisplayName("Tenant 2 should not access tenant 1's product by ID")
    void tenant2ShouldNotAccessTenant1ProductById() throws Exception {
        // Create product as tenant 1
        ProductDTO product = ProductDTO.builder()
                .name("Secret Product")
                .price(BigDecimal.valueOf(999.99))
                .active(true)
                .build();

        MvcResult createResult = mockMvc.perform(post("/products")
                        .header("X-Tenant-ID", tenant1Id)
                        .header("Authorization", "Bearer " + tenant1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isCreated())
                .andReturn();

        String productId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asText();

        // Tenant 2 tries to access tenant 1's product — should get 404
        mockMvc.perform(get("/products/" + productId)
                        .header("X-Tenant-ID", tenant2Id)
                        .header("Authorization", "Bearer " + tenant2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(3)
    @DisplayName("Should reject requests without X-Tenant-ID header")
    void shouldRejectRequestsWithoutTenantHeader() throws Exception {
        mockMvc.perform(get("/products")
                        .header("Authorization", "Bearer " + tenant1Token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    @DisplayName("Should reject JWT with mismatched tenant ID")
    void shouldRejectMismatchedTenantId() throws Exception {
        // Token is for tenant1 but header says tenant2
        mockMvc.perform(get("/products")
                        .header("X-Tenant-ID", tenant2Id)
                        .header("Authorization", "Bearer " + tenant1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    @DisplayName("Auth login should be tenant-scoped")
    void authLoginShouldBeTenantScoped() throws Exception {
        AuthRequest request = AuthRequest.builder()
                .email("admin@tenant1.com")
                .password("password")
                .build();

        // Login to correct tenant
        mockMvc.perform(post("/auth/login")
                        .header("X-Tenant-ID", tenant1Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tenantId").value(tenant1Id));

        // Login to wrong tenant — user doesn't exist there
        mockMvc.perform(post("/auth/login")
                        .header("X-Tenant-ID", tenant2Id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}

