package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.ProductDto;
import com.yourstore.app.backend.model.enums.ProductCategory;
import com.yourstore.app.frontend.service.ProductClientService; // Assuming you have this
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Window;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
public class ProductEditDialogController {

    @FXML private Label headerLabel;
    @FXML private TextField nameField;
    @FXML private ComboBox<ProductCategory> categoryComboBox;
    @FXML private TextArea descriptionArea;
    @FXML private TextField supplierField;
    @FXML private Spinner<Double> purchasePriceSpinner;
    @FXML private Spinner<Double> sellingPriceSpinner;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private Label validationErrorLabel;
    @FXML private DialogPane dialogPane; // To get the button


    private ProductClientService productClientService; // Will be injected

    private ProductDto currentProduct; // For editing
    private boolean saveClicked = false;

    @Autowired
    public ProductEditDialogController(ProductClientService productClientService) {
        this.productClientService = productClientService;
    }

    @FXML
    public void initialize() {
        categoryComboBox.getItems().setAll(ProductCategory.values());
        validationErrorLabel.setText(""); // Clear any previous errors

        // Configure Spinners if not already fully done in FXML (e.g. for precision)
        // For DoubleSpinners, ensure they handle decimal input correctly based on Locale if needed.
        // The FXML setup with DoubleSpinnerValueFactory should be mostly sufficient.
    }

    public void setDialogMode(boolean isEditMode) {
        if (isEditMode) {
            headerLabel.setText("Edit Product");
        } else {
            headerLabel.setText("Add New Product");
        }
    }

    public void setProduct(ProductDto product) {
        this.currentProduct = product; // Key: store the original product DTO
        saveClicked = false;
        validationErrorLabel.setText("");

        if (product != null) { // Editing existing product
            nameField.setText(product.getName());
            categoryComboBox.setValue(product.getCategory());
            descriptionArea.setText(product.getDescription());
            supplierField.setText(product.getSupplier());
            purchasePriceSpinner.getValueFactory().setValue(product.getPurchasePrice() != null ? product.getPurchasePrice().doubleValue() : 0.0);
            sellingPriceSpinner.getValueFactory().setValue(product.getSellingPrice() != null ? product.getSellingPrice().doubleValue() : 0.0);
            quantitySpinner.getValueFactory().setValue(product.getQuantityInStock());
            // ID is preserved in this.currentProduct
        } else { // Adding new product
            nameField.clear();
            categoryComboBox.getSelectionModel().clearSelection();
            descriptionArea.clear();
            supplierField.clear();
            purchasePriceSpinner.getValueFactory().setValue(0.0);
            sellingPriceSpinner.getValueFactory().setValue(0.0);
            quantitySpinner.getValueFactory().setValue(0);
        }
    }

    public ProductDto getProduct() {
        ProductDto productToSave;
        if (this.currentProduct != null && this.currentProduct.getId() != null) { // If editing an existing product
            productToSave = this.currentProduct; // Start with the existing DTO (to preserve ID and audit fields if not changed)
        } else { // If adding a new product
            productToSave = new ProductDto();
        }

        productToSave.setName(nameField.getText());
        productToSave.setCategory(categoryComboBox.getValue());
        productToSave.setDescription(descriptionArea.getText());
        productToSave.setSupplier(supplierField.getText());
        productToSave.setPurchasePrice(BigDecimal.valueOf(purchasePriceSpinner.getValue()));
        productToSave.setSellingPrice(BigDecimal.valueOf(sellingPriceSpinner.getValue()));
        productToSave.setQuantityInStock(quantitySpinner.getValue());
        // createdAt and updatedAt from currentProduct (if editing) are preserved if not explicitly cleared or modified by backend
        return productToSave;
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    // This method will be called by the Dialog framework when the "Save" button is pressed
    // if we link it correctly. We'll do that in ProductListViewController.
    // For now, we need a way to trigger save and validation.
    public boolean handleSave() {
        if (isInputValid()) {
            saveClicked = true;
            return true;
        }
        return false; // Validation failed
    }


    private boolean isInputValid() {
        StringBuilder errorMessage = new StringBuilder();

        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errorMessage.append("Product name is required.\n");
        }
        if (categoryComboBox.getValue() == null) {
            errorMessage.append("Product category is required.\n");
        }
        if (sellingPriceSpinner.getValue() == null || sellingPriceSpinner.getValue() < 0) {
            errorMessage.append("Selling price must be a non-negative value.\n");
        }
         // Purchase price can be optional or 0, but not negative.
        if (purchasePriceSpinner.getValue() != null && purchasePriceSpinner.getValue() < 0) {
            errorMessage.append("Purchase price cannot be negative.\n");
        }
        if (quantitySpinner.getValue() == null || quantitySpinner.getValue() < 0) {
            errorMessage.append("Quantity in stock must be a non-negative integer.\n");
        }

        if (errorMessage.length() == 0) {
            validationErrorLabel.setText("");
            return true;
        } else {
            // Show the error message.
            validationErrorLabel.setText("Please correct the following errors:\n" + errorMessage.toString());
            return false;
        }
    }

    public void setDialogPane(DialogPane dialogPane) {
        this.dialogPane = dialogPane;
    }
}