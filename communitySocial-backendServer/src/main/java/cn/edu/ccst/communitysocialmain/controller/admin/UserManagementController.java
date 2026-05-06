package cn.edu.ccst.communitysocialmain.controller.admin;

import cn.edu.ccst.communitysocialmain.dto.UserCreateDTO;
import cn.edu.ccst.communitysocialmain.dto.UserProfileUpdateDTO;
import cn.edu.ccst.communitysocialmain.dto.UserUpdateDTO;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.service.AliyunOssService;
import cn.edu.ccst.communitysocialmain.service.UserService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import cn.edu.ccst.communitysocialmain.vo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/user")
public class UserManagementController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private AliyunOssService aliyunOssService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取用户列表（分页）
     */
    @GetMapping("/list")
    public ResultVO<PageVO<UserInfoVO>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer userRole,
            @RequestParam(required = false) String community,
            HttpServletRequest request) {
        try {
            checkAdminPermission(request);
                
            User condition = new User();
            if (nickname != null && !nickname.isEmpty()) {
                condition.setNickname(nickname);
            }
            if (status != null) {
                condition.setStatus(status);
            }
            if (userRole != null) {
                condition.setUserRole(userRole);
            }
            if (community != null && !community.isEmpty()) {
                condition.setCommunity(community);
            }
                
            PageVO<UserInfoVO> users = userService.getUsersByCondition(condition, page, size);
            return ResultVO.success("获取用户列表成功", users);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ResultVO.error("获取用户列表失败：" + e.getMessage());
        }
    }

    /**
     * 创建用户
     */
    @PostMapping("/create")
    public ResultVO<UserInfoVO> createUser(@Valid @RequestBody UserCreateDTO userCreateDTO,
                                         HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            
            String userIdStr = userService.createUserByAdmin(userCreateDTO);
            Long userId = Long.parseLong(userIdStr);
            UserInfoVO user = userService.getUserInfo(userId);
            log.info("管理员创建了新用户：{}", user.getPhone());
            return ResultVO.success("创建用户成功", user);
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return ResultVO.error("创建用户失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/update/{userId}")
    public ResultVO<UserInfoVO> updateUser(@PathVariable Long userId,
                                         @Valid @RequestBody UserUpdateDTO userUpdateDTO,
                                         HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            
            UserInfoVO user = userService.updateUserByAdmin(userId, userUpdateDTO);
            log.info("管理员更新了用户{}的信息", userId);
            return ResultVO.success("更新用户成功", user);
        } catch (Exception e) {
            log.error("更新用户失败", e);
            return ResultVO.error("更新用户失败: " + e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/delete/{userId}")
    public ResultVO<Void> deleteUser(@PathVariable Long userId,
                                   HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            
            userService.deleteUser(userId);
            log.info("管理员删除了用户{}", userId);
            return ResultVO.success("删除用户成功", null);
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return ResultVO.error("删除用户失败: " + e.getMessage());
        }
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/status/{userId}")
    public ResultVO<Void> updateUserStatus(@PathVariable Long userId,
                                         @RequestParam Integer status,
                                         HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            
            userService.updateUserStatus(userId, status);
            log.info("管理员更新了用户{}的状态为{}", userId, status);
            return ResultVO.success("更新用户状态成功", null);
        } catch (Exception e) {
            log.error("更新用户状态失败", e);
            return ResultVO.error("更新用户状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取单个用户详细信息
     */
    @GetMapping("/detail/{userId}")
    public ResultVO<UserInfoVO> getUserDetail(@PathVariable Long userId,
                                            HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            
            UserInfoVO user = userService.getUserInfo(userId);
            return ResultVO.success("获取用户信息成功", user);
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return ResultVO.error("获取用户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 重置用户密码
     */
    @PutMapping("/reset-password/{userId}")
    public ResultVO<Void> resetUserPassword(@PathVariable Long userId,
                                          @RequestBody Map<String, String> requestBody,
                                          HttpServletRequest request) {
        try {
            checkAdminPermission(request);
                
            String newPassword = requestBody.get("newPassword");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResultVO.error("新密码不能为空");
            }
                
            userService.resetUserPassword(userId, newPassword.trim());
            log.info("管理员重置了用户{}的密码", userId);
            return ResultVO.success("重置密码成功", null);
        } catch (Exception e) {
            log.error("重置用户密码失败", e);
            return ResultVO.error("重置密码失败：" + e.getMessage());
        }
    }
        
    /**
     * 上传头像
     */
    @PostMapping("/upload-avatar")
    public ResultVO<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                       HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
                
            // 验证文件
            if (file.isEmpty()) {
                return ResultVO.error("请选择要上传的文件");
            }
                
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResultVO.error("只支持图片文件上传");
            }
                
            // 验证文件大小（限制为 5MB）
            long maxSize = 5 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResultVO.error("文件大小不能超过 5MB");
            }
                
            // 使用阿里云 OSS 上传头像
            String fileUrl = aliyunOssService.uploadFile(file, "avatar");
                
            // 更新用户头像 URL 到数据库
            UserProfileUpdateDTO profileUpdateDTO = new UserProfileUpdateDTO();
            profileUpdateDTO.setAvatarUrl(fileUrl);
            userService.updateProfile(userId, profileUpdateDTO);
                
            // 同时更新头像来源为用户上传 (1)
            userService.updateUserAvatarSource(userId, 1);
            log.info("管理员用户{}上传头像成功，设置头像来源为用户上传：{}", userId, fileUrl);
                
            return ResultVO.success("上传成功", fileUrl);
                
        } catch (Exception e) {
            log.error("上传头像失败", e);
            return ResultVO.error("上传失败：" + e.getMessage());
        }
    }
        
    /**
     * 使用默认头像
     */
    @PostMapping("/use-default-avatar")
    public ResultVO<String> useDefaultAvatar(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
                
            // 使用默认头像 URL
            String defaultAvatarUrl = "/image/avatar/default.png";
                
            // 更新用户头像 URL 到数据库
            UserProfileUpdateDTO profileUpdateDTO = new UserProfileUpdateDTO();
            profileUpdateDTO.setAvatarUrl(defaultAvatarUrl);
            userService.updateProfile(userId, profileUpdateDTO);
                
            // 更新头像来源为系统默认 (0)
            userService.updateUserAvatarSource(userId, 0);
            log.info("管理员用户{}使用默认头像", userId);
                
            return ResultVO.success("设置成功", defaultAvatarUrl);
                
        } catch (Exception e) {
            log.error("使用默认头像失败", e);
            return ResultVO.error("设置失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有社区列表
     */
    @GetMapping("/communities")
    public ResultVO<java.util.List<String>> getAllCommunities(HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            
            // 查询所有不同的社区名称
            java.util.List<String> communities = userService.getAllCommunities();
            return ResultVO.success("获取社区列表成功", communities);
        } catch (Exception e) {
            log.error("获取社区列表失败", e);
            return ResultVO.error("获取社区列表失败：" + e.getMessage());
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