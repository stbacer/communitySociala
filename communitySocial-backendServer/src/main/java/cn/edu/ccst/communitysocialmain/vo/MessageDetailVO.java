package cn.edu.ccst.communitysocialmain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息详情 VO（管理员使用，包含发送者信息）
 */
@Data
public class MessageDetailVO {
    /**
     * 消息 ID
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
     * 消息内容
     */
    private String content;
    
    /**
     * 图片 URL
     */
    private String imageUrl;
    
    /**
     * 消息类型
     */
    private Integer messageType;
    
    /**
     * 状态（1:未读 2:已读 3:撤回）
     */
    private Integer status;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
}
