package cn.edu.ccst.communitysocialmain.service.impl;

import cn.edu.ccst.communitysocialmain.config.WebSocketServer;
import cn.edu.ccst.communitysocialmain.controller.resident.ResidentMessageController;
import cn.edu.ccst.communitysocialmain.dto.ConversationDTO;
import cn.edu.ccst.communitysocialmain.dto.MessageRecordDTO;
import cn.edu.ccst.communitysocialmain.entity.PrivateMessage;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.mapper.PrivateMessageMapper;
import cn.edu.ccst.communitysocialmain.mapper.UserMapper;
import cn.edu.ccst.communitysocialmain.service.MessageService;
import cn.edu.ccst.communitysocialmain.service.RedisMessageQueueService;
import cn.edu.ccst.communitysocialmain.utils.CommonUtil;
import cn.edu.ccst.communitysocialmain.utils.SnowflakeIdGenerator;
import cn.edu.ccst.communitysocialmain.vo.ConversationVO;
import cn.edu.ccst.communitysocialmain.vo.ConversationDetailVO;
import cn.edu.ccst.communitysocialmain.vo.MessageDetailVO;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.UserInfoVO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息服务实现类
 */
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {
    
    @Autowired
    private PrivateMessageMapper privateMessageMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RedisMessageQueueService redisMessageQueueService;

    @Override
    public PageVO<ConversationDTO> getConversations(Long userId, Integer page, Integer size, String keyword) {
        int offset = (page - 1) * size;
            
        log.info("获取用户{}的会话列表，page={}, size={}, keyword={}", userId, page, size, keyword);
        log.debug("keyword 参数实际值：'{}', isNull: {}, isEmpty: {}", keyword, keyword == null, keyword != null && keyword.isEmpty());
            
        // 获取用户的所有消息记录
        List<PrivateMessage> allMessages = privateMessageMapper.selectUserMessages(userId, 0, 1000);
        log.info("查询到{}条消息记录", allMessages.size());
            
        // 按联系人分组，获取每个联系人的最后一条消息和未读数量
        Map<String, PrivateMessage> conversationMap = new HashMap<>();
        Map<String, Long> unreadCountMap = new HashMap<>();
        Set<String> contactIds = new HashSet<>();
            
        for (PrivateMessage message : allMessages) {
            Long contactId = message.getSenderId().equals(userId) ? 
                              message.getReceiverId() : message.getSenderId();
                
            log.debug("处理消息：senderId={}, receiverId={}, contactId={}, content={}", 
                     message.getSenderId(), message.getReceiverId(), contactId, message.getContent());
                
            // 统计未读消息数量
            if (message.getStatus() == 1 && message.getReceiverId().equals(userId)) {
                unreadCountMap.put(contactId.toString(), unreadCountMap.getOrDefault(contactId.toString(), 0L) + 1);
            }
                
            // 如果有搜索关键词，需要匹配联系人昵称
            if (keyword != null && !keyword.trim().isEmpty()) {
                User contactUser = userMapper.selectById(contactId);
                log.debug("搜索过滤：contactId={}, contactUser={}, nickname={}, username={}, keyword={}",
                         contactId, contactUser, 
                         contactUser != null ? contactUser.getNickname() : "null",
                         contactUser != null ? contactUser.getUsername() : "null",
                         keyword);
                if (contactUser == null || 
                    (!contactUser.getNickname().contains(keyword))) {
                    log.debug("消息被过滤掉");
                    continue;
                }
            } else {
                log.debug("无搜索关键词，不过滤");
            }
                
            contactIds.add(contactId.toString());
                
            // 保留每个联系人的最新消息
            if (!conversationMap.containsKey(contactId.toString()) || 
                message.getSendTime().isAfter(conversationMap.get(contactId.toString()).getSendTime())) {
                conversationMap.put(contactId.toString(), message);
                log.debug("添加到 conversationMap: contactId={}", contactId);
            }
        }
            
        log.info("分组后得到{}个会话", conversationMap.size());
            
        // 转换为列表并排序
        List<PrivateMessage> conversations = new ArrayList<>(conversationMap.values());
        conversations.sort((m1, m2) -> m2.getSendTime().compareTo(m1.getSendTime()));
            
        // 分页处理
        int total = conversations.size();
        int fromIndex = offset;
        int toIndex = Math.min(offset + size, total);
            
        List<PrivateMessage> pageConversations = fromIndex < total ? 
            conversations.subList(fromIndex, toIndex) : new ArrayList<>();
            
        log.info("分页后返回{}条会话记录", pageConversations.size());
            
        // 转换为 DTO，并补充联系人信息
        List<ConversationDTO> dtoList = new ArrayList<>();
        for (PrivateMessage conversation : pageConversations) {
            Long contactId = conversation.getSenderId().equals(userId) ? 
                              conversation.getReceiverId() : conversation.getSenderId();
            
            ConversationDTO dto = new ConversationDTO();
            BeanUtils.copyProperties(conversation, dto);
            dto.setContactId(contactId);
            dto.setUnreadCount(unreadCountMap.getOrDefault(contactId.toString(), 0L));
            
            // 获取联系人用户信息
            User contactUser = userMapper.selectById(contactId);
            if (contactUser != null) {
                dto.setContactNickname(contactUser.getNickname());
                dto.setContactAvatarUrl(contactUser.getAvatarUrl());
            } else {
                dto.setContactNickname("未知用户");
                dto.setContactAvatarUrl(null);
                log.warn("未找到联系人{}的信息", contactId);
            }
            
            dtoList.add(dto);
        }
            
        return new PageVO<>(page, size, (long) total, dtoList);
    }

    @Override
    public PageVO<ConversationVO> getAllConversations(Integer page, Integer size, String keyword) {
        int offset = (page - 1) * size;
        
        log.info("获取所有会话列表，page={}, size={}, keyword={}", page, size, keyword);
        
        // 获取所有消息记录（限制 1000 条）
        List<PrivateMessage> allMessages = privateMessageMapper.selectAllMessages(0, 1000);
        
        // 按会话分组
        Map<String, ConversationData> conversationMap = new HashMap<>();
        
        for (PrivateMessage message : allMessages) {
            String convId = message.getSenderId().compareTo(message.getReceiverId()) < 0 
                          ? message.getSenderId() + "_" + message.getReceiverId()
                          : message.getReceiverId() + "_" + message.getSenderId();
            
            // 统计未读数量
            if (message.getStatus() == 1) {
                ConversationData data = conversationMap.computeIfAbsent(convId, k -> new ConversationData());
                data.unreadCount++;
            }
            
            // 保留最新消息
            ConversationData data = conversationMap.computeIfAbsent(convId, k -> new ConversationData());
            if (data.lastMessage == null || message.getSendTime().isAfter(data.lastMessage.getSendTime())) {
                data.lastMessage = message;
            }
        }
        
        // 转换为 VO 列表
        List<ConversationVO> conversations = new ArrayList<>();
        for (Map.Entry<String, ConversationData> entry : conversationMap.entrySet()) {
            ConversationVO vo = convertToConversationVO(entry.getKey(), entry.getValue());
            
            // 关键词过滤
            if (keyword != null && !keyword.trim().isEmpty()) {
                if (!vo.getSenderInfo().getNickname().contains(keyword) &&
                    !vo.getReceiverInfo().getNickname().contains(keyword)) {
                    continue;
                }
            }
            
            conversations.add(vo);
        }
        
        // 排序
        conversations.sort((v1, v2) -> v2.getLastActiveTime().compareTo(v1.getLastActiveTime()));
        
        // 分页
        int total = conversations.size();
        int fromIndex = offset;
        int toIndex = Math.min(offset + size, total);
        List<ConversationVO> pageConversations = fromIndex < total 
            ? conversations.subList(fromIndex, toIndex) 
            : new ArrayList<>();
        
        return new PageVO<>(page, size, (long) total, pageConversations);
    }
    
    @Override
    public ConversationDetailVO getConversationDetail(String conversationId) {
        log.info("获取会话详情，conversationId={}", conversationId);
        
        // 解析会话 ID（格式：userId1_userId2）
        String[] parts = conversationId.split("_");
        if (parts.length != 2) {
            throw new RuntimeException("无效的会话 ID 格式");
        }
        
        Long userId1 = Long.parseLong(parts[0]);
        Long userId2 = Long.parseLong(parts[1]);
        
        // 获取两个用户之间的所有消息
        List<PrivateMessage> messages = privateMessageMapper.selectConversation(userId1.toString(), userId2.toString(), 0, 100);
        
        // 统计未读数量
        Long unreadCount = privateMessageMapper.countUnreadInConversation(userId1.toString(), userId2.toString());
        
        // 转换为 VO
        ConversationDetailVO detailVO = new ConversationDetailVO();
        detailVO.setConversationId(conversationId);
        detailVO.setSenderId(userId1);
        detailVO.setReceiverId(userId2);
        
        // 设置用户信息
        User user1 = userMapper.selectById(userId1);
        if (user1 != null) {
            UserInfoVO senderInfo = new UserInfoVO();
            BeanUtils.copyProperties(user1, senderInfo);
            detailVO.setSenderInfo(senderInfo);
        }
        
        User user2 = userMapper.selectById(userId2);
        if (user2 != null) {
            UserInfoVO receiverInfo = new UserInfoVO();
            BeanUtils.copyProperties(user2, receiverInfo);
            detailVO.setReceiverInfo(receiverInfo);
        }
        
        // 设置消息列表
        List<MessageDetailVO> messageDetailVOs = messages.stream()
                .map(this::convertToMessageDetailVO)
                .collect(Collectors.toList());
        detailVO.setMessages(messageDetailVOs);
        detailVO.setTotalMessages((long) messages.size());
        detailVO.setUnreadCount(unreadCount);
        
        if (!messages.isEmpty()) {
            detailVO.setLastActiveTime(messages.get(messages.size() - 1).getSendTime());
        }
        
        return detailVO;
    }
    
    /**
     * 内部类，用于存储会话数据
     */
    private static class ConversationData {
        PrivateMessage lastMessage;
        Long unreadCount = 0L;
    }
    
    /**
     * 转换为 ConversationVO
     */
    private ConversationVO convertToConversationVO(String conversationId, ConversationData data) {
        ConversationVO vo = new ConversationVO();
        vo.setConversationId(conversationId);
        
        if (data.lastMessage != null) {
            vo.setMessageId(data.lastMessage.getMessageId());
            vo.setSenderId(data.lastMessage.getSenderId());
            vo.setReceiverId(data.lastMessage.getReceiverId());
            vo.setContent(data.lastMessage.getContent());
            vo.setMessageType(data.lastMessage.getMessageType());
            vo.setStatus(data.lastMessage.getStatus());
            vo.setLastActiveTime(data.lastMessage.getSendTime());
        }
        
        vo.setUnreadCount(data.unreadCount);
        return vo;
    }
    
    /**
     * 转换为 MessageDetailVO
     */
    private MessageDetailVO convertToMessageDetailVO(PrivateMessage message) {
        MessageDetailVO vo = new MessageDetailVO();
        BeanUtils.copyProperties(message, vo);
        
        // 设置发送者信息
        User sender = userMapper.selectById(message.getSenderId());
        if (sender != null) {
            UserInfoVO senderInfo = new UserInfoVO();
            BeanUtils.copyProperties(sender, senderInfo);
            vo.setSenderInfo(senderInfo);
        }
        
        // 设置接收者信息
        User receiver = userMapper.selectById(message.getReceiverId());
        if (receiver != null) {
            UserInfoVO receiverInfo = new UserInfoVO();
            BeanUtils.copyProperties(receiver, receiverInfo);
            vo.setReceiverInfo(receiverInfo);
        }
        
        return vo;
    }

    @Override
    public PageVO<MessageRecordDTO> getMessageRecords(Long userId, Long contactId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        
        // 获取两个用户之间的所有消息
        List<PrivateMessage> messages = privateMessageMapper.selectConversation(String.valueOf(userId), String.valueOf(contactId), offset, size);
        Long total = privateMessageMapper.countConversationMessages(String.valueOf(userId), String.valueOf(contactId));
        
        // 按时间正序排列（旧的在前）
        messages.sort(Comparator.comparing(PrivateMessage::getSendTime));
        
        // 转换为 DTO，并补充发送方用户信息
        List<MessageRecordDTO> dtoList = new ArrayList<>();
        for (PrivateMessage msg : messages) {
            MessageRecordDTO dto = new MessageRecordDTO();
            BeanUtils.copyProperties(msg, dto);
            
            // 获取发送方用户信息
            User sender = userMapper.selectById(msg.getSenderId());
            if (sender != null) {
                dto.setSenderNickname(sender.getNickname());
                dto.setSenderAvatarUrl(sender.getAvatarUrl());
            } else {
                dto.setSenderNickname("未知用户");
                dto.setSenderAvatarUrl(null);
            }
            
            dtoList.add(dto);
        }
        
        return new PageVO<>(page, size, total, dtoList);
    }

    @Override
    @Transactional
    public PrivateMessage sendMessage(Long senderId, MessageService.MessageSendDTO messageSendDTO) {
        // 验证接收方用户是否存在
        User receiver = userMapper.selectById(messageSendDTO.getReceiverId());
        if (receiver == null) {
            throw new RuntimeException("接收方用户不存在");
        }
            
        // 创建消息对象
        PrivateMessage message = new PrivateMessage();
        message.setMessageId(SnowflakeIdGenerator.nextId());
        message.setSenderId(senderId);
        message.setReceiverId(messageSendDTO.getReceiverId());
        message.setContent(messageSendDTO.getContent());
        message.setImageUrl(messageSendDTO.getImageUrl());
        message.setMessageType(messageSendDTO.getMessageType() != null ? 
                              messageSendDTO.getMessageType() : 1); // 默认文本消息
        message.setStatus(1); // 未读状态
        message.setSendTime(LocalDateTime.now());
            
        // 插入消息
        int result = privateMessageMapper.insert(message);
        if (result <= 0) {
            throw new RuntimeException("发送消息失败");
        }
            
        log.info("用户{}向用户{}发送消息：{}", senderId, messageSendDTO.getReceiverId(), 
                 messageSendDTO.getContent());
            
        // 通过 WebSocket 推送新消息通知给接收者
        try {
            notifyNewMessage(message);
        } catch (Exception e) {
            log.error("WebSocket 推送新消息通知失败：{}", e.getMessage());
            // WebSocket 推送失败不影响主业务
        }
            
        return message;
    }

    @Override
    @Transactional
    public void markMessagesAsRead(Long userId, Long contactId) {
        // 批量更新消息状态为已读
        privateMessageMapper.batchUpdateToRead(userId, contactId);
        log.info("用户{}标记与用户{}的对话为已读", userId, contactId);
    }

    @Override
    public Long getUnreadMessageCount(Long userId) {
        return privateMessageMapper.countUnreadMessages(userId);
    }

    @Override
    @Transactional
    public void recallMessage(Long userId, Long messageId) {
        PrivateMessage message = privateMessageMapper.selectById(messageId);
        if (message == null) {
            throw new RuntimeException("消息不存在");
        }
        
        // 验证权限（只能撤回自己发送的消息）
        if (!message.getSenderId().equals(userId)) {
            throw new RuntimeException("无权撤回此消息");
        }
        
        // 验证撤回时间限制（假设2分钟内可以撤回）
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(message.getSendTime().plusMinutes(2))) {
            throw new RuntimeException("超过撤回时间限制");
        }
        
        // 更新消息状态为撤回
        privateMessageMapper.recallMessage(messageId);
        log.info("用户{}撤回消息{}", userId, messageId);
    }

    @Override
    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        PrivateMessage message = privateMessageMapper.selectById(messageId);
        if (message == null) {
            throw new RuntimeException("消息不存在");
        }
        
        // 验证权限（只能删除自己相关联的消息）
        if (!message.getSenderId().equals(userId) && !message.getReceiverId().equals(userId)) {
            throw new RuntimeException("无权删除此消息");
        }
        
        // 删除消息
        privateMessageMapper.deleteById(messageId);
        log.info("用户{}删除消息{}", userId, messageId);
    }

    @Transactional
    public void sendSystemMessage(Long receiverId, String content, Integer messageType) {
        // 创建系统消息对象
        PrivateMessage message = new PrivateMessage();
        message.setMessageId(SnowflakeIdGenerator.nextId());
        message.setSenderId(1L);  // 使用系统通知用户ID
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : 1); // 默认文本消息
        message.setStatus(1); // 未读状态
        message.setSendTime(LocalDateTime.now());
        
        // 插入消息
        int result = privateMessageMapper.insert(message);
        if (result <= 0) {
            throw new RuntimeException("发送系统消息失败");
        }
        
        log.info("向用户{}发送系统消息：{}", receiverId, content);
        
        // 通过 WebSocket 推送系统消息通知
        try {
            notifySystemMessage(message);
        } catch (Exception e) {
            log.error("WebSocket 推送系统消息通知失败：{}", e.getMessage());
            // WebSocket 推送失败不影响主业务
        }
    }

    /**
     * 通过 WebSocket 推送新消息通知
     * @param message 消息对象
     */
    private void notifyNewMessage(PrivateMessage message) {
        try {
            // 构建消息数据
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("messageId", message.getMessageId());
            messageData.put("senderId", message.getSenderId());
            messageData.put("receiverId", message.getReceiverId());
            messageData.put("content", message.getContent());
            messageData.put("messageType", message.getMessageType());
            messageData.put("sendTime", message.getSendTime().toString());
            messageData.put("status", message.getStatus());

            // 获取发送者和接收者信息
            User sender = userMapper.selectById(message.getSenderId());
            if (sender != null) {
                messageData.put("senderNickname", sender.getNickname());
                messageData.put("senderAvatarUrl", sender.getAvatarUrl());
            }

            // 转换为 JSON
            String jsonData = JSON.toJSONString(messageData);

            // 推送给接收者
            Long receiverId = message.getReceiverId();
            WebSocketServer.sendNewMessageNotification(receiverId.toString(), jsonData);

            // 注意：不再推送给发送者，避免重复显示
            // 发送者通过前端乐观更新显示消息

            log.info("WebSocket 推送新消息通知成功：receiverId={}", receiverId);
        } catch (Exception e) {
            log.error("WebSocket 推送新消息通知异常：{}", e.getMessage(), e);
            throw new RuntimeException("WebSocket 推送失败", e);
        }
    }
    
    /**
     * 通过 WebSocket 推送系统消息通知
     * @param message 系统消息对象
     */
    private void notifySystemMessage(PrivateMessage message) {
        try {
            // 构建消息数据
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("messageId", message.getMessageId());
            messageData.put("senderId", "SYSTEM");
            messageData.put("receiverId", message.getReceiverId());
            messageData.put("content", message.getContent());
            messageData.put("messageType", message.getMessageType());
            messageData.put("sendTime", message.getSendTime().toString());
            messageData.put("status", message.getStatus());
            messageData.put("isSystemMessage", true);
            
            // 根据消息类型设置标题
            String title = "系统通知";
            Integer messageType = message.getMessageType();
            if (messageType != null) {
                switch (messageType) {
                    case 1:
                        title = "实名认证通知";
                        break;
                    case 2:
                        title = "帖子审核通知";
                        break;
                    case 3:
                        title = "举报处理通知";
                        break;
                    default:
                        title = "系统通知";
                }
            }
            messageData.put("title", title);

            // 转换为 JSON
            String jsonData = JSON.toJSONString(messageData);

            // 推送给接收者
            Long receiverId = message.getReceiverId();
            WebSocketServer.sendNewMessageNotification(receiverId.toString(), jsonData);

            log.info("WebSocket 推送系统消息通知成功：receiverId={}, title={}", receiverId, title);
        } catch (Exception e) {
            log.error("WebSocket 推送系统消息通知异常：{}", e.getMessage(), e);
            throw new RuntimeException("WebSocket 推送失败", e);
        }
    }
}