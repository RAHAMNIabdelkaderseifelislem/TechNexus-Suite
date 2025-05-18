package com.yourstore.app.frontend.controller;

import com.yourstore.app.frontend.FrontendApplication; // If needed for context/stage
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane; // Assuming your MainView's root is BorderPane
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Component
public class MainViewController {

    @FXML private Label welcomeLabel;
    @FXML private Label backendStatusLabel;
    @FXML private BorderPane mainBorderPane; // Assuming your MainView.fxml root is a BorderPane and you give it an fx:id="mainBorderPane"

    private final Environment environment;
    private final ConfigurableApplicationContext springContext; // For loading FXML with Spring context

    @Autowired
    public MainViewController(Environment environment, ConfigurableApplicationContext springContext) {
        this.environment = environment;
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        String appTitle = environment.getProperty("javafx.application.title", "Computer Store App");
        welcomeLabel.setText("Welcome to " + appTitle + "!");

        if (springContext != null && springContext.isActive()) {
            backendStatusLabel.setText("Backend Status: Connected (Port: " + environment.getProperty("server.port","N/A") + ")");
            backendStatusLabel.setStyle("-fx-text-fill: green;");
        } else {
            backendStatusLabel.setText("Backend Status: Not Connected");
            backendStatusLabel.setStyle("-fx-text-fill: red;");
        }
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

    private void openInNewWindow(Parent root, String title) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        // stage.initModality(Modality.APPLICATION_MODAL); // If you want it to block other windows
        // stage.initOwner(mainBorderPane.getScene().getWindow()); // If opening from an existing window
        stage.show();
    }

    // Placeholder for other menu actions
}