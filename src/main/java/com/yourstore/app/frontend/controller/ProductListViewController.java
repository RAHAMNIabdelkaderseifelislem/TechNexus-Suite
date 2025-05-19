package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.ProductDto;
import com.yourstore.app.backend.model.enums.ProductCategory;
import com.yourstore.app.frontend.service.ProductClientService;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext; // For loading FXML with Spring context
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class ProductListViewController {

    @FXML private TableView<ProductDto> productTableView;
    @FXML private TableColumn<ProductDto, Long> idColumn;
    @FXML private TableColumn<ProductDto, String> nameColumn;
    @FXML private TableColumn<ProductDto, ProductCategory> categoryColumn;
    @FXML private TableColumn<ProductDto, String> descriptionColumn;
    @FXML private TableColumn<ProductDto, String> supplierColumn;
    @FXML private TableColumn<ProductDto, BigDecimal> purchasePriceColumn;
    @FXML private TableColumn<ProductDto, BigDecimal> sellingPriceColumn;
    @FXML private TableColumn<ProductDto, Integer> quantityColumn;
    @FXML private TableColumn<ProductDto, LocalDateTime> createdAtColumn;
    @FXML private TableColumn<ProductDto, LocalDateTime> updatedAtColumn;

    @FXML private Button addProductButton;
    @FXML private Button editProductButton;
    @FXML private Button deleteProductButton;
    @FXML private Button refreshButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;
    @FXML private Button exportProductsCsvButton;
    @FXML private TextField searchProductField;
    @FXML private Button homeButton;

    private final ProductClientService productClientService;
    private final StageManager stageManager; // To close or navigate
    private final ConfigurableApplicationContext springContext; // Inject Spring context
    private final ObservableList<ProductDto> productList = FXCollections.observableArrayList();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ObservableList<ProductDto> productMasterList = FXCollections.observableArrayList();
    private FilteredList<ProductDto> filteredProductData;

    @Autowired
    public ProductListViewController(ProductClientService productClientService, StageManager stageManager, ConfigurableApplicationContext springContext) {
        this.productClientService = productClientService;
        this.stageManager = stageManager;
        this.springContext = springContext; // Initialize Spring context
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        // Do not call loadProducts() directly here if it populates productMasterList.
        // Initialize the filtered list first, then load data.

        // 1. Wrap the ObservableList in a FilteredList (initially display all data).
        filteredProductData = new FilteredList<>(productMasterList, p -> true);

        // 2. Set the filter Predicate whenever the filter changes.
        searchProductField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredProductData.setPredicate(product -> {
                // If filter text is empty, display all products.
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();

                if (product.getName() != null && product.getName().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches name.
                } else if (product.getCategory() != null && product.getCategory().toString().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches category.
                } else if (product.getSupplier() != null && product.getSupplier().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches supplier.
                } else if (product.getDescription() != null && product.getDescription().toLowerCase().contains(lowerCaseFilter)) {
                    return true; // Filter matches description
                }
                return false; // Does not match.
            });
        });

        // 3. Wrap the FilteredList in a SortedList.
        SortedList<ProductDto> sortedData = new SortedList<>(filteredProductData);

        // 4. Bind the SortedList comparator to the TableView comparator.
        sortedData.comparatorProperty().bind(productTableView.comparatorProperty());

        // 5. Add sorted (and filtered) data to the table.
        productTableView.setItems(sortedData);

        setupRowSelectionListener(); // Keep this
        loadProducts(); // Now load data into productMasterList
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        supplierColumn.setCellValueFactory(new PropertyValueFactory<>("supplier"));
        purchasePriceColumn.setCellValueFactory(new PropertyValueFactory<>("purchasePrice"));
        sellingPriceColumn.setCellValueFactory(new PropertyValueFactory<>("sellingPrice"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantityInStock"));

        // Custom cell factory for LocalDateTime formatting
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        createdAtColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(dateTimeFormatter.format(item));
                }
            }
        });

        updatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("updatedAt"));
        updatedAtColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(dateTimeFormatter.format(item));
                }
            }
        });

        productTableView.setItems(productList);
    }

    private void setupRowSelectionListener() {
        productTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;
            editProductButton.setDisable(!hasSelection);
            deleteProductButton.setDisable(!hasSelection);
        });
    }

    @FXML
    private void handleRefreshProducts() {
        loadProducts();
    }

    private void loadProducts() {
        showProgress(true, "Loading products...");
        productClientService.getAllProducts()
            .thenAcceptAsync(products -> Platform.runLater(() -> {
                productMasterList.setAll(products); // Update the master list
                // The FilteredList and SortedList will update the TableView automatically
                showProgress(false, "Products loaded. Found " + productMasterList.size() + " items.");
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    System.err.println("Error loading products: " + ex.getMessage());
                    ex.printStackTrace();
                    showProgress(false, "Error loading products: " + ex.getCause().getMessage());
                    // Show alert dialog
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to load products");
                    alert.setContentText(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    alert.showAndWait();
                });
                return null;
            });
    }

    private void showProgress(boolean show, String message) {
        progressIndicator.setVisible(show);
        statusLabel.setText(message != null ? message : "");
    }

    @FXML
    private void handleAddProduct() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/ProductEditDialog.fxml")));
            loader.setControllerFactory(springContext::getBean); // Use Spring context

            DialogPane dialogPane = loader.load();
            ProductEditDialogController controller = loader.getController();
            controller.setDialogMode(false); // Set to Add mode
            controller.setProduct(null);     // No existing product to edit
            controller.setDialogPane(dialogPane); // Pass the dialogPane to controller

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Add New Product");
            dialog.initOwner(addProductButton.getScene().getWindow()); // Set owner for proper modality

            // This is how you get the "Save" button from the DialogPane
            Button saveButtonNode = (Button) dialogPane.lookupButton(dialogPane.getButtonTypes().stream()
                            .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Save ButtonType not found")));


            // Override action for the save button to include validation from controller
            saveButtonNode.setOnAction(event -> {
                if (controller.handleSave()) { // Perform validation and update ProductDto
                    // If validation passes, manually close the dialog by simulating a click on the original button type
                    // This ensures the dialog.showAndWait() below will return the correct ButtonType
                    dialog.setResult(dialogPane.getButtonTypes().stream()
                            .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                            .findFirst().get());
                    dialog.close();
                } else {
                    event.consume(); // Prevent dialog from closing if validation fails
                }
            });


            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE && controller.isSaveClicked()) {
                ProductDto newProduct = controller.getProduct();
                showProgress(true, "Adding product...");
                productClientService.createProduct(newProduct)
                    .thenAcceptAsync(savedProduct -> Platform.runLater(() -> {
                        showProgress(false, "Product '" + savedProduct.getName() + "' added successfully.");
                        loadProducts(); // Refresh the list
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            System.err.println("Error adding product: " + ex.getMessage());
                            ex.printStackTrace();
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            showProgress(false, "Error adding product: " + cause.getMessage());
                            stageManager.showErrorAlert("Failed to Add Product", cause.getMessage());
                        });
                        return null;
                    });
            }

        } catch (IOException e) {
            e.printStackTrace();
            stageManager.showErrorAlert("Dialog Error", "Failed to load the add product dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleEditProduct() {
        ProductDto selectedProduct = productTableView.getSelectionModel().getSelectedItem();
        if (selectedProduct == null) {
            stageManager.showInfoAlert("No Product Selected", "Please select a product in the table to edit.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/ProductEditDialog.fxml")));
            loader.setControllerFactory(springContext::getBean); // Use Spring context

            DialogPane dialogPane = loader.load();
            ProductEditDialogController controller = loader.getController();
            controller.setDialogMode(true);    // Set to Edit mode
            controller.setProduct(selectedProduct); // Pass the selected product
            controller.setDialogPane(dialogPane);


            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Edit Product");
            dialog.initOwner(editProductButton.getScene().getWindow()); // Set owner

            Button saveButtonNode = (Button) dialogPane.lookupButton(dialogPane.getButtonTypes().stream()
                            .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Save ButtonType not found in dialog")));

            saveButtonNode.setOnAction(event -> {
                if (controller.handleSave()) { // Perform validation
                    dialog.setResult(dialogPane.getButtonTypes().stream()
                            .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                            .findFirst().get());
                    dialog.close();
                } else {
                    event.consume(); // Prevent dialog from closing if validation fails
                }
            });

            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE && controller.isSaveClicked()) {
                ProductDto updatedProductDetails = controller.getProduct();
                showProgress(true, "Updating product '" + updatedProductDetails.getName() + "'...");
                productClientService.updateProduct(selectedProduct.getId(), updatedProductDetails)
                    .thenAcceptAsync(savedProduct -> Platform.runLater(() -> {
                        showProgress(false, "Product '" + savedProduct.getName() + "' updated successfully.");
                        loadProducts(); // Refresh the list
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            System.err.println("Error updating product: " + ex.getMessage());
                            ex.printStackTrace();
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            showProgress(false, "Error updating product: " + cause.getMessage());
                            stageManager.showErrorAlert("Failed to Update Product", cause.getMessage());
                        });
                        return null;
                    });
            }

        } catch (IOException e) {
            e.printStackTrace();
            stageManager.showErrorAlert("Dialog Error", "Failed to load the edit product dialog: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteProduct() {
        ProductDto selectedProduct = productTableView.getSelectionModel().getSelectedItem();

        if (selectedProduct == null) {
            stageManager.showInfoAlert("No Product Selected", "Please select a product in the table to delete.");
            return;
        }

        // Confirmation Dialog
        Alert confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Delete Product");
        confirmationDialog.setHeaderText("Confirm Deletion");
        confirmationDialog.setContentText("Are you sure you want to delete the product: '" + selectedProduct.getName() + "' (ID: " + selectedProduct.getId() + ")?");
        confirmationDialog.initOwner(deleteProductButton.getScene().getWindow()); // Set owner for proper modality

        Optional<ButtonType> result = confirmationDialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // User confirmed deletion
            showProgress(true, "Deleting product '" + selectedProduct.getName() + "'...");
            productClientService.deleteProduct(selectedProduct.getId())
                .thenRunAsync(() -> Platform.runLater(() -> { // thenRunAsync for CompletableFuture<Void>
                    showProgress(false, "Product '" + selectedProduct.getName() + "' deleted successfully.");
                    loadProducts(); // Refresh the list
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        System.err.println("Error deleting product: " + ex.getMessage());
                        ex.printStackTrace();
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        showProgress(false, "Error deleting product: " + cause.getMessage());
                        stageManager.showErrorAlert("Failed to Delete Product", cause.getMessage());
                    });
                    return null;
                });
        } else {
            // User cancelled
            statusLabel.setText("Product deletion cancelled.");
        }
    }

    @FXML
    private void handleExportProductsToCsv() {
        if (productList.isEmpty()) {
            stageManager.showInfoAlert("No Data", "There are no products to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Product List as CSV");
        fileChooser.setInitialFileName("products_export_" + System.currentTimeMillis() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(stageManager.getPrimaryStage()); // Assuming stageManager is available

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                // Write CSV Header
                writer.println("ID,Name,Category,Description,Supplier,PurchasePrice,SellingPrice,QuantityInStock,CreatedAt,UpdatedAt");

                // Write Data
                for (ProductDto product : productList) { // productList is your ObservableList<ProductDto>
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
                stageManager.showInfoAlert("Export Successful", "Product list exported successfully to:\n" + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                stageManager.showErrorAlert("Export Failed", "Could not export product list: " + e.getMessage());
            }
        }
    }

    // Helper to escape CSV special characters (quotes and commas)
    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String stringValue = value.toString();
        // Replace quotes with double quotes, and if it contains comma or quote, enclose in quotes
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