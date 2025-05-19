package com.yourstore.app.backend.repository;

import com.yourstore.app.backend.model.entity.Product;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // JpaRepository provides: save, findById, findAll, deleteById, etc.

    // Custom queries can be added here if needed, e.g.:
    // List<Product> findByCategory(ProductCategory category);
    // List<Product> findByNameContainingIgnoreCase(String name);
    // In ProductRepository.java
    @Query("SELECT COUNT(p.id) FROM Product p WHERE p.quantityInStock < :threshold")
    Long countLowStockItems(@Param("threshold") int threshold);
    // Default threshold could be e.g., 5 or a configurable value

    List<Product> findByQuantityInStockLessThan(int threshold, Pageable pageable);
}