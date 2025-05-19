package com.yourstore.app.backend.repository;

import com.yourstore.app.backend.model.entity.RepairJob;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    @Query("SELECT COUNT(rj.id) FROM RepairJob rj WHERE rj.status NOT IN (com.yourstore.app.backend.model.enums.RepairStatus.COMPLETED_PAID, com.yourstore.app.backend.model.enums.RepairStatus.COMPLETED_UNPAID, com.yourstore.app.backend.model.enums.RepairStatus.CANCELLED_BY_CUSTOMER, com.yourstore.app.backend.model.enums.RepairStatus.CANCELLED_BY_STORE, com.yourstore.app.backend.model.enums.RepairStatus.UNREPAIRABLE))")
    Long countPendingRepairs();
}