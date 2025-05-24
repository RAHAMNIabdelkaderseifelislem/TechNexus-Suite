package com.yourstore.app.backend.model.dto.reports;

import com.yourstore.app.backend.model.enums.ProductCategory;
import java.math.BigDecimal;

public class StockReportItemDto {
    private Long productId;
    private String name;
    private ProductCategory category;
    private String supplier;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private int quantityInStock;
    private BigDecimal stockValueByPurchasePrice; // Qty * PurchasePrice
    private BigDecimal potentialRevenueAtSellingPrice; // Qty * SellingPrice

    // Constructors, Getters, Setters
    public StockReportItemDto() {}

    // Getters and Setters...
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
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
    public BigDecimal getStockValueByPurchasePrice() { return stockValueByPurchasePrice; }
    public void setStockValueByPurchasePrice(BigDecimal stockValueByPurchasePrice) { this.stockValueByPurchasePrice = stockValueByPurchasePrice; }
    public BigDecimal getPotentialRevenueAtSellingPrice() { return potentialRevenueAtSellingPrice; }
    public void setPotentialRevenueAtSellingPrice(BigDecimal potentialRevenueAtSellingPrice) { this.potentialRevenueAtSellingPrice = potentialRevenueAtSellingPrice; }
}