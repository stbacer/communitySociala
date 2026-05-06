package cn.edu.ccst.communitysocialmain.controller.resident;

import cn.edu.ccst.communitysocialmain.dto.ReportHandleDTO;
import cn.edu.ccst.communitysocialmain.dto.ReportSubmitDTO;
import cn.edu.ccst.communitysocialmain.service.ReportService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 居民端举报控制器
 */
@Slf4j
@RestController
@RequestMapping("/resident/report")
public class ResidentReportController {
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 提交举报
     */
    @PostMapping("/submit")
    public ResultVO<Void> submitReport(@Valid @RequestBody ReportSubmitDTO reportSubmitDTO,
                                     HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        log.info("收到举报请求: userId={}, targetType={}, targetId={}", userId, reportSubmitDTO.getTargetType(), reportSubmitDTO.getTargetId());
        reportService.submitReport(userId, reportSubmitDTO);
        log.info("举报提交成功: userId={}, targetId={}", userId, reportSubmitDTO.getTargetId());
        return ResultVO.success("举报提交成功", null);
    }
    
    /**
     * 获取用户举报历史
     */
    @GetMapping("/my-reports")
    public ResultVO<cn.edu.ccst.communitysocialmain.vo.PageVO<ReportService.ReportVO>> getUserReports(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        cn.edu.ccst.communitysocialmain.vo.PageVO<ReportService.ReportVO> reports = 
            reportService.getUserReports(userId, page, size);
        return ResultVO.success(reports);
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
}