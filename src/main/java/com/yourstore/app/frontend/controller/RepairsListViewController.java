package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.RepairJobDto;
import com.yourstore.app.backend.model.enums.RepairStatus;
import com.yourstore.app.frontend.service.RepairClientService;
import com.yourstore.app.frontend.util.StageManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

@Component
public class RepairsListViewController {

    private static final Logger logger = LoggerFactory.getLogger(RepairsListViewController.class);

    // --- FXML Injected Fields ---
    @FXML private TableView<RepairJobDto> repairsTableView;
    @FXML private TableColumn<RepairJobDto, Long> repairIdColumn;
    @FXML private TableColumn<RepairJobDto, String> customerNameColumn;
    @FXML private TableColumn<RepairJobDto, String> itemTypeColumn;
    @FXML private TableColumn<RepairJobDto, String> itemBrandModelColumn;
    // @FXML private TableColumn<RepairJobDto, String> reportedIssueColumn; // Display in edit view
    @FXML private TableColumn<RepairJobDto, RepairStatus> statusColumn;
    @FXML private TableColumn<RepairJobDto, String> assignedToColumn;
    @FXML private TableColumn<RepairJobDto, LocalDateTime> dateReceivedColumn;
    @FXML private TableColumn<RepairJobDto, LocalDate> estCompletionColumn;

    @FXML private TextField searchRepairField;
    @FXML private Button newRepairButton;
    @FXML private Button editRepairButton;
    @FXML private Button refreshButton;
    @FXML private Button homeButton;

    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    // --- Services and Utilities ---
    private final RepairClientService repairClientService;
    private final StageManager stageManager;
    private final ConfigurableApplicationContext springContext;

    // --- Data Lists ---
    private final ObservableList<RepairJobDto> repairJobsMasterList = FXCollections.observableArrayList();
    private FilteredList<RepairJobDto> filteredRepairJobsData;

    // --- Formatters ---
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM); // For LocalDate
    private final DateTimeFormatter searchDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @Autowired
    public RepairsListViewController(RepairClientService repairClientService, StageManager stageManager, ConfigurableApplicationContext springContext) {
        this.repairClientService = repairClientService;
        this.stageManager = stageManager;
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        logger.info("Initializing RepairsListViewController.");
        showProgress(false, "Ready.");

        filteredRepairJobsData = new FilteredList<>(repairJobsMasterList, p -> true);

        searchRepairField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredRepairJobsData.setPredicate(job -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();

                if (String.valueOf(job.getId()).contains(lowerCaseFilter)) return true;
                if (job.getCustomerName() != null && job.getCustomerName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (job.getItemType() != null && job.getItemType().toLowerCase().contains(lowerCaseFilter)) return true;
                if (job.getItemBrand() != null && job.getItemBrand().toLowerCase().contains(lowerCaseFilter)) return true;
                if (job.getItemModel() != null && job.getItemModel().toLowerCase().contains(lowerCaseFilter)) return true;
                if (job.getStatus() != null && job.getStatus().getDisplayName().toLowerCase().contains(lowerCaseFilter)) return true; // Search by display name
                if (job.getAssignedToUsername() != null && job.getAssignedToUsername().toLowerCase().contains(lowerCaseFilter)) return true;
                if (job.getDateReceived() != null) {
                     if (dateTimeFormatter.format(job.getDateReceived()).toLowerCase().contains(lowerCaseFilter)) return true;
                     if (searchDateFormatter.format(job.getDateReceived()).toLowerCase().contains(lowerCaseFilter)) return true;
                }
                if (job.getEstimatedCompletionDate() != null && dateFormatter.format(job.getEstimatedCompletionDate()).toLowerCase().contains(lowerCaseFilter)) return true;
                
                return false;
            });
        });

        SortedList<RepairJobDto> sortedData = new SortedList<>(filteredRepairJobsData);
        sortedData.comparatorProperty().bind(repairsTableView.comparatorProperty());
        repairsTableView.setItems(sortedData);

        setupTableColumns();
        
        repairsTableView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> editRepairButton.setDisable(newSelection == null)
        );
        // Make rows double-clickable to edit
        repairsTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && repairsTableView.getSelectionModel().getSelectedItem() != null) {
                handleEditRepairJob();
            }
        });

        loadRepairJobs();
    }

    private void setupTableColumns() {
        logger.debug("Setting up repairs table columns.");
        repairIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        itemTypeColumn.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        itemBrandModelColumn.setCellValueFactory(cellData -> {
            RepairJobDto job = cellData.getValue();
            String brand = job.getItemBrand() != null ? job.getItemBrand() : "";
            String model = job.getItemModel() != null ? job.getItemModel() : "";
            return new SimpleStringProperty(brand + (!brand.isEmpty() && !model.isEmpty() ? " / " : "") + model);
        });
        // reportedIssueColumn.setCellValueFactory(new PropertyValueFactory<>("reportedIssue")); // Usually too long for list
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status")); // Enum's toString() will be used (displayName)
        assignedToColumn.setCellValueFactory(new PropertyValueFactory<>("assignedToUsername"));

        dateReceivedColumn.setCellValueFactory(new PropertyValueFactory<>("dateReceived"));
        dateReceivedColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : dateTimeFormatter.format(item));
            }
        });
        estCompletionColumn.setCellValueFactory(new PropertyValueFactory<>("estimatedCompletionDate"));
        estCompletionColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : dateFormatter.format(item));
            }
        });
    }

    @FXML
    private void handleRefresh() {
        logger.info("Refresh repair jobs button clicked.");
        searchRepairField.clear();
        loadRepairJobs();
    }

    private void loadRepairJobs() {
        showProgress(true, "Loading repair jobs...");
        repairClientService.getAllRepairJobs()
            .thenAcceptAsync(jobs -> Platform.runLater(() -> {
                repairJobsMasterList.setAll(jobs);
                showProgress(false, "Repair jobs loaded. Found " + repairJobsMasterList.size() + " records.");
                logger.info("Loaded {} repair jobs.", repairJobsMasterList.size());
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    repairJobsMasterList.clear();
                    showProgress(false, "Error loading repair jobs.");
                    logger.error("Error loading repair jobs: {}", ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage(), ex);
                    // Alert is handled by RepairClientService
                });
                return null;
            });
    }
    
    @FXML
    private void handleNewRepairJob() {
        logger.info("New Repair Job button clicked.");
        openRepairJobEditView(null, "Log New Repair Job");
    }

    @FXML
    private void handleEditRepairJob() {
        RepairJobDto selectedJob = repairsTableView.getSelectionModel().getSelectedItem();
        if (selectedJob != null) {
            logger.info("Edit Repair Job button clicked for ID: {}.", selectedJob.getId());
            openRepairJobEditView(selectedJob, "Edit Repair Job - ID: " + selectedJob.getId());
        } else {
            stageManager.showInfoAlert("No Selection", "Please select a repair job to view or edit.");
        }
    }

    private void openRepairJobEditView(RepairJobDto jobToEdit, String viewTitle) {
        try {
            // Pass data to the controller before loading its FXML
            RepairJobEditViewController.setJobToEdit(jobToEdit); 
            
            MainViewController mainViewController = springContext.getBean(MainViewController.class);
            mainViewController.loadCenterView("/fxml/RepairJobEditView.fxml");
            // The title of the view itself is set within RepairJobEditViewController.initialize()
        } catch (Exception e) {
            logger.error("Error opening repair job edit view for job ID {}: {}", (jobToEdit != null ? jobToEdit.getId() : "new"), e.getMessage(), e);
            stageManager.showErrorAlert("Navigation Error", "Could not open the repair job form.");
        }
    }

    @FXML
    private void handleGoHome() {
        logger.debug("Go Home button clicked from Repairs List.");
        try {
            MainViewController mainViewController = springContext.getBean(MainViewController.class);
            mainViewController.handleShowDashboard();
        } catch (Exception e) {
            logger.error("Error navigating to home (dashboard) from Repairs List: {}", e.getMessage(), e);
            stageManager.showErrorAlert("Navigation Error", "Could not return to the main dashboard.");
        }
    }

    private void showProgress(boolean show, String message) {
        if (progressIndicator != null) progressIndicator.setVisible(show);
        if (statusLabel != null) statusLabel.setText(message != null ? message : "");
    }
}