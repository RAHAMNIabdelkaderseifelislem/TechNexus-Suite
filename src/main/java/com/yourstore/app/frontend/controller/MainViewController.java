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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox; // For topBarHBox and statusBarHBox
import javafx.scene.layout.Pane;  // For spacer in status/top bar
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment; // Not directly used now, but kept if needed later
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

    // --- FXML Injected Fields ---
    @FXML private BorderPane mainBorderPane;
    @FXML private MenuBar mainMenuBar;
    @FXML private HBox topBarHBox;
    @FXML private Label loggedInUserLabel;
    @FXML private VBox sideNavigationPanel;
    @FXML private StackPane mainContentArea; // Main area to load different views
    @FXML private HBox statusBarHBox;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;

    // MenuItems for programmatic access (e.g., role-based visibility)
    @FXML private MenuItem logoutMenuItem;
    @FXML private MenuItem manageProductsMenuItem; // From Stock menu
    @FXML private MenuItem backupDbMenuItem;     // From Admin menu
    @FXML private Menu adminMenu;                // The Admin Menu itself

    // Services and Utilities
    private final Environment environment; // Kept for potential future use (e.g. app title from properties)
    private final ConfigurableApplicationContext springContext;
    private final AuthClientService authClientService;
    private final StageManager stageManager;
    private final AdminClientService adminClientService;

    private Button lastSelectedNavButton = null;

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
        logger.info("Initializing MainViewController with new layout...");
        showProgress(false, "Ready.");
        createSideNavigation();
        loadUserDetails(); // This will also trigger setupRoleBasedVisibilityWithRoles

        // Load initial view (Dashboard) into the main content area
        handleShowDashboard();
        logger.info("MainViewController initialized. Dashboard loaded.");
    }

    private void createSideNavigation() {
        if (sideNavigationPanel == null) {
            logger.error("sideNavigationPanel is null. Cannot create side navigation.");
            return;
        }
        sideNavigationPanel.getChildren().clear();
        sideNavigationPanel.setAlignment(Pos.TOP_CENTER); // Align buttons to top

        // Arguments for createSideNavButton: Text, Icon Path, Action Handler
        Button dashboardNav = createSideNavButton("Dashboard", "/icons/dashboard_64.png", event -> handleShowDashboard());
        Button productsNav = createSideNavButton("Products", "/icons/products_64.png", event -> handleManageProducts());
        Button newSaleNav = createSideNavButton("New Sale", "/icons/new_sale_64.png", event -> handleNewSaleInMain());
        Button salesRecordsNav = createSideNavButton("Sales Records", "/icons/sales_records_64.png", event -> handleViewSales());
        Button newPurchaseNav = createSideNavButton("New Purchase", "/icons/new_purchase_64.png", event -> handleNewPurchaseInMain());
        Button purchasesRecordsNav = createSideNavButton("Purchase Records", "/icons/purchase_records_64.png", event -> handleViewPurchases());
        Button repairsNav = createSideNavButton("Repairs", "/icons/repairs_64.png", event -> handleViewRepairs());

        sideNavigationPanel.getChildren().addAll(dashboardNav, productsNav, newSaleNav, salesRecordsNav, newPurchaseNav, purchasesRecordsNav, repairsNav);
        
        // Select Dashboard by default if it's the first item and exists
        if (dashboardNav != null) {
            selectNavButton(dashboardNav);
        }
        logger.info("Side navigation panel created and populated.");
    }

    private Button createSideNavButton(String text, String iconPath, EventHandler<ActionEvent> action) {
        Button navButton = new Button(text.toUpperCase()); // Uppercase text for a more "button" feel
        navButton.getStyleClass().add("side-nav-button");
        navButton.setPrefWidth(Double.MAX_VALUE); // Make button take full width of VBox
        navButton.setAlignment(Pos.CENTER_LEFT); // Align text and graphic to left
        navButton.setContentDisplay(ContentDisplay.LEFT); // Icon to the left of text
        navButton.setGraphicTextGap(15); // Increased gap

        navButton.setOnAction(event -> {
            selectNavButton(navButton);
            action.handle(event);
        });

        try {
            String correctedIconPath = iconPath.startsWith("/") ? iconPath : "/" + iconPath;
            if (getClass().getResource(correctedIconPath) != null) {
                ImageView iconView = new ImageView(new Image(getClass().getResourceAsStream(correctedIconPath)));
                iconView.setFitHeight(28); // Slightly larger icons
                iconView.setFitWidth(28);
                navButton.setGraphic(iconView);
            } else {
                 logger.warn("Side nav icon not found at classpath: {}", correctedIconPath);
            }
        } catch (Exception e) {
            logger.error("Error loading side nav icon {}: {}", iconPath, e.getMessage(), e);
        }
        return navButton;
    }
    
    private void selectNavButton(Button selectedButton) {
        if (lastSelectedNavButton != null) {
            lastSelectedNavButton.getStyleClass().remove("selected");
        }
        if (selectedButton != null) {
            selectedButton.getStyleClass().add("selected");
        }
        lastSelectedNavButton = selectedButton;
    }

    private void loadUserDetails() {
        authClientService.getCurrentUserDetails()
            .thenAcceptAsync(userDetailsMap -> Platform.runLater(() -> {
                if (userDetailsMap != null && userDetailsMap.get("username") instanceof String) {
                    String username = (String) userDetailsMap.get("username");
                    @SuppressWarnings("unchecked")
                    List<String> roles = (List<String>) userDetailsMap.getOrDefault("roles", List.of());
                    loggedInUserLabel.setText("User: " + username); // Simpler display
                    logger.info("User details loaded for top bar: {}", username);
                    setupRoleBasedVisibilityWithRoles(roles);
                } else {
                    loggedInUserLabel.setText("User: Guest");
                    logger.warn("Could not load user details or user not authenticated for label display.");
                    setupRoleBasedVisibilityWithRoles(List.of()); // Apply default (no roles) visibility
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    loggedInUserLabel.setText("User: Error");
                    logger.error("Failed to load user details in MainView: {}", ex.getMessage(), ex);
                    setupRoleBasedVisibilityWithRoles(List.of()); // Apply default visibility on error
                });
                return null;
            });
    }
    
    private void setupRoleBasedVisibilityWithRoles(List<String> roles) {
        if (roles == null) roles = List.of(); // Ensure roles is not null
        boolean isAdmin = roles.contains(UserRole.ROLE_ADMIN.name());
        boolean isManager = roles.contains(UserRole.ROLE_MANAGER.name());
        // boolean isStaff = roles.contains(UserRole.ROLE_STAFF.name()); // Example if needed

        if (backupDbMenuItem != null) backupDbMenuItem.setVisible(isAdmin);
        if (adminMenu != null) adminMenu.setVisible(isAdmin);
        
        // Example: Control visibility of side navigation buttons
        // This requires sideNavButtons to be stored or iterated
        sideNavigationPanel.getChildren().forEach(node -> {
            if (node instanceof Button) {
                Button btn = (Button) node;
                if (btn.getText().equalsIgnoreCase("PURCHASES") || btn.getText().equalsIgnoreCase("NEW PURCHASE")) {
                    btn.setVisible(isAdmin || isManager);
                    btn.setManaged(isAdmin || isManager); // Important for layout
                }
                // Add other role-based visibility for side nav buttons as needed
            }
        });
        logger.info("Role-based UI visibility updated. Admin access: {}", isAdmin);
    }

    public void loadCenterView(String fxmlPath) {
        if (mainContentArea == null) {
            logger.error("mainContentArea (StackPane) is null. Cannot load view: {}", fxmlPath);
            stageManager.showErrorAlert("Navigation Error", "Main content display area is not available.");
            return;
        }
        if (fxmlPath == null || fxmlPath.trim().isEmpty()){
            logger.warn("Cannot load view: FXML path is null or empty.");
            stageManager.showErrorAlert("Navigation Error", "Invalid view path provided.");
            return;
        }

        String viewId = fxmlPathToId(fxmlPath);
        showProgress(true, "Loading: " + viewId + "...");
        logger.debug("Attempting to load center view: {} (ID: {}) into mainContentArea", fxmlPath, viewId);

        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(fxmlPath), "FXML file not found: " + fxmlPath));
            loader.setControllerFactory(springContext::getBean); // CRUCIAL for Spring DI in loaded controllers
            Parent viewRoot = loader.load();
            viewRoot.setId(viewId); 
            
            mainContentArea.getChildren().clear(); 
            mainContentArea.getChildren().add(viewRoot); 

            showProgress(false, viewId + " loaded.");
            logger.info("View {} loaded into mainContentArea.", viewId);
        } catch (IOException | NullPointerException e) {
            logger.error("Failed to load view FXML {}: {}", fxmlPath, e.getMessage(), e);
            showProgress(false, "Error loading view.");
            stageManager.showErrorAlert("View Loading Error", "Failed to load screen: " + viewId + "\nDetails: " + e.getMessage());
            mainContentArea.getChildren().clear();
            Label errorLabel = new Label("Error loading view: " + viewId + "\n" + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: -fx-ruby-error; -fx-padding: 20px;");
            mainContentArea.getChildren().add(errorLabel);
        }
    }
    
    private String fxmlPathToId(String fxmlPath) {
        if(fxmlPath == null || fxmlPath.isEmpty()) return "unknownView";
        String id = fxmlPath.substring(fxmlPath.lastIndexOf('/') + 1).replace(".fxml", "");
        return id.isEmpty() ? "unknownView" : id;
    }

    // --- Menu & Side Navigation Action Handlers ---
    @FXML public void handleShowDashboard() { 
        loadCenterView("/fxml/DashboardView.fxml"); 
        selectSideNavButtonByText("DASHBOARD");
    }
    @FXML private void handleManageProducts() { 
        loadCenterView("/fxml/ProductListView.fxml"); 
        selectSideNavButtonByText("PRODUCTS");
    }
    @FXML private void handleNewSaleInMain() { 
        loadCenterView("/fxml/NewSaleView.fxml"); 
        selectSideNavButtonByText("NEW SALE");
    }
    @FXML private void handleViewSales() { 
        loadCenterView("/fxml/SalesListView.fxml"); 
        selectSideNavButtonByText("SALES RECORDS");
    }
    @FXML private void handleNewPurchaseInMain() { 
        loadCenterView("/fxml/NewPurchaseView.fxml"); 
        selectSideNavButtonByText("NEW PURCHASE");
    }
    @FXML private void handleViewPurchases() { 
        loadCenterView("/fxml/PurchasesListView.fxml"); 
        selectSideNavButtonByText("PURCHASE RECORDS");
    }
    @FXML private void handleViewRepairs() { 
        loadCenterView("/fxml/RepairsListView.fxml"); 
        selectSideNavButtonByText("REPAIRS");
    }
    
    private void selectSideNavButtonByText(String buttonText) {
        sideNavigationPanel.getChildren().stream()
            .filter(node -> node instanceof Button && ((Button)node).getText().equalsIgnoreCase(buttonText))
            .findFirst().ifPresent(node -> selectNavButton((Button)node));
    }

    // Placeholder Handlers
    @FXML private void handleSalesReport() { stageManager.showInfoAlert("Sales Report", "Custom sales reports feature is under construction.");}
    @FXML private void handleStockReport() { stageManager.showInfoAlert("Stock Report", "Custom stock reports feature is under construction.");}
    @FXML private void handleAppSettings() { stageManager.showInfoAlert("Application Settings", "Application settings screen is under construction.");}
    @FXML private void handleAbout() { stageManager.showInfoAlert("About TechNexus Suite", "Version 1.0\nComputer Retail & Repair Management");}

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
        Optional<ButtonType> result = stageManager.showConfirmationAlert("Confirm Exit", "Are you sure you want to exit TechNexus Suite?", "");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            logger.info("Application exiting via menu.");
            Platform.exit();
            System.exit(0);
        }
    }
    
    // --- UI Feedback Helper Methods ---
    private void showProgress(boolean show, String message) {
        if (progressIndicator != null) progressIndicator.setVisible(show);
        if (statusLabel != null) statusLabel.setText(message != null ? message : "");
    }
    // Note: showErrorAlert and showInfoAlert are now primarily called via stageManager instance.
}