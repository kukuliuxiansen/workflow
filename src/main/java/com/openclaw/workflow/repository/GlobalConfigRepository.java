package com.openclaw.workflow.repository;

import com.openclaw.workflow.entity.GlobalConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GlobalConfigRepository extends JpaRepository<GlobalConfig, String> {

    Optional<GlobalConfig> findByKey(String key);
}