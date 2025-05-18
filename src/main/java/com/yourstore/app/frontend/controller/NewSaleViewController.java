package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.ProductDto;
import com.yourstore.app.backend.model.dto.SaleDto;
import com.yourstore.app.backend.model.dto.SaleItemDto;
import com.yourstore.app.frontend.service.ProductClientService;
import com.yourstore.app.frontend.service.SaleClientService;
import com.yourstore.app.frontend.util.StageManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

@Component
public class NewSaleViewController {

    @FXML private TextField customerNameField;
    @FXML private ComboBox<ProductDto> productComboBox;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private Button addItemButton;
    @FXML private TableView<SaleItemDto> saleItemsTableView;
    @FXML private TableColumn<SaleItemDto, String> productNameColumn;
    @FXML private TableColumn<SaleItemDto, Integer> quantityColumn;
    @FXML private TableColumn<SaleItemDto, BigDecimal> priceAtSaleColumn;
    @FXML private TableColumn<SaleItemDto, BigDecimal> subtotalColumn;
    @FXML private TableColumn<SaleItemDto, Void> actionsColumn;
    @FXML private Label totalAmountLabel;
    @FXML private Button completeSaleButton;
    @FXML private Button cancelSaleButton;
    @FXML private Label statusMessageLabel;
    @FXML private Label errorMessageLabel;


    private final ProductClientService productClientService;
    private final SaleClientService saleClientService;
    private final StageManager stageManager; // To close or navigate

    private ObservableList<ProductDto> availableProducts = FXCollections.observableArrayList();
    private ObservableList<SaleItemDto> currentSaleItems = FXCollections.observableArrayList();
    private BigDecimal currentTotalAmount = BigDecimal.ZERO;

    @Autowired
    public NewSaleViewController(ProductClientService productClientService, SaleClientService saleClientService, StageManager stageManager) {
        this.productClientService = productClientService;
        this.saleClientService = saleClientService;
        this.stageManager = stageManager;
    }

    @FXML
    public void initialize() {
        setupProductComboBox();
        setupSaleItemsTable();
        loadAvailableProducts();
        updateTotalAmountDisplay();
        clearMessages();
    }
    
    private void clearMessages() {
        statusMessageLabel.setText("");
        errorMessageLabel.setText("");
    }

