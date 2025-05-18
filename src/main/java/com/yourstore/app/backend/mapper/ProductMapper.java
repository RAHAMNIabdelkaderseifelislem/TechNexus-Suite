package com.yourstore.app.backend.mapper;

import com.yourstore.app.backend.model.dto.ProductDto;
import com.yourstore.app.backend.model.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductDto toDto(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductDto(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getCategory(),
            product.getSupplier(),
            product.getPurchasePrice(),
            product.getSellingPrice(),
            product.getQuantityInStock(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    public Product toEntity(ProductDto productDto) {
        if (productDto == null) {
            return null;
        }
        Product product = new Product();
        // product.setId(productDto.getId()); // Usually not set when creating new entity from DTO
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setCategory(productDto.getCategory());
        product.setSupplier(productDto.getSupplier());
        product.setPurchasePrice(productDto.getPurchasePrice());
        product.setSellingPrice(productDto.getSellingPrice());
        product.setQuantityInStock(productDto.getQuantityInStock());
        // createdAt and updatedAt are handled by Auditing
        return product;
    }

    public void updateEntityFromDto(ProductDto productDto, Product product) {
        if (productDto == null || product == null) {
            return;
        }
        product.setName(productDto.getName());
        product.setDescription(productDto.getDescription());
        product.setCategory(productDto.getCategory());
        product.setSupplier(productDto.getSupplier());
        product.setPurchasePrice(productDto.getPurchasePrice());
        product.setSellingPrice(productDto.getSellingPrice());
        product.setQuantityInStock(productDto.getQuantityInStock());
        // ID, createdAt, updatedAt are generally not updated from DTO in this manner
    }
}