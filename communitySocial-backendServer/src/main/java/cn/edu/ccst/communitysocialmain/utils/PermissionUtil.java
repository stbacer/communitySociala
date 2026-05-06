package cn.edu.ccst.communitysocialmain.utils;

import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 权限验证工具类
 */
@Slf4j
@Component
public class PermissionUtil {

    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 检查是否为超级管理员
     */
    public boolean isSuperAdmin(Long userId) {
        User user = userService.getUserById(userId);
        return user != null && user.getUserRole() == 3;
    }

    /**
     * 检查是否为管理员（包括超级管理员）
     */
    public boolean isAdmin(Long userId) {
        User user = userService.getUserById(userId);
        return user != null && (user.getUserRole() == 2 || user.getUserRole() == 3);
    }

    /**
     * 检查是否为普通用户
     */
    public boolean isNormalUser(Long userId) {
        User user = userService.getUserById(userId);
        return user != null && user.getUserRole() == 1;
    }

    /**
     * 验证超级管理员权限
     * @param userId 用户ID
     * @param operation 操作描述
     */
    public void checkSuperAdminPermission(Long userId, String operation) {
        if (!isSuperAdmin(userId)) {
            log.warn("用户 {} 尝试执行需要超级管理员权限的操作: {}", userId, operation);
            throw new RuntimeException("权限不足：需要超级管理员权限才能执行此操作");
        }
        log.info("超级管理员 {} 执行操作: {}", userId, operation);
    }

    /**
     * 验证管理员权限
     * @param userId 用户ID
     * @param operation 操作描述
     */
    public void checkAdminPermission(Long userId, String operation) {
        if (!isAdmin(userId)) {
            log.warn("用户 {} 尝试执行需要管理员权限的操作: {}", userId, operation);
            throw new RuntimeException("权限不足：需要管理员权限才能执行此操作");
        }
        log.info("管理员 {} 执行操作: {}", userId, operation);
    }

    /**
     * 验证特定权限
     * @param userId 用户ID
     * @param requiredRole 所需角色级别（1:普通用户, 2:管理员, 3:超级管理员）
     * @param operation 操作描述
     */
    public void checkPermission(Long userId, Integer requiredRole, String operation) {
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (user.getUserRole() < requiredRole) {
            log.warn("用户 {} (角色: {}) 尝试执行需要角色 {} 的操作: {}", 
                    userId, user.getUserRole(), requiredRole, operation);
            throw new RuntimeException("权限不足：需要更高级别的权限才能执行此操作");
        }
        
        String roleName = getRoleName(user.getUserRole());
        log.info("{} {} 执行操作: {}", roleName, userId, operation);
    }

    /**
     * 从请求中获取当前用户ID
     */
    public String getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        throw new RuntimeException("用户未登录");
    }

    /**
     * 获取角色名称
     */
    public String getRoleName(Integer role) {
        switch (role) {
            case 1: return "普通用户";
            case 2: return "管理员";
            case 3: return "超级管理员";
            default: return "未知角色";
        }
    }

    /**
     * 检查用户是否有特定功能权限
     * @param userId 用户ID
     * @param feature 功能标识
     */
    public boolean hasFeaturePermission(Long userId, String feature) {
        User user = userService.getUserById(userId);
        if (user == null) {
            return false;
        }

        // 超级管理员拥有所有权限
        if (user.getUserRole() == 3) {
            return true;
        }

        // 管理员拥有一般管理权限
        if (user.getUserRole() == 2) {
            switch (feature) {
                case "user_management":
                case "content_review":
                case "report_handling":
                case "statistics_view":
                    return true;
                case "system_config":
                case "admin_management":
                    return false; // 管理员无权访问系统配置和管理员管理
                default:
                    return false;
            }
        }

        // 普通用户权限
        return false;
    }
}