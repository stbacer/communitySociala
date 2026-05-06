package cn.edu.ccst.communitysocialmain.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统日志实体类
 */
@Data
public class SystemLog {
    
    /**
     * 日志ID
     */
    private String logId;
    
    /**
     * 操作用户ID
     */
    private Long userId;
    
    /**
     * 操作类型
     */
    private String operationType;
    
    /**
     * 操作描述
     */
    private String operationDesc;
    
    /**
     * 请求方法
     */
    private String method;
    
    /**
     * 请求URL
     */
    private String url;
    
    /**
     * 请求参数
     */
    private String params;
    
    /**
     * 响应结果
     */
    private String result;
    
    /**
     * IP地址
     */
    private String ip;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 日志级别：INFO, WARN, ERROR
     */
    private String level;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}