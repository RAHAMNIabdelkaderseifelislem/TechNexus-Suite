package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.ProductDto;
import com.yourstore.app.backend.model.enums.ProductCategory;
import com.yourstore.app.frontend.service.ProductClientService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext; // For loading FXML with Spring context
import org.springframework.stereotype.Component;

import java.io.IOException;
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

    private final ProductClientService productClientService;
    private final ConfigurableApplicationContext springContext; // Inject Spring context
    private final ObservableList<ProductDto> productList = FXCollections.observableArrayList();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public ProductListViewController(ProductClientService productClientService, ConfigurableApplicationContext springContext) {
        this.productClientService = productClientService;
        this.springContext = springContext; // Initialize Spring context
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        setupRowSelectionListener();
        loadProducts();
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
            .thenAcceptAsync(products -> Platform.runLater(() -> { // Ensure UI updates on JavaFX thread
                productList.setAll(products);
                productTableView.sort(); // Re-apply sort if any
                showProgress(false, "Products loaded successfully. Found " + products.size() + " items.");
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

    // Ensure these helper methods are part of the class
    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null); // Or a more specific header
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null); // Or a more specific header
        alert.setContentText(content);
        alert.showAndWait();
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
                            showErrorAlert("Failed to Add Product", cause.getMessage());
                        });
                        return null;
                    });
            }

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Dialog Error", "Failed to load the add product dialog: " + e.getMessage());
        }
    }

     @FXML
    private void handleEditProduct() {
        ProductDto selectedProduct = productTableView.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            statusLabel.setText("Edit Product clicked for: " + selectedProduct.getName() + " - Placeholder");
            // Implementation will be similar to handleAddProduct but for editing
            showInfoAlert("Edit Product", "This feature will be implemented in the next commit."); // This call should now be fine
        } else {
            showInfoAlert("No Selection", "Please select a product to edit."); // This call should now be fine
        }
    }

    @FXML
    private void handleDeleteProduct() {
        ProductDto selectedProduct = productTableView.getSelectionModel().getSelectedItem();
        if (selectedProduct != null) {
            statusLabel.setText("Delete Product clicked for: " + selectedProduct.getName() + " - Placeholder");
            // Implementation will be similar, with a confirmation dialog
            showInfoAlert("Delete Product", "This feature will be implemented in a future commit."); // This call should now be fine
        } else {
            showInfoAlert("No Selection", "Please select a product to delete."); // This call should now be fine
        }
    }
}