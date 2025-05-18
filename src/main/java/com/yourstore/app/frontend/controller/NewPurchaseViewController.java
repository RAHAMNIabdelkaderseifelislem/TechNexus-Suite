package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.ProductDto;
import com.yourstore.app.backend.model.dto.PurchaseDto;
import com.yourstore.app.backend.model.dto.PurchaseItemDto;
import com.yourstore.app.frontend.service.ProductClientService;
import com.yourstore.app.frontend.service.PurchaseClientService;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Optional;

@Component
public class NewPurchaseViewController {

    @FXML private TextField supplierNameField;
    @FXML private TextField invoiceNumberField;
    @FXML private ComboBox<ProductDto> productComboBox;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private Spinner<Double> costPriceSpinner;
    @FXML private TableView<PurchaseItemDto> purchaseItemsTableView;
    @FXML private TableColumn<PurchaseItemDto, String> productNameColumn;
    @FXML private TableColumn<PurchaseItemDto, Integer> quantityColumn;
    @FXML private TableColumn<PurchaseItemDto, BigDecimal> costPriceColumn;
    @FXML private TableColumn<PurchaseItemDto, BigDecimal> subtotalColumn;
    @FXML private TableColumn<PurchaseItemDto, Void> actionsColumn;
    @FXML private Label totalAmountLabel;
    @FXML private Button completePurchaseButton;
    @FXML private Button cancelPurchaseButton;
    @FXML private Label statusMessageLabel;
    @FXML private Label errorMessageLabel;

    private final ProductClientService productClientService;
    private final PurchaseClientService purchaseClientService;
    private final StageManager stageManager;

    private ObservableList<ProductDto> availableProducts = FXCollections.observableArrayList();
    private ObservableList<PurchaseItemDto> currentPurchaseItems = FXCollections.observableArrayList();
    private BigDecimal currentTotalAmount = BigDecimal.ZERO;

    @Autowired
    public NewPurchaseViewController(ProductClientService productClientService, PurchaseClientService purchaseClientService, StageManager stageManager) {
        this.productClientService = productClientService;
        this.purchaseClientService = purchaseClientService;
        this.stageManager = stageManager;
    }

