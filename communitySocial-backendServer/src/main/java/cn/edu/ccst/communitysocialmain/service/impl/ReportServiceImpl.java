package cn.edu.ccst.communitysocialmain.service.impl;

import cn.edu.ccst.communitysocialmain.dto.ReportHandleDTO;
import cn.edu.ccst.communitysocialmain.dto.ReportSubmitDTO;
import cn.edu.ccst.communitysocialmain.entity.OperationLog;
import cn.edu.ccst.communitysocialmain.entity.Post;
import cn.edu.ccst.communitysocialmain.entity.Report;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.entity.Comment;
import cn.edu.ccst.communitysocialmain.enums.ReportStatusEnum;
import cn.edu.ccst.communitysocialmain.enums.ReportTypeEnum;
import cn.edu.ccst.communitysocialmain.mapper.PostMapper;
import cn.edu.ccst.communitysocialmain.mapper.ReportMapper;
import cn.edu.ccst.communitysocialmain.mapper.UserMapper;
import cn.edu.ccst.communitysocialmain.mapper.CommentMapper;
import cn.edu.ccst.communitysocialmain.service.MessageService;
import cn.edu.ccst.communitysocialmain.service.OperationLogService;
import cn.edu.ccst.communitysocialmain.service.ReportService;
import cn.edu.ccst.communitysocialmain.utils.CommonUtil;
import cn.edu.ccst.communitysocialmain.utils.SnowflakeIdGenerator;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 举报服务实现类
 */
@Slf4j
@Service
public class ReportServiceImpl implements ReportService {
    
    @Autowired
    private ReportMapper reportMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PostMapper postMapper;
    
    @Autowired
    private CommentMapper commentMapper;
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private MessageService messageService;
    
    @Override
    @Transactional
    public void submitReport(Long reporterId, ReportSubmitDTO reportSubmitDTO) {
        long startTime = System.currentTimeMillis();
        
        // 验证举报人是否存在
        User reporter = userMapper.selectById(reporterId);
        if (reporter == null) {
            throw new RuntimeException("举报人不存在");
        }
        
        // 验证目标是否存在
        validateTargetExists(reportSubmitDTO.getTargetType(), reportSubmitDTO.getTargetId());
        
        // 检查是否已经举报过相同目标
        List<Report> existingReports = reportMapper.selectByTarget(
            reportSubmitDTO.getTargetType(), 
            reportSubmitDTO.getTargetId(), 
            0, 100
        );
        
        boolean alreadyReported = existingReports.stream()
            .anyMatch(report -> report.getReporterId().equals(reporterId) && 
                       ReportStatusEnum.isPending(report.getStatus()));
            
        if (alreadyReported) {
            throw new RuntimeException("您已经举报过该内容，请勿重复举报");
        }
        
        // 创建举报记录
        Report report = new Report();
        report.setReportId(SnowflakeIdGenerator.nextId());
        report.setReporterId(reporterId);
        report.setTargetType(reportSubmitDTO.getTargetType());
        report.setTargetId(reportSubmitDTO.getTargetId());
        report.setReason(StringUtils.hasText(reportSubmitDTO.getReason()) ? 
            reportSubmitDTO.getReason() : "其他违规");
        report.setStatus(ReportStatusEnum.PENDING.getCode()); // 待处理
        
        // 插入举报记录
        int result = reportMapper.insert(report);
        if (result <= 0) {
            throw new RuntimeException("举报失败");
        }
        
        log.info("用户{}举报了类型为{}的目标{}，原因：{}", 
            reporterId, reportSubmitDTO.getTargetType(), reportSubmitDTO.getTargetId(), reportSubmitDTO.getReason());
        
        // 发送系统消息通知被举报内容的作者（如果是帖子或评论）
        try {
            String targetContent = getTargetContentSummary(reportSubmitDTO.getTargetType(), reportSubmitDTO.getTargetId());
            String targetTypeText = getTargetTypeName(reportSubmitDTO.getTargetType());
            
            // 获取被举报内容的作者 ID
            Long targetAuthorId = null;
            if (reportSubmitDTO.getTargetType() == 1) { // 帖子
                Post post = postMapper.selectById(reportSubmitDTO.getTargetId());
                if (post != null) {
                    targetAuthorId = post.getUserId();
                }
            }
            // 评论类型暂时不处理，需要 CommentMapper 支持
            
            // 如果举报的是他人的内容，发送通知
            if (targetAuthorId != null && !targetAuthorId.equals(reporterId)) {
                String messageContent = String.format("您发布的%s（%s）被用户%s举报了，举报原因：%s。管理员将尽快处理。", 
                    targetTypeText,
                    targetContent.length() > 20 ? targetContent.substring(0, 20) + "..." : targetContent,
                    reporter.getNickname(),
                    report.getReason());
                messageService.sendSystemMessage(targetAuthorId, messageContent, 1);
            }
        } catch (Exception e) {
            log.warn("发送举报通知失败：{}", e.getMessage());
        }
        
        // 记录操作日志
        try {
            User user = userMapper.selectById(reporterId);
            OperationLog log = new OperationLog();
            log.setUserId(reporterId);
            log.setNickname(user != null ? user.getNickname() : "");
            log.setOperatorName(user != null ? user.getNickname() : "未知用户");
            log.setOperation("REPORT"); // 举报操作
            log.setContent(String.format("%s（id:%s）举报了%s(id:%s)，原因：%s", 
                user != null ? user.getNickname() : "未知用户", 
                reporterId, 
                getTargetTypeName(reportSubmitDTO.getTargetType()),
                reportSubmitDTO.getTargetId(),
                report.getReason()));
            log.setModule("REPORT");
            log.setSubModule("SUBMIT");
            log.setClientType(3); // 居民端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(log);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            log.warn("记录举报日志失败：{}", e.getMessage());
        }
    }
    
