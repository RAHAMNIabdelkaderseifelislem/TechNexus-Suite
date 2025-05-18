package com.yourstore.app.backend.model.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import java.math.BigDecimal;

public class PurchaseItemDto {
    private Long id;

    @NotNull(message = "Product ID for purchase item cannot be null")
    private Long productId;
    private String productName; // For display, not for creation request usually

    @Min(value = 1, message = "Quantity for purchase item must be at least 1")
    private int quantity;

    @NotNull(message = "Cost price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Cost price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Cost price format invalid")
    private BigDecimal costPrice;
    private BigDecimal subtotal; // Calculated

    public PurchaseItemDto() {}

    public PurchaseItemDto(Long id, Long productId, String productName, int quantity, BigDecimal costPrice, BigDecimal subtotal) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.costPrice = costPrice;
        this.subtotal = subtotal;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}