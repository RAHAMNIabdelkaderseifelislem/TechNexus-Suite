package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.ProductDto;
import com.yourstore.app.backend.model.enums.ProductCategory;
import com.yourstore.app.frontend.service.ProductClientService; // For potential future direct calls, not used for save here
import com.yourstore.app.frontend.util.StageManager; // For alerts if needed directly
import javafx.application.Platform; // Import Platform
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // ProductClientService is not directly used now
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
// import java.util.Arrays; // Not needed if populating ComboBox with Enum.values()

@Component
public class ProductEditDialogController {

    private static final Logger logger = LoggerFactory.getLogger(ProductEditDialogController.class);

    @FXML private Label headerLabel;
    @FXML private TextField nameField;
    @FXML private ComboBox<ProductCategory> categoryComboBox;
    @FXML private TextArea descriptionArea;
    @FXML private TextField supplierField;
    @FXML private Spinner<Double> purchasePriceSpinner;
    @FXML private Spinner<Double> sellingPriceSpinner;
    @FXML private Spinner<Integer> quantitySpinner;
    @FXML private Label validationErrorLabel;
    @FXML private DialogPane dialogPane; // Injected to help ProductListViewController find the button node if needed

    // ProductClientService is not strictly needed here if save is orchestrated by ProductListViewController
    // private final ProductClientService productClientService;
    private final StageManager stageManager; // For showing alerts from this controller, if any

    private ProductDto currentProduct; // For holding data when editing
    private boolean saveClicked = false;

    // Constructor - StageManager is useful for standalone alerts from this dialog
    @Autowired
    public ProductEditDialogController(StageManager stageManager /*, ProductClientService productClientService - optional here */) {
        // this.productClientService = productClientService;
        this.stageManager = stageManager;
    }

    @FXML
    public void initialize() {
        logger.debug("Initializing ProductEditDialogController.");
        categoryComboBox.setItems(FXCollections.observableArrayList(ProductCategory.values()));
        validationErrorLabel.setText("");
        validationErrorLabel.setManaged(false); // Don't take space if empty
        validationErrorLabel.setVisible(false);

        // Add listeners to clear error message on input change (optional UX improvement)
        nameField.textProperty().addListener((obs, ov, nv) -> clearValidationError());
        categoryComboBox.valueProperty().addListener((obs, ov, nv) -> clearValidationError());
        // ... add for other fields if desired
    }
    
    private void clearValidationError(){
        if (validationErrorLabel.isVisible()){
            validationErrorLabel.setText("");
            validationErrorLabel.setManaged(false);
            validationErrorLabel.setVisible(false);
        }
    }


    public void setDialogMode(boolean isEditMode) {
        if (isEditMode) {
            headerLabel.setText("Edit Product Details");
        } else {
            headerLabel.setText("Add New Product");
        }
        logger.debug("Dialog mode set to: {}", isEditMode ? "Edit" : "Add");
    }

    public void setProduct(ProductDto product) {
        this.currentProduct = product; // This DTO is passed from ProductListViewController
        saveClicked = false;
        clearValidationError();

        if (product != null) { // Editing existing product
            logger.debug("Populating dialog for editing product ID: {}", product.getId());
            nameField.setText(product.getName());
            categoryComboBox.setValue(product.getCategory());
            descriptionArea.setText(product.getDescription());
            supplierField.setText(product.getSupplier());
            purchasePriceSpinner.getValueFactory().setValue(product.getPurchasePrice() != null ? product.getPurchasePrice().doubleValue() : 0.0);
            sellingPriceSpinner.getValueFactory().setValue(product.getSellingPrice() != null ? product.getSellingPrice().doubleValue() : 0.01); // Ensure not 0 for min
            quantitySpinner.getValueFactory().setValue(product.getQuantityInStock());
        } else { // Adding new product
            logger.debug("Initializing dialog for new product.");
            nameField.clear();
            categoryComboBox.getSelectionModel().clearSelection();
            descriptionArea.clear();
            supplierField.clear();
            purchasePriceSpinner.getValueFactory().setValue(0.0);
            sellingPriceSpinner.getValueFactory().setValue(0.01); // Default selling price > 0
            quantitySpinner.getValueFactory().setValue(0);
        }
    }

    public ProductDto getProduct() {
        // This method is called by ProductListViewController after dialog is confirmed
        ProductDto productDto = (currentProduct != null && currentProduct.getId() != null) ? currentProduct : new ProductDto();
        
        productDto.setName(nameField.getText().trim());
        productDto.setCategory(categoryComboBox.getValue());
        productDto.setDescription(descriptionArea.getText().trim());
        productDto.setSupplier(supplierField.getText().trim());
        
        // Handle potential nulls from spinners if editable text is cleared
        Double purchasePriceVal = purchasePriceSpinner.getValue();
        productDto.setPurchasePrice(purchasePriceVal != null ? BigDecimal.valueOf(purchasePriceVal).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        
        Double sellingPriceVal = sellingPriceSpinner.getValue();
        productDto.setSellingPrice(sellingPriceVal != null ? BigDecimal.valueOf(sellingPriceVal).setScale(2, RoundingMode.HALF_UP) : BigDecimal.valueOf(0.01));

        Integer quantityVal = quantitySpinner.getValue();
        productDto.setQuantityInStock(quantityVal != null ? quantityVal : 0);
        
        logger.debug("Product DTO collected from dialog: {}", productDto.getName());
        return productDto;
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    // This method is called by ProductListViewController when the "Save" button from DialogPane is confirmed
    public boolean handleSave() {
        logger.debug("Attempting to save product from dialog.");
        if (isInputValid()) {
            saveClicked = true;
            logger.info("Product input valid, save confirmed.");
            return true;
        }
        logger.warn("Product input invalid.");
        return false; 
    }

    private boolean isInputValid() {
        StringBuilder errorMessage = new StringBuilder();

        if (nameField.getText() == null || nameField.getText().trim().isEmpty()) {
            errorMessage.append("- Product name is required.\n");
        } else if (nameField.getText().trim().length() < 3) {
             errorMessage.append("- Product name must be at least 3 characters.\n");
        }
        if (categoryComboBox.getValue() == null) {
            errorMessage.append("- Product category must be selected.\n");
        }
        
        // Validate selling price
        Double sellingPriceVal = sellingPriceSpinner.getValue();
        if (sellingPriceVal == null || sellingPriceVal <= 0) {
            errorMessage.append("- Selling price must be greater than 0.\n");
        }
        // Validate purchase price (can be 0, but not negative)
        Double purchasePriceVal = purchasePriceSpinner.getValue();
        if (purchasePriceVal != null && purchasePriceVal < 0) {
             errorMessage.append("- Purchase price cannot be negative.\n");
        }
        // Validate quantity
        Integer quantityVal = quantitySpinner.getValue();
        if (quantityVal == null || quantityVal < 0) {
            errorMessage.append("- Quantity in stock cannot be negative.\n");
        }


        if (errorMessage.length() == 0) {
            clearValidationError();
            return true;
        } else {
            validationErrorLabel.setText("Please correct the following errors:\n" + errorMessage.toString());
            validationErrorLabel.setManaged(true);
            validationErrorLabel.setVisible(true);
            return false;
        }
    }

    // This method allows ProductListViewController to get the DialogPane reference
    // if it needs to find buttons by type, though the standard lookupButton should work too.
    public void setDialogPane(DialogPane dialogPane) {
        this.dialogPane = dialogPane;
    }
}