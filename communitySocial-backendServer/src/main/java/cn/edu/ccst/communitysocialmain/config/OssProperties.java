package cn.edu.ccst.communitysocialmain.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {
    
    /**
     * OSS Endpoint
     */
    private String endpoint;
    
    /**
     * AccessKey ID
     */
    private String accessKeyId;
    
    /**
     * AccessKey Secret
     */
    private String accessKeySecret;
    
    /**
     * OSS Bucket 名称
     */
    private String bucketName;
    
    /**
     * 自定义域名（可选，用于 CDN 加速）
     */
    private String customDomain;
    
    /**
     * 文件前缀路径（如：community-social）
     */
    private String filePrefix;
}
