package cn.edu.ccst.communitysocialmain.mapper;

import cn.edu.ccst.communitysocialmain.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {
    
    /**
     * 根据用户 ID 查询用户
     */
    User selectById(@Param("userId") Long userId);
        
    /**
     * 根据手机号查询用户
     */
    User selectByPhone(@Param("phone") String phone);
        
    /**
     * 根据微信 OpenID 查询用户
     */
    User selectByOpenId(@Param("openId") String openId);
    
    /**
     * 查询所有用户（分页）
     */
    List<User> selectAll(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    /**
     * 根据条件查询用户列表
     */
    List<User> selectByCondition(@Param("user") User user, 
                                @Param("offset") Integer offset, 
                                @Param("limit") Integer limit);
    
    /**
     * 查询待审核的实名认证用户
     */
    List<User> selectPendingAuthUsers(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    /**
     * 统计待审核用户总数
     */
    Long countPendingAuthUsers();
    
    /**
     * 根据社区查询待审核的实名认证用户列表
     */
    List<User> selectPendingAuthUsersByCommunity(@Param("community") String community,
                                                 @Param("offset") Integer offset,
                                                 @Param("limit") Integer limit);
    
    /**
     * 根据社区统计待审核的实名认证用户数量
     */
    Long countPendingAuthUsersByCommunity(@Param("community") String community);
    
    /**
     * 统计用户总数
     */
    Long countAll();
    
    /**
     * 根据条件统计用户数量
     */
    Long countByCondition(@Param("user") User user);
    
    /**
     * 插入用户
     */
    int insert(User user);
    
    /**
     * 更新用户信息
     */
    int update(User user);
    
    /**
     * 更新最后登录时间
     */
    int updateLastLoginTime(@Param("userId") Long userId);
    
    /**
     * 更新用户状态
     */
    int updateStatus(@Param("userId") Long userId, @Param("status") Integer status);
    
    /**
     * 更新实名认证状态
     */
    int updateAuthStatus(@Param("userId") Long userId, @Param("authStatus") Integer authStatus);
    
    /**
     * 删除用户
     */
    int deleteById(@Param("userId") Long userId);
    
    /**
     * 统计指定时间段内的新增用户数
     */
    Long countNewUsersInPeriod(@Param("startTime") java.time.LocalDateTime startTime, 
                              @Param("endTime") java.time.LocalDateTime endTime);
    
    /**
     * 统计指定时间段内的活跃用户数
     */
    Long countActiveUsers(@Param("startTime") java.time.LocalDateTime startTime, 
                         @Param("endTime") java.time.LocalDateTime endTime);
    
    /**
     * 统计指定天数内的活跃用户数
     */
    Long countActiveUsersInPeriod(@Param("days") Integer days);
    
    /**
     * 根据用户角色统计用户数
     */
    Long countByRole(@Param("userRole") String userRole);
    
    /**
     * 格局用户状态统计用户数
     */
    Long countByStatus(@Param("status") Integer status);
    
    /**
     * 统计用户发布的帖子数量
     */
    Long countUserPosts(@Param("userId") Long userId);
    
    /**
     * 统计用户收到的点赞数量
     */
    Long countUserReceivedLikes(@Param("userId") Long userId);
    
    /**
     * 统计用户发表的评论数量
     */
    Long countUserComments(@Param("userId") Long userId);
    
    /**
     * 统计用户收藏的帖子数量
     */
    Long countUserCollections(@Param("userId") Long userId);
    
    /**
     * 查询管理员列表
     */
    List<User> selectAdmins(@Param("offset") Integer offset, 
                           @Param("limit") Integer limit, 
                           @Param("keyword") String keyword);
    
    /**
     * 统计管理员数量
     */
    Long countAdmins(@Param("keyword") String keyword);
    
    /**
     * 按社区分布统计用户数
     * @return List<Map<String, Object>>，每个 Map 包含 community 和 count 字段
     */
    java.util.List<java.util.Map<String, Object>> selectCommunityDistribution();
    
    /**
     * 根据社区统计用户数量
     */
    Long countUsersByCommunity(@Param("community") String community);
    
    /**
     * 根据社区查询用户列表
     */
    List<User> selectByCommunity(@Param("community") String community);
    
    /**
     * 根据社区和时间范围统计新增用户数量
     */
    Long countNewUsersByCommunityAndTimeRange(@Param("community") String community,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计社区总数（去重）
     */
    Long countDistinctCommunities();
    
    /**
     * 根据时间范围查询有行为的用户 ID 列表（通用方法，支持多表）
     * @param tableName 表名
     * @param userIdColumn 用户 ID 列名
     * @param timeColumn 时间列名
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户 ID 列表
     */
    List<java.util.Map<String, Object>> selectUserIdsWithActionInTimeRange(
            @Param("tableName") String tableName,
            @Param("userIdColumn") String userIdColumn,
            @Param("timeColumn") String timeColumn,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定用户 ID 列表在指定时间范围内的活跃用户数
     * @param userIds 用户 ID 列表
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 活跃用户数
     */
    Long countActiveUsersByIdsAndTimeRange(@Param("userIds") List<String> userIds,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 根据社区和时间范围查询登录过的用户
     */
    List<User> selectUsersByLoginTimeRange(
        @Param("community") String community,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询所有不同的社区名称
     */
    java.util.List<String> selectDistinctCommunities();
    
    /**
     * 查询待审核的管理员实名认证列表（user_role=2 且 auth_status=1）
     */
    List<User> selectPendingAdminAuthList(@Param("offset") Integer offset,
                                          @Param("limit") Integer limit,
                                          @Param("keyword") String keyword);
    
    /**
     * 统计待审核的管理员实名认证数量
     */
    Long countPendingAdminAuthList(@Param("keyword") String keyword);
    
    /**
     * 根据用户角色查询用户列表
     * @param userRole 用户角色（1-普通用户，2-社区管理员，3-超级管理员）
     * @return 用户列表
     */
    List<User> selectByUserRole(@Param("userRole") Integer userRole);
}