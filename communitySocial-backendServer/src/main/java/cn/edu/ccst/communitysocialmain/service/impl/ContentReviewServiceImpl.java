package cn.edu.ccst.communitysocialmain.service.impl;

import cn.edu.ccst.communitysocialmain.dto.ContentReviewDTO;
import cn.edu.ccst.communitysocialmain.dto.PendingContentQueryDTO;
import cn.edu.ccst.communitysocialmain.entity.Comment;
import cn.edu.ccst.communitysocialmain.entity.Post;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.mapper.CommentMapper;
import cn.edu.ccst.communitysocialmain.mapper.PostMapper;
import cn.edu.ccst.communitysocialmain.mapper.UserMapper;
import cn.edu.ccst.communitysocialmain.service.ContentReviewService;
import cn.edu.ccst.communitysocialmain.service.ContentReviewService;
import cn.edu.ccst.communitysocialmain.service.MessageService;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 内容审核服务实现类
 */
@Slf4j
@Service
public class ContentReviewServiceImpl implements ContentReviewService {
    
    @Autowired
    private PostMapper postMapper;
    
    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private MessageService messageService;
    
    @Override
    public PageVO<ContentReviewService.PendingContentVO> getPendingContentList(PendingContentQueryDTO queryDTO) {
        List<ContentReviewService.PendingContentVO> pendingContents = new ArrayList<>();
        Long total = 0L;
        
        int offset = (queryDTO.getPage() - 1) * queryDTO.getSize();
        
        // 根据内容类型查询不同的待审核内容
        if (StringUtils.hasText(queryDTO.getContentType())) {
            switch (queryDTO.getContentType().toLowerCase()) {
                case "post":
                    pendingContents = getPendingPosts(offset, queryDTO.getSize(), queryDTO.getKeyword());
                    total = postMapper.countByStatus(1); // 1表示待审核
                    break;
                case "comment":
                    pendingContents = getPendingComments(offset, queryDTO.getSize(), queryDTO.getKeyword());
                    total = commentMapper.countByStatus(0); // 0表示待审核
                    break;
                case "user":
                    pendingContents = getPendingUsers(offset, queryDTO.getSize(), queryDTO.getKeyword());
                    total = userMapper.countByCondition(createPendingUserCondition());
                    break;
                default:
                    // 返回所有类型的内容
                    pendingContents = getAllPendingContents(offset, queryDTO.getSize(), queryDTO.getKeyword());
                    total = getTotalPendingCount();
                    break;
            }
        } else {
            // 返回所有类型的内容
            pendingContents = getAllPendingContents(offset, queryDTO.getSize(), queryDTO.getKeyword());
            total = getTotalPendingCount();
        }
        
        return new PageVO<>(queryDTO.getPage(), queryDTO.getSize(), total, pendingContents);
    }
    
    @Override
    @Transactional
    public void reviewContent(String adminUserId, ContentReviewDTO reviewDTO) {
        try {
            switch (reviewDTO.getContentType().toLowerCase()) {
                case "post":
                    reviewPost(adminUserId, reviewDTO);
                    break;
                case "comment":
                    reviewComment(adminUserId, reviewDTO);
                    break;
                case "user":
                    reviewUser(adminUserId, reviewDTO);
                    break;
                default:
                    throw new RuntimeException("不支持的内容类型：" + reviewDTO.getContentType());
            }
            
            log.info("内容审核完成 - 类型: {}, ID: {}, 状态: {}, 操作人: {}", 
                    reviewDTO.getContentType(), reviewDTO.getContentId(), reviewDTO.getStatus(), adminUserId);
                    
        } catch (Exception e) {
            log.error("内容审核失败", e);
            throw new RuntimeException("审核失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void batchReviewContent(String adminUserId, List<ContentReviewDTO> reviewDTOs) {
        if (reviewDTOs == null || reviewDTOs.isEmpty()) {
            return;
        }
        
        int successCount = 0;
        List<String> failedContents = new ArrayList<>();
        
        for (ContentReviewDTO reviewDTO : reviewDTOs) {
            try {
                reviewContent(adminUserId, reviewDTO);
                successCount++;
            } catch (Exception e) {
                failedContents.add(reviewDTO.getContentId() + "(" + reviewDTO.getContentType() + ")");
                log.error("批量审核失败 - 内容ID: {}, 类型: {}", reviewDTO.getContentId(), reviewDTO.getContentType(), e);
            }
        }
        
        log.info("批量审核完成 - 成功: {}, 失败: {}, 失败内容: {}", successCount, failedContents.size(), failedContents);
        
        if (!failedContents.isEmpty()) {
            throw new RuntimeException("部分审核失败，失败内容：" + String.join(", ", failedContents));
        }
    }
    
    @Override
    public ContentReviewService.ContentStatistics getReviewStatistics() {
        ContentReviewService.ContentStatistics statistics = new ContentReviewService.ContentStatistics();
        
        // 统计各类待审核内容数量
        statistics.setPendingPostCount(postMapper.countByStatus(1));
        statistics.setPendingCommentCount(0L); // 暂时设为0，需要实现CommentMapper的对应方法
        statistics.setPendingUserCount(userMapper.countByCondition(createPendingUserCondition()));
        
        // 统计今日已审核数（简化处理，实际应查询操作日志）
        statistics.setTodayReviewedCount(0L);
        
        // 统计总审核数（简化处理）
        statistics.setTotalReviewedCount(0L);
        
        return statistics;
    }
    
    /**
     * 获取待审核帖子
     */
    private List<ContentReviewService.PendingContentVO> getPendingPosts(int offset, int size, String keyword) {
        List<Post> posts = postMapper.selectPendingPosts(offset, size);
        
        return posts.stream()
                .filter(post -> StringUtils.hasText(keyword) ? 
                        (StringUtils.hasText(post.getTitle()) && post.getTitle().contains(keyword)) ||
                        (StringUtils.hasText(post.getContent()) && post.getContent().contains(keyword)) :
                        true)
                .map(this::convertPostToPendingContent)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取待审核评论
     */
    private List<ContentReviewService.PendingContentVO> getPendingComments(int offset, int size, String keyword) {
        // 这里需要在CommentMapper中添加相应的方法
        // 暂时返回空列表
        return new ArrayList<>();
    }
    
    /**
     * 获取待审核用户（实名认证）
     */
    private List<ContentReviewService.PendingContentVO> getPendingUsers(int offset, int size, String keyword) {
        List<User> users = userMapper.selectPendingAuthUsers(offset, size);
        
        return users.stream()
                .filter(user -> StringUtils.hasText(keyword) ? 
                        (StringUtils.hasText(user.getNickname()) && user.getNickname().contains(keyword)) ||
                        (StringUtils.hasText(user.getRealName()) && user.getRealName().contains(keyword)) :
                        true)
                .map(this::convertUserToPendingContent)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有待审核内容
     */
    private List<ContentReviewService.PendingContentVO> getAllPendingContents(int offset, int size, String keyword) {
        List<ContentReviewService.PendingContentVO> allContents = new ArrayList<>();
        
        // 获取待审核帖子
        allContents.addAll(getPendingPosts(0, size, keyword));
        
        // 获取待审核评论
        allContents.addAll(getPendingComments(0, size, keyword));
        
        // 获取待审核用户
        allContents.addAll(getPendingUsers(0, size, keyword));
        
        // 按提交时间排序并分页
        return allContents.stream()
                .sorted((a, b) -> b.getSubmitTime().compareTo(a.getSubmitTime()))
                .skip(offset)
                .limit(size)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取总的待审核内容数量
     */
    private Long getTotalPendingCount() {
        Long postCount = postMapper.countByStatus(1);
        Long commentCount = 0L; // 暂时设为0
        Long userCount = userMapper.countByCondition(createPendingUserCondition());
        return postCount + commentCount + userCount;
    }
    
    /**
     * 创建待审核用户查询条件
     */
    private User createPendingUserCondition() {
        User condition = new User();
        condition.setAuthStatus(1); // 1表示认证中
        return condition;
    }
    
    /**
     * 审核帖子
     */
    private void reviewPost(String adminUserId, ContentReviewDTO reviewDTO) {
        Long contentId = Long.parseLong(reviewDTO.getContentId());
        Post post = postMapper.selectById(contentId);
        if (post == null) {
            throw new RuntimeException("帖子不存在");
        }
        
        // 更新帖子状态
        postMapper.updateStatus(contentId, reviewDTO.getStatus(), adminUserId);
        
        // 发送系统消息通知作者审核结果
        try {
            Long authorId = post.getUserId();
            User author = userMapper.selectById(authorId);
            String statusText = reviewDTO.getStatus() == 2 ? "通过" : "拒绝";
            String messageContent = String.format("您的帖子《%s》审核%s。备注：%s", 
                post.getTitle(), 
                statusText,
                reviewDTO.getRemark() != null ? reviewDTO.getRemark() : "无");
            messageService.sendSystemMessage(authorId, messageContent, 1);
        } catch (Exception e) {
            log.warn("发送帖子审核通知失败：{}", e.getMessage());
        }
        
        // 记录审核日志（可选）
        logAuditLog(adminUserId, "POST_REVIEW", contentId.toString(), reviewDTO.getStatus(), reviewDTO.getRemark());
    }
    
    /**
     * 审核评论
     */
    private void reviewComment(String adminUserId, ContentReviewDTO reviewDTO) {
        Long contentId = Long.parseLong(reviewDTO.getContentId());
        Comment comment = commentMapper.selectById(contentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        
        // 更新评论状态
        commentMapper.updateStatus(contentId, reviewDTO.getStatus());
        
        // 记录审核日志（可选）
        logAuditLog(adminUserId, "COMMENT_REVIEW", contentId.toString(), reviewDTO.getStatus(), reviewDTO.getRemark());
    }
    
    /**
     * 审核用户实名认证
     */
    private void reviewUser(String adminUserId, ContentReviewDTO reviewDTO) {
        Long contentId = Long.parseLong(reviewDTO.getContentId());
        User user = userMapper.selectById(contentId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新用户认证状态
        userMapper.updateAuthStatus(contentId, reviewDTO.getStatus());
        
        // 记录审核日志（可选）
        logAuditLog(adminUserId, "USER_AUTH_REVIEW", contentId.toString(), reviewDTO.getStatus(), reviewDTO.getRemark());
    }
    
    /**
     * 记录审核日志
     */
    private void logAuditLog(String adminUserId, String operation, String targetId, Integer status, String remark) {
        // 这里可以集成操作日志服务
        log.info("审核日志 - 操作人: {}, 操作: {}, 目标: {}, 状态: {}, 备注: {}", 
                adminUserId, operation, targetId, status, remark);
    }
    
    /**
     * 将Post转换为PendingContentVO
     */
    private ContentReviewService.PendingContentVO convertPostToPendingContent(Post post) {
        PendingContentVO vo = new PendingContentVO();
        vo.setContentId(post.getPostId().toString());
        vo.setContentType("post");
        vo.setTitle(post.getTitle());
        vo.setContent(post.getContent());
        vo.setAuthorId(post.getUserId().toString());
        
        // 获取作者昵称
        User author = userMapper.selectById(post.getUserId());
        vo.setAuthorName(author != null ? author.getNickname() : "未知用户");
        
        vo.setSubmitTime(post.getPublishTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        vo.setStatus(post.getStatus());
        
        // 设置图片URL列表
        vo.setImageUrls(post.getImageUrls());
        
        return vo;
    }
    
    /**
     * 将User转换为PendingContentVO
     */
    private ContentReviewService.PendingContentVO convertUserToPendingContent(User user) {
        PendingContentVO vo = new PendingContentVO();
        vo.setContentId(user.getUserId().toString());
        vo.setContentType("user");
        vo.setTitle("实名认证申请");
        vo.setContent("真实姓名：" + user.getRealName() + "，身份证号：" + user.getIdCard());
        vo.setAuthorId(user.getUserId().toString());
        vo.setAuthorName(user.getNickname());
        vo.setSubmitTime(user.getUpdateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        vo.setStatus(user.getAuthStatus());
        
        return vo;
    }
}