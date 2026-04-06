package com.multitenant.saas.repository;

import com.multitenant.saas.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findAllByTenantId(String tenantId);

    Optional<Product> findByIdAndTenantId(String id, String tenantId);

    void deleteByIdAndTenantId(String id, String tenantId);
}

