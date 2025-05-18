package com.yourstore.app.backend.service;

import com.yourstore.app.backend.model.enums.ProductCategory; // Import ProductCategory
import com.yourstore.app.backend.repository.ProductRepository;
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

    @Autowired
    public DashboardService(ProductRepository productRepository, SaleRepository saleRepository) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
    }

    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalProducts", productRepository.count());
        metrics.put("totalSalesCount", saleRepository.count());
        BigDecimal totalRevenue = saleRepository.findTotalSalesRevenue();
        metrics.put("totalSalesRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

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