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
import javafx.scene.Node; // Import Node
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger; // Using SLF4J for logging
import org.slf4j.LoggerFactory; // Using SLF4J for logging
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class MainViewController {

    private static final Logger logger = LoggerFactory.getLogger(MainViewController.class);

    @FXML private BorderPane mainBorderPane;
    @FXML private Label loggedInUserLabel;
    @FXML private MenuItem logoutMenuItem;
    @FXML private MenuItem manageProductsMenuItem;
    @FXML private MenuItem backupDbMenuItem;
    @FXML private TilePane mainTilesPane;
    @FXML private ScrollPane homeScrollPane; // Injected ScrollPane for the tiles
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    // Add FXML fields for other menu items if their visibility needs to be controlled by roles
    @FXML private Menu adminMenu; // Assuming the "Admin" menu has fx:id="adminMenu"
    // Add fx:id to other menus/items if needed for role-based visibility

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
        logger.info("Initializing MainViewController...");
        showProgress(false, "Ready.");
        loadUserDetails();
        createMainTiles(); // Populates mainTilesPane
        setupRoleBasedVisibility();

        // Ensure the home view (tiles within the scroll pane) is shown initially
        if (mainBorderPane != null && homeScrollPane != null) {
            // Ensure mainTilesPane is the content of homeScrollPane (it should be if FXML is correct)
            if (homeScrollPane.getContent() != mainTilesPane) {
                 logger.warn("homeScrollPane's content is not mainTilesPane. Setting it now.");
                 homeScrollPane.setContent(mainTilesPane);
            }
            mainBorderPane.setCenter(homeScrollPane);
            logger.info("Home/Tile view set as initial center content.");
        } else {
            logger.error("Critical error: mainBorderPane or homeScrollPane is null during initial setup.");
            stageManager.showErrorAlert("Startup Error", "Main layout components could not be initialized.");
        }
    }

    private void loadUserDetails() {
        authClientService.getCurrentUserDetails()
            .thenAcceptAsync(userDetailsMap -> Platform.runLater(() -> {
                if (userDetailsMap != null && userDetailsMap.get("username") instanceof String) {
                    String username = (String) userDetailsMap.get("username");
                    @SuppressWarnings("unchecked") // Assuming roles are List<String> from backend
                    List<String> roles = (List<String>) userDetailsMap.getOrDefault("roles", List.of());
                    loggedInUserLabel.setText("User: " + username + " " + roles.toString());
                    logger.info("User details loaded: {}", username);
                    setupRoleBasedVisibilityWithRoles(roles); // Re-apply visibility once roles are known
                } else {
                    loggedInUserLabel.setText("User: Unknown / Not Authenticated");
                    logger.warn("Could not load user details or user not authenticated for label display.");
                    setupRoleBasedVisibilityWithRoles(List.of()); // Apply default visibility
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    loggedInUserLabel.setText("User: Error loading details");
                    logger.error("Failed to load user details in MainView: {}", ex.getMessage(), ex);
                    setupRoleBasedVisibilityWithRoles(List.of()); // Apply default visibility on error
                });
                return null;
            });
    }
    
    private void setupRoleBasedVisibility() {
        // Initial setup, might be called again with actual roles
        setupRoleBasedVisibilityWithRoles(List.of());
    }

    private void setupRoleBasedVisibilityWithRoles(List<String> roles) {
        boolean isAdmin = roles.contains(UserRole.ROLE_ADMIN.name());
        boolean isManager = roles.contains(UserRole.ROLE_MANAGER.name());
        boolean isStaff = roles.contains(UserRole.ROLE_STAFF.name());

        if (backupDbMenuItem != null) {
            backupDbMenuItem.setVisible(isAdmin);
        }
        if (adminMenu != null) { // Assuming the whole Admin menu visibility
             adminMenu.setVisible(isAdmin);
        }
        // Example: Purchases tile/menu might be for Admin/Manager
        // if (purchasesTile != null) purchasesTile.setVisible(isAdmin || isManager);
        // if (purchasesMenu != null) purchasesMenu.setVisible(isAdmin || isManager);
        
        // You can add more specific controls here based on their fx:id and roles
        logger.info("Role-based UI visibility updated. Admin: {}", isAdmin);
    }


    private void createMainTiles() {
        if (mainTilesPane == null) {
            logger.error("mainTilesPane is null. Cannot create main navigation tiles.");
            return;
        }
        mainTilesPane.getChildren().clear();
        mainTilesPane.setTileAlignment(Pos.CENTER_LEFT);

        Button dashboardTile = createTile("Dashboard", "/icons/dashboard_64.png", event -> handleShowDashboard());
        Button productsTile = createTile("Products", "/icons/products_64.png", event -> handleManageProducts());
        Button newSaleTile = createTile("New Sale", "/icons/new_sale_64.png", event -> handleNewSaleInMain());
        Button salesRecordsTile = createTile("Sales Records", "/icons/sales_records_64.png", event -> handleViewSales());
        Button newPurchaseTile = createTile("New Purchase", "/icons/new_purchase_64.png", event -> handleNewPurchaseInMain());
        Button purchasesRecordsTile = createTile("Purchase Records", "/icons/purchase_records_64.png", event -> handleViewPurchases());
        Button repairsTile = createTile("Repairs", "/icons/repairs_64.png", event -> handleViewRepairs());

        mainTilesPane.getChildren().addAll(dashboardTile, productsTile, newSaleTile, salesRecordsTile, newPurchaseTile, purchasesRecordsTile, repairsTile);
        logger.info("Main navigation tiles created and added.");
    }

    private Button createTile(String text, String iconPath, EventHandler<ActionEvent> action) {
        Button tileButton = new Button(text);
        tileButton.setPrefSize(180, 150);
        tileButton.setContentDisplay(ContentDisplay.TOP);
        tileButton.getStyleClass().add("main-tile-button");
        tileButton.setOnAction(action);

        try {
            String correctedIconPath = iconPath.startsWith("/") ? iconPath : "/" + iconPath;
            if (getClass().getResource(correctedIconPath) != null) {
                ImageView iconView = new ImageView(new Image(getClass().getResourceAsStream(correctedIconPath)));
                iconView.setFitHeight(64);
                iconView.setFitWidth(64);
                tileButton.setGraphic(iconView);
            } else {
                 logger.warn("Icon not found at classpath: {}", correctedIconPath);
            }
        } catch (Exception e) {
            logger.error("Error loading icon {}: {}", iconPath, e.getMessage(), e);
        }
        return tileButton;
    }

    public void showHomeTiles() {
        if (mainBorderPane == null || homeScrollPane == null || mainTilesPane == null) {
            logger.error("Cannot show home tiles: Main layout components are not injected or null.");
            stageManager.showErrorAlert("Navigation Error", "Could not display the home screen components.");
            return;
        }
        createMainTiles(); // Ensure tiles are populated
        mainBorderPane.setCenter(homeScrollPane); // Set the ScrollPane (containing tiles) as center
        logger.info("Home/Tile view displayed.");
    }

    public void loadCenterView(String fxmlPath) {
        if (mainBorderPane == null) {
            logger.error("mainBorderPane is null. Cannot load center view: {}", fxmlPath);
            stageManager.showErrorAlert("Navigation Error", "Main application layout is not available.");
            return;
        }
        if (fxmlPath == null || fxmlPath.trim().isEmpty()){
            logger.warn("Cannot load view: FXML path is null or empty.");
            stageManager.showErrorAlert("Navigation Error", "Invalid view path provided.");
            return;
        }

        String viewId = fxmlPathToId(fxmlPath);
        showProgress(true, "Loading: " + viewId + "...");
        logger.debug("Attempting to load center view: {} (ID: {})", fxmlPath, viewId);

        Node currentCenter = mainBorderPane.getCenter();
        // Check if it's already loaded (and not the homeScrollPane itself being re-identified as another view)
        if (currentCenter != null && currentCenter != homeScrollPane && currentCenter.getId() != null && currentCenter.getId().equals(viewId)) {
            showProgress(false, viewId + " already loaded.");
            logger.debug("View {} already loaded.", viewId);
            return;
        }
        // If the target is the dashboard/home view, use showHomeTiles
        if ("/fxml/DashboardView.fxml".equals(fxmlPath) || "/fxml/MainViewTiles.fxml".equals(fxmlPath)) { // Assuming Dashboard is where tiles live, or you have a dedicated tiles FXML
             showHomeTiles(); // This will ensure tiles are shown
             showProgress(false, "Dashboard/Home view loaded.");
             logger.debug("Navigated to home/dashboard tiles view via loadCenterView redirect.");
             return;
        }


        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(fxmlPath), "FXML file not found: " + fxmlPath));
            loader.setControllerFactory(springContext::getBean);
            Parent viewRoot = loader.load();
            viewRoot.setId(viewId);
            mainBorderPane.setCenter(viewRoot);
            showProgress(false, viewId + " loaded.");
            logger.info("View {} loaded into center.", viewId);
        } catch (IOException | NullPointerException e) { // Catch NullPointerException for getResource
            logger.error("Failed to load view FXML {}: {}", fxmlPath, e.getMessage(), e);
            showProgress(false, "Error loading view.");
            stageManager.showErrorAlert("View Loading Error", "Failed to load view: " + viewId + "\nError: " + e.getMessage());
        }
    }
    
    private String fxmlPathToId(String fxmlPath) {
        if(fxmlPath == null) return "unknownView";
        String id = fxmlPath.substring(fxmlPath.lastIndexOf('/') + 1).replace(".fxml", "");
        return id.isEmpty() ? "unknownView" : id;
    }

    // --- Menu Action Handlers ---
    @FXML private void handleShowDashboard() { showHomeTiles(); /* Or loadCenterView("/fxml/DashboardView.fxml"); if dashboard is separate from tiles */ }
    @FXML private void handleManageProducts() { loadCenterView("/fxml/ProductListView.fxml"); }
    @FXML private void handleNewSaleInMain() { loadCenterView("/fxml/NewSaleView.fxml"); }
    @FXML private void handleViewSales() { loadCenterView("/fxml/SalesListView.fxml"); }
    @FXML private void handleNewPurchaseInMain() { loadCenterView("/fxml/NewPurchaseView.fxml"); }
    @FXML private void handleViewPurchases() { loadCenterView("/fxml/PurchasesListView.fxml"); }
    @FXML private void handleViewRepairs() { loadCenterView("/fxml/RepairsListView.fxml"); }
    
    @FXML private void handleSalesReport() { stageManager.showInfoAlert("Sales Report", "Custom sales reports feature is under construction.");}
    @FXML private void handleStockReport() { stageManager.showInfoAlert("Stock Report", "Custom stock reports feature is under construction.");}
    @FXML private void handleAppSettings() { stageManager.showInfoAlert("Application Settings", "Application settings screen is under construction.");}
    @FXML private void handleAbout() { stageManager.showInfoAlert("About Computer Store Management", "Version 1.0\nDeveloped with Spring Boot & JavaFX.");}

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
                    stageManager.showInfoAlert("Backup Success", message + "\nFile saved to: " + filePath);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showProgress(false, "Backup failed.");
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        stageManager.showErrorAlert("Backup Failed", "Database backup error: " + cause.getMessage());
                        logger.error("Database backup failed", cause);
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
                    stageManager.showLoginView(); 
                    logger.error("Logout failed", ex);
                });
                return null;
            });
    }

    @FXML
    private void handleExit() {
        Optional<ButtonType> result = stageManager.showConfirmationAlert("Confirm Exit", "Are you sure you want to exit the application?", "");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            logger.info("Application exiting via menu.");
            Platform.exit();
            System.exit(0);
        }
    }
    
    // --- Helper Methods for UI feedback ---
    private void showProgress(boolean show, String message) {
        if (progressIndicator != null) progressIndicator.setVisible(show);
        if (statusLabel != null) statusLabel.setText(message != null ? message : "");
    }
    // Alert methods are now in StageManager, but keeping these private wrappers
    // could be useful if MainViewController needs to do something before/after showing alert.
    // For now, directly using stageManager.showXxxAlert() is also fine in handlers.
}