package com.multitenant.saas.model;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "roles")
public class Role extends BaseEntity {

    private String roleName;

    public Role() {}

    public Role(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public static RoleBuilder builder() { return new RoleBuilder(); }

    public static class RoleBuilder {
        private String roleName;
        public RoleBuilder roleName(String roleName) { this.roleName = roleName; return this; }
        public Role build() {
            return new Role(roleName);
        }
    }
}
