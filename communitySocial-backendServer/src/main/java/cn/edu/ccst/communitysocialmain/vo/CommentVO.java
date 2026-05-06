package cn.edu.ccst.communitysocialmain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论详情VO
 */
@Data
public class CommentVO {
    /**
     * 评论ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long commentId;
    
    /**
     * 用户信息
     */
    private UserInfoVO userInfo;
    
    /**
     * 父评论信息
     */
    private CommentVO parentComment;
    
    /**
     * 评论内容
     */
    private String content;
    
    /**
     * 评论图片URL
     */
    private String imageUrl;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 评论时间
     */
    private LocalDateTime commentTime;
    
    /**
     * 当前用户是否点赞
     */
    private Boolean isLiked;
}