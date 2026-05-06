package cn.edu.ccst.communitysocialmain.controller.sadmin;

import cn.edu.ccst.communitysocialmain.entity.OperationLog;
import cn.edu.ccst.communitysocialmain.service.OperationLogService;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 超级管理员 - 操作日志管理接口
 */
@RestController
@RequestMapping("/sadmin/log")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SAdminLogController {
    
    @Autowired
    private OperationLogService operationLogService;
    
    /**
     * 分页查询操作日志列表（支持筛选）
     */
    @GetMapping("/list")
    public ResultVO<PageVO<OperationLog>> getLogList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
            
        try {
            if (size > 100) {
                size = 100;
            }
            
            System.out.println("=== 开始查询日志列表 ===");
            System.out.println("页码：" + page + ", 每页数量：" + size);
            System.out.println("筛选条件 - operator:" + operator + ", operationType:" + operationType + 
                             ", module:" + module + ", startTime:" + startTime + ", endTime:" + endTime);
            
            int offset = (page - 1) * size;
            
            List<OperationLog> logs = operationLogService.getGlobalLogs(operator, operationType, startTime, endTime, offset, size);
            System.out.println("查询到 " + logs.size() + " 条日志（已筛选）");
            
            long total = operationLogService.countGlobalLogs(operator, operationType, startTime, endTime);
            System.out.println("符合条件的日志总数：" + total);
                
            PageVO<OperationLog> pageVO = new PageVO<>(page, size, total, logs);
            return ResultVO.success(pageVO);
        } catch (Exception e) {
            System.err.println("查询日志失败：" + e.getMessage());
            e.printStackTrace();
            return ResultVO.error("查询日志失败：" + e.getMessage());
        }
    }
    
    /**
     * 查询日志详情
     */
    @GetMapping("/{logId}")
    public ResultVO<OperationLog> getLogDetail(@PathVariable String logId) {
        try {
            System.out.println("=== 开始查询日志详情，logId=" + logId);
            OperationLog log = operationLogService.getLogById(logId);
            if (log != null) {
                System.out.println("查询到日志：" + log.getLogId());
                return ResultVO.success(log);
            } else {
                System.out.println("日志不存在：" + logId);
                return ResultVO.error("日志不存在");
            }
        } catch (Exception e) {
            System.err.println("查询日志详情失败：" + e.getMessage());
            e.printStackTrace();
            return ResultVO.error("查询日志详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除单条日志
     */
    @DeleteMapping("/{logId}")
    public ResultVO<Void> deleteLog(@PathVariable String logId) {
        try {
            System.out.println("=== 开始删除日志，logId: " + logId);
            
            if (logId == null || logId.trim().isEmpty()) {
                return ResultVO.error("日志 ID 不能为空");
            }
            
            boolean success = operationLogService.deleteLog(logId);
            
            if (success) {
                System.out.println("日志删除成功：" + logId);
                return ResultVO.success("删除成功", null);
            } else {
                System.out.println("日志不存在或删除失败：" + logId);
                return ResultVO.error("日志不存在或删除失败");
            }
        } catch (Exception e) {
            System.err.println("删除日志失败：" + e.getMessage());
            e.printStackTrace();
            return ResultVO.error("删除失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量删除日志
     */
    @PostMapping("/batch-delete")
    public ResultVO<Map<String, Object>> batchDeleteLogs(@RequestBody List<String> logIds) {
        try {
            System.out.println("=== 开始批量删除日志，数量：" + (logIds != null ? logIds.size() : 0));
            
            if (logIds == null || logIds.isEmpty()) {
                return ResultVO.error("请选择要删除的日志");
            }
            
            int successCount = operationLogService.batchDeleteLogs(logIds);
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", logIds.size());
            result.put("successCount", successCount);
            result.put("failedCount", logIds.size() - successCount);
            
            System.out.println("批量删除完成，成功：" + successCount + ", 失败：" + (logIds.size() - successCount));
            return ResultVO.success(result);
        } catch (Exception e) {
            System.err.println("批量删除失败：" + e.getMessage());
            e.printStackTrace();
            return ResultVO.error("批量删除失败：" + e.getMessage());
        }
    }
    
    /**
     * 查询用户的操作日志
     */
    @GetMapping("/user/{userId}")
    public ResultVO<List<OperationLog>> getUserLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        try {
            List<OperationLog> logs = operationLogService.getUserLogs(String.valueOf(userId), (page - 1) * size, size);
            return ResultVO.success(logs);
        } catch (Exception e) {
            return ResultVO.error("查询用户日志失败：" + e.getMessage());
        }
    }
    
    /**
     * 查询用户的所有登录和注册日志
     */
    @GetMapping("/user/{userId}/login-register")
    public ResultVO<PageVO<OperationLog>> getUserLoginAndRegisterLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer size) {
        
        try {
            System.out.println("=== 开始查询用户" + userId + "的登录和注册日志 ===");
            List<OperationLog> logs = operationLogService.getUserLoginAndRegisterLogs(String.valueOf(userId), (page - 1) * size, size);
            System.out.println("查询到 " + logs.size() + " 条日志");
            
            // 统计总数（暂时用列表大小代替）
            long total = logs.size();
            
            PageVO<OperationLog> pageVO = new PageVO<>(page, size, total, logs);
            return ResultVO.success(pageVO);
        } catch (Exception e) {
            System.err.println("查询用户登录注册日志失败：" + e.getMessage());
            e.printStackTrace();
            return ResultVO.error("查询日志失败：" + e.getMessage());
        }
    }
    
    /**
     * 查询帖子的历史操作日志
     */
    @GetMapping("/post/{postId}")
    public ResultVO<PageVO<OperationLog>> getPostHistoryLogs(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer size) {
        
        try {
            System.out.println("=== 开始查询帖子" + postId + "的历史操作日志 ===");
            List<OperationLog> logs = operationLogService.getPostHistoryLogs(String.valueOf(postId), (page - 1) * size, size);
            System.out.println("查询到 " + logs.size() + " 条日志");
            
            long total = logs.size();
            
            PageVO<OperationLog> pageVO = new PageVO<>(page, size, total, logs);
            return ResultVO.success(pageVO);
        } catch (Exception e) {
            System.err.println("查询帖子历史日志失败：" + e.getMessage());
            e.printStackTrace();
            return ResultVO.error("查询帖子历史日志失败：" + e.getMessage());
        }
    }
    
    /**
     * 重建指定帖子的操作日志索引
     */
    @PostMapping("/post/{postId}/rebuild-index")
    public ResultVO<Integer> rebuildPostLogIndex(@PathVariable Long postId) {
        try {
            System.out.println("=== 开始重建帖子" + postId + "的操作日志索引 ===");
            int count = operationLogService.rebuildPostLogIndex(String.valueOf(postId));
            System.out.println("重建完成，共添加 " + count + " 条日志到帖子时间线");
            return ResultVO.success(count);
        } catch (Exception e) {
            System.err.println("重建索引失败：" + e.getMessage());
            e.printStackTrace();
            return ResultVO.error("重建索引失败：" + e.getMessage());
        }
    }
    
    /**
     * 查询指定模块的操作日志
     */
    @GetMapping("/module/{module}")
    public ResultVO<List<OperationLog>> getModuleLogs(
            @PathVariable String module,
            @RequestParam String date,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        try {
            List<OperationLog> logs = operationLogService.getModuleLogs(module, date, (page - 1) * size, size);
            return ResultVO.success(logs);
        } catch (Exception e) {
            return ResultVO.error("查询模块日志失败：" + e.getMessage());
        }
    }
    
    /**
     * 测试接口：创建测试日志数据
     */
    @PostMapping("/test/create-sample")
    public ResultVO<String> createSampleLogs() {
        try {
            System.out.println("=== 开始创建测试日志数据 ===");
            
            for (int i = 1; i <= 5; i++) {
                OperationLog testLog = new OperationLog();
                testLog.setLogId("test-log-" + i);
                testLog.setUserId((long)i);
                testLog.setNickname("测试用户" + i);
                testLog.setOperatorName("测试操作员" + i);
                testLog.setOperation("TEST");
                testLog.setContent("这是第" + i + "条测试日志");
                testLog.setModule("TEST_MODULE");
                testLog.setSubModule("TEST_SUB");
                testLog.setClientType(1);
                testLog.setDuration((long)(i * 100));
                testLog.setCreateTime(java.time.LocalDateTime.now().minusMinutes(i));
                testLog.setTimestamp(testLog.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                
                operationLogService.logSuccess(testLog);
                System.out.println("创建测试日志：" + testLog.getLogId());
            }
            
            System.out.println("测试日志创建完成");
            return ResultVO.success("成功创建 5 条测试日志", null);
        } catch (Exception e) {
            System.err.println("创建测试日志失败：" + e.getMessage());
            e.printStackTrace();
            return ResultVO.error("创建测试日志失败：" + e.getMessage());
        }
    }
    
    /**
     * 统计今日操作数量
     */
    @GetMapping("/stats/today")
    public ResultVO<Map<String, Object>> getTodayStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalOperations", operationLogService.countGlobalTodayOperations());
            // TODO: 添加更多统计数据
            
            return ResultVO.success(stats);
        } catch (Exception e) {
            return ResultVO.error("查询统计失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取所有已记录的操作类型（从 Redis Set 中读取）
     */
    @GetMapping("/operation-types")
    public ResultVO<List<String>> getOperationTypes() {
        try {
            List<String> types = operationLogService.getOperationTypes();
            return ResultVO.success(types);
        } catch (Exception e) {
            System.err.println("获取操作类型失败：" + e.getMessage());
            e.printStackTrace();
            return ResultVO.error("获取操作类型失败：" + e.getMessage());
        }
    }
}