    @FXML
    public void initialize() {
        setupProductComboBox();
        setupPurchaseItemsTable();
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
                return product != null ? product.getName() + " (Stock: " + product.getQuantityInStock() + ")" : "";
            }
            @Override
            public ProductDto fromString(String string) { return null; }
        });
        productComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getPurchasePrice() != null) {
                costPriceSpinner.getValueFactory().setValue(newVal.getPurchasePrice().doubleValue());
            } else if (newVal != null) {
                 costPriceSpinner.getValueFactory().setValue(0.01); // Default if no purchase price
            }
        });
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

    private void setupPurchaseItemsTable() {
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        costPriceColumn.setCellValueFactory(new PropertyValueFactory<>("costPrice"));
        subtotalColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
        addRemoveButtonToTable();
        purchaseItemsTableView.setItems(currentPurchaseItems);
    }

    private void addRemoveButtonToTable() {
        Callback<TableColumn<PurchaseItemDto, Void>, TableCell<PurchaseItemDto, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btn = new Button("Remove");
            {
                btn.setOnAction(event -> {
                    PurchaseItemDto item = getTableView().getItems().get(getIndex());
                    currentPurchaseItems.remove(item);
                    calculateTotalAmount();
                    updateTotalAmountDisplay();
                });
            }
            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        };
        actionsColumn.setCellFactory(cellFactory);
    }

    @FXML
    private void handleAddItemToPurchase() {
        clearMessages();
        ProductDto selectedProduct = productComboBox.getSelectionModel().getSelectedItem();
        int quantity = quantitySpinner.getValue();
        double costPriceDouble = costPriceSpinner.getValue();

        if (selectedProduct == null) {
            errorMessageLabel.setText("Please select a product.");
            return;
        }
        if (quantity <= 0) {
            errorMessageLabel.setText("Quantity must be greater than 0.");
            return;
        }
        if (costPriceDouble <= 0) {
            errorMessageLabel.setText("Cost price must be greater than 0.");
            return;
        }
        BigDecimal costPrice = BigDecimal.valueOf(costPriceDouble).setScale(2, RoundingMode.HALF_UP);

        Optional<PurchaseItemDto> existingItemOpt = currentPurchaseItems.stream()
            .filter(item -> item.getProductId().equals(selectedProduct.getId()) && item.getCostPrice().compareTo(costPrice) == 0)
            .findFirst();

        if (existingItemOpt.isPresent()) {
            PurchaseItemDto existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            existingItem.setSubtotal(costPrice.multiply(BigDecimal.valueOf(existingItem.getQuantity())));
            purchaseItemsTableView.refresh();
        } else {
            PurchaseItemDto newItem = new PurchaseItemDto();
            newItem.setProductId(selectedProduct.getId());
            newItem.setProductName(selectedProduct.getName());
            newItem.setQuantity(quantity);
            newItem.setCostPrice(costPrice);
            newItem.setSubtotal(costPrice.multiply(BigDecimal.valueOf(quantity)));
            currentPurchaseItems.add(newItem);
        }
        
        calculateTotalAmount();
        updateTotalAmountDisplay();
        quantitySpinner.getValueFactory().setValue(1);
    }
    
    private void calculateTotalAmount() {
        currentTotalAmount = BigDecimal.ZERO;
        for (PurchaseItemDto item : currentPurchaseItems) {
            currentTotalAmount = currentTotalAmount.add(item.getSubtotal());
        }
    }

    private void updateTotalAmountDisplay() {
        totalAmountLabel.setText(String.format("%.2f", currentTotalAmount));
    }

    @FXML
    private void handleCompletePurchase() {
        clearMessages();
        if (currentPurchaseItems.isEmpty()) {
            errorMessageLabel.setText("Cannot complete purchase with no items.");
            return;
        }
        String supplier = supplierNameField.getText();
        if(supplier == null || supplier.trim().isEmpty()){
            errorMessageLabel.setText("Supplier name is required.");
            return;
        }


        PurchaseDto newPurchase = new PurchaseDto();
        newPurchase.setSupplierName(supplier);
        newPurchase.setInvoiceNumber(invoiceNumberField.getText());
        newPurchase.setItems(new ArrayList<>(currentPurchaseItems));

        completePurchaseButton.setDisable(true);
        cancelPurchaseButton.setDisable(true);
        statusMessageLabel.setText("Processing purchase...");

        purchaseClientService.createPurchase(newPurchase)
            .thenAcceptAsync(savedPurchase -> Platform.runLater(() -> {
                statusMessageLabel.setText("Purchase completed successfully! Purchase ID: " + savedPurchase.getId());
                resetPurchaseForm();
                loadAvailableProducts(); // Refresh product stock in ComboBox
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    errorMessageLabel.setText("Failed to complete purchase: " + cause.getMessage());
                    System.err.println("Purchase completion error: " + cause.getMessage());
                    // cause.printStackTrace(); // Already handled by client service
                    completePurchaseButton.setDisable(false);
                    cancelPurchaseButton.setDisable(false);
                });
                return null;
            });
    }

    private void resetPurchaseForm() {
        supplierNameField.clear();
        invoiceNumberField.clear();
        currentPurchaseItems.clear();
        calculateTotalAmount();
        updateTotalAmountDisplay();
        if (!availableProducts.isEmpty()) productComboBox.getSelectionModel().selectFirst();
        quantitySpinner.getValueFactory().setValue(1);
        costPriceSpinner.getValueFactory().setValue(1.0);
        completePurchaseButton.setDisable(false);
        cancelPurchaseButton.setDisable(false);
        clearMessages();
    }

    @FXML
    private void handleCancelPurchase() {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION, "Cancel this purchase and clear all items?", ButtonType.YES, ButtonType.NO);
        confirmDialog.setTitle("Confirm Cancel");
        confirmDialog.setHeaderText(null);
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            resetPurchaseForm();
            statusMessageLabel.setText("Purchase cancelled.");
        }
    }
}