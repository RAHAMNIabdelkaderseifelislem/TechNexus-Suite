package com.yourstore.app.backend.model.dto.reports;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProfitLossReportDto {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal totalSalesRevenue;       // Revenue from product sales
    private BigDecimal totalRepairRevenue;      // Revenue from repairs
    private BigDecimal totalRevenue;            // Combined: Sales + Repairs
    private BigDecimal totalCostOfGoodsOrPurchases; // Cost of items purchased (simple COGS for now)
    // private BigDecimal totalRepairCosts;     // Future: Cost of performing repairs (parts, specific labor)
    private BigDecimal grossProfit;

    // Getters and Setters for all fields
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public BigDecimal getTotalSalesRevenue() { return totalSalesRevenue; }
    public void setTotalSalesRevenue(BigDecimal totalSalesRevenue) { this.totalSalesRevenue = totalSalesRevenue; }
    public BigDecimal getTotalRepairRevenue() { return totalRepairRevenue; }
    public void setTotalRepairRevenue(BigDecimal totalRepairRevenue) { this.totalRepairRevenue = totalRepairRevenue; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public BigDecimal getTotalCostOfGoodsOrPurchases() { return totalCostOfGoodsOrPurchases; }
    public void setTotalCostOfGoodsOrPurchases(BigDecimal totalCostOfGoodsOrPurchases) { this.totalCostOfGoodsOrPurchases = totalCostOfGoodsOrPurchases; }
    public BigDecimal getGrossProfit() { return grossProfit; }
    public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }
}