package cn.edu.ccst.communitysocialmain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 私信列表VO
 */
@Data
public class MessageVO {
    /**
     * 消息ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long messageId;
    
    /**
     * 发送方用户信息
     */
    private UserInfoVO senderInfo;
    
    /**
     * 接收方用户信息
     */
    private UserInfoVO receiverInfo;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息图片URL
     */
    private String imageUrl;
    
    /**
     * 状态
     */
    private Integer status;
    
    /**
     * 发送时间
     */
    private LocalDateTime sendTime;
}