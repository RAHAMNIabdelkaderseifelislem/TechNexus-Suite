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
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button; // Added for refreshButton
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
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

    @FXML private TableView<RepairJobDto> latestRepairsTableView;
    @FXML private TableColumn<RepairJobDto, Long> latestRepairIdColumn;
    @FXML private TableColumn<RepairJobDto, String> latestRepairCustomerColumn;
    @FXML private TableColumn<RepairJobDto, String> latestRepairItemColumn;
    @FXML private TableColumn<RepairJobDto, RepairStatus> latestRepairStatusColumn; // Store RepairStatus for logic

    // Chart
    @FXML private BarChart<String, Number> weeklySalesChart;
    @FXML private CategoryAxis dayAxis; // X-axis for weekly sales chart
    @FXML private NumberAxis salesAmountAxis; // Y-axis for weekly sales chart

    // Dynamic List Containers
    @FXML private VBox latestRepairsVBox;
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
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("fr", "DZ")); // Algerian Dinar
    private final DateTimeFormatter shortDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
    // private final DateTimeFormatter shortTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT); // Not used yet

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
        if (dayAxis != null) dayAxis.setLabel("Day of Week"); // Set axis labels if not set in FXML
        if (salesAmountAxis != null) salesAmountAxis.setLabel("Sales Amount (" + currencyFormatter.getCurrency().getSymbol() + ")");
        
        setupLatestRepairsTable(); // New setup method
        loadDashboardMetrics();
    }

    @FXML
    private void loadDashboardMetrics() {
        logger.info("Loading dashboard metrics...");
        setMetricsLoadingState(true); // Show loading state
        if (weeklySalesChart != null) weeklySalesChart.getData().clear();
        if (latestRepairsVBox != null) latestRepairsVBox.getChildren().clear();
        if (recentSalesVBox != null) recentSalesVBox.getChildren().clear();
        if (lowStockAlertsVBox != null) lowStockAlertsVBox.getChildren().clear();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getDashboardBaseUrl() + "/metrics"))
                .GET().build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAcceptAsync(response -> Platform.runLater(() -> { // Ensure UI updates are on JavaFX Application Thread
                setMetricsLoadingState(false); // Hide loading state once response is processed
                if (response.statusCode() == 200) {
                    try {
                        Map<String, Object> metrics = objectMapper.readValue(response.body(), new TypeReference<>() {});
                        logger.debug("Dashboard metrics received: {}", metrics);

                        populateMetricCards(metrics);
                        populateWeeklySalesChart(metrics);
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
                if (count > 10) pendingRepairsCard.getStyleClass().add("dashboard-card-accent-ruby"); // Example thresholds
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
            private final HBox graphic = new HBox(5); // Spacing between dot and text
            private final Circle statusDot = new Circle(5);
            private final Text statusText = new Text();
            {
                graphic.setAlignment(Pos.CENTER_LEFT);
                graphic.getChildren().addAll(statusDot, statusText);
            }

            @Override
            protected void updateItem(RepairStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    statusDot.setFill(getRepairStatusColor(status)); // Uses existing helper
                    statusText.setText(status.getDisplayName());
                    statusText.setStyle("-fx-fill: #1f2937;"); // Ensure text color from CSS
                    setGraphic(graphic);
                }
            }
        });

        // Double-click to open repair job
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

    private void populateWeeklySalesChart(Map<String, Object> metrics) {
        if (weeklySalesChart != null && metrics.get("weeklySalesChartData") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Number> weeklyData = (Map<String, Number>) metrics.get("weeklySalesChartData");
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Sales Amount");
            
            if (weeklyData.isEmpty()) {
                logger.info("No weekly sales data for chart.");
                // Optionally add a placeholder data point or text to chart
            } else {
                // Assuming service returns data ordered by day or keys are sortable day names
                weeklyData.forEach((day, amount) -> {
                    series.getData().add(new XYChart.Data<>(day, amount.doubleValue())); // Ensure it's double for NumberAxis
                });
            }
            weeklySalesChart.getData().setAll(series);
        } else if (weeklySalesChart != null) {
            weeklySalesChart.getData().clear();
            logger.warn("Weekly sales chart data not found or in unexpected map format.");
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
            } catch (IllegalArgumentException e) { // Catch TypeReference conversion errors
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
            Integer threshold = (Integer) metrics.getOrDefault("lowStockThreshold", 5); // Get threshold from backend
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
        if (weeklySalesChart != null) weeklySalesChart.getData().clear();
        if (latestRepairsVBox != null) latestRepairsVBox.getChildren().setAll(new Label("Error loading data."));
        if (recentSalesVBox != null) recentSalesVBox.getChildren().setAll(new Label("Error loading data."));
        if (lowStockAlertsVBox != null) lowStockAlertsVBox.getChildren().setAll(new Label("Error loading data."));
        stageManager.showErrorAlert("Dashboard Error", message);
    }

    // Helper methods to create UI nodes for lists
    private Node createRepairJobEntryNode(RepairJobDto job) {
        VBox entry = new VBox(2); // Reduced spacing
        entry.getStyleClass().add("dashboard-list-entry");
        entry.setCursor(Cursor.HAND);
        entry.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                logger.debug("Navigating to edit repair job ID: {}", job.getId());
                RepairJobEditViewController.setJobToEdit(job);
                springContext.getBean(MainViewController.class).loadCenterView("/fxml/RepairJobEditView.fxml");
            }
        });

        HBox titleLine = new HBox(5);
        titleLine.setAlignment(Pos.CENTER_LEFT);
        Circle statusDot = new Circle(6, getRepairStatusColor(job.getStatus()));
        Text customerText = new Text(job.getCustomerName() != null ? job.getCustomerName() : "N/A");
        customerText.setStyle("-fx-font-weight: bold; -fx-fill: #1f2937;");
        Label idLabel = new Label(" (R-" + job.getId() + ")");
        idLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6B7280;");
        titleLine.getChildren().addAll(statusDot, customerText, idLabel);

        String itemDesc = (job.getItemBrand() != null ? job.getItemBrand() : "") + " " + (job.getItemModel() != null ? job.getItemModel() : job.getItemType());
        Text itemIssueText = new Text(itemDesc.trim() + ": " + (job.getReportedIssue().length() > 50 ? job.getReportedIssue().substring(0, 47) + "..." : job.getReportedIssue()));
        itemIssueText.setWrappingWidth(200); // Max width for item/issue line
        itemIssueText.setStyle("-fx-font-size: 12px; -fx-fill: #6B7280;");
        
        entry.getChildren().addAll(titleLine, itemIssueText);
        return entry;
    }

    private Color getRepairStatusColor(RepairStatus status) {
        if (status == null) return Color.web("#CCCCCC"); // Neutral gray for null
        switch (status) {
            case PENDING_ASSESSMENT:
            case WAITING_FOR_PARTS:
            case ASSESSED_WAITING_APPROVAL: // Added this based on RepairStatus enum
                return Color.web("#F59E0B"); // -fx-amber-warning

            case IN_PROGRESS:
                return Color.web("#2563EB"); // -fx-primary-blue

            case READY_FOR_PICKUP:
                return Color.web("#0D9488"); // #0d9488
                
            case COMPLETED_PAID:
            case COMPLETED_UNPAID:
                return Color.GREEN; // Or use #0d9488 if preferred

            case CANCELLED_BY_CUSTOMER:
            case CANCELLED_BY_STORE:
            case UNREPAIRABLE:
                return Color.web("#E11D48"); // #e11d48
                
            default:
                return Color.LIGHTGRAY; // Fallback for any other status
        }
    }

    private Node createSaleEntryNode(SaleDto sale) {
        HBox entry = new HBox(10);
        entry.setPadding(new Insets(4,0,4,0));
        entry.getStyleClass().add("dashboard-list-entry-simple");
        entry.setCursor(Cursor.HAND);
        entry.setOnMouseClicked(event -> {
            // TODO: Navigate to Sale Detail View if one exists
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
            stockLabel.setStyle("-fx-text-fill: #e11d48; -fx-font-weight: bold;");
        } else if (product.getQuantityInStock() <= threshold + 5) { // Example: nearing low stock
            stockLabel.setStyle("-fx-text-fill: -fx-amber-warning;");
        }
        
        Button orderButton = new Button("Order");
        orderButton.getStyleClass().add("button-secondary");
        orderButton.setPadding(new Insets(2,8,2,8)); // Smaller padding
        orderButton.setOnAction(e -> {
            logger.info("Order button clicked for product: {}", product.getName());
            stageManager.showInfoAlert("Order Stock", "Ordering " + product.getName() + " (Action not fully implemented).");
            // Potentially navigate to New Purchase view with this product pre-selected
        });
        entry.getChildren().addAll(productName, stockLabel, orderButton);
        return entry;
    }

    // --- Navigation Handlers for Hyperlinks in Dashboard ---
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