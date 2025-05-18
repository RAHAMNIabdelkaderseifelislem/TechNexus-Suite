package com.yourstore.app.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class DatabaseService {

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${spring.datasource.url}")
    private String dbUrl; // e.g., jdbc:mysql://localhost:3306/computer_store_db

    // Path to mysqldump.exe. For simplicity, assume it's in PATH.
    // For a real app, make this configurable or bundled.
    private String mysqldumpPath = "mysqldump"; // Or full path like "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe"


    private String getDbNameFromUrl(String url) {
        try {
            // jdbc:mysql://localhost:3306/computer_store_db?otherParams
            String dbNameWithParams = url.substring(url.lastIndexOf("/") + 1);
            if (dbNameWithParams.contains("?")) {
                return dbNameWithParams.substring(0, dbNameWithParams.indexOf("?"));
            }
            return dbNameWithParams;
        } catch (Exception e) {
            System.err.println("Could not parse database name from URL: " + url);
            throw new IllegalArgumentException("Invalid database URL format for parsing DB name.", e);
        }
    }


    public String backupDatabase(String backupDirectory) throws IOException, InterruptedException {
        String dbName = getDbNameFromUrl(this.dbUrl);
        if (dbName == null || dbName.trim().isEmpty()) {
            throw new IllegalStateException("Database name could not be determined from datasource URL.");
        }

        // Ensure backup directory exists
        Files.createDirectories(Paths.get(backupDirectory));

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String backupFileName = "backup_" + dbName + "_" + timestamp + ".sql";
        String backupFilePath = Paths.get(backupDirectory, backupFileName).toString();

        // Construct the mysqldump command
        // Add --column-statistics=0 if you encounter issues with newer mysqldump versions and older servers.
        // Add --routines --triggers --events if you use them.
        ProcessBuilder processBuilder = new ProcessBuilder(
                mysqldumpPath,
                "--user=" + dbUsername,
                "--password=" + dbPassword, // Be cautious with password on command line in some environments
                "--host=" + getDbHostFromUrl(this.dbUrl), // Assumes localhost if not specified
                "--port=" + getDbPortFromUrl(this.dbUrl),   // Assumes 3306 if not specified
                dbName,
                "--result-file=" + backupFilePath
        );

        // For more security with password, consider using a MySQL option file (.my.cnf)
        // or environment variables if mysqldump supports them securely.

        System.out.println("Executing backup command: " + String.join(" ", processBuilder.command()));

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode == 0) {
            System.out.println("Database backup successful: " + backupFilePath);
            return backupFilePath;
        } else {
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }
            String commonError = errorOutput.toString();
            System.err.println("Database backup failed. Exit code: " + exitCode);
            System.err.println("Error output:\n" + commonError);

            String userMessage = "Database backup failed with exit code " + exitCode + ".";
            if (commonError.toLowerCase().contains("not recognized") || commonError.toLowerCase().contains("not found") || commonError.toLowerCase().contains("no such file")) {
                userMessage += " This might indicate 'mysqldump' is not installed or not in the system's PATH. Please check its configuration.";
            } else if (!commonError.trim().isEmpty()){
                userMessage += " Details: " + commonError.substring(0, Math.min(commonError.length(), 150)) + "..."; // Show snippet
            }
            throw new IOException(userMessage); // Throw with potentially more helpful message
        }
    }

    private String getDbHostFromUrl(String url) {
        // jdbc:mysql://localhost:3306/dbname
        try {
            String temp = url.substring(url.indexOf("//") + 2); // localhost:3306/dbname
            temp = temp.substring(0, temp.indexOf(":")); // localhost
            return temp;
        } catch (Exception e) {
            System.err.println("Could not parse host from URL: " + url + ", defaulting to localhost");
            return "localhost"; // Default if parsing fails
        }
    }

    private String getDbPortFromUrl(String url) {
        // jdbc:mysql://localhost:3306/dbname
        try {
            String temp = url.substring(url.indexOf("//") + 2); // localhost:3306/dbname
            temp = temp.substring(temp.indexOf(":") + 1);    // 3306/dbname
            temp = temp.substring(0, temp.indexOf("/"));     // 3306
            return temp;
        } catch (Exception e) {
            System.err.println("Could not parse port from URL: " + url + ", defaulting to 3306");
            return "3306"; // Default MySQL port
        }
    }
}