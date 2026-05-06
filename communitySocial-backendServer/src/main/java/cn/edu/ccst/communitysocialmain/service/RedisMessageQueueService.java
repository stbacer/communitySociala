package cn.edu.ccst.communitysocialmain.service;

import cn.edu.ccst.communitysocialmain.entity.PrivateMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 消息队列服务
 * 用于处理 WebSocket 离线消息存储和推送
 */
@Slf4j
@Service
public class RedisMessageQueueService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // Redis Key 前缀
    private static final String OFFLINE_MESSAGE_QUEUE_PREFIX = "offline:message:queue:";
    private static final String USER_ONLINE_STATUS_PREFIX = "user:online:";
    private static final String MESSAGE_RETRY_QUEUE_PREFIX = "message:retry:";
    
    // 离线消息过期时间（7天）
    private static final long OFFLINE_MESSAGE_EXPIRE_DAYS = 7;
    
    /**
     * 将消息存入用户的离线消息队列
     * 
     * @param userId 用户ID
     * @param message 消息对象
     */
    public void storeOfflineMessage(String userId, PrivateMessage message) {
        try {
            String queueKey = OFFLINE_MESSAGE_QUEUE_PREFIX + userId;
            
            // 将消息添加到队列尾部
            redisTemplate.opsForList().rightPush(queueKey, message);
            
            // 设置过期时间（7天）
            redisTemplate.expire(queueKey, OFFLINE_MESSAGE_EXPIRE_DAYS, TimeUnit.DAYS);
            
            log.info("消息已存入离线队列：userId={}, messageId={}", userId, message.getMessageId());
        } catch (Exception e) {
            log.error("存储离线消息失败：userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取用户的离线消息列表
     * 
     * @param userId 用户ID
     * @return 离线消息列表
     */
    public List<PrivateMessage> getOfflineMessages(String userId) {
        try {
            String queueKey = OFFLINE_MESSAGE_QUEUE_PREFIX + userId;
            
            // 获取队列中的所有消息
            Long size = redisTemplate.opsForList().size(queueKey);
            if (size == null || size == 0) {
                return new ArrayList<>();
            }
            
            // 取出所有消息（从头部开始，一次性取出）
            List<Object> messages = redisTemplate.opsForList().range(queueKey, 0, -1);
            
            if (messages == null || messages.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 转换为 PrivateMessage 列表
            List<PrivateMessage> result = new ArrayList<>();
            for (Object obj : messages) {
                if (obj instanceof PrivateMessage) {
                    result.add((PrivateMessage) obj);
                }
            }
            
            log.info("获取离线消息成功：userId={}, count={}", userId, result.size());
            return result;
        } catch (Exception e) {
            log.error("获取离线消息失败：userId={}, error={}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 清除用户的离线消息队列
     * 
     * @param userId 用户ID
     */
    public void clearOfflineMessages(String userId) {
        try {
            String queueKey = OFFLINE_MESSAGE_QUEUE_PREFIX + userId;
            redisTemplate.delete(queueKey);
            log.info("已清除用户离线消息队列：userId={}", userId);
        } catch (Exception e) {
            log.error("清除离线消息队列失败：userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 设置用户在线状态
     * 
     * @param userId 用户ID
     */
    public void setUserOnline(String userId) {
        try {
            String statusKey = USER_ONLINE_STATUS_PREFIX + userId;
            redisTemplate.opsForValue().set(statusKey, "online", 30, TimeUnit.MINUTES);
            log.debug("用户在线状态已更新：userId={}", userId);
        } catch (Exception e) {
            log.error("设置用户在线状态失败：userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 清除用户在线状态
     * 
     * @param userId 用户ID
     */
    public void setUserOffline(String userId) {
        try {
            String statusKey = USER_ONLINE_STATUS_PREFIX + userId;
            redisTemplate.delete(statusKey);
            log.debug("用户离线状态已更新：userId={}", userId);
        } catch (Exception e) {
            log.error("清除用户在线状态失败：userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 检查用户是否在线
     * 
     * @param userId 用户ID
     * @return true-在线，false-离线
     */
    public boolean isUserOnline(String userId) {
        try {
            String statusKey = USER_ONLINE_STATUS_PREFIX + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(statusKey));
        } catch (Exception e) {
            log.error("检查用户在线状态失败：userId={}, error={}", userId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 刷新用户在线状态的过期时间
     * 
     * @param userId 用户ID
     */
    public void refreshOnlineStatus(String userId) {
        try {
            String statusKey = USER_ONLINE_STATUS_PREFIX + userId;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(statusKey))) {
                redisTemplate.expire(statusKey, 30, TimeUnit.MINUTES);
            }
        } catch (Exception e) {
            log.error("刷新在线状态失败：userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取离线消息数量
     * 
     * @param userId 用户ID
     * @return 离线消息数量
     */
    public long getOfflineMessageCount(String userId) {
        try {
            String queueKey = OFFLINE_MESSAGE_QUEUE_PREFIX + userId;
            Long size = redisTemplate.opsForList().size(queueKey);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.error("获取离线消息数量失败：userId={}, error={}", userId, e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 批量删除已推送的离线消息
     * 
     * @param userId 用户ID
     * @param messageIds 已推送的消息ID列表
     */
    public void removeDeliveredMessages(String userId, List<Long> messageIds) {
        try {
            String queueKey = OFFLINE_MESSAGE_QUEUE_PREFIX + userId;
            
            if (messageIds == null || messageIds.isEmpty()) {
                return;
            }
            
            // 遍历队列，删除已推送的消息
            List<Object> allMessages = redisTemplate.opsForList().range(queueKey, 0, -1);
            if (allMessages == null || allMessages.isEmpty()) {
                return;
            }
            
            // 重建队列，只保留未推送的消息
            redisTemplate.delete(queueKey);
            
            for (Object obj : allMessages) {
                if (obj instanceof PrivateMessage) {
                    PrivateMessage msg = (PrivateMessage) obj;
                    if (!messageIds.contains(msg.getMessageId())) {
                        redisTemplate.opsForList().rightPush(queueKey, msg);
                    }
                }
            }
            
            // 重新设置过期时间
            if (Boolean.TRUE.equals(redisTemplate.hasKey(queueKey))) {
                redisTemplate.expire(queueKey, OFFLINE_MESSAGE_EXPIRE_DAYS, TimeUnit.DAYS);
            }
            
            log.info("已删除已推送的离线消息：userId={}, count={}", userId, messageIds.size());
        } catch (Exception e) {
            log.error("删除已推送消息失败：userId={}, error={}", userId, e.getMessage(), e);
        }
    }
}