    private void setupProductComboBox() {
        productComboBox.setItems(availableProducts);
        productComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProductDto product) {
                return product != null ? product.getName() + " (Stock: " + product.getQuantityInStock() + ", Price: " + product.getSellingPrice() + ")" : "";
            }
            @Override
            public ProductDto fromString(String string) { return null; /* Not needed for non-editable ComboBox selection */ }
        });
        // Optional: Add filtering for ComboBox as user types (more complex)
    }

    private void loadAvailableProducts() {
        productClientService.getAllProducts()
            .thenAcceptAsync(products -> Platform.runLater(() -> {
                availableProducts.setAll(products);
                if (!products.isEmpty()) productComboBox.getSelectionModel().selectFirst();
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> errorMessageLabel.setText("Error loading products: " + ex.getMessage()));
                return null;
            });
    }

    private void setupSaleItemsTable() {
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceAtSaleColumn.setCellValueFactory(new PropertyValueFactory<>("priceAtSale"));
        subtotalColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        addRemoveButtonToTable();
        saleItemsTableView.setItems(currentSaleItems);
    }
    
    private void addRemoveButtonToTable() {
        Callback<TableColumn<SaleItemDto, Void>, TableCell<SaleItemDto, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<SaleItemDto, Void> call(final TableColumn<SaleItemDto, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button("Remove");
                    {
                        btn.setOnAction(event -> {
                            SaleItemDto item = getTableView().getItems().get(getIndex());
                            currentSaleItems.remove(item);
                            calculateTotalAmount();
                            updateTotalAmountDisplay();
                        });
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
            }
        };
        actionsColumn.setCellFactory(cellFactory);
    }


    @FXML
    private void handleAddItemToSale() {
        clearMessages();
        ProductDto selectedProduct = productComboBox.getSelectionModel().getSelectedItem();
        int quantity = quantitySpinner.getValue();

        if (selectedProduct == null) {
            errorMessageLabel.setText("Please select a product.");
            return;
        }
        if (quantity <= 0) {
            errorMessageLabel.setText("Quantity must be greater than 0.");
            return;
        }
        if (selectedProduct.getQuantityInStock() < quantity) {
            errorMessageLabel.setText("Not enough stock for " + selectedProduct.getName() + ". Available: " + selectedProduct.getQuantityInStock());
            return;
        }

        // Check if item already exists, if so, update quantity (optional)
        Optional<SaleItemDto> existingItemOpt = currentSaleItems.stream()
            .filter(item -> item.getProductId().equals(selectedProduct.getId()))
            .findFirst();

        if (existingItemOpt.isPresent()) {
            SaleItemDto existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantity;
             if (selectedProduct.getQuantityInStock() < newQuantity - existingItem.getQuantity()) { // Check against original stock + what's already in cart
                errorMessageLabel.setText("Not enough stock for " + selectedProduct.getName() + " to add more. Available: " + selectedProduct.getQuantityInStock());
                return;
            }
            existingItem.setQuantity(newQuantity);
            existingItem.setSubtotal(selectedProduct.getSellingPrice().multiply(BigDecimal.valueOf(newQuantity)));
            saleItemsTableView.refresh(); // Refresh table to show updated subtotal
        } else {
            SaleItemDto newItem = new SaleItemDto();
            newItem.setProductId(selectedProduct.getId());
            newItem.setProductName(selectedProduct.getName());
            newItem.setQuantity(quantity);
            newItem.setPriceAtSale(selectedProduct.getSellingPrice());
            newItem.setSubtotal(selectedProduct.getSellingPrice().multiply(BigDecimal.valueOf(quantity)));
            currentSaleItems.add(newItem);
        }
        
        calculateTotalAmount();
        updateTotalAmountDisplay();
        quantitySpinner.getValueFactory().setValue(1); // Reset spinner
    }
    
    private void calculateTotalAmount() {
        currentTotalAmount = BigDecimal.ZERO;
        for (SaleItemDto item : currentSaleItems) {
            currentTotalAmount = currentTotalAmount.add(item.getSubtotal());
        }
    }

    private void updateTotalAmountDisplay() {
        totalAmountLabel.setText(String.format("%.2f", currentTotalAmount));
    }

    @FXML
    private void handleCompleteSale() {
        clearMessages();
        if (currentSaleItems.isEmpty()) {
            errorMessageLabel.setText("Cannot complete sale with no items.");
            return;
        }

        SaleDto newSale = new SaleDto();
        newSale.setCustomerName(customerNameField.getText());
        newSale.setItems(new ArrayList<>(currentSaleItems)); // Create a new list from observable list
        // totalAmount and saleDate will be set by backend or SaleService

        completeSaleButton.setDisable(true);
        cancelSaleButton.setDisable(true);
        statusMessageLabel.setText("Processing sale...");

        saleClientService.createSale(newSale)
            .thenAcceptAsync(savedSale -> Platform.runLater(() -> {
                statusMessageLabel.setText("Sale completed successfully! Sale ID: " + savedSale.getId());
                resetSaleForm();
                loadAvailableProducts(); // Refresh product stock in ComboBox
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    errorMessageLabel.setText("Failed to complete sale: " + cause.getMessage());
                    System.err.println("Sale completion error: " + cause.getMessage());
                    cause.printStackTrace();
                    completeSaleButton.setDisable(false);
                    cancelSaleButton.setDisable(false);
                });
                return null;
            });
    }

    private void resetSaleForm() {
        customerNameField.clear();
        currentSaleItems.clear();
        calculateTotalAmount();
        updateTotalAmountDisplay();
        productComboBox.getSelectionModel().clearSelection();
        if (!availableProducts.isEmpty()) productComboBox.getSelectionModel().selectFirst();
        quantitySpinner.getValueFactory().setValue(1);
        completeSaleButton.setDisable(false);
        cancelSaleButton.setDisable(false);
        clearMessages();
    }

    @FXML
    private void handleCancelSale() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to cancel this sale and clear all items?", ButtonType.YES, ButtonType.NO);
        confirmDialog.setTitle("Confirm Cancel");
        confirmDialog.setHeaderText(null);
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            resetSaleForm();
            statusMessageLabel.setText("Sale cancelled.");
        }
    }
}