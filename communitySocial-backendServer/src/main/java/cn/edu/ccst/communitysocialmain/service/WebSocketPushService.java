package cn.edu.ccst.communitysocialmain.service;

import cn.edu.ccst.communitysocialmain.config.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * WebSocket 消息推送服务
 * 封装 WebSocket 推送功能，提供便捷的消息推送接口
 */
@Slf4j
@Service
public class WebSocketPushService {
    
    /**
     * 推送新私信消息通知
     * 
     * @param receiverId 接收者用户 ID
     * @param messageData 消息数据（JSON 格式）
     */
    public void pushNewMessage(String receiverId, String messageData) {
        try {
            WebSocketServer.sendNewMessageNotification(receiverId, messageData);
            log.debug("推送新消息通知成功：receiverId={}", receiverId);
        } catch (Exception e) {
            log.error("推送新消息通知失败：receiverId={}, error={}", receiverId, e.getMessage(), e);
        }
    }
    
    /**
     * 推送系统通知
     * 
     * @param userId 用户 ID
     * @param title 通知标题
     * @param content 通知内容
     */
    public void pushSystemNotification(String userId, String title, String content) {
        try {
            WebSocketServer.sendSystemNotification(userId, title, content);
            log.debug("推送系统通知成功：userId={}, title={}", userId, title);
        } catch (Exception e) {
            log.error("推送系统通知失败：userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 推送帖子互动通知
     * 
     * @param userId 用户 ID
     * @param postId 帖子 ID
     * @param action 操作类型（like-点赞, comment-评论, collect-收藏）
     * @param operatorNickname 操作人昵称
     */
    public void pushPostNotification(String userId, String postId, String action, String operatorNickname) {
        try {
            WebSocketServer.sendPostNotification(userId, postId, action, operatorNickname);
            log.debug("推送帖子通知成功：userId={}, action={}", userId, action);
        } catch (Exception e) {
            log.error("推送帖子通知失败：userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 推送举报处理结果通知
     * 
     * @param userId 用户 ID
     * @param reportId 举报 ID
     * @param status 处理状态（1-通过, 2-驳回）
     * @param reason 处理原因
     */
    public void pushReportNotification(String userId, String reportId, Integer status, String reason) {
        try {
            WebSocketServer.sendReportNotification(userId, reportId, status, reason);
            log.debug("推送举报通知成功：userId={}, status={}", userId, status);
        } catch (Exception e) {
            log.error("推送举报通知失败：userId={}, error={}", userId, e.getMessage(), e);
        }
    }
    
    /**
     * 批量推送系统通知
     * 
     * @param userIds 用户 ID 列表
     * @param title 通知标题
     * @param content 通知内容
     */
    public void batchPushSystemNotification(java.util.List<String> userIds, String title, String content) {
        if (userIds == null || userIds.isEmpty()) {
            log.warn("批量推送系统通知失败：用户列表为空");
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (String userId : userIds) {
            try {
                WebSocketServer.sendSystemNotification(userId, title, content);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("批量推送系统通知失败：userId={}, error={}", userId, e.getMessage());
            }
        }
        
        log.info("批量推送系统通知完成：总数={}, 成功={}, 失败={}", userIds.size(), successCount, failCount);
    }
    
    /**
     * 检查用户是否在线
     * 
     * @param userId 用户 ID
     * @return true-在线，false-离线
     */
    public boolean isUserOnline(String userId) {
        return WebSocketServer.isUserOnline(userId);
    }
    
    /**
     * 获取在线用户数量
     * 
     * @return 在线用户数量
     */
    public int getOnlineUserCount() {
        return WebSocketServer.getOnlineUserCount();
    }
}
