package com.multitenant.saas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public class UserDTO {
    private String id;
    @NotBlank(message = "Email is required") @Email(message = "Invalid email format")
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private boolean active;
    private String tenantId;

    public UserDTO() {}
    public UserDTO(String id, String email, String password, String firstName, String lastName, Set<String> roles, boolean active, String tenantId) {
        this.id = id; this.email = email; this.password = password; this.firstName = firstName; this.lastName = lastName; this.roles = roles; this.active = active; this.tenantId = tenantId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public static UserDTOBuilder builder() { return new UserDTOBuilder(); }
    public static class UserDTOBuilder {
        private String id, email, password, firstName, lastName, tenantId;
        private Set<String> roles;
        private boolean active;
        public UserDTOBuilder id(String id) { this.id = id; return this; }
        public UserDTOBuilder email(String email) { this.email = email; return this; }
        public UserDTOBuilder password(String password) { this.password = password; return this; }
        public UserDTOBuilder firstName(String firstName) { this.firstName = firstName; return this; }
        public UserDTOBuilder lastName(String lastName) { this.lastName = lastName; return this; }
        public UserDTOBuilder roles(Set<String> roles) { this.roles = roles; return this; }
        public UserDTOBuilder active(boolean active) { this.active = active; return this; }
        public UserDTOBuilder tenantId(String tenantId) { this.tenantId = tenantId; return this; }
        public UserDTO build() { return new UserDTO(id, email, password, firstName, lastName, roles, active, tenantId); }
    }
}
