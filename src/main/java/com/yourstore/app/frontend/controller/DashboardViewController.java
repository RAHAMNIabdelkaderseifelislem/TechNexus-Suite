package com.yourstore.app.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Component
public class DashboardViewController {

    @FXML private Label totalProductsLabel;
    @FXML private Label totalSalesCountLabel;
    @FXML private Label totalRevenueLabel;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${server.port:8080}")
    private String serverPort;
    private String getDashboardBaseUrl() { return "http://localhost:" + serverPort + "/api/v1/dashboard"; }


    @Autowired
    public DashboardViewController(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @FXML
    public void initialize() {
        loadDashboardMetrics();
    }

    @FXML
    private void loadDashboardMetrics() {
        totalProductsLabel.setText("Loading...");
        totalSalesCountLabel.setText("Loading...");
        totalRevenueLabel.setText("Loading...");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getDashboardBaseUrl() + "/metrics"))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAcceptAsync(response -> {
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            Map<String, Object> metrics = objectMapper.readValue(response.body(), new TypeReference<>() {});
                            totalProductsLabel.setText(metrics.getOrDefault("totalProducts", "N/A").toString());
                            totalSalesCountLabel.setText(metrics.getOrDefault("totalSalesCount", "N/A").toString());
                            Object revenueObj = metrics.get("totalSalesRevenue");
                            if (revenueObj instanceof Number) {
                                totalRevenueLabel.setText(String.format("%.2f", ((Number) revenueObj).doubleValue()));
                            } else {
                                totalRevenueLabel.setText(revenueObj != null ? revenueObj.toString() : "N/A");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            showError("Failed to parse dashboard metrics.");
                        }
                    } else {
                        showError("Failed to load dashboard metrics. Status: " + response.statusCode());
                    }
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> showError("Error fetching dashboard metrics: " + ex.getMessage()));
                return null;
            });
    }
    
    private void showError(String message) {
        totalProductsLabel.setText("Error");
        totalSalesCountLabel.setText("Error");
        totalRevenueLabel.setText("Error");
        // Consider using a shared Alert utility here
        System.err.println(message);
    }
}