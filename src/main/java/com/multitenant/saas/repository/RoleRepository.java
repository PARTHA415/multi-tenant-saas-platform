package com.multitenant.saas.repository;

import com.multitenant.saas.model.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends MongoRepository<Role, String> {

    List<Role> findAllByTenantId(String tenantId);

    Optional<Role> findByTenantIdAndRoleName(String tenantId, String roleName);
}

