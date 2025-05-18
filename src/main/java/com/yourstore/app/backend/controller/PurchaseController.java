package com.yourstore.app.backend.controller;

import com.yourstore.app.backend.model.dto.PurchaseDto;
import com.yourstore.app.backend.service.PurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @Autowired
    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public ResponseEntity<PurchaseDto> createPurchase(@Valid @RequestBody PurchaseDto purchaseDto) {
        PurchaseDto createdPurchase = purchaseService.createPurchase(purchaseDto);
        return new ResponseEntity<>(createdPurchase, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PurchaseDto>> getAllPurchases() {
        List<PurchaseDto> purchases = purchaseService.getAllPurchases();
        return ResponseEntity.ok(purchases);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseDto> getPurchaseById(@PathVariable Long id) {
        PurchaseDto purchaseDto = purchaseService.getPurchaseById(id);
        return ResponseEntity.ok(purchaseDto);
    }
}