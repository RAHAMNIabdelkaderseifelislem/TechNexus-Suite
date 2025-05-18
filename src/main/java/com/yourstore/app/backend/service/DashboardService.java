package com.yourstore.app.backend.service;

import com.yourstore.app.backend.repository.ProductRepository;
import com.yourstore.app.backend.repository.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

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
        // Add more metrics as needed
        return metrics;
    }
}