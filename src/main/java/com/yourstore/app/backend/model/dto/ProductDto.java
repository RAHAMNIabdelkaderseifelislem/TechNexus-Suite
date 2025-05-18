package com.yourstore.app.backend.model.dto;

import com.yourstore.app.backend.model.enums.ProductCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private ProductCategory category;
    private String supplier;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private int quantityInStock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors, Getters, Setters

    public ProductDto() {}

    public ProductDto(Long id, String name, String description, ProductCategory category, String supplier, BigDecimal purchasePrice, BigDecimal sellingPrice, int quantityInStock, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.supplier = supplier;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.quantityInStock = quantityInStock;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    // Standard Getters and Setters for all fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public int getQuantityInStock() { return quantityInStock; }
    public void setQuantityInStock(int quantityInStock) { this.quantityInStock = quantityInStock; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}