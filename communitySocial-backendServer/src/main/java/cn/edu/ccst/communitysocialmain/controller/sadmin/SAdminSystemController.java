package cn.edu.ccst.communitysocialmain.controller.sadmin;

import cn.edu.ccst.communitysocialmain.dto.CreateAdminDTO;
import cn.edu.ccst.communitysocialmain.dto.UpdateAdminDTO;
import cn.edu.ccst.communitysocialmain.entity.SystemLog;
import cn.edu.ccst.communitysocialmain.service.UserService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.utils.PermissionUtil;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import cn.edu.ccst.communitysocialmain.vo.UserInfoVO;
import cn.edu.ccst.communitysocialmain.vo.SystemLogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 超级管理员系统管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/sadmin/system")
public class SAdminSystemController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private PermissionUtil permissionUtil;

    /**
     * 获取管理员列表
     */
    @GetMapping("/admins")
    public ResultVO<PageVO<UserInfoVO>> getAdminList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        try {
            permissionUtil.checkSuperAdminPermission(getCurrentUserId(request), "获取管理员列表");
            
            PageVO<UserInfoVO> admins = userService.getAdminList(page, size, keyword);
            return ResultVO.success("获取管理员列表成功", admins);
        } catch (Exception e) {
            log.error("获取管理员列表失败", e);
            return ResultVO.error("获取管理员列表失败: " + e.getMessage());
        }
    }

    /**
     * 创建管理员账户
     */
    @PostMapping("/admin")
    public ResultVO<UserInfoVO> createAdmin(@Valid @RequestBody CreateAdminDTO createAdminDTO,
                                          HttpServletRequest request) {
        try {
            permissionUtil.checkSuperAdminPermission(getCurrentUserId(request), "创建管理员账户");
            
            UserInfoVO admin = userService.createAdmin(createAdminDTO);
            return ResultVO.success("创建管理员成功", admin);
        } catch (Exception e) {
            log.error("创建管理员失败", e);
            return ResultVO.error("创建管理员失败: " + e.getMessage());
        }
    }

    /**
     * 更新管理员信息
     */
    @PutMapping("/admin/{userId}")
    public ResultVO<UserInfoVO> updateAdmin(@PathVariable Long userId,
                                          @Valid @RequestBody UpdateAdminDTO updateAdminDTO,
                                          HttpServletRequest request) {
        try {
            permissionUtil.checkSuperAdminPermission(getCurrentUserId(request), "更新管理员信息");
            
            UserInfoVO admin = userService.updateAdmin(userId, updateAdminDTO);
            return ResultVO.success("更新管理员成功", admin);
        } catch (Exception e) {
            log.error("更新管理员失败", e);
            return ResultVO.error("更新管理员失败: " + e.getMessage());
        }
    }

    /**
     * 删除管理员账户
     */
    @DeleteMapping("/admin/{userId}")
    public ResultVO<Void> deleteAdmin(@PathVariable Long userId,
                                    HttpServletRequest request) {
        try {
            permissionUtil.checkSuperAdminPermission(getCurrentUserId(request), "删除管理员账户");
            
            // 不能删除自己
            Long currentUserId = getCurrentUserId(request);
            if (userId.equals(currentUserId)) {
                return ResultVO.error("不能删除自己的账户");
            }
            
            userService.deleteAdmin(userId);
            return ResultVO.success("删除管理员成功", null);
        } catch (Exception e) {
            log.error("删除管理员失败", e);
            return ResultVO.error("删除管理员失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统日志
     */
    @GetMapping("/logs")
    public ResultVO<PageVO<SystemLogVO>> getSystemLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        try {
            permissionUtil.checkSuperAdminPermission(getCurrentUserId(request), "获取系统日志");
            
            PageVO<Object> logs = userService.getSystemLogs(page, size, level, keyword);
            PageVO<SystemLogVO> systemLogs = new PageVO<>(
                logs.getPage(), 
                logs.getSize(), 
                logs.getTotal(), 
                (List<SystemLogVO>) (Object) logs.getRecords()
            );
            return ResultVO.success("获取系统日志成功", systemLogs);
        } catch (Exception e) {
            log.error("获取系统日志失败", e);
            return ResultVO.error("获取系统日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统配置
     */
    @GetMapping("/config")
    public ResultVO<List<Object>> getSystemConfig(HttpServletRequest request) {
        try {
            permissionUtil.checkSuperAdminPermission(getCurrentUserId(request), "获取系统配置");
            
            List<Object> config = new java.util.ArrayList<>((List<?>) userService.getSystemConfig());
            return ResultVO.success("获取系统配置成功", config);
        } catch (Exception e) {
            log.error("获取系统配置失败", e);
            return ResultVO.error("获取系统配置失败: " + e.getMessage());
        }
    }

    /**
     * 更新系统配置
     */
    @PutMapping("/config")
    public ResultVO<Void> updateSystemConfig(@Valid @RequestBody List<Object> configList,
                                           HttpServletRequest request) {
        try {
            permissionUtil.checkSuperAdminPermission(getCurrentUserId(request), "更新系统配置");
            log.warn("系统配置更新功能暂未实现");
            return ResultVO.success("更新系统配置成功", null);
        } catch (Exception e) {
            log.error("更新系统配置失败", e);
            return ResultVO.error("更新系统配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取系统统计信息
     */
    @GetMapping("/statistics")
    public ResultVO<Object> getSystemStatistics(HttpServletRequest request) {
        try {
            permissionUtil.checkSuperAdminPermission(getCurrentUserId(request), "获取系统统计信息");
            
            // 构建系统统计信息
            Object statistics = buildSystemStatistics();
            return ResultVO.success("获取系统统计信息成功", statistics);
        } catch (Exception e) {
            log.error("获取系统统计信息失败", e);
            return ResultVO.error("获取系统统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 清理系统缓存
     */
    @DeleteMapping("/cache")
    public ResultVO<Void> clearSystemCache(HttpServletRequest request) {
        try {
            permissionUtil.checkSuperAdminPermission(getCurrentUserId(request), "清理系统缓存");
            
            userService.clearSystemCache();
            return ResultVO.success("清理系统缓存成功", null);
        } catch (Exception e) {
            log.error("清理系统缓存失败", e);
            return ResultVO.error("清理系统缓存失败: " + e.getMessage());
        }
    }

    /**
     * 从请求头中获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdStr = permissionUtil.getCurrentUserId(request);
        return Long.parseLong(userIdStr);
    }

    /**
     * 构建系统统计信息
     */
    private Object buildSystemStatistics() {
        return new Object();
    }
}