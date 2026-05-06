package cn.edu.ccst.communitysocialmain.service;

import cn.edu.ccst.communitysocialmain.config.OssProperties;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 阿里云 OSS 文件上传服务
 */
@Slf4j
@Service
public class AliyunOssService {
    
    @Autowired
    private OssProperties ossProperties;
    
    /**
     * 上传文件到 OSS
     *
     * @param file     上传的文件
     * @param fileType 文件类型（用于分类存储）
     * @return 文件访问 URL
     * @throws IOException IO 异常
     */
    public String uploadFile(MultipartFile file, String fileType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        
        // 获取原始文件名和扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        
        // 生成新的文件名
        String filename = generateFileName(fileType, extension);
        
        // 获取 OSS 客户端
        OSS ossClient = getOssClient();
        
        try {
            // 上传文件
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossProperties.getBucketName(),
                    filename,
                    new ByteArrayInputStream(file.getBytes())
            );
            
            // 设置文件元数据（可选）
            // ObjectMetadata metadata = new ObjectMetadata();
            // metadata.setContentType(file.getContentType());
            // putObjectRequest.setMetadata(metadata);
            
            ossClient.putObject(putObjectRequest);
            
            // 生成访问 URL
            String fileUrl = buildFileUrl(filename);
            
            log.info("文件上传成功：{}, URL: {}", filename, fileUrl);
            
            return fileUrl;
            
        } finally {
            // 关闭 OSS 客户端
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }
    
    /**
     * 删除 OSS 上的文件
     *
     * @param fileUrl 文件 URL
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        
        try {
            // 从 URL 中提取文件名
            String objectKey = extractObjectKey(fileUrl);
            
            if (objectKey != null) {
                OSS ossClient = getOssClient();
                try {
                    ossClient.deleteObject(ossProperties.getBucketName(), objectKey);
                    log.info("文件删除成功：{}", objectKey);
                } finally {
                    if (ossClient != null) {
                        ossClient.shutdown();
                    }
                }
            }
        } catch (Exception e) {
            log.error("删除文件失败：{}", fileUrl, e);
        }
    }
    
    /**
     * 获取 OSS 客户端
     */
    private OSS getOssClient() {
        return new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );
    }
    
    /**
     * 生成文件名
     */
    private String generateFileName(String fileType, String extension) {
        // 生成日期目录
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        
        // 生成唯一文件名
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String filename = uuid + extension;
        
        // 构建完整路径
        StringBuilder fullPath = new StringBuilder();
        
        // 添加文件前缀
        if (ossProperties.getFilePrefix() != null && !ossProperties.getFilePrefix().isEmpty()) {
            fullPath.append(ossProperties.getFilePrefix()).append("/");
        }
        
        // 添加文件类型目录
        fullPath.append(fileType).append("/");
        
        // 添加日期目录
        fullPath.append(datePath).append("/");
        
        // 添加文件名
        fullPath.append(filename);
        
        return fullPath.toString();
    }
    
    /**
     * 构建文件访问 URL
     */
    private String buildFileUrl(String objectKey) {
        // 如果配置了自定义域名，优先使用自定义域名
        if (ossProperties.getCustomDomain() != null && !ossProperties.getCustomDomain().isEmpty()) {
            String domain = ossProperties.getCustomDomain();
            if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
                domain = "https://" + domain;
            }
            return domain + "/" + objectKey;
        }
        
        // 否则使用 OSS 默认域名
        String endpoint = ossProperties.getEndpoint();
        // 移除 endpoint 中的 https:// 前缀
        endpoint = endpoint.replace("https://", "").replace("http://", "");
        
        return "https://" + ossProperties.getBucketName() + "." + endpoint + "/" + objectKey;
    }
    
    /**
     * 从 URL 中提取对象键（object key）
     */
    private String extractObjectKey(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }
        
        // 如果配置了自定义域名
        if (ossProperties.getCustomDomain() != null && !ossProperties.getCustomDomain().isEmpty()) {
            String domain = ossProperties.getCustomDomain();
            if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
                domain = "https://" + domain;
            }
            if (fileUrl.startsWith(domain)) {
                return fileUrl.substring(domain.length() + 1); // +1 用于跳过斜杠
            }
        }
        
        // 使用 OSS 默认域名
        String bucketName = ossProperties.getBucketName();
        String endpoint = ossProperties.getEndpoint();
        endpoint = endpoint.replace("https://", "").replace("http://", "");
        
        String defaultDomain = "https://" + bucketName + "." + endpoint;
        if (fileUrl.startsWith(defaultDomain)) {
            return fileUrl.substring(defaultDomain.length() + 1);
        }
        
        // 如果都不匹配，尝试直接提取最后部分
        int lastSlashIndex = fileUrl.lastIndexOf("/");
        if (lastSlashIndex != -1 && lastSlashIndex < fileUrl.length() - 1) {
            // 返回相对路径
            return fileUrl.substring(lastSlashIndex + 1);
        }
        
        return null;
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return ".jpg"; // 默认扩展名
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }
}
