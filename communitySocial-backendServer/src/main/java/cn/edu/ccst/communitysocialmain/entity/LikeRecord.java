package cn.edu.ccst.communitysocialmain.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 点赞记录实体类
 */
@Data
public class LikeRecord {
    /**
     * 点赞ID
     */
    private Long likeId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 目标类型：1帖子，2评论
     */
    private Integer targetType;
    
    /**
     * 目标ID
     */
    private Long targetId;
    
    /**
     * 点赞时间
     */
    private LocalDateTime likeTime;
}