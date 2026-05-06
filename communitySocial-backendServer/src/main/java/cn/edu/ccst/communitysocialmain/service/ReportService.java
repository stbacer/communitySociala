package cn.edu.ccst.communitysocialmain.service;

import cn.edu.ccst.communitysocialmain.dto.ReportHandleDTO;
import cn.edu.ccst.communitysocialmain.dto.ReportSubmitDTO;
import cn.edu.ccst.communitysocialmain.vo.PageVO;

import java.util.List;

/**
 * 举报服务接口
 */
public interface ReportService {
    
    /**
     * 提交举报
     */
    void submitReport(Long reporterId, ReportSubmitDTO reportSubmitDTO);
    
    /**
     * 获取用户举报历史
     */
    PageVO<ReportVO> getUserReports(Long userId, Integer page, Integer size);
    
    /**
     * 获取待处理举报列表（管理员用）
     */
    PageVO<ReportVO> getPendingReports(Integer page, Integer size);
    
    /**
     * 处理举报（管理员用）
     */
    void handleReport(Long handlerId, ReportHandleDTO reportHandleDTO);
    
    /**
     * 获取所有举报列表（管理员用）
     */
    PageVO<ReportVO> getAllReports(Integer page, Integer size);
    
    /**
     * 根据社区获取举报列表（社区管理员用）
     */
    PageVO<ReportVO> getReportsByCommunity(String community, Integer page, Integer size);
    
    /**
     * 根据状态获取举报列表（管理员用）
     */
    PageVO<ReportVO> getReportsByStatus(Integer status, Integer page, Integer size);
    
    /**
     * 根据社区和状态获取举报列表（社区管理员用）
     */
    PageVO<ReportVO> getReportsByCommunityAndStatus(String community, Integer status, Integer page, Integer size);
    
    /**
     * 搜索举报（管理员用）
     */
    PageVO<ReportVO> searchReports(String keyword, Integer page, Integer size);
    
    /**
     * 获取举报详情（管理员用）
     */
    ReportVO getReportDetail(String reportId);
    
    /**
     * 获取总举报数统计
     */
    Long getTotalReportCount();
    
    /**
     * 获取待处理举报数统计
     */
    Long getPendingReportCount();
    
    /**
     * 获取已处理举报数统计
     */
    Long getHandledReportCount();
    
    /**
     * 获取已驳回举报数统计
     */
    Long getRejectedReportCount();
    
    /**
     * 批量处理举报
     */
    void batchHandleReports(Long handlerId, List<ReportHandleDTO> reportHandleDTOs);
    
    /**
     * 导出举报数据
     */
    List<ReportExportVO> exportReports(List<String> reportIds);
    
    /**
     * 举报导出VO
     */
    class ReportExportVO {
        private Long reportId;
        private String reporterName;
        private String targetTypeDesc;
        private String targetContent;
        private String reason;
        private String statusDesc;
        private String handlerName;
        private String handleResult;
        private String reportTime;
        private String handleTime;
        
        // Getters and Setters
        public Long getReportId() { return reportId; }
        public void setReportId(Long reportId) { this.reportId = reportId; }
        
        public String getReporterName() { return reporterName; }
        public void setReporterName(String reporterName) { this.reporterName = reporterName; }
        
        public String getTargetTypeDesc() { return targetTypeDesc; }
        public void setTargetTypeDesc(String targetTypeDesc) { this.targetTypeDesc = targetTypeDesc; }
        
        public String getTargetContent() { return targetContent; }
        public void setTargetContent(String targetContent) { this.targetContent = targetContent; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getStatusDesc() { return statusDesc; }
        public void setStatusDesc(String statusDesc) { this.statusDesc = statusDesc; }
        
        public String getHandlerName() { return handlerName; }
        public void setHandlerName(String handlerName) { this.handlerName = handlerName; }
        
        public String getHandleResult() { return handleResult; }
        public void setHandleResult(String handleResult) { this.handleResult = handleResult; }
        
        public String getReportTime() { return reportTime; }
        public void setReportTime(String reportTime) { this.reportTime = reportTime; }
        
        public String getHandleTime() { return handleTime; }
        public void setHandleTime(String handleTime) { this.handleTime = handleTime; }
    }
    
    /**
     * 举报VO
     */
    class ReportVO {
        private Long reportId;        // 举报ID
        private Long reporterId;      // 举报人ID
        private String reporterName;    // 举报人昵称
        private Integer targetType;     // 目标类型
        private Long targetId;        // 目标ID
        private String targetContent;   // 被举报内容摘要
        private String reason;          // 举报原因
        private Integer status;         // 处理状态
        private Long handlerId;       // 处理人ID
        private String handlerName;     // 处理人昵称
        private String handleResult;    // 处理结果
        private String reportTime;      // 举报时间
        private String handleTime;      // 处理时间
        
        // Getters and Setters
        public Long getReportId() { return reportId; }
        public void setReportId(Long reportId) { this.reportId = reportId; }
        
        public Long getReporterId() { return reporterId; }
        public void setReporterId(Long reporterId) { this.reporterId = reporterId; }
        
        public String getReporterName() { return reporterName; }
        public void setReporterName(String reporterName) { this.reporterName = reporterName; }
        
        public Integer getTargetType() { return targetType; }
        public void setTargetType(Integer targetType) { this.targetType = targetType; }
        
        public Long getTargetId() { return targetId; }
        public void setTargetId(Long targetId) { this.targetId = targetId; }
        
        public String getTargetContent() { return targetContent; }
        public void setTargetContent(String targetContent) { this.targetContent = targetContent; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        
        public Long getHandlerId() { return handlerId; }
        public void setHandlerId(Long handlerId) { this.handlerId = handlerId; }
        
        public String getHandlerName() { return handlerName; }
        public void setHandlerName(String handlerName) { this.handlerName = handlerName; }
        
        public String getHandleResult() { return handleResult; }
        public void setHandleResult(String handleResult) { this.handleResult = handleResult; }
        
        public String getReportTime() { return reportTime; }
        public void setReportTime(String reportTime) { this.reportTime = reportTime; }
        
        public String getHandleTime() { return handleTime; }
        public void setHandleTime(String handleTime) { this.handleTime = handleTime; }
    }
}