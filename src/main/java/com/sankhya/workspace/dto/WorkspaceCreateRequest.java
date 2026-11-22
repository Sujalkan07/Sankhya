package com.sankhya.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WorkspaceCreateRequest {

    @NotBlank(message = "Supermarket name is required")
    @Size(min = 3, max = 100, message = "Supermarket name must be between 3 and 100 characters")
    private String name;

    private String storeFormat = "Supermarket";

    @Size(max = 100, message = "City name must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State name must not exceed 100 characters")
    private String state;

    @Size(max = 100, message = "Country name must not exceed 100 characters")
    private String country = "India";

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    public WorkspaceCreateRequest() {
    }

    public WorkspaceCreateRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public WorkspaceCreateRequest(String name, String storeFormat, String city, String state, String country, String description) {
        this.name = name;
        this.storeFormat = storeFormat != null && !storeFormat.isBlank() ? storeFormat : "Supermarket";
        this.city = city;
        this.state = state;
        this.country = country != null && !country.isBlank() ? country : "India";
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStoreFormat() {
        return storeFormat != null ? storeFormat : "Supermarket";
    }

    public void setStoreFormat(String storeFormat) {
        this.storeFormat = storeFormat;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country != null ? country : "India";
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
