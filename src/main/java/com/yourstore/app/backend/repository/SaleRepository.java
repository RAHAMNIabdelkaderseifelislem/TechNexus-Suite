package com.yourstore.app.backend.repository;

import com.yourstore.app.backend.model.entity.Sale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    // Fetch sales with items and products to avoid N+1 queries when listing
    @Override
    @EntityGraph(attributePaths = {"items", "items.product", "user"})
    List<Sale> findAll();
}