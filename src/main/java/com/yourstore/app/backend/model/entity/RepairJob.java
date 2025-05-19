package com.yourstore.app.backend.model.entity;

import com.yourstore.app.backend.model.entity.base.Auditable;
import com.yourstore.app.backend.model.enums.RepairStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate; // For estimated completion
import java.time.LocalDateTime;

@Entity
@Table(name = "repair_jobs")
public class RepairJob extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Customer Information
    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    // Item Information
    @Column(name = "item_type", length = 100) // e.g., Laptop, Desktop, Printer
    private String itemType;

    @Column(name = "item_brand", length = 50)
    private String itemBrand;

    @Column(name = "item_model", length = 100)
    private String itemModel;

    @Column(name = "item_serial_number", length = 50)
    private String itemSerialNumber;

    @Lob
    @Column(name = "reported_issue", nullable = false)
    private String reportedIssue;

    @Lob
    @Column(name = "technician_notes")
    private String technicianNotes; // Notes from the technician working on it

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RepairStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id") // User (technician) assigned to the job
    private User assignedToUser;

    @Column(name = "estimated_cost", precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 10, scale = 2)
    private BigDecimal actualCost; // Includes parts and labor

    @Column(name = "date_received", nullable = false)
    private LocalDateTime dateReceived;

    @Column(name = "estimated_completion_date")
    private LocalDate estimatedCompletionDate;

    @Column(name = "date_completed")
    private LocalDateTime dateCompleted;

    // Placeholder for linking to parts used from inventory - to be a separate entity list
    // @OneToMany(...)
    // private List<RepairItemUsed> partsUsed;

    public RepairJob() {
        this.dateReceived = LocalDateTime.now();
        this.status = RepairStatus.PENDING_ASSESSMENT;
    }

    // Getters and Setters (Generate all)
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
    public User getAssignedToUser() { return assignedToUser; }
    public void setAssignedToUser(User assignedToUser) { this.assignedToUser = assignedToUser; }
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
}