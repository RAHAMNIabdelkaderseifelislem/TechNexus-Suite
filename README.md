# TechNexus Suite - Computer Store Management System

TechNexus Suite is a comprehensive desktop application designed to manage the operations of a computer retail and repair store. It features a Spring Boot backend providing a RESTful API and a JavaFX frontend for the user interface.

## Table of Contents

- [Features](#features)
- [Technologies Used](#technologies-used)
- [Prerequisites](#prerequisites)
- [Setup and Installation](#setup-and-installation)
  - [Backend Configuration](#backend-configuration)
  - [Database Setup](#database-setup)
- [Running the Application](#running-the-application)
  - [Using Maven](#using-maven)
- [Default Users](#default-users)
- [Project Structure](#project-structure)
- [Key Functionalities](#key-functionalities)
  - [Dashboard](#dashboard)
  - [Product Management](#product-management)
  - [Sales Management](#sales-management)
  - [Purchase Management](#purchase-management)
  - [Repair Job Management](#repair-job-management)
  - [Reporting](#reporting)
  - [Admin Functions](#admin-functions)
- [Future Enhancements (TODO)](#future-enhancements-todo)
- [Contributing](#contributing)
- [License](#license)

## Features

*   **Dashboard:** Overview of key store metrics (sales, stock, pending repairs, financial summaries, charts).
*   **Product Management:** Add, edit, delete, and view product inventory. Includes categories, pricing, stock levels.
*   **Sales Processing:** Create new sales transactions, manage items, and calculate totals.
*   **Purchase Management:** Record new stock purchases from suppliers.
*   **Repair Job Tracking:** Log new repair jobs, manage customer and item details, track status, assign technicians, and record costs.
*   **User Authentication & Authorization:** Secure login with role-based access control (Admin, Manager, Staff, User).
*   **Reporting:**
    *   Detailed Sales Report (by date range)
    *   Current Stock Report (with valuation)
    *   (Future reports: Profit/Loss, Sales by Product/Category, etc.)
*   **Database Management:** Initial data seeding and database backup functionality for administrators.
*   **Responsive UI:** Designed with JavaFX for a desktop environment.

## Technologies Used

*   **Backend:**
    *   Java 11 (or your project's Java version)
    *   Spring Boot 2.7.x (or your project's Spring Boot version)
    *   Spring Data JPA
    *   Spring Security
    *   Hibernate
    *   MySQL (or your configured database)
    *   Maven (for build and dependency management)
    *   SLF4J with Logback (for logging)
    *   Apache Commons CSV (for CSV export/import)
*   **Frontend:**
    *   JavaFX 17.0.x (or your project's JavaFX version)
    *   FXML for UI layout
    *   CSS for styling
*   **Development Tools:**
    *   IntelliJ IDEA / Eclipse / VSCode (or your preferred IDE)
    *   Git for version control
    *   phpMyAdmin / MySQL Workbench (for database management)

## Prerequisites

*   JDK 11 (or the Java version specified in your `pom.xml`)
*   Apache Maven 3.6+
*   MySQL Server (version 8.x recommended)
*   An IDE that supports Maven and Java.
*   `mysqldump` utility accessible in your system's PATH (for the database backup feature).

## Setup and Installation

1.  **Clone the Repository:**
    ```bash
    git clone https://your-repository-url/TechNexusSuite.git
    cd TechNexusSuite
    ```

2.  **Backend Configuration:**
    *   Modify `src/main/resources/application.properties` to configure your database connection:
        ```properties
        spring.datasource.url=jdbc:mysql://localhost:3306/computer_store_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
        spring.datasource.username=your_mysql_user
        spring.datasource.password=your_mysql_password
        ```
    *   Replace `your_mysql_user` and `your_mysql_password` with your MySQL credentials.

3.  **Database Setup:**
    *   Ensure your MySQL server is running.
    *   The application is configured to create the database (`computer_store_db`) if it doesn't exist (`createDatabaseIfNotExist=true`).
    *   The schema will be automatically created/updated by Hibernate based on the entities (`spring.jpa.hibernate.ddl-auto=update`).
    *   On the first run (or after dropping the database), the `DataInitializer` will populate the database with default users and test data.

## Running the Application

### Using Maven

1.  **Build the project (compiles, runs tests, and packages):**
    ```bash
    mvn clean install
    ```
2.  **Run the application (starts Spring Boot backend and then launches JavaFX frontend):**
    ```bash
    mvn javafx:run
    ```
    Alternatively, after a successful `mvn clean install`, you can run the packaged JAR (ensure your `pom.xml` is configured for an executable Spring Boot JAR with JavaFX dependencies properly handled):
    ```bash
    java -jar target/computer-store-management-1.0-SNAPSHOT.jar
    ```
    *(Note: Direct execution of the fat JAR might require specific `pom.xml` configuration for JavaFX if you are using Java 11+ with modules, or if the JavaFX SDK is not globally available. The `mvn javafx:run` command is generally more reliable for development.)*

## Default Users

The `DataInitializer` creates the following default users if they don't exist:

*   **Admin:**
    *   Username: `admin`
    *   Password: `adminpass`
    *   Roles: ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF, ROLE_USER
*   **Regular User:**
    *   Username: `user`
    *   Password: `userpass`
    *   Roles: ROLE_USER, ROLE_STAFF

## Project Structure

The project follows a standard Maven layout:
```
TechNexusSuite/
├── pom.xml
└── src/
├── main/
│ ├── java/com/yourstore/app/
│ │ ├── backend/ # Spring Boot backend logic
│ │ │ ├── config/
│ │ │ ├── controller/
│ │ │ ├── exception/
│ │ │ ├── mapper/
│ │ │ ├── model/
│ │ │ │ ├── dto/
│ │ │ │ ├── entity/
│ │ │ │ └── enums/
│ │ │ ├── repository/
│ │ │ ├── security/
│ │ │ └── service/
│ │ ├── frontend/ # JavaFX frontend logic
│ │ │ ├── config/
│ │ │ ├── controller/
│ │ │ ├── service/ # Client services for API communication
│ │ │ └── util/
│ │ ├── launcher/ # Main application launcher
│ │ └── BackendApplication.java # Spring Boot main
│ │ └── FrontendApplication.java # JavaFX main
│ └── resources/
│ ├── fxml/ # FXML UI layout files
│ ├── css/ # CSS stylesheets
│ ├── icons/ # Application icons
│ ├── fonts/ # Font files
│ └── application.properties # Spring Boot configuration
└── test/
└── java/ # Unit and integration tests
```


## Key Functionalities

(Briefly describe each module's purpose as listed above in Features)

### Dashboard
Provides an at-a-glance view of crucial store metrics including daily sales, total products, pending repairs, low stock items, financial summaries (sales, purchases, profit for the last 7/30 days), and various charts for performance analysis, sales by category, and top-selling products.

### Product Management
Allows users to add new products, edit existing product details (name, category, description, supplier, purchase price, selling price, quantity), and delete products from the inventory. Includes search and filtering capabilities.

### Sales Management
Facilitates the creation of new sales transactions. Users can add products to a sale, specify quantities, and the system calculates the total amount. Sales records can be viewed with filtering options.

### Purchase Management
Enables recording of new stock purchases from suppliers, including details like supplier name, invoice number, items purchased, quantities, and cost prices.

### Repair Job Management
Tracks customer repair jobs from logging the issue to completion. Includes managing customer details, item information, reported problems, technician notes, repair status, assignments, and estimated/actual costs.

### Reporting
Currently includes:
*   **Detailed Sales Report:** View sales transactions within a specified date range, with options to export.
*   **Current Stock Report:** Displays all products with current stock levels, cost price, selling price, stock valuation at cost, and potential revenue. Includes filtering and export options.
(Future reports will cover profitability, purchases by supplier/product, etc.)

### Admin Functions
*   **Database Backup:** Allows administrators to create a backup of the application database.
*   **User Management (Implicit):** Roles defined in `UserRole` and controlled via `SecurityConfig` and `DataInitializer`. (A dedicated UI for user management could be a future enhancement).

## Future Enhancements (TODO)

*   More comprehensive reporting modules (e.g., detailed profit/loss, purchases by supplier).
*   Advanced inventory management (e.g., stock alerts, reorder points, stock adjustments).
*   Customer management module.
*   Supplier management module.
*   User interface for managing application users and roles by an Admin.
*   Printing functionality for invoices, receipts, and reports.
*   Integration with barcode scanners.
*   More sophisticated COGS tracking for precise profit calculation.
*   Internationalization (i18n) for multi-language support.
*   Unit and Integration tests for backend services and controllers.
*   Frontend tests using TestFX.

## Contributing

Contributions are welcome! If you'd like to contribute, please follow these steps:
1. Fork the repository.
2. Create a new branch (`git checkout -b feature/your-feature-name`).
3. Make your changes.
4. Commit your changes (`git commit -m 'Add some feature'`).
5. Push to the branch (`git push origin feature/your-feature-name`).
6. Open a Pull Request.

Please make sure to update tests as appropriate.

