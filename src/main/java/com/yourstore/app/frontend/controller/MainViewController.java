package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.enums.UserRole;
import com.yourstore.app.frontend.service.AdminClientService;
import com.yourstore.app.frontend.service.AuthClientService;
import com.yourstore.app.frontend.util.StageManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class MainViewController {

    @FXML private BorderPane mainBorderPane;
    @FXML private Label loggedInUserLabel;
    @FXML private MenuItem logoutMenuItem;
    @FXML private MenuItem manageProductsMenuItem; // From Stock menu
    @FXML private MenuItem backupDbMenuItem;     // From Admin menu
    @FXML private TilePane mainTilesPane;        // For main content area
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;


    private final Environment environment;
    private final ConfigurableApplicationContext springContext;
    private final AuthClientService authClientService;
    private final StageManager stageManager;
    private final AdminClientService adminClientService;

    @Autowired
    public MainViewController(Environment environment, ConfigurableApplicationContext springContext,
                              AuthClientService authClientService, StageManager stageManager,
                              AdminClientService adminClientService) {
        this.environment = environment;
        this.springContext = springContext;
        this.authClientService = authClientService;
        this.stageManager = stageManager;
        this.adminClientService = adminClientService;
    }

    @FXML
    public void initialize() {
        showProgress(false, "Ready.");
        loadUserDetails();
        createMainTiles();
        setupRoleBasedVisibility();
    }
    
    private void setupRoleBasedVisibility() {
         authClientService.getCurrentUserDetails().thenAcceptAsync(details -> Platform.runLater(()->{
            if (backupDbMenuItem != null) { 
                if (details != null && details.get("roles") instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) details.get("roles");
                    backupDbMenuItem.setVisible(roles.contains(UserRole.ROLE_ADMIN.name()));
                    // Add more role-based visibility for menus or tiles here
                } else {
                    backupDbMenuItem.setVisible(false); 
                }
            }
        }));
    }

    private void loadUserDetails() {
        authClientService.getCurrentUserDetails()
            .thenAcceptAsync(userDetailsMap -> Platform.runLater(() -> {
                if (userDetailsMap != null && userDetailsMap.containsKey("username")) {
                    String username = (String) userDetailsMap.get("username");
                    List<String> roles = (List<String>) userDetailsMap.getOrDefault("roles", List.of());
                    loggedInUserLabel.setText("User: " + username + " " + roles.toString());
                } else {
                    loggedInUserLabel.setText("User: Unknown");
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> loggedInUserLabel.setText("User: Error loading details"));
                return null;
            });
    }

    private void createMainTiles() {
        mainTilesPane.getChildren().clear();
        mainTilesPane.setTileAlignment(Pos.CENTER_LEFT); // Align tiles

        // Define tiles: Text, Icon Path (relative to resources), Action
        Button dashboardTile = createTile("Dashboard", "/icons/dashboard_64.png", event -> handleShowDashboard());
        Button productsTile = createTile("Products", "/icons/products_64.png", event -> handleManageProducts());
        Button newSaleTile = createTile("New Sale", "/icons/new_sale_64.png", event -> handleNewSaleInMain());
        Button salesRecordsTile = createTile("Sales Records", "/icons/sales_records_64.png", event -> handleViewSales());
        Button newPurchaseTile = createTile("New Purchase", "/icons/new_purchase_64.png", event -> handleNewPurchaseInMain());
        Button purchasesRecordsTile = createTile("Purchase Records", "/icons/purchase_records_64.png", event -> handleViewPurchases());
        Button repairsTile = createTile("Repairs", "/icons/repairs_64.png", event -> handleViewRepairs());
        // Add more tiles as needed

        mainTilesPane.getChildren().addAll(dashboardTile, productsTile, newSaleTile, salesRecordsTile, newPurchaseTile, purchasesRecordsTile, repairsTile);
    }

    private Button createTile(String text, String iconPath, EventHandler<ActionEvent> action) {
        Button tileButton = new Button(text);
        tileButton.setPrefSize(180, 150); // Increased size for better touch/click
        tileButton.setContentDisplay(ContentDisplay.TOP); // Icon on top, text below
        tileButton.getStyleClass().add("main-tile-button"); // Add a style class for CSS

        // Load icon
        try {
            // Check if iconPath starts with a slash, if not, add it for consistency with getResourceAsStream
            String correctedIconPath = iconPath.startsWith("/") ? iconPath : "/" + iconPath;
            if (getClass().getResource(correctedIconPath) != null) {
                ImageView iconView = new ImageView(new Image(getClass().getResourceAsStream(correctedIconPath)));
                iconView.setFitHeight(64); // Adjusted icon size
                iconView.setFitWidth(64);
                tileButton.setGraphic(iconView);
            } else {
                 System.err.println("Icon not found: " + correctedIconPath + " (Trying from: " + iconPath + ")");
            }
        } catch (Exception e) {
            System.err.println("Error loading icon " + iconPath + ": " + e.getMessage());
            // e.printStackTrace();
        }
        tileButton.setOnAction(action);
        return tileButton;
    }

    public void loadCenterView(String fxmlPath) {
        showProgress(true, "Loading view...");
        // If already on dashboard and trying to load dashboard, or current view is same as fxmlPath, do nothing or just refresh
        if (mainBorderPane.getCenter() != null && mainBorderPane.getCenter().getId() != null && mainBorderPane.getCenter().getId().equals(fxmlPathToId(fxmlPath))) {
             showProgress(false, "View already loaded.");
             // Optionally call a refresh method on the controller if it exists
             return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            loader.setControllerFactory(springContext::getBean);
            Parent viewRoot = loader.load();
            viewRoot.setId(fxmlPathToId(fxmlPath)); // Set an ID for the root to check if it's already loaded
            mainBorderPane.setCenter(viewRoot);
            showProgress(false, "View loaded.");
        } catch (IOException e) {
            e.printStackTrace();
            showProgress(false, "Error loading view.");
            stageManager.showErrorAlert("View Loading Error", "Failed to load view: " + fxmlPath + "\nError: " + e.getMessage());
        }
    }
    
    private String fxmlPathToId(String fxmlPath) {
        // Simple way to create an ID from FXML path, e.g., "/fxml/DashboardView.fxml" -> "DashboardView"
        if(fxmlPath == null) return null;
        return fxmlPath.substring(fxmlPath.lastIndexOf('/') + 1).replace(".fxml", "");
    }


    // --- Menu Action Handlers ---
    @FXML private void handleShowDashboard() { loadCenterView("/fxml/DashboardView.fxml"); }
    @FXML private void handleManageProducts() { loadCenterView("/fxml/ProductListView.fxml"); }
    @FXML private void handleNewSaleInMain() { loadCenterView("/fxml/NewSaleView.fxml"); }
    @FXML private void handleViewSales() { loadCenterView("/fxml/SalesListView.fxml"); }
    @FXML private void handleNewPurchaseInMain() { loadCenterView("/fxml/NewPurchaseView.fxml"); }
    @FXML private void handleViewPurchases() { loadCenterView("/fxml/PurchasesListView.fxml"); }
    @FXML private void handleViewRepairs() { stageManager.showInfoAlert("Repairs", "The Repairs module will be implemented soon!"); }
@FXML private void handleSalesReport() { stageManager.showInfoAlert("Sales Report", "Custom sales reports are coming soon!"); }
@FXML private void handleStockReport() { stageManager.showInfoAlert("Stock Report", "Custom stock reports are coming soon!"); }
@FXML private void handleAppSettings() { stageManager.showInfoAlert("Application Settings", "Application settings will be available in a future update."); }
@FXML private void handleAbout() { stageManager.showInfoAlert("About Computer Store Management", "Version 1.0\nDeveloped with JavaFX and Spring Boot."); }


    @FXML
    private void handleBackupDatabase() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory to Save Database Backup");
        File selectedDirectory = directoryChooser.showDialog(stageManager.getPrimaryStage());

        if (selectedDirectory != null) {
            String backupPath = selectedDirectory.getAbsolutePath();
            showProgress(true, "Starting database backup to: " + backupPath + "...");
            adminClientService.backupDatabase(backupPath)
                .thenAcceptAsync(responseMap -> Platform.runLater(() -> {
                    String message = responseMap.getOrDefault("message", "Backup process finished.");
                    String filePath = responseMap.getOrDefault("path", "N/A");
                    showProgress(false, message);
                    showInfoAlert("Backup Success", message + "\nFile saved to: " + filePath);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showProgress(false, "Backup failed.");
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        stageManager.showErrorAlert("Backup Failed", "Database backup error: " + cause.getMessage());
                    });
                    return null;
                });
        } else {
             showProgress(false, "Backup directory selection cancelled.");
        }
    }

    @FXML
    private void handleLogout() {
        showProgress(true, "Logging out...");
        authClientService.logout()
            .thenRunAsync(() -> Platform.runLater(() -> {
                showProgress(false, "Logged out.");
                stageManager.showLoginView();
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    showProgress(false, "Logout error.");
                    stageManager.showErrorAlert("Logout Error", "An error occurred during logout: " + ex.getMessage());
                    stageManager.showLoginView(); // Still attempt to go to login view
                });
                return null;
            });
    }

    @FXML
    private void handleExit() {
        Platform.exit();
        System.exit(0);
    }
    
    // --- Helper Methods ---
    private void showProgress(boolean show, String message) {
        if (progressIndicator != null) progressIndicator.setVisible(show);
        if (statusLabel != null) statusLabel.setText(message != null ? message : "");
    }


     private void openInNewWindow(Parent root, String title) { // Fallback if mainBorderPane is null
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.show();
    }
}