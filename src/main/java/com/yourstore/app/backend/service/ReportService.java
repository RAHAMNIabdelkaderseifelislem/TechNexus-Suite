// src/main/java/com/yourstore/app/backend/service/ReportService.java
package com.yourstore.app.backend.service;

import com.yourstore.app.backend.mapper.SaleMapper;
import com.yourstore.app.backend.model.dto.SaleDto;
// Ensure these DTOs are in the correct backend package
import com.yourstore.app.backend.model.dto.reports.ProfitLossReportDto;   // <<< CORRECTED: Import backend DTO
import com.yourstore.app.backend.model.dto.reports.SalesByProductDto;   // <<< CORRECTED: Import backend DTO
import com.yourstore.app.backend.model.dto.reports.StockReportItemDto;
import com.yourstore.app.backend.model.entity.Product;
import com.yourstore.app.backend.repository.ProductRepository;
import com.yourstore.app.backend.repository.PurchaseRepository;
import com.yourstore.app.backend.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final SaleMapper saleMapper;

    @Autowired
    public ReportService(SaleRepository saleRepository, ProductRepository productRepository,
                         PurchaseRepository purchaseRepository, SaleMapper saleMapper) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
        this.saleMapper = saleMapper;
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
        BigDecimal totalSales = saleRepository.findTotalSalesBetweenDates(startDate, endDate);
        BigDecimal totalPurchases = purchaseRepository.findTotalPurchasesBetweenDates(startDate, endDate);

        // Handle nulls from repository if no sales/purchases in period
        if (totalSales == null) totalSales = BigDecimal.ZERO;
        if (totalPurchases == null) totalPurchases = BigDecimal.ZERO;

        // Use the DTO from com.yourstore.app.backend.model.dto.reports
        ProfitLossReportDto report = new ProfitLossReportDto();
        report.setStartDate(startDate);                     // <<< CORRECTED
        report.setEndDate(endDate);                         // <<< CORRECTED
        report.setTotalRevenue(totalSales);                 // <<< CORRECTED
        report.setTotalCostOfGoodsOrPurchases(totalPurchases); // <<< CORRECTED
        report.setGrossProfit(totalSales.subtract(totalPurchases)); // <<< CORRECTED
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