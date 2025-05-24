package com.yourstore.app.backend.repository;

import com.yourstore.app.backend.model.entity.Sale;
import com.yourstore.app.backend.model.enums.ProductCategory; // Kept if used by findRevenuePerCategory
import org.springframework.data.domain.Pageable; // <<< CORRECTED: IMPORT ADDED
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
    BigDecimal findTotalSalesRevenue(); // This was an old one, may or may not be used by dashboard now

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

    // --- Methods added for enhanced dashboard ---
    @Query("SELECT SUM(s.totalAmount) FROM Sale s WHERE s.saleDate BETWEEN :startDate AND :endDate")
    BigDecimal findTotalSalesBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT si.product.category, SUM(si.subtotal) as revenuePerCategory " +
           "FROM Sale s JOIN s.items si " +
           "GROUP BY si.product.category ORDER BY revenuePerCategory DESC")
    List<Object[]> findRevenuePerCategory();

    @Query("SELECT si.product.name, SUM(si.quantity) as totalQuantitySold " +
           "FROM SaleItem si GROUP BY si.product.name ORDER BY totalQuantitySold DESC")
    List<Object[]> findTopSellingProductsByQuantity(Pageable pageable); // <<< Pageable is now resolved

    @Query("SELECT si.product.name, SUM(si.subtotal) as totalRevenueGenerated " +
           "FROM SaleItem si GROUP BY si.product.name ORDER BY totalRevenueGenerated DESC")
    List<Object[]> findTopSellingProductsByRevenue(Pageable pageable); // <<< Pageable is now resolved

    @EntityGraph(attributePaths = {"items", "items.product", "user"}) // Ensure related data is fetched
    List<Sale> findBySaleDateBetweenOrderBySaleDateDesc(LocalDateTime startDate, LocalDateTime endDate);
}