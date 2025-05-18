package com.yourstore.app.backend.controller;

import com.yourstore.app.backend.model.dto.ProductDto;
import com.yourstore.app.backend.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) {
        // Basic validation can be added here or using @Valid with Bean Validation
        if (productDto.getName() == null || productDto.getName().trim().isEmpty() ||
            productDto.getCategory() == null || productDto.getSellingPrice() == null) {
             // More specific error messages can be thrown from service or use Bean Validation
             return ResponseEntity.badRequest().body(null); // Or a more descriptive error DTO
        }
        ProductDto createdProduct = productService.createProduct(productDto);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        ProductDto productDto = productService.getProductById(id);
        return ResponseEntity.ok(productDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @RequestBody ProductDto productDto) {
        if (productDto.getName() == null || productDto.getName().trim().isEmpty() ||
            productDto.getCategory() == null || productDto.getSellingPrice() == null) {
            return ResponseEntity.badRequest().body(null);
        }
        ProductDto updatedProduct = productService.updateProduct(id, productDto);
        return ResponseEntity.ok(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build(); // Standard response for successful deletion
    }
}