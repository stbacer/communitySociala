package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;

/**
 * 待审核内容查询DTO
 */
@Data
public class PendingContentQueryDTO {
    /**
     * 页码
     */
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 10;
    
    /**
     * 内容类型：post帖子，comment评论，user用户实名认证
     */
    private String contentType;
    
    /**
     * 关键词搜索
     */
    private String keyword;
    
    /**
     * 提交时间起始
     */
    private String startTime;
    
    /**
     * 提交时间结束
     */
    private String endTime;
}