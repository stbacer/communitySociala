package cn.edu.ccst.communitysocialmain.service.impl;

import cn.edu.ccst.communitysocialmain.entity.OperationLog;
import cn.edu.ccst.communitysocialmain.service.OperationLogService;
import cn.edu.ccst.communitysocialmain.utils.CommonUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 操作日志服务实现类（基于 Redis）
 */
@Slf4j
@Service
public class OperationLogServiceImpl implements OperationLogService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // Redis key 前缀
    private static final String OPERATION_LOG_KEY_PREFIX = "operation:log:";
    private static final String OPERATION_LOG_ZSET_KEY = "operation:log:zset";
    private static final String OPERATION_LOG_USER_ZSET_KEY_PREFIX = "operation:log:user:";
    private static final String OPERATION_LOG_MODULE_ZSET_KEY_PREFIX = "operation:log:module:";
    private static final String OPERATION_LOG_POST_ZSET_KEY_PREFIX = "operation:log:post:";
    private static final String OPERATION_TYPE_SET_KEY = "log:operation:types";  // 操作类型集合
    
    @Override
    public void logSuccess(OperationLog operationLog) {
        try {
            // 生成日志 ID
            if (operationLog.getLogId() == null || operationLog.getLogId().isEmpty()) {
                operationLog.setLogId(CommonUtil.generateUUID());
            }
            
            // 设置创建时间和时间戳
            if (operationLog.getCreateTime() == null) {
                operationLog.setCreateTime(LocalDateTime.now());
            }
            if (operationLog.getTimestamp() == null) {
                operationLog.setTimestamp(operationLog.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
            
            // 将日志对象转换为 JSON 字符串
            String logJson = JSON.toJSONString(operationLog);
            
            // 存储到 Redis Hash 中
            String logKey = OPERATION_LOG_KEY_PREFIX + operationLog.getLogId();
            redisTemplate.opsForValue().set(logKey, logJson, 30, TimeUnit.DAYS);
            
            // 添加到全局 ZSet（按时间戳排序）
            redisTemplate.opsForZSet().add(OPERATION_LOG_ZSET_KEY, operationLog.getLogId(), operationLog.getTimestamp());
            
            // 添加到用户日志 ZSet
            if (operationLog.getUserId() != null) {
                String userZSetKey = OPERATION_LOG_USER_ZSET_KEY_PREFIX + operationLog.getUserId();
                redisTemplate.opsForZSet().add(userZSetKey, operationLog.getLogId(), operationLog.getTimestamp());
            }
            
            // 添加到模块日志 ZSet
            if (operationLog.getModule() != null && !operationLog.getModule().isEmpty()) {
                String moduleZSetKey = OPERATION_LOG_MODULE_ZSET_KEY_PREFIX + operationLog.getModule();
                redisTemplate.opsForZSet().add(moduleZSetKey, operationLog.getLogId(), operationLog.getTimestamp());
            }
            
            // 如果是帖子相关操作，添加到帖子日志 ZSet
            if ("POST".equals(operationLog.getModule()) && operationLog.getContent() != null) {
                // 从内容中提取 postId
                String postId = extractPostIdFromContent(operationLog.getContent());
                if (postId != null && !postId.isEmpty()) {
                    String postZSetKey = OPERATION_LOG_POST_ZSET_KEY_PREFIX + postId;
                    redisTemplate.opsForZSet().add(postZSetKey, operationLog.getLogId(), operationLog.getTimestamp());
                }
            }
            
            // 将操作类型添加到 Redis Set（用于前端动态获取）
            if (operationLog.getOperation() != null && !operationLog.getOperation().isEmpty()) {
                redisTemplate.opsForSet().add(OPERATION_TYPE_SET_KEY, operationLog.getOperation());
            }
            
            System.out.println("记录操作日志成功：logId=" + operationLog.getLogId() + ", userId=" + operationLog.getUserId() + ", operation=" + operationLog.getOperation());
        } catch (Exception e) {
            System.err.println("记录操作日志失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public List<OperationLog> getUserLogs(String userId, int offset, int limit) {
        try {
            String userZSetKey = OPERATION_LOG_USER_ZSET_KEY_PREFIX + userId;
            
            // 从 ZSet 中获取日志 ID 列表（按时间倒序）
            Set<String> logIds = redisTemplate.opsForZSet().reverseRange(userZSetKey, offset, offset + limit - 1);
            
            if (logIds == null || logIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 批量获取日志详情
            List<OperationLog> logs = new ArrayList<>();
            for (String logId : logIds) {
                OperationLog log = getLogById(logId);
                if (log != null) {
                    logs.add(log);
                }
            }
            
            return logs;
        } catch (Exception e) {
            log.error("查询用户日志失败：userId={}, error={}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<OperationLog> getUserLoginAndRegisterLogs(String userId, int offset, int limit) {
        try {
            String userZSetKey = OPERATION_LOG_USER_ZSET_KEY_PREFIX + userId;
            
            // 从 ZSet 中获取所有日志 ID
            Set<String> logIds = redisTemplate.opsForZSet().reverseRange(userZSetKey, offset, offset + limit - 1);
            
            if (logIds == null || logIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 过滤出登录和注册相关的日志
            List<OperationLog> logs = new ArrayList<>();
            for (String logId : logIds) {
                OperationLog log = getLogById(logId);
                if (log != null && ("LOGIN".equals(log.getOperation()) || "REGISTER".equals(log.getOperation()))) {
                    logs.add(log);
                }
            }
            
            return logs;
        } catch (Exception e) {
            log.error("查询用户登录注册日志失败：userId={}, error={}", userId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public OperationLog getLogById(String logId) {
        try {
            String logKey = OPERATION_LOG_KEY_PREFIX + logId;
            String logJson = redisTemplate.opsForValue().get(logKey);
            
            if (logJson != null && !logJson.isEmpty()) {
                return JSON.parseObject(logJson, OperationLog.class);
            }
            
            return null;
        } catch (Exception e) {
            log.error("查询日志详情失败：logId={}, error={}", logId, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public List<OperationLog> getGlobalLogs(String operator, String operationType, String startTime, String endTime, int offset, int limit) {
        try {
            // 从全局 ZSet 中获取所有日志 ID（按时间倒序）
            Set<String> allLogIds = redisTemplate.opsForZSet().reverseRange(OPERATION_LOG_ZSET_KEY, 0, -1);
            
            if (allLogIds == null || allLogIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 在内存中进行筛选
            List<OperationLog> filteredLogs = new ArrayList<>();
            for (String logId : allLogIds) {
                OperationLog log = getLogById(logId);
                if (log != null && matchesFilter(log, operator, operationType, startTime, endTime)) {
                    filteredLogs.add(log);
                }
            }
            
            // 应用分页
            int fromIndex = Math.min(offset, filteredLogs.size());
            int toIndex = Math.min(offset + limit, filteredLogs.size());
            
            if (fromIndex >= toIndex) {
                return new ArrayList<>();
            }
            
            return filteredLogs.subList(fromIndex, toIndex);
        } catch (Exception e) {
            log.error("查询全局日志失败：error={}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 检查日志是否符合筛选条件
     */
    private boolean matchesFilter(OperationLog log, String operator, String operationType, String startTime, String endTime) {
        // 筛选操作用户
        if (operator != null && !operator.trim().isEmpty()) {
            String lowerOperator = operator.toLowerCase();
            boolean operatorMatch = (log.getOperatorName() != null && log.getOperatorName().toLowerCase().contains(lowerOperator))
                                 || (log.getNickname() != null && log.getNickname().toLowerCase().contains(lowerOperator));
            if (!operatorMatch) {
                return false;
            }
        }
        
        // 筛选操作类型
        if (operationType != null && !operationType.trim().isEmpty()) {
            if (!operationType.equals(log.getOperation())) {
                return false;
            }
        }
        
        // 筛选开始时间
        if (startTime != null && !startTime.trim().isEmpty() && log.getCreateTime() != null) {
            LocalDateTime startDateTime = parseDateTime(startTime);
            if (startDateTime != null && log.getCreateTime().isBefore(startDateTime)) {
                return false;
            }
        }
        
        // 筛选结束时间
        if (endTime != null && !endTime.trim().isEmpty() && log.getCreateTime() != null) {
            LocalDateTime endDateTime = parseDateTime(endTime);
            if (endDateTime != null && log.getCreateTime().isAfter(endDateTime)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 解析日期时间字符串
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            // 支持多种格式
            if (dateTimeStr.length() == 10) {
                // yyyy-MM-dd
                return LocalDate.parse(dateTimeStr).atStartOfDay();
            } else if (dateTimeStr.length() >= 19) {
                // yyyy-MM-ddTHH:mm:ss 或 yyyy-MM-dd HH:mm:ss
                dateTimeStr = dateTimeStr.replace('T', ' ');
                return LocalDateTime.parse(dateTimeStr.substring(0, 19));
            }
        } catch (Exception e) {
            log.warn("解析日期失败：{}", dateTimeStr);
        }
        return null;
    }
    
    @Override
    public List<OperationLog> getModuleLogs(String module, String date, int offset, int limit) {
        try {
            String moduleZSetKey = OPERATION_LOG_MODULE_ZSET_KEY_PREFIX + module;
            
            // 从模块 ZSet 中获取日志 ID 列表
            Set<String> logIds = redisTemplate.opsForZSet().reverseRange(moduleZSetKey, offset, offset + limit - 1);
            
            if (logIds == null || logIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 批量获取日志详情
            List<OperationLog> logs = new ArrayList<>();
            for (String logId : logIds) {
                OperationLog log = getLogById(logId);
                if (log != null) {
                    // 如果指定了日期，过滤日期
                    if (date != null && !date.isEmpty() && log.getCreateTime() != null) {
                        String logDate = log.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        if (!date.equals(logDate)) {
                            continue;
                        }
                    }
                    logs.add(log);
                }
            }
            
            return logs;
        } catch (Exception e) {
            log.error("查询模块日志失败：module={}, error={}", module, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public int countUserTodayOperations(String userId) {
        try {
            String userZSetKey = OPERATION_LOG_USER_ZSET_KEY_PREFIX + userId;
            
            // 获取今天的开始时间戳
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            long todayStartMillis = todayStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            // 获取今天的结束时间戳
            LocalDateTime todayEnd = LocalDate.now().plusDays(1).atStartOfDay();
            long todayEndMillis = todayEnd.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            // 统计今天操作数
            Long count = redisTemplate.opsForZSet().count(userZSetKey, todayStartMillis, todayEndMillis);
            
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("统计用户今日操作数失败：userId={}, error={}", userId, e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public int countGlobalTodayOperations() {
        try {
            // 获取今天的开始时间戳
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            long todayStartMillis = todayStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            // 获取今天的结束时间戳
            LocalDateTime todayEnd = LocalDate.now().plusDays(1).atStartOfDay();
            long todayEndMillis = todayEnd.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            // 统计今天操作数
            Long count = redisTemplate.opsForZSet().count(OPERATION_LOG_ZSET_KEY, todayStartMillis, todayEndMillis);
            
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("统计全局今日操作数失败：error={}", e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public long countGlobalAllOperations() {
        try {
            // 统计所有操作日志总数
            Long count = redisTemplate.opsForZSet().size(OPERATION_LOG_ZSET_KEY);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("统计全局操作日志总数失败：error={}", e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public long countGlobalLogs(String operator, String operationType, String startTime, String endTime) {
        try {
            // 从全局 ZSet 中获取所有日志 ID
            Set<String> allLogIds = redisTemplate.opsForZSet().reverseRange(OPERATION_LOG_ZSET_KEY, 0, -1);
            
            if (allLogIds == null || allLogIds.isEmpty()) {
                return 0;
            }
            
            // 在内存中进行筛选并计数
            long count = 0;
            for (String logId : allLogIds) {
                OperationLog log = getLogById(logId);
                if (log != null && matchesFilter(log, operator, operationType, startTime, endTime)) {
                    count++;
                }
            }
            
            return count;
        } catch (Exception e) {
            log.error("统计符合条件的日志失败：error={}", e.getMessage(), e);
            return 0;
        }
    }
    
    @Override
    public boolean deleteLog(String logId) {
        try {
            // 先获取日志详情
            OperationLog operationLog = getLogById(logId);
            if (operationLog == null) {
                return false;
            }
            
            // 删除 Redis Hash 中的日志数据
            String logKey = OPERATION_LOG_KEY_PREFIX + logId;
            redisTemplate.delete(logKey);
            
            // 从全局 ZSet 中删除
            redisTemplate.opsForZSet().remove(OPERATION_LOG_ZSET_KEY, logId);
            
            // 从用户日志 ZSet 中删除
            if (operationLog.getUserId() != null) {
                String userZSetKey = OPERATION_LOG_USER_ZSET_KEY_PREFIX + operationLog.getUserId();
                redisTemplate.opsForZSet().remove(userZSetKey, logId);
            }
            
            // 从模块日志 ZSet 中删除
            if (operationLog.getModule() != null && !operationLog.getModule().isEmpty()) {
                String moduleZSetKey = OPERATION_LOG_MODULE_ZSET_KEY_PREFIX + operationLog.getModule();
                redisTemplate.opsForZSet().remove(moduleZSetKey, logId);
            }
            
            // 从帖子日志 ZSet 中删除
            if ("POST".equals(operationLog.getModule()) && operationLog.getContent() != null) {
                String postId = extractPostIdFromContent(operationLog.getContent());
                if (postId != null && !postId.isEmpty()) {
                    String postZSetKey = OPERATION_LOG_POST_ZSET_KEY_PREFIX + postId;
                    redisTemplate.opsForZSet().remove(postZSetKey, logId);
                }
            }
            
            System.out.println("删除操作日志成功：logId=" + logId);
            return true;
        } catch (Exception e) {
            System.err.println("删除操作日志失败：logId=" + logId + ", error=" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public int batchDeleteLogs(List<String> logIds) {
        int successCount = 0;
        for (String logId : logIds) {
            if (deleteLog(logId)) {
                successCount++;
            }
        }
        return successCount;
    }
    
    @Override
    public List<OperationLog> getPostHistoryLogs(String postId, int offset, int limit) {
        try {
            String postZSetKey = OPERATION_LOG_POST_ZSET_KEY_PREFIX + postId;
            
            // 从帖子 ZSet 中获取日志 ID 列表
            Set<String> logIds = redisTemplate.opsForZSet().reverseRange(postZSetKey, offset, offset + limit - 1);
            
            if (logIds == null || logIds.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 批量获取日志详情
            List<OperationLog> logs = new ArrayList<>();
            for (String logId : logIds) {
                OperationLog log = getLogById(logId);
                if (log != null) {
                    logs.add(log);
                }
            }
            
            return logs;
        } catch (Exception e) {
            log.error("查询帖子历史日志失败：postId={}, error={}", postId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public int rebuildPostLogIndex(String postId) {
        // 这个方法用于修复帖子的操作日志索引
        // 由于操作日志已经存储在 Redis 中，暂时不需要重建索引
        log.warn("rebuildPostLogIndex 方法暂未实现");
        return 0;
    }
    
    /**
     * 从内容中提取帖子 ID
     */
    private String extractPostIdFromContent(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        
        // 尝试从内容中提取 postId
        // 例如："xxx（id:postId）发布了帖子 xxx（id:postId）"
        try {
            // 简单匹配最后一个 id:后面的内容
            int lastIdIndex = content.lastIndexOf("id:");
            if (lastIdIndex != -1) {
                int start = lastIdIndex + 3;
                int end = content.indexOf(")", start);
                if (end != -1) {
                    return content.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.error("提取 postId 失败：content={}, error={}", content, e.getMessage());
        }
        
        return null;
    }
    
    @Override
    public List<String> getOperationTypes() {
        try {
            // 从 Redis Set 中获取所有操作类型
            Set<String> typesSet = redisTemplate.opsForSet().members(OPERATION_TYPE_SET_KEY);
            
            if (typesSet == null || typesSet.isEmpty()) {
                // 如果 Set 为空，返回默认的操作类型
                return Arrays.asList("QUERY", "CREATE", "UPDATE", "DELETE");
            }
            
            // 转换为字符串列表并排序
            List<String> typeList = typesSet.stream()
                    .sorted()
                    .collect(Collectors.toList());
            
            System.out.println("获取到 " + typeList.size() + " 种操作类型：" + typeList);
            return typeList;
        } catch (Exception e) {
            log.error("获取操作类型失败：error={}", e.getMessage(), e);
            // 降级方案：返回默认操作类型
            return Arrays.asList("QUERY", "CREATE", "UPDATE", "DELETE");
        }
    }
}
