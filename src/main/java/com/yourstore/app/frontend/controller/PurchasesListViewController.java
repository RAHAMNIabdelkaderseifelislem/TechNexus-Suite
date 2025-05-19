package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.PurchaseDto;
import com.yourstore.app.frontend.service.PurchaseClientService;
import com.yourstore.app.frontend.util.StageManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class PurchasesListViewController {

    private static final Logger logger = LoggerFactory.getLogger(PurchasesListViewController.class);

    // --- FXML Injected Fields ---
    @FXML private TableView<PurchaseDto> purchasesTableView;
    @FXML private TableColumn<PurchaseDto, Long> purchaseIdColumn;
    @FXML private TableColumn<PurchaseDto, String> supplierNameColumn;
    @FXML private TableColumn<PurchaseDto, String> invoiceNumberColumn;
    @FXML private TableColumn<PurchaseDto, BigDecimal> totalAmountColumn;
    @FXML private TableColumn<PurchaseDto, LocalDateTime> purchaseDateColumn;
    @FXML private TableColumn<PurchaseDto, String> recordedByColumn;
    @FXML private TableColumn<PurchaseDto, String> itemsCountColumn;
    @FXML private TableColumn<PurchaseDto, LocalDateTime> createdAtColumn;

    @FXML private TextField searchPurchaseField;
    @FXML private Button newPurchaseButton;
    @FXML private Button refreshButton;
    // @FXML private Button exportPurchasesButton; // For future CSV export
    @FXML private Button homeButton;

    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    // --- Services and Utilities ---
    private final PurchaseClientService purchaseClientService;
    private final StageManager stageManager;
    private final ConfigurableApplicationContext springContext;

    // --- Data Lists ---
    private final ObservableList<PurchaseDto> purchasesMasterList = FXCollections.observableArrayList();
    private FilteredList<PurchaseDto> filteredPurchasesData;

    // --- Formatters ---
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter searchDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public PurchasesListViewController(PurchaseClientService purchaseClientService, StageManager stageManager, ConfigurableApplicationContext springContext) {
        this.purchaseClientService = purchaseClientService;
        this.stageManager = stageManager;
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        logger.info("Initializing PurchasesListViewController.");
        showProgress(false, "Ready.");

        filteredPurchasesData = new FilteredList<>(purchasesMasterList, p -> true);

        searchPurchaseField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredPurchasesData.setPredicate(purchase -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();

                if (String.valueOf(purchase.getId()).contains(lowerCaseFilter)) return true;
                if (purchase.getSupplierName() != null && purchase.getSupplierName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (purchase.getInvoiceNumber() != null && purchase.getInvoiceNumber().toLowerCase().contains(lowerCaseFilter)) return true;
                if (purchase.getUsername() != null && purchase.getUsername().toLowerCase().contains(lowerCaseFilter)) return true; // Recorded By
                if (purchase.getTotalAmount() != null && purchase.getTotalAmount().toPlainString().contains(lowerCaseFilter)) return true;
                if (purchase.getPurchaseDate() != null) {
                    if (dateTimeFormatter.format(purchase.getPurchaseDate()).toLowerCase().contains(lowerCaseFilter)) return true;
                    if (searchDateFormatter.format(purchase.getPurchaseDate()).toLowerCase().contains(lowerCaseFilter)) return true;
                }
                return false;
            });
        });

        SortedList<PurchaseDto> sortedData = new SortedList<>(filteredPurchasesData);
        sortedData.comparatorProperty().bind(purchasesTableView.comparatorProperty());
        purchasesTableView.setItems(sortedData);

        setupTableColumns();
        loadPurchases();
    }

    private void setupTableColumns() {
        logger.debug("Setting up purchases table columns.");
        purchaseIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        supplierNameColumn.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        invoiceNumberColumn.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        recordedByColumn.setCellValueFactory(new PropertyValueFactory<>("username")); // From PurchaseDto.username
        
        purchaseDateColumn.setCellValueFactory(new PropertyValueFactory<>("purchaseDate"));
        purchaseDateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : dateTimeFormatter.format(item));
            }
        });
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdAtColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : dateTimeFormatter.format(item));
            }
        });
        
        itemsCountColumn.setCellValueFactory(cellData -> {
            int count = cellData.getValue().getItems() != null ? cellData.getValue().getItems().size() : 0;
            return new SimpleStringProperty(String.valueOf(count));
        });
    }

    @FXML
    private void handleRefreshPurchases() {
        logger.info("Refresh purchases button clicked.");
        searchPurchaseField.clear();
        loadPurchases();
    }

    private void loadPurchases() {
        showProgress(true, "Loading purchase records...");
        purchaseClientService.getAllPurchases()
            .thenAcceptAsync(purchases -> Platform.runLater(() -> {
                purchasesMasterList.setAll(purchases);
                showProgress(false, "Purchases loaded. Found " + purchasesMasterList.size() + " records.");
                logger.info("Loaded {} purchases into master list.", purchasesMasterList.size());
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    purchasesMasterList.clear();
                    showProgress(false, "Error loading purchases.");
                    // Alert is handled by PurchaseClientService
                    logger.error("Error loading purchases: {}", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(), ex);
                });
                return null;
            });
    }

    @FXML
    private void handleNewPurchase() {
        logger.info("New Purchase button clicked from Purchases List.");
        try {
            MainViewController mainViewController = springContext.getBean(MainViewController.class);
            mainViewController.loadCenterView("/fxml/NewPurchaseView.fxml");
        } catch (Exception e) {
            logger.error("Error navigating to New Purchase view: {}", e.getMessage(), e);
            stageManager.showErrorAlert("Navigation Error", "Could not open the New Purchase screen.");
        }
    }

    /*
    @FXML
    private void handleExportPurchases() {
        // TODO: Implement CSV export for purchases similar to sales/products
        logger.info("Export Purchases (CSV) button clicked - Not implemented yet.");
        stageManager.showInfoAlert("Coming Soon", "Exporting purchases to CSV will be implemented in a future update.");
    }
    */

    @FXML
    private void handleGoHome() {
        logger.debug("Go Home button clicked from Purchases List.");
        try {
            MainViewController mainViewController = springContext.getBean(MainViewController.class);
            mainViewController.handleShowDashboard(); // Navigate to Dashboard
        } catch (Exception e) {
            logger.error("Error navigating to home (dashboard) from Purchases List: {}", e.getMessage(), e);
            stageManager.showErrorAlert("Navigation Error", "Could not return to the main dashboard.");
        }
    }
    
    private void showProgress(boolean show, String message) {
        if (progressIndicator != null) progressIndicator.setVisible(show);
        if (statusLabel != null) statusLabel.setText(message != null ? message : "");
    }
}