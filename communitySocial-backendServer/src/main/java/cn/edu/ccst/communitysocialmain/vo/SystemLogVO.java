package cn.edu.ccst.communitysocialmain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统日志VO
 */
@Data
public class SystemLogVO {
    
    /**
     * 日志ID
     */
    private String logId;
    
    /**
     * 操作用户昵称
     */
    private String userNickname;
    
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
     * 日志级别
     */
    private String level;
    
    /**
     * IP地址
     */
    private String ip;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}