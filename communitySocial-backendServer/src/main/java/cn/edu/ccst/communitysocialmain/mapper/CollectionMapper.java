package cn.edu.ccst.communitysocialmain.mapper;

import cn.edu.ccst.communitysocialmain.entity.UserCollection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 收藏Mapper接口
 */
@Mapper
public interface CollectionMapper {
    
    /**
     * 根据收藏ID查询收藏
     */
    UserCollection selectById(@Param("collectionId") Long collectionId);
    
    /**
     * 根据用户ID和帖子ID查询收藏
     */
    UserCollection selectByUserAndPost(@Param("userId") Long userId, 
                                  @Param("postId") Long postId);
    
    /**
     * 根据用户ID查询收藏列表
     */
    List<UserCollection> selectByUserId(@Param("userId") Long userId,
                                   @Param("offset") Integer offset,
                                   @Param("limit") Integer limit);
    
    /**
     * 根据帖子ID查询收藏用户列表
     */
    List<UserCollection> selectByPostId(@Param("postId") Long postId,
                                   @Param("offset") Integer offset,
                                   @Param("limit") Integer limit);
    
    /**
     * 统计用户收藏数量
     */
    Long countByUserId(@Param("userId") Long userId);
    
    /**
     * 统计帖子被收藏数量
     */
    Long countByPostId(@Param("postId") Long postId);
    
    /**
     * 插入收藏
     */
    int insert(UserCollection collection);
    
    /**
     * 删除收藏
     */
    int deleteById(@Param("collectionId") Long collectionId);
    
    /**
     * 根据用户ID和帖子ID删除收藏
     */
    int deleteByUserAndPost(@Param("userId") Long userId, 
                           @Param("postId") Long postId);
    
    /**
     * 根据用户ID删除所有收藏
     */
    int deleteByUserId(@Param("userId") Long userId);
    
    /**
     * 统计指定时间段内的收藏数
     */
    Long countByTimeRange(@Param("startTime") java.time.LocalDateTime startTime, 
                         @Param("endTime") java.time.LocalDateTime endTime);
    
    /**
     * 统计收藏总数
     */
    Long countAll();
}