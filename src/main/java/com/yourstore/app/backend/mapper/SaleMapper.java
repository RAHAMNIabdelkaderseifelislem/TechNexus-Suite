package com.yourstore.app.backend.mapper;

import com.yourstore.app.backend.model.dto.SaleDto;
import com.yourstore.app.backend.model.dto.SaleItemDto;
import com.yourstore.app.backend.model.entity.Sale;
import com.yourstore.app.backend.model.entity.SaleItem;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class SaleMapper {

    public SaleItemDto toDto(SaleItem saleItem) {
        if (saleItem == null) return null;
        return new SaleItemDto(
                saleItem.getId(),
                saleItem.getProduct().getId(),
                saleItem.getProduct().getName(),
                saleItem.getQuantity(),
                saleItem.getPriceAtSale(),
                saleItem.getSubtotal()
        );
    }

    // toEntity for SaleItem would require fetching Product, typically handled in service

    public SaleDto toDto(Sale sale) {
        if (sale == null) return null;
        return new SaleDto(
                sale.getId(),
                sale.getCustomerName(),
                sale.getItems().stream().map(this::toDto).collect(Collectors.toList()),
                sale.getTotalAmount(),
                sale.getSaleDate(),
                sale.getUser() != null ? sale.getUser().getUsername() : null,
                sale.getCreatedAt(),
                sale.getUpdatedAt()
        );
    }
    // toEntity for Sale would also be complex, handled in service layer
}