    @Override
    public PageVO<ReportVO> getUserReports(Long userId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        
        // 查询用户举报记录
        List<Report> reports = reportMapper.selectByReporterId(String.valueOf(userId), offset, size);
        Long total = reportMapper.countByReporterId(String.valueOf(userId));
        
        // 转换为VO
        List<ReportVO> reportVOs = reports.stream()
            .map(this::convertToReportVO)
            .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, reportVOs);
    }
    
    @Override
    public PageVO<ReportVO> getPendingReports(Integer page, Integer size) {
        int offset = (page - 1) * size;
        
        // 查询待处理举报
        List<Report> reports = reportMapper.selectPendingReports(offset, size);
        Long total = reportMapper.countPendingReports();
        
        // 转换为VO
        List<ReportVO> reportVOs = reports.stream()
            .map(this::convertToReportVO)
            .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, reportVOs);
    }
    
    @Override
    @Transactional
    public void handleReport(Long handlerId, ReportHandleDTO reportHandleDTO) {
        long startTime = System.currentTimeMillis();
        
        // 验证处理人权限（这里简化处理，实际应该验证管理员权限）
        User handler = userMapper.selectById(handlerId);
        if (handler == null) {
            throw new RuntimeException("处理人不存在");
        }
        
        // 验证举报记录存在
        Report report = reportMapper.selectById(reportHandleDTO.getReportId());
        if (report == null) {
            throw new RuntimeException("举报记录不存在");
        }
        
        // 更新举报状态
        reportMapper.updateStatus(
            reportHandleDTO.getReportId(),
            reportHandleDTO.getStatus(),
            String.valueOf(handlerId),
            reportHandleDTO.getHandleResult()
        );
        
        log.info("管理员{}处理了举报{}，处理结果：{}，说明：{}", 
            handlerId, reportHandleDTO.getReportId(), 
            reportHandleDTO.getStatus(), reportHandleDTO.getHandleResult());
        
        // 如果处理结果为"删除处理"（status=1），则删除对应的帖子或评论
        if (reportHandleDTO.getStatus() == 1) {
            deleteTargetContent(report.getTargetType(), report.getTargetId(), handlerId);
        }
        
        // 发送系统消息通知举报人处理结果
        try {
            Long reporterId = report.getReporterId();
            User reporter = userMapper.selectById(reporterId);
            
            String resultText = reportHandleDTO.getStatus() == 1 ? "已受理并删除相关内容" : "未予受理";
            String targetTypeText = getTargetTypeName(report.getTargetType());
            String targetContent = getTargetContentSummary(report.getTargetType(), report.getTargetId());
            
            String messageContent = String.format("您举报的%s（%s）处理结果为：%s。处理说明：%s", 
                targetTypeText,
                targetContent.length() > 20 ? targetContent.substring(0, 20) + "..." : targetContent,
                resultText,
                reportHandleDTO.getHandleResult() != null ? reportHandleDTO.getHandleResult() : "无");
            
            messageService.sendSystemMessage(reporterId, messageContent, 1);
        } catch (Exception e) {
            log.warn("发送举报处理结果通知失败：{}", e.getMessage());
        }
        
        // 如果被删除，发送通知给被举报内容的发布人
        if (reportHandleDTO.getStatus() == 1) {
            try {
                Long targetOwnerId = getTargetContentOwnerId(report.getTargetType(), report.getTargetId());
                if (targetOwnerId != null && !targetOwnerId.equals(report.getReporterId())) {
                    User targetOwner = userMapper.selectById(targetOwnerId);
                    
                    String targetTypeText = getTargetTypeName(report.getTargetType());
                    String messageContent = String.format("您发布的%s因被用户举报且经管理员核实违规，已被删除。如有疑问请联系管理员。", targetTypeText);
                    
                    messageService.sendSystemMessage(targetOwnerId, messageContent, 1);
                    log.info("已发送删除通知给被举报内容发布人：{}", targetOwnerId);
                }
            } catch (Exception e) {
                log.warn("发送删除通知给被举报内容发布人失败：{}", e.getMessage());
            }
        }
        
        // 记录操作日志
        try {
            OperationLog log = new OperationLog();
            log.setUserId(handlerId);
            log.setNickname(handler != null ? handler.getNickname() : "");
            log.setOperatorName(handler != null ? handler.getNickname() : "管理员");
            log.setOperation("实名审核"); // 审核操作
            
            // 根据处理状态生成不同的描述
            String actionDesc = reportHandleDTO.getStatus() == 1 ? "通过" : "驳回";
            log.setContent(String.format("%s（id:%s）%s了举报%s(id:%s)，处理说明：%s", 
                handler != null ? handler.getNickname() : "管理员", 
                handlerId,
                actionDesc,
                report.getReason(),
                report.getReportId(),
                reportHandleDTO.getHandleResult() != null ? reportHandleDTO.getHandleResult() : "无"));
            
            log.setModule("REPORT");
            log.setSubModule("HANDLE");
            log.setClientType(2); // 社区管理员端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            System.out.println("=== 准备记录举报处理日志 ===");
            System.out.println("operation 值：" + log.getOperation());
            System.out.println("content 值：" + log.getContent());
            System.out.println("处理结果：" + (reportHandleDTO.getStatus() == 1 ? "通过" : "驳回"));
            
            operationLogService.logSuccess(log);
            System.out.println("举报处理日志记录成功");
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            System.err.println("记录举报处理日志失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public PageVO<ReportVO> getAllReports(Integer page, Integer size) {
        int offset = (page - 1) * size;
            
        // 查询所有举报
        List<Report> reports = reportMapper.selectAll(offset, size);
        Long total = reportMapper.countAll();
            
        // 转换为 VO
        List<ReportVO> reportVOs = reports.stream()
            .map(this::convertToReportVO)
            .collect(Collectors.toList());
            
        return new PageVO<>(page, size, total, reportVOs);
    }
        
    @Override
    public PageVO<ReportVO> getReportsByCommunity(String community, Integer page, Integer size) {
        int offset = (page - 1) * size;
            
        // 使用社区过滤的查询方法（需要添加到 Mapper）
        // 这里暂时使用内存过滤，后续可以优化为 SQL 过滤
        List<Report> allReports = reportMapper.selectAll(0, Integer.MAX_VALUE);
            
        // 根据举报目标类型获取对应的社区信息进行过滤
        List<Report> filteredReports = allReports.stream()
            .filter(report -> isReportInCommunity(report, community))
            .skip(offset)
            .limit(size)
            .collect(Collectors.toList());
            
        Long total = allReports.stream()
            .filter(report -> isReportInCommunity(report, community))
            .count();
            
        // 转换为 VO
        List<ReportVO> reportVOs = filteredReports.stream()
            .map(this::convertToReportVO)
            .collect(Collectors.toList());
            
        return new PageVO<>(page, size, total, reportVOs);
    }
        
    @Override
    public PageVO<ReportVO> getReportsByStatus(Integer status, Integer page, Integer size) {
        int offset = (page - 1) * size;
            
        // 根据状态查询举报
        List<Report> reports = reportMapper.selectByStatus(status, offset, size);
        Long total = reportMapper.countByStatus(status);
            
        // 转换为 VO
        List<ReportVO> reportVOs = reports.stream()
            .map(this::convertToReportVO)
            .collect(Collectors.toList());
            
        return new PageVO<>(page, size, total, reportVOs);
    }
        
    @Override
    public PageVO<ReportVO> getReportsByCommunityAndStatus(String community, Integer status, Integer page, Integer size) {
        int offset = (page - 1) * size;
            
        // 使用社区和状态过滤的查询方法
        List<Report> allReports = reportMapper.selectByStatus(status, 0, Integer.MAX_VALUE);
            
        // 根据举报目标类型获取对应的社区信息进行过滤
        List<Report> filteredReports = allReports.stream()
            .filter(report -> report.getStatus().equals(status) && isReportInCommunity(report, community))
            .skip(offset)
            .limit(size)
            .collect(Collectors.toList());
            
        Long total = allReports.stream()
            .filter(report -> report.getStatus().equals(status) && isReportInCommunity(report, community))
            .count();
            
        // 转换为 VO
        List<ReportVO> reportVOs = filteredReports.stream()
            .map(this::convertToReportVO)
            .collect(Collectors.toList());
            
        return new PageVO<>(page, size, total, reportVOs);
    }
    
    @Override
    public PageVO<ReportVO> searchReports(String keyword, Integer page, Integer size) {
        int offset = (page - 1) * size;
        
        // 搜索举报
        List<Report> reports = reportMapper.selectByKeyword(keyword, offset, size);
        Long total = reportMapper.countByKeyword(keyword);
        
        // 转换为VO
        List<ReportVO> reportVOs = reports.stream()
            .map(this::convertToReportVO)
            .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, reportVOs);
    }
    
    @Override
    public ReportVO getReportDetail(String reportId) {
        Report report = reportMapper.selectById(Long.parseLong(reportId));
        if (report == null) {
            throw new RuntimeException("举报记录不存在");
        }
        return convertToReportVO(report);
    }
    
    @Override
    public Long getTotalReportCount() {
        return reportMapper.countAll();
    }
    
    @Override
    public Long getPendingReportCount() {
        return reportMapper.countPendingReports();
    }
    
    @Override
    public Long getHandledReportCount() {
        return reportMapper.countByStatus(1);
    }
    
    @Override
    public Long getRejectedReportCount() {
        return reportMapper.countByStatus(ReportStatusEnum.REJECTED.getCode());
    }
    
    @Override
    @Transactional
    public void batchHandleReports(Long handlerId, List<ReportHandleDTO> reportHandleDTOs) {
        // 验证处理人权限
        User handler = userMapper.selectById(handlerId);
        if (handler == null) {
            throw new RuntimeException("处理人不存在");
        }
        
        // 批量处理举报
        for (ReportHandleDTO dto : reportHandleDTOs) {
            Report report = reportMapper.selectById(dto.getReportId());
            if (report == null) {
                throw new RuntimeException("举报记录不存在: " + dto.getReportId());
            }
            
            reportMapper.updateStatus(
                dto.getReportId(),
                dto.getStatus(),
                String.valueOf(handlerId),
                dto.getHandleResult()
            );
        }
        
        log.info("管理员{}批量处理了{}条举报记录", handlerId, reportHandleDTOs.size());
    }
    
    @Override
    public List<ReportExportVO> exportReports(List<String> reportIds) {
        List<ReportExportVO> exportVOs = new ArrayList<>();
        
        for (String reportId : reportIds) {
            Report report = reportMapper.selectById(Long.parseLong(reportId));
            if (report != null) {
                ReportExportVO exportVO = convertToReportExportVO(report);
                exportVOs.add(exportVO);
            }
        }
        
        return exportVOs;
    }
    
    /**
     * 判断举报是否属于指定社区
     */
    private boolean isReportInCommunity(Report report, String community) {
        try {
            // 根据举报目标类型获取对应的社区信息
            ReportTypeEnum typeEnum = ReportTypeEnum.fromCode(report.getTargetType());
            if (typeEnum == null) {
                return false;
            }
            
            switch (typeEnum) {
                case POST:
                    // 帖子举报：查询帖子的作者所在社区
                    Post post = postMapper.selectById(report.getTargetId());
                    if (post != null) {
                        User postAuthor = userMapper.selectById(post.getUserId());
                        return postAuthor != null && community.equals(postAuthor.getCommunity());
                    }
                    break;
                case COMMENT:
                    // 评论举报：查询评论的作者所在社区（需要 CommentMapper 支持）
                    // 暂时返回 true，后续补充
                    return true;
                case USER:
                    // 用户举报：查询被举报用户所在社区
                    User reportedUser = userMapper.selectById(report.getTargetId());
                    return reportedUser != null && community.equals(reportedUser.getCommunity());
                default:
                    return false;
            }
        } catch (Exception e) {
            log.error("判断举报所属社区失败", e);
            return false;
        }
        return false;
    }
    
    /**
     * 验证举报目标是否存在
     */
    private void validateTargetExists(Integer targetType, Long targetId) {
        ReportTypeEnum typeEnum = ReportTypeEnum.fromCode(targetType);
        if (typeEnum == null) {
            throw new RuntimeException("不支持的举报目标类型");
        }
        
        switch (typeEnum) {
            case POST:
                Post post = postMapper.selectById(targetId);
                if (post == null) {
                    throw new RuntimeException("被举报的帖子不存在");
                }
                break;
            case COMMENT:
                // 评论验证需要CommentMapper支持
                // 这里暂时不做验证，后续补充
                break;
            case USER:
                User user = userMapper.selectById(targetId);
                if (user == null) {
                    throw new RuntimeException("被举报的用户不存在");
                }
                break;
        }
    }
    
    /**
     * 将Report实体转换为ReportVO
     */
    private ReportVO convertToReportVO(Report report) {
        ReportVO vo = new ReportVO();
        BeanUtils.copyProperties(report, vo);
        
        // 设置举报人信息
        User reporter = userMapper.selectById(report.getReporterId());
        if (reporter != null) {
            vo.setReporterName(reporter.getNickname());
        }
        
        // 设置处理人信息
        if (report.getHandlerId() != null) {
            User handler = userMapper.selectById(report.getHandlerId());
            if (handler != null) {
                vo.setHandlerName(handler.getNickname());
            }
        }
        
        // 设置被举报内容摘要
        vo.setTargetContent(getTargetContentSummary(report.getTargetType(), report.getTargetId()));
        
        // 格式化时间
        if (report.getReportTime() != null) {
            vo.setReportTime(report.getReportTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (report.getHandleTime() != null) {
            vo.setHandleTime(report.getHandleTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        return vo;
    }
    
    /**
     * 获取被举报内容摘要
     */
    private String getTargetContentSummary(Integer targetType, Long targetId) {
        try {
            ReportTypeEnum typeEnum = ReportTypeEnum.fromCode(targetType);
            if (typeEnum == null) {
                return "未知类型内容";
            }
            
            switch (typeEnum) {
                case POST:
                    Post post = postMapper.selectById(targetId);
                    if (post != null) {
                        String title = StringUtils.hasText(post.getTitle()) ? post.getTitle() : "";
                        String content = StringUtils.hasText(post.getContent()) ? 
                            post.getContent().substring(0, Math.min(50, post.getContent().length())) : "";
                        return title + (StringUtils.hasText(title) && StringUtils.hasText(content) ? " - " : "") + content;
                    }
                    break;
                case COMMENT:
                    // 需要CommentMapper支持
                    return "评论内容";
                case USER:
                    User user = userMapper.selectById(targetId);
                    if (user != null) {
                        return "用户：" + user.getNickname();
                    }
                    break;
            }
        } catch (Exception e) {
            log.warn("获取被举报内容摘要失败", e);
        }
        return "内容获取失败";
    }
    
    /**
     * 将Report实体转换为ReportExportVO
     */
    private ReportExportVO convertToReportExportVO(Report report) {
        ReportExportVO vo = new ReportExportVO();
        
        // 基本信息
        vo.setReportId(report.getReportId());
        vo.setReason(report.getReason());
        vo.setReportTime(report.getReportTime() != null ? 
            report.getReportTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
        vo.setHandleTime(report.getHandleTime() != null ? 
            report.getHandleTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
        
        // 举报人信息
        User reporter = userMapper.selectById(report.getReporterId());
        if (reporter != null) {
            vo.setReporterName(reporter.getNickname());
        }
        
        // 目标类型描述
        vo.setTargetTypeDesc(ReportTypeEnum.getDescriptionByCode(report.getTargetType()));
        
        // 被举报内容摘要
        vo.setTargetContent(getTargetContentSummary(report.getTargetType(), report.getTargetId()));
        
        // 状态描述
        vo.setStatusDesc(ReportStatusEnum.getDescriptionByCode(report.getStatus()));
        
        // 处理人信息
        if (report.getHandlerId() != null) {
            User handler = userMapper.selectById(report.getHandlerId());
            if (handler != null) {
                vo.setHandlerName(handler.getNickname());
            }
            vo.setHandleResult(report.getHandleResult());
        }
        
        return vo;
    }
    
    /**
     * 获取目标类型名称
     */
    private String getTargetTypeName(Integer targetType) {
        if (targetType == null) {
            return "未知目标";
        }
        
        switch (targetType) {
            case 1:
                return "帖子";
            case 2:
                return "评论";
            case 3:
                return "用户";
            default:
                return "未知目标";
        }
    }
    
    /**
     * 删除被举报的内容
     */
    private void deleteTargetContent(Integer targetType, Long targetId, Long handlerId) {
        if (targetType == null || targetId == null) {
            log.warn("无法删除内容：目标类型或 ID 为空");
            return;
        }
        
        try {
            switch (targetType) {
                case 1: // 帖子
                    Post post = postMapper.selectById(targetId);
                    if (post != null) {
                        post.setStatus(0); // 设置为已删除
                        postMapper.update(post);
                        log.info("已删除帖子：{}, 操作人：{}", targetId, handlerId);
                    } else {
                        log.warn("帖子不存在，无法删除：{}", targetId);
                    }
                    break;
                    
                case 2: // 评论
                    Comment comment = commentMapper.selectById(targetId);
                    if (comment != null) {
                        comment.setStatus(0); // 设置为已删除
                        commentMapper.update(comment);
                        log.info("已删除评论：{}, 操作人：{}", targetId, handlerId);
                    } else {
                        log.warn("评论不存在，无法删除：{}", targetId);
                    }
                    break;
                    
                case 3: // 用户（暂时不处理，需要更复杂的逻辑）
                    log.info("用户举报暂不支持删除处理：{}", targetId);
                    break;
                    
                default:
                    log.warn("不支持的目标类型，无法删除：{}", targetType);
            }
        } catch (Exception e) {
            log.error("删除被举报内容失败：type={}, id={}, error={}", targetType, targetId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取被举报内容的发布人 ID
     */
    private Long getTargetContentOwnerId(Integer targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return null;
        }
        
        try {
            switch (targetType) {
                case 1: // 帖子
                    Post post = postMapper.selectById(targetId);
                    return post != null ? post.getUserId() : null;
                    
                case 2: // 评论
                    Comment comment = commentMapper.selectById(targetId);
                    return comment != null ? comment.getUserId() : null;
                    
                case 3: // 用户
                    return targetId; // 用户举报直接返回用户 ID
                    
                default:
                    return null;
            }
        } catch (Exception e) {
            log.warn("获取被举报内容发布人失败：type={}, id={}, error={}", targetType, targetId, e.getMessage());
            return null;
        }
    }
}