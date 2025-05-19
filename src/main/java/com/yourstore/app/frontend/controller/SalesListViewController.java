package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.SaleDto;
import com.yourstore.app.frontend.service.SaleClientService;
import com.yourstore.app.frontend.util.StageManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.scene.Parent;    // For loaded root
import org.springframework.context.ConfigurableApplicationContext; // To get beans for controller factory

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path; // For Path
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class SalesListViewController {

    @FXML private TableView<SaleDto> salesTableView;
    @FXML private TableColumn<SaleDto, Long> saleIdColumn;
    @FXML private TableColumn<SaleDto, String> customerNameColumn;
    @FXML private TableColumn<SaleDto, BigDecimal> totalAmountColumn;
    @FXML private TableColumn<SaleDto, LocalDateTime> saleDateColumn;
    @FXML private TableColumn<SaleDto, String> cashierColumn;
    @FXML private TableColumn<SaleDto, String> itemsColumn; // To display item count or summary
    @FXML private TextField searchSaleField;

    @FXML private Button exportButton;
    private final StageManager stageManager;


    @FXML private Button refreshButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;
    @FXML private Button homeButton;

    private final SaleClientService saleClientService;
    private final ObservableList<SaleDto> salesList = FXCollections.observableArrayList();
    private final ObservableList<SaleDto> salesMasterList = FXCollections.observableArrayList();
    private FilteredList<SaleDto> filteredSalesData;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ConfigurableApplicationContext springContext; // To get controller beans

    // Updated Constructor
    @Autowired
    public SalesListViewController(SaleClientService saleClientService, StageManager stageManager, ConfigurableApplicationContext springContext) {
        this.saleClientService = saleClientService;
        this.stageManager = stageManager;
        this.springContext = springContext; // Initialize
    }


    @FXML
    public void initialize() {
        setupTableColumns();

        filteredSalesData = new FilteredList<>(salesMasterList, p -> true);

        searchSaleField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredSalesData.setPredicate(sale -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();

                if (String.valueOf(sale.getId()).contains(lowerCaseFilter)) return true;
                if (sale.getCustomerName() != null && sale.getCustomerName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (sale.getUsername() != null && sale.getUsername().toLowerCase().contains(lowerCaseFilter)) return true;
                if (sale.getSaleDate() != null && dateTimeFormatter.format(sale.getSaleDate()).toLowerCase().contains(lowerCaseFilter)) return true;
                // Add more fields to search if needed, e.g., total amount (convert to string)
                if (sale.getTotalAmount() != null && sale.getTotalAmount().toPlainString().contains(lowerCaseFilter)) return true;
                
                return false;
            });
        });

        SortedList<SaleDto> sortedData = new SortedList<>(filteredSalesData);
        sortedData.comparatorProperty().bind(salesTableView.comparatorProperty());
        salesTableView.setItems(sortedData);

        loadSales();
    }

    private void setupTableColumns() {
        saleIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        cashierColumn.setCellValueFactory(new PropertyValueFactory<>("username")); // From SaleDto.username

        saleDateColumn.setCellValueFactory(new PropertyValueFactory<>("saleDate"));
        saleDateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : dateTimeFormatter.format(item));
            }
        });
        
        itemsColumn.setCellValueFactory(cellData -> {
            int count = cellData.getValue().getItems() != null ? cellData.getValue().getItems().size() : 0;
            return new javafx.beans.property.SimpleStringProperty(count + " items");
        });


        salesTableView.setItems(salesList);
    }

    @FXML
    private void handleRefreshSales() {
        loadSales();
    }

    private void loadSales() {
        showProgress(true, "Loading sales records...");
        saleClientService.getAllSales()
            .thenAcceptAsync(sales -> Platform.runLater(() -> {
                salesMasterList.setAll(sales); // Update master list
                showProgress(false, "Sales loaded. Found " + salesMasterList.size() + " records.");
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    System.err.println("Error loading sales: " + ex.getMessage());
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showProgress(false, "Error loading sales: " + cause.getMessage());
                    // Error alert is handled by SaleClientService's handleHttpError or exceptionally block
                });
                return null;
            });
    }

    private void showProgress(boolean show, String message) {
        progressIndicator.setVisible(show);
        statusLabel.setText(message != null ? message : "");
    }

    @FXML
    private void handleNewSale() {
        try {
            // Use StageManager to show the NewSaleView, similar to how MainView is shown after login
            // This assumes NewSaleView will replace the current content of the primary stage
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/NewSaleView.fxml")));
            loader.setControllerFactory(springContext::getBean); // Crucial for Spring DI in NewSaleViewController
            Parent newSaleRoot = loader.load();

            // If SalesListView is in the center of MainView's BorderPane, we need access to that BorderPane.
            // For simplicity now, let's make NewSaleView take over the scene like other main views.
            // This requires StageManager to be able to set scene on primary stage.
            // If MainView is still the root, StageManager needs to be able to change its center.
            // Let's assume StageManager's showView can handle loading into the primary stage's scene.
            // Or, more directly, we can get the mainBorderPane from MainViewController if needed.

            // Option A: Use StageManager to replace the whole scene (like login -> main)
            // stageManager.showView("/fxml/NewSaleView.fxml", "Create New Sale");
            // This assumes StageManager's showView method sets the scene on primaryStage.

            // Option B: If SalesListView is inside MainView's BorderPane, and we want NewSaleView
            // to also be in the center of that SAME BorderPane. This is more complex from here.
            // The easiest is if MainViewController exposes a method to set its center.
            // Or, if SalesListView is always loaded into MainView's center,
            // its root's parent might be the BorderPane.

            // For now, let's use the main instance of MainViewController if available
            // to change its center content. This is a common pattern for sub-view navigation.
            MainViewController mainViewController = springContext.getBean(MainViewController.class);
            if (mainViewController != null) {
                mainViewController.loadCenterView("/fxml/NewSaleView.fxml");
            } else {
                // Fallback: if MainViewController isn't easily accessible, use StageManager to switch scenes
                System.err.println("MainViewController not found, switching scene via StageManager for New Sale.");
                stageManager.showView("/fxml/NewSaleView.fxml", "Create New Sale");
            }

        } catch (IOException e) {
            e.printStackTrace();
            stageManager.showErrorAlert("Error Loading View", "Could not load the New Sale screen: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportSales() { // This method was previously a placeholder
        if (salesList.isEmpty() && false) { // Keep the button active even if list is empty, backend provides all data
            stageManager.showInfoAlert("No Data", "There are no sales records currently displayed to export.");
            // Or, allow export of all data regardless of current view:
            // statusLabel.setText("Preparing to export all sales data...");
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save All Sales Data as CSV");
        fileChooser.setInitialFileName("all_sales_export_" + System.currentTimeMillis() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        
        File file = fileChooser.showSaveDialog(stageManager.getPrimaryStage());

        if (file != null) {
            Path targetPath = file.toPath();
            showProgress(true, "Exporting all sales data to CSV...");

            saleClientService.exportAllSalesToCsv(targetPath)
                .thenAcceptAsync(downloadedPath -> Platform.runLater(() -> {
                    showProgress(false, "Sales data exported successfully.");
                    stageManager.showInfoAlert("Export Successful", "All sales data exported successfully to:\n" + downloadedPath.toString());
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showProgress(false, "Sales data export failed.");
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        stageManager.showErrorAlert("Export Failed", "Could not export sales data: " + cause.getMessage());
                    });
                    return null;
                });
        } else {
            statusLabel.setText("Sales data export cancelled.");
        }
    }

    // Helper to escape CSV special characters (quotes and commas)
    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String stringValue = value.toString();
        if (stringValue.contains("\"") || stringValue.contains(",") || stringValue.contains("\n") || stringValue.contains("\r")) {
            return "\"" + stringValue.replace("\"", "\"\"") + "\"";
        }
        return stringValue;
    }

    @FXML
    private void handleGoHome() {
        try {
            MainViewController mainViewController = springContext.getBean(MainViewController.class);
            mainViewController.showHomeTiles(); // Call the public method
        } catch (Exception e) {
            System.err.println("Error navigating to home: " + e.getMessage());
            e.printStackTrace();
            // stageManager.showErrorAlert("Navigation Error", "Could not return to the main dashboard.");
            // Fallback to reloading main view if mainViewController cannot be obtained or fails
             stageManager.showMainView();
        }
    }

}