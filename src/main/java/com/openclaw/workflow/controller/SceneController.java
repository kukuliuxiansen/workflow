package com.openclaw.workflow.controller;

import com.openclaw.workflow.entity.SmartDecomposeScene;
import com.openclaw.workflow.repository.SmartDecomposeSceneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SmartDecompose 场景管理 API
 */
@RestController
@RequestMapping("/api/smart-decompose/scenes")
public class SceneController {

    @Autowired
    private SmartDecomposeSceneRepository sceneRepository;

    /**
     * 获取所有场景
     */
    @GetMapping
    public List<SmartDecomposeScene> list() {
        return sceneRepository.findAllByOrderByNameAsc();
    }

    /**
     * 获取单个场景
     */
    @GetMapping("/{id}")
    public ResponseEntity<SmartDecomposeScene> get(@PathVariable String id) {
        return sceneRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取默认场景
     */
    @GetMapping("/default")
    public ResponseEntity<SmartDecomposeScene> getDefault() {
        return sceneRepository.findByIsDefaultTrue()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建场景
     */
    @PostMapping
    public ResponseEntity<SmartDecomposeScene> create(@RequestBody SmartDecomposeScene scene) {
        scene.setCreatedAt(LocalDateTime.now());
        scene.setUpdatedAt(LocalDateTime.now());
        if (scene.getIsDefault() == null) {
            scene.setIsDefault(false);
        }
        if (scene.getDecisionThresholdMinutes() == null) {
            scene.setDecisionThresholdMinutes(8);
        }
        SmartDecomposeScene saved = sceneRepository.save(scene);
        return ResponseEntity.ok(saved);
    }

    /**
     * 更新场景
     */
    @PutMapping("/{id}")
    public ResponseEntity<SmartDecomposeScene> update(@PathVariable String id, @RequestBody SmartDecomposeScene scene) {
        Optional<SmartDecomposeScene> existing = sceneRepository.findById(id);
        if (!existing.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        SmartDecomposeScene toUpdate = existing.get();
        if (scene.getName() != null) toUpdate.setName(scene.getName());
        if (scene.getDescription() != null) toUpdate.setDescription(scene.getDescription());
        if (scene.getDecisionTemplateId() != null) toUpdate.setDecisionTemplateId(scene.getDecisionTemplateId());
        if (scene.getReviewTemplateId() != null) toUpdate.setReviewTemplateId(scene.getReviewTemplateId());
        if (scene.getRetryTemplateId() != null) toUpdate.setRetryTemplateId(scene.getRetryTemplateId());
        if (scene.getDecisionThresholdMinutes() != null) toUpdate.setDecisionThresholdMinutes(scene.getDecisionThresholdMinutes());
        if (scene.getOutputFormatExecute() != null) toUpdate.setOutputFormatExecute(scene.getOutputFormatExecute());
        if (scene.getOutputFormatSplit() != null) toUpdate.setOutputFormatSplit(scene.getOutputFormatSplit());
        if (scene.getIsDefault() != null) toUpdate.setIsDefault(scene.getIsDefault());

        toUpdate.setUpdatedAt(LocalDateTime.now());
        SmartDecomposeScene saved = sceneRepository.save(toUpdate);
        return ResponseEntity.ok(saved);
    }

    /**
     * 删除场景
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!sceneRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        sceneRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 设置默认场景
     */
    @PostMapping("/{id}/set-default")
    public ResponseEntity<SmartDecomposeScene> setDefault(@PathVariable String id) {
        Optional<SmartDecomposeScene> scene = sceneRepository.findById(id);
        if (!scene.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // 清除旧的默认场景
        sceneRepository.findByIsDefaultTrue().ifPresent(old -> {
            old.setIsDefault(false);
            sceneRepository.save(old);
        });

        // 设置新的默认场景
        SmartDecomposeScene newDefault = scene.get();
        newDefault.setIsDefault(true);
        newDefault.setUpdatedAt(LocalDateTime.now());
        sceneRepository.save(newDefault);

        return ResponseEntity.ok(newDefault);
    }
}