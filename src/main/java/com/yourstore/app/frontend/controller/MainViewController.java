package com.yourstore.app.frontend.controller;

import com.yourstore.app.backend.model.enums.UserRole;
import com.yourstore.app.frontend.FrontendApplication; // If needed for context/stage
import com.yourstore.app.frontend.service.AdminClientService;
import com.yourstore.app.frontend.service.AuthClientService;
import com.yourstore.app.frontend.util.StageManager;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane; // Assuming your MainView's root is BorderPane
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Component
public class MainViewController {

    @FXML private Label welcomeLabel;
    @FXML private Label backendStatusLabel;
    @FXML private BorderPane mainBorderPane; // Assuming your MainView.fxml root is a BorderPane and you give it an fx:id="mainBorderPane"
    @FXML private Label loggedInUserLabel; // Add this for the new label


    private final AuthClientService authClientService;
    private final StageManager stageManager;

    private final Environment environment;
    private final ConfigurableApplicationContext springContext; // For loading FXML with Spring context

    private final AdminClientService adminClientService;

    @FXML private MenuItem backupDbMenuItem;
    @FXML private Label statusLabel; // Ensure this exists in FXML (e.g., bottom status bar)
    @FXML private ProgressIndicator progressIndicator; // Ensure this exists in FXML

    @Autowired
    public MainViewController(Environment environment, ConfigurableApplicationContext springContext,
                              AuthClientService authClientService, StageManager stageManager,
                              AdminClientService adminClientService) { // Added AdminClientService
        this.environment = environment;
        this.springContext = springContext;
        this.authClientService = authClientService;
        this.stageManager = stageManager;
        this.adminClientService = adminClientService; // Initialize
    }

    @FXML
    public void initialize() {
        loadUserDetails();
        // Conditionally show admin menu items based on roles
        authClientService.getCurrentUserDetails().thenAcceptAsync(details -> Platform.runLater(()->{
            if (details != null && details.get("roles") instanceof List) {
                List<String> roles = (List<String>) details.get("roles");
                if (backupDbMenuItem != null) {
                    backupDbMenuItem.setVisible(roles.contains(UserRole.ROLE_ADMIN.name()));
                }
                // Add similar logic for other admin-only menu items/buttons
            } else if (backupDbMenuItem != null) {
                 backupDbMenuItem.setVisible(false); // Hide if roles can't be determined
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
                    // Here you could also adapt UI based on roles
                    // For example: adminMenu.setVisible(roles.contains("ROLE_ADMIN"));
                } else {
                    loggedInUserLabel.setText("User: Unknown (or not fully logged in)");
                    // This case might indicate an issue or that /users/me wasn't hit properly after login
                    // Or session expired and this view was reloaded without re-authentication
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    loggedInUserLabel.setText("User: Error loading details");
                    System.err.println("Failed to load user details in MainView: " + ex.getMessage());
                });
                return null;
            });
    }

    @FXML
    private void handleLogout() {
        authClientService.logout()
            .thenRunAsync(() -> Platform.runLater(() -> {
                System.out.println("Logout successful on client side.");
                stageManager.showLoginView(); // Transition back to login view
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    System.err.println("Logout error: " + ex.getMessage());
                    ex.printStackTrace();
                    // Optionally show an error alert, but still attempt to go to login view
                    stageManager.showLoginView();
                });
                return null;
            });
    }

    @FXML
    private void handleExit() {
        Platform.exit();
        System.exit(0);
    }

    @FXML
    private void handleManageProducts() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/ProductListView.fxml")));
            loader.setControllerFactory(springContext::getBean); // IMPORTANT: Use Spring context for controller creation
            Parent productListRoot = loader.load();

            // Option 1: Replace center content of MainView (if MainView is a BorderPane)
            // Ensure MainView.fxml's root element has fx:id="mainBorderPane"
            if (mainBorderPane != null) {
                 mainBorderPane.setCenter(productListRoot);
            } else {
                // Option 2: Open in a new window/stage (Modal or Non-Modal)
                System.err.println("mainBorderPane is null. Opening Product List in a new window as fallback.");
                openInNewWindow(productListRoot, "Manage Products");
            }

        } catch (IOException e) {
            e.printStackTrace();
            // Show error alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not load Product Management View");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleViewSales() { // New method
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/SalesListView.fxml")));
            loader.setControllerFactory(springContext::getBean);
            Parent salesListRoot = loader.load();
            if (mainBorderPane != null) {
                mainBorderPane.setCenter(salesListRoot);
            } else {
                openInNewWindow(salesListRoot, "View Sales");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error Loading View", "Could not load the sales view: " + e.getMessage());
        }
    }

    @FXML
    private void handleNewSaleInMain() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/NewSaleView.fxml")));
            loader.setControllerFactory(springContext::getBean);
            Parent newSaleRoot = loader.load();
            if (mainBorderPane != null) {
                mainBorderPane.setCenter(newSaleRoot);
            } else {
                openInNewWindow(newSaleRoot, "New Sale");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error Loading View", "Could not load the new sale view: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleBackupDatabase() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory to Save Database Backup");
        // Set initial directory (optional)
        // String lastBackupPath = preferences.get("lastBackupPath", System.getProperty("user.home"));
        // directoryChooser.setInitialDirectory(new File(lastBackupPath));

        File selectedDirectory = directoryChooser.showDialog(stageManager.getPrimaryStage());

        if (selectedDirectory != null) {
            String backupPath = selectedDirectory.getAbsolutePath();
            // preferences.put("lastBackupPath", backupPath); // Save for next time (requires Preferences API)

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
                        System.err.println("Backup exception: " + ex.getMessage());
                        // ex.printStackTrace(); // Already printed by service or deeper layers
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        showErrorAlert("Backup Failed", "Could not complete database backup: " + cause.getMessage());
                    });
                    return null;
                });
        } else {
             if (statusLabel != null) statusLabel.setText("Database backup directory selection cancelled.");
             else System.out.println("Database backup directory selection cancelled.");
        }
    }

    
    
    private void showProgress(boolean show, String message) {
        if (progressIndicator != null) progressIndicator.setVisible(show);
        if (statusLabel != null) statusLabel.setText(message != null ? message : "");
    }
    
    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void openInNewWindow(Parent root, String title) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        // stage.initModality(Modality.APPLICATION_MODAL); // If you want it to block other windows
        // stage.initOwner(mainBorderPane.getScene().getWindow()); // If opening from an existing window
        stage.show();
    }

    public void loadCenterView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(fxmlPath)));
            loader.setControllerFactory(springContext::getBean); // Use Spring context
            Parent viewRoot = loader.load();
            if (mainBorderPane != null) {
                mainBorderPane.setCenter(viewRoot);
            } else {
                System.err.println("mainBorderPane is null in MainViewController. Cannot set center view.");
                // Fallback or error
                openInNewWindow(viewRoot, "View"); // Using existing helper
            }
        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("View Loading Error", "Failed to load view: " + fxmlPath + "\n" + e.getMessage());
        }
    }
    @FXML
    private void handleShowDashboard() { // New method
        loadCenterView("/fxml/DashboardView.fxml");
    }
}