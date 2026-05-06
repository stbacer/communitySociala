package cn.edu.ccst.communitysocialmain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话详情 VO（管理员使用，包含完整消息记录）
 */
@Data
public class ConversationDetailVO {
    /**
     * 会话 ID
     */
    private String conversationId;
    
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
     * 消息列表
     */
    private List<MessageDetailVO> messages;
    
    /**
     * 总消息数
     */
    private Long totalMessages;
    
    /**
     * 未读消息数量
     */
    private Long unreadCount;
    
    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;
}
