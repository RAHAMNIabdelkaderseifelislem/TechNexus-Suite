package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.SaleDto;
import com.yourstore.app.frontend.service.SaleClientService;
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
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.File;
// IOException removed as exportAllSalesToCsv in client service handles file writing part, this controller just gets the path
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class SalesListViewController {

    private static final Logger logger = LoggerFactory.getLogger(SalesListViewController.class);

    // --- FXML Injected Fields ---
    @FXML private TableView<SaleDto> salesTableView;
    @FXML private TableColumn<SaleDto, Long> saleIdColumn;
    @FXML private TableColumn<SaleDto, String> customerNameColumn;
    @FXML private TableColumn<SaleDto, BigDecimal> totalAmountColumn;
    @FXML private TableColumn<SaleDto, LocalDateTime> saleDateColumn;
    @FXML private TableColumn<SaleDto, String> cashierColumn;
    @FXML private TableColumn<SaleDto, String> itemsCountColumn;
    @FXML private TableColumn<SaleDto, LocalDateTime> createdAtColumn; // Optional

    @FXML private TextField searchSaleField;
    @FXML private Button newSaleButton;
    @FXML private Button refreshButton;
    @FXML private Button exportButton;
    @FXML private Button homeButton;

    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    // --- Services and Utilities ---
    private final SaleClientService saleClientService;
    private final StageManager stageManager;
    private final ConfigurableApplicationContext springContext;

    // --- Data Lists ---
    private final ObservableList<SaleDto> salesMasterList = FXCollections.observableArrayList();
    private FilteredList<SaleDto> filteredSalesData;

    // --- Formatters ---
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter searchDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @Autowired
    public SalesListViewController(SaleClientService saleClientService, StageManager stageManager, ConfigurableApplicationContext springContext) {
        this.saleClientService = saleClientService;
        this.stageManager = stageManager;
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        logger.info("Initializing SalesListViewController.");
        showProgress(false, "Ready.");

        filteredSalesData = new FilteredList<>(salesMasterList, p -> true);

        searchSaleField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredSalesData.setPredicate(sale -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();

                if (String.valueOf(sale.getId()).contains(lowerCaseFilter)) return true;
                if (sale.getCustomerName() != null && sale.getCustomerName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (sale.getUsername() != null && sale.getUsername().toLowerCase().contains(lowerCaseFilter)) return true;
                if (sale.getTotalAmount() != null && sale.getTotalAmount().toPlainString().contains(lowerCaseFilter)) return true;
                if (sale.getSaleDate() != null) {
                    if (dateTimeFormatter.format(sale.getSaleDate()).toLowerCase().contains(lowerCaseFilter)) return true;
                    if (searchDateFormatter.format(sale.getSaleDate()).toLowerCase().contains(lowerCaseFilter)) return true; // Search by date part only
                }
                return false;
            });
        });

        SortedList<SaleDto> sortedData = new SortedList<>(filteredSalesData);
        sortedData.comparatorProperty().bind(salesTableView.comparatorProperty());
        salesTableView.setItems(sortedData);

        setupTableColumns();
        loadSales();
    }

    private void setupTableColumns() {
        logger.debug("Setting up sales table columns.");
        saleIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        cashierColumn.setCellValueFactory(new PropertyValueFactory<>("username")); // From SaleDto.username
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));


        saleDateColumn.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        saleDateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : dateTimeFormatter.format(item));
            }
        });
        createdAtColumn.setCellFactory(column -> new TableCell<>() { // Formatter for createdAt if shown
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
    private void handleRefreshSales() {
        logger.info("Refresh sales button clicked.");
        searchSaleField.clear();
        loadSales();
    }

    private void loadSales() {
        showProgress(true, "Loading sales records...");
        saleClientService.getAllSales()
            .thenAcceptAsync(sales -> Platform.runLater(() -> {
                salesMasterList.setAll(sales);
                showProgress(false, "Sales loaded. Found " + salesMasterList.size() + " records.");
                logger.info("Loaded {} sales into master list.", salesMasterList.size());
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    salesMasterList.clear();
                    showProgress(false, "Error loading sales.");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    // Alert is handled by SaleClientService's handleHttpError or exceptionally block
                    logger.error("Error loading sales: {}", cause.getMessage(), cause);
                });
                return null;
            });
    }

    @FXML
    private void handleNewSale() {
        logger.info("New Sale button clicked from Sales List.");
        try {
            MainViewController mainViewController = springContext.getBean(MainViewController.class);
            mainViewController.loadCenterView("/fxml/NewSaleView.fxml");
        } catch (Exception e) {
            logger.error("Error navigating to New Sale view: {}", e.getMessage(), e);
            stageManager.showErrorAlert("Navigation Error", "Could not open the New Sale screen.");
        }
    }

    @FXML
    private void handleExportSales() {
        logger.info("Export sales to CSV button clicked.");
        // The client service method now handles file download and returns Path
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save All Sales Data as CSV");
        fileChooser.setInitialFileName("all_sales_export_" + System.currentTimeMillis() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        
        File file = fileChooser.showSaveDialog(stageManager.getPrimaryStage());

        if (file != null) {
            Path targetPath = file.toPath();
            showProgress(true, "Exporting all sales data to CSV...");

            saleClientService.exportAllSalesToCsv(targetPath)
                .thenAcceptAsync(downloadedPath -> Platform.runLater(() -> {
                    showProgress(false, "Sales data exported successfully.");
                    stageManager.showInfoAlert("Export Successful", "All sales data exported to:\n" + downloadedPath.toString());
                    logger.info("Sales data exported to CSV: {}", downloadedPath.toString());
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showProgress(false, "Sales data export failed.");
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        stageManager.showErrorAlert("Export Failed", "Could not export sales data: " + cause.getMessage());
                        logger.error("Failed to export sales data: {}", cause.getMessage(), cause);
                    });
                    return null;
                });
        } else {
            showProgress(false, "CSV Export cancelled.");
        }
    }

    @FXML
    private void handleGoHome() {
        logger.debug("Go Home button clicked from Sales List.");
        try {
            MainViewController mainViewController = springContext.getBean(MainViewController.class);
            mainViewController.handleShowDashboard(); // Navigate to Dashboard which is home
        } catch (Exception e) {
            logger.error("Error navigating to home (dashboard) from Sales List: {}", e.getMessage(), e);
            stageManager.showErrorAlert("Navigation Error", "Could not return to the main dashboard.");
        }
    }
    
    private void showProgress(boolean show, String message) {
        if (progressIndicator != null) progressIndicator.setVisible(show);
        if (statusLabel != null) statusLabel.setText(message != null ? message : "");
    }
}