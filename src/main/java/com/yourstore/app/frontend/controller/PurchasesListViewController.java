package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.PurchaseDto;
import com.yourstore.app.frontend.service.PurchaseClientService;
import com.yourstore.app.frontend.util.StageManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class PurchasesListViewController {

    @FXML private TableView<PurchaseDto> purchasesTableView;
    @FXML private TableColumn<PurchaseDto, Long> purchaseIdColumn;
    @FXML private TableColumn<PurchaseDto, String> supplierNameColumn;
    @FXML private TableColumn<PurchaseDto, String> invoiceNumberColumn;
    @FXML private TableColumn<PurchaseDto, BigDecimal> totalAmountColumn;
    @FXML private TableColumn<PurchaseDto, LocalDateTime> purchaseDateColumn;
    @FXML private TableColumn<PurchaseDto, String> recordedByColumn;
    @FXML private TableColumn<PurchaseDto, String> itemsCountColumn;
    @FXML private TextField searchPurchaseField;

    @FXML private Button refreshButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;
    @FXML private Button homeButton;

    private final PurchaseClientService purchaseClientService;
    private final StageManager stageManager;
    private final ConfigurableApplicationContext springContext;
    private final ObservableList<PurchaseDto> purchasesList = FXCollections.observableArrayList();
    private final ObservableList<PurchaseDto> purchasesMasterList = FXCollections.observableArrayList();
    private FilteredList<PurchaseDto> filteredPurchasesData;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public PurchasesListViewController(PurchaseClientService purchaseClientService, StageManager stageManager, ConfigurableApplicationContext springContext) {
        this.purchaseClientService = purchaseClientService;
        this.stageManager = stageManager;
        this.springContext = springContext;
    }

   @FXML
    public void initialize() {
        setupTableColumns();

        filteredPurchasesData = new FilteredList<>(purchasesMasterList, p -> true);

        searchPurchaseField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredPurchasesData.setPredicate(purchase -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();

                if (String.valueOf(purchase.getId()).contains(lowerCaseFilter)) return true;
                if (purchase.getSupplierName() != null && purchase.getSupplierName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (purchase.getInvoiceNumber() != null && purchase.getInvoiceNumber().toLowerCase().contains(lowerCaseFilter)) return true;
                if (purchase.getUsername() != null && purchase.getUsername().toLowerCase().contains(lowerCaseFilter)) return true; // Recorded By
                // Add more fields if needed
                if (purchase.getTotalAmount() != null && purchase.getTotalAmount().toPlainString().contains(lowerCaseFilter)) return true;

                return false;
            });
        });

        SortedList<PurchaseDto> sortedData = new SortedList<>(filteredPurchasesData);
        sortedData.comparatorProperty().bind(purchasesTableView.comparatorProperty());
        purchasesTableView.setItems(sortedData);

        loadPurchases();
    }


    private void setupTableColumns() {
        purchaseIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        supplierNameColumn.setCellValueFactory(new PropertyValueFactory<>("supplierName"));
        invoiceNumberColumn.setCellValueFactory(new PropertyValueFactory<>("invoiceNumber"));
        totalAmountColumn.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        recordedByColumn.setCellValueFactory(new PropertyValueFactory<>("username"));

        purchaseDateColumn.setCellValueFactory(new PropertyValueFactory<>("purchaseDate"));
        purchaseDateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : dateTimeFormatter.format(item));
            }
        });

        itemsCountColumn.setCellValueFactory(cellData -> {
            int count = cellData.getValue().getItems() != null ? cellData.getValue().getItems().size() : 0;
            return new javafx.beans.property.SimpleStringProperty(count + " items");
        });
        purchasesTableView.setItems(purchasesList);
    }

    @FXML
    private void handleRefreshPurchases() {
        loadPurchases();
    }

    private void loadPurchases() {
        showProgress(true, "Loading purchase records...");
        purchaseClientService.getAllPurchases()
            .thenAcceptAsync(purchases -> Platform.runLater(() -> {
                purchasesMasterList.setAll(purchases); // Update master list
                showProgress(false, "Purchases loaded. Found " + purchasesMasterList.size() + " records.");
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showProgress(false, "Error loading purchases: " + cause.getMessage());
                });
                return null;
            });
    }

    private void showProgress(boolean show, String message) {
        progressIndicator.setVisible(show);
        statusLabel.setText(message != null ? message : "");
    }

    @FXML
    private void handleNewPurchase() {
        MainViewController mainViewController = springContext.getBean(MainViewController.class);
        if (mainViewController != null) {
            mainViewController.loadCenterView("/fxml/NewPurchaseView.fxml");
        } else {
            stageManager.showView("/fxml/NewPurchaseView.fxml", "New Purchase");
        }
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