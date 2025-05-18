package com.yourstore.app.backend.service;

import com.yourstore.app.backend.exception.ResourceNotFoundException;
import com.yourstore.app.backend.mapper.SaleMapper;
import com.yourstore.app.backend.model.dto.SaleDto;
import com.yourstore.app.backend.model.dto.SaleItemDto;
import com.yourstore.app.backend.model.entity.Product;
import com.yourstore.app.backend.model.entity.Sale;
import com.yourstore.app.backend.model.entity.SaleItem;
import com.yourstore.app.backend.model.entity.User;
import com.yourstore.app.backend.repository.ProductRepository;
import com.yourstore.app.backend.repository.SaleRepository;
import com.yourstore.app.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.csv.CSVFormat; // Add Apache Commons CSV dependency
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SaleMapper saleMapper;

    @Autowired
    public SaleService(SaleRepository saleRepository, ProductRepository productRepository,
                       UserRepository userRepository, SaleMapper saleMapper) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.saleMapper = saleMapper;
    }

    @Transactional
    public SaleDto createSale(SaleDto saleDto) {
        Sale sale = new Sale();
        sale.setCustomerName(saleDto.getCustomerName());
        sale.setSaleDate(LocalDateTime.now());

        // Get current authenticated user
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails)principal).getUsername();
        } else {
            username = principal.toString();
        }
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username + " to associate with sale."));
        sale.setUser(currentUser);

        if (saleDto.getItems() == null || saleDto.getItems().isEmpty()) {
            throw new IllegalArgumentException("Sale must have at least one item.");
        }

        for (SaleItemDto itemDto : saleDto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + itemDto.getProductId()));

            if (product.getQuantityInStock() < itemDto.getQuantity()) {
                throw new IllegalArgumentException("Not enough stock for product: " + product.getName() +
                                                   ". Requested: " + itemDto.getQuantity() +
                                                   ", Available: " + product.getQuantityInStock());
            }
            // Use product's current selling price for priceAtSale
            SaleItem saleItem = new SaleItem(sale, product, itemDto.getQuantity(), product.getSellingPrice());
            sale.addItem(saleItem);

            // Decrease product stock
            product.setQuantityInStock(product.getQuantityInStock() - itemDto.getQuantity());
            productRepository.save(product);
        }
        // Total amount is calculated by addItem or explicitly:
        sale.calculateTotalAmount();
        Sale savedSale = saleRepository.save(sale);
        return saleMapper.toDto(savedSale);
    }

    @Transactional(readOnly = true)
    public List<SaleDto> getAllSales() {
        return saleRepository.findAll().stream()
                .map(saleMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SaleDto getSaleById(Long id) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));
        return saleMapper.toDto(sale);
    }

    @Transactional(readOnly = true)
    public void exportSalesToCsv(Writer writer) throws IOException {
        List<Sale> sales = saleRepository.findAll(); // Uses the @EntityGraph version

        // Define CSV Headers
        String[] saleHeaders = {"SaleID", "CustomerName", "TotalAmount", "SaleDate", "CashierUsername", "CreatedAt", "UpdatedAt"};
        String[] saleItemHeaders = {"SaleItemID", "SaleID_FK", "ProductID", "ProductName", "Quantity", "PriceAtSale", "Subtotal"};

        // Using a try-with-resources for CSVPrinter for sales might be tricky if you want two sections.
        // Let's write headers manually and then print records.

        try (CSVPrinter salesCsvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(saleHeaders))) {
            for (Sale sale : sales) {
                salesCsvPrinter.printRecord(
                        sale.getId(),
                        sale.getCustomerName(),
                        sale.getTotalAmount(),
                        sale.getSaleDate(),
                        sale.getUser() != null ? sale.getUser().getUsername() : "N/A",
                        sale.getCreatedAt(),
                        sale.getUpdatedAt()
                );
            }
        } // Sales printer closes here

        // Add a separator or new section for sale items
        writer.write("\n\nSALE_ITEMS\n"); // Indicates start of sale items section

        try (CSVPrinter saleItemsCsvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(saleItemHeaders))) {
             for (Sale sale : sales) { // Iterate through sales again to get their items
                for (SaleItem item : sale.getItems()) {
                    saleItemsCsvPrinter.printRecord(
                            item.getId(),
                            sale.getId(), // Foreign key to Sale
                            item.getProduct().getId(),
                            item.getProduct().getName(),
                            item.getQuantity(),
                            item.getPriceAtSale(),
                            item.getSubtotal()
                    );
                }
            }
        } // Sale items printer closes here
    }
}