package cn.edu.ccst.communitysocialmain.controller.resident;

import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import cn.edu.ccst.communitysocialmain.entity.LikeRecord;
import cn.edu.ccst.communitysocialmain.entity.UserCollection;
import cn.edu.ccst.communitysocialmain.entity.Follow;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.utils.CommonUtil;
import cn.edu.ccst.communitysocialmain.utils.SnowflakeIdGenerator;
import cn.edu.ccst.communitysocialmain.service.PostService;
import cn.edu.ccst.communitysocialmain.service.UserService;
import cn.edu.ccst.communitysocialmain.mapper.CollectionMapper;
import cn.edu.ccst.communitysocialmain.mapper.FollowMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 居民端互动控制器
 * 处理点赞、收藏等互动功能
 */
@Slf4j
@RestController
@RequestMapping("/resident/interaction")
public class ResidentInteractionController {

    @Autowired
    private PostService postService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private CollectionMapper collectionMapper;
    
    @Autowired
    private FollowMapper followMapper;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 点赞/取消点赞
     */
    @PostMapping("/like")
    public ResultVO<Void> likePost(@Valid @RequestBody LikeRequestDTO likeRequest,
                                  HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            
            // 执行点赞操作
            Long targetId = likeRequest.getTargetId();
            postService.likePost(userId, targetId);
            
            log.info("用户{}点赞帖子{}", userId, targetId);
            return ResultVO.success("点赞成功", null);
        } catch (Exception e) {
            log.error("点赞操作失败", e);
            return ResultVO.error("点赞失败: " + e.getMessage());
        }
    }

    /**
     * 取消点赞
     */
    @DeleteMapping("/like")
    public ResultVO<Void> unlikePost(@Valid @RequestBody LikeRequestDTO likeRequest,
                                    HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            
            // 执行取消点赞操作
            Long targetId = likeRequest.getTargetId();
            postService.unlikePost(userId, targetId);
            
            log.info("用户{}取消点赞帖子{}", userId, targetId);
            return ResultVO.success("取消点赞成功", null);
        } catch (Exception e) {
            log.error("取消点赞操作失败", e);
            return ResultVO.error("取消点赞失败: " + e.getMessage());
        }
    }

    /**
     * 收藏/取消收藏
     */
    @PostMapping("/collect")
    public ResultVO<Void> collectPost(@Valid @RequestBody CollectRequestDTO collectRequest,
                                     HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            
            // 执行收藏操作
            Long targetId = collectRequest.getTargetId();
            postService.collectPost(userId, targetId);
            
            log.info("用户{}收藏帖子{}", userId, targetId);
            return ResultVO.success("收藏成功", null);
        } catch (Exception e) {
            log.error("收藏操作失败", e);
            return ResultVO.error("收藏失败: " + e.getMessage());
        }
    }

    /**
     * 取消收藏
     */
    @DeleteMapping("/collect")
    public ResultVO<Void> uncollectPost(@Valid @RequestBody CollectRequestDTO collectRequest,
                                       HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
                
            // 执行取消收藏操作
            Long targetId = collectRequest.getTargetId();
            postService.uncollectPost(userId, targetId);
                
            log.info("用户{}取消收藏帖子{}", userId, targetId);
            return ResultVO.success("取消收藏成功", null);
        } catch (Exception e) {
            log.error("取消收藏操作失败", e);
            return ResultVO.error("取消收藏失败：" + e.getMessage());
        }
    }
        
    /**
     * 获取用户的收藏列表
     */
    @GetMapping("/collection/user/{userId}")
    public ResultVO<?> getUserCollections(@PathVariable Long userId,
                                         @RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         HttpServletRequest request) {
        try {
            log.info("获取用户{}的收藏列表，页码{}，每页数量{}", userId, page, size);
                
            // 调用 Service 获取收藏列表
            var collectionList = postService.getUserCollections(userId, page, size);
                
            log.info("获取收藏列表成功，共{}条", collectionList.getTotal());
            return ResultVO.success("获取收藏列表成功", collectionList);
        } catch (Exception e) {
            log.error("获取收藏列表失败", e);
            return ResultVO.error("获取收藏列表失败：" + e.getMessage());
        }
    }
        
    /**
     * 取消收藏
     */
    @DeleteMapping("/collection")
    public ResultVO<Void> cancelCollectionByPost(@RequestParam String postId,
                                                 HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            log.info("用户{}取消收藏帖子{}", userId, postId);
            
            // 调用 Service 取消收藏
            Long postIdLong = Long.parseLong(postId);
            postService.uncollectPost(userId, postIdLong);
            
            log.info("取消收藏成功：帖子{}", postId);
            return ResultVO.success("取消收藏成功", null);
        } catch (Exception e) {
            log.error("取消收藏失败", e);
            return ResultVO.error("取消收藏失败：" + e.getMessage());
        }
    }
    
    // ==================== 关注相关接口 ====================
    
    /**
     * 关注用户
     */
    @PostMapping("/follow")
    public ResultVO<Void> followUser(@RequestBody Map<String, String> params,
                                     HttpServletRequest request) {
        try {
            Long followerId = getCurrentUserId(request);
            String followedId = params.get("followedUserId");
            
            if (followedId == null || followedId.equals(followerId)) {
                return ResultVO.error("不能关注自己");
            }
            
            // 检查是否已经关注
            Long followedIdLong = Long.parseLong(followedId);
            Follow existingFollow = followMapper.selectByFollowerAndFollowed(followerId, followedIdLong);
            if (existingFollow != null) {
                return ResultVO.error("已关注该用户");
            }
            
            // 创建关注记录
            Follow follow = new Follow();
            follow.setFollowId(SnowflakeIdGenerator.nextId());
            follow.setFollowerId(followerId);
            follow.setFollowedId(followedIdLong);
            follow.setFollowTime(LocalDateTime.now());
            
            followMapper.insert(follow);
            
            log.info("用户{}关注用户{}", followerId, followedId);
            return ResultVO.success("关注成功", null);
        } catch (Exception e) {
            log.error("关注操作失败", e);
            return ResultVO.error("关注失败：" + e.getMessage());
        }
    }
    
    /**
     * 取消关注用户
     */
    @DeleteMapping("/follow/{followedId}")
    public ResultVO<Void> unfollowUser(@PathVariable String followedId,
                                       HttpServletRequest request) {
        try {
            Long followerId = getCurrentUserId(request);
            
            // 删除关注记录
            Long followedIdLong = Long.parseLong(followedId);
            followMapper.deleteByFollowerAndFollowed(followerId, followedIdLong);
            
            log.info("用户{}取消关注用户{}", followerId, followedId);
            return ResultVO.success("取消关注成功", null);
        } catch (Exception e) {
            log.error("取消关注操作失败", e);
            return ResultVO.error("取消关注失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取关注状态
     */
    @GetMapping("/follow/status/{userId}")
    public ResultVO<Map<String, Boolean>> getFollowStatus(@PathVariable Long userId,
                                                          HttpServletRequest request) {
        try {
            Long followerId = getCurrentUserId(request);
            
            Follow follow = followMapper.selectByFollowerAndFollowed(followerId, userId);
            boolean isFollowed = follow != null;
            
            Map<String, Boolean> result = new HashMap<>();
            result.put("isFollowed", isFollowed);
            
            return ResultVO.success("获取关注状态成功", result);
        } catch (Exception e) {
            log.error("获取关注状态失败", e);
            Map<String, Boolean> result = new HashMap<>();
            result.put("isFollowed", false);
            return ResultVO.success("获取关注状态成功", result);
        }
    }
    
    /**
     * 获取关注列表
     */
    @GetMapping("/follow/following/{userId}")
    public ResultVO<?> getFollowingList(@PathVariable Long userId,
                                        @RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        HttpServletRequest request) {
        try {
            log.info("获取用户{}的关注列表，页码{}，每页数量{}", userId, page, size);
            
            // 调用 Service 获取关注列表
            var followingList = userService.getFollowingList(userId, page, size);
            
            log.info("获取关注列表成功，共{}条", followingList.getTotal());
            return ResultVO.success("获取关注列表成功", followingList);
        } catch (Exception e) {
            log.error("获取关注列表失败", e);
            return ResultVO.error("获取关注列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取粉丝列表
     */
    @GetMapping("/follow/follower/{userId}")
    public ResultVO<?> getFollowerList(@PathVariable Long userId,
                                       @RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       HttpServletRequest request) {
        try {
            log.info("获取用户{}的粉丝列表，页码{}，每页数量{}", userId, page, size);
            
            // 调用 Service 获取粉丝列表
            var followerList = userService.getFollowerList(userId, page, size);
            
            log.info("获取粉丝列表成功，共{}条", followerList.getTotal());
            return ResultVO.success("获取粉丝列表成功", followerList);
        } catch (Exception e) {
            log.error("获取粉丝列表失败", e);
            return ResultVO.error("获取粉丝列表失败：" + e.getMessage());
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

    /**
     * 点赞请求DTO
     */
    public static class LikeRequestDTO {
        private Long targetId;  // 目标ID（帖子ID）
        private Boolean isLike;   // 是否点赞

        // Getters and Setters
        public Long getTargetId() { return targetId; }
        public void setTargetId(Long targetId) { this.targetId = targetId; }
        
        public Boolean getIsLike() { return isLike; }
        public void setIsLike(Boolean isLike) { this.isLike = isLike; }
    }

    /**
     * 收藏请求DTO
     */
    public static class CollectRequestDTO {
        private Long targetId;     // 目标ID（帖子ID）
        private Boolean isCollect;   // 是否收藏

        // Getters and Setters
        public Long getTargetId() { return targetId; }
        public void setTargetId(Long targetId) { this.targetId = targetId; }
        
        public Boolean getIsCollect() { return isCollect; }
        public void setIsCollect(Boolean isCollect) { this.isCollect = isCollect; }
    }
}