package cn.edu.ccst.communitysocialmain.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体类 (Redis 存储)
 */
@Data
public class OperationLog {
    
    /** 日志 ID */
    private String logId;
    
    /** 用户 ID */
    private Long userId;
    
    /** 用户昵称（已弃用 username，使用 nickname） */
    private String nickname;
    
    /** 操作员名称 */
    private String operatorName;
    
    /** 操作类型：QUERY/CREATE/UPDATE/DELETE */
    private String operation;
    
    /** 操作内容描述 */
    private String content;
    
    /** 功能模块：USER/POST/CATEGORY/SENSITIVE_WORD/SYSTEM */
    private String module;
    
    /** 子模块：REGISTER/LOGIN/AUTH_SUBMIT/PUBLISH/LIKE 等 */
    private String subModule;
    
    /** HTTP 方法：GET/POST/PUT/DELETE */
    private String method;
    
    /** 请求 URL */
    private String url;
    
    /** 请求 IP */
    private String ip;
    
    /** 客户端类型：1-超级管理员端，2-社区管理员端，3-居民端 */
    private Integer clientType;
    
    /** 执行时长 (毫秒) */
    private Long duration;
    
    /** 请求参数 JSON */
    private String params;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 时间戳 (用于 Redis ZSet 排序) */
    private Long timestamp;
}
