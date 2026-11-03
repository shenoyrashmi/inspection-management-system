package com.example.inspection_management.dto;

public class AssetResponseDTO {

    private Long id;
    private String name;
    private String serialNumber;
    private String location;
    private String status;

    public AssetResponseDTO() {
    }

    public AssetResponseDTO(Long id, String name, String serialNumber,
                            String location, String status) {
        this.id = id;
        this.name = name;
        this.serialNumber = serialNumber;
        this.location = location;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getLocation() {
        return location;
    }

    public String getStatus() {
        return status;
    }
}
