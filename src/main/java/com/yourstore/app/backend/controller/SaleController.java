package com.yourstore.app.backend.controller;

import com.yourstore.app.backend.model.dto.SaleDto;
import com.yourstore.app.backend.service.SaleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.validation.Valid;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/sales")
public class SaleController {

    private final SaleService saleService;

    @Autowired
    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    public ResponseEntity<SaleDto> createSale(@Valid @RequestBody SaleDto saleDto) { // Add @Valid
        SaleDto createdSale = saleService.createSale(saleDto);
        return new ResponseEntity<>(createdSale, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SaleDto>> getAllSales() {
        List<SaleDto> sales = saleService.getAllSales();
        return ResponseEntity.ok(sales);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleDto> getSaleById(@PathVariable Long id) {
        SaleDto saleDto = saleService.getSaleById(id);
        return ResponseEntity.ok(saleDto);
    }

    @GetMapping("/export/csv")
    public void exportSalesToCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        response.setHeader("Content-Disposition", "attachment; filename=\"sales_export_" + timestamp + ".csv\"");

        saleService.exportSalesToCsv(response.getWriter());
    }
}