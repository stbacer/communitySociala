package cn.edu.ccst.communitysocialmain.mapper;

import cn.edu.ccst.communitysocialmain.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 举报Mapper接口
 */
@Mapper
public interface ReportMapper {
    
    /**
     * 根据举报ID查询举报
     */
    Report selectById(@Param("reportId") Long reportId);
    
    /**
     * 根据举报用户ID查询举报列表
     */
    List<Report> selectByReporterId(@Param("reporterId") String reporterId,
                                   @Param("offset") Integer offset,
                                   @Param("limit") Integer limit);
    
    /**
     * 根据目标类型和目标ID查询举报
     */
    List<Report> selectByTarget(@Param("targetType") Integer targetType,
                               @Param("targetId") Long targetId,
                               @Param("offset") Integer offset,
                               @Param("limit") Integer limit);
    
    /**
     * 查询待处理举报
     */
    List<Report> selectPendingReports(@Param("offset") Integer offset,
                                     @Param("limit") Integer limit);
    
    /**
     * 统计待处理举报数量
     */
    Long countPendingReports();
    
    /**
     * 统计用户举报数量
     */
    Long countByReporterId(@Param("reporterId") String reporterId);
    
    /**
     * 查询所有举报（分页）
     */
    List<Report> selectAll(@Param("offset") Integer offset, @Param("limit") Integer limit);
    
    /**
     * 根据状态查询举报列表
     */
    List<Report> selectByStatus(@Param("status") Integer status,
                               @Param("offset") Integer offset,
                               @Param("limit") Integer limit);
    
    /**
     * 根据关键词搜索举报
     */
    List<Report> selectByKeyword(@Param("keyword") String keyword,
                                @Param("offset") Integer offset,
                                @Param("limit") Integer limit);
    
    /**
     * 统计总举报数量
     */
    Long countAll();
    
    /**
     * 根据状态统计举报数量
     */
    Long countByStatus(@Param("status") Integer status);
    
    /**
     * 根据关键词统计举报数量
     */
    Long countByKeyword(@Param("keyword") String keyword);
    
    /**
     * 插入举报
     */
    int insert(Report report);
    
    /**
     * 更新举报状态和处理结果
     */
    int updateStatus(@Param("reportId") Long reportId,
                    @Param("status") Integer status,
                    @Param("handlerId") String handlerId,
                    @Param("handleResult") String handleResult);
    
    /**
     * 删除举报
     */
    int deleteById(@Param("reportId") Long reportId);
}