package cn.edu.ccst.communitysocialmain.service;

import cn.edu.ccst.communitysocialmain.dto.ConversationDTO;
import cn.edu.ccst.communitysocialmain.dto.MessageRecordDTO;
import cn.edu.ccst.communitysocialmain.entity.PrivateMessage;
import cn.edu.ccst.communitysocialmain.vo.ConversationVO;
import cn.edu.ccst.communitysocialmain.vo.ConversationDetailVO;
import cn.edu.ccst.communitysocialmain.vo.PageVO;

/**
 * 消息服务接口
 */
public interface MessageService {
    
    /**
     * 获取用户的会话列表（包含对方用户信息）
     */
    PageVO<ConversationDTO> getConversations(Long userId, Integer page, Integer size, String keyword);
    
    /**
     * 获取所有会话列表（管理员使用）
     */
    PageVO<ConversationVO> getAllConversations(Integer page, Integer size, String keyword);
    
    /**
     * 获取会话详情（管理员使用）
     */
    ConversationDetailVO getConversationDetail(String conversationId);
    
    /**
     * 获取两个用户之间的消息记录（包含发送方用户信息）
     */
    PageVO<MessageRecordDTO> getMessageRecords(Long userId, Long contactId, Integer page, Integer size);
    
    /**
     * 发送消息
     */
    PrivateMessage sendMessage(Long senderId, MessageSendDTO messageSendDTO);
    
    /**
     * 消息发送DTO
     */
    class MessageSendDTO {
        private Long receiverId;
        private String content;
        private String imageUrl;
        private Integer messageType; // 1: 文本, 2: 图片, 3: 语音

        // Getters and Setters
        public Long getReceiverId() { return receiverId; }
        public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        
        public Integer getMessageType() { return messageType; }
        public void setMessageType(Integer messageType) { this.messageType = messageType; }
    }
    
    /**
     * 标记消息为已读
     */
    void markMessagesAsRead(Long userId, Long contactId);
    
    /**
     * 获取未读消息数量
     */
    Long getUnreadMessageCount(Long userId);
    
    /**
     * 撤回消息
     */
    void recallMessage(Long userId, Long messageId);
    
    /**
     * 删除消息
     */
    void deleteMessage(Long userId, Long messageId);
    
    /**
     * 发送系统消息
     * @param receiverId 接收方用户 ID
     * @param content 消息内容
     * @param messageType 消息类型（可选，默认 1）
     */
    void sendSystemMessage(Long receiverId, String content, Integer messageType);
}