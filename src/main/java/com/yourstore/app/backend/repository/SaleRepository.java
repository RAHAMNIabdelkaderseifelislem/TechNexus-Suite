package com.yourstore.app.backend.repository;

import com.yourstore.app.backend.model.entity.Sale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.math.BigDecimal;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    @Override
    @EntityGraph(attributePaths = {"items", "items.product", "user"})
    List<Sale> findAll();

    @Query("SELECT SUM(s.totalAmount) FROM Sale s")
    BigDecimal findTotalSalesRevenue();
}