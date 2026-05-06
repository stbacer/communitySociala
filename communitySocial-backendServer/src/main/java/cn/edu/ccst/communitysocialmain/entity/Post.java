package cn.edu.ccst.communitysocialmain.entity;

import lombok.Data;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.StringTypeHandler;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子实体类
 */
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.*;
import com.alibaba.fastjson.JSON;
import java.util.List;

public class Post {
    /**
     * 帖子ID
     */
    private Long postId;
    
    /**
     * 用户 ID
     */
    private Long userId;
        
    /**
     * 板块分类 ID（关联 category 表）
     */
    private Integer categoryId;
    
    /**
     * 帖子标题
     */
    private String title;
    
    /**
     * 帖子内容
     */
    private String content;
    
    /**
     * 图片URL列表
     */
    private List<String> imageUrls;
    
    /**
     * 发布位置经度
     */
    private BigDecimal longitude;
    
    /**
     * 发布位置纬度
     */
    private BigDecimal latitude;
    
    /**
     * 是否匿名：0 否，1 是
     */
    private Integer isAnonymous;
        
    /**
     * 是否置顶：0 否，1 是
     */
    private Integer isTop;
        
    /**
     * 帖子状态：0 已删除，1 待审核，2 已发布
     */
    private Integer status;
    
    /**
     * 浏览次数
     */
    private Integer viewCount;
    
    /**
     * 点赞次数
     */
    private Integer likeCount;
    
    /**
     * 评论次数
     */
    private Integer commentCount;
    
    /**
     * 收藏次数
     */
    private Integer collectCount;
    
    /**
     * 物品价格（元）- 二手交易专用
     */
    private java.math.BigDecimal price;
    
    /**
     * 交易方式：1 自提，2 快递，3 两者皆可 - 二手交易专用
     */
    private Integer transactionMode;
    
    /**
     * 联系方式 - 二手交易专用
     */
    private String contactInfo;
    
    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
    
    /**
     * 审核人ID
     */
    private Long reviewerId;
    
    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;
    
    /**
     * 审核备注
     */
    private String reviewRemark;
    
    // Getters and Setters
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    
    public Integer getIsAnonymous() { return isAnonymous; }
    public void setIsAnonymous(Integer isAnonymous) { this.isAnonymous = isAnonymous; }
    
    public Integer getIsTop() { return isTop; }
    public void setIsTop(Integer isTop) { this.isTop = isTop; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    
    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }
    
    public Integer getCollectCount() { return collectCount; }
    public void setCollectCount(Integer collectCount) { this.collectCount = collectCount; }
    
    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }
    
    public Integer getTransactionMode() { return transactionMode; }
    public void setTransactionMode(Integer transactionMode) { this.transactionMode = transactionMode; }
    
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
    
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    
    public LocalDateTime getReviewTime() { return reviewTime; }
    public void setReviewTime(LocalDateTime reviewTime) { this.reviewTime = reviewTime; }
    
    public String getReviewRemark() { return reviewRemark; }
    public void setReviewRemark(String reviewRemark) { this.reviewRemark = reviewRemark; }
}