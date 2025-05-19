package com.yourstore.app.backend.model.dto;

import com.yourstore.app.backend.model.enums.RepairStatus;
import javax.validation.constraints.*; // For Spring Boot 2.x
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class RepairJobDto {
    private Long id;

    @NotBlank(message = "Customer name is required.")
    @Size(max = 100)
    private String customerName;

    @Size(max = 20)
    private String customerPhone;

    @Email(message = "Invalid email format.")
    @Size(max = 100)
    private String customerEmail;

    @NotBlank(message = "Item type is required.")
    @Size(max = 100)
    private String itemType;

    @Size(max = 50)
    private String itemBrand;
    @Size(max = 100)
    private String itemModel;
    @Size(max = 50)
    private String itemSerialNumber;

    @NotBlank(message = "Reported issue is required.")
    private String reportedIssue;

    private String technicianNotes;
    private RepairStatus status;
    private String assignedToUsername; // Username of assigned technician
    private Long assignedToUserId;

    @DecimalMin(value = "0.0", inclusive = true, message = "Estimated cost cannot be negative.")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal estimatedCost;

    @DecimalMin(value = "0.0", inclusive = true, message = "Actual cost cannot be negative.")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal actualCost;

    private LocalDateTime dateReceived;
    private LocalDate estimatedCompletionDate;
    private LocalDateTime dateCompleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors, Getters, Setters (generate all)
    public RepairJobDto() {}

    // Example full constructor
    public RepairJobDto(Long id, String customerName, String customerPhone, String customerEmail, String itemType, String itemBrand, String itemModel, String itemSerialNumber, String reportedIssue, String technicianNotes, RepairStatus status, String assignedToUsername, Long assignedToUserId, BigDecimal estimatedCost, BigDecimal actualCost, LocalDateTime dateReceived, LocalDate estimatedCompletionDate, LocalDateTime dateCompleted, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerEmail = customerEmail;
        this.itemType = itemType;
        this.itemBrand = itemBrand;
        this.itemModel = itemModel;
        this.itemSerialNumber = itemSerialNumber;
        this.reportedIssue = reportedIssue;
        this.technicianNotes = technicianNotes;
        this.status = status;
        this.assignedToUsername = assignedToUsername;
        this.assignedToUserId = assignedToUserId;
        this.estimatedCost = estimatedCost;
        this.actualCost = actualCost;
        this.dateReceived = dateReceived;
        this.estimatedCompletionDate = estimatedCompletionDate;
        this.dateCompleted = dateCompleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getItemBrand() { return itemBrand; }
    public void setItemBrand(String itemBrand) { this.itemBrand = itemBrand; }
    public String getItemModel() { return itemModel; }
    public void setItemModel(String itemModel) { this.itemModel = itemModel; }
    public String getItemSerialNumber() { return itemSerialNumber; }
    public void setItemSerialNumber(String itemSerialNumber) { this.itemSerialNumber = itemSerialNumber; }
    public String getReportedIssue() { return reportedIssue; }
    public void setReportedIssue(String reportedIssue) { this.reportedIssue = reportedIssue; }
    public String getTechnicianNotes() { return technicianNotes; }
    public void setTechnicianNotes(String technicianNotes) { this.technicianNotes = technicianNotes; }
    public RepairStatus getStatus() { return status; }
    public void setStatus(RepairStatus status) { this.status = status; }
    public String getAssignedToUsername() { return assignedToUsername; }
    public void setAssignedToUsername(String assignedToUsername) { this.assignedToUsername = assignedToUsername; }
    public Long getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(Long assignedToUserId) { this.assignedToUserId = assignedToUserId; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }
    public BigDecimal getActualCost() { return actualCost; }
    public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }
    public LocalDateTime getDateReceived() { return dateReceived; }
    public void setDateReceived(LocalDateTime dateReceived) { this.dateReceived = dateReceived; }
    public LocalDate getEstimatedCompletionDate() { return estimatedCompletionDate; }
    public void setEstimatedCompletionDate(LocalDate estimatedCompletionDate) { this.estimatedCompletionDate = estimatedCompletionDate; }
    public LocalDateTime getDateCompleted() { return dateCompleted; }
    public void setDateCompleted(LocalDateTime dateCompleted) { this.dateCompleted = dateCompleted; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}