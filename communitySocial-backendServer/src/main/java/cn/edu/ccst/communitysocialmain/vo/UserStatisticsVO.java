package cn.edu.ccst.communitysocialmain.vo;

import lombok.Data;

/**
 * 用户个人统计数据VO
 */
@Data
public class UserStatisticsVO {
    /**
     * 发布的帖子数量
     */
    private Long postCount;
    
    /**
     * 获得的点赞数量
     */
    private Long likeCount;
    
    /**
     * 粉丝数量
     */
    private Long followerCount;
    
    /**
     * 关注的用户数量
     */
    private Long followingCount;
    
    /**
     * 收藏的帖子数量
     */
    private Long collectCount;
    
    /**
     * 评论数量
     */
    private Long commentCount;
    
    // Getters and Setters
    public Long getPostCount() { return postCount; }
    public void setPostCount(Long postCount) { this.postCount = postCount; }
    
    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
    
    public Long getFollowerCount() { return followerCount; }
    public void setFollowerCount(Long followerCount) { this.followerCount = followerCount; }
    
    public Long getFollowingCount() { return followingCount; }
    public void setFollowingCount(Long followingCount) { this.followingCount = followingCount; }
    
    public Long getCollectCount() { return collectCount; }
    public void setCollectCount(Long collectCount) { this.collectCount = collectCount; }
    
    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }
}