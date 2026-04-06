package com.multitenant.saas.config;

import com.multitenant.saas.interceptor.RateLimitInterceptor;
import com.multitenant.saas.interceptor.TenantInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    public WebMvcConfig(TenantInterceptor tenantInterceptor, RateLimitInterceptor rateLimitInterceptor) {
        this.tenantInterceptor = tenantInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Tenant interceptor applies to all API paths except auth and swagger
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/tenants/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/actuator/**",
                        "/error"
                );

        // Rate limit interceptor applies after tenant resolution
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/tenants/**",
                        "/auth/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/actuator/**",
                        "/error"
                );
    }
}


