package com.example.inspection_management.repository;

import com.example.inspection_management.entity.Inspection;
import com.example.inspection_management.entity.InspectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {

    List<Inspection> findByInspectorEmail(String email);

    long countByStatus(InspectionStatus status);

    List<Inspection> findByAssetId(Long assetId);
}
