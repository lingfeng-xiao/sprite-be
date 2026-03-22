package com.lingfeng.sprite.action.Actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.lingfeng.sprite.action.ActionPlugin;
import com.lingfeng.sprite.action.ActionResult;

/**
 * 文件搜索动作
 *
 * 使用 java.nio.file 递归搜索文件
 */
public class SearchFilesAction implements ActionPlugin {

    private static final int DEFAULT_MAX_RESULTS = 20;

    @Override
    public String getName() {
        return "SearchFiles";
    }

    @Override
    public ActionResult execute(Map<String, Object> params) {
        String query = null;
        String pathStr = System.getProperty("user.home");

        // 从 params 中提取参数
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Object value = entry.getValue();
            if (value == null) continue;

            String strValue = value.toString();
            if (strValue.isBlank()) continue;

            if (key.equals("query") || key.equals("search") || key.equals("关键词") || key.equals("文件名")) {
                query = strValue;
            } else if (key.equals("path") || key.equals("directory") || key.equals("目录") || key.equals("dir")) {
                pathStr = strValue;
            }
        }

        // 如果没有 query 参数，尝试从 actionParam 获取
        if (query == null || query.isBlank()) {
            Object actionParam = params.get("actionParam");
            if (actionParam != null) {
                String paramStr = actionParam.toString();
                // 格式可能是 "query: xxx" 或 "xxx"
                if (paramStr.contains(":")) {
                    query = paramStr.substring(paramStr.indexOf(":") + 1).trim();
                } else {
                    query = paramStr;
                }
            }
        }

        if (query == null || query.isBlank()) {
            return ActionResult.failure("缺少搜索关键词");
        }

        try {
            Path searchPath = Paths.get(pathStr);
            if (!Files.exists(searchPath) || !Files.isDirectory(searchPath)) {
                return ActionResult.failure("目录不存在: " + pathStr);
            }

            String finalQuery = query;
            try (Stream<Path> paths = Files.walk(searchPath)) {
                List<Path> matches = paths
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.contains(finalQuery.toLowerCase());
                    })
                    .limit(DEFAULT_MAX_RESULTS)
                    .collect(Collectors.toList());

                if (matches.isEmpty()) {
                    return ActionResult.success("未找到包含 '" + query + "' 的文件");
                }

                List<String> results = new ArrayList<>();
                for (Path p : matches) {
                    try {
                        boolean isDir = Files.isDirectory(p);
                        String size = isDir ? "文件夹" : formatFileSize(Files.size(p));
                        results.add(p.toAbsolutePath() + " (" + size + ")");
                    } catch (IOException e) {
                        results.add(p.toAbsolutePath() + " (无法读取)");
                    }
                }

                String message = "找到 " + matches.size() + " 个结果:\n" + String.join("\n", results);
                return ActionResult.success(message, results);
            }

        } catch (Exception e) {
            return ActionResult.failure("搜索失败: " + e.getMessage());
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
}
