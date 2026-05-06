package cn.edu.ccst.communitysocialmain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论详情 VO（管理员使用）
 */
@Data
public class CommentDetailVO {
    /**
     * 评论 ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long commentId;
    
    /**
     * 帖子 ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long postId;
    
    /**
     * 用户信息
     */
    private UserInfoVO userInfo;
    
    /**
     * 评论内容
     */
    private String content;
    
    /**
     * 父评论 ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;
    
    /**
     * 目标类型（1:帖子 2:评论 3:用户）
     */
    private Integer targetType;
    
    /**
     * 目标 ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;
    
    /**
     * 被评论的帖子标题
     */
    private String targetPostTitle;
    
    /**
     * 被评论的评论内容
     */
    private String targetCommentContent;
    
    /**
     * 点赞数
     */
    private Integer likeCount;
    
    /**
     * 状态（0:删除 1:待审核 2:已通过 3:已拒绝）
     */
    private Integer status;
    
    /**
     * 评论时间
     */
    private LocalDateTime commentTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
