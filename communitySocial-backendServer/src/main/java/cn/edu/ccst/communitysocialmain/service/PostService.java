package cn.edu.ccst.communitysocialmain.service;

import cn.edu.ccst.communitysocialmain.dto.PostCreateDTO;
import cn.edu.ccst.communitysocialmain.dto.ReviewDTO;
import cn.edu.ccst.communitysocialmain.entity.Post;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.PostDetailVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 帖子服务接口
 */
public interface PostService {
    
    /**
     * 发布帖子
     */
    PostDetailVO createPost(Long userId, PostCreateDTO postCreateDTO);
    
    /**
     * 更新帖子
     */
    PostDetailVO updatePost(Long userId, PostCreateDTO postUpdateDTO);
    
    /**
     * 根据帖子ID获取帖子详情
     */
    PostDetailVO getPostDetail(Long postId, Long currentUserId);
    
    /**
     * 根据用户ID获取帖子列表
     */
    PageVO<PostDetailVO> getPostsByUserId(Long userId, Integer page, Integer size, Long currentUserId);
    
    /**
     * 根据类型获取帖子列表
     */
    PageVO<PostDetailVO> getPostsByType(Integer type, Integer page, Integer size, Long currentUserId);
    
    /**
     * 根据状态获取帖子列表（管理员用）
     */
    PageVO<PostDetailVO> getPostsByStatus(Integer status, Integer page, Integer size);
    
    /**
     * 获取待审核帖子列表
     */
    PageVO<PostDetailVO> getPendingPosts(Integer page, Integer size);
    
    /**
     * 根据社区获取待审核帖子列表
     */
    PageVO<PostDetailVO> getPendingPostsByCommunity(String community, Integer page, Integer size, Long currentUserId);
    
    /**
     * 根据条件获取帖子列表（支持搜索和筛选）
     */
    PageVO<PostDetailVO> getPostsByConditions(Integer page, Integer size, Integer status, String keyword, Integer categoryId);
    
    /**
     * 根据条件获取帖子列表（支持社区过滤）
     */
    PageVO<PostDetailVO> getPostsByConditionsWithCommunity(Integer page, Integer size, Integer status, String keyword, Integer categoryId, String community);
    
    /**
     * 获取帖子列表（公开接口）
     */
    PageVO<PostDetailVO> getPostList(Integer page, Integer size, Integer type, Integer categoryId, String keyword, Long currentUserId);
    
    /**
     * 获取按时间排序的帖子列表
     */
    PageVO<PostDetailVO> getPostsByTime(Integer page, Integer size, Integer type, Integer categoryId, String keyword, Long currentUserId);
    
    /**
     * 获取按热度排序的帖子列表
     */
    PageVO<PostDetailVO> getPostsByHot(Integer page, Integer size, Integer type, Integer categoryId, String keyword, Long currentUserId);
    
    /**
     * 获取附近帖子列表
     */
    PageVO<PostDetailVO> getPostsByNearby(Double longitude, Double latitude, Double radius, 
                                         Integer page, Integer size, Integer type, Integer categoryId, 
                                         String keyword, Long currentUserId);
    
    /**
     * 获取帖子统计信息
     */
    Map<String, Object> getPostStatistics();
    
    /**
     * 获取帖子审核历史
     */
    List<Map<String, Object>> getReviewHistory(Long postId);
    
    /**
     * 搜索帖子
     */
    PageVO<PostDetailVO> searchPosts(String keyword, Integer page, Integer size, Long currentUserId);
    
    /**
     * 获取附近帖子
     */
    PageVO<PostDetailVO> getNearbyPosts(Double longitude, Double latitude, Double radius, 
                                       Integer page, Integer size, Long currentUserId);
    
    /**
     * 获取热门帖子
     */
    PageVO<PostDetailVO> getHotPosts(Integer days, Integer page, Integer size, Long currentUserId);
    
    /**
     * 审核帖子
     */
    void reviewPost(Long adminUserId, ReviewDTO reviewDTO);
    
    /**
     * 置顶帖子
     */
    void topPost(Long postId);
    
    /**
     * 删除帖子
     */
    void deletePost(Long postId, Long userId);
    
    /**
     * 增加帖子浏览次数
     */
    void incrementViewCount(Long postId);
    
    /**
     * 获取帖子实体
     */
    Post getPostById(Long postId);
    
    /**
     * 生成帖子ID
     */
    Long generatePostId();
    
    /**
     * 点赞帖子
     */
    void likePost(Long userId, Long postId);
    
    /**
     * 取消点赞帖子
     */
    void unlikePost(Long userId, Long postId);
    
    /**
     * 收藏帖子
     */
    void collectPost(Long userId, Long postId);
    
    /**
     * 取消收藏帖子
     */
    void uncollectPost(Long userId, Long postId);
    
    /**
     * 获取用户的收藏列表
     */
    PageVO<Post> getUserCollections(Long userId, Integer page, Integer size);
    
    /**
     * 根据社区统计帖子数量
     */
    Long countPostsByCommunity(String community, Long userId);
    
    /**
     * 根据用户 ID 统计帖子数量
     */
    Long countPostsByUserId(Long userId);
    
    /**
     * 根据社区和时间范围统计帖子数量
     */
    Long countPostsByCommunityAndTimeRange(String community, LocalDateTime startTime, LocalDateTime endTime);
}