package com.yourstore.app.backend.model.entity;

import com.yourstore.app.backend.model.entity.base.Auditable; // Assuming Auditable for createdAt/updatedAt

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
public class Sale extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", length = 100) // Simple customer name for now
    private String customerName;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SaleItem> items = new ArrayList<>();

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "sale_date", nullable = false)
    private LocalDateTime saleDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // User who processed the sale (cashier)
    private User user;

    // Constructors
    public Sale() {
        this.saleDate = LocalDateTime.now(); // Default to current time
    }

    // Helper method to calculate total amount
    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
                                .map(SaleItem::getSubtotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Helper method to add item
    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
        calculateTotalAmount();
    }

    public void removeItem(SaleItem item) {
        items.remove(item);
        item.setSale(null);
        calculateTotalAmount();
    }


    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public List<SaleItem> getItems() { return items; }
    public void setItems(List<SaleItem> items) { this.items = items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public LocalDateTime getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDateTime saleDate) { this.saleDate = saleDate; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}