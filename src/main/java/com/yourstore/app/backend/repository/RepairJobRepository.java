package com.yourstore.app.backend.repository;

import com.yourstore.app.backend.model.entity.RepairJob;
import com.yourstore.app.backend.model.enums.RepairStatus; // Import RepairStatus
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; // Import Param
import org.springframework.stereotype.Repository;

import java.util.Collection; // Import Collection
import java.util.List;
import java.util.Optional;

@Repository
public interface RepairJobRepository extends JpaRepository<RepairJob, Long> {
    @Override
    @EntityGraph(attributePaths = {"assignedToUser"})
    List<RepairJob> findAll();

    @Override
    @EntityGraph(attributePaths = {"assignedToUser"})
    Optional<RepairJob> findById(Long id);

    // Corrected query for counting pending repairs
    @Query("SELECT COUNT(rj.id) FROM RepairJob rj WHERE rj.status NOT IN :excludedStatuses")
    Long countByStatusNotIn(@Param("excludedStatuses") Collection<RepairStatus> excludedStatuses);
    
    // You can keep the old method name if you prefer and just change the query and parameters
    // default Long countPendingRepairs() {
    //     return countByStatusNotIn(List.of(
    //             RepairStatus.COMPLETED_PAID,
    //             RepairStatus.COMPLETED_UNPAID,
    //             RepairStatus.CANCELLED_BY_CUSTOMER,
    //             RepairStatus.CANCELLED_BY_STORE,
    //             RepairStatus.UNREPAIRABLE
    //     ));
    // }
}