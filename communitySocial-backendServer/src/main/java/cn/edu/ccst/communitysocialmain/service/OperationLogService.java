package cn.edu.ccst.communitysocialmain.service;

import cn.edu.ccst.communitysocialmain.entity.OperationLog;
import java.util.List;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {
    
    /**
     * 记录成功操作日志
     * @param log 操作日志对象
     */
    void logSuccess(OperationLog log);
    
    /**
     * 查询用户操作日志列表
     * @param userId 用户 ID
     * @param offset 偏移量
     * @param limit 数量限制
     * @return 操作日志列表
     */
    List<OperationLog> getUserLogs(String userId, int offset, int limit);
    
    /**
     * 查询用户的登录和注册日志列表
     * @param userId 用户 ID
     * @param offset 偏移量
     * @param limit 数量限制
     * @return 操作日志列表
     */
    List<OperationLog> getUserLoginAndRegisterLogs(String userId, int offset, int limit);
    
    /**
     * 根据 ID 获取日志详情
     * @param logId 日志 ID
     * @return 操作日志详情
     */
    OperationLog getLogById(String logId);
    
    /**
     * 查询全局操作日志列表（支持筛选）
     * @param operator 操作用户昵称/姓名
     * @param operationType 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param offset 偏移量
     * @param limit 数量限制
     * @return 操作日志列表
     */
    List<OperationLog> getGlobalLogs(String operator, String operationType, String startTime, String endTime, int offset, int limit);
    
    /**
     * 统计符合条件的日志总数（支持筛选）
     * @param operator 操作用户昵称/姓名
     * @param operationType 操作类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日志总数
     */
    long countGlobalLogs(String operator, String operationType, String startTime, String endTime);
    
    /**
     * 根据模块查询操作日志
     * @param module 模块名称
     * @param date 日期 (yyyy-MM-dd)
     * @param offset 偏移量
     * @param limit 数量限制
     * @return 操作日志列表
     */
    List<OperationLog> getModuleLogs(String module, String date, int offset, int limit);
    
    /**
     * 统计用户今日操作数
     * @param userId 用户 ID
     * @return 操作数量
     */
    int countUserTodayOperations(String userId);
    
    /**
     * 统计全局今日操作数
     * @return 操作数量
     */
    int countGlobalTodayOperations();
    
    /**
     * 统计全局所有操作日志总数（不仅仅是今日）
     * @return 操作日志总数
     */
    long countGlobalAllOperations();
    
    /**
     * 删除单条操作日志
     * @param logId 日志 ID
     * @return 是否删除成功
     */
    boolean deleteLog(String logId);
    
    /**
     * 批量删除操作日志
     * @param logIds 日志 ID 列表
     * @return 删除成功的数量
     */
    int batchDeleteLogs(List<String> logIds);
    
    /**
     * 查询帖子的历史操作日志
     * @param postId 帖子 ID
     * @param offset 偏移量
     * @param limit 数量限制
     * @return 操作日志列表
     */
    List<OperationLog> getPostHistoryLogs(String postId, int offset, int limit);
    
    /**
     * 重建指定帖子的操作日志索引（用于修复历史数据）
     * @param postId 帖子 ID
     * @return 添加到时间线的日志数量
     */
    int rebuildPostLogIndex(String postId);
    
    /**
     * 获取所有已记录的操作类型（从 Redis Set 中读取）
     * @return 操作类型列表（已排序）
     */
    List<String> getOperationTypes();
}
