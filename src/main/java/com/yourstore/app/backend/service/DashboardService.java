// src/main/java/com/yourstore/app/backend/service/DashboardService.java
package com.yourstore.app.backend.service;

import com.yourstore.app.backend.mapper.ProductMapper;
import com.yourstore.app.backend.mapper.RepairJobMapper;
import com.yourstore.app.backend.mapper.SaleMapper;
import com.yourstore.app.backend.model.dto.ProductDto;
import com.yourstore.app.backend.model.dto.RepairJobDto;
import com.yourstore.app.backend.model.dto.SaleDto;
import com.yourstore.app.backend.model.enums.ProductCategory;
import com.yourstore.app.backend.model.enums.RepairStatus;
import com.yourstore.app.backend.repository.ProductRepository;
import com.yourstore.app.backend.repository.PurchaseRepository; // Import PurchaseRepository
import com.yourstore.app.backend.repository.RepairJobRepository;
import com.yourstore.app.backend.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final RepairJobRepository repairJobRepository;
    private final PurchaseRepository purchaseRepository; // Added
    private final ProductMapper productMapper;
    private final SaleMapper saleMapper;
    private final RepairJobMapper repairJobMapper;

    private static final List<RepairStatus> COMPLETED_REPAIR_STATUSES = Arrays.asList(
        RepairStatus.COMPLETED_PAID,
        RepairStatus.COMPLETED_UNPAID,
        RepairStatus.READY_FOR_PICKUP // Assuming 'Ready for Pickup' means work is done and billable
    );

    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int MAX_LIST_ITEMS = 5;
    private static final int TOP_PRODUCTS_COUNT = 5; // For top selling products chart

    @Autowired
    public DashboardService(ProductRepository productRepository, SaleRepository saleRepository,
                            RepairJobRepository repairJobRepository, PurchaseRepository purchaseRepository, // Added
                            ProductMapper productMapper, SaleMapper saleMapper, RepairJobMapper repairJobMapper) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.repairJobRepository = repairJobRepository;
        this.purchaseRepository = purchaseRepository; // Added
        this.productMapper = productMapper;
        this.saleMapper = saleMapper;
        this.repairJobMapper = repairJobMapper;
    }

    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        Pageable listLimit = PageRequest.of(0, MAX_LIST_ITEMS);
        Pageable topProductsLimit = PageRequest.of(0, TOP_PRODUCTS_COUNT);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfToday = now.with(LocalTime.MAX);

        // Time Periods
        LocalDateTime startOfLast7Days = now.minusDays(6).with(LocalTime.MIN);
        LocalDateTime startOfLast30Days = now.minusDays(29).with(LocalTime.MIN); // Approx. last month

        // Metric Cards
        metrics.put("totalProducts", productRepository.count());
        BigDecimal todaysRevenue = saleRepository.findTodaysSalesRevenue();
        metrics.put("todaysSalesRevenue", todaysRevenue != null ? todaysRevenue : BigDecimal.ZERO);
        metrics.put("lowStockItemsCount", productRepository.countLowStockItems(LOW_STOCK_THRESHOLD));
        List<RepairStatus> excludedRepairStatuses = Arrays.asList(
                RepairStatus.COMPLETED_PAID, RepairStatus.COMPLETED_UNPAID,
                RepairStatus.CANCELLED_BY_CUSTOMER, RepairStatus.CANCELLED_BY_STORE, RepairStatus.UNREPAIRABLE);
        metrics.put("pendingRepairsCount", repairJobRepository.countByStatusNotIn(excludedRepairStatuses));

        // Financial Summaries
        BigDecimal salesLast7Days = Optional.ofNullable(saleRepository.findTotalSalesBetweenDates(startOfLast7Days, endOfToday)).orElse(BigDecimal.ZERO);
        BigDecimal purchasesLast7Days = Optional.ofNullable(purchaseRepository.findTotalPurchasesBetweenDates(startOfLast7Days, endOfToday)).orElse(BigDecimal.ZERO);
        BigDecimal repairRevenueLast7Days = Optional.ofNullable(
            repairJobRepository.findTotalActualCostByDateCompletedBetweenAndStatusIn(startOfLast7Days, endOfToday, COMPLETED_REPAIR_STATUSES)
        ).orElse(BigDecimal.ZERO);

        metrics.put("salesLast7Days", salesLast7Days);
        metrics.put("purchasesLast7Days", purchasesLast7Days);
        metrics.put("repairRevenueLast7Days", repairRevenueLast7Days); // Store it separately for potential display
        // Overall Profit = (Sales + Repair Revenue) - Purchases
        metrics.put("profitLast7Days", (salesLast7Days.add(repairRevenueLast7Days)).subtract(purchasesLast7Days));


        BigDecimal salesLast30Days = Optional.ofNullable(saleRepository.findTotalSalesBetweenDates(startOfLast30Days, endOfToday)).orElse(BigDecimal.ZERO);
        BigDecimal purchasesLast30Days = Optional.ofNullable(purchaseRepository.findTotalPurchasesBetweenDates(startOfLast30Days, endOfToday)).orElse(BigDecimal.ZERO);
        BigDecimal repairRevenueLast30Days = Optional.ofNullable(
            repairJobRepository.findTotalActualCostByDateCompletedBetweenAndStatusIn(startOfLast30Days, endOfToday, COMPLETED_REPAIR_STATUSES)
        ).orElse(BigDecimal.ZERO);

        metrics.put("salesLast30Days", salesLast30Days);
        metrics.put("purchasesLast30Days", purchasesLast30Days);
        metrics.put("repairRevenueLast30Days", repairRevenueLast30Days);
        metrics.put("profitLast30Days", (salesLast30Days.add(repairRevenueLast30Days)).subtract(purchasesLast30Days));


        // Weekly Performance Chart Data (Sales, Purchases, Repair Revenue, Profit)
        List<Object[]> dailySalesData = saleRepository.findDailySalesTotalsBetweenDates(startOfLast7Days, endOfToday);
        List<Object[]> dailyPurchaseData = purchaseRepository.findDailyPurchaseTotalsBetweenDates(startOfLast7Days, endOfToday);
        List<Object[]> dailyRepairRevenueData = repairJobRepository.findDailyRepairRevenueBetweenDatesAndStatusIn(startOfLast7Days, endOfToday, COMPLETED_REPAIR_STATUSES);

        Map<String, BigDecimal> weeklySalesMap = new LinkedHashMap<>();
        Map<String, BigDecimal> weeklyPurchasesMap = new LinkedHashMap<>();
        Map<String, BigDecimal> weeklyRepairRevenueMap = new LinkedHashMap<>(); // New map
        Map<String, BigDecimal> weeklyProfitMap = new LinkedHashMap<>();

        for (int i = 6; i >= 0; i--) {
            String dayKey = LocalDate.now().minusDays(i).getDayOfWeek().toString().substring(0, 3).toUpperCase();
            weeklySalesMap.put(dayKey, BigDecimal.ZERO);
            weeklyPurchasesMap.put(dayKey, BigDecimal.ZERO);
            weeklyRepairRevenueMap.put(dayKey, BigDecimal.ZERO); // Initialize
            weeklyProfitMap.put(dayKey, BigDecimal.ZERO);
        }

        dailySalesData.forEach(row -> {
            LocalDate day = ((java.sql.Date) row[0]).toLocalDate();
            weeklySalesMap.put(day.getDayOfWeek().toString().substring(0, 3).toUpperCase(), (BigDecimal) row[1]);
        });
        dailyPurchaseData.forEach(row -> {
            LocalDate day = ((java.sql.Date) row[0]).toLocalDate();
            weeklyPurchasesMap.put(day.getDayOfWeek().toString().substring(0, 3).toUpperCase(), (BigDecimal) row[1]);
        });
        dailyRepairRevenueData.forEach(row -> { // Populate repair revenue
            LocalDate day = ((java.sql.Date) row[0]).toLocalDate();
            weeklyRepairRevenueMap.put(day.getDayOfWeek().toString().substring(0, 3).toUpperCase(), (BigDecimal) row[1]);
        });

        // Recalculate profit including repair revenue
        weeklySalesMap.forEach((day, sales) -> {
            BigDecimal purchases = weeklyPurchasesMap.getOrDefault(day, BigDecimal.ZERO);
            BigDecimal repairRevenue = weeklyRepairRevenueMap.getOrDefault(day, BigDecimal.ZERO);
            weeklyProfitMap.put(day, (sales.add(repairRevenue)).subtract(purchases));
        });

        metrics.put("weeklySalesChartData", weeklySalesMap);
        metrics.put("weeklyPurchasesChartData", weeklyPurchasesMap);
        metrics.put("weeklyRepairRevenueChartData", weeklyRepairRevenueMap); // Send this to frontend
        metrics.put("weeklyProfitChartData", weeklyProfitMap);

        // Sales by Category (Pie Chart - by Revenue)
        List<Object[]> revenueCategoryData = saleRepository.findRevenuePerCategory();
        Map<String, Number> salesByCategoryChartData = new LinkedHashMap<>();
        revenueCategoryData.stream().limit(5).forEach(row -> { // Top 5 categories
            ProductCategory category = (ProductCategory) row[0];
            BigDecimal revenue = (BigDecimal) row[1];
            salesByCategoryChartData.put(category.toString(), revenue);
        });
        if (revenueCategoryData.size() > 5) {
            BigDecimal otherRevenue = revenueCategoryData.stream().skip(5)
                                    .map(row -> (BigDecimal) row[1])
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (otherRevenue.compareTo(BigDecimal.ZERO) > 0) {
               salesByCategoryChartData.put("OTHER", otherRevenue);
            }
        }
        metrics.put("salesByCategoryChartData", salesByCategoryChartData);

        // Top Selling Products (by Quantity)
        List<Object[]> topProductsQtyData = saleRepository.findTopSellingProductsByQuantity(topProductsLimit);
        Map<String, Number> topSellingProductsQtyChartData = new LinkedHashMap<>();
        topProductsQtyData.forEach(row -> topSellingProductsQtyChartData.put((String) row[0], (Number) row[1]));
        metrics.put("topSellingProductsByQtyChartData", topSellingProductsQtyChartData);
        
        // Top Selling Products (by Revenue)
        List<Object[]> topProductsRevData = saleRepository.findTopSellingProductsByRevenue(topProductsLimit);
        Map<String, Number> topSellingProductsRevChartData = new LinkedHashMap<>();
        topProductsRevData.forEach(row -> topSellingProductsRevChartData.put((String) row[0], (Number) row[1]));
        metrics.put("topSellingProductsByRevChartData", topSellingProductsRevChartData);


        // Existing Lists
        metrics.put("latestRepairJobs", repairJobRepository.findByStatusNotInOrderByDateReceivedDesc(excludedRepairStatuses, listLimit)
                .stream().map(repairJobMapper::toDto).collect(Collectors.toList()));
        metrics.put("recentSales", saleRepository.findTop5ByOrderBySaleDateDesc()
                .stream().map(saleMapper::toDto).collect(Collectors.toList()));
        metrics.put("lowStockAlerts", productRepository.findByQuantityInStockLessThan(LOW_STOCK_THRESHOLD, listLimit)
                .stream().map(productMapper::toDto).collect(Collectors.toList()));
        metrics.put("lowStockThreshold", LOW_STOCK_THRESHOLD);

        return metrics;
    }
}