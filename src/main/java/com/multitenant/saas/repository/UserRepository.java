package com.multitenant.saas.repository;

import com.multitenant.saas.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByTenantIdAndEmail(String tenantId, String email);

    List<User> findAllByTenantId(String tenantId);

    boolean existsByTenantIdAndEmail(String tenantId, String email);

    Optional<User> findByIdAndTenantId(String id, String tenantId);
}

