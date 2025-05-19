package com.yourstore.app.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourstore.app.frontend.util.StageManager; // Import StageManager for alerts
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
// import java.math.BigDecimal; // Not directly needed for parsing Number
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat; // For currency formatting
import java.util.Locale; // For currency formatting
import java.util.Map;

@Component
public class DashboardViewController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardViewController.class);

    @FXML private Label totalProductsLabel;
    @FXML private Label totalSalesCountLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label pendingRepairsLabel; // For the new card

    @FXML private BarChart<String, Number> salesByCategoryChart;
    @FXML private CategoryAxis categoryAxis; // Although not directly manipulated after FXML load, good to have if needed
    @FXML private NumberAxis salesCountAxis;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final StageManager stageManager; // For alerts

    @Value("${server.port:8080}")
    private String serverPort;
    private String getDashboardBaseUrl() { return "http://localhost:" + serverPort + "/api/v1/dashboard"; }

    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US")); // Example: US Dollar

    @Autowired
    public DashboardViewController(HttpClient httpClient, ObjectMapper objectMapper, StageManager stageManager) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.stageManager = stageManager;
    }

    @FXML
    public void initialize() {
        logger.info("Initializing DashboardViewController.");
        // Set initial loading text for the new card
        if (pendingRepairsLabel != null) {
            pendingRepairsLabel.setText("Loading...");
        }
        loadDashboardMetrics();
    }

    @FXML
    private void loadDashboardMetrics() {
        logger.info("Loading dashboard metrics...");
        setLabelsLoading();
        salesByCategoryChart.getData().clear(); // Clear previous chart data

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getDashboardBaseUrl() + "/metrics"))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAcceptAsync(response -> { // Run UI updates on JavaFX Application Thread
                Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            Map<String, Object> metrics = objectMapper.readValue(response.body(), new TypeReference<>() {});
                            logger.debug("Metrics received: {}", metrics);

                            totalProductsLabel.setText(metrics.getOrDefault("totalProducts", "N/A").toString());
                            totalSalesCountLabel.setText(metrics.getOrDefault("totalSalesCount", "N/A").toString());
                            
                            Object revenueObj = metrics.get("totalSalesRevenue");
                            if (revenueObj instanceof Number) {
                                totalRevenueLabel.setText(currencyFormatter.format(((Number) revenueObj).doubleValue()));
                                // Example: Apply style class based on value
                                VBox revenueCard = (VBox) totalRevenueLabel.getParent();
                                revenueCard.getStyleClass().remove("dashboard-card-teal"); // remove others if any
                                if (((Number) revenueObj).doubleValue() > 0) {
                                    revenueCard.getStyleClass().add("dashboard-card-teal");
                                }
                            } else {
                                totalRevenueLabel.setText(revenueObj != null ? revenueObj.toString() : "N/A");
                            }

                            // Example for pending repairs (assuming backend adds this metric)
                            if (pendingRepairsLabel != null) {
                                pendingRepairsLabel.setText(metrics.getOrDefault("pendingRepairsCount", "N/A").toString());
                                VBox repairsCard = (VBox) pendingRepairsLabel.getParent();
                                repairsCard.getStyleClass().removeAll("dashboard-card-amber", "dashboard-card-ruby");
                                Object pendingRepairs = metrics.get("pendingRepairsCount");
                                if (pendingRepairs instanceof Number && ((Number)pendingRepairs).intValue() > 0) {
                                     repairsCard.getStyleClass().add("dashboard-card-amber");
                                }
                            }


                            // Populate Bar Chart for Sales by Category
                            if (metrics.get("salesByCategory") instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Number> salesCatData = (Map<String, Number>) metrics.get("salesByCategory");
                                XYChart.Series<String, Number> series = new XYChart.Series<>();
                                series.setName("Sales Count");
                                if (salesCatData.isEmpty()) {
                                    logger.info("No sales by category data to display in chart.");
                                    // Optionally display a message on the chart
                                } else {
                                    salesCatData.forEach((category, count) -> {
                                        series.getData().add(new XYChart.Data<>(category, count));
                                    });
                                }
                                salesByCategoryChart.getData().setAll(series); // Clears old series and adds new one
                            } else {
                                 logger.warn("Sales by category data not found or not in expected map format.");
                                 salesByCategoryChart.getData().clear();
                            }

                        } catch (IOException e) {
                            logger.error("Failed to parse dashboard metrics: {}", e.getMessage(), e);
                            showErrorUI("Failed to parse dashboard data.");
                        }
                    } else {
                        logger.error("Failed to load dashboard metrics. Status: {}", response.statusCode());
                        showErrorUI("Failed to load dashboard metrics. Server returned status: " + response.statusCode());
                    }
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    logger.error("Exception while fetching dashboard metrics: {}", ex.getMessage(), ex);
                    showErrorUI("Error fetching dashboard metrics: " + ex.getCause().getMessage());
                });
                return null;
            });
    }
    
    private void setLabelsLoading() {
        totalProductsLabel.setText("Loading...");
        totalSalesCountLabel.setText("Loading...");
        totalRevenueLabel.setText("Loading...");
        if (pendingRepairsLabel != null) {
            pendingRepairsLabel.setText("Loading...");
        }
        // Clear style classes that might color the metric value
        if (totalRevenueLabel != null && totalRevenueLabel.getParent() instanceof VBox) {
            ((VBox)totalRevenueLabel.getParent()).getStyleClass().remove("dashboard-card-teal");
        }
        if (pendingRepairsLabel != null && pendingRepairsLabel.getParent() instanceof VBox) {
            ((VBox)pendingRepairsLabel.getParent()).getStyleClass().removeAll("dashboard-card-amber", "dashboard-card-ruby");
        }
    }
    
    private void showErrorUI(String message) {
        totalProductsLabel.setText("Error");
        totalSalesCountLabel.setText("Error");
        totalRevenueLabel.setText("Error");
        if (pendingRepairsLabel != null) {
             pendingRepairsLabel.setText("Error");
        }
        salesByCategoryChart.getData().clear();
        stageManager.showErrorAlert("Dashboard Error", message); // Use StageManager for alerts
    }
}