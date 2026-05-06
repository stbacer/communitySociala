package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 消息记录 DTO（包含发送方用户信息）
 */
@Data
public class MessageRecordDTO {
    /**
     * 消息 ID
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
     * 消息内容
     */
    private String content;
    
    /**
     * 消息图片 URL
     */
    private String imageUrl;
    
    /**
     * 消息类型：1 文本，2 图片，3 语音
     */
    private Integer messageType;
    
    /**
     * 状态：0 撤回，1 未读，2 已读
     */
    private Integer status;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
    
    /**
     * 发送方用户昵称
     */
    private String senderNickname;
    
    /**
     * 发送方用户头像 URL
     */
    private String senderAvatarUrl;
}
