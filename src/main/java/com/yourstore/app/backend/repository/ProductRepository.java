package com.yourstore.app.backend.repository;

import com.yourstore.app.backend.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // JpaRepository provides: save, findById, findAll, deleteById, etc.

    // Custom queries can be added here if needed, e.g.:
    // List<Product> findByCategory(ProductCategory category);
    // List<Product> findByNameContainingIgnoreCase(String name);
}