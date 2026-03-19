package com.openclaw.workflow.service;

import com.openclaw.workflow.entity.Folder;
import com.openclaw.workflow.repository.FolderRepository;
import com.openclaw.workflow.repository.WorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final WorkflowRepository workflowRepository;

    public FolderService(FolderRepository folderRepository,
                         WorkflowRepository workflowRepository) {
        this.folderRepository = folderRepository;
        this.workflowRepository = workflowRepository;
    }

    public List<Folder> findRootFolders() {
        return folderRepository.findByParentIdIsNullOrderBySortOrderAsc();
    }

    public List<Folder> findByParentId(String parentId) {
        return folderRepository.findByParentIdOrderBySortOrderAsc(parentId);
    }

    public List<Folder> findAll() {
        return folderRepository.findAll();
    }

    public Folder findById(String id) {
        return folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文件夹不存在: " + id));
    }

    @Transactional
    public Folder create(String name, String parentId) {
        Folder folder = new Folder();
        folder.setId("folder_" + UUID.randomUUID().toString().substring(0, 8));
        folder.setName(name);
        folder.setParentId(parentId);
        folder.setCreatedAt(LocalDateTime.now());
        return folderRepository.save(folder);
    }

    @Transactional
    public Folder update(String id, String name) {
        Folder folder = findById(id);
        folder.setName(name);
        return folderRepository.save(folder);
    }

    @Transactional
    public void delete(String id) {
        // 检查是否有子文件夹
        long childCount = folderRepository.countByParentId(id);
        if (childCount > 0) {
            throw new RuntimeException("文件夹下有子文件夹，无法删除");
        }

        // 检查是否有工作流
        long workflowCount = workflowRepository.countByFolderId(id);
        if (workflowCount > 0) {
            throw new RuntimeException("文件夹下有工作流，无法删除");
        }

        folderRepository.deleteById(id);
    }
}