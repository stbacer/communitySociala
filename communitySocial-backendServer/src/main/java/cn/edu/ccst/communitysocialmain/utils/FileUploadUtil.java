package cn.edu.ccst.communitysocialmain.utils;

import cn.edu.ccst.communitysocialmain.service.AliyunOssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 文件上传工具类
 */
@Slf4j
@Component
public class FileUploadUtil {
    
    @Autowired
    private AliyunOssService aliyunOssService;
    
    // 允许的文件扩展名
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );
    
    // 最大文件大小 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    
    /**
     * 上传文件
     */
    public String uploadFile(MultipartFile file) {
        try {
            // 使用阿里云 OSS 上传文件
            return aliyunOssService.uploadFile(file, "other");
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除文件
     */
    public boolean deleteFile(String fileUrl) {
        try {
            // 使用阿里云 OSS 删除文件
            aliyunOssService.deleteFile(fileUrl);
            return true;
        } catch (Exception e) {
            log.error("删除文件失败：{}", fileUrl, e);
            return false;
        }
    }
    
    /**
     * 获取允许的文件类型
     */
    public List<String> getAllowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }
    
    /**
     * 获取最大文件大小
     */
    public long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }
}