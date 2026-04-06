package com.multitenant.saas.dto;

import java.util.Set;

public class AuthResponse {
    private String token;
    private String email;
    private String tenantId;
    private Set<String> roles;

    public AuthResponse() {}
    public AuthResponse(String token, String email, String tenantId, Set<String> roles) {
        this.token = token; this.email = email; this.tenantId = tenantId; this.roles = roles;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }

    public static AuthResponseBuilder builder() { return new AuthResponseBuilder(); }
    public static class AuthResponseBuilder {
        private String token, email, tenantId;
        private Set<String> roles;
        public AuthResponseBuilder token(String token) { this.token = token; return this; }
        public AuthResponseBuilder email(String email) { this.email = email; return this; }
        public AuthResponseBuilder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public AuthResponseBuilder roles(Set<String> roles) { this.roles = roles; return this; }
        public AuthResponse build() { return new AuthResponse(token, email, tenantId, roles); }
    }
}
