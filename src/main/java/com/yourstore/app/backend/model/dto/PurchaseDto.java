package com.yourstore.app.backend.model.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PurchaseDto {
    private Long id;

    @Size(max = 100, message = "Supplier name cannot exceed 100 characters")
    private String supplierName;

    @Size(max = 50, message = "Invoice number cannot exceed 50 characters")
    private String invoiceNumber;

    @NotEmpty(message = "Purchase must contain at least one item")
    @Valid
    private List<PurchaseItemDto> items;

    private BigDecimal totalAmount;
    private LocalDateTime purchaseDate;
    private String username; // User who recorded
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PurchaseDto() {}

    public PurchaseDto(Long id, String supplierName, String invoiceNumber, List<PurchaseItemDto> items, BigDecimal totalAmount, LocalDateTime purchaseDate, String username, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.supplierName = supplierName;
        this.invoiceNumber = invoiceNumber;
        this.items = items;
        this.totalAmount = totalAmount;
        this.purchaseDate = purchaseDate;
        this.username = username;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public List<PurchaseItemDto> getItems() { return items; }
    public void setItems(List<PurchaseItemDto> items) { this.items = items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDateTime purchaseDate) { this.purchaseDate = purchaseDate; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}