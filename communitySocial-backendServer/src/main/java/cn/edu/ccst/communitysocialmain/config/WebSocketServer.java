package cn.edu.ccst.communitysocialmain.config;

import cn.edu.ccst.communitysocialmain.entity.PrivateMessage;
import cn.edu.ccst.communitysocialmain.mapper.UserMapper;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.service.MessageService;
import cn.edu.ccst.communitysocialmain.service.RedisMessageQueueService;
import cn.edu.ccst.communitysocialmain.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务器配置
 * 用于实现消息的实时推送
 */
@Slf4j
@Component
@ServerEndpoint("/ws/message")
public class WebSocketServer {

    /**
     * 存储在线用户的 WebSocket 连接
     * key: userId, value: Session
     */
    private static final ConcurrentHashMap<String, Session> USER_SESSIONS = new ConcurrentHashMap<>();

    /**
     * 当前连接数
     */
    private static volatile int connectionCount = 0;

    /**
     * 连接建立成功调用
     *
     * @param session 会话连接
     */
    @OnOpen
    public void onOpen(Session session) {
        String userId = getUserIdFromSession(session);
        
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("WebSocket 连接失败：userId 为空");
            closeSession(session);
            return;
        }

        Session oldSession = USER_SESSIONS.get(userId);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                oldSession.close();
                log.info("用户 {} 的旧连接已关闭", userId);
            } catch (IOException e) {
                log.error("关闭旧连接失败：{}", e.getMessage());
            }
        }

        // 存储用户会话
        USER_SESSIONS.put(userId, session);
        incrementConnectionCount();

        log.info("✓ 用户 {} 连接到 WebSocket 服务器，当前在线人数：{}", userId, connectionCount);
        
        // 更新 Redis 中的在线状态
        try {
            RedisMessageQueueService redisMessageQueueService = SpringContextUtil.getBean(RedisMessageQueueService.class);
            redisMessageQueueService.setUserOnline(userId);
            
            pushOfflineMessages(userId);
        } catch (Exception e) {
            log.error("处理用户上线逻辑失败：userId={}, error={}", userId, e.getMessage(), e);
        }

    }

    /**
     * 连接关闭调用
     *
     * @param session 会话连接
     */
    @OnClose
    public void onClose(Session session) {
        String userId = getUserIdFromSession(session);
        if (userId != null && !userId.trim().isEmpty()) {
            USER_SESSIONS.remove(userId);
            decrementConnectionCount();
            log.info("用户 {} 断开 WebSocket 连接，当前在线人数：{}", userId, connectionCount);
            
            try {
                RedisMessageQueueService redisMessageQueueService = SpringContextUtil.getBean(RedisMessageQueueService.class);
                redisMessageQueueService.setUserOffline(userId);
            } catch (Exception e) {
                log.error("更新用户离线状态失败：userId={}, error={}", userId, e.getMessage(), e);
            }
        }
    }

    /**
     * 收到客户端消息后调用
     *
     * @param session 会话连接
     * @param message 客户端发送的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        String userId = getUserIdFromSession(session);
        
        // 处理空消息，防止 NPE
        if (message == null || message.trim().isEmpty()) {
            log.debug("收到空消息，忽略");
            return;
        }
        
        log.info("收到用户 {} 的消息：{}", userId, message);

        // 处理心跳（兼容 JSON 格式和纯文本）
        if (message.contains("heartbeat")) {
            sendMessageToUser(userId, buildMessage("pong", "心跳响应", null));
            return;
        }
        
        try {
            log.info("开始处理聊天消息：senderId={}", userId);
            handleChatMessage(userId, message);
            log.info("聊天消息处理完成：senderId={}", userId);
        } catch (Exception e) {
            log.error("处理聊天消息失败：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理聊天消息
     */
    private void handleChatMessage(String senderId, String messageJson) {
        try {
            log.info("解析消息 JSON：{}", messageJson);
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(messageJson);
            String toUserId = json.getString("toUserId");
            String type = json.getString("type");
            String content = json.getString("content");
            
            log.info("消息详情：senderId={}, toUserId={}, type={}, content={}", senderId, toUserId, type, content);
            
            if (toUserId == null || content == null) {
                log.warn("消息格式错误：toUserId 或 content 为空");
                return;
            }
            
            // 构造 MessageSendDTO
            MessageService.MessageSendDTO messageSendDTO = new MessageService.MessageSendDTO();
            messageSendDTO.setReceiverId(Long.parseLong(toUserId));
            messageSendDTO.setContent(content);
            messageSendDTO.setMessageType("image".equals(type) ? 2 : 1);
            
            log.info("准备保存消息到数据库...");
            // 通过 SpringContextUtil 获取 MessageService
            MessageService messageService = SpringContextUtil.getBean(MessageService.class);
            
            // 调用 Service 保存消息到数据库
            PrivateMessage savedMessage = messageService.sendMessage(Long.parseLong(senderId), messageSendDTO);
            
            log.info("✓ 消息已保存：messageId={}, senderId={}, receiverId={}", 
                    savedMessage.getMessageId(), senderId, toUserId);
            
            // 构建推送给接收方的消息数据
            com.alibaba.fastjson.JSONObject pushData = new com.alibaba.fastjson.JSONObject();
            pushData.put("messageId", savedMessage.getMessageId());
            pushData.put("senderId", senderId);
            pushData.put("receiverId", toUserId);
            pushData.put("content", content);
            pushData.put("messageType", savedMessage.getMessageType());
            pushData.put("sendTime", savedMessage.getSendTime().toString());
            
            // 获取发送者信息
            UserMapper userMapper = SpringContextUtil.getBean(UserMapper.class);
            User sender = userMapper.selectById(Long.parseLong(senderId));
            if (sender != null) {
                pushData.put("senderNickname", sender.getNickname());
                pushData.put("senderAvatarUrl", sender.getAvatarUrl());
            }
            
            // 推送给接收方
            sendNewMessageNotification(toUserId, pushData.toJSONString());

            
        } catch (Exception e) {
            log.error("处理聊天消息异常：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 发生错误时调用
     *
     * @param session 会话连接
     * @param error   发生的错误
     */
    @OnError
    public void onError(Session session, Throwable error) {
        String userId = getUserIdFromSession(session);
        
        // EOFException 是微信小程序断开连接的正常现象，降低日志级别为 DEBUG
        if (error instanceof java.io.EOFException) {
            log.debug("WebSocket 连接关闭 - 用户：{} (EOFException)", userId);
        } else {
            log.error("WebSocket 连接错误 - 用户：{}, 错误：{}", userId, error.getMessage(), error);
        }
        
        if (userId != null && !userId.trim().isEmpty()) {
            USER_SESSIONS.remove(userId);
            decrementConnectionCount();
        }
    }

    /**
     * 从 Session 中获取 userId
     *
     * @param session WebSocket 会话
     * @return userId
     */
    private String getUserIdFromSession(Session session) {
        try {
            String queryString = session.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                String[] params = queryString.split("&");
                for (String param : params) {
                    if (param.startsWith("userId=")) {
                        return param.substring(7);
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取 userId 失败：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 向指定用户发送消息
     *
     * @param userId  接收消息的用户 ID
     * @param message 要发送的消息内容（JSON 格式）
     */
    public static void sendMessageToUser(String userId, String message) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("发送消息失败：userId 为空");
            return;
        }

        Session session = USER_SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                log.debug("消息已发送给用户 {}: {}", userId, message);
            } catch (IOException e) {
                log.error("发送消息给用户 {} 失败：{}", userId, e.getMessage());
                USER_SESSIONS.remove(userId);
                decrementConnectionCount();
            }
        } else {
            log.warn("用户 {} 不在线或连接已关闭", userId);
        }
    }

    /**
     * 向指定用户发送新消息通知
     *
     * @param receiverId 接收者用户 ID
     * @param messageData 消息数据（JSON 格式）
     */
    public static void sendNewMessageNotification(String receiverId, String messageData) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            log.warn("发送新消息通知失败：receiverId 为空");
            return;
        }

        String message = buildMessage("new_message", "新消息通知", messageData);
        sendMessageToUser(receiverId, message);
        
        try {
            RedisMessageQueueService redisMessageQueueService = SpringContextUtil.getBean(RedisMessageQueueService.class);
            
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(messageData);
            PrivateMessage offlineMessage = new PrivateMessage();
            offlineMessage.setMessageId(json.getLong("messageId"));
            offlineMessage.setSenderId(json.getLong("senderId"));
            offlineMessage.setReceiverId(json.getLong("receiverId"));
            offlineMessage.setContent(json.getString("content"));
            offlineMessage.setMessageType(json.getInteger("messageType"));
            
            redisMessageQueueService.storeOfflineMessage(receiverId, offlineMessage);
            
            log.debug("消息已同步到 Redis 离线队列：receiverId={}, messageId={}", 
                    receiverId, offlineMessage.getMessageId());
        } catch (Exception e) {
            log.error("同步消息到 Redis 离线队列失败：receiverId={}, error={}", 
                    receiverId, e.getMessage(), e);
            // 不影响主流程，只记录错误
        }
    }
    
    /**
     * 向指定用户发送系统通知
     *
     * @param userId 用户 ID
     * @param title 通知标题
     * @param content 通知内容
     */
    public static void sendSystemNotification(String userId, String title, String content) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("发送系统通知失败：userId 为空");
            return;
        }
        
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("title", title);
        data.put("content", content);
        data.put("timestamp", System.currentTimeMillis());
        
        String jsonData = com.alibaba.fastjson.JSON.toJSONString(data);
        String message = buildMessage("system_notification", title, jsonData);
        sendMessageToUser(userId, message);
    }
    
    /**
     * 向指定用户发送帖子相关通知
     *
     * @param userId 用户 ID
     * @param postId 帖子 ID
     * @param action 操作类型（like-点赞, comment-评论, collect-收藏）
     * @param operatorNickname 操作人昵称
     */
    public static void sendPostNotification(String userId, String postId, String action, String operatorNickname) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("发送帖子通知失败：userId 为空");
            return;
        }
        
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("postId", postId);
        data.put("action", action);
        data.put("operatorNickname", operatorNickname);
        data.put("timestamp", System.currentTimeMillis());
        
        String title = "";
        switch (action) {
            case "like":
                title = operatorNickname + " 赞了你的帖子";
                break;
            case "comment":
                title = operatorNickname + " 评论了你的帖子";
                break;
            case "collect":
                title = operatorNickname + " 收藏了你的帖子";
                break;
            default:
                title = "帖子动态";
        }
        
        String jsonData = com.alibaba.fastjson.JSON.toJSONString(data);
        String message = buildMessage("post_notification", title, jsonData);
        sendMessageToUser(userId, message);
    }
    
    /**
     * 向指定用户发送举报处理结果通知
     *
     * @param userId 用户 ID
     * @param reportId 举报 ID
     * @param status 处理状态（1-通过, 2-驳回）
     * @param reason 处理原因
     */
    public static void sendReportNotification(String userId, String reportId, Integer status, String reason) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("发送举报通知失败：userId 为空");
            return;
        }
        
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("reportId", reportId);
        data.put("status", status);
        data.put("reason", reason);
        data.put("timestamp", System.currentTimeMillis());
        
        String title = status == 1 ? "举报处理通过" : "举报处理驳回";
        String jsonData = com.alibaba.fastjson.JSON.toJSONString(data);
        String message = buildMessage("report_notification", title, jsonData);
        sendMessageToUser(userId, message);
    }

    /**
     * 广播消息给所有在线用户
     *
     * @param message 要广播的消息内容
     */
    public static void broadcastMessage(String message) {
        for (String userId : USER_SESSIONS.keySet()) {
            sendMessageToUser(userId, message);
        }
    }

    /**
     * 构建消息 JSON
     *
     * @param type    消息类型
     * @param title   消息标题
     * @param data    消息数据
     * @return JSON 格式的消息字符串
     */
    private static String buildMessage(String type, String title, String data) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"type\":\"").append(type).append("\",");
        json.append("\"title\":\"").append(title).append("\",");
        if (data != null) {
            json.append("\"data\":").append(data);
        } else {
            json.append("\"data\":null");
        }
        json.append("}");
        return json.toString();
    }

    /**
     * 关闭会话
     *
     * @param session 会话连接
     */
    private void closeSession(Session session) {
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                log.error("关闭 WebSocket 连接失败：{}", e.getMessage());
            }
        }
    }

    /**
     * 增加连接数（线程安全）
     */
    private synchronized void incrementConnectionCount() {
        connectionCount++;
    }

    /**
     * 减少连接数（线程安全）
     */
    private static synchronized void decrementConnectionCount() {
        if (connectionCount > 0) {
            connectionCount--;
        }
    }

    /**
     * 获取在线用户数量
     *
     * @return 在线用户数量
     */
    public static int getOnlineUserCount() {
        return connectionCount;
    }

    /**
     * 判断用户是否在线
     *
     * @param userId 用户 ID
     * @return true-在线，false-离线
     */
    public static boolean isUserOnline(String userId) {
        return userId != null && USER_SESSIONS.containsKey(userId);
    }

    /**
     * 获取所有在线用户 ID
     *
     * @return 在线用户 ID 集合
     */
    public static java.util.Set<String> getOnlineUserIds() {
        return USER_SESSIONS.keySet();
    }
    
    /**
     * 推送用户的离线消息
     *
     * @param userId 用户 ID
     */
    private void pushOfflineMessages(String userId) {
        try {
            RedisMessageQueueService redisMessageQueueService = SpringContextUtil.getBean(RedisMessageQueueService.class);
            
            // 获取离线消息列表
            List<PrivateMessage> offlineMessages = redisMessageQueueService.getOfflineMessages(userId);
            
            if (offlineMessages == null || offlineMessages.isEmpty()) {
                log.debug("用户 {} 没有离线消息", userId);
                return;
            }
            
            log.info("开始推送离线消息：userId={}, count={}", userId, offlineMessages.size());
            
            // 逐个推送离线消息
            int successCount = 0;
            List<Long> deliveredMessageIds = new java.util.ArrayList<>();
            
            for (PrivateMessage message : offlineMessages) {
                try {
                    // 构建消息数据
                    com.alibaba.fastjson.JSONObject pushData = new com.alibaba.fastjson.JSONObject();
                    pushData.put("messageId", message.getMessageId());
                    pushData.put("senderId", message.getSenderId());
                    pushData.put("receiverId", message.getReceiverId());
                    pushData.put("content", message.getContent());
                    pushData.put("messageType", message.getMessageType());
                    pushData.put("sendTime", message.getSendTime().toString());
                    pushData.put("isOfflineMessage", true); // 标记为离线消息
                    
                    // 获取发送者信息
                    UserMapper userMapper = SpringContextUtil.getBean(UserMapper.class);
                    User sender = userMapper.selectById(message.getSenderId());
                    if (sender != null) {
                        pushData.put("senderNickname", sender.getNickname());
                        pushData.put("senderAvatarUrl", sender.getAvatarUrl());
                    }
                    
                    String messageJson = pushData.toJSONString();
                    String notificationMessage = buildMessage("new_message", "离线消息", messageJson);
                    
                    // 发送消息
                    sendMessageToUser(userId, notificationMessage);
                    
                    successCount++;
                    deliveredMessageIds.add(message.getMessageId());
                    
                    // 每条消息间隔 100ms，避免前端处理不过来
                    Thread.sleep(100);
                } catch (Exception e) {
                    log.error("推送单条离线消息失败：userId={}, messageId={}, error={}", 
                            userId, message.getMessageId(), e.getMessage(), e);
                }
            }
            
            // 删除已推送的消息
            if (!deliveredMessageIds.isEmpty()) {
                redisMessageQueueService.removeDeliveredMessages(userId, deliveredMessageIds);
            }
            
            log.info("离线消息推送完成：userId={}, total={}, success={}", 
                    userId, offlineMessages.size(), successCount);
        } catch (Exception e) {
            log.error("推送离线消息失败：userId={}, error={}", userId, e.getMessage(), e);
        }
    }
}
