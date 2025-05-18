package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.ProductDto;
import com.yourstore.app.backend.model.enums.ProductCategory;
import com.yourstore.app.frontend.service.ProductClientService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ProductListViewController {

    @FXML private TableView<ProductDto> productTableView;
    @FXML private TableColumn<ProductDto, Long> idColumn;
    @FXML private TableColumn<ProductDto, String> nameColumn;
    @FXML private TableColumn<ProductDto, ProductCategory> categoryColumn;
    @FXML private TableColumn<ProductDto, String> descriptionColumn;
    @FXML private TableColumn<ProductDto, String> supplierColumn;
    @FXML private TableColumn<ProductDto, BigDecimal> purchasePriceColumn;
    @FXML private TableColumn<ProductDto, BigDecimal> sellingPriceColumn;
    @FXML private TableColumn<ProductDto, Integer> quantityColumn;
    @FXML private TableColumn<ProductDto, LocalDateTime> createdAtColumn;
    @FXML private TableColumn<ProductDto, LocalDateTime> updatedAtColumn;

    @FXML private Button addProductButton;
    @FXML private Button editProductButton;
    @FXML private Button deleteProductButton;
    @FXML private Button refreshButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;

    private final ProductClientService productClientService;
    private final ObservableList<ProductDto> productList = FXCollections.observableArrayList();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    @Autowired
    public ProductListViewController(ProductClientService productClientService) {
        this.productClientService = productClientService;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupRowSelectionListener();
        loadProducts();
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        purchasePriceColumn.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        sellingPriceColumn.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantityInStock"));

        // Custom cell factory for LocalDateTime formatting
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdAtColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(dateTimeFormatter.format(item));
                }
            }
        });

        updatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
        updatedAtColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(dateTimeFormatter.format(item));
                }
            }
        });

        productTableView.setItems(productList);
    }

    private void setupRowSelectionListener() {
        productTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            editProductButton.setDisable(!hasSelection);
            deleteProductButton.setDisable(!hasSelection);
        });
    }

    @FXML
    private void handleRefreshProducts() {
        loadProducts();
    }

    private void loadProducts() {
        showProgress(true, "Loading products...");
        productClientService.getAllProducts()
            .thenAcceptAsync(products -> Platform.runLater(() -> { // Ensure UI updates on JavaFX thread
                productList.setAll(products);
                productTableView.sort(); // Re-apply sort if any
                showProgress(false, "Products loaded successfully. Found " + products.size() + " items.");
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    System.err.println("Error loading products: " + ex.getMessage());
                    ex.printStackTrace();
                    showProgress(false, "Error loading products: " + ex.getCause().getMessage());
                    // Show alert dialog
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to load products");
                    alert.setContentText(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    alert.showAndWait();
                });
                return null;
            });
    }

    private void showProgress(boolean show, String message) {
        progressIndicator.setVisible(show);
        statusLabel.setText(message != null ? message : "");
    }


    @FXML
    private void handleAddProduct() {
        // To be implemented: Open a dialog/new view to add a product
        statusLabel.setText("Add Product clicked - Placeholder");
        System.out.println("Add Product clicked");
         Alert alert = new Alert(Alert.AlertType.INFORMATION);
         alert.setTitle("Information");
         alert.setHeaderText("Add Product");
         alert.setContentText("This feature will be implemented in a future commit.");
         alert.showAndWait();
    }

    @FXML
    private void handleEditProduct() {
        ProductDto selectedProduct = productTableView.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            // To be implemented: Open a dialog/new view to edit selectedProduct
            statusLabel.setText("Edit Product clicked for: " + selectedProduct.getName() + " - Placeholder");
            System.out.println("Edit Product: " + selectedProduct.getName());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information");
            alert.setHeaderText("Edit Product");
            alert.setContentText("This feature will be implemented in a future commit.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleDeleteProduct() {
        ProductDto selectedProduct = productTableView.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            // To be implemented: Confirm and delete selectedProduct using ProductClientService
            statusLabel.setText("Delete Product clicked for: " + selectedProduct.getName() + " - Placeholder");
            System.out.println("Delete Product: " + selectedProduct.getName());
             Alert alert = new Alert(Alert.AlertType.INFORMATION);
             alert.setTitle("Information");
             alert.setHeaderText("Delete Product");
             alert.setContentText("This feature will be implemented in a future commit.");
             alert.showAndWait();
        }
    }
}