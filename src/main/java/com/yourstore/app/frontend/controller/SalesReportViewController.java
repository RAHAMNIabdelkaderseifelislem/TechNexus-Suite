package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.SaleDto;
import com.yourstore.app.frontend.service.ReportClientService; // To be created
import com.yourstore.app.frontend.util.StageManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class SalesReportViewController {
    private static final Logger logger = LoggerFactory.getLogger(SalesReportViewController.class);

    @FXML private Button homeButton;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button generateReportButton;
    @FXML private Button exportCsvButton;
    @FXML private TableView<SaleDto> salesReportTableView;
    @FXML private TableColumn<SaleDto, Long> saleIdColumn;
    @FXML private TableColumn<SaleDto, String> customerNameColumn;
    @FXML private TableColumn<SaleDto, BigDecimal> totalAmountColumn;
    @FXML private TableColumn<SaleDto, LocalDateTime> saleDateColumn;
    @FXML private TableColumn<SaleDto, String> cashierColumn;
    @FXML private TableColumn<SaleDto, String> itemsCountColumn;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    private final ReportClientService reportClientService;
    private final StageManager stageManager;
    private final ConfigurableApplicationContext springContext;
    private final ObservableList<SaleDto> reportData = FXCollections.observableArrayList();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public SalesReportViewController(ReportClientService reportClientService, StageManager stageManager, ConfigurableApplicationContext springContext) {
        this.reportClientService = reportClientService;
        this.stageManager = stageManager;
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        startDatePicker.setValue(LocalDate.now().minusDays(7)); // Default to last 7 days
        endDatePicker.setValue(LocalDate.now());
        setupTableColumns();
        salesReportTableView.setItems(reportData);
        exportCsvButton.setDisable(true);
    }

    private void setupTableColumns() {
        saleIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        cashierColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        saleDateColumn.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        saleDateColumn.setCellFactory(column -> new TableCell<>() {
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
    private void handleGenerateReport() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();

        if (start == null || end == null) {
            stageManager.showErrorAlert("Validation Error", "Please select both start and end dates.");
            return;
        }
        if (end.isBefore(start)) {
            stageManager.showErrorAlert("Validation Error", "End date cannot be before start date.");
            return;
        }

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        showProgress(true, "Generating sales report...");
        reportClientService.getDetailedSalesReport(startDateTime, endDateTime)
            .thenAcceptAsync(sales -> Platform.runLater(() -> {
                reportData.setAll(sales);
                showProgress(false, "Report generated. Found " + sales.size() + " sales records.");
                exportCsvButton.setDisable(sales.isEmpty());
                if (sales.isEmpty()) {
                    salesReportTableView.setPlaceholder(new Label("No sales found for the selected period."));
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    showProgress(false, "Failed to generate report.");
                    stageManager.showErrorAlert("Report Error", "Could not fetch sales report: " + ex.getMessage());
                    logger.error("Error generating sales report", ex);
                    exportCsvButton.setDisable(true);
                });
                return null;
            });
    }

    @FXML
    private void handleExportToCsv() {
        if (reportData.isEmpty()) {
            stageManager.showInfoAlert("No Data", "There is no data to export.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Sales Report as CSV");
        fileChooser.setInitialFileName("sales_report_" + System.currentTimeMillis() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(stageManager.getPrimaryStage());

        if (file != null) {
            showProgress(true, "Exporting to CSV...");
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // Header
                writer.println("SaleID,CustomerName,TotalAmount,SaleDate,Cashier,ItemsCount");
                // Data
                for (SaleDto sale : reportData) {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%d\"\n",
                            escapeCsv(sale.getId()),
                            escapeCsv(sale.getCustomerName()),
                            escapeCsv(sale.getTotalAmount()),
                            escapeCsv(sale.getSaleDate() != null ? dateTimeFormatter.format(sale.getSaleDate()) : ""),
                            escapeCsv(sale.getUsername()),
                            sale.getItems() != null ? sale.getItems().size() : 0
                    );
                }
                showProgress(false, "Export successful.");
                stageManager.showInfoAlert("Export Successful", "Report exported to:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                showProgress(false, "Export failed.");
                stageManager.showErrorAlert("Export Error", "Could not write CSV file: " + e.getMessage());
                logger.error("Error exporting sales report to CSV", e);
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
            logger.error("Error navigating to home from sales report: {}", e.getMessage(), e);
            stageManager.showErrorAlert("Navigation Error", "Could not return to dashboard.");
        }
    }

    private void showProgress(boolean show, String message) {
        if (progressIndicator != null) progressIndicator.setVisible(show);
        if (statusLabel != null) statusLabel.setText(message != null ? message : "");
        generateReportButton.setDisable(show);
        exportCsvButton.setDisable(show || reportData.isEmpty());
    }
}