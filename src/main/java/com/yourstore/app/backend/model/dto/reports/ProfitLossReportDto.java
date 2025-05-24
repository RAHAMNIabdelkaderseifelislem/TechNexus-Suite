package com.yourstore.app.backend.model.dto.reports;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProfitLossReportDto {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal totalRevenue;
    private BigDecimal totalCostOfGoodsOrPurchases;
    private BigDecimal grossProfit;
    // Getters & Setters
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public BigDecimal getTotalCostOfGoodsOrPurchases() { return totalCostOfGoodsOrPurchases; }
    public void setTotalCostOfGoodsOrPurchases(BigDecimal totalCostOfGoodsOrPurchases) { this.totalCostOfGoodsOrPurchases = totalCostOfGoodsOrPurchases; }
    public BigDecimal getGrossProfit() { return grossProfit; }
    public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }
}
