package com.yourstore.app.backend.repository;

import com.yourstore.app.backend.model.entity.Purchase;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
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
}