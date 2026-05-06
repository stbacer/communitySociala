package cn.edu.ccst.communitysocialmain.service.impl;

import cn.edu.ccst.communitysocialmain.dto.CommentCreateDTO;
import cn.edu.ccst.communitysocialmain.entity.Comment;
import cn.edu.ccst.communitysocialmain.entity.OperationLog;
import cn.edu.ccst.communitysocialmain.entity.Post;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.mapper.CommentMapper;
import cn.edu.ccst.communitysocialmain.mapper.PostMapper;
import cn.edu.ccst.communitysocialmain.mapper.UserMapper;
import cn.edu.ccst.communitysocialmain.service.CommentService;
import cn.edu.ccst.communitysocialmain.service.OperationLogService;
import cn.edu.ccst.communitysocialmain.utils.CommonUtil;
import cn.edu.ccst.communitysocialmain.utils.SensitiveWordFilter;
import cn.edu.ccst.communitysocialmain.utils.SnowflakeIdGenerator;
import cn.edu.ccst.communitysocialmain.vo.CommentVO;
import cn.edu.ccst.communitysocialmain.vo.CommentDetailVO;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 */
@Slf4j
@Service
public class CommentServiceImpl implements CommentService {
    
    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private PostMapper postMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Override
    @Transactional
    public CommentVO createComment(Long currentUserId, CommentCreateDTO commentCreateDTO) {
        long startTime = System.currentTimeMillis();
        
        // 检查用户和帖子是否存在
        User user = userMapper.selectById(currentUserId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getAuthStatus() != 2) {
            throw new RuntimeException("请先完成实名认证");
        }
        
        Post post = postMapper.selectById(commentCreateDTO.getPostId());
        if (post == null || post.getStatus() != 2) {
            throw new RuntimeException("帖子不存在或未发布");
        }
        
        // 检查父评论是否存在
        if (commentCreateDTO.getParentId() != null) {
            Comment parentComment = commentMapper.selectById(commentCreateDTO.getParentId());
            if (parentComment == null || parentComment.getStatus() != 1) {
                throw new RuntimeException("父评论不存在");
            }
        }
        
        // 创建评论对象
        Comment comment = new Comment();
        comment.setCommentId(SnowflakeIdGenerator.nextId());
        comment.setPostId(commentCreateDTO.getPostId());
        comment.setUserId(currentUserId);
        comment.setParentId(commentCreateDTO.getParentId());
        comment.setContent(commentCreateDTO.getContent());
        comment.setStatus(1); // 默认已发布
        comment.setLikeCount(0);
        comment.setCreateTime(LocalDateTime.now());
        comment.setCommentTime(LocalDateTime.now()); // 设置评论时间
        
        // 插入评论
        int result = commentMapper.insert(comment);
        if (result <= 0) {
            throw new RuntimeException("发布评论失败");
        }
        
        // 增加帖子评论数
        postMapper.incrementCommentCount(commentCreateDTO.getPostId());
        
        log.info("用户{}评论了帖子{}", currentUserId, commentCreateDTO.getPostId());
        
        // 记录操作日志
        try {
            User commentUser = userMapper.selectById(currentUserId);
            OperationLog log = new OperationLog();
            log.setUserId(currentUserId);
            log.setNickname(commentUser != null ? commentUser.getNickname() : "");
            log.setOperatorName(commentUser != null ? commentUser.getNickname() : "未知用户");
            log.setOperation("CREATE"); // 创建操作
            log.setContent(String.format("%s（id:%s）在帖子%s(id:%s）下发表了评论",
                commentUser != null ? commentUser.getNickname() : "未知用户",
                currentUserId,
                post.getTitle(),
                commentCreateDTO.getPostId()));
            log.setModule("COMMENT");
            log.setSubModule("CREATE");
            log.setClientType(3); // 居民端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(log);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            log.warn("记录评论日志失败：{}", e.getMessage());
        }
        
        return getCommentDetail(comment.getCommentId(), currentUserId);
    }
    
    @Override
    public PageVO<CommentVO> getCommentsByPostId(Long postId, Integer page, Integer size, Long currentUserId) {
        int offset = (page - 1) * size;
        List<Comment> comments = commentMapper.selectByPostId(postId, offset, size);
        Long total = commentMapper.countByPostId(postId);
        
        List<CommentVO> commentVOs = comments.stream()
                .map(comment -> convertToCommentVO(comment, currentUserId))
                .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, commentVOs);
    }
    
    @Override
    public PageVO<CommentVO> getCommentsByUserId(Long userId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Comment> comments = commentMapper.selectByUserId(userId, offset, size);
        Long total = commentMapper.countByUserId(userId);
        
        List<CommentVO> commentVOs = comments.stream()
                .map(comment -> convertToCommentVO(comment, userId))
                .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, commentVOs);
    }
    
    @Override
    public PageVO<CommentDetailVO> getAllComments(Integer page, Integer size, String keyword) {
        int offset = (page - 1) * size;
        List<Comment> comments;
        Long total;
        
        if (StringUtils.hasText(keyword)) {
            comments = commentMapper.selectByKeyword(keyword, offset, size);
            total = commentMapper.countByKeyword(keyword);
        } else {
            comments = commentMapper.selectAll(offset, size);
            total = commentMapper.countAll();
        }
        
        List<CommentDetailVO> commentDetailVOs = comments.stream()
                .map(this::convertToCommentDetailVO)
                .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, commentDetailVOs);
    }
    
    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        long startTime = System.currentTimeMillis();
        
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        
        // 检查权限（只能删除自己的评论或管理员操作）
        if (!comment.getUserId().equals(userId)) {
            User currentUser = userMapper.selectById(userId);
            if (currentUser == null || !"admin".equals(currentUser.getUserRole()) && !"sadmin".equals(currentUser.getUserRole())) {
                throw new RuntimeException("无权限删除此评论");
            }
        }
        
        // 获取帖子信息用于日志记录
        Post post = postMapper.selectById(comment.getPostId());
        
        commentMapper.deleteById(commentId);
        
        // 减少帖子评论数
        postMapper.decrementCommentCount(comment.getPostId());
        
        log.info("用户{}删除了评论{}", userId, commentId);
        
        // 记录操作日志
        try {
            User user = userMapper.selectById(userId);
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setNickname(user.getNickname());
            log.setOperatorName(user != null ? user.getNickname() : "未知用户");
            log.setOperation("DELETE"); // 删除操作
            log.setContent(String.format("%s（id:%s）删除了评论%s(id:%s)",
                user != null ? user.getNickname() : "未知用户",
                userId,
                commentId,
                commentId));
            log.setModule("COMMENT");
            log.setSubModule("DELETE");
            log.setClientType(3); // 居民端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(log);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            log.warn("记录删除评论日志失败：{}", e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void likeComment(Long userId, Long commentId) {
        long startTime = System.currentTimeMillis();
        
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw new RuntimeException("评论不存在");
        }
        
        // 这里应该检查是否已点赞，简化处理
        commentMapper.incrementLikeCount(commentId);
        
        log.info("用户{}点赞了评论{}", userId, commentId);
        
        // 记录操作日志
        try {
            User user = userMapper.selectById(userId);
            Comment targetComment = commentMapper.selectById(commentId);
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setNickname(user.getNickname());
            log.setOperatorName(user != null ? user.getNickname() : "未知用户");
            log.setOperation("LIKE"); // 点赞操作
            log.setContent(String.format("%s（id:%s）点赞了评论%s(id:%s)",
                user != null ? user.getNickname() : "未知用户",
                userId,
                commentId,
                commentId));
            log.setModule("COMMENT");
            log.setSubModule("LIKE");
            log.setClientType(3); // 居民端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(log);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            log.warn("记录评论点赞日志失败：{}", e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void unlikeComment(Long userId, Long commentId) {
        long startTime = System.currentTimeMillis();
        
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw new RuntimeException("评论不存在");
        }
        
        commentMapper.decrementLikeCount(commentId);
        
        log.info("用户{}取消点赞评论{}", userId, commentId);
        
        // 记录操作日志
        try {
            User user = userMapper.selectById(userId);
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setNickname(user.getNickname());
            log.setOperatorName(user != null ? user.getNickname() : "未知用户");
            log.setOperation("UNLIKE"); // 取消点赞操作
            log.setContent(String.format("%s（id:%s）取消了对评论%s(id:%s)的点赞",
                user != null ? user.getNickname() : "未知用户",
                userId,
                commentId,
                commentId));
            log.setModule("COMMENT");
            log.setSubModule("UNLIKE");
            log.setClientType(3); // 居民端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(log);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            log.warn("记录取消评论点赞日志失败：{}", e.getMessage());
        }
    }
    
    @Override
    public CommentVO getCommentDetail(Long commentId, Long currentUserId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            throw new RuntimeException("评论不存在");
        }
        
        return convertToCommentVO(comment, currentUserId);
    }
    
    /**
     * 获取评论详情（管理员使用）
     */
    public CommentDetailVO getCommentDetailForAdmin(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        
        return convertToCommentDetailVO(comment);
    }
    
    @Override
    public Long generateCommentId() {
        return SnowflakeIdGenerator.nextId();
    }
    
    /**
     * 将Comment实体转换为CommentVO
     */
    private CommentVO convertToCommentVO(Comment comment, Long currentUserId) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);
        
        // 设置用户信息
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            UserInfoVO userInfoVO = new UserInfoVO();
            BeanUtils.copyProperties(user, userInfoVO);
            vo.setUserInfo(userInfoVO);
        }
        
        // 设置父评论信息
        if (comment.getParentId() != null) {
            Comment parentComment = commentMapper.selectById(comment.getParentId());
            if (parentComment != null && parentComment.getStatus() == 1) {
                vo.setParentComment(convertToCommentVO(parentComment, currentUserId));
            }
        }
        
        // 设置当前用户是否点赞
        if (currentUserId != null) {
            // 这里应该查询点赞状态，简化处理
            vo.setIsLiked(false);
        }
        
        return vo;
    }
    
    /**
     * 将 Comment 实体转换为 CommentDetailVO（管理员使用）
     */
    private CommentDetailVO convertToCommentDetailVO(Comment comment) {
        CommentDetailVO vo = new CommentDetailVO();
        BeanUtils.copyProperties(comment, vo);
        
        // 设置用户信息
        User user = userMapper.selectById(comment.getUserId());
        if (user != null) {
            UserInfoVO userInfoVO = new UserInfoVO();
            BeanUtils.copyProperties(user, userInfoVO);
            vo.setUserInfo(userInfoVO);
        }
        
        // 设置被评论的帖子标题
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null) {
            vo.setTargetPostTitle(post.getTitle());
        }
        
        return vo;
    }
}