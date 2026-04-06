package com.multitenant.saas.repository;

import com.multitenant.saas.model.Tenant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends MongoRepository<Tenant, String> {

    Optional<Tenant> findByName(String name);

    boolean existsByName(String name);
}

