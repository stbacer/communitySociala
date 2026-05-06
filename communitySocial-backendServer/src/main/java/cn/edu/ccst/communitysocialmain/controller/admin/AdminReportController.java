package cn.edu.ccst.communitysocialmain.controller.admin;

import cn.edu.ccst.communitysocialmain.dto.ReportHandleDTO;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.service.ReportService;
import cn.edu.ccst.communitysocialmain.service.UserService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * 管理员举报管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/report")
public class AdminReportController {
    
    @Autowired
    private ReportService reportService;
        
    @Autowired
    private JwtUtil jwtUtil;
        
    @Autowired
    private UserService userService;
        
    /**
     * 获取举报列表（支持状态筛选）
     */
    @GetMapping("/list")
    public ResultVO<PageVO<ReportService.ReportVO>> getReportList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        try {
            Long adminUserId = getCurrentUserId(request);
            User adminUser = userService.getUserById(adminUserId);
                
            PageVO<ReportService.ReportVO> reports;
            if (adminUser != null && adminUser.getUserRole() == 2 && adminUser.getCommunity() != null) {
                // 社区管理员只能看到本社区的举报
                if (status != null) {
                    reports = reportService.getReportsByCommunityAndStatus(adminUser.getCommunity(), status, page, size);
                } else if (keyword != null && !keyword.trim().isEmpty()) {
                    // 搜索暂时使用内存过滤
                    reports = reportService.getReportsByCommunity(adminUser.getCommunity(), page, size);
                } else {
                    reports = reportService.getReportsByCommunity(adminUser.getCommunity(), page, size);
                }
                log.info("社区管理员{}查看本社区{}的举报列表", adminUserId, adminUser.getCommunity());
            } else {
                // 超级管理员可以看到所有举报
                if (status != null) {
                    reports = reportService.getReportsByStatus(status, page, size);
                } else if (keyword != null && !keyword.trim().isEmpty()) {
                    reports = reportService.searchReports(keyword, page, size);
                } else {
                    reports = reportService.getAllReports(page, size);
                }
                log.info("超级管理员{}查看所有举报列表", adminUserId);
            }
                
            return ResultVO.success("获取举报列表成功", reports);
        } catch (Exception e) {
            log.error("获取举报列表失败", e);
            return ResultVO.error("获取举报列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取待处理举报列表
     */
    @GetMapping("/pending")
    public ResultVO<PageVO<ReportService.ReportVO>> getPendingReports(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            PageVO<ReportService.ReportVO> reports = reportService.getPendingReports(page, size);
            return ResultVO.success("获取待处理举报成功", reports);
        } catch (Exception e) {
            log.error("获取待处理举报失败", e);
            return ResultVO.error("获取待处理举报失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理举报
     */
    @PutMapping("/handle")
    public ResultVO<Void> handleReport(@Valid @RequestBody ReportHandleDTO reportHandleDTO,
                                     HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            Long adminUserId = getCurrentUserId(request);
            reportService.handleReport(adminUserId, reportHandleDTO);
            return ResultVO.success("举报处理成功", null);
        } catch (Exception e) {
            log.error("处理举报失败", e);
            return ResultVO.error("处理举报失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取举报统计信息
     */
    @GetMapping("/statistics")
    public ResultVO<ReportStatisticsVO> getReportStatistics(HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            
            ReportStatisticsVO stats = new ReportStatisticsVO();
            
            // 获取各类统计数据
            Long totalReports = reportService.getTotalReportCount();
            Long pendingReports = reportService.getPendingReportCount();
            Long handledReports = reportService.getHandledReportCount();
            Long rejectedReports = reportService.getRejectedReportCount();
            
            stats.setTotalReports(totalReports);
            stats.setPendingReports(pendingReports);
            stats.setHandledReports(handledReports);
            stats.setRejectedReports(rejectedReports);
            
            return ResultVO.success("获取统计信息成功", stats);
        } catch (Exception e) {
            log.error("获取举报统计信息失败", e);
            return ResultVO.error("获取统计信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量处理举报
     */
    @PutMapping("/batch-handle")
    public ResultVO<Void> batchHandleReports(@Valid @RequestBody List<ReportHandleDTO> reportHandleDTOs,
                                           HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            Long adminUserId = getCurrentUserId(request);
            reportService.batchHandleReports(adminUserId, reportHandleDTOs);
            return ResultVO.success("批量处理举报成功", null);
        } catch (Exception e) {
            log.error("批量处理举报失败", e);
            return ResultVO.error("批量处理举报失败: " + e.getMessage());
        }
    }
    
    /**
     * 导出举报数据
     */
    @PostMapping("/export")
    public ResultVO<List<ReportService.ReportExportVO>> exportReports(@RequestBody List<String> reportIds,
                                                                     HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            List<ReportService.ReportExportVO> exportData = reportService.exportReports(reportIds);
            return ResultVO.success("导出举报数据成功", exportData);
        } catch (Exception e) {
            log.error("导出举报数据失败", e);
            return ResultVO.error("导出举报数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取举报详情
     */
    @GetMapping("/{reportId}")
    public ResultVO<ReportService.ReportVO> getReportDetail(@PathVariable Long reportId,
                                                          HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            ReportService.ReportVO report = reportService.getReportDetail(String.valueOf(reportId));
            return ResultVO.success("获取举报详情成功", report);
        } catch (Exception e) {
            log.error("获取举报详情失败", e);
            return ResultVO.error("获取举报详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查管理员权限
     */
    private void checkAdminPermission(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
    }
    
    /**
     * 从请求头中获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return Long.parseLong(jwtUtil.getUserIdFromToken(token));
        }
        throw new RuntimeException("用户未登录");
    }
    
    /**
     * 举报统计信息VO
     */
    public static class ReportStatisticsVO {
        private Long totalReports;
        private Long pendingReports;
        private Long handledReports;
        private Long rejectedReports;
        
        // Getters and Setters
        public Long getTotalReports() { return totalReports; }
        public void setTotalReports(Long totalReports) { this.totalReports = totalReports; }
        
        public Long getPendingReports() { return pendingReports; }
        public void setPendingReports(Long pendingReports) { this.pendingReports = pendingReports; }
        
        public Long getHandledReports() { return handledReports; }
        public void setHandledReports(Long handledReports) { this.handledReports = handledReports; }
        
        public Long getRejectedReports() { return rejectedReports; }
        public void setRejectedReports(Long rejectedReports) { this.rejectedReports = rejectedReports; }
    }
}