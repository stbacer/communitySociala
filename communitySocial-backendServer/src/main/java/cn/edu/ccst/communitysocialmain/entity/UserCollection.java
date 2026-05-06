package cn.edu.ccst.communitysocialmain.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 收藏实体类
 */
@Data
public class UserCollection {
    /**
     * 收藏ID
     */
    private Long collectionId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 帖子ID
     */
    private Long postId;
    
    /**
     * 收藏时间
     */
    private LocalDateTime collectTime;
}