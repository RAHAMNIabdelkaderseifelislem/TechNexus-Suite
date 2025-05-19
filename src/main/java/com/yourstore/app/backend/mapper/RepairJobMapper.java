package com.yourstore.app.backend.mapper;

import com.yourstore.app.backend.model.dto.RepairJobDto;
import com.yourstore.app.backend.model.entity.RepairJob;
import com.yourstore.app.backend.model.entity.User; // Import User
import com.yourstore.app.backend.model.enums.RepairStatus;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class RepairJobMapper {

    public RepairJobDto toDto(RepairJob job) {
        if (job == null) return null;
        return new RepairJobDto(
                job.getId(),
                job.getCustomerName(),
                job.getCustomerPhone(),
                job.getCustomerEmail(),
                job.getItemType(),
                job.getItemBrand(),
                job.getItemModel(),
                job.getItemSerialNumber(),
                job.getReportedIssue(),
                job.getTechnicianNotes(),
                job.getStatus(),
                job.getAssignedToUser() != null ? job.getAssignedToUser().getUsername() : null,
                job.getAssignedToUser() != null ? job.getAssignedToUser().getId() : null,
                job.getEstimatedCost(),
                job.getActualCost(),
                job.getDateReceived(),
                job.getEstimatedCompletionDate(),
                job.getDateCompleted(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    public RepairJob toEntity(RepairJobDto dto, User assignedUser) { // Pass User object if ID is used to fetch
        if (dto == null) return null;
        RepairJob job = new RepairJob();
        // ID is not set from DTO for new entity
        job.setCustomerName(dto.getCustomerName());
        job.setCustomerPhone(dto.getCustomerPhone());
        job.setCustomerEmail(dto.getCustomerEmail());
        job.setItemType(dto.getItemType());
        job.setItemBrand(dto.getItemBrand());
        job.setItemModel(dto.getItemModel());
        job.setItemSerialNumber(dto.getItemSerialNumber());
        job.setReportedIssue(dto.getReportedIssue());
        job.setTechnicianNotes(dto.getTechnicianNotes()); // Can be set during creation or update
        job.setStatus(dto.getStatus() != null ? dto.getStatus() : RepairStatus.PENDING_ASSESSMENT); // Default status
        job.setAssignedToUser(assignedUser); // User object
        job.setEstimatedCost(dto.getEstimatedCost());
        job.setActualCost(dto.getActualCost()); // Usually set later
        if (dto.getDateReceived() != null) job.setDateReceived(dto.getDateReceived()); // Usually set by system
        job.setEstimatedCompletionDate(dto.getEstimatedCompletionDate());
        job.setDateCompleted(dto.getDateCompleted()); // Usually set later
        // Auditable fields (createdAt, updatedAt) handled by JPA
        return job;
    }

    public void updateEntityFromDto(RepairJobDto dto, RepairJob job, User assignedUser) {
        if (dto == null || job == null) return;

        job.setCustomerName(dto.getCustomerName());
        job.setCustomerPhone(dto.getCustomerPhone());
        job.setCustomerEmail(dto.getCustomerEmail());
        job.setItemType(dto.getItemType());
        job.setItemBrand(dto.getItemBrand());
        job.setItemModel(dto.getItemModel());
        job.setItemSerialNumber(dto.getItemSerialNumber());
        job.setReportedIssue(dto.getReportedIssue());
        job.setTechnicianNotes(dto.getTechnicianNotes());
        if (dto.getStatus() != null) job.setStatus(dto.getStatus());
        if (assignedUser != null || dto.getAssignedToUserId() == null) { // Allow unassigning by passing null user
            job.setAssignedToUser(assignedUser);
        }
        job.setEstimatedCost(dto.getEstimatedCost());
        job.setActualCost(dto.getActualCost());
        job.setEstimatedCompletionDate(dto.getEstimatedCompletionDate());
        if (dto.getStatus() == RepairStatus.COMPLETED_PAID || dto.getStatus() == RepairStatus.COMPLETED_UNPAID) {
             if (job.getDateCompleted() == null) job.setDateCompleted(LocalDateTime.now());
        } else {
            job.setDateCompleted(dto.getDateCompleted()); // Allow manual setting if not auto
        }
    }
}