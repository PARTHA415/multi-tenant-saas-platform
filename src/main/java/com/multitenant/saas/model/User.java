package com.multitenant.saas.model;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
@CompoundIndex(name = "tenant_email_idx", def = "{'tenantId': 1, 'email': 1}", unique = true)
public class User extends BaseEntity {

    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private Set<String> roles = new HashSet<>();
    private boolean active;

    public User() {}

    public User(String email, String password, String firstName, String lastName, Set<String> roles, boolean active) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = roles != null ? roles : new HashSet<>();
        this.active = active;
    }

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

    public static UserBuilder builder() { return new UserBuilder(); }

    public static class UserBuilder {
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private Set<String> roles = new HashSet<>();
        private boolean active;

        public UserBuilder email(String email) { this.email = email; return this; }
        public UserBuilder password(String password) { this.password = password; return this; }
        public UserBuilder firstName(String firstName) { this.firstName = firstName; return this; }
        public UserBuilder lastName(String lastName) { this.lastName = lastName; return this; }
        public UserBuilder roles(Set<String> roles) { this.roles = roles; return this; }
        public UserBuilder active(boolean active) { this.active = active; return this; }

        public User build() {
            return new User(email, password, firstName, lastName, roles, active);
        }
    }
}
