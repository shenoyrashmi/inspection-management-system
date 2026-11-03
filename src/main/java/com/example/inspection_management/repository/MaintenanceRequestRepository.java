package com.example.inspection_management.repository;

import com.example.inspection_management.entity.MaintenanceRequest;
import com.example.inspection_management.entity.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceRequestRepository
        extends JpaRepository<MaintenanceRequest, Long> {

    boolean existsByInspectionId(Long inspectionId);

    long countByStatus(MaintenanceStatus status);
}
