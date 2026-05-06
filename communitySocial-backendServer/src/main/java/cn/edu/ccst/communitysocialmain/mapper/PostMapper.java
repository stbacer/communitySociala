package cn.edu.ccst.communitysocialmain.mapper;

import cn.edu.ccst.communitysocialmain.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子Mapper接口
 */
@Mapper
public interface PostMapper {
    
    /**
     * 根据帖子ID查询帖子
     */
    Post selectById(@Param("postId") Long postId);
    
    /**
     * 根据用户 ID 查询帖子列表
     */
    List<Post> selectByUserId(@Param("userId") Long userId, 
                             @Param("offset") Integer offset, 
                             @Param("limit") Integer limit);
        
    /**
     * 根据用户 ID 查询帖子列表（用于用户主页，只显示审核已通过的帖子）
     */
    List<Post> selectByUserIdAndStatus(@Param("userId") Long userId,
                                       @Param("status") Integer status,
                                       @Param("offset") Integer offset,
                                       @Param("limit") Integer limit);
    
    /**
     * 根据用户 ID 查询帖子列表（用于我的帖子页，包括待审核和已审核通过的帖子）
     */
    List<Post> selectByUserIdForOwner(@Param("userId") Long userId,
                                      @Param("offset") Integer offset,
                                      @Param("limit") Integer limit);
    
    /**
     * 根据板块分类 ID 查询帖子列表
     */
    List<Post> selectByCategoryId(@Param("categoryId") Integer categoryId,
                                 @Param("offset") Integer offset,
                                 @Param("limit") Integer limit);
    
    /**
     * 根据状态查询帖子列表
     */
    List<Post> selectByStatus(@Param("status") Integer status,
                             @Param("offset") Integer offset,
                             @Param("limit") Integer limit);
    
    /**
     * 查询待审核帖子
     */
    List<Post> selectPendingPosts(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    /**
     * 根据关键词搜索帖子
     */
    List<Post> selectByKeyword(@Param("keyword") String keyword,
                              @Param("status") Integer status,
                              @Param("categoryId") Integer categoryId,
                              @Param("offset") Integer offset,
                              @Param("limit") Integer limit);
    
    /**
     * 根据条件筛选帖子
     */
    List<Post> selectByConditions(@Param("status") Integer status,
                                 @Param("categoryId") Integer categoryId,
                                 @Param("offset") Integer offset,
                                 @Param("limit") Integer limit);
    
    /**
     * 查询所有帖子
     */
    List<Post> selectAll(@Param("offset") Integer offset,
                        @Param("limit") Integer limit);
    
    /**
     * 根据地理位置查询附近帖子
     */
    List<Post> selectNearbyPosts(@Param("longitude") Double longitude,
                                @Param("latitude") Double latitude,
                                @Param("radius") Double radius,
                                @Param("offset") Integer offset,
                                @Param("limit") Integer limit);
    
    /**
     * 搜索帖子（标题和内容）
     */
    List<Post> searchPosts(@Param("keyword") String keyword,
                          @Param("offset") Integer offset,
                          @Param("limit") Integer limit);
    
    /**
     * 查询热门帖子（按点赞数和评论数排序）
     */
    List<Post> selectHotPosts(@Param("days") Integer days,
                             @Param("offset") Integer offset,
                             @Param("limit") Integer limit);
    
    /**
     * 按时间排序查询帖子
     */
    List<Post> selectByTime(@Param("categoryId") Integer categoryId,
                           @Param("keyword") String keyword,
                           @Param("status") Integer status,
                           @Param("offset") Integer offset,
                           @Param("limit") Integer limit);
    
    /**
     * 按热度排序查询帖子
     */
    List<Post> selectByHot(@Param("categoryId") Integer categoryId,
                          @Param("keyword") String keyword,
                          @Param("status") Integer status,
                          @Param("offset") Integer offset,
                          @Param("limit") Integer limit);
    
    /**
     * 按地理位置查询附近帖子
     */
    List<Post> selectByNearby(@Param("longitude") Double longitude,
                             @Param("latitude") Double latitude,
                             @Param("radius") Double radius,
                             @Param("categoryId") Integer categoryId,
                             @Param("keyword") String keyword,
                             @Param("status") Integer status,
                             @Param("offset") Integer offset,
                             @Param("limit") Integer limit);
    
    /**
     * 根据时间范围查询帖子列表
     */
    List<Post> selectByTimeRange(@Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计帖子总数
     */
    Long countAll();
    
    /**
     * 根据用户 ID 统计帖子数量
     */
    Long countByUserId(@Param("userId") Long userId);
        
    /**
     * 根据用户 ID 和状态统计帖子数量
     */
    Long countByUserIdAndStatus(@Param("userId") Long userId,
                                @Param("status") Integer status);
    
    /**
     * 根据用户 ID 统计帖子数量（用于我的帖子页，包括待审核和已审核通过的帖子）
     */
    Long countByUserIdForOwner(@Param("userId") Long userId);
    
    /**
     * 根据板块分类 ID 统计帖子数量
     */
    Long countByCategoryId(@Param("categoryId") Integer categoryId);
    
    /**
     * 根据板块分类 ID 和时间范围统计帖子数量
     */
    Long countByCategoryIdAndTimeRange(
        @Param("categoryId") Integer categoryId,
        @Param("startTime") java.time.LocalDateTime startTime
    );
    
    /**
     * 根据板块分类 ID、时间范围和社区统计帖子数量（社区管理员用）
     */
    Long countByCategoryIdAndTimeRangeAndCommunity(
        @Param("categoryId") Integer categoryId,
        @Param("startTime") java.time.LocalDateTime startTime,
        @Param("community") String community
    );
    
    /**
     * 根据板块分类 ID 和社区统计帖子数量（社区管理员用）
     */
    Long countByCategoryIdAndCommunity(
        @Param("categoryId") Integer categoryId,
        @Param("community") String community
    );
    
    /**
     * 根据关键词统计帖子数量
     */
    Long countByKeyword(@Param("keyword") String keyword,
                       @Param("status") Integer status,
                       @Param("categoryId") Integer categoryId);
    
    /**
     * 根据条件统计帖子数量
     */
    Long countByConditions(@Param("status") Integer status,
                          @Param("categoryId") Integer categoryId);
    
    /**
     * 根据状态统计帖子数量
     */
    Long countByStatus(@Param("status") Integer status);
    
    /**
     * 统计今日审核数量
     */
    Long countReviewedToday(@Param("startTime") java.time.LocalDateTime startTime,
                           @Param("endTime") java.time.LocalDateTime endTime);
    
    /**
     * 插入帖子
     */
    int insert(Post post);
    
    /**
     * 更新帖子
     */
    int update(Post post);
    
    /**
     * 更新帖子状态
     */
    int updateStatus(@Param("postId") Long postId, 
                    @Param("status") Integer status,
                    @Param("reviewerId") String reviewerId);
    
    /**
     * 更新帖子统计数据
     */
    int updateStats(@Param("postId") Long postId,
                   @Param("viewCount") Integer viewCount,
                   @Param("likeCount") Integer likeCount,
                   @Param("commentCount") Integer commentCount,
                   @Param("collectCount") Integer collectCount);
    
    /**
     * 增加浏览次数
     */
    int incrementViewCount(@Param("postId") Long postId);
    
    /**
     * 增加点赞数
     */
    int incrementLikeCount(@Param("postId") Long postId);
    
    /**
     * 减少点赞数
     */
    int decrementLikeCount(@Param("postId") Long postId);
    
    /**
     * 增加评论数
     */
    int incrementCommentCount(@Param("postId") Long postId);
    
    /**
     * 减少评论数
     */
    int decrementCommentCount(@Param("postId") Long postId);
    
    /**
     * 增加收藏数
     */
    int incrementCollectCount(@Param("postId") Long postId);
    
    /**
     * 减少收藏数
     */
    int decrementCollectCount(@Param("postId") Long postId);
    
    /**
     * 删除帖子
     */
    int deleteById(@Param("postId") Long postId);
    
    /**
     * 根据类型查询帖子列表
     */
    List<Post> selectByType(@Param("type") Integer type,
                           @Param("offset") Integer offset,
                           @Param("limit") Integer limit);
    
    /**
     * 根据类型统计帖子数量
     */
    Long countByType(@Param("type") Integer type);
    
    /**
     * 统计指定时间段内的帖子数
     */
    Long countByTimeRange(@Param("startTime") java.time.LocalDateTime startTime, 
                         @Param("endTime") java.time.LocalDateTime endTime);
    
    /**
     * 根据社区查询帖子列表（居民端专用）
     */
    List<Post> selectByCommunity(@Param("community") String community,
                                @Param("offset") Integer offset,
                                @Param("limit") Integer limit);
    
    /**
     * 根据社区和分类查询帖子列表（支持状态筛选）
     */
    List<Post> selectByCommunityAndCategory(@Param("community") String community,
                                           @Param("categoryId") Integer categoryId,
                                           @Param("status") Integer status,
                                           @Param("offset") Integer offset,
                                           @Param("limit") Integer limit);
    
    /**
     * 根据社区和关键词搜索帖子
     */
    List<Post> selectByCommunityAndKeyword(@Param("community") String community,
                                          @Param("keyword") String keyword,
                                          @Param("status") Integer status,
                                          @Param("categoryId") Integer categoryId,
                                          @Param("offset") Integer offset,
                                          @Param("limit") Integer limit);
    
    /**
     * 根据社区统计帖子数量
     */
    Long countByCommunity(@Param("community") String community);
    
    /**
     * 根据社区和时间范围统计帖子数量
     */
    Long countByCommunityAndTimeRange(@Param("community") String community,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * 根据社区和分类统计帖子数量
     */
    Long countByCommunityAndCategory(@Param("community") String community,
                                     @Param("categoryId") Integer categoryId);
}