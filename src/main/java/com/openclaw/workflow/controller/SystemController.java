package com.openclaw.workflow.controller;

import com.openclaw.workflow.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Tag(name = "System", description = "系统接口")
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Value("${user.home}")
    private String userHome;

    @Operation(summary = "浏览目录")
    @PostMapping("/browse-directory")
    public ApiResponse<Map<String, Object>> browseDirectory(@RequestBody(required = false) Map<String, String> request) {
        String path = request != null ? request.get("path") : null;
        if (path == null || path.isEmpty()) {
            path = userHome;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("path", path);

        try {
            File dir = new File(path);
            if (!dir.exists() || !dir.isDirectory()) {
                result.put("error", "目录不存在");
                result.put("directories", Collections.emptyList());
                return ApiResponse.success(result);
            }

            List<Map<String, Object>> directories = new ArrayList<>();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && !file.isHidden()) {
                        Map<String, Object> dirInfo = new HashMap<>();
                        dirInfo.put("name", file.getName());
                        dirInfo.put("path", file.getAbsolutePath());
                        directories.add(dirInfo);
                    }
                }
            }

            // 按名称排序
            directories.sort(Comparator.comparing(d -> (String) d.get("name")));

            result.put("directories", directories);
            result.put("parent", dir.getParent());

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("directories", Collections.emptyList());
        }

        return ApiResponse.success(result);
    }

    @Operation(summary = "读取文件")
    @GetMapping("/read-file")
    public ApiResponse<Map<String, Object>> readFile(@RequestParam String path) {
        Map<String, Object> result = new HashMap<>();
        result.put("path", path);

        try {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                result.put("error", "文件不存在");
                result.put("exists", false);
                return ApiResponse.success(result);
            }

            String content = Files.readString(filePath);
            result.put("content", content);
            result.put("exists", true);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("exists", false);
        }

        return ApiResponse.success(result);
    }

    @Operation(summary = "写入文件")
    @PostMapping("/write-file")
    public ApiResponse<Void> writeFile(@RequestBody Map<String, String> request) {
        String path = request.get("path");
        String content = request.get("content");

        try {
            Path filePath = Paths.get(path);
            Files.writeString(filePath, content);
        } catch (Exception e) {
            throw new RuntimeException("写入文件失败: " + e.getMessage());
        }

        return ApiResponse.success();
    }
}