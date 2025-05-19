package com.yourstore.app.backend.model.dto; // Or frontend.model.dto

public class UserBasicDto {
    private Long id;
    private String username;

    public UserBasicDto() {}
    public UserBasicDto(Long id, String username) {
        this.id = id;
        this.username = username;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    @Override
    public String toString() { // Important for ComboBox display
        return username;
    }
}