package com.yourstore.app.backend.service;

import com.yourstore.app.backend.exception.ResourceNotFoundException;
import com.yourstore.app.backend.mapper.RepairJobMapper;
import com.yourstore.app.backend.model.dto.RepairJobDto;
import com.yourstore.app.backend.model.entity.RepairJob;
import com.yourstore.app.backend.model.entity.User;
import com.yourstore.app.backend.model.enums.RepairStatus;
import com.yourstore.app.backend.repository.RepairJobRepository;
import com.yourstore.app.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RepairJobService {
    private static final Logger logger = LoggerFactory.getLogger(RepairJobService.class);

    private final RepairJobRepository repairJobRepository;
    private final UserRepository userRepository;
    private final RepairJobMapper repairJobMapper;

    @Autowired
    public RepairJobService(RepairJobRepository repairJobRepository, UserRepository userRepository, RepairJobMapper repairJobMapper) {
        this.repairJobRepository = repairJobRepository;
        this.userRepository = userRepository;
        this.repairJobMapper = repairJobMapper;
    }

    @Transactional
    public RepairJobDto createRepairJob(RepairJobDto repairJobDto) {
        logger.info("Creating new repair job for customer: {}", repairJobDto.getCustomerName());
        User assignedUser = null;
        if (repairJobDto.getAssignedToUserId() != null) {
            assignedUser = userRepository.findById(repairJobDto.getAssignedToUserId())
                    .orElse(null); // Or throw if user must exist
        } else if (repairJobDto.getAssignedToUsername() != null && !repairJobDto.getAssignedToUsername().isBlank()){
             assignedUser = userRepository.findByUsername(repairJobDto.getAssignedToUsername())
                    .orElse(null);
        }


        RepairJob repairJob = repairJobMapper.toEntity(repairJobDto, assignedUser);
        repairJob.setDateReceived(LocalDateTime.now()); // System set
        repairJob.setStatus(RepairStatus.PENDING_ASSESSMENT); // Initial status

        // Set user who created the record (logged in user) if not assigned
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String creatorUsername = (principal instanceof UserDetails) ? ((UserDetails)principal).getUsername() : principal.toString();
        // This field is not explicitly in RepairJob entity, but assignedToUser could be current user if no one else is assigned
        // If you add a 'createdByUserId' field to RepairJob, set it here.

        RepairJob savedJob = repairJobRepository.save(repairJob);
        logger.info("Repair job created with ID: {}", savedJob.getId());
        return repairJobMapper.toDto(savedJob);
    }

    @Transactional(readOnly = true)
    public List<RepairJobDto> getAllRepairJobs() {
        logger.info("Fetching all repair jobs.");
        return repairJobRepository.findAll().stream()
                .map(repairJobMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RepairJobDto getRepairJobById(Long id) {
        logger.info("Fetching repair job by ID: {}", id);
        RepairJob job = repairJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repair job not found with ID: " + id));
        return repairJobMapper.toDto(job);
    }

    @Transactional
    public RepairJobDto updateRepairJob(Long id, RepairJobDto repairJobDto) {
        logger.info("Updating repair job ID: {}", id);
        RepairJob existingJob = repairJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repair job not found with ID: " + id));

        User assignedUser = null;
        if (repairJobDto.getAssignedToUserId() != null) {
            assignedUser = userRepository.findById(repairJobDto.getAssignedToUserId()).orElse(null);
        } else if (repairJobDto.getAssignedToUsername() != null && !repairJobDto.getAssignedToUsername().isBlank()){
             assignedUser = userRepository.findByUsername(repairJobDto.getAssignedToUsername()).orElse(null);
        }


        repairJobMapper.updateEntityFromDto(repairJobDto, existingJob, assignedUser);
        RepairJob updatedJob = repairJobRepository.save(existingJob);
        logger.info("Repair job ID: {} updated successfully.", updatedJob.getId());
        return repairJobMapper.toDto(updatedJob);
    }

    // Delete method can be added if needed
}