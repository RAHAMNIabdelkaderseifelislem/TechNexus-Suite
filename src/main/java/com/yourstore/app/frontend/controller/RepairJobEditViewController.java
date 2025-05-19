package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.RepairJobDto;
import com.yourstore.app.backend.model.dto.UserBasicDto; // For assignable users
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List; // For List of users
import java.util.concurrent.CompletableFuture;

@Component
public class RepairJobEditViewController {

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
    @FXML private Button cancelButton;

    private final RepairClientService repairClientService;
    private final AuthClientService userClientService; // Using AuthClientService for getAssignableUsers for now
    private final StageManager stageManager;
    private final ConfigurableApplicationContext springContext;


    private static RepairJobDto jobToEditHolder; // Static holder for simplicity
    private RepairJobDto currentJob;
    private boolean isEditMode = false;

    private ObservableList<UserBasicDto> assignableUsers = FXCollections.observableArrayList();

    @Autowired
    public RepairJobEditViewController(RepairClientService rcs, AuthClientService ucs, StageManager sm, ConfigurableApplicationContext ctx) {
        this.repairClientService = rcs;
        this.userClientService = ucs;
        this.stageManager = sm;
        this.springContext = ctx;
    }
    
    // Static setter to pass data before FXML is loaded by MainViewController
    public static void setJobToEdit(RepairJobDto job) {
        jobToEditHolder = job;
    }

    @FXML
    public void initialize() {
        this.currentJob = jobToEditHolder; // Get the job passed statically
        jobToEditHolder = null; // Clear static holder

        statusComboBox.setItems(FXCollections.observableArrayList(RepairStatus.values()));
        assignedToUserComboBox.setItems(assignableUsers);
        assignedToUserComboBox.setConverter(new StringConverter<UserBasicDto>() {
            @Override public String toString(UserBasicDto user) { return user != null ? user.getUsername() : null; }
            @Override public UserBasicDto fromString(String s) { return null; }
        });
        loadAssignableUsers();

        if (currentJob != null) {
            isEditMode = true;
            viewTitleLabel.setText("Edit Repair Job - ID: " + currentJob.getId());
            populateFields();
        } else {
            isEditMode = false;
            viewTitleLabel.setText("Log New Repair Job");
            statusComboBox.setValue(RepairStatus.PENDING_ASSESSMENT); // Default for new
        }
        clearMessages();
    }
    
    private void clearMessages() {
        errorMessageLabel.setText("");
        statusMessageLabel.setText("");
    }

    private void loadAssignableUsers() {
        userClientService.getAssignableUsers() // Assumes method exists in AuthClientService or a dedicated UserClientService
            .thenAcceptAsync(users -> Platform.runLater(() -> {
                assignableUsers.setAll(users);
                if (currentJob != null && currentJob.getAssignedToUserId() != null) {
                    assignableUsers.stream()
                        .filter(u -> u.getId().equals(currentJob.getAssignedToUserId()))
                        .findFirst().ifPresent(assignedToUserComboBox::setValue);
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> errorMessageLabel.setText("Failed to load technicians: " + ex.getMessage()));
                return null;
            });
    }

    private void populateFields() {
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
        // Assigned user handled by loadAssignableUsers and selection
        estimatedCostField.setText(currentJob.getEstimatedCost() != null ? currentJob.getEstimatedCost().toPlainString() : "");
        actualCostField.setText(currentJob.getActualCost() != null ? currentJob.getActualCost().toPlainString() : "");
        estimatedCompletionDatePicker.setValue(currentJob.getEstimatedCompletionDate());
    }

    @FXML
    private void handleSaveRepairJob() {
        clearMessages();
        if (!validateInput()) return;

        RepairJobDto dtoToSave = isEditMode ? currentJob : new RepairJobDto();
        
        dtoToSave.setCustomerName(customerNameField.getText());
        dtoToSave.setCustomerPhone(customerPhoneField.getText());
        dtoToSave.setCustomerEmail(customerEmailField.getText());
        dtoToSave.setItemType(itemTypeField.getText());
        dtoToSave.setItemBrand(itemBrandField.getText());
        dtoToSave.setItemModel(itemModelField.getText());
        dtoToSave.setItemSerialNumber(itemSerialNumberField.getText());
        dtoToSave.setReportedIssue(reportedIssueArea.getText());
        dtoToSave.setTechnicianNotes(technicianNotesArea.getText());
        dtoToSave.setStatus(statusComboBox.getValue());
        
        UserBasicDto selectedUser = assignedToUserComboBox.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            dtoToSave.setAssignedToUserId(selectedUser.getId());
            dtoToSave.setAssignedToUsername(selectedUser.getUsername()); // DTO might only need ID for backend
        } else {
            dtoToSave.setAssignedToUserId(null);
            dtoToSave.setAssignedToUsername(null);
        }

        try {
            if (!estimatedCostField.getText().trim().isEmpty()) dtoToSave.setEstimatedCost(new BigDecimal(estimatedCostField.getText()));
            else dtoToSave.setEstimatedCost(null);
        } catch (NumberFormatException e) { /* Handled by validateInput */ }
         try {
            if (!actualCostField.getText().trim().isEmpty()) dtoToSave.setActualCost(new BigDecimal(actualCostField.getText()));
            else dtoToSave.setActualCost(null);
        } catch (NumberFormatException e) { /* Handled by validateInput */ }
        dtoToSave.setEstimatedCompletionDate(estimatedCompletionDatePicker.getValue());

        saveButton.setDisable(true);
        cancelButton.setDisable(true);
        statusMessageLabel.setText("Saving repair job...");

        CompletableFuture<RepairJobDto> saveFuture;
        if (isEditMode) {
            saveFuture = repairClientService.updateRepairJob(currentJob.getId(), dtoToSave);
        } else {
            saveFuture = repairClientService.createRepairJob(dtoToSave);
        }

        saveFuture.thenAcceptAsync(savedJob -> Platform.runLater(() -> {
            statusMessageLabel.setText("Repair job " + (isEditMode ? "updated" : "created") + " successfully! ID: " + savedJob.getId());
            // Navigate back to list or show success
            goBackToList();
        }))
        .exceptionally(ex -> {
            Platform.runLater(() -> {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                errorMessageLabel.setText("Failed to save repair job: " + cause.getMessage());
                saveButton.setDisable(false);
                cancelButton.setDisable(false);
            });
            return null;
        });
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();
        if (customerNameField.getText() == null || customerNameField.getText().trim().isEmpty()) errors.append("- Customer name is required.\n");
        if (itemTypeField.getText() == null || itemTypeField.getText().trim().isEmpty()) errors.append("- Item type is required.\n");
        if (reportedIssueArea.getText() == null || reportedIssueArea.getText().trim().isEmpty()) errors.append("- Reported issue is required.\n");
        if (statusComboBox.getValue() == null) errors.append("- Status is required.\n");

        try {
            if (!estimatedCostField.getText().trim().isEmpty()) new BigDecimal(estimatedCostField.getText());
        } catch (NumberFormatException e) { errors.append("- Estimated cost must be a valid number.\n"); }
         try {
            if (!actualCostField.getText().trim().isEmpty()) new BigDecimal(actualCostField.getText());
        } catch (NumberFormatException e) { errors.append("- Actual cost must be a valid number.\n"); }


        if (errors.length() > 0) {
            errorMessageLabel.setText("Please correct the following errors:\n" + errors.toString());
            return false;
        }
        return true;
    }

    @FXML
    private void handleCancel() {
        goBackToList();
    }
    
    private void goBackToList() {
         MainViewController mainViewController = springContext.getBean(MainViewController.class);
         if (mainViewController != null) {
            mainViewController.loadCenterView("/fxml/RepairsListView.fxml");
        } else {
            // Fallback if MainViewController is not available for some reason
            stageManager.showView("/fxml/RepairsListView.fxml", "Manage Repairs");
        }
    }
}