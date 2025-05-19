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
import org.springframework.context.ConfigurableApplicationContext;
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

    // Metric Card Labels
    @FXML private Label todaysSalesLabel;
    @FXML private Label totalProductsLabel; // Was already there
    @FXML private Label pendingRepairsLabel;
    @FXML private Label lowStockItemsLabel; // New label for the card

    // Metric Card VBoxes (for applying accent styles)
    @FXML private VBox todaysSalesCard;
    @FXML private VBox productsInStockCard; // Renamed from totalProductsCard for clarity
    @FXML private VBox pendingRepairsCard;
    @FXML private VBox lowStockItemsCard;

    // Chart (as before)
    @FXML private BarChart<String, Number> salesByCategoryChart; // Or rename to weeklySalesChart
    @FXML private CategoryAxis categoryAxis;
    @FXML private NumberAxis salesCountAxis; // Or rename to salesAmountAxis

    // Placeholder VBoxes
    @FXML private VBox latestRepairsPlaceholder;
    @FXML private VBox recentSalesPlaceholder;
    @FXML private VBox lowStockAlertsPlaceholder;


    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final StageManager stageManager;
    private final ConfigurableApplicationContext springContext; // For accessing MainViewController

    @Value("${server.port:8080}")
    private String serverPort;
    private String getDashboardBaseUrl() { return "http://localhost:" + serverPort + "/api/v1/dashboard"; }

    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));

    @Autowired
    public DashboardViewController(HttpClient httpClient, ObjectMapper objectMapper, StageManager stageManager, ConfigurableApplicationContext springContext) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.stageManager = stageManager;
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {
        logger.info("Initializing DashboardViewController.");
        // Initial chart setup (axis labels etc. can be set here if not in FXML)
        // categoryAxis.setLabel("Day of Week"); // Or Product Category for the current chart
        // salesCountAxis.setLabel("Sales Amount");
        loadDashboardMetrics();
    }

    @FXML
    private void loadDashboardMetrics() {
        logger.info("Loading dashboard metrics...");
        setMetricsLoadingState();
        salesByCategoryChart.getData().clear();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getDashboardBaseUrl() + "/metrics"))
                .GET().build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAcceptAsync(response -> Platform.runLater(() -> {
                if (response.statusCode() == 200) {
                    try {
                        Map<String, Object> metrics = objectMapper.readValue(response.body(), new TypeReference<>() {});
                        logger.debug("Dashboard metrics received: {}", metrics);

                        // Populate Metric Cards
                        Object todaysSalesObj = metrics.get("todaysSalesRevenue");
                        if (todaysSalesCard != null) {
                            todaysSalesCard.getStyleClass().removeAll("dashboard-card-accent-teal");
                            if (todaysSalesObj instanceof Number) {
                                double revenue = ((Number) todaysSalesObj).doubleValue();
                                todaysSalesLabel.setText(currencyFormatter.format(revenue));
                                if (revenue > 0) todaysSalesCard.getStyleClass().add("dashboard-card-accent-teal");
                            } else {
                                todaysSalesLabel.setText(todaysSalesObj != null ? todaysSalesObj.toString() : "$0.00");
                            }
                        }

                        totalProductsLabel.setText(metrics.getOrDefault("totalProducts", "0").toString());

                        Object pendingRepairsObj = metrics.get("pendingRepairsCount");
                        if (pendingRepairsCard != null) {
                            pendingRepairsCard.getStyleClass().removeAll("dashboard-card-accent-amber");
                            String pendingText = (pendingRepairsObj != null) ? pendingRepairsObj.toString() : "0";
                            pendingRepairsLabel.setText(pendingText);
                            if (pendingRepairsObj instanceof Number && ((Number)pendingRepairsObj).intValue() > 0) {
                                pendingRepairsCard.getStyleClass().add("dashboard-card-accent-amber");
                            }
                        }
                        
                        Object lowStockObj = metrics.get("lowStockItemsCount");
                        if (lowStockItemsCard != null) {
                            lowStockItemsCard.getStyleClass().removeAll("dashboard-card-accent-ruby");
                            String lowStockText = (lowStockObj != null) ? lowStockObj.toString() : "0";
                            lowStockItemsLabel.setText(lowStockText);
                            if (lowStockObj instanceof Number && ((Number)lowStockObj).intValue() > 0) {
                                lowStockItemsCard.getStyleClass().add("dashboard-card-accent-ruby");
                            }
                        }

                        // Populate "Weekly Sales" Chart (currently sales by category)
                        // TODO: Change backend to provide actual weekly sales data for this chart
                        if (metrics.get("salesByCategory") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Number> salesCatData = (Map<String, Number>) metrics.get("salesByCategory");
                            XYChart.Series<String, Number> series = new XYChart.Series<>();
                            series.setName("Sales Count"); // Or "Sales Amount" if data changes
                            if (salesCatData.isEmpty()) {
                                logger.info("No sales by category data for chart.");
                            } else {
                                salesCatData.forEach((category, count) -> series.getData().add(new XYChart.Data<>(category, count)));
                            }
                            salesByCategoryChart.getData().setAll(series);
                        } else {
                             logger.warn("Sales by category data not found or in unexpected format for chart.");
                             salesByCategoryChart.getData().clear();
                        }

                    } catch (IOException e) {
                        logger.error("Failed to parse dashboard metrics: {}", e.getMessage(), e);
                        showErrorUIState("Failed to parse dashboard data.");
                    }
                } else {
                    logger.error("Failed to load dashboard metrics. Status: {}", response.statusCode());
                    showErrorUIState("Server error loading dashboard (Status: " + response.statusCode() + ")");
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    logger.error("Exception fetching dashboard metrics: {}", ex.getMessage(), ex);
                    showErrorUIState("Network error fetching dashboard data.");
                });
                return null;
            });
    }
    
    private void setMetricsLoadingState() {
        todaysSalesLabel.setText("Loading...");
        totalProductsLabel.setText("Loading...");
        pendingRepairsLabel.setText("Loading...");
        lowStockItemsLabel.setText("Loading...");

        if (todaysSalesCard != null) todaysSalesCard.getStyleClass().removeAll("dashboard-card-accent-teal");
        if (pendingRepairsCard != null) pendingRepairsCard.getStyleClass().removeAll("dashboard-card-accent-amber");
        if (lowStockItemsCard != null) lowStockItemsCard.getStyleClass().removeAll("dashboard-card-accent-ruby");
    }
    
    private void showErrorUIState(String message) {
        todaysSalesLabel.setText("Error");
        totalProductsLabel.setText("Error");
        pendingRepairsLabel.setText("Error");
        lowStockItemsLabel.setText("Error");
        salesByCategoryChart.getData().clear();
        stageManager.showErrorAlert("Dashboard Error", message);
    }

    // --- Navigation Handlers for Hyperlinks ---
    @FXML private void navigateToViewRepairs() {
        springContext.getBean(MainViewController.class).loadCenterView("/fxml/RepairsListView.fxml");
    }
    @FXML private void navigateToViewSales() {
        springContext.getBean(MainViewController.class).loadCenterView("/fxml/SalesListView.fxml");
    }
    @FXML private void navigateToProducts() { // For "Order Stock" or "View All Products"
        springContext.getBean(MainViewController.class).loadCenterView("/fxml/ProductListView.fxml");
    }
}