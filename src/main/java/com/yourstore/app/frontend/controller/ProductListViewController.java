package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.ProductDto;
import com.yourstore.app.backend.model.enums.ProductCategory; // Assuming ProductDto has ProductCategory
import com.yourstore.app.frontend.service.ProductClientService;
import com.yourstore.app.frontend.util.StageManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader; // For Add/Edit Dialog
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class ProductListViewController {

    private static final Logger logger = LoggerFactory.getLogger(ProductListViewController.class);

    // --- FXML Injected Fields ---
    @FXML private TableView<ProductDto> productTableView;
    @FXML private TableColumn<ProductDto, Long> idColumn;
    @FXML private TableColumn<ProductDto, String> nameColumn;
    @FXML private TableColumn<ProductDto, ProductCategory> categoryColumn;
    @FXML private TableColumn<ProductDto, BigDecimal> sellingPriceColumn;
    @FXML private TableColumn<ProductDto, Integer> quantityColumn;
    @FXML private TableColumn<ProductDto, String> supplierColumn;
    @FXML private TableColumn<ProductDto, BigDecimal> purchasePriceColumn;
    @FXML private TableColumn<ProductDto, LocalDateTime> createdAtColumn;
    // @FXML private TableColumn<ProductDto, LocalDateTime> updatedAtColumn; // Uncomment if used
    // @FXML private TableColumn<ProductDto, String> descriptionColumn; // Uncomment if used

    @FXML private TextField searchProductField;
    @FXML private Button addProductButton;
    @FXML private Button editProductButton;
    @FXML private Button deleteProductButton;
    @FXML private Button refreshButton;
    @FXML private Button exportProductsCsvButton;
    @FXML private Button homeButton; // For "Return to Home/Dashboard"

    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    // --- Services and Utilities ---
    private final ProductClientService productClientService;
    private final StageManager stageManager;
    private final ConfigurableApplicationContext springContext;

    // --- Data Lists ---
    private final ObservableList<ProductDto> productMasterList = FXCollections.observableArrayList();
    private FilteredList<ProductDto> filteredProductData;

    // --- Constants and Formatters ---
    private static final int LOW_STOCK_THRESHOLD = 10;    // Define low stock
    private static final int CRITICAL_STOCK_THRESHOLD = 5; // Define critical/out of stock
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public ProductListViewController(ProductClientService productClientService, StageManager stageManager, ConfigurableApplicationContext springContext) {
        this.productClientService = productClientService;
        this.stageManager = stageManager;
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        logger.info("Initializing ProductListViewController.");
        showProgress(false, "Ready.");

        // 1. Initialize FilteredList around the master list.
        filteredProductData = new FilteredList<>(productMasterList, p -> true);

        // 2. Set the filter Predicate whenever the filter text changes.
        searchProductField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredProductData.setPredicate(product -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true; // Display all products if filter is empty.
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (product.getName() != null && product.getName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (product.getCategory() != null && product.getCategory().toString().toLowerCase().contains(lowerCaseFilter)) return true;
                if (product.getSupplier() != null && product.getSupplier().toLowerCase().contains(lowerCaseFilter)) return true;
                if (product.getDescription() != null && product.getDescription().toLowerCase().contains(lowerCaseFilter)) return true;
                if (String.valueOf(product.getId()).contains(lowerCaseFilter)) return true; // Search by ID
                return false; // Does not match.
            });
        });

        // 3. Wrap the FilteredList in a SortedList.
        SortedList<ProductDto> sortedData = new SortedList<>(filteredProductData);

        // 4. Bind the SortedList comparator to the TableView comparator.
        sortedData.comparatorProperty().bind(productTableView.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.
        productTableView.setItems(sortedData);

        setupTableColumns(); // Configure cell value factories and cell factories
        setupRowSelectionListener(); // For enabling/disabling edit/delete buttons

        loadProducts(); // Load initial data
    }

    private void setupTableColumns() {
        logger.debug("Setting up table columns.");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        sellingPriceColumn.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        purchasePriceColumn.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));

        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdAtColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : dateTimeFormatter.format(item));
            }
        });

        // Stock Quantity Column with Visual Indicator
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantityInStock"));
        quantityColumn.setCellFactory(column -> new TableCell<>() { // Explicit type arguments
            @Override
            protected void updateItem(Integer quantity, boolean empty) {
                super.updateItem(quantity, empty);
                // Clear previous styles first by removing them all from the current cell.
                // This is important because TableCells are reused by JavaFX.
                this.getStyleClass().removeAll("stock-critical", "stock-low", "stock-sufficient");
                setText(null); // Clear text before setting

                if (empty || quantity == null) {
                    // No style needed for empty cells
                } else {
                    setText(quantity.toString());
                    if (quantity <= CRITICAL_STOCK_THRESHOLD) {
                        this.getStyleClass().add("stock-critical");
                    } else if (quantity <= LOW_STOCK_THRESHOLD) {
                        this.getStyleClass().add("stock-low");
                    } else {
                        this.getStyleClass().add("stock-sufficient");
                    }
                }
            }
        });
    }

    private void setupRowSelectionListener() {
        productTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean itemSelected = (newSelection != null);
            editProductButton.setDisable(!itemSelected);
            deleteProductButton.setDisable(!itemSelected);
        });
    }

    private void loadProducts() {
        showProgress(true, "Loading products...");
        productClientService.getAllProducts()
            .thenAcceptAsync(products -> Platform.runLater(() -> {
                productMasterList.setAll(products);
                showProgress(false, "Products loaded. Found " + productMasterList.size() + " items.");
                logger.info("Loaded {} products into master list.", productMasterList.size());
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    productMasterList.clear();
                    showProgress(false, "Error loading products.");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    stageManager.showErrorAlert("Load Error", "Could not load products: " + cause.getMessage());
                    logger.error("Error loading products: {}", cause.getMessage(), cause);
                });
                return null;
            });
    }

    @FXML
    private void handleRefreshProducts() {
        logger.info("Refresh products button clicked.");
        searchProductField.clear(); // Optionally clear search on refresh
        loadProducts();
    }

    @FXML
    private void handleAddProduct() {
        logger.info("Add product button clicked.");
        openProductEditDialog(null); // Pass null for new product
    }

    @FXML
    private void handleEditProduct() {
        ProductDto selectedProduct = productTableView.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            logger.info("Edit product button clicked for product ID: {}", selectedProduct.getId());
            openProductEditDialog(selectedProduct);
        } else {
            stageManager.showInfoAlert("No Selection", "Please select a product to edit.");
        }
    }

    private void openProductEditDialog(ProductDto productToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/ProductEditDialog.fxml")));
            loader.setControllerFactory(springContext::getBean); 
            DialogPane dialogPane = loader.load();

            ProductEditDialogController controller = loader.getController();
            controller.setDialogMode(productToEdit != null);
            controller.setProduct(productToEdit);
            controller.setDialogPane(dialogPane); // Pass the dialogPane to controller for button lookup

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle(productToEdit != null ? "Edit Product" : "Add New Product");
            dialog.initOwner(stageManager.getPrimaryStage());

            Button saveButtonNode = (Button) dialogPane.lookupButton(dialogPane.getButtonTypes().stream()
                            .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Save ButtonType not found in dialog")));
            
            // Add primary style to save button if not already handled by CSS for default buttons in dialogs
            if (!saveButtonNode.getStyleClass().contains("button-primary")) {
                 saveButtonNode.getStyleClass().add("button-primary");
            }


            saveButtonNode.setOnAction(event -> {
                if (controller.handleSave()) {
                    dialog.setResult(dialogPane.getButtonTypes().stream()
                            .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                            .findFirst().get());
                    dialog.close();
                } else {
                    event.consume(); 
                }
            });

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE && controller.isSaveClicked()) {
                ProductDto productFromDialog = controller.getProduct();
                showProgress(true, "Saving product...");
                CompletableFuture<ProductDto> saveFuture = productToEdit == null ?
                    productClientService.createProduct(productFromDialog) :
                    productClientService.updateProduct(productFromDialog.getId(), productFromDialog);

                saveFuture.thenAcceptAsync(savedProduct -> Platform.runLater(() -> {
                    showProgress(false, "Product saved successfully.");
                    stageManager.showInfoAlert("Success", "Product '" + savedProduct.getName() + "' saved.");
                    loadProducts(); // Refresh list
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showProgress(false, "Error saving product.");
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        stageManager.showErrorAlert("Save Failed", "Could not save product: " + cause.getMessage());
                        logger.error("Failed to save product: {}", cause.getMessage(), cause);
                    });
                    return null;
                });
            }
        } catch (IOException | IllegalStateException e) { // Catch IllegalStateException from lookupButton
            logger.error("Failed to open product edit dialog: {}", e.getMessage(), e);
            stageManager.showErrorAlert("Dialog Error", "Could not open the product form: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteProduct() {
        ProductDto selectedProduct = productTableView.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            stageManager.showInfoAlert("No Selection", "Please select a product to delete.");
            return;
        }
        logger.info("Delete product button clicked for product ID: {}", selectedProduct.getId());

        Optional<ButtonType> result = stageManager.showConfirmationAlert(
                "Confirm Deletion",
                "Delete Product: " + selectedProduct.getName(),
                "Are you sure you want to permanently delete this product? This action cannot be undone."
        );

        if (result.isPresent() && result.get() == ButtonType.OK) {
            showProgress(true, "Deleting product...");
            productClientService.deleteProduct(selectedProduct.getId())
                .thenRunAsync(() -> Platform.runLater(() -> {
                    showProgress(false, "Product deleted.");
                    stageManager.showInfoAlert("Success", "Product '" + selectedProduct.getName() + "' deleted.");
                    loadProducts(); // Refresh list
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showProgress(false, "Error deleting product.");
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        stageManager.showErrorAlert("Delete Failed", "Could not delete product: " + cause.getMessage());
                        logger.error("Failed to delete product ID {}: {}", selectedProduct.getId(), cause.getMessage(), cause);
                    });
                    return null;
                });
        }
    }

    @FXML
    private void handleExportProductsToCsv() {
        logger.info("Export products to CSV button clicked.");
        if (productMasterList.isEmpty()) { // Export from master list to get all data
            stageManager.showInfoAlert("No Data", "There are no products to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Product List as CSV");
        fileChooser.setInitialFileName("products_export_" + System.currentTimeMillis() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        File file = fileChooser.showSaveDialog(stageManager.getPrimaryStage());

        if (file != null) {
            showProgress(true, "Exporting products to CSV...");
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("ID,Name,Category,Description,Supplier,PurchasePrice,SellingPrice,QuantityInStock,CreatedAt,UpdatedAt");
                for (ProductDto product : productMasterList) {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%d\",\"%s\",\"%s\"\n",
                            escapeCsv(product.getId()),
                            escapeCsv(product.getName()),
                            escapeCsv(product.getCategory()),
                            escapeCsv(product.getDescription()),
                            escapeCsv(product.getSupplier()),
                            escapeCsv(product.getPurchasePrice()),
                            escapeCsv(product.getSellingPrice()),
                            product.getQuantityInStock(),
                            escapeCsv(product.getCreatedAt() != null ? dateTimeFormatter.format(product.getCreatedAt()) : ""),
                            escapeCsv(product.getUpdatedAt() != null ? dateTimeFormatter.format(product.getUpdatedAt()) : "")
                    );
                }
                showProgress(false, "Export successful.");
                stageManager.showInfoAlert("Export Successful", "Product list exported successfully to:\n" + file.getAbsolutePath());
                logger.info("Products exported to CSV: {}", file.getAbsolutePath());
            } catch (IOException e) {
                showProgress(false, "Export failed.");
                logger.error("Could not export product list to CSV: {}", e.getMessage(), e);
                stageManager.showErrorAlert("Export Failed", "Could not export product list: " + e.getMessage());
            }
        } else {
            showProgress(false, "CSV Export cancelled.");
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
        logger.debug("Go Home button clicked from Product List.");
        try {
            MainViewController mainViewController = springContext.getBean(MainViewController.class);
            // Call the method that loads the main/default/dashboard view
            mainViewController.handleShowDashboard(); // This will load DashboardView.fxml into mainContentArea
                                                     // and also select the "Dashboard" side nav button
        } catch (Exception e) {
            logger.error("Error navigating to home (dashboard) from Product List: {}", e.getMessage(), e);
            stageManager.showErrorAlert("Navigation Error", "Could not return to the main dashboard.");
            // As a last resort, try to re-initialize the main view (might be too heavy)
            // stageManager.showMainView();
        }
    }
    
    private void showProgress(boolean show, String message) {
        if (progressIndicator != null) progressIndicator.setVisible(show);
        if (statusLabel != null) statusLabel.setText(message != null ? message : "");
    }
}