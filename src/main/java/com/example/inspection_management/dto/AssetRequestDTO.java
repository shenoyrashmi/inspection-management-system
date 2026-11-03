package com.example.inspection_management.dto;

import jakarta.validation.constraints.NotBlank;

public class AssetRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Serial number is required")
    private String serialNumber;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Status is required")
    private String status;

    public AssetRequestDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
