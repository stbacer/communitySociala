package cn.edu.ccst.communitysocialmain.controller.admin;

import cn.edu.ccst.communitysocialmain.dto.ChangePasswordDTO;
import cn.edu.ccst.communitysocialmain.dto.ChangePhoneDTO;
import cn.edu.ccst.communitysocialmain.dto.CreateAdminDTO;
import cn.edu.ccst.communitysocialmain.dto.UpdateAdminDTO;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.service.UserService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.utils.PasswordEncoder;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import cn.edu.ccst.communitysocialmain.vo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

/**
 * 管理员管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/management")
public class AdminManagementController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

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
            checkAdminPermission(request);
            
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
            checkAdminPermission(request);
            
            // 验证当前用户权限（只有超级管理员才能创建管理员）
            Long currentUserId = getCurrentUserId(request);
            User currentUser = userService.getUserById(currentUserId);
            if (currentUser.getUserRole() != 3) {
                return ResultVO.error("权限不足：仅超级管理员可创建管理员账户");
            }
            
            UserInfoVO admin = userService.createAdmin(createAdminDTO);
            log.info("管理员{}创建了新管理员账户：{}", currentUserId, admin.getNickname());
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
            checkAdminPermission(request);
            
            Long currentUserId = getCurrentUserId(request);
            User currentUser = userService.getUserById(currentUserId);
            
            // 不能修改自己的角色和状态
            if (userId.equals(currentUserId)) {
                if (updateAdminDTO.getUserRole() != null || updateAdminDTO.getStatus() != null) {
                    return ResultVO.error("不能修改自己的角色和状态");
                }
            }
            
            // 只有超级管理员可以修改其他管理员的角色
            if (currentUser.getUserRole() != 3 && updateAdminDTO.getUserRole() != null) {
                return ResultVO.error("权限不足：仅超级管理员可修改管理员角色");
            }
            
            UserInfoVO admin = userService.updateAdmin(userId, updateAdminDTO);
            log.info("管理员{}更新了管理员{}的信息", currentUserId, userId);
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
            checkAdminPermission(request);
            
            Long currentUserId = getCurrentUserId(request);
            User currentUser = userService.getUserById(currentUserId);
            
            // 不能删除自己
            if (userId.equals(currentUserId)) {
                return ResultVO.error("不能删除自己的账户");
            }
            
            // 只有超级管理员可以删除管理员
            if (currentUser.getUserRole() != 3) {
                return ResultVO.error("权限不足：仅超级管理员可删除管理员账户");
            }
            
            userService.deleteAdmin(userId);
            log.info("管理员{}删除了管理员{}", currentUserId, userId);
            return ResultVO.success("删除管理员成功", null);
        } catch (Exception e) {
            log.error("删除管理员失败", e);
            return ResultVO.error("删除管理员失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个管理员详细信息
     */
    @GetMapping("/admin/{userId}")
    public ResultVO<UserInfoVO> getAdminDetail(@PathVariable Long userId,
                                             HttpServletRequest request) {
        try {
            checkAdminPermission(request);
                
            UserInfoVO admin = userService.getUserInfo(userId);
            // 验证是否为管理员账户
            if (admin.getUserRole() != 2 && admin.getUserRole() != 3) {
                return ResultVO.error("该用户不是管理员");
            }
                
            return ResultVO.success("获取管理员信息成功", admin);
        } catch (Exception e) {
            log.error("获取管理员信息失败", e);
            return ResultVO.error("获取管理员信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public ResultVO<Void> changePassword(@Valid @RequestBody ChangePasswordDTO changePasswordDTO,
                                        HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
            
        try {
            log.info("管理员{}开始修改密码", userId);
                
            // 验证原密码（如果有）
            if (changePasswordDTO.getOldPassword() != null && !changePasswordDTO.getOldPassword().isEmpty()) {
                User user = userService.getUserByUserId(userId);
                if (user == null) {
                    return ResultVO.error("用户不存在");
                }
                    
                // 检查用户是否有密码
                if (user.getPassword() == null || user.getPassword().isEmpty()) {
                    // 首次设置密码，不需要验证原密码
                    log.info("管理员{}首次设置密码", userId);
                } else {
                    // 验证原密码
                    if (!PasswordEncoder.verify(changePasswordDTO.getOldPassword(), user.getPassword())) {
                        return ResultVO.error("原密码错误");
                    }
                }
            }
                
            // 更新密码
            userService.resetUserPassword(userId, changePasswordDTO.getNewPassword());
                
            log.info("管理员{}密码修改成功", userId);
            return ResultVO.success("密码修改成功", null);
                
        } catch (Exception e) {
            log.error("管理员{}修改密码失败：{}", userId, e.getMessage(), e);
            return ResultVO.error("修改失败：" + e.getMessage());
        }
    }
    
    /**
     * 修改手机号（管理员自己）
     */
    @PostMapping("/change-phone")
    public ResultVO<Void> changePhone(@Valid @RequestBody ChangePhoneDTO changePhoneDTO,
                                     HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
            
        try {
            log.info("管理员{}开始修改手机号：{}", userId, changePhoneDTO.getNewPhone());
                
            // 检查新手机号是否已被使用
            User existingPhoneUser = userService.getUserByPhone(changePhoneDTO.getNewPhone());
            if (existingPhoneUser != null && !existingPhoneUser.getUserId().equals(userId)) {
                return ResultVO.error("该手机号已被其他账号绑定");
            }
                
            // TODO: 验证短信验证码
            // 目前暂时跳过验证码验证，实际项目中需要集成短信服务
                
            // 更新手机号
            User user = userService.getUserByUserId(userId);
            if (user == null) {
                return ResultVO.error("用户不存在");
            }
                
            user.setPhone(changePhoneDTO.getNewPhone());
            user.setUpdateTime(java.time.LocalDateTime.now());
                
            int result = userService.updateUser(user);
            if (result <= 0) {
                return ResultVO.error("修改手机号失败");
            }
                
            log.info("管理员{}手机号修改成功", userId);
            return ResultVO.success("手机号修改成功", null);
                
        } catch (Exception e) {
            log.error("管理员{}修改手机号失败：{}", userId, e.getMessage(), e);
            return ResultVO.error("修改失败：" + e.getMessage());
        }
    }

    /**
     * 获取待审核管理员实名认证列表
     */
    @GetMapping("/pending-auth")
    public ResultVO<PageVO<UserInfoVO>> getPendingAdminAuthList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            
            PageVO<UserInfoVO> pendingAuthList = userService.getPendingAdminAuthList(page, size, keyword);
            return ResultVO.success("获取待审核管理员列表成功", pendingAuthList);
        } catch (Exception e) {
            log.error("获取待审核管理员列表失败", e);
            return ResultVO.error("获取待审核管理员列表失败：" + e.getMessage());
        }
    }

    /**
     * 通过管理员实名认证
     */
    @PutMapping("/approve-auth")
    public ResultVO<Void> approveAdminAuth(@RequestBody Map<String, String> data,
                                          HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            
            String userIdStr = data.get("userId");
            Long userId = Long.parseLong(userIdStr);
            Long currentUserId = getCurrentUserId(request);
            
            userService.approveAdminAuth(userId, currentUserId);
            
            log.info("管理员{}通过了管理员{}的实名认证申请", currentUserId, userId);
            return ResultVO.success("通过实名认证成功", null);
        } catch (Exception e) {
            log.error("通过实名认证失败", e);
            return ResultVO.error("通过实名认证失败：" + e.getMessage());
        }
    }

    /**
     * 驳回管理员实名认证
     */
    @PutMapping("/reject-auth")
    public ResultVO<Void> rejectAdminAuth(@RequestBody Map<String, String> data,
                                         HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            
            String userIdStr = data.get("userId");
            Long userId = Long.parseLong(userIdStr);
            String reason = data.get("reason");
            Long currentUserId = getCurrentUserId(request);
            
            userService.rejectAdminAuth(userId, reason, currentUserId);
            
            log.info("管理员{}驳回了管理员{}的实名认证申请，原因：{}", currentUserId, userId, reason);
            return ResultVO.success("驳回实名认证成功", null);
        } catch (Exception e) {
            log.error("驳回实名认证失败", e);
            return ResultVO.error("驳回实名认证失败：" + e.getMessage());
        }
    }

    /**
     * 检查管理员权限
     */
    private void checkAdminPermission(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户角色是否为管理员或超级管理员
        if (user.getUserRole() != 2 && user.getUserRole() != 3) {
            throw new RuntimeException("权限不足，需要管理员权限");
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
}