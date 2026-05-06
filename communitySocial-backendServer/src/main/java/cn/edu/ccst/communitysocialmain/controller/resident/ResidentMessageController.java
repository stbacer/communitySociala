package cn.edu.ccst.communitysocialmain.controller.resident;

import cn.edu.ccst.communitysocialmain.dto.ConversationDTO;
import cn.edu.ccst.communitysocialmain.dto.MessageRecordDTO;
import cn.edu.ccst.communitysocialmain.entity.PrivateMessage;
import cn.edu.ccst.communitysocialmain.service.MessageService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 居民端消息控制器
 * 处理私信消息相关功能
 */
@Slf4j
@RestController
@RequestMapping("/resident/message")
public class ResidentMessageController {

    @Autowired
    private MessageService messageService;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取会话列表（包含对方用户信息）
     */
    @GetMapping("/conversations")
    public ResultVO<PageVO<ConversationDTO>> getConversations(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            // 处理 keyword 参数，null、空字符串或"null"都视为无搜索条件
            String searchKeyword = (keyword == null || keyword.trim().isEmpty() || "null".equalsIgnoreCase(keyword.trim())) ? null : keyword.trim();
            log.debug("接收到的 keyword: '{}', 处理后的 searchKeyword: {}", keyword, searchKeyword);
            PageVO<ConversationDTO> conversations = messageService.getConversations(userId, page, size, searchKeyword);
            return ResultVO.success("获取会话列表成功", conversations);
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return ResultVO.error("获取会话列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取消息记录（包含发送方用户信息）
     */
    @GetMapping("/records")
    public ResultVO<PageVO<MessageRecordDTO>> getMessageRecords(
            @RequestParam Long contactId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            PageVO<MessageRecordDTO> messages = messageService.getMessageRecords(userId, contactId, page, size);
            return ResultVO.success("获取消息记录成功", messages);
        } catch (Exception e) {
            log.error("获取消息记录失败", e);
            return ResultVO.error("获取消息记录失败：" + e.getMessage());
        }
    }

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public ResultVO<PrivateMessage> sendMessage(@Valid @RequestBody MessageService.MessageSendDTO messageSendDTO,
                                               HttpServletRequest request) {
        try {
            Long senderId = getCurrentUserId(request);
            PrivateMessage message = messageService.sendMessage(senderId, messageSendDTO);
            return ResultVO.success("发送消息成功", message);
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return ResultVO.error("发送消息失败: " + e.getMessage());
        }
    }

    /**
     * 标记消息为已读
     */
    @PutMapping("/read/{contactId}")
    public ResultVO<Void> markMessageRead(@PathVariable Long contactId,
                                         HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            messageService.markMessagesAsRead(userId, contactId);
            return ResultVO.success("标记已读成功", null);
        } catch (Exception e) {
            log.error("标记已读失败", e);
            return ResultVO.error("标记已读失败: " + e.getMessage());
        }
    }

    /**
     * 获取未读消息数量
     */
    @GetMapping("/unread-count")
    public ResultVO<Long> getUnreadCount(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            Long unreadCount = messageService.getUnreadMessageCount(userId);
            return ResultVO.success("获取未读数量成功", unreadCount);
        } catch (Exception e) {
            log.error("获取未读数量失败", e);
            return ResultVO.error("获取未读数量失败: " + e.getMessage());
        }
    }

    /**
     * 撤回消息
     */
    @DeleteMapping("/recall/{messageId}")
    public ResultVO<Void> recallMessage(@PathVariable Long messageId,
                                       HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            messageService.recallMessage(userId, messageId);
            return ResultVO.success("撤回消息成功", null);
        } catch (Exception e) {
            log.error("撤回消息失败", e);
            return ResultVO.error("撤回消息失败: " + e.getMessage());
        }
    }

    /**
     * 删除消息
     */
    @DeleteMapping("/{messageId}")
    public ResultVO<Void> deleteMessage(@PathVariable Long messageId,
                                       HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            messageService.deleteMessage(userId, messageId);
            return ResultVO.success("删除消息成功", null);
        } catch (Exception e) {
            log.error("删除消息失败", e);
            return ResultVO.error("删除消息失败: " + e.getMessage());
        }
    }

    /**
     * 从请求头中获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return Long.parseLong(jwtUtil.getUserIdFromToken(token));
        }
        throw new RuntimeException("用户未登录");
    }
}