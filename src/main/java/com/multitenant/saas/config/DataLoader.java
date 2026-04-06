package com.multitenant.saas.config;

import com.multitenant.saas.model.Product;
import com.multitenant.saas.model.Tenant;
import com.multitenant.saas.model.Usage;
import com.multitenant.saas.model.User;
import com.multitenant.saas.repository.ProductRepository;
import com.multitenant.saas.repository.TenantRepository;
import com.multitenant.saas.repository.UsageRepository;
import com.multitenant.saas.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final UsageRepository usageRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(TenantRepository tenantRepository, UserRepository userRepository,
                      ProductRepository productRepository, UsageRepository usageRepository,
                      PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.usageRepository = usageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (tenantRepository.count() > 0) {
            log.info("Database already seeded, skipping data loader");
            return;
        }

        log.info("=== Seeding sample data ===");

        // Create tenants
        Tenant tenant1 = tenantRepository.save(Tenant.builder()
                .name("Acme Corp")
                .subscriptionPlan("ENTERPRISE")
                .active(true)
                .config(Map.of("theme", "dark", "maxUsers", 100))
                .createdAt(LocalDateTime.now())
                .build());

        Tenant tenant2 = tenantRepository.save(Tenant.builder()
                .name("Startup Inc")
                .subscriptionPlan("STARTER")
                .active(true)
                .config(Map.of("theme", "light", "maxUsers", 20))
                .createdAt(LocalDateTime.now())
                .build());

        Tenant tenant3 = tenantRepository.save(Tenant.builder()
                .name("FreeUser LLC")
                .subscriptionPlan("FREE")
                .active(true)
                .config(Map.of("theme", "light", "maxUsers", 5))
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Created tenants: {}, {}, {}", tenant1.getId(), tenant2.getId(), tenant3.getId());

        // Create users for tenant 1
        User superAdmin = User.builder()
                .email("admin@acme.com")
                .password(passwordEncoder.encode("admin123"))
                .firstName("Super")
                .lastName("Admin")
                .roles(Set.of("SUPER_ADMIN"))
                .active(true)
                .build();
        superAdmin.setTenantId(tenant1.getId());
        userRepository.save(superAdmin);

        User tenantAdmin = User.builder()
                .email("manager@acme.com")
                .password(passwordEncoder.encode("manager123"))
                .firstName("Tenant")
                .lastName("Admin")
                .roles(Set.of("TENANT_ADMIN"))
                .active(true)
                .build();
        tenantAdmin.setTenantId(tenant1.getId());
        userRepository.save(tenantAdmin);

        User regularUser = User.builder()
                .email("user@acme.com")
                .password(passwordEncoder.encode("user123"))
                .firstName("Regular")
                .lastName("User")
                .roles(Set.of("USER"))
                .active(true)
                .build();
        regularUser.setTenantId(tenant1.getId());
        userRepository.save(regularUser);

        // Create user for tenant 2
        User tenant2Admin = User.builder()
                .email("admin@startup.com")
                .password(passwordEncoder.encode("admin123"))
                .firstName("Startup")
                .lastName("Admin")
                .roles(Set.of("TENANT_ADMIN"))
                .active(true)
                .build();
        tenant2Admin.setTenantId(tenant2.getId());
        userRepository.save(tenant2Admin);

        log.info("Created users for tenants");

        // Create products for tenant 1
        Product product1 = Product.builder()
                .name("Widget Pro")
                .description("Professional grade widget")
                .price(BigDecimal.valueOf(29.99))
                .active(true)
                .productCount(50)
                .attributes(Map.of("color", "blue", "weight", "150g", "warranty", "2 years"))
                .build();
        product1.setTenantId(tenant1.getId());
        productRepository.save(product1);

        Product product2 = Product.builder()
                .name("Gadget Plus")
                .description("Advanced gadget with premium features")
                .price(BigDecimal.valueOf(99.99))
                .active(true)
                .productCount(30)
                .attributes(Map.of("material", "titanium", "battery", "5000mAh"))
                .build();
        product2.setTenantId(tenant1.getId());
        productRepository.save(product2);

        // Create product for tenant 2
        Product product3 = Product.builder()
                .name("Startup Kit")
                .description("Everything you need to start")
                .price(BigDecimal.valueOf(49.99))
                .active(true)
                .productCount(100)
                .attributes(Map.of("items", "10", "category", "bundle"))
                .build();
        product3.setTenantId(tenant2.getId());
        productRepository.save(product3);

        log.info("Created sample products");

        // Create sample usage data
        Usage usage1 = Usage.builder()
                .apiCalls(1500)
                .storageUsed(256.5)
                .month("2026-03")
                .build();
        usage1.setTenantId(tenant1.getId());
        usageRepository.save(usage1);

        Usage usage2 = Usage.builder()
                .apiCalls(300)
                .storageUsed(50.0)
                .month("2026-03")
                .build();
        usage2.setTenantId(tenant2.getId());
        usageRepository.save(usage2);

        log.info("Created sample usage data");

        log.info("=== Sample data seeding complete ===");
        log.info("");
        log.info("=== Test Credentials ===");
        log.info("Tenant 1 (Acme Corp) ID: {}", tenant1.getId());
        log.info("  SUPER_ADMIN: admin@acme.com / admin123");
        log.info("  TENANT_ADMIN: manager@acme.com / manager123");
        log.info("  USER: user@acme.com / user123");
        log.info("Tenant 2 (Startup Inc) ID: {}", tenant2.getId());
        log.info("  TENANT_ADMIN: admin@startup.com / admin123");
        log.info("Tenant 3 (FreeUser LLC) ID: {}", tenant3.getId());
    }
}


