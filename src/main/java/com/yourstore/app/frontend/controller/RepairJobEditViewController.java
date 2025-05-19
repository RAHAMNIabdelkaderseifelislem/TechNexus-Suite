package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.RepairJobDto;
import com.yourstore.app.backend.model.dto.UserBasicDto;
import com.yourstore.app.backend.model.enums.RepairStatus;
import com.yourstore.app.frontend.service.AuthClientService; // Or UserClientService
import com.yourstore.app.frontend.service.RepairClientService;
import com.yourstore.app.frontend.util.StageManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class RepairJobEditViewController {

    private static final Logger logger = LoggerFactory.getLogger(RepairJobEditViewController.class);

    @FXML private Button backToListButton; // Corresponds to the "Home" button in FXML
    @FXML private Label viewTitleLabel;
    @FXML private TextField customerNameField;
    @FXML private TextField customerPhoneField;
    @FXML private TextField customerEmailField;
    @FXML private TextField itemTypeField;
    @FXML private TextField itemBrandField;
    @FXML private TextField itemModelField;
    @FXML private TextField itemSerialNumberField;
    @FXML private TextArea reportedIssueArea;
    @FXML private TextArea technicianNotesArea;
    @FXML private ComboBox<RepairStatus> statusComboBox;
    @FXML private ComboBox<UserBasicDto> assignedToUserComboBox;
    @FXML private TextField estimatedCostField;
    @FXML private TextField actualCostField;
    @FXML private DatePicker estimatedCompletionDatePicker;
    @FXML private Label errorMessageLabel;
    @FXML private Label statusMessageLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelButton; // This is the "Back to List" button

    private final RepairClientService repairClientService;
    private final AuthClientService userClientService; 
    private final StageManager stageManager;
    private final ConfigurableApplicationContext springContext;

    private static RepairJobDto jobToEditHolder;
    private RepairJobDto currentJob;
    private boolean isEditMode = false;
    private final ObservableList<UserBasicDto> assignableUsers = FXCollections.observableArrayList();

    @Autowired
    public RepairJobEditViewController(RepairClientService rcs, AuthClientService ucs, StageManager sm, ConfigurableApplicationContext ctx) {
        this.repairClientService = rcs;
        this.userClientService = ucs;
        this.stageManager = sm;
        this.springContext = ctx;
    }
    
    public static void setJobToEdit(RepairJobDto job) {
        jobToEditHolder = job;
        logger.debug("Static jobToEditHolder set with job ID: {}", (job != null ? job.getId() : "null"));
    }

    @FXML
    public void initialize() {
        this.currentJob = jobToEditHolder;
        jobToEditHolder = null; // Important: Clear static holder after use

        logger.info("Initializing RepairJobEditViewController. Edit mode: {}", (currentJob != null));

        statusComboBox.setItems(FXCollections.observableArrayList(RepairStatus.values()));
        assignedToUserComboBox.setItems(assignableUsers);
        assignedToUserComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(UserBasicDto user) { return user != null ? user.getUsername() : "Unassigned"; }
            @Override public UserBasicDto fromString(String s) { return null; } // Not used for non-editable ComboBox
        });
        assignedToUserComboBox.getItems().add(0, null); // Add an "Unassigned" option

        loadAssignableUsers();
        clearMessages();
        errorMessageLabel.managedProperty().bind(errorMessageLabel.visibleProperty());
        statusMessageLabel.managedProperty().bind(statusMessageLabel.visibleProperty());


        if (currentJob != null) {
            isEditMode = true;
            viewTitleLabel.setText("Edit Repair Job - ID: " + currentJob.getId());
            populateFields();
        } else {
            isEditMode = false;
            viewTitleLabel.setText("Log New Repair Job");
            statusComboBox.setValue(RepairStatus.PENDING_ASSESSMENT);
            // Set other defaults for a new job if necessary
        }
    }
    
    private void clearMessages() {
        errorMessageLabel.setText("");
        statusMessageLabel.setText("");
        errorMessageLabel.setVisible(false);
        statusMessageLabel.setVisible(false);
    }

    private void showStatusMessage(String message) {
        clearMessages();
        statusMessageLabel.setText(message);
        statusMessageLabel.setVisible(true);
    }

    private void showErrorMessage(String message) {
        clearMessages();
        errorMessageLabel.setText(message);
        errorMessageLabel.setVisible(true);
    }


    private void loadAssignableUsers() {
        userClientService.getAssignableUsers()
            .thenAcceptAsync(users -> Platform.runLater(() -> {
                assignableUsers.setAll(users);
                assignableUsers.add(0, null); // Add null option for "Unassigned"
                if (currentJob != null && currentJob.getAssignedToUserId() != null) {
                    assignableUsers.stream()
                        .filter(u -> u != null && u.getId().equals(currentJob.getAssignedToUserId()))
                        .findFirst().ifPresent(assignedToUserComboBox::setValue);
                } else {
                    assignedToUserComboBox.getSelectionModel().select(null); // Select "Unassigned"
                }
                logger.debug("Assignable users loaded: {}", users.size());
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    showErrorMessage("Failed to load technicians: " + ex.getMessage());
                    logger.error("Failed to load assignable users", ex);
                });
                return null;
            });
    }

    private void populateFields() {
        if (currentJob == null) return;
        customerNameField.setText(currentJob.getCustomerName());
        customerPhoneField.setText(currentJob.getCustomerPhone());
        customerEmailField.setText(currentJob.getCustomerEmail());
        itemTypeField.setText(currentJob.getItemType());
        itemBrandField.setText(currentJob.getItemBrand());
        itemModelField.setText(currentJob.getItemModel());
        itemSerialNumberField.setText(currentJob.getItemSerialNumber());
        reportedIssueArea.setText(currentJob.getReportedIssue());
        technicianNotesArea.setText(currentJob.getTechnicianNotes());
        statusComboBox.setValue(currentJob.getStatus());
        estimatedCostField.setText(currentJob.getEstimatedCost() != null ? currentJob.getEstimatedCost().toPlainString() : "");
        actualCostField.setText(currentJob.getActualCost() != null ? currentJob.getActualCost().toPlainString() : "");
        estimatedCompletionDatePicker.setValue(currentJob.getEstimatedCompletionDate());
        // Assigned user is handled by ComboBox selection after loadAssignableUsers finishes
    }

    @FXML
    private void handleSaveRepairJob() {
        clearMessages();
        if (!validateInput()) {
            logger.warn("Validation failed for repair job save.");
            return;
        }

        RepairJobDto dtoToSave = isEditMode ? currentJob : new RepairJobDto();
        
        dtoToSave.setCustomerName(customerNameField.getText().trim());
        dtoToSave.setCustomerPhone(customerPhoneField.getText().trim());
        dtoToSave.setCustomerEmail(customerEmailField.getText().trim());
        dtoToSave.setItemType(itemTypeField.getText().trim());
        dtoToSave.setItemBrand(itemBrandField.getText().trim());
        dtoToSave.setItemModel(itemModelField.getText().trim());
        dtoToSave.setItemSerialNumber(itemSerialNumberField.getText().trim());
        dtoToSave.setReportedIssue(reportedIssueArea.getText().trim());
        dtoToSave.setTechnicianNotes(technicianNotesArea.getText().trim());
        dtoToSave.setStatus(statusComboBox.getValue());
        
        UserBasicDto selectedUser = assignedToUserComboBox.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            dtoToSave.setAssignedToUserId(selectedUser.getId());
            // Backend should use ID to fetch full User. DTO might not need username for saving.
            // dtoToSave.setAssignedToUsername(selectedUser.getUsername()); 
        } else {
            dtoToSave.setAssignedToUserId(null); // Unassigned
            // dtoToSave.setAssignedToUsername(null);
        }

        try {
            dtoToSave.setEstimatedCost(!estimatedCostField.getText().trim().isEmpty() ? new BigDecimal(estimatedCostField.getText().trim()).setScale(2, RoundingMode.HALF_UP) : null);
            dtoToSave.setActualCost(!actualCostField.getText().trim().isEmpty() ? new BigDecimal(actualCostField.getText().trim()).setScale(2, RoundingMode.HALF_UP) : null);
        } catch (NumberFormatException e) {
            showErrorMessage("Invalid cost format. Please use numbers (e.g., 123.45).");
            logger.warn("NumberFormatException for cost fields.", e);
            return;
        }
        dtoToSave.setEstimatedCompletionDate(estimatedCompletionDatePicker.getValue());

        showProgress(true, "Saving repair job...");

        CompletableFuture<RepairJobDto> saveFuture;
        if (isEditMode) {
            logger.info("Updating existing repair job ID: {}", currentJob.getId());
            saveFuture = repairClientService.updateRepairJob(currentJob.getId(), dtoToSave);
        } else {
            logger.info("Creating new repair job for customer: {}", dtoToSave.getCustomerName());
            saveFuture = repairClientService.createRepairJob(dtoToSave);
        }

        saveFuture.thenAcceptAsync(savedJob -> Platform.runLater(() -> {
            showProgress(false, "Save successful!");
            stageManager.showInfoAlert("Success", "Repair job " + (isEditMode ? "updated" : "created") + " successfully! ID: " + savedJob.getId());
            goBackToList(); // Navigate back to the list view
            logger.info("Repair job {} successfully with ID: {}", (isEditMode ? "updated" : "created"), savedJob.getId());
        }))
        .exceptionally(ex -> {
            Platform.runLater(() -> {
                showProgress(false, "Save failed.");
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                showErrorMessage("Failed to save repair job: " + cause.getMessage());
                logger.error("Failed to save repair job: {}", cause.getMessage(), cause);
            });
            return null;
        });
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();
        if (customerNameField.getText() == null || customerNameField.getText().trim().isEmpty()) errors.append("- Customer name is required.\n");
        if (itemTypeField.getText() == null || itemTypeField.getText().trim().isEmpty()) errors.append("- Item type is required.\n");
        if (reportedIssueArea.getText() == null || reportedIssueArea.getText().trim().isEmpty()) errors.append("- Reported issue is required.\n");
        if (statusComboBox.getValue() == null) errors.append("- Status must be selected.\n");

        // Validate cost fields if not empty
        if (!estimatedCostField.getText().trim().isEmpty()) {
            try { new BigDecimal(estimatedCostField.getText().trim()); } 
            catch (NumberFormatException e) { errors.append("- Estimated cost must be a valid number (e.g., 123.45).\n"); }
        }
         if (!actualCostField.getText().trim().isEmpty()) {
            try { new BigDecimal(actualCostField.getText().trim()); } 
            catch (NumberFormatException e) { errors.append("- Actual cost must be a valid number (e.g., 123.45).\n"); }
        }

        if (errors.length() > 0) {
            showErrorMessage("Please correct the following errors:\n" + errors.toString());
            return false;
        }
        return true;
    }

    @FXML
    private void handleCancel() { // This is now effectively "Back to List"
        logger.debug("Cancel/Back button clicked from Repair Job Edit view.");
        goBackToList();
    }
    
    @FXML
    private void handleGoHome() { // If you add a dedicated "Home" button to this view
        logger.debug("Go Home button clicked from Repair Job Edit view.");
        try {
            MainViewController mainViewController = springContext.getBean(MainViewController.class);
            mainViewController.handleShowDashboard();
        } catch (Exception e) {
            logger.error("Error navigating to home from Repair Job Edit: {}", e.getMessage(), e);
            stageManager.showErrorAlert("Navigation Error", "Could not return to the main dashboard.");
        }
    }

    private void goBackToList() {
        logger.debug("Navigating back to Repairs List view.");
         MainViewController mainViewController = springContext.getBean(MainViewController.class);
         if (mainViewController != null) {
            mainViewController.loadCenterView("/fxml/RepairsListView.fxml");
        } else {
            logger.warn("MainViewController not found, attempting fallback navigation for repairs list.");
            stageManager.showView("/fxml/RepairsListView.fxml", "Manage Repair Jobs"); // Fallback
        }
    }
    
    private void showProgress(boolean show, String message) {
        // This view doesn't have a global progress indicator from MainView.
        // We can disable save/cancel buttons.
        saveButton.setDisable(show);
        cancelButton.setDisable(show); // Or backToListButton
        if (backToListButton != null) backToListButton.setDisable(show);

        if(show) {
            showStatusMessage(message);
        }
        // The error/status messages are specific to this view
    }
}