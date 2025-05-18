package com.yourstore.app.backend.controller;

import com.yourstore.app.backend.service.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final DatabaseService databaseService;

    @Autowired
    public AdminController(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @PostMapping("/db/backup")
    public ResponseEntity<?> backupDatabase(@RequestBody(required = false) Map<String, String> payload) {
        // Expecting a payload like {"backupDirectory": "C:/path/to/backups"}
        // If payload is null or backupDirectory not provided, use a default.
        String defaultBackupDir = System.getProperty("user.home") + "/computer_store_backups";
        String backupDirectory = (payload != null && payload.containsKey("backupDirectory")) ?
                                  payload.get("backupDirectory") : defaultBackupDir;

        try {
            String backupFilePath = databaseService.backupDatabase(backupDirectory);
            return ResponseEntity.ok(Map.of("message", "Database backup successful!", "path", backupFilePath));
        } catch (Exception e) {
            e.printStackTrace(); // Log full error on server
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", "Database backup failed: " + e.getMessage()));
        }
    }
}