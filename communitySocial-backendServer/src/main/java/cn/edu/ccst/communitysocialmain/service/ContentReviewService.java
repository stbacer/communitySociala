package cn.edu.ccst.communitysocialmain.service;

import cn.edu.ccst.communitysocialmain.dto.ContentReviewDTO;
import cn.edu.ccst.communitysocialmain.dto.PendingContentQueryDTO;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import java.util.List;

/**
 * 全站内容审核服务接口
 */
public interface ContentReviewService {
    
    /**
     * 获取待审核内容列表
     */
    PageVO<PendingContentVO> getPendingContentList(PendingContentQueryDTO queryDTO);
    
    /**
     * 审核单个内容
     */
    void reviewContent(String adminUserId, ContentReviewDTO reviewDTO);
    
    /**
     * 批量审核内容
     */
    void batchReviewContent(String adminUserId, List<ContentReviewDTO> reviewDTOs);
    
    /**
     * 获取审核统计信息
     */
    ContentStatistics getReviewStatistics();
    
    /**
     * 待审核内容VO
     */
    class PendingContentVO {
        private String contentId;
        private String contentType;
        private String title;
        private String content;
        private String authorId;
        private String authorName;
        private String submitTime;
        private Integer status;
        private List<String> imageUrls; // 图片URL列表
        
        // Getters and Setters
        public String getContentId() { return contentId; }
        public void setContentId(String contentId) { this.contentId = contentId; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getAuthorId() { return authorId; }
        public void setAuthorId(String authorId) { this.authorId = authorId; }
        
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        
        public String getSubmitTime() { return submitTime; }
        public void setSubmitTime(String submitTime) { this.submitTime = submitTime; }
        
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        
        public List<String> getImageUrls() { return imageUrls; }
        public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    }
    
    /**
     * 内容统计信息
     */
    class ContentStatistics {
        private Long pendingPostCount;      // 待审核帖子数
        private Long pendingCommentCount;   // 待审核评论数
        private Long pendingUserCount;      // 待审核用户数
        private Long todayReviewedCount;    // 今日已审核数
        private Long totalReviewedCount;    // 总审核数
        
        // Getters and Setters
        public Long getPendingPostCount() { return pendingPostCount; }
        public void setPendingPostCount(Long pendingPostCount) { this.pendingPostCount = pendingPostCount; }
        
        public Long getPendingCommentCount() { return pendingCommentCount; }
        public void setPendingCommentCount(Long pendingCommentCount) { this.pendingCommentCount = pendingCommentCount; }
        
        public Long getPendingUserCount() { return pendingUserCount; }
        public void setPendingUserCount(Long pendingUserCount) { this.pendingUserCount = pendingUserCount; }
        
        public Long getTodayReviewedCount() { return todayReviewedCount; }
        public void setTodayReviewedCount(Long todayReviewedCount) { this.todayReviewedCount = todayReviewedCount; }
        
        public Long getTotalReviewedCount() { return totalReviewedCount; }
        public void setTotalReviewedCount(Long totalReviewedCount) { this.totalReviewedCount = totalReviewedCount; }
    }
}