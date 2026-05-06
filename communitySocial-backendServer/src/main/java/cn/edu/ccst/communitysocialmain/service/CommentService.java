package cn.edu.ccst.communitysocialmain.service;

import cn.edu.ccst.communitysocialmain.dto.CommentCreateDTO;
import cn.edu.ccst.communitysocialmain.entity.Comment;
import cn.edu.ccst.communitysocialmain.vo.CommentDetailVO;
import cn.edu.ccst.communitysocialmain.vo.CommentVO;
import cn.edu.ccst.communitysocialmain.vo.PageVO;

/**
 * 评论服务接口
 */
public interface CommentService {
    
    /**
     * 发布评论
     */
    CommentVO createComment(Long userId, CommentCreateDTO commentCreateDTO);
    
    /**
     * 根据帖子ID获取评论列表
     */
    PageVO<CommentVO> getCommentsByPostId(Long postId, Integer page, Integer size, Long currentUserId);
    
    /**
     * 根据用户 ID 获取评论列表
     */
    PageVO<CommentVO> getCommentsByUserId(Long userId, Integer page, Integer size);
        
    /**
     * 获取所有评论（管理员使用）
     */
    PageVO<CommentDetailVO> getAllComments(Integer page, Integer size, String keyword);
    
    /**
     * 删除评论
     */
    void deleteComment(Long commentId, Long userId);
    
    /**
     * 点赞评论
     */
    void likeComment(Long userId, Long commentId);
    
    /**
     * 取消点赞评论
     */
    void unlikeComment(Long userId, Long commentId);
    
    /**
     * 获取评论详情
     */
    CommentVO getCommentDetail(Long commentId, Long currentUserId);
    
    /**
     * 生成评论ID
     */
    Long generateCommentId();
}