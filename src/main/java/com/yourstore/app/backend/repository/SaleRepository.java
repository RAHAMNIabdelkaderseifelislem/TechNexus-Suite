package com.yourstore.app.backend.repository;

import com.yourstore.app.backend.model.entity.Sale;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    @Override
    @EntityGraph(attributePaths = {"items", "items.product", "user"})
    List<Sale> findAll();

    @Override
    @EntityGraph(attributePaths = {"items", "items.product", "user"})
    Optional<Sale> findById(Long id);

    @Query("SELECT SUM(s.totalAmount) FROM Sale s")
    BigDecimal findTotalSalesRevenue();

    @Query("SELECT si.product.category, COUNT(DISTINCT s.id) as salesCount " +
           "FROM Sale s JOIN s.items si " +
           "GROUP BY si.product.category ORDER BY salesCount DESC")
    List<Object[]> findSalesCountPerCategory(); // Object[] because category is Enum
    // In SaleRepository.java
    @Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE DATE(s.saleDate) = CURRENT_DATE")
    BigDecimal findTodaysSalesRevenue();

    @Query("SELECT FUNCTION('DATE', s.saleDate) as saleDay, SUM(s.totalAmount) as dailyTotal " +
           "FROM Sale s " +
           "WHERE s.saleDate BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('DATE', s.saleDate) " +
           "ORDER BY saleDay ASC")
    List<Object[]> findDailySalesTotalsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Find top N recent sales
    List<Sale> findTop5ByOrderBySaleDateDesc(); // Spring Data JPA derived query
}