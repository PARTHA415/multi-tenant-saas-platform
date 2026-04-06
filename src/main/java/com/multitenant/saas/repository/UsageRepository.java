package com.multitenant.saas.repository;

import com.multitenant.saas.model.Usage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsageRepository extends MongoRepository<Usage, String> {

    Optional<Usage> findByTenantIdAndMonth(String tenantId, String month);

    List<Usage> findAllByTenantId(String tenantId);

    List<Usage> findAllByMonth(String month);
}

