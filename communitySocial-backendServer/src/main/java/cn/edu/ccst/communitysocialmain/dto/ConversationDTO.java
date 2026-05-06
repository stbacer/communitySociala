package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话 DTO（包含对方用户信息）
 */
@Data
public class ConversationDTO {
    /**
     * 消息 ID（最后一条消息的 ID）
     */
    private Long messageId;
    
    /**
     * 发送方用户 ID
     */
    private Long senderId;
    
    /**
     * 接收方用户 ID
     */
    private Long receiverId;
    
    /**
     * 消息内容（最后一条消息）
     */
    private String content;
    
    /**
     * 消息类型
     */
    private Integer messageType;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 发送时间（最后一条消息的时间）
     */
    private LocalDateTime sendTime;
    
    /**
     * 对话方用户 ID（对方用户 ID）
     */
    private Long contactId;
    
    /**
     * 对话方用户昵称
     */
    private String contactNickname;
    
    /**
     * 对话方用户头像 URL
     */
    private String contactAvatarUrl;
    
    /**
     * 未读消息数量
     */
    private Long unreadCount;
}
