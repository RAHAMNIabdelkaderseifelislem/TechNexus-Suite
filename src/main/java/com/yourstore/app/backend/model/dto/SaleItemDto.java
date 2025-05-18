package com.yourstore.app.backend.model.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class SaleItemDto {
    private Long id;

    @NotNull(message = "Product ID for sale item cannot be null")
    private Long productId;
    private String productName;
    @Min(value = 1, message = "Quantity for sale item must be at least 1")
    private int quantity;
    private BigDecimal priceAtSale;
    private BigDecimal subtotal;

    // Constructors, Getters, Setters
    public SaleItemDto() {}

    public SaleItemDto(Long id, Long productId, String productName, int quantity, BigDecimal priceAtSale, BigDecimal subtotal) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.priceAtSale = priceAtSale;
        this.subtotal = subtotal;
    }
    // Standard Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getPriceAtSale() { return priceAtSale; }
    public void setPriceAtSale(BigDecimal priceAtSale) { this.priceAtSale = priceAtSale; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}