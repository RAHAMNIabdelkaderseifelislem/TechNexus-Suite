package com.yourstore.app.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
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
    @FXML private BarChart<String, Number> salesByCategoryChart;
    @FXML private CategoryAxis categoryAxis;
    @FXML private NumberAxis salesCountAxis;


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
        salesByCategoryChart.getData().clear();


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
                            } else if (revenueObj instanceof String) { // If backend sends it as formatted string
                                totalRevenueLabel.setText((String) revenueObj);
                            }
                             else {
                                totalRevenueLabel.setText(revenueObj != null ? revenueObj.toString() : "N/A");
                            }

                            // Populate Bar Chart
                            if (metrics.get("salesByCategory") instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Number> salesCatData = (Map<String, Number>) metrics.get("salesByCategory");
                                XYChart.Series<String, Number> series = new XYChart.Series<>();
                                series.setName("Sales Count");
                                salesCatData.forEach((category, count) -> {
                                    series.getData().add(new XYChart.Data<>(category, count.longValue())); // Ensure count is Number
                                });
                                salesByCategoryChart.getData().setAll(series);
                            } else {
                                 salesByCategoryChart.getData().clear();
                                 System.out.println("Sales by category data not found or not in expected format.");
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                            showError("Failed to parse dashboard metrics: " + e.getMessage());
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
        salesByCategoryChart.getData().clear();
        System.err.println("Dashboard Error: " + message);
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle("Dashboard Error");
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}