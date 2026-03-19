package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.ApiResponse;
import com.openclaw.workflow.entity.Folder;
import com.openclaw.workflow.service.FolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Folder", description = "文件夹管理接口")
@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderService folderService;

    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @Operation(summary = "获取文件夹列表")
    @GetMapping
    public ApiResponse<List<Folder>> list(@RequestParam(required = false) String parentId) {
        List<Folder> folders;
        if (parentId != null) {
            folders = folderService.findByParentId(parentId);
        } else {
            folders = folderService.findRootFolders();
        }
        return ApiResponse.success(folders);
    }

    @Operation(summary = "创建文件夹")
    @PostMapping
    public ApiResponse<Folder> create(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String parentId = request.get("parentId");
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("文件夹名称不能为空");
        }
        Folder folder = folderService.create(name, parentId);
        return ApiResponse.success(folder);
    }

    @Operation(summary = "更新文件夹")
    @PutMapping("/{id}")
    public ApiResponse<Folder> update(@PathVariable String id,
                                       @RequestBody Map<String, String> request) {
        String name = request.get("name");
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("文件夹名称不能为空");
        }
        Folder folder = folderService.update(id, name);
        return ApiResponse.success(folder);
    }

    @Operation(summary = "删除文件夹")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        folderService.delete(id);
        return ApiResponse.success();
    }
}