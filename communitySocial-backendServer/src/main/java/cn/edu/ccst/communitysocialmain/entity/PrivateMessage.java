package cn.edu.ccst.communitysocialmain.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 私信实体类
 */
@Data
public class PrivateMessage {
    /**
     * 消息ID
     */
    private Long messageId;
    
    /**
     * 发送方用户ID
     */
    private Long senderId;
    
    /**
     * 接收方用户ID
     */
    private Long receiverId;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息图片URL
     */
    private String imageUrl;
    
    /**
     * 消息类型：1文本，2图片，3语音
     */
    private Integer messageType;
    
    /**
     * 状态：0撤回，1未读，2已读
     */
    private Integer status;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
}