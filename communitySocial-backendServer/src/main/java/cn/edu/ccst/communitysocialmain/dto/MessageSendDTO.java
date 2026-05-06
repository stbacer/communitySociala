package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 发送私信DTO
 */
@Data
public class MessageSendDTO {
    /**
     * 接收方用户ID
     */
    @NotNull(message = "接收方用户ID不能为空")
    private Long receiverId;
    
    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 1000, message = "消息内容长度不能超过1000个字符")
    private String content;
    
    /**
     * 消息图片URL
     */
    private String imageUrl;
}