package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.SaleDto;
import com.yourstore.app.frontend.service.SaleClientService;
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

@Component
public class SalesListViewController {

    @FXML private TableView<SaleDto> salesTableView;
    @FXML private TableColumn<SaleDto, Long> saleIdColumn;
    @FXML private TableColumn<SaleDto, String> customerNameColumn;
    @FXML private TableColumn<SaleDto, BigDecimal> totalAmountColumn;
    @FXML private TableColumn<SaleDto, LocalDateTime> saleDateColumn;
    @FXML private TableColumn<SaleDto, String> cashierColumn;
    @FXML private TableColumn<SaleDto, String> itemsColumn; // To display item count or summary


    @FXML private Button refreshButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;

    private final SaleClientService saleClientService;
    private final ObservableList<SaleDto> salesList = FXCollections.observableArrayList();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public SalesListViewController(SaleClientService saleClientService) {
        this.saleClientService = saleClientService;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
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
                salesList.setAll(sales);
                showProgress(false, "Sales loaded. Found " + sales.size() + " records.");
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
        statusLabel.setText("New Sale: Placeholder - To be implemented.");
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "This feature (New Sale screen) will be implemented later.");
        alert.showAndWait();
    }

    @FXML
    private void handleExportSales() {
        statusLabel.setText("Export Sales: Placeholder - To be implemented.");
         Alert alert = new Alert(Alert.AlertType.INFORMATION, "This feature (Exporting sales data) will be implemented later.");
        alert.showAndWait();
    }
}