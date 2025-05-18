package com.yourstore.app.backend.mapper;

import com.yourstore.app.backend.model.dto.PurchaseDto;
import com.yourstore.app.backend.model.dto.PurchaseItemDto;
import com.yourstore.app.backend.model.entity.Purchase;
import com.yourstore.app.backend.model.entity.PurchaseItem;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class PurchaseMapper {

    public PurchaseItemDto toDto(PurchaseItem item) {
        if (item == null) return null;
        return new PurchaseItemDto(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getCostPrice(),
                item.getSubtotal()
        );
    }

    public PurchaseDto toDto(Purchase purchase) {
        if (purchase == null) return null;
        return new PurchaseDto(
                purchase.getId(),
                purchase.getSupplierName(),
                purchase.getInvoiceNumber(),
                purchase.getItems().stream().map(this::toDto).collect(Collectors.toList()),
                purchase.getTotalAmount(),
                purchase.getPurchaseDate(),
                purchase.getUser() != null ? purchase.getUser().getUsername() : null,
                purchase.getCreatedAt(),
                purchase.getUpdatedAt()
        );
    }
}