package com.yourstore.app.backend.service;

import com.yourstore.app.backend.model.dto.ProductDto; // For low stock list
import com.yourstore.app.backend.model.dto.RepairJobDto; // For recent repairs list
import com.yourstore.app.backend.model.dto.SaleDto; // For recent sales list
import com.yourstore.app.backend.mapper.ProductMapper; // If converting Product to ProductDto
import com.yourstore.app.backend.mapper.RepairJobMapper;
import com.yourstore.app.backend.mapper.SaleMapper;
import com.yourstore.app.backend.model.enums.ProductCategory;
import com.yourstore.app.backend.model.enums.RepairStatus;
import com.yourstore.app.backend.repository.ProductRepository;
import com.yourstore.app.backend.repository.RepairJobRepository;
import com.yourstore.app.backend.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap; // To preserve order for weekly sales chart
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final RepairJobRepository repairJobRepository;
    private final ProductMapper productMapper; // Assuming you have this
    private final SaleMapper saleMapper;
    private final RepairJobMapper repairJobMapper;

    private static final int LOW_STOCK_THRESHOLD = 5; // Default threshold
    private static final int MAX_LIST_ITEMS = 5; // Max items for "Recent" lists

    @Autowired
    public DashboardService(ProductRepository productRepository, SaleRepository saleRepository,
                            RepairJobRepository repairJobRepository, ProductMapper productMapper,
                            SaleMapper saleMapper, RepairJobMapper repairJobMapper) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.repairJobRepository = repairJobRepository;
        this.productMapper = productMapper;
        this.saleMapper = saleMapper;
        this.repairJobMapper = repairJobMapper;
    }

    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        Pageable limitToMax = PageRequest.of(0, MAX_LIST_ITEMS);

        // --- Metric Cards Data ---
        metrics.put("totalProducts", productRepository.count());
        BigDecimal todaysRevenue = saleRepository.findTodaysSalesRevenue();
        metrics.put("todaysSalesRevenue", todaysRevenue != null ? todaysRevenue : BigDecimal.ZERO);
        metrics.put("lowStockItemsCount", productRepository.countLowStockItems(LOW_STOCK_THRESHOLD));
        List<RepairStatus> excludedRepairStatuses = Arrays.asList(
                RepairStatus.COMPLETED_PAID, RepairStatus.COMPLETED_UNPAID,
                RepairStatus.CANCELLED_BY_CUSTOMER, RepairStatus.CANCELLED_BY_STORE, RepairStatus.UNREPAIRABLE);
        metrics.put("pendingRepairsCount", repairJobRepository.countByStatusNotIn(excludedRepairStatuses));

        // --- Weekly Sales Chart Data (Sales for last 7 days) ---
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(6).with(LocalTime.MIN); // Start of the day, 6 days ago
        List<Object[]> dailySalesData = saleRepository.findDailySalesTotalsBetweenDates(startDate, endDate);
        
        Map<String, BigDecimal> weeklySalesChartData = new LinkedHashMap<>();
        // Initialize map with all 7 days to ensure they appear on chart even if no sales
        for (int i = 6; i >= 0; i--) {
            weeklySalesChartData.put(LocalDate.now().minusDays(i).getDayOfWeek().toString().substring(0,3), BigDecimal.ZERO);
        }
        dailySalesData.forEach(row -> {
            java.sql.Date sqlDate = (java.sql.Date) row[0]; // JDBC might return java.sql.Date
            LocalDate saleDay = sqlDate.toLocalDate();
            BigDecimal dailyTotal = (BigDecimal) row[1];
            weeklySalesChartData.put(saleDay.getDayOfWeek().toString().substring(0,3), dailyTotal);
        });
        metrics.put("weeklySalesChartData", weeklySalesChartData); // Key: Day Name (String), Value: Total (BigDecimal)


        // --- Latest Repair Jobs List Data ---
        List<RepairJobDto> recentRepairs = repairJobRepository.findByStatusNotInOrderByDateReceivedDesc(excludedRepairStatuses, limitToMax)
                .stream().map(repairJobMapper::toDto).collect(Collectors.toList());
        metrics.put("latestRepairJobs", recentRepairs);

        // --- Recent Sales List Data ---
        List<SaleDto> recentSales = saleRepository.findTop5ByOrderBySaleDateDesc()
                .stream().map(saleMapper::toDto).collect(Collectors.toList());
        metrics.put("recentSales", recentSales);

        // --- Low Stock Alerts List Data ---
        List<ProductDto> lowStockProducts = productRepository.findByQuantityInStockLessThan(LOW_STOCK_THRESHOLD, limitToMax)
                .stream().map(productMapper::toDto).collect(Collectors.toList());
        metrics.put("lowStockAlerts", lowStockProducts);
        metrics.put("lowStockThreshold", LOW_STOCK_THRESHOLD); // Send threshold to UI

        // Sales by Category (kept if still used elsewhere or as fallback)
        // metrics.put("salesByCategory", ... );

        return metrics;
    }
}