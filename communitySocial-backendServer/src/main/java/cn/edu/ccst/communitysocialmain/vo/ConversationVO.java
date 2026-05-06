package cn.edu.ccst.communitysocialmain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话 VO（管理员使用，包含发送者和接收者信息）
 */
@Data
public class ConversationVO {
    /**
     * 会话 ID（由两个用户 ID 组成）
     */
    private String conversationId;
    
    /**
     * 最后一条消息的 ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long messageId;
    
    /**
     * 发送方用户 ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;
    
    /**
     * 发送者用户信息
     */
    private UserInfoVO senderInfo;
    
    /**
     * 接收方用户 ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long receiverId;
    
    /**
     * 接收者用户信息
     */
    private UserInfoVO receiverInfo;
    
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
    private LocalDateTime lastActiveTime;
    
    /**
     * 未读消息数量
     */
    private Long unreadCount;
}
