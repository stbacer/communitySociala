package cn.edu.ccst.communitysocialmain.controller.sadmin;

import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.service.DataStatisticsService;
import cn.edu.ccst.communitysocialmain.service.UserService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.utils.PermissionUtil;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 数据统计报表控制器
 */
@Slf4j
@RestController
@RequestMapping("/sadmin/statistics")
public class StatisticsController {

    @Autowired
    private DataStatisticsService dataStatisticsService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private PermissionUtil permissionUtil;

    /**
     * 检查超级管理员权限
     */
    private void checkSAdminPermission(HttpServletRequest request) {
        String userIdStr = permissionUtil.getCurrentUserId(request);
        Long userId = Long.parseLong(userIdStr);
        permissionUtil.checkSuperAdminPermission(userId, "访问超级管理员统计接口");
    }

    /**
     * 获取平台总览统计数据
     */
    @GetMapping("/overview")
    public ResultVO<DataStatisticsService.PlatformOverviewVO> getPlatformOverview(HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            DataStatisticsService.PlatformOverviewVO overview = dataStatisticsService.getPlatformOverview();
            return ResultVO.success("获取平台总览成功", overview);
        } catch (Exception e) {
            log.error("获取平台总览统计数据失败", e);
            return ResultVO.error("获取平台总览失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/users")
    public ResultVO<DataStatisticsService.UserStatisticsVO> getUserStatistics(HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            DataStatisticsService.UserStatisticsVO userStats = dataStatisticsService.getUserStatistics();
            return ResultVO.success("获取用户统计成功", userStats);
        } catch (Exception e) {
            log.error("获取用户统计数据失败", e);
            return ResultVO.error("获取用户统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取内容统计信息
     */
    @GetMapping("/content")
    public ResultVO<DataStatisticsService.ContentStatisticsVO> getContentStatistics(HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            DataStatisticsService.ContentStatisticsVO contentStats = dataStatisticsService.getContentStatistics();
            return ResultVO.success("获取内容统计成功", contentStats);
        } catch (Exception e) {
            log.error("获取内容统计数据失败", e);
            return ResultVO.error("获取内容统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取互动统计信息
     */
    @GetMapping("/interaction")
    public ResultVO<DataStatisticsService.InteractionStatisticsVO> getInteractionStatistics(HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            DataStatisticsService.InteractionStatisticsVO interactionStats = dataStatisticsService.getInteractionStatistics();
            return ResultVO.success("获取互动统计成功", interactionStats);
        } catch (Exception e) {
            log.error("获取互动统计数据失败", e);
            return ResultVO.error("获取互动统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取时间趋势统计
     */
    @GetMapping("/trend")
    public ResultVO<DataStatisticsService.TimeTrendVO> getTimeTrendStatistics(
            @RequestParam(required = false) Integer days,
            HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            DataStatisticsService.TimeTrendVO trendStats = dataStatisticsService.getTimeTrendStatistics(days);
            return ResultVO.success("获取时间趋势统计成功", trendStats);
        } catch (Exception e) {
            log.error("获取时间趋势统计数据失败", e);
            return ResultVO.error("获取时间趋势统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取分类统计信息
     */
    @GetMapping("/category")
    public ResultVO<DataStatisticsService.CategoryStatisticsVO> getCategoryStatistics(HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            DataStatisticsService.CategoryStatisticsVO categoryStats = dataStatisticsService.getCategoryStatistics();
            return ResultVO.success("获取分类统计成功", categoryStats);
        } catch (Exception e) {
            log.error("获取分类统计数据失败", e);
            return ResultVO.error("获取分类统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取地域分布统计
     */
    @GetMapping("/location")
    public ResultVO<DataStatisticsService.LocationDistributionVO> getLocationDistribution(HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            DataStatisticsService.LocationDistributionVO locationStats = dataStatisticsService.getLocationDistribution();
            return ResultVO.success("获取地域分布统计成功", locationStats);
        } catch (Exception e) {
            log.error("获取地域分布统计数据失败", e);
            return ResultVO.error("获取地域分布统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取活跃度统计
     */
    @GetMapping("/activity")
    public ResultVO<DataStatisticsService.ActivityStatisticsVO> getActivityStatistics(HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            DataStatisticsService.ActivityStatisticsVO activityStats = dataStatisticsService.getActivityStatistics();
            return ResultVO.success("获取活跃度统计成功", activityStats);
        } catch (Exception e) {
            log.error("获取活跃度统计数据失败", e);
            return ResultVO.error("获取活跃度统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统性能统计
     */
    @GetMapping("/performance")
    public ResultVO<DataStatisticsService.PerformanceStatisticsVO> getPerformanceStatistics(HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            DataStatisticsService.PerformanceStatisticsVO perfStats = dataStatisticsService.getPerformanceStatistics();
            return ResultVO.success("获取性能统计成功", perfStats);
        } catch (Exception e) {
            log.error("获取性能统计数据失败", e);
            return ResultVO.error("获取性能统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有社区的统计数据列表
     */
    @GetMapping("/communities")
    public ResultVO<List<DataStatisticsService.CommunityStatisticsItemVO>> getCommunitiesStatistics(HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            List<DataStatisticsService.CommunityStatisticsItemVO> communitiesStats = 
                dataStatisticsService.getAllCommunitiesStatistics();
            return ResultVO.success("获取社区统计数据成功", communitiesStats);
        } catch (Exception e) {
            log.error("获取社区统计数据失败", e);
            return ResultVO.error("获取社区统计数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取指定社区的详细统计信息
     */
    @GetMapping("/community/detail")
    public ResultVO<DataStatisticsService.CommunityDetailVO> getCommunityDetail(
            @RequestParam String community,
            HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            DataStatisticsService.CommunityDetailVO detail = 
                dataStatisticsService.getCommunityDetail(community);
            return ResultVO.success("获取社区详情数据成功", detail);
        } catch (Exception e) {
            log.error("获取社区详情数据失败", e);
            return ResultVO.error("获取社区详情数据失败：" + e.getMessage());
        }
    }

    /**
     * 获取综合统计报告
     */
    @GetMapping("/report")
    public ResultVO<ComprehensiveReportVO> getComprehensiveReport(
            @RequestParam(required = false) Integer days,
            HttpServletRequest request) {
        try {
            checkSAdminPermission(request);
            
            ComprehensiveReportVO report = new ComprehensiveReportVO();
            report.setPlatformOverview(dataStatisticsService.getPlatformOverview());
            report.setUserStatistics(dataStatisticsService.getUserStatistics());
            report.setContentStatistics(dataStatisticsService.getContentStatistics());
            report.setInteractionStatistics(dataStatisticsService.getInteractionStatistics());
            report.setTimeTrend(dataStatisticsService.getTimeTrendStatistics(days));
            report.setCategoryStatistics(dataStatisticsService.getCategoryStatistics());
            report.setLocationDistribution(dataStatisticsService.getLocationDistribution());
            report.setActivityStatistics(dataStatisticsService.getActivityStatistics());
            report.setPerformanceStatistics(dataStatisticsService.getPerformanceStatistics());
            
            return ResultVO.success("获取综合统计报告成功", report);
        } catch (Exception e) {
            log.error("获取综合统计报告失败", e);
            return ResultVO.error("获取综合统计报告失败: " + e.getMessage());
        }
    }

    /**
     * 综合统计报告VO
     */
    public static class ComprehensiveReportVO {
        private DataStatisticsService.PlatformOverviewVO platformOverview;
        private DataStatisticsService.UserStatisticsVO userStatistics;
        private DataStatisticsService.ContentStatisticsVO contentStatistics;
        private DataStatisticsService.InteractionStatisticsVO interactionStatistics;
        private DataStatisticsService.TimeTrendVO timeTrend;
        private DataStatisticsService.CategoryStatisticsVO categoryStatistics;
        private DataStatisticsService.LocationDistributionVO locationDistribution;
        private DataStatisticsService.ActivityStatisticsVO activityStatistics;
        private DataStatisticsService.PerformanceStatisticsVO performanceStatistics;
        
        // Getters and Setters
        public DataStatisticsService.PlatformOverviewVO getPlatformOverview() { return platformOverview; }
        public void setPlatformOverview(DataStatisticsService.PlatformOverviewVO platformOverview) { this.platformOverview = platformOverview; }
        
        public DataStatisticsService.UserStatisticsVO getUserStatistics() { return userStatistics; }
        public void setUserStatistics(DataStatisticsService.UserStatisticsVO userStatistics) { this.userStatistics = userStatistics; }
        
        public DataStatisticsService.ContentStatisticsVO getContentStatistics() { return contentStatistics; }
        public void setContentStatistics(DataStatisticsService.ContentStatisticsVO contentStatistics) { this.contentStatistics = contentStatistics; }
        
        public DataStatisticsService.InteractionStatisticsVO getInteractionStatistics() { return interactionStatistics; }
        public void setInteractionStatistics(DataStatisticsService.InteractionStatisticsVO interactionStatistics) { this.interactionStatistics = interactionStatistics; }
        
        public DataStatisticsService.TimeTrendVO getTimeTrend() { return timeTrend; }
        public void setTimeTrend(DataStatisticsService.TimeTrendVO timeTrend) { this.timeTrend = timeTrend; }
        
        public DataStatisticsService.CategoryStatisticsVO getCategoryStatistics() { return categoryStatistics; }
        public void setCategoryStatistics(DataStatisticsService.CategoryStatisticsVO categoryStatistics) { this.categoryStatistics = categoryStatistics; }
        
        public DataStatisticsService.LocationDistributionVO getLocationDistribution() { return locationDistribution; }
        public void setLocationDistribution(DataStatisticsService.LocationDistributionVO locationDistribution) { this.locationDistribution = locationDistribution; }
        
        public DataStatisticsService.ActivityStatisticsVO getActivityStatistics() { return activityStatistics; }
        public void setActivityStatistics(DataStatisticsService.ActivityStatisticsVO activityStatistics) { this.activityStatistics = activityStatistics; }
        
        public DataStatisticsService.PerformanceStatisticsVO getPerformanceStatistics() { return performanceStatistics; }
        public void setPerformanceStatistics(DataStatisticsService.PerformanceStatisticsVO performanceStatistics) { this.performanceStatistics = performanceStatistics; }
    }
}