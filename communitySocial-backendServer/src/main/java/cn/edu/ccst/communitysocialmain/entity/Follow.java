package cn.edu.ccst.communitysocialmain.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 关注实体类
 */
@Data
public class Follow {
    /**
     * 关注ID
     */
    private Long followId;
    
    /**
     * 关注者ID
     */
    private Long followerId;
    
    /**
     * 被关注者ID
     */
    private Long followedId;
    
    /**
     * 关注时间
     */
    private LocalDateTime followTime;
}