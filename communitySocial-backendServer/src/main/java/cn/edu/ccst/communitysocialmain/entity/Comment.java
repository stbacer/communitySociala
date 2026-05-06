package cn.edu.ccst.communitysocialmain.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论实体类
 */
public class Comment {
    /**
     * 评论ID
     */
    private Long commentId;
    
    /**
     * 帖子ID
     */
    private Long postId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 回复的评论ID（为空表示直接回复帖子）
     */
    private Long parentId;
    
    /**
     * 评论内容
     */
    private String content;
    
    /**
     * 评论状态：0待审核，1已发布，2已删除
     */
    private Integer status;
    
    /**
     * 图片URL
     */
    private String imageUrl;
    
    /**
     * 点赞次数
     */
    private Integer likeCount;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 评论时间（用于数据库字段映射）
     */
    private LocalDateTime commentTime;
    
    // Getters and Setters
    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }
    
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public LocalDateTime getCommentTime() { return commentTime; }
    public void setCommentTime(LocalDateTime commentTime) { this.commentTime = commentTime; }
}