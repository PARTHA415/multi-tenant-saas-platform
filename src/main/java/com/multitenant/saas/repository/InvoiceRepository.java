package com.multitenant.saas.repository;

import com.multitenant.saas.model.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {

    List<Invoice> findAllByTenantId(String tenantId);

    Optional<Invoice> findByTenantIdAndMonth(String tenantId, String month);
}

