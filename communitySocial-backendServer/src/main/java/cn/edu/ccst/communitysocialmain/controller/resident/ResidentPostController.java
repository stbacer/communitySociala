package cn.edu.ccst.communitysocialmain.controller.resident;

import cn.edu.ccst.communitysocialmain.dto.PostCreateDTO;
import cn.edu.ccst.communitysocialmain.entity.Post;
import cn.edu.ccst.communitysocialmain.mapper.PostMapper;
import cn.edu.ccst.communitysocialmain.service.MessageService;
import cn.edu.ccst.communitysocialmain.service.PostService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.PostDetailVO;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 居民端帖子控制器
 */
@Slf4j
@RestController
@RequestMapping("/resident/post")
public class ResidentPostController {
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private PostMapper postMapper;
    
    /**
     * 发布帖子
     */
    @PostMapping("/create")
    public ResultVO<PostDetailVO> createPost(@Valid @RequestBody PostCreateDTO postCreateDTO,
                                           HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        PostDetailVO post = postService.createPost(userId, postCreateDTO);
        
        // 发送系统消息通知用户
        String messageContent = String.format("您的帖子《%s》已发布成功，正在等待审核。", post.getTitle());
        messageService.sendSystemMessage(userId, messageContent, 1);
        
        return ResultVO.success("发布成功", post);
    }
    
    /**
     * 更新帖子
     */
    @PutMapping("/update")
    public ResultVO<PostDetailVO> updatePost(@Valid @RequestBody PostCreateDTO postUpdateDTO,
                                           HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        PostDetailVO post = postService.updatePost(userId, postUpdateDTO);
        return ResultVO.success("更新成功", post);
    }
    
    /**
     * 获取帖子详情
     */
    @GetMapping("/{postId}")
    public ResultVO<PostDetailVO> getPostDetail(@PathVariable Long postId,
                                              HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        PostDetailVO post = postService.getPostDetail(postId, userId);
        return ResultVO.success(post);
    }
    
    /**
     * 根据用户 ID 获取帖子列表
     */
    @GetMapping("/user/{userId}")
    public ResultVO<PageVO<PostDetailVO>> getPostsByUserId(@PathVariable Long userId,
                                                         @RequestParam(defaultValue = "1") Integer page,
                                                         @RequestParam(defaultValue = "10") Integer size,
                                                         HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        PageVO<PostDetailVO> posts = postService.getPostsByUserId(userId, page, size, currentUserId);
        return ResultVO.success(posts);
    }
        
    /**
     * 获取当前登录用户的帖子列表
     * 用于"我的帖子"页面
     */
    @GetMapping("/my-posts")
    public ResultVO<PageVO<PostDetailVO>> getMyPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
                
            // 查询当前用户的所有帖子（status=1 待审核 + status=2 已发布）
            int offset = (page - 1) * size;
            List<Post> posts = postMapper.selectByUserIdForOwner(currentUserId, offset, size);
            Long total = postMapper.countByUserIdForOwner(currentUserId);
                
            // 转换为 PostDetailVO
            List<PostDetailVO> postVOs = posts.stream()
                    .map(post -> {
                        try {
                            return postService.getPostDetail(post.getPostId(), currentUserId);
                        } catch (Exception e) {
                            log.error("获取帖子详情失败：{}", post.getPostId(), e);
                            return null;
                        }
                    })
                    .filter(vo -> vo != null)
                    .collect(Collectors.toList());
                
