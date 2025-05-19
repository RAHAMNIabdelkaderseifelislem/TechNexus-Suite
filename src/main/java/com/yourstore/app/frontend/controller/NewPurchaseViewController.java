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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

@Component
public class NewPurchaseViewController {

    private static final Logger logger = LoggerFactory.getLogger(NewPurchaseViewController.class);

    @FXML private Button homeButton;
    @FXML private TextField supplierNameField;
    @FXML private TextField invoiceNumberField;
    @FXML private ComboBox<ProductDto> productComboBox;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private Spinner<Double> costPriceSpinner; // For entering unit cost price
    @FXML private Button addItemButton;
    @FXML private TableView<PurchaseItemDto> purchaseItemsTableView;
    @FXML private TableColumn<PurchaseItemDto, String> productNameColumn;
    @FXML private TableColumn<PurchaseItemDto, Integer> quantityColumn;
    @FXML private TableColumn<PurchaseItemDto, BigDecimal> costPriceColumn; // Display unit cost
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
    private final ConfigurableApplicationContext springContext;

    private final ObservableList<ProductDto> availableProducts = FXCollections.observableArrayList();
    private final ObservableList<PurchaseItemDto> currentPurchaseItems = FXCollections.observableArrayList();
    private BigDecimal currentTotalAmount = BigDecimal.ZERO;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("fr", "DZ")); // Algerian Dinar


    @Autowired
    public NewPurchaseViewController(ProductClientService productClientService, PurchaseClientService purchaseClientService, StageManager stageManager, ConfigurableApplicationContext springContext) {
        this.productClientService = productClientService;
        this.purchaseClientService = purchaseClientService;
        this.stageManager = stageManager;
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        logger.info("Initializing NewPurchaseViewController.");
        // Set up Spinners for quantity and cost price
        SpinnerValueFactory.IntegerSpinnerValueFactory quantityFactory =  new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1);
        quantitySpinner.setValueFactory(quantityFactory);
        SpinnerValueFactory.DoubleSpinnerValueFactory costPriceFactory = new SpinnerValueFactory.DoubleSpinnerValueFactory(0.01, 99999999.99, 1.00, 0.01);
        costPriceSpinner.setValueFactory(costPriceFactory);
        setupProductComboBox();
        setupPurchaseItemsTable();
        loadAvailableProducts();
        updateTotalAmountDisplay();
        clearMessages();

        statusMessageLabel.managedProperty().bind(statusMessageLabel.visibleProperty());
        errorMessageLabel.managedProperty().bind(errorMessageLabel.visibleProperty());
        statusMessageLabel.setVisible(false);
        errorMessageLabel.setVisible(false);
    }
    
    private void clearMessages() {
        statusMessageLabel.setText("");
        errorMessageLabel.setText("");
        statusMessageLabel.setVisible(false);
        errorMessageLabel.setVisible(false);
    }
    
    private void showStatusMessage(String message) {
        clearMessages();
        statusMessageLabel.setText(message);
        statusMessageLabel.setVisible(true);
    }

    private void showErrorMessage(String message) {
        clearMessages();
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(true);
    }


    private void setupProductComboBox() {
        productComboBox.setItems(availableProducts);
        productComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ProductDto product) {
                // Display current purchase price if available, or a default
                String costInfo = (product != null && product.getPurchasePrice() != null) ? 
                                  currencyFormatter.format(product.getPurchasePrice()) : "N/A";
                return product != null ? String.format("%s (Stock: %d, Last Cost: %s)", 
                                         product.getName(), product.getQuantityInStock(), costInfo) 
                                       : "";
            }
            @Override
            public ProductDto fromString(String string) { return null; }
        });
        // When a product is selected, populate the costPriceSpinner with its last known purchase price
        productComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getPurchasePrice() != null) {
                costPriceSpinner.getValueFactory().setValue(newVal.getPurchasePrice().doubleValue());
            } else if (newVal != null) { // If no purchase price, set a default small value
                costPriceSpinner.getValueFactory().setValue(1.00); // Or 0.01, or leave blank
            } else {
                costPriceSpinner.getValueFactory().setValue(0.00);
            }
        });
    }

    private void loadAvailableProducts() {
        productClientService.getAllProducts()
            .thenAcceptAsync(products -> Platform.runLater(() -> {
                availableProducts.setAll(products);
                if (!products.isEmpty()) productComboBox.getSelectionModel().selectFirst();
                logger.debug("Available products loaded for New Purchase view: {}", products.size());
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    showErrorMessage("Error loading products: " + ex.getMessage());
                    logger.error("Error loading products for New Purchase view", ex);
                });
                return null;
            });
    }

    private void setupPurchaseItemsTable() {
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        costPriceColumn.setCellValueFactory(new PropertyValueFactory<>("costPrice"));
        costPriceColumn.setCellFactory(column -> new TableCell<>() {
             @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormatter.format(item));
            }
        });
        subtotalColumn.setCellValueFactory(new PropertyValueFactory<>("subtotal"));
         subtotalColumn.setCellFactory(column -> new TableCell<>() {
             @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormatter.format(item));
            }
        });
        addRemoveButtonToPurchaseTable();
        purchaseItemsTableView.setItems(currentPurchaseItems);
    }
    
    private void addRemoveButtonToPurchaseTable() {
        Callback<TableColumn<PurchaseItemDto, Void>, TableCell<PurchaseItemDto, Void>> cellFactory = param -> {
            final TableCell<PurchaseItemDto, Void> cell = new TableCell<>() {
                private final Button btn = new Button("Remove");
                {
                    btn.getStyleClass().add("button-danger");
                    btn.setOnAction(event -> {
                        PurchaseItemDto item = getTableView().getItems().get(getIndex());
                        logger.debug("Removing item from purchase: {}", item.getProductName());
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
            return cell;
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
            showErrorMessage("Please select a product.");
            return;
        }
        if (quantity <= 0) {
            showErrorMessage("Quantity must be at least 1.");
            return;
        }
        if (costPriceDouble <= 0) {
            showErrorMessage("Unit Cost Price must be greater than 0.");
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
            logger.debug("Updated quantity for existing item {} in purchase.", existingItem.getProductName());
            purchaseItemsTableView.refresh();
        } else {
            PurchaseItemDto newItem = new PurchaseItemDto();
            newItem.setProductId(selectedProduct.getId());
            newItem.setProductName(selectedProduct.getName());
            newItem.setQuantity(quantity);
            newItem.setCostPrice(costPrice);
            newItem.setSubtotal(costPrice.multiply(BigDecimal.valueOf(quantity)));
            currentPurchaseItems.add(newItem);
            logger.debug("Added new item {} to purchase.", newItem.getProductName());
        }
        
        calculateTotalAmount();
        updateTotalAmountDisplay();
        quantitySpinner.getValueFactory().setValue(1); // Reset quantity spinner
        // costPriceSpinner value is already set based on product selection or user input
        productComboBox.requestFocus();
    }
    
    private void calculateTotalAmount() {
        currentTotalAmount = BigDecimal.ZERO;
        for (PurchaseItemDto item : currentPurchaseItems) {
            currentTotalAmount = currentTotalAmount.add(item.getSubtotal());
        }
    }

    private void updateTotalAmountDisplay() {
        totalAmountLabel.setText(currencyFormatter.format(currentTotalAmount));
    }

    @FXML
    private void handleCompletePurchase() {
        clearMessages();
        if (supplierNameField.getText() == null || supplierNameField.getText().trim().isEmpty()) {
            showErrorMessage("Supplier name is required.");
            return;
        }
        if (currentPurchaseItems.isEmpty()) {
            showErrorMessage("Cannot complete purchase with no items.");
            return;
        }

        PurchaseDto newPurchase = new PurchaseDto();
        newPurchase.setSupplierName(supplierNameField.getText().trim());
        newPurchase.setInvoiceNumber(invoiceNumberField.getText().trim());
        newPurchase.setItems(new ArrayList<>(currentPurchaseItems));

        logger.info("Attempting to complete purchase with {} items, total: {}", currentPurchaseItems.size(), currentTotalAmount);
        showProgress(true, "Completing purchase...");

        purchaseClientService.createPurchase(newPurchase)
            .thenAcceptAsync(savedPurchase -> Platform.runLater(() -> {
                showProgress(false, "Purchase completed!");
                stageManager.showInfoAlert("Purchase Completed", "Purchase ID: " + savedPurchase.getId() + "\nTotal: " + currencyFormatter.format(savedPurchase.getTotalAmount()));
                resetPurchaseForm();
                loadAvailableProducts(); // Refresh product data (e.g., stock might have changed)
                logger.info("Purchase completed successfully. ID: {}", savedPurchase.getId());
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    showProgress(false, "Purchase completion failed.");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showErrorMessage("Failed to complete purchase: " + cause.getMessage());
                    logger.error("Purchase completion error: {}", cause.getMessage(), cause);
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
        else productComboBox.getSelectionModel().clearSelection();
        quantitySpinner.getValueFactory().setValue(1);
        costPriceSpinner.getValueFactory().setValue(1.00); // Reset cost price
        clearMessages();
        logger.debug("New Purchase form reset.");
    }

    @FXML
    private void handleCancelPurchase() {
        logger.debug("Cancel purchase button clicked.");
         if (currentPurchaseItems.isEmpty() && supplierNameField.getText().trim().isEmpty() && invoiceNumberField.getText().trim().isEmpty()) {
            goBackOrToDashboard();
            return;
        }
        Optional<ButtonType> result = stageManager.showConfirmationAlert(
            "Confirm Cancel Purchase",
            "Are you sure you want to cancel this purchase?",
            "All items added to the current purchase will be cleared."
        );
        if (result.isPresent() && result.get() == ButtonType.OK) {
            resetPurchaseForm();
            showStatusMessage("Purchase cancelled.");
            logger.info("Purchase cancelled by user.");
        }
    }
    
    @FXML
    private void handleGoHome() {
        logger.debug("Go Home button clicked from New Purchase view.");
        if (!currentPurchaseItems.isEmpty()) {
             Optional<ButtonType> result = stageManager.showConfirmationAlert(
                "Confirm Navigation",
                "Leave New Purchase Page?",
                "The current purchase has items. If you leave, these items will be cleared. Are you sure?"
            );
            if (result.isPresent() && result.get() == ButtonType.OK) {
                 goBackOrToDashboard();
            }
        } else {
            goBackOrToDashboard();
        }
    }

    private void goBackOrToDashboard() {
        try {
            MainViewController mainViewController = springContext.getBean(MainViewController.class);
            mainViewController.handleShowDashboard(); // Or navigate to purchases list
        } catch (Exception e) {
            logger.error("Error navigating from New Purchase: {}", e.getMessage(), e);
            stageManager.showErrorAlert("Navigation Error", "Could not return to the main dashboard.");
        }
    }
    
    private void showProgress(boolean show, String message) {
        // Similar to NewSaleView, manage button states and use specific labels for feedback
        completePurchaseButton.setDisable(show);
        addItemButton.setDisable(show);
        cancelPurchaseButton.setDisable(show && message.startsWith("Completing"));
        if(show) {
            statusMessageLabel.setText(message);
            statusMessageLabel.setVisible(true);
        }
    }
}