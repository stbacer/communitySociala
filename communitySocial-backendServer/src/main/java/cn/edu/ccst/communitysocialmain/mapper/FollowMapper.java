package cn.edu.ccst.communitysocialmain.mapper;

import cn.edu.ccst.communitysocialmain.entity.Follow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 关注 Mapper 接口
 */
@Mapper
public interface FollowMapper {
    
    /**
     * 根据关注 ID 查询关注
     */
    Follow selectById(@Param("followId") String followId);
    
    /**
     * 根据关注者和被关注者 ID 查询关注
     */
    Follow selectByFollowerAndFollowed(@Param("followerId") Long followerId, 
                                       @Param("followedId") Long followedId);
    
    /**
     * 查询用户的关注列表
     */
    List<Follow> selectFollowingByUserId(@Param("userId") Long userId,
                                         @Param("offset") Integer offset,
                                         @Param("limit") Integer limit);
    
    /**
     * 查询用户的粉丝列表
     */
    List<Follow> selectFollowerByUserId(@Param("userId") Long userId,
                                        @Param("offset") Integer offset,
                                        @Param("limit") Integer limit);
    
    /**
     * 统计用户关注数量
     */
    Long countFollowingByUserId(@Param("userId") Long userId);
    
    /**
     * 统计用户粉丝数量
     */
    Long countFollowerByUserId(@Param("userId") Long userId);
    
    /**
     * 插入关注
     */
    int insert(Follow follow);
    
    /**
     * 删除关注
     */
    int deleteById(@Param("followId") String followId);
    
    /**
     * 根据关注者和被关注者 ID 删除关注
     */
    int deleteByFollowerAndFollowed(@Param("followerId") Long followerId, 
                                    @Param("followedId") Long followedId);
    
    /**
     * 根据用户 ID 删除所有关注记录（作为关注者）
     */
    int deleteByFollowerId(@Param("followerId") Long followerId);
    
    /**
     * 根据用户 ID 删除所有关注记录（作为被关注者）
     */
    int deleteByFollowedId(@Param("followedId") Long followedId);
}
