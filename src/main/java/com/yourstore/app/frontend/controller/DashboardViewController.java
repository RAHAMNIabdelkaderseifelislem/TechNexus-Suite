// src/main/java/com/yourstore/app/frontend/controller/DashboardViewController.java
package com.yourstore.app.frontend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourstore.app.backend.model.dto.ProductDto;
import com.yourstore.app.backend.model.dto.RepairJobDto;
import com.yourstore.app.backend.model.dto.SaleDto;
import com.yourstore.app.backend.model.enums.RepairStatus;
import com.yourstore.app.frontend.util.StageManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart; // Import PieChart
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip; // For pie chart tooltips
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
// import javafx.scene.text.TextFlow; // Not currently used, commented out
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DashboardViewController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardViewController.class);

    // --- FXML Injected Fields ---
    // Metric Card Labels & Containers
    @FXML private Label todaysSalesLabel;
    @FXML private Label totalProductsLabel;
    @FXML private Label pendingRepairsLabel;
    @FXML private Label lowStockItemsLabel;

    @FXML private VBox todaysSalesCard;
    @FXML private VBox productsInStockCard;
    @FXML private VBox pendingRepairsCard;
    @FXML private VBox lowStockItemsCard;

    // New FXML Fields for Financial Summary
    @FXML private Label salesLast7DaysLabel;
    @FXML private Label purchasesLast7DaysLabel;
    @FXML private Label profitLast7DaysLabel;
    @FXML private Label salesLast30DaysLabel;
    @FXML private Label purchasesLast30DaysLabel;
    @FXML private Label profitLast30DaysLabel;

    // Latest Repairs Table
    @FXML private TableView<RepairJobDto> latestRepairsTableView;
    @FXML private TableColumn<RepairJobDto, Long> latestRepairIdColumn;
    @FXML private TableColumn<RepairJobDto, String> latestRepairCustomerColumn;
    @FXML private TableColumn<RepairJobDto, String> latestRepairItemColumn;
    @FXML private TableColumn<RepairJobDto, RepairStatus> latestRepairStatusColumn;

    // Charts
    @FXML private BarChart<String, Number> weeklyPerformanceChart; // <<< CORRECTED: Renamed from weeklySalesChart
    @FXML private CategoryAxis dayAxisPerformance; // <<< CORRECTED: Renamed from dayAxis
    @FXML private NumberAxis amountAxisPerformance; // <<< CORRECTED: Renamed from salesAmountAxis

    @FXML private PieChart salesByCategoryPieChart;
    @FXML private BarChart<String, Number> topSellingProductsQtyChart;
    @FXML private CategoryAxis productNameQtyAxis;
    @FXML private NumberAxis quantitySoldAxis;
    @FXML private BarChart<String, Number> topSellingProductsRevChart;
    @FXML private CategoryAxis productNameRevAxis;
    @FXML private NumberAxis revenueGeneratedAxis;

    // Dynamic List Containers (if still used for other purposes, or can be removed if replaced by tables/charts)
    @FXML private VBox latestRepairsVBox; // This was likely replaced by latestRepairsTableView
    @FXML private VBox recentSalesVBox;
    @FXML private VBox lowStockAlertsVBox;

    // Refresh Button
    @FXML private Button refreshButton;

    // --- Services, Utilities, and Configuration ---
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final StageManager stageManager;
    private final ConfigurableApplicationContext springContext;

    @Value("${server.port:8080}")
    private String serverPort;
    private String getDashboardBaseUrl() { return "http://localhost:" + serverPort + "/api/v1/dashboard"; }

    // --- Formatters ---
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("fr", "DZ"));
    private final DateTimeFormatter shortDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

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
        // Axis labels
        if (dayAxisPerformance != null) dayAxisPerformance.setLabel("Day of Week");
        if (amountAxisPerformance != null) amountAxisPerformance.setLabel("Amount (" + currencyFormatter.getCurrency().getSymbol() + ")");
        if (salesByCategoryPieChart != null) salesByCategoryPieChart.setTitle("");
        if (productNameQtyAxis != null) productNameQtyAxis.setLabel("Product");
        if (quantitySoldAxis != null) quantitySoldAxis.setLabel("Units Sold");
        if (productNameRevAxis != null) productNameRevAxis.setLabel("Product");
        if (revenueGeneratedAxis != null) revenueGeneratedAxis.setLabel("Revenue ("+currencyFormatter.getCurrency().getSymbol()+")");

        setupLatestRepairsTable();
        loadDashboardMetrics();
    }

    @FXML
    private void loadDashboardMetrics() {
        logger.info("Loading dashboard metrics...");
        setMetricsLoadingState(true);
        if (weeklyPerformanceChart != null) weeklyPerformanceChart.getData().clear(); // <<< CORRECTED
        if (salesByCategoryPieChart != null) salesByCategoryPieChart.getData().clear();
        if (topSellingProductsQtyChart != null) topSellingProductsQtyChart.getData().clear();
        if (topSellingProductsRevChart != null) topSellingProductsRevChart.getData().clear();
        // If latestRepairsVBox is truly replaced by the TableView, this clear might not be needed.
        // However, it's safer to keep if some part of it might be used as a fallback.
        if (latestRepairsVBox != null) latestRepairsVBox.getChildren().clear();
        if (recentSalesVBox != null) recentSalesVBox.getChildren().clear();
        if (lowStockAlertsVBox != null) lowStockAlertsVBox.getChildren().clear();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getDashboardBaseUrl() + "/metrics"))
                .GET().build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAcceptAsync(response -> Platform.runLater(() -> {
                setMetricsLoadingState(false);
                if (response.statusCode() == 200) {
                    try {
                        Map<String, Object> metrics = objectMapper.readValue(response.body(), new TypeReference<>() {});
                        logger.debug("Dashboard metrics received: {}", metrics.keySet());

                        populateMetricCards(metrics);
                        populateFinancialSummaryCards(metrics);
                        populateWeeklyPerformanceChart(metrics); // <<< CORRECTED
                        populateSalesByCategoryPieChart(metrics);
                        populateTopSellingProductsQtyChart(metrics);
                        populateTopSellingProductsRevChart(metrics);

                        populateLatestRepairs(metrics);
                        populateRecentSales(metrics);
                        populateLowStockAlerts(metrics);

                    } catch (IOException e) {
                        logger.error("Failed to parse dashboard metrics: {}", e.getMessage(), e);
                        showErrorUIState("Error: Could not parse dashboard data from server.");
                    }
                } else {
                    logger.error("Failed to load dashboard metrics. Server Status: {}", response.statusCode());
                    showErrorUIState("Error: Could not load dashboard data (Server Status: " + response.statusCode() + ")");
                }
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    setMetricsLoadingState(false);
                    logger.error("Exception while fetching dashboard metrics: {}", ex.getMessage(), ex);
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    showErrorUIState("Error: Could not connect to server or an unexpected error occurred (" + cause.getMessage() + ")");
                });
                return null;
            });
    }

    private void populateMetricCards(Map<String, Object> metrics) {
        // Today's Sales
        Object todaysSalesObj = metrics.get("todaysSalesRevenue");
        if (todaysSalesCard != null && todaysSalesLabel != null) {
            todaysSalesCard.getStyleClass().removeAll("dashboard-card-accent-teal");
            if (todaysSalesObj instanceof Number) {
                double revenue = ((Number) todaysSalesObj).doubleValue();
                todaysSalesLabel.setText(currencyFormatter.format(revenue));
                if (revenue > 0) todaysSalesCard.getStyleClass().add("dashboard-card-accent-teal");
            } else {
                todaysSalesLabel.setText(todaysSalesObj != null ? todaysSalesObj.toString() : currencyFormatter.format(0));
            }
        }
        // Products in Stock
        if (totalProductsLabel != null) totalProductsLabel.setText(metrics.getOrDefault("totalProducts", "0").toString());

        // Pending Repairs
        Object pendingRepairsObj = metrics.get("pendingRepairsCount");
        if (pendingRepairsCard != null && pendingRepairsLabel != null) {
            pendingRepairsCard.getStyleClass().removeAll("dashboard-card-accent-amber", "dashboard-card-accent-ruby");
            String pendingText = (pendingRepairsObj != null) ? pendingRepairsObj.toString() : "0";
            pendingRepairsLabel.setText(pendingText);
            if (pendingRepairsObj instanceof Number) {
                int count = ((Number)pendingRepairsObj).intValue();
                if (count > 10) pendingRepairsCard.getStyleClass().add("dashboard-card-accent-ruby");
                else if (count > 0) pendingRepairsCard.getStyleClass().add("dashboard-card-accent-amber");
            }
        }

        // Low Stock Items
        Object lowStockObj = metrics.get("lowStockItemsCount");
        if (lowStockItemsCard != null && lowStockItemsLabel != null) {
            lowStockItemsCard.getStyleClass().removeAll("dashboard-card-accent-ruby");
            String lowStockText = (lowStockObj != null) ? lowStockObj.toString() : "0";
            lowStockItemsLabel.setText(lowStockText);
            if (lowStockObj instanceof Number && ((Number)lowStockObj).intValue() > 0) {
                lowStockItemsCard.getStyleClass().add("dashboard-card-accent-ruby");
            }
        }
    }

     private void populateFinancialSummaryCards(Map<String, Object> metrics) {
        if (salesLast7DaysLabel == null) return;

        salesLast7DaysLabel.setText("Sales: " + currencyFormatter.format(metrics.getOrDefault("salesLast7Days", BigDecimal.ZERO)));
        purchasesLast7DaysLabel.setText("Purchases: " + currencyFormatter.format(metrics.getOrDefault("purchasesLast7Days", BigDecimal.ZERO)));
        BigDecimal profit7 = (BigDecimal) metrics.getOrDefault("profitLast7Days", BigDecimal.ZERO);
        profitLast7DaysLabel.setText("Profit: " + currencyFormatter.format(profit7));
        profitLast7DaysLabel.setStyle(profit7.compareTo(BigDecimal.ZERO) >= 0 ? "-fx-text-fill: #0D9488;" : "-fx-text-fill: #D32F2F; -fx-font-weight: bold;"); // Green / Red

        salesLast30DaysLabel.setText("Sales: " + currencyFormatter.format(metrics.getOrDefault("salesLast30Days", BigDecimal.ZERO)));
        purchasesLast30DaysLabel.setText("Purchases: " + currencyFormatter.format(metrics.getOrDefault("purchasesLast30Days", BigDecimal.ZERO)));
        BigDecimal profit30 = (BigDecimal) metrics.getOrDefault("profitLast30Days", BigDecimal.ZERO);
        profitLast30DaysLabel.setText("Profit: " + currencyFormatter.format(profit30));
        profitLast30DaysLabel.setStyle(profit30.compareTo(BigDecimal.ZERO) >= 0 ? "-fx-text-fill: #0D9488;" : "-fx-text-fill: #D32F2F; -fx-font-weight: bold;"); // Green / Red
    }

    private void setupLatestRepairsTable() {
        if (latestRepairsTableView == null) {
            logger.warn("latestRepairsTableView is null. Cannot setup.");
            return;
        }
        latestRepairIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        latestRepairCustomerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        latestRepairItemColumn.setCellValueFactory(cellData -> {
            RepairJobDto job = cellData.getValue();
            String itemSummary = (job.getItemBrand() != null ? job.getItemBrand() : "") +
                                (job.getItemModel() != null ? " " + job.getItemModel() : "") +
                                (!job.getItemType().isEmpty() && (job.getItemBrand() != null || job.getItemModel() != null) ? " ("+job.getItemType()+")" : job.getItemType());
            return new SimpleStringProperty(itemSummary.trim());
        });

        latestRepairStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        latestRepairStatusColumn.setCellFactory(column -> new TableCell<RepairJobDto, RepairStatus>() {
            private final HBox graphic = new HBox(5);
            private final Circle statusDot = new Circle(5);
            private final Text statusText = new Text();
            {
                graphic.setAlignment(Pos.CENTER_LEFT);
                graphic.getChildren().addAll(statusDot, statusText);
                // Apply style class for CSS styling instead of inline style
                statusText.getStyleClass().add("table-cell-status-text"); // Example style class
            }

            @Override
            protected void updateItem(RepairStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    statusDot.setFill(getRepairStatusColor(status));
                    statusText.setText(status.getDisplayName());
                    setGraphic(graphic);
                }
            }
        });

        latestRepairsTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                RepairJobDto selectedJob = latestRepairsTableView.getSelectionModel().getSelectedItem();
                if (selectedJob != null) {
                    logger.debug("Double-clicked repair job ID: {}", selectedJob.getId());
                    RepairJobEditViewController.setJobToEdit(selectedJob);
                    springContext.getBean(MainViewController.class).loadCenterView("/fxml/RepairJobEditView.fxml");
                }
            }
        });
        latestRepairsTableView.setPlaceholder(new Label("No recent repair jobs to display."));
    }


    private void populateWeeklyPerformanceChart(Map<String, Object> metrics) {
        if (weeklyPerformanceChart != null && // <<< CORRECTED
            metrics.get("weeklySalesChartData") instanceof Map &&
            metrics.get("weeklyPurchasesChartData") instanceof Map &&
            metrics.get("weeklyProfitChartData") instanceof Map) {

            @SuppressWarnings("unchecked") Map<String, Number> salesData = (Map<String, Number>) metrics.get("weeklySalesChartData");
            @SuppressWarnings("unchecked") Map<String, Number> purchasesData = (Map<String, Number>) metrics.get("weeklyPurchasesChartData");
            @SuppressWarnings("unchecked") Map<String, Number> profitData = (Map<String, Number>) metrics.get("weeklyProfitChartData");

            XYChart.Series<String, Number> salesSeries = new XYChart.Series<>();
            salesSeries.setName("Sales");
            salesData.forEach((day, amount) -> salesSeries.getData().add(new XYChart.Data<>(day, amount.doubleValue())));

            XYChart.Series<String, Number> purchasesSeries = new XYChart.Series<>();
            purchasesSeries.setName("Purchases");
            purchasesData.forEach((day, amount) -> purchasesSeries.getData().add(new XYChart.Data<>(day, amount.doubleValue())));

            XYChart.Series<String, Number> profitSeries = new XYChart.Series<>();
            profitSeries.setName("Profit");
            profitData.forEach((day, amount) -> profitSeries.getData().add(new XYChart.Data<>(day, amount.doubleValue())));

            weeklyPerformanceChart.getData().setAll(salesSeries, purchasesSeries, profitSeries); // <<< CORRECTED
        } else if (weeklyPerformanceChart != null) { // <<< CORRECTED
            weeklyPerformanceChart.getData().clear(); // <<< CORRECTED
            logger.warn("Weekly performance chart data not found or in unexpected map format.");
        }
    }

    private void populateSalesByCategoryPieChart(Map<String, Object> metrics) {
        if (salesByCategoryPieChart != null && metrics.get("salesByCategoryChartData") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Number> categoryData = (Map<String, Number>) metrics.get("salesByCategoryChartData");
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            if (categoryData.isEmpty()) {
                logger.info("No sales by category data for pie chart.");
                pieChartData.add(new PieChart.Data("No Data", 1));
            } else {
                categoryData.forEach((categoryName, revenue) -> {
                    PieChart.Data slice = new PieChart.Data(categoryName, revenue.doubleValue());
                    pieChartData.add(slice);
                });
            }
            salesByCategoryPieChart.setData(pieChartData);
            salesByCategoryPieChart.setLabelsVisible(false);
            salesByCategoryPieChart.setLegendVisible(true);

            // Add tooltips
            final double totalRevenue = categoryData.values().stream().mapToDouble(Number::doubleValue).sum();
            if (totalRevenue > 0) { // Avoid division by zero if no revenue
                 pieChartData.forEach(slice -> Tooltip.install(slice.getNode(),
                    new Tooltip(String.format("%s: %s (%.2f%%)",
                            slice.getName(),
                            currencyFormatter.format(slice.getPieValue()),
                            (slice.getPieValue() / totalRevenue * 100)))
                ));
            } else {
                 pieChartData.forEach(slice -> Tooltip.install(slice.getNode(),
                    new Tooltip(String.format("%s: %s",
                            slice.getName(),
                            currencyFormatter.format(slice.getPieValue())))
                ));
            }

        } else if (salesByCategoryPieChart != null) {
            salesByCategoryPieChart.getData().clear();
            logger.warn("Sales by category data not found for pie chart.");
        }
    }

    private void populateTopSellingProductsQtyChart(Map<String, Object> metrics) {
       if (topSellingProductsQtyChart != null && metrics.get("topSellingProductsByQtyChartData") instanceof Map) {
           @SuppressWarnings("unchecked") Map<String, Number> productData = (Map<String, Number>) metrics.get("topSellingProductsByQtyChartData");
           XYChart.Series<String, Number> series = new XYChart.Series<>();
           if (productData.isEmpty()) {
               logger.info("No top selling products (qty) data.");
           } else {
               productData.forEach((name, qty) -> series.getData().add(new XYChart.Data<>(name, qty.intValue())));
           }
           topSellingProductsQtyChart.getData().setAll(series);
       } else if (topSellingProductsQtyChart != null) {
            topSellingProductsQtyChart.getData().clear();
            logger.warn("Top selling products (qty) data not found.");
       }
   }

    private void populateTopSellingProductsRevChart(Map<String, Object> metrics) {
       if (topSellingProductsRevChart != null && metrics.get("topSellingProductsByRevChartData") instanceof Map) {
           @SuppressWarnings("unchecked") Map<String, Number> productData = (Map<String, Number>) metrics.get("topSellingProductsByRevChartData");
           XYChart.Series<String, Number> series = new XYChart.Series<>();
           if (productData.isEmpty()) {
               logger.info("No top selling products (revenue) data.");
           } else {
               productData.forEach((name, rev) -> series.getData().add(new XYChart.Data<>(name, rev.doubleValue())));
           }
           topSellingProductsRevChart.getData().setAll(series);
       } else if (topSellingProductsRevChart != null) {
            topSellingProductsRevChart.getData().clear();
            logger.warn("Top selling products (revenue) data not found.");
       }
   }

    private void populateLatestRepairs(Map<String, Object> metrics) {
        if (latestRepairsTableView != null && metrics.get("latestRepairJobs") != null) {
            try {
                List<RepairJobDto> repairDtos = objectMapper.convertValue(
                    metrics.get("latestRepairJobs"),
                    new TypeReference<List<RepairJobDto>>() {}
                );
                latestRepairsTableView.setItems(FXCollections.observableArrayList(repairDtos));
                logger.info("Populated latest repairs table with {} items.", repairDtos.size());
            } catch (IllegalArgumentException e) {
                logger.error("Error converting latestRepairJobs metric to List<RepairJobDto>: {}", e.getMessage(), e);
                latestRepairsTableView.setPlaceholder(new Label("Error loading repair data."));
            }
        } else if (latestRepairsTableView != null) {
            latestRepairsTableView.getItems().clear();
            latestRepairsTableView.setPlaceholder(new Label("Repair data currently unavailable."));
            logger.warn("Latest repair jobs data not found in metrics or TableView is null.");
        }
    }

    private void populateRecentSales(Map<String, Object> metrics) {
        if (recentSalesVBox != null && metrics.get("recentSales") != null) {
            List<SaleDto> salesDtos = objectMapper.convertValue(metrics.get("recentSales"), new TypeReference<List<SaleDto>>() {});
            recentSalesVBox.getChildren().clear();
            if (salesDtos.isEmpty()) {
                recentSalesVBox.getChildren().add(new Label("No recent sales to display."));
            } else {
                salesDtos.forEach(sale -> recentSalesVBox.getChildren().add(createSaleEntryNode(sale)));
            }
        } else if (recentSalesVBox != null) {
            recentSalesVBox.getChildren().clear();
            recentSalesVBox.getChildren().add(new Label("Recent sales data unavailable."));
        }
    }

    private void populateLowStockAlerts(Map<String, Object> metrics) {
        if (lowStockAlertsVBox != null && metrics.get("lowStockAlerts") != null) {
            List<ProductDto> lowStockProducts = objectMapper.convertValue(metrics.get("lowStockAlerts"), new TypeReference<List<ProductDto>>() {});
            Integer threshold = (Integer) metrics.getOrDefault("lowStockThreshold", 5);
            lowStockAlertsVBox.getChildren().clear();
            if (lowStockProducts.isEmpty()) {
                lowStockAlertsVBox.getChildren().add(new Label("All products above stock threshold (" + threshold + ")."));
            } else {
                lowStockProducts.forEach(prod -> lowStockAlertsVBox.getChildren().add(createLowStockEntryNode(prod, threshold)));
            }
        } else if (lowStockAlertsVBox != null) {
            lowStockAlertsVBox.getChildren().clear();
            lowStockAlertsVBox.getChildren().add(new Label("Low stock data unavailable."));
        }
    }

    private void setMetricsLoadingState(boolean isLoading) {
        if (isLoading) {
            todaysSalesLabel.setText("Loading...");
            totalProductsLabel.setText("Loading...");
            pendingRepairsLabel.setText("Loading...");
            lowStockItemsLabel.setText("Loading...");
            if(salesLast7DaysLabel != null) salesLast7DaysLabel.setText("Sales: Loading...");
            if(purchasesLast7DaysLabel != null) purchasesLast7DaysLabel.setText("Purchases: Loading...");
            if(profitLast7DaysLabel != null) profitLast7DaysLabel.setText("Profit: Loading...");
            if(salesLast30DaysLabel != null) salesLast30DaysLabel.setText("Sales: Loading...");
            if(purchasesLast30DaysLabel != null) purchasesLast30DaysLabel.setText("Purchases: Loading...");
            if(profitLast30DaysLabel != null) profitLast30DaysLabel.setText("Profit: Loading...");
        }
        if (todaysSalesCard != null) todaysSalesCard.getStyleClass().removeAll("dashboard-card-accent-teal");
        if (pendingRepairsCard != null) pendingRepairsCard.getStyleClass().removeAll("dashboard-card-accent-amber", "dashboard-card-accent-ruby");
        if (lowStockItemsCard != null) lowStockItemsCard.getStyleClass().removeAll("dashboard-card-accent-ruby");

        if (refreshButton != null) refreshButton.setDisable(isLoading);
    }

    private void showErrorUIState(String message) {
        todaysSalesLabel.setText("Error");
        totalProductsLabel.setText("Error");
        pendingRepairsLabel.setText("Error");
        lowStockItemsLabel.setText("Error");
        if(salesLast7DaysLabel != null) salesLast7DaysLabel.setText("Sales: Error");
        if(purchasesLast7DaysLabel != null) purchasesLast7DaysLabel.setText("Purchases: Error");
        if(profitLast7DaysLabel != null) profitLast7DaysLabel.setText("Profit: Error");
        if(salesLast30DaysLabel != null) salesLast30DaysLabel.setText("Sales: Error");
        if(purchasesLast30DaysLabel != null) purchasesLast30DaysLabel.setText("Purchases: Error");
        if(profitLast30DaysLabel != null) profitLast30DaysLabel.setText("Profit: Error");

        if (weeklyPerformanceChart != null) weeklyPerformanceChart.getData().clear(); // <<< CORRECTED
        if (salesByCategoryPieChart != null) salesByCategoryPieChart.getData().clear();
        if (topSellingProductsQtyChart != null) topSellingProductsQtyChart.getData().clear();
        if (topSellingProductsRevChart != null) topSellingProductsRevChart.getData().clear();

        if (latestRepairsVBox != null) latestRepairsVBox.getChildren().setAll(new Label("Error loading data."));
        if (recentSalesVBox != null) recentSalesVBox.getChildren().setAll(new Label("Error loading data."));
        if (lowStockAlertsVBox != null) lowStockAlertsVBox.getChildren().setAll(new Label("Error loading data."));
        stageManager.showErrorAlert("Dashboard Error", message);
    }

    private Node createSaleEntryNode(SaleDto sale) {
        HBox entry = new HBox(10);
        entry.setPadding(new Insets(4,0,4,0));
        entry.getStyleClass().add("dashboard-list-entry-simple");
        entry.setCursor(Cursor.HAND);
        entry.setOnMouseClicked(event -> {
            stageManager.showInfoAlert("Sale Details", "Viewing details for Sale ID: " + sale.getId() + " (Not Implemented).");
        });

        Label idLabel = new Label("S-" + sale.getId());
        idLabel.setMinWidth(60);
        Label customerLabel = new Label(sale.getCustomerName() != null && !sale.getCustomerName().isEmpty() ? sale.getCustomerName() : "Walk-in Customer");
        customerLabel.setPrefWidth(150); customerLabel.setMaxWidth(150); customerLabel.setWrapText(false); customerLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        Label amountLabel = new Label(currencyFormatter.format(sale.getTotalAmount()));
        amountLabel.setStyle("-fx-font-weight: bold;");
        amountLabel.setMinWidth(80); amountLabel.setAlignment(Pos.CENTER_RIGHT);
        Label dateLabel = new Label(sale.getSaleDate() != null ? sale.getSaleDate().format(shortDateFormatter) : "N/A");
        dateLabel.setMinWidth(100);
        entry.getChildren().addAll(idLabel, customerLabel, amountLabel, dateLabel);
        return entry;
    }

    private Node createLowStockEntryNode(ProductDto product, int threshold) {
        HBox entry = new HBox(10);
        entry.setPadding(new Insets(4,0,4,0));
        entry.getStyleClass().add("dashboard-list-entry-simple");

        Label productName = new Label(product.getName());
        productName.setPrefWidth(180); productName.setMaxWidth(180); productName.setWrapText(false); productName.setTextOverrun(OverrunStyle.ELLIPSIS);

        Label stockLabel = new Label("Stock: " + product.getQuantityInStock() + " (Thr: " + threshold + ")");
        stockLabel.setMinWidth(100);
        if (product.getQuantityInStock() <= threshold) {
            stockLabel.setStyle("-fx-text-fill: #E11D48; -fx-font-weight: bold;"); // Ruby error
        } else if (product.getQuantityInStock() <= threshold + 5) {
            stockLabel.setStyle("-fx-text-fill: #F59E0B;"); // Amber warning
        }

        Button orderButton = new Button("Order");
        orderButton.getStyleClass().add("button-secondary");
        orderButton.setPadding(new Insets(2,8,2,8));
        orderButton.setOnAction(e -> {
            logger.info("Order button clicked for product: {}", product.getName());
            stageManager.showInfoAlert("Order Stock", "Ordering " + product.getName() + " (Action not fully implemented).");
        });
        entry.getChildren().addAll(productName, stockLabel, orderButton);
        return entry;
    }

     private Color getRepairStatusColor(RepairStatus status) {
        if (status == null) return Color.web("#CCCCCC");
        switch (status) {
            case PENDING_ASSESSMENT:
            case WAITING_FOR_PARTS:
            case ASSESSED_WAITING_APPROVAL:
                return Color.web("#F59E0B"); // Amber
            case IN_PROGRESS:
                return Color.web("#2563EB"); // Blue
            case READY_FOR_PICKUP:
                return Color.web("#0D9488"); // Teal
            case COMPLETED_PAID:
            case COMPLETED_UNPAID:
                return Color.web("#10B981"); // Green
            case CANCELLED_BY_CUSTOMER:
            case CANCELLED_BY_STORE:
            case UNREPAIRABLE:
                return Color.web("#E11D48"); // Ruby
            default:
                return Color.LIGHTGRAY;
        }
    }

    // --- Navigation Handlers ---
    @FXML private void navigateToViewRepairs() {
        logger.debug("Navigating to View Repairs from dashboard hyperlink.");
        springContext.getBean(MainViewController.class).handleViewRepairs();
    }
    @FXML private void navigateToViewSales() {
        logger.debug("Navigating to View Sales from dashboard hyperlink.");
        springContext.getBean(MainViewController.class).handleViewSales();
    }
    @FXML private void navigateToProducts() {
        logger.debug("Navigating to Products from dashboard hyperlink.");
        springContext.getBean(MainViewController.class).handleManageProducts();
    }
}