package cn.edu.ccst.communitysocialmain.mapper;

import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;

/**
 * 用户统计Mapper接口
 */
public interface UserStatisticsMapper {
    
    /**
     * 统计指定时间段内的新增用户数
     */
    Long countNewUsersInPeriod(@Param("startTime") LocalDateTime startTime, 
                              @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定时间段内的活跃用户数
     */
    Long countActiveUsers(@Param("startTime") LocalDateTime startTime, 
                         @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定天数内的活跃用户数
     */
    Long countActiveUsersInPeriod(@Param("days") Integer days);
    
    /**
     * 根据用户角色统计用户数
     */
    Long countByRole(@Param("userRole") Integer userRole);
    
    /**
     * 根据用户状态统计用户数
     */
    Long countByStatus(@Param("status") Integer status);
}