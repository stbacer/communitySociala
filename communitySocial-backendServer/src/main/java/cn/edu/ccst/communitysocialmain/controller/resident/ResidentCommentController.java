package cn.edu.ccst.communitysocialmain.controller.resident;

import cn.edu.ccst.communitysocialmain.dto.CommentCreateDTO;
import cn.edu.ccst.communitysocialmain.service.CommentService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.vo.CommentVO;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 居民端评论控制器
 */
@Slf4j
@RestController
@RequestMapping("/resident/comment")
public class ResidentCommentController {
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 发布评论
     */
    @PostMapping("/create")
    public ResultVO<CommentVO> createComment(@Valid @RequestBody CommentCreateDTO commentCreateDTO,
                                           HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        CommentVO comment = commentService.createComment(userId, commentCreateDTO);
        return ResultVO.success("评论成功", comment);
    }
    
    /**
     * 获取帖子评论列表
     */
    @GetMapping("/post/{postId}")
    public ResultVO<PageVO<CommentVO>> getCommentsByPostId(@PathVariable Long postId,
                                                         @RequestParam(defaultValue = "1") Integer page,
                                                         @RequestParam(defaultValue = "10") Integer size,
                                                         HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        PageVO<CommentVO> comments = commentService.getCommentsByPostId(postId, page, size, currentUserId);
        return ResultVO.success(comments);
    }
    
    /**
     * 删除自己的评论
     */
    @DeleteMapping("/{commentId}")
    public ResultVO<Void> deleteComment(@PathVariable Long commentId,
                                      HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        commentService.deleteComment(commentId, userId);
        return ResultVO.success("删除成功", null);
    }
    
    /**
     * 点赞评论
     */
    @PostMapping("/like/{commentId}")
    public ResultVO<Void> likeComment(@PathVariable Long commentId,
                                    HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        commentService.likeComment(userId, commentId);
        return ResultVO.success("点赞成功", null);
    }
    
    /**
     * 取消点赞评论
     */
    @DeleteMapping("/like/{commentId}")
    public ResultVO<Void> unlikeComment(@PathVariable Long commentId,
                                      HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        commentService.unlikeComment(userId, commentId);
        return ResultVO.success("取消点赞成功", null);
    }
    
    /**
     * 获取用户的评论列表
     */
    @GetMapping("/user/{userId}")
    public ResultVO<PageVO<CommentVO>> getUserComments(@PathVariable Long userId,
                                                      @RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "10") Integer size,
                                                      HttpServletRequest request) {
        try {
            log.info("获取用户{}的评论列表，页码{}，每页数量{}", userId, page, size);
            
            // 调用 Service 获取评论列表
            PageVO<CommentVO> comments = commentService.getCommentsByUserId(userId, page, size);
            
            log.info("获取评论列表成功，共{}条", comments.getTotal());
            return ResultVO.success("获取评论列表成功", comments);
        } catch (Exception e) {
            log.error("获取评论列表失败", e);
            return ResultVO.error("获取评论列表失败：" + e.getMessage());
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