package com.yourstore.app.backend.service;

import com.yourstore.app.backend.model.entity.Product;
import com.yourstore.app.backend.model.entity.Purchase;
import com.yourstore.app.backend.model.entity.PurchaseItem;
import com.yourstore.app.backend.model.entity.User;
import com.yourstore.app.backend.model.dto.PurchaseDto;
import com.yourstore.app.backend.model.dto.PurchaseItemDto;
import com.yourstore.app.backend.repository.ProductRepository;
import com.yourstore.app.backend.repository.PurchaseRepository;
import com.yourstore.app.backend.repository.UserRepository;
import com.yourstore.app.backend.mapper.PurchaseMapper;
import com.yourstore.app.backend.exception.ResourceNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PurchaseService {
    private static final Logger logger = LoggerFactory.getLogger(PurchaseService.class);

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PurchaseMapper purchaseMapper;

    @Autowired
    public PurchaseService(PurchaseRepository purchaseRepository, ProductRepository productRepository,
                           UserRepository userRepository, PurchaseMapper purchaseMapper) {
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.purchaseMapper = purchaseMapper;
    }

    @Transactional
    public PurchaseDto createPurchase(PurchaseDto purchaseDto) {
        logger.info("Creating new purchase for supplier: {}", purchaseDto.getSupplierName());
        Purchase purchase = new Purchase();
        purchase.setSupplierName(purchaseDto.getSupplierName());
        purchase.setInvoiceNumber(purchaseDto.getInvoiceNumber());
        purchase.setPurchaseDate(LocalDateTime.now()); // Or use a date from DTO if provided

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails)principal).getUsername();
        } else {
            username = principal.toString();
        }
        User currentUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User '" + username + "' not found. Cannot record purchase."));
        purchase.setUser(currentUser);

        if (purchaseDto.getItems() == null || purchaseDto.getItems().isEmpty()) {
            throw new IllegalArgumentException("Purchase must have at least one item.");
        }

        for (PurchaseItemDto itemDto : purchaseDto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + itemDto.getProductId() + " not found."));

            PurchaseItem purchaseItem = new PurchaseItem(purchase, product, itemDto.getQuantity(), itemDto.getCostPrice());
            purchase.addItem(purchaseItem);

            // Update product stock
            int newStock = product.getQuantityInStock() + itemDto.getQuantity();
            product.setQuantityInStock(newStock);
            // Optionally update product's purchasePrice if this new costPrice is more relevant for average costing etc.
            // For simplicity, we might just update it if it's not set or this one is different.
            // product.setPurchasePrice(itemDto.getCostPrice());
            productRepository.save(product);
            logger.info("Updated stock for product {}: new stock {}", product.getName(), newStock);
        }
        purchase.calculateTotalAmount(); // Calculate total after all items are added and processed
        
        Purchase savedPurchase = purchaseRepository.save(purchase);
        logger.info("Purchase created successfully with ID: {}", savedPurchase.getId());
        return purchaseMapper.toDto(savedPurchase);
    }

    @Transactional(readOnly = true)
    public List<PurchaseDto> getAllPurchases() {
        logger.info("Fetching all purchases");
        return purchaseRepository.findAll().stream()
                                 .map(purchaseMapper::toDto)
                                 .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseDto getPurchaseById(Long id) {
        logger.info("Fetching purchase by ID: {}", id);
        Purchase purchase = purchaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with ID: " + id));
        return purchaseMapper.toDto(purchase);
    }
}