package cn.edu.ccst.communitysocialmain.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 举报实体类
 */
@Data
public class Report {
    /**
     * 举报ID
     */
    private Long reportId;
    
    /**
     * 举报用户ID
     */
    private Long reporterId;
    
    /**
     * 被举报目标类型：1帖子，2评论，3用户
     */
    private Integer targetType;
    
    /**
     * 目标ID
     */
    private Long targetId;
    
    /**
     * 举报原因
     */
    private String reason;
    
    /**
     * 处理状态：0待处理，1已处理，2已驳回
     */
    private Integer status;
    
    /**
     * 处理人ID
     */
    private Long handlerId;
    
    /**
     * 处理时间
     */
    private LocalDateTime handleTime;
    
    /**
     * 处理结果
     */
    private String handleResult;
    
    /**
     * 举报时间
     */
    private LocalDateTime reportTime;
}