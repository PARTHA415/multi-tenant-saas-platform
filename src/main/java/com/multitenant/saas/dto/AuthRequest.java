package com.multitenant.saas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthRequest {
    @NotBlank(message = "Email is required") @Email(message = "Invalid email format")
    private String email;
    @NotBlank(message = "Password is required")
    private String password;

    public AuthRequest() {}
    public AuthRequest(String email, String password) { this.email = email; this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public static AuthRequestBuilder builder() { return new AuthRequestBuilder(); }
    public static class AuthRequestBuilder {
        private String email, password;
        public AuthRequestBuilder email(String email) { this.email = email; return this; }
        public AuthRequestBuilder password(String password) { this.password = password; return this; }
        public AuthRequest build() { return new AuthRequest(email, password); }
    }
}