            return ResultVO.success(new PageVO<>(page, size, total, postVOs));
        } catch (Exception e) {
            log.error("获取我的帖子列表失败", e);
            return ResultVO.error("获取帖子列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据类型获取帖子列表
     */
    @GetMapping("/type/{type}")
    public ResultVO<PageVO<PostDetailVO>> getPostsByType(@PathVariable Integer type,
                                                       @RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "10") Integer size,
                                                       HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        PageVO<PostDetailVO> posts = postService.getPostsByType(type, page, size, currentUserId);
        return ResultVO.success(posts);
    }
    
    /**
     * 搜索帖子
     */
    @GetMapping("/search")
    public ResultVO<PageVO<PostDetailVO>> searchPosts(@RequestParam String keyword,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer size,
                                                    HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        PageVO<PostDetailVO> posts = postService.searchPosts(keyword, page, size, currentUserId);
        return ResultVO.success(posts);
    }
    
    /**
     * 获取附近帖子
     */
    @GetMapping("/nearby")
    public ResultVO<PageVO<PostDetailVO>> getNearbyPosts(@RequestParam Double longitude,
                                                       @RequestParam Double latitude,
                                                       @RequestParam(defaultValue = "5.0") Double radius,
                                                       @RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "10") Integer size,
                                                       HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        // 调用正确的 Service 方法：getPostsByNearby
        PageVO<PostDetailVO> posts = postService.getPostsByNearby(longitude, latitude, radius, page, size, null, null, null, currentUserId);
        return ResultVO.success(posts);
    }
    
    /**
     * 获取热门帖子
     */
    @GetMapping("/hot")
    public ResultVO<PageVO<PostDetailVO>> getHotPosts(@RequestParam(defaultValue = "7") Integer days,
                                                    @RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer size,
                                                    HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        // 调用正确的 Service 方法：getPostsByHot
        PageVO<PostDetailVO> posts = postService.getPostsByHot(page, size, null, null, null, currentUserId);
        return ResultVO.success(posts);
    }
    
    /**
     * 删除自己的帖子
     */
    @DeleteMapping("/{postId}")
    public ResultVO<Void> deletePost(@PathVariable Long postId,
                                   HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        postService.deletePost(postId, userId);
        return ResultVO.success("删除成功", null);
    }
    
    /**
     * 获取帖子列表
     */
    @GetMapping("/list")
    public ResultVO<PageVO<PostDetailVO>> getPostList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        
        try {
            // 尝试获取当前用户ID（可选）
            Long currentUserId = null;
            try {
                currentUserId = getCurrentUserId(request);
            } catch (Exception e) {
                // 用户未登录，不影响获取帖子列表
                log.debug("用户未登录，以游客身份获取帖子列表");
            }
            
            // 构建查询条件
            PageVO<PostDetailVO> posts = postService.getPostList(page, size, type, categoryId, keyword, currentUserId);
            return ResultVO.success(posts);
        } catch (Exception e) {
            log.error("获取帖子列表失败", e);
            return ResultVO.error("获取帖子列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取按时间排序的帖子列表
     */
    @GetMapping("/list/time")
    public ResultVO<PageVO<PostDetailVO>> getPostsByTime(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        
        try {
            Long currentUserId = null;
            try {
                currentUserId = getCurrentUserId(request);
            } catch (Exception e) {
                log.debug("用户未登录，以游客身份获取时间排序帖子列表");
            }
            
            PageVO<PostDetailVO> posts = postService.getPostsByTime(page, size, type, categoryId, keyword, currentUserId);
            return ResultVO.success(posts);
        } catch (Exception e) {
            log.error("获取时间排序帖子列表失败", e);
            return ResultVO.error("获取时间排序帖子列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取按热度排序的帖子列表
     */
    @GetMapping("/list/hot")
    public ResultVO<PageVO<PostDetailVO>> getPostsByHot(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        
        try {
            Long currentUserId = null;
            try {
                currentUserId = getCurrentUserId(request);
            } catch (Exception e) {
                log.debug("用户未登录，以游客身份获取热度排序帖子列表");
            }
            
            PageVO<PostDetailVO> posts = postService.getPostsByHot(page, size, type, categoryId, keyword, currentUserId);
            return ResultVO.success(posts);
        } catch (Exception e) {
            log.error("获取热度排序帖子列表失败", e);
            return ResultVO.error("获取热度排序帖子列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取附近帖子列表
     */
    @GetMapping("/list/nearby")
    public ResultVO<PageVO<PostDetailVO>> getPostsByNearby(
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam(defaultValue = "5.0") Double radius,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        
        try {
            Long currentUserId = null;
            try {
                currentUserId = getCurrentUserId(request);
            } catch (Exception e) {
                log.debug("用户未登录，以游客身份获取附近帖子列表");
            }
            
            PageVO<PostDetailVO> posts = postService.getPostsByNearby(longitude, latitude, radius, page, size, type, categoryId, keyword, currentUserId);
            return ResultVO.success(posts);
        } catch (Exception e) {
            log.error("获取附近帖子列表失败", e);
            return ResultVO.error("获取附近帖子列表失败: " + e.getMessage());
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