package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.dto.RepairJobDto;
import com.yourstore.app.backend.model.enums.RepairStatus; // For status column
import com.yourstore.app.frontend.service.RepairClientService;
import com.yourstore.app.frontend.util.StageManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle; // For LocalDate formatting

@Component
public class RepairsListViewController {

    @FXML private TableView<RepairJobDto> repairsTableView;
    @FXML private TableColumn<RepairJobDto, Long> repairIdColumn;
    @FXML private TableColumn<RepairJobDto, String> customerNameColumn;
    @FXML private TableColumn<RepairJobDto, String> itemTypeColumn;
    @FXML private TableColumn<RepairJobDto, String> itemBrandModelColumn;
    @FXML private TableColumn<RepairJobDto, String> reportedIssueColumn;
    @FXML private TableColumn<RepairJobDto, RepairStatus> statusColumn;
    @FXML private TableColumn<RepairJobDto, String> assignedToColumn;
    @FXML private TableColumn<RepairJobDto, LocalDateTime> dateReceivedColumn;
    @FXML private TableColumn<RepairJobDto, LocalDate> estCompletionColumn;

    @FXML private Button newRepairButton;
    @FXML private Button editRepairButton;
    @FXML private Button refreshButton;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;

    private final RepairClientService repairClientService;
    private final StageManager stageManager;
    private final ConfigurableApplicationContext springContext;
    private final ObservableList<RepairJobDto> repairJobsList = FXCollections.observableArrayList();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);


    @Autowired
    public RepairsListViewController(RepairClientService repairClientService, StageManager stageManager, ConfigurableApplicationContext springContext) {
        this.repairClientService = repairClientService;
        this.stageManager = stageManager;
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        loadRepairJobs();
        repairsTableView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> editRepairButton.setDisable(newSelection == null)
        );
    }

    private void setupTableColumns() {
        repairIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        itemTypeColumn.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        itemBrandModelColumn.setCellValueFactory(cellData -> {
            RepairJobDto job = cellData.getValue();
            String brand = job.getItemBrand() != null ? job.getItemBrand() : "";
            String model = job.getItemModel() != null ? job.getItemModel() : "";
            return new SimpleStringProperty(brand + (!brand.isEmpty() && !model.isEmpty() ? " / " : "") + model);
        });
        reportedIssueColumn.setCellValueFactory(new PropertyValueFactory<>("reportedIssue"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
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
        repairsTableView.setItems(repairJobsList);
    }

    @FXML
    private void handleRefresh() {
        loadRepairJobs();
    }

    private void loadRepairJobs() {
        showProgress(true, "Loading repair jobs...");
        repairClientService.getAllRepairJobs()
            .thenAcceptAsync(jobs -> Platform.runLater(() -> {
                repairJobsList.setAll(jobs);
                showProgress(false, "Repair jobs loaded. Found " + jobs.size() + " records.");
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showProgress(false, "Error loading repair jobs: " + cause.getMessage());
                    // Alert is handled by client service
                });
                return null;
            });
    }
    
    @FXML
    private void handleNewRepairJob() {
        openRepairJobEditView(null);
    }

    @FXML
    private void handleEditRepairJob() {
        RepairJobDto selectedJob = repairsTableView.getSelectionModel().getSelectedItem();
        if (selectedJob != null) {
            openRepairJobEditView(selectedJob);
        } else {
            stageManager.showInfoAlert("No Selection", "Please select a repair job to view/edit.");
        }
    }

    private void openRepairJobEditView(RepairJobDto jobToEdit) {
        // This is where we need access to the RepairJobEditViewController.
        // We can load it into a new dialog or a main content area.
        // For consistency, let's load it into the main content area via MainViewController.
        MainViewController mainViewController = springContext.getBean(MainViewController.class);
        if (mainViewController != null) {
            // We need a way to pass the jobToEdit to the RepairJobEditViewController
            // One way is to have a static field or a method in the target controller, then load.
            // Or more Spring-like, use a shared context/bean if it were a very complex state.
            // For now, a simple "setter" on the controller after loading it is feasible.
            
            RepairJobEditViewController.setJobToEdit(jobToEdit); // Static setter for simplicity here
            mainViewController.loadCenterView("/fxml/RepairJobEditView.fxml");
        } else {
            stageManager.showErrorAlert("Navigation Error", "Cannot access MainViewController to open repair edit view.");
        }
    }

    private void showProgress(boolean show, String message) {
        progressIndicator.setVisible(show);
        statusLabel.setText(message != null ? message : "");
    }
}