package com.yourstore.app.backend.service;

import com.yourstore.app.backend.model.enums.ProductCategory; // Import ProductCategory
import com.yourstore.app.backend.model.enums.RepairStatus;
import com.yourstore.app.backend.repository.ProductRepository;
import com.yourstore.app.backend.repository.RepairJobRepository;
import com.yourstore.app.backend.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final RepairJobRepository repairJobRepository; // Add this
    private static final int LOW_STOCK_THRESHOLD = 5; // Define a threshold

    @Autowired
    public DashboardService(ProductRepository productRepository, SaleRepository saleRepository, RepairJobRepository repairJobRepository) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.repairJobRepository = repairJobRepository; // Initialize
    }

    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        // Existing metrics
        metrics.put("totalProducts", productRepository.count());
        metrics.put("totalSalesCount", saleRepository.count());
        BigDecimal totalRevenue = saleRepository.findTotalSalesRevenue();
        metrics.put("totalSalesRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

        List<Object[]> salesByCategoryData = saleRepository.findSalesCountPerCategory();
        if (salesByCategoryData != null) {
            metrics.put("salesByCategory", salesByCategoryData.stream()
                .filter(row -> row[0] instanceof ProductCategory && row[1] instanceof Long)
                .collect(Collectors.toMap(
                    row -> ((ProductCategory)row[0]).toString(),
                    row -> (Long)row[1]
                )));
        } else {
            metrics.put("salesByCategory", Collections.emptyMap());
        }

        BigDecimal todaysRevenue = saleRepository.findTodaysSalesRevenue();
        metrics.put("todaysSalesRevenue", todaysRevenue != null ? todaysRevenue : BigDecimal.ZERO);
        metrics.put("lowStockItemsCount", productRepository.countLowStockItems(LOW_STOCK_THRESHOLD));
        
        // Call the corrected repository method for pending repairs
        List<RepairStatus> excludedStatuses = Arrays.asList(
                RepairStatus.COMPLETED_PAID,
                RepairStatus.COMPLETED_UNPAID,
                RepairStatus.CANCELLED_BY_CUSTOMER,
                RepairStatus.CANCELLED_BY_STORE,
                RepairStatus.UNREPAIRABLE
        );
        Long pendingRepairs = repairJobRepository.countByStatusNotIn(excludedStatuses);
        metrics.put("pendingRepairsCount", pendingRepairs != null ? pendingRepairs : 0L);
            
        return metrics;
    }
}