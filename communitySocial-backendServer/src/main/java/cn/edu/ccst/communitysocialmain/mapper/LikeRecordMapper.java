package cn.edu.ccst.communitysocialmain.mapper;

import cn.edu.ccst.communitysocialmain.entity.LikeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 点赞记录Mapper接口
 */
@Mapper
public interface LikeRecordMapper {
    
    /**
     * 根据点赞ID查询点赞记录
     */
    LikeRecord selectById(@Param("likeId") Long likeId);
    
    /**
     * 根据用户ID、目标类型和目标ID查询点赞记录
     */
    LikeRecord selectByUserAndTarget(@Param("userId") Long userId,
                                    @Param("targetType") Integer targetType,
                                    @Param("targetId") Long targetId);
    
    /**
     * 根据用户ID查询点赞列表
     */
    List<LikeRecord> selectByUserId(@Param("userId") Long userId,
                                   @Param("targetType") Integer targetType,
                                   @Param("offset") Integer offset,
                                   @Param("limit") Integer limit);
    
    /**
     * 根据目标类型和目标ID查询点赞用户列表
     */
    List<LikeRecord> selectByTarget(@Param("targetType") Integer targetType,
                                   @Param("targetId") Long targetId,
                                   @Param("offset") Integer offset,
                                   @Param("limit") Integer limit);
    
    /**
     * 统计目标被点赞数量
     */
    Long countByTarget(@Param("targetType") Integer targetType,
                      @Param("targetId") Long targetId);
    
    /**
     * 插入点赞记录
     */
    int insert(LikeRecord likeRecord);
    
    /**
     * 删除点赞记录
     */
    int deleteById(@Param("likeId") Long likeId);
    
    /**
     * 根据用户ID、目标类型和目标ID删除点赞记录
     */
    int deleteByUserAndTarget(@Param("userId") Long userId,
                             @Param("targetType") Integer targetType,
                             @Param("targetId") Long targetId);
    
    /**
     * 统计点赞总数
     */
    Long countAll();
    
    /**
     * 统计指定时间段内的点赞数
     */
    Long countByTimeRange(@Param("startTime") java.time.LocalDateTime startTime, 
                         @Param("endTime") java.time.LocalDateTime endTime);
}