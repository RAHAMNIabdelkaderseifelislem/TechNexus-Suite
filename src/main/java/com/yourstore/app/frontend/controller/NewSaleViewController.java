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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

@Component
public class NewSaleViewController {

    private static final Logger logger = LoggerFactory.getLogger(NewSaleViewController.class);

    @FXML private Button homeButton;
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
    private final StageManager stageManager;
    private final ConfigurableApplicationContext springContext;

    private final ObservableList<ProductDto> availableProducts = FXCollections.observableArrayList();
    private final ObservableList<SaleItemDto> currentSaleItems = FXCollections.observableArrayList();
    private BigDecimal currentTotalAmount = BigDecimal.ZERO;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US")); // Or your desired locale

    @Autowired
    public NewSaleViewController(ProductClientService productClientService, SaleClientService saleClientService, StageManager stageManager, ConfigurableApplicationContext springContext) {
        this.productClientService = productClientService;
        this.saleClientService = saleClientService;
        this.stageManager = stageManager;
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        logger.info("Initializing NewSaleViewController.");
        setupProductComboBox();
        setupSaleItemsTable();
        loadAvailableProducts();
        updateTotalAmountDisplay();
        clearMessages();

        // Ensure error/status labels don't take up space when empty
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
                return product != null ? String.format("%s (Stock: %d, Price: %s)", 
                                         product.getName(), 
                                         product.getQuantityInStock(),
                                         currencyFormatter.format(product.getSellingPrice())) 
                                       : "";
            }
            @Override
            public ProductDto fromString(String string) { return null; }
        });
        // Consider adding an Autocomplete/Filtering mechanism to ComboBox later for many products.
    }

    private void loadAvailableProducts() {
        productClientService.getAllProducts()
            .thenAcceptAsync(products -> Platform.runLater(() -> {
                availableProducts.setAll(products);
                if (!products.isEmpty()) productComboBox.getSelectionModel().selectFirst();
                logger.debug("Available products loaded for New Sale view: {}", products.size());
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    showErrorMessage("Error loading products: " + ex.getMessage());
                    logger.error("Error loading products for New Sale view", ex);
                });
                return null;
            });
    }

    private void setupSaleItemsTable() {
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceAtSaleColumn.setCellValueFactory(new PropertyValueFactory<>("priceAtSale"));
        priceAtSaleColumn.setCellFactory(column -> new TableCell<>() {
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
        addRemoveButtonToTable();
        saleItemsTableView.setItems(currentSaleItems);
    }
    
    private void addRemoveButtonToTable() {
        Callback<TableColumn<SaleItemDto, Void>, TableCell<SaleItemDto, Void>> cellFactory = param -> {
            final TableCell<SaleItemDto, Void> cell = new TableCell<>() {
                private final Button btn = new Button("Remove");
                {
                    btn.getStyleClass().add("button-danger"); // Style as danger
                    btn.setOnAction(event -> {
                        SaleItemDto item = getTableView().getItems().get(getIndex());
                        logger.debug("Removing item from sale: {}", item.getProductName());
                        currentSaleItems.remove(item);
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
    private void handleAddItemToSale() {
        clearMessages();
        ProductDto selectedProduct = productComboBox.getSelectionModel().getSelectedItem();
        int quantityToAdd = quantitySpinner.getValue();

        if (selectedProduct == null) {
            showErrorMessage("Please select a product to add.");
            return;
        }
        if (quantityToAdd <= 0) {
            showErrorMessage("Quantity must be at least 1.");
            return;
        }
        
        Optional<SaleItemDto> existingItemOpt = currentSaleItems.stream()
            .filter(item -> item.getProductId().equals(selectedProduct.getId()))
            .findFirst();

        int currentQuantityInCart = existingItemOpt.map(SaleItemDto::getQuantity).orElse(0);
        int totalQuantityNeeded = existingItemOpt.isPresent() ? currentQuantityInCart + quantityToAdd : quantityToAdd;
        
        if (selectedProduct.getQuantityInStock() < (totalQuantityNeeded - (existingItemOpt.isPresent() ? currentQuantityInCart : 0))) {
             showErrorMessage("Not enough stock for " + selectedProduct.getName() + 
                             ". Available: " + selectedProduct.getQuantityInStock() + 
                             ", In Cart: " + currentQuantityInCart + 
                             ", Requested to add: " + quantityToAdd);
            return;
        }


        if (existingItemOpt.isPresent()) {
            SaleItemDto existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + quantityToAdd);
            existingItem.setSubtotal(selectedProduct.getSellingPrice().multiply(BigDecimal.valueOf(existingItem.getQuantity())));
            logger.debug("Updated quantity for existing item {} in sale.", existingItem.getProductName());
            saleItemsTableView.refresh();
        } else {
            SaleItemDto newItem = new SaleItemDto();
            newItem.setProductId(selectedProduct.getId());
            newItem.setProductName(selectedProduct.getName());
            newItem.setQuantity(quantityToAdd);
            newItem.setPriceAtSale(selectedProduct.getSellingPrice());
            newItem.setSubtotal(selectedProduct.getSellingPrice().multiply(BigDecimal.valueOf(quantityToAdd)));
            currentSaleItems.add(newItem);
            logger.debug("Added new item {} to sale.", newItem.getProductName());
        }
        
        calculateTotalAmount();
        updateTotalAmountDisplay();
        quantitySpinner.getValueFactory().setValue(1); // Reset spinner
        productComboBox.requestFocus(); // Move focus back to product selection
    }
    
    private void calculateTotalAmount() {
        currentTotalAmount = BigDecimal.ZERO;
        for (SaleItemDto item : currentSaleItems) {
            currentTotalAmount = currentTotalAmount.add(item.getSubtotal());
        }
    }

    private void updateTotalAmountDisplay() {
        totalAmountLabel.setText(currencyFormatter.format(currentTotalAmount));
    }

    @FXML
    private void handleCompleteSale() {
        clearMessages();
        if (currentSaleItems.isEmpty()) {
            showErrorMessage("Cannot complete sale with no items.");
            return;
        }

        SaleDto newSale = new SaleDto();
        newSale.setCustomerName(customerNameField.getText().trim());
        newSale.setItems(new ArrayList<>(currentSaleItems)); // Convert ObservableList

        logger.info("Attempting to complete sale with {} items, total: {}", currentSaleItems.size(), currentTotalAmount);
        showProgress(true, "Completing sale...");

        saleClientService.createSale(newSale)
            .thenAcceptAsync(savedSale -> Platform.runLater(() -> {
                showProgress(false, "Sale completed!");
                stageManager.showInfoAlert("Sale Completed", "Sale ID: " + savedSale.getId() + "\nTotal: " + currencyFormatter.format(savedSale.getTotalAmount()));
                resetSaleForm();
                loadAvailableProducts(); // Refresh product stock in ComboBox
                logger.info("Sale completed successfully. ID: {}", savedSale.getId());
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    showProgress(false, "Sale completion failed.");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showErrorMessage("Failed to complete sale: " + cause.getMessage());
                    logger.error("Sale completion error: {}", cause.getMessage(), cause);
                });
                return null;
            });
    }

    private void resetSaleForm() {
        customerNameField.clear();
        currentSaleItems.clear();
        calculateTotalAmount();
        updateTotalAmountDisplay();
        if (!availableProducts.isEmpty()) productComboBox.getSelectionModel().selectFirst();
        else productComboBox.getSelectionModel().clearSelection();
        quantitySpinner.getValueFactory().setValue(1);
        clearMessages();
        logger.debug("New Sale form reset.");
    }

    @FXML
    private void handleCancelSale() {
        logger.debug("Cancel sale button clicked.");
        if (currentSaleItems.isEmpty() && customerNameField.getText().trim().isEmpty()) {
            // If form is empty, just go back or clear
            goBackOrToDashboard();
            return;
        }
        Optional<ButtonType> result = stageManager.showConfirmationAlert(
            "Confirm Cancel Sale",
            "Are you sure you want to cancel this sale?",
            "All items added to the current sale will be cleared."
        );
        if (result.isPresent() && result.get() == ButtonType.OK) {
            resetSaleForm();
            showStatusMessage("Sale cancelled.");
            logger.info("Sale cancelled by user.");
        }
    }

    @FXML
    private void handleGoHome() {
        logger.debug("Go Home button clicked from New Sale view.");
        // If there are items, confirm before leaving
        if (!currentSaleItems.isEmpty()) {
             Optional<ButtonType> result = stageManager.showConfirmationAlert(
                "Confirm Navigation",
                "Leave New Sale Page?",
                "The current sale has items. If you leave, these items will be cleared. Are you sure?"
            );
            if (result.isPresent() && result.get() == ButtonType.OK) {
                 goBackOrToDashboard();
            } // else do nothing, stay on page
        } else {
            goBackOrToDashboard();
        }
    }
    
    private void goBackOrToDashboard() {
        try {
            MainViewController mainViewController = springContext.getBean(MainViewController.class);
            mainViewController.handleShowDashboard(); // Or navigate to sales list: mainViewController.loadCenterView("/fxml/SalesListView.fxml");
        } catch (Exception e) {
            logger.error("Error navigating from New Sale: {}", e.getMessage(), e);
            stageManager.showErrorAlert("Navigation Error", "Could not return to the previous screen.");
        }
    }
    
    private void showProgress(boolean show, String message) {
        // This view doesn't have a global progress indicator/status label
        // Feedback is through errorMessageLabel and statusMessageLabel
        // We can disable buttons during async operations
        completeSaleButton.setDisable(show);
        addItemButton.setDisable(show); // Could also disable product ComboBox and quantity
        cancelSaleButton.setDisable(show && message.startsWith("Completing")); // Only disable cancel during final save
        if(show) {
            statusMessageLabel.setText(message);
            statusMessageLabel.setVisible(true);
        }
    }
}