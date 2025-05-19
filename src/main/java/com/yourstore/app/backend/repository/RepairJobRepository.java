package com.yourstore.app.backend.repository;

import com.yourstore.app.backend.model.entity.RepairJob;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepairJobRepository extends JpaRepository<RepairJob, Long> {
    @Override
    @EntityGraph(attributePaths = {"assignedToUser"}) // Eager fetch assigned user
    List<RepairJob> findAll();

    @Override
    @EntityGraph(attributePaths = {"assignedToUser"})
    Optional<RepairJob> findById(Long id);

    // Find by status, customer name, etc. can be added later
}