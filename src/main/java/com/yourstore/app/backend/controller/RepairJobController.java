package com.yourstore.app.backend.controller;

import com.yourstore.app.backend.model.dto.RepairJobDto;
import com.yourstore.app.backend.service.RepairJobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/repairs")
public class RepairJobController {

    private final RepairJobService repairJobService;

    @Autowired
    public RepairJobController(RepairJobService repairJobService) {
        this.repairJobService = repairJobService;
    }

    @PostMapping
    public ResponseEntity<RepairJobDto> createRepairJob(@Valid @RequestBody RepairJobDto repairJobDto) {
        RepairJobDto createdJob = repairJobService.createRepairJob(repairJobDto);
        return new ResponseEntity<>(createdJob, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<RepairJobDto>> getAllRepairJobs() {
        List<RepairJobDto> jobs = repairJobService.getAllRepairJobs();
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RepairJobDto> getRepairJobById(@PathVariable Long id) {
        RepairJobDto jobDto = repairJobService.getRepairJobById(id);
        return ResponseEntity.ok(jobDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RepairJobDto> updateRepairJob(@PathVariable Long id, @Valid @RequestBody RepairJobDto repairJobDto) {
        RepairJobDto updatedJob = repairJobService.updateRepairJob(id, repairJobDto);
        return ResponseEntity.ok(updatedJob);
    }
}