// src/main/java/com/yourstore/app/backend/service/ReportService.java
package com.yourstore.app.backend.service;

import com.yourstore.app.backend.mapper.SaleMapper;
import com.yourstore.app.backend.model.dto.SaleDto;
// Ensure these DTOs are in the correct backend package
import com.yourstore.app.backend.model.dto.reports.ProfitLossReportDto;   // <<< CORRECTED: Import backend DTO
import com.yourstore.app.backend.model.dto.reports.SalesByProductDto;   // <<< CORRECTED: Import backend DTO
import com.yourstore.app.backend.model.dto.reports.StockReportItemDto;
import com.yourstore.app.backend.model.entity.Product;
import com.yourstore.app.backend.model.enums.RepairStatus;
import com.yourstore.app.backend.repository.ProductRepository;
import com.yourstore.app.backend.repository.PurchaseRepository;
import com.yourstore.app.backend.repository.RepairJobRepository;
import com.yourstore.app.backend.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final SaleMapper saleMapper;
    private final RepairJobRepository repairJobRepository;

    // Define billable/completed statuses for repairs (can be shared or redefined)
    private static final List<RepairStatus> COMPLETED_REPAIR_STATUSES_FOR_REPORTS = Arrays.asList(
        RepairStatus.COMPLETED_PAID,
        RepairStatus.COMPLETED_UNPAID,
        RepairStatus.READY_FOR_PICKUP
    );

    @Autowired
    public ReportService(SaleRepository saleRepository, ProductRepository productRepository,
                         PurchaseRepository purchaseRepository, SaleMapper saleMapper,
                         RepairJobRepository repairJobRepository) { // Add RepairJobRepository
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.saleMapper = saleMapper;
        this.repairJobRepository = repairJobRepository; // Initialize
    }

    @Transactional(readOnly = true)
    public List<SaleDto> getDetailedSalesReport(LocalDateTime startDate, LocalDateTime endDate) {
        return saleRepository.findBySaleDateBetweenOrderBySaleDateDesc(startDate, endDate)
                .stream()
                .map(saleMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProfitLossReportDto getProfitLossReport(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal totalSales = Optional.ofNullable(saleRepository.findTotalSalesBetweenDates(startDate, endDate)).orElse(BigDecimal.ZERO);
        BigDecimal totalPurchases = Optional.ofNullable(purchaseRepository.findTotalPurchasesBetweenDates(startDate, endDate)).orElse(BigDecimal.ZERO);
        BigDecimal totalRepairRevenue = Optional.ofNullable(
            repairJobRepository.findTotalActualCostByDateCompletedBetweenAndStatusIn(startDate, endDate, COMPLETED_REPAIR_STATUSES_FOR_REPORTS)
        ).orElse(BigDecimal.ZERO);

        ProfitLossReportDto report = new ProfitLossReportDto();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setTotalRevenue(totalSales.add(totalRepairRevenue)); // Combined revenue
        report.setTotalSalesRevenue(totalSales); // Optionally keep separate sales revenue
        report.setTotalRepairRevenue(totalRepairRevenue); // Optionally add a field for this in DTO
        report.setTotalCostOfGoodsOrPurchases(totalPurchases); // This is still just purchase cost

        // Gross Profit = (Sales Revenue + Repair Revenue) - Purchase Costs
        // Note: This doesn't account for the *cost* of repairs (parts, specific labor).
        // If you want a truer profit, you'd need:
        // Profit = (Sales Revenue - COGS_for_Sales) + (Repair Revenue - Cost_of_Repairs) - Other_Operational_Costs
        report.setGrossProfit(report.getTotalRevenue().subtract(report.getTotalCostOfGoodsOrPurchases()));
        return report;
    }
    @Transactional(readOnly = true)
    public List<SalesByProductDto> getSalesByProductReport(LocalDateTime startDate, LocalDateTime endDate) {
        // TODO: Implement actual logic for sales by product.
        // This will involve querying SaleItems, grouping by product, and summing quantities/revenue.
        // Example:
        // List<Object[]> results = saleItemRepository.findSalesByProductBetweenDates(startDate, endDate);
        // return results.stream().map(obj -> new SalesByProductDto(...)).collect(Collectors.toList());
        return List.of(/* mock or placeholder data for now */);
    }

    @Transactional(readOnly = true)
    public List<StockReportItemDto> getCurrentStockReport() {
        List<Product> products = productRepository.findAll();
        return products.stream().map(product -> {
            StockReportItemDto item = new StockReportItemDto();
            item.setProductId(product.getId());
            item.setName(product.getName());
            item.setCategory(product.getCategory());
            item.setSupplier(product.getSupplier());
            item.setPurchasePrice(product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO);
            item.setSellingPrice(product.getSellingPrice() != null ? product.getSellingPrice() : BigDecimal.ZERO);
            item.setQuantityInStock(product.getQuantityInStock());

            BigDecimal purchasePrice = item.getPurchasePrice();
            BigDecimal sellingPrice = item.getSellingPrice();
            BigDecimal quantity = BigDecimal.valueOf(item.getQuantityInStock());

            item.setStockValueByPurchasePrice(purchasePrice.multiply(quantity));
            item.setPotentialRevenueAtSellingPrice(sellingPrice.multiply(quantity));
            return item;
        }).collect(Collectors.toList());
    }
}