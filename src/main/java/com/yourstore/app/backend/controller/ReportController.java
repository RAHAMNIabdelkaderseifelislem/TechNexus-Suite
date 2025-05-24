package com.yourstore.app.backend.controller;

import com.yourstore.app.backend.model.dto.SaleDto;
import com.yourstore.app.backend.model.dto.reports.StockReportItemDto; 
import com.yourstore.app.backend.model.dto.reports.SalesByProductDto; // Ensure this DTO is in the correct package
import com.yourstore.app.backend.model.dto.reports.ProfitLossReportDto; // Ensure this DTO is in the correct package
import com.yourstore.app.backend.service.ReportService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // For method-level security
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')") // Apply to all methods in controller
public class ReportController {
    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/detailed-sales")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')") // Or specific roles
    public ResponseEntity<List<SaleDto>> getDetailedSales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(reportService.getDetailedSalesReport(startDate, endDate));
    }

    // Endpoint for Profit/Loss Report (example from previous steps)
    @GetMapping("/profit-loss")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<ProfitLossReportDto> getProfitLoss(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        // Ensure ProfitLossReportDto is defined and ReportService.getProfitLossReport is implemented
        return ResponseEntity.ok(reportService.getProfitLossReport(startDate, endDate));
    }

    @GetMapping("/current-stock")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    public ResponseEntity<List<StockReportItemDto>> getCurrentStockReport() {
        return ResponseEntity.ok(reportService.getCurrentStockReport());
    }
}