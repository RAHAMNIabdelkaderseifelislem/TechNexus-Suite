package com.yourstore.app.backend.service;

import com.yourstore.app.backend.model.enums.ProductCategory; // Import ProductCategory
import com.yourstore.app.backend.repository.ProductRepository;
import com.yourstore.app.backend.repository.RepairJobRepository;
import com.yourstore.app.backend.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final RepairJobRepository repairJobRepository;

    @Autowired
    public DashboardService(ProductRepository productRepository, SaleRepository saleRepository, RepairJobRepository repairJobRepository) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.repairJobRepository = repairJobRepository;
    }

    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalProducts", productRepository.count());
        metrics.put("totalSalesCount", saleRepository.count());
        BigDecimal totalRevenue = saleRepository.findTotalSalesRevenue();
        metrics.put("totalSalesRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        metrics.put("pendingRepairsCount", repairJobRepository.countPendingRepairs() != null ? repairJobRepository.countPendingRepairs() : 0L);

        List<Object[]> salesByCategoryData = saleRepository.findSalesCountPerCategory();
        if (salesByCategoryData != null) {
            metrics.put("salesByCategory", salesByCategoryData.stream()
                .filter(row -> row[0] instanceof ProductCategory && row[1] instanceof Long) // Type check
                .collect(Collectors.toMap(
                    row -> ((ProductCategory)row[0]).toString(), // Category Name
                    row -> (Long)row[1]                            // Count
                )));
        } else {
            metrics.put("salesByCategory", Collections.emptyMap());
        }
        return metrics;
    }
}