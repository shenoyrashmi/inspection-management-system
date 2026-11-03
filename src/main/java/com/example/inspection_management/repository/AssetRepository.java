package com.example.inspection_management.repository;

import com.example.inspection_management.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, Long> {
}
