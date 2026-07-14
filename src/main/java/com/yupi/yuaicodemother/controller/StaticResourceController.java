package com.yupi.yuaicodemother.controller;

import com.yupi.yuaicodemother.constant.AppConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.File;

/**
 * 静态资源访问
 */
@RestController
@RequestMapping("/static")
public class StaticResourceController {

    // 应用生成根目录（用于预览未部署的代码）
    private static final String CODE_OUTPUT_ROOT = AppConstant.CODE_OUTPUT_ROOT_DIR;
    
    // 应用部署根目录（用于访问已部署的应用）
    private static final String CODE_DEPLOY_ROOT = AppConstant.CODE_DEPLOY_ROOT_DIR;

    /**
     * 预览未部署的生成代码
     * 访问格式：http://localhost:8123/api/static/{codeGenType}_{appId}[/{fileName}]
     */
    @GetMapping("/{codeGenType}_{appId}/**")
    public ResponseEntity<Resource> previewGeneratedCode(
            @PathVariable String codeGenType,
            @PathVariable Long appId,
            HttpServletRequest request) {
        String key = codeGenType + "_" + appId;
        return serveResource(CODE_OUTPUT_ROOT, key, request);
    }

    /**
     * 访问已部署的应用
     * 访问格式：http://localhost:8123/api/static/{deployKey}[/{fileName}]
     */
    @GetMapping("/{deployKey}/**")
    public ResponseEntity<Resource> serveDeployedApp(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        return serveResource(CODE_DEPLOY_ROOT, deployKey, request);
    }

    /**
     * 共用的资源提供方法
     */
    private ResponseEntity<Resource> serveResource(String rootDir, String key, HttpServletRequest request) {
        try {
            // 获取资源路径
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            String pathPrefix = "/static/" + key;
            if (resourcePath.startsWith(pathPrefix)) {
                resourcePath = resourcePath.substring(pathPrefix.length());
            }
            // 如果是目录访问（不带斜杠），重定向到带斜杠的URL
            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }
            // 默认返回 index.html
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }
            // 构建文件路径
            String filePath = rootDir + File.separator + key + resourcePath;
            File file = new File(filePath);
            // 检查文件是否存在
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            // 返回文件资源
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                    .header("Content-Type", getContentTypeWithCharset(filePath))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     */
    private String getContentTypeWithCharset(String filePath) {
        if (filePath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (filePath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (filePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (filePath.endsWith(".png")) return "image/png";
        if (filePath.endsWith(".jpg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
