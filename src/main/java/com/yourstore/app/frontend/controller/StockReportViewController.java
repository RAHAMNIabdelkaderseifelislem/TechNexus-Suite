package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.reports.StockReportItemDto;
import com.yourstore.app.backend.model.enums.ProductCategory;
import com.yourstore.app.frontend.service.ReportClientService;
import com.yourstore.app.frontend.util.StageManager;
import javafx.application.Platform;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Component
public class StockReportViewController {
    private static final Logger logger = LoggerFactory.getLogger(StockReportViewController.class);

    @FXML private Button homeButton;
    @FXML private TextField filterField;
    @FXML private Button refreshButton; // Renamed from generateReportButton for consistency
    @FXML private Button exportCsvButton;
    @FXML private TableView<StockReportItemDto> stockReportTableView;
    @FXML private TableColumn<StockReportItemDto, Long> productIdColumn;
    @FXML private TableColumn<StockReportItemDto, String> nameColumn;
    @FXML private TableColumn<StockReportItemDto, ProductCategory> categoryColumn;
    @FXML private TableColumn<StockReportItemDto, String> supplierColumn;
    @FXML private TableColumn<StockReportItemDto, BigDecimal> purchasePriceColumn;
    @FXML private TableColumn<StockReportItemDto, BigDecimal> sellingPriceColumn;
    @FXML private TableColumn<StockReportItemDto, Integer> quantityColumn;
    @FXML private TableColumn<StockReportItemDto, BigDecimal> stockValueColumn;
    @FXML private TableColumn<StockReportItemDto, BigDecimal> potentialRevenueColumn;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label totalStockValueLabel;
    @FXML private Label totalPotentialRevenueLabel;


    private final ReportClientService reportClientService;
    private final StageManager stageManager;
    private final ConfigurableApplicationContext springContext;
    private final ObservableList<StockReportItemDto> masterReportData = FXCollections.observableArrayList();
    private FilteredList<StockReportItemDto> filteredReportData;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("fr", "DZ"));


    @Autowired
    public StockReportViewController(ReportClientService reportClientService, StageManager stageManager, ConfigurableApplicationContext springContext) {
        this.reportClientService = reportClientService;
        this.stageManager = stageManager;
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        filteredReportData = new FilteredList<>(masterReportData, p -> true);
        filterField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        SortedList<StockReportItemDto> sortedData = new SortedList<>(filteredReportData);
        sortedData.comparatorProperty().bind(stockReportTableView.comparatorProperty());
        stockReportTableView.setItems(sortedData);
        exportCsvButton.setDisable(true);
        handleGenerateReport(); // Load data on initialization
    }

    private void setupTableColumns() {
        productIdColumn.setCellValueFactory(new PropertyValueFactory<>("productId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        purchasePriceColumn.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        sellingPriceColumn.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantityInStock"));
        stockValueColumn.setCellValueFactory(new PropertyValueFactory<>("stockValueByPurchasePrice"));
        potentialRevenueColumn.setCellValueFactory(new PropertyValueFactory<>("potentialRevenueAtSellingPrice"));

        // Currency formatting for relevant columns
        formatCurrencyColumn(purchasePriceColumn);
        formatCurrencyColumn(sellingPriceColumn);
        formatCurrencyColumn(stockValueColumn);
        formatCurrencyColumn(potentialRevenueColumn);
    }

    private void formatCurrencyColumn(TableColumn<StockReportItemDto, BigDecimal> column) {
        column.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : currencyFormatter.format(item));
            }
        });
    }
    
    private void applyFilter() {
        String filterText = filterField.getText();
        if (filterText == null || filterText.isEmpty()) {
            filteredReportData.setPredicate(p -> true);
        } else {
            String lowerCaseFilter = filterText.toLowerCase();
            filteredReportData.setPredicate(item -> {
                if (item.getName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (item.getCategory() != null && item.getCategory().toString().toLowerCase().contains(lowerCaseFilter)) return true;
                if (item.getSupplier() != null && item.getSupplier().toLowerCase().contains(lowerCaseFilter)) return true;
                if (String.valueOf(item.getProductId()).contains(lowerCaseFilter)) return true;
                return false;
            });
        }
        calculateAndDisplayTotals(); // Recalculate totals based on filtered data
    }


    @FXML
    private void handleGenerateReport() {
        showProgress(true, "Generating stock report...");
        reportClientService.getCurrentStockReport()
            .thenAcceptAsync(stockItems -> Platform.runLater(() -> {
                masterReportData.setAll(stockItems);
                applyFilter(); // Apply filter which also calculates totals
                showProgress(false, "Stock report generated. Displaying " + filteredReportData.size() + " items.");
                exportCsvButton.setDisable(masterReportData.isEmpty());
                 if (masterReportData.isEmpty()) {
                    stockReportTableView.setPlaceholder(new Label("No stock data found."));
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    showProgress(false, "Failed to generate report.");
                    stageManager.showErrorAlert("Report Error", "Could not fetch stock report: " + ex.getMessage());
                    logger.error("Error generating stock report", ex);
                    exportCsvButton.setDisable(true);
                });
                return null;
            });
    }

    private void calculateAndDisplayTotals() {
        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (StockReportItemDto item : filteredReportData) { // Calculate based on filtered data
            if (item.getStockValueByPurchasePrice() != null) {
                totalValue = totalValue.add(item.getStockValueByPurchasePrice());
            }
            if (item.getPotentialRevenueAtSellingPrice() != null) {
                totalRevenue = totalRevenue.add(item.getPotentialRevenueAtSellingPrice());
            }
        }
        totalStockValueLabel.setText(currencyFormatter.format(totalValue));
        totalPotentialRevenueLabel.setText(currencyFormatter.format(totalRevenue));
    }


    @FXML
    private void handleExportToCsv() {
         if (filteredReportData.isEmpty()) { // Export filtered data
            stageManager.showInfoAlert("No Data", "There is no data to export from the current view.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Stock Report as CSV");
        fileChooser.setInitialFileName("stock_report_" + System.currentTimeMillis() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(stageManager.getPrimaryStage());

        if (file != null) {
            showProgress(true, "Exporting to CSV...");
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("ProductID,Name,Category,Supplier,PurchasePrice,SellingPrice,QuantityInStock,StockValueAtCost,PotentialRevenue");
                for (StockReportItemDto item : filteredReportData) { // Export filtered data
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%d\",\"%s\",\"%s\"\n",
                            escapeCsv(item.getProductId()),
                            escapeCsv(item.getName()),
                            escapeCsv(item.getCategory()),
                            escapeCsv(item.getSupplier()),
                            escapeCsv(item.getPurchasePrice()),
                            escapeCsv(item.getSellingPrice()),
                            item.getQuantityInStock(),
                            escapeCsv(item.getStockValueByPurchasePrice()),
                            escapeCsv(item.getPotentialRevenueAtSellingPrice())
                    );
                }
                showProgress(false, "Export successful.");
                stageManager.showInfoAlert("Export Successful", "Report exported to:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showProgress(false, "Export failed.");
                stageManager.showErrorAlert("Export Error", "Could not write CSV file: " + e.getMessage());
                logger.error("Error exporting stock report to CSV", e);
            }
        }
    }
     private String escapeCsv(Object value) {
        if (value == null) return "";
        String stringValue = value.toString();
        if (stringValue.contains("\"") || stringValue.contains(",") || stringValue.contains("\n") || stringValue.contains("\r")) {
            return "\"" + stringValue.replace("\"", "\"\"") + "\"";
        }
        return stringValue;
    }

    @FXML
    private void handleGoHome() {
         try {
            springContext.getBean(MainViewController.class).handleShowDashboard();
        } catch (Exception e) {
            logger.error("Error navigating to home from stock report: {}", e.getMessage(), e);
            stageManager.showErrorAlert("Navigation Error", "Could not return to dashboard.");
        }
    }

    private void showProgress(boolean show, String message) {
        if (progressIndicator != null) progressIndicator.setVisible(show);
        if (statusLabel != null) statusLabel.setText(message != null ? message : "");
        refreshButton.setDisable(show);
        exportCsvButton.setDisable(show || masterReportData.isEmpty());
    }
}