package com.yourstore.app.backend.service;

import com.yourstore.app.backend.exception.ResourceNotFoundException;
import com.yourstore.app.backend.mapper.ProductMapper;
import com.yourstore.app.backend.model.dto.ProductDto;
import com.yourstore.app.backend.model.entity.Product;
import com.yourstore.app.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Autowired
    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Transactional
    public ProductDto createProduct(ProductDto productDto) {
        Product product = productMapper.toEntity(productDto);
        if (product.getSellingPrice() == null) {
            throw new IllegalArgumentException("Selling price cannot be null for a new product.");
        }
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty.");
        }
        if (product.getCategory() == null) {
            throw new IllegalArgumentException("Product category cannot be null.");
        }
        Product savedProduct = productRepository.save(product);
        return productMapper.toDto(savedProduct);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toDto(product);
    }

    @Transactional
    public ProductDto updateProduct(Long id, ProductDto productDto) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id + " for update."));

        // Use mapper to update fields from DTO to existing entity
        productMapper.updateEntityFromDto(productDto, existingProduct);

        // Add any specific validation for update
        if (existingProduct.getSellingPrice() == null) {
            throw new IllegalArgumentException("Selling price cannot be null when updating product.");
        }
        if (existingProduct.getName() == null || existingProduct.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty when updating.");
        }
        if (existingProduct.getCategory() == null) {
            throw new IllegalArgumentException("Product category cannot be null when updating.");
        }


        Product updatedProduct = productRepository.save(existingProduct);
        return productMapper.toDto(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id + " for deletion.");
        }
        productRepository.deleteById(id);
    }
}