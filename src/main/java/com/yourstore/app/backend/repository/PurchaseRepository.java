package com.yourstore.app.backend.repository;

import com.yourstore.app.backend.model.entity.Purchase;
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
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    @Override
    @EntityGraph(attributePaths = {"items", "items.product", "user"})
    List<Purchase> findAll();

    @Override
    @EntityGraph(attributePaths = {"items", "items.product", "user"})
    Optional<Purchase> findById(Long id);

    @Query("SELECT SUM(p.totalAmount) FROM Purchase p WHERE p.purchaseDate BETWEEN :startDate AND :endDate")
    BigDecimal findTotalPurchasesBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT FUNCTION('DATE', p.purchaseDate) as purchaseDay, SUM(p.totalAmount) as dailyTotal " +
           "FROM Purchase p " +
           "WHERE p.purchaseDate BETWEEN :startDate AND :endDate " +
           "GROUP BY FUNCTION('DATE', p.purchaseDate) " +
           "ORDER BY purchaseDay ASC")
    List<Object[]> findDailyPurchaseTotalsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}