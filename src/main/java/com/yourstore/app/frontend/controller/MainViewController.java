package com.yourstore.app.frontend.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component // Marks this class as a Spring component
public class MainViewController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label backendStatusLabel;

    private final Environment environment; // For accessing application.properties
    private final ConfigurableApplicationContext springContext;

    // Constructor injection for Spring dependencies
    @Autowired
    public MainViewController(Environment environment, ConfigurableApplicationContext springContext) {
        this.environment = environment;
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        // Set welcome message, potentially from properties
        String appTitle = environment.getProperty("javafx.application.title", "Computer Store App");
        welcomeLabel.setText("Welcome to " + appTitle + "!");

        // Check Spring context status
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

    // Placeholder for other menu actions
}