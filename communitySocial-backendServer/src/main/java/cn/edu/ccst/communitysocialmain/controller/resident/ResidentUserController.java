package cn.edu.ccst.communitysocialmain.controller.resident;

import cn.edu.ccst.communitysocialmain.dto.AuthSubmitDTO;
import cn.edu.ccst.communitysocialmain.dto.ChangePasswordDTO;
import cn.edu.ccst.communitysocialmain.dto.ChangePhoneDTO;
import cn.edu.ccst.communitysocialmain.dto.UserProfileUpdateDTO;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.mapper.UserMapper;
import cn.edu.ccst.communitysocialmain.service.AliyunOssService;
import cn.edu.ccst.communitysocialmain.service.MessageService;
import cn.edu.ccst.communitysocialmain.service.UserService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.utils.PasswordEncoder;
import cn.edu.ccst.communitysocialmain.vo.PostVO;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import cn.edu.ccst.communitysocialmain.vo.UserInfoVO;
import cn.edu.ccst.communitysocialmain.vo.UserProfileCompleteVO;
import cn.edu.ccst.communitysocialmain.vo.UserStatisticsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;

/**
 * 居民端用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/resident/user")
public class ResidentUserController {
    
    @Autowired
    private AliyunOssService aliyunOssService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public ResultVO<UserInfoVO> getUserInfo(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        UserInfoVO userInfo = userService.getUserInfo(userId);
        
        log.info("=== Controller 返回用户信息 ===");
        log.info("userId: {}", userId);
        log.info("hasPassword: {}", userInfo.getHasPassword());
        
        return ResultVO.success(userInfo);
    }
    
    /**
     * 获取其他用户详情（用于查看他人主页）- 返回完整数据
     * 包括：用户信息、统计数据、帖子列表、关注列表、粉丝列表
     */
    @GetMapping("/profile/{userId}")
    public ResultVO<UserProfileCompleteVO> getUserProfileComplete(@PathVariable Long userId,
                                                                 @RequestParam(defaultValue = "1") Integer page,
                                                                 @RequestParam(defaultValue = "5") Integer size) {
        try {
            log.info("开始获取用户完整主页信息，userId: {}, page: {}, size: {}", userId, page, size);
            
            UserProfileCompleteVO completeVO = userService.getUserProfileComplete(userId, page, size);
            
            if (completeVO != null && completeVO.getUserInfo() != null) {
                log.info("获取用户完整主页信息成功：{}", userId);
                return ResultVO.success(completeVO);
            } else {
                log.warn("用户不存在：{}", userId);
                return ResultVO.error("用户不存在");
            }
        } catch (Exception e) {
            log.error("获取用户完整主页信息失败：{}", e.getMessage(), e);
            return ResultVO.error("获取失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取其他用户详情
     */
    @GetMapping("/{userId}")
    public ResultVO<UserInfoVO> getUserDetail(@PathVariable Long userId) {
        try {
            log.info("开始获取用户详情，userId: {}", userId);
            
            UserInfoVO userInfo = userService.getUserInfo(userId);
            if (userInfo != null) {
                log.info("获取用户详情成功：{}", userInfo.getUserId());
                return ResultVO.success(userInfo);
            } else {
                log.warn("用户不存在：{}", userId);
                return ResultVO.error("用户不存在");
            }
        } catch (Exception e) {
            log.error("获取用户详情失败：{}", e.getMessage(), e);
            return ResultVO.error("获取用户详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取当前用户个人统计数据
     */
    @GetMapping("/stats")
    public ResultVO<UserStatisticsVO> getUserStats(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        UserStatisticsVO userStats = userService.getUserPersonalStats(userId);
        return ResultVO.success(userStats);
    }
    
    /**
     * 提交实名认证
     */
    @PostMapping("/auth-submit")
    public ResultVO<Void> submitAuth(@Valid @RequestBody AuthSubmitDTO authSubmitDTO,
                                    HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        userService.submitAuth(userId, authSubmitDTO);
        
        // 发送系统消息通知用户
        String messageContent = "您的实名认证申请已提交，请耐心等待审核。";
        messageService.sendSystemMessage(userId, messageContent, 1);
        
        return ResultVO.success("认证申请提交成功", null);
    }
    
    /**
     * 更新用户个人信息
     */
    @PutMapping("/profile")
    public ResultVO<UserInfoVO> updateUserProfile(@Valid @RequestBody UserProfileUpdateDTO profileUpdateDTO,
                                                 HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        
        try {
            log.info("用户{}开始更新个人信息: {}", userId, profileUpdateDTO);
            
            // 调用服务层更新用户信息
            UserInfoVO updatedUserInfo = userService.updateUserProfile(userId, profileUpdateDTO);
            
            log.info("用户{}个人信息更新成功", userId);
            return ResultVO.success("个人信息更新成功", updatedUserInfo);
            
        } catch (Exception e) {
            log.error("用户{}更新个人信息失败: {}", userId, e.getMessage(), e);
            return ResultVO.error("更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传头像文件
     */
    @PostMapping("/upload-avatar")
    public ResultVO<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                       HttpServletRequest request) {
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
        
        try {
            // 使用阿里云 OSS 上传头像
            String fileUrl = aliyunOssService.uploadFile(file, "avatar");
            
            // 更新用户头像 URL 到数据库
            UserProfileUpdateDTO profileUpdateDTO = new UserProfileUpdateDTO();
            profileUpdateDTO.setAvatarUrl(fileUrl);
            userService.updateProfile(userId, profileUpdateDTO);
            
            // 同时更新头像来源为用户上传 (1)
            userService.updateUserAvatarSource(userId, 1);
            log.info("用户{}上传头像成功，设置头像来源为用户上传：{}", userId, fileUrl);
            
            log.info("用户{}上传头像成功并更新数据库：{}", userId, fileUrl);
            
            return ResultVO.success("上传成功", fileUrl);
            
        } catch (Exception e) {
            log.error("上传过程中发生错误", e);
            return ResultVO.error("上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取社区管理员的省市区和社区列表（用于居民端实名认证）
     */
    @GetMapping("/admin-regions")
    public ResultVO<?> getAdminRegions() {
        try {
            return ResultVO.success(userService.getAdminRegions());
        } catch (Exception e) {
            log.error("获取社区管理员区域信息失败：{}", e.getMessage(), e);
            return ResultVO.error("获取失败：" + e.getMessage());
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
            log.info("用户{}开始修改密码", userId);
            
            // 验证原密码（如果有）
            if (changePasswordDTO.getOldPassword() != null && !changePasswordDTO.getOldPassword().isEmpty()) {
                User user = userService.getUserByUserId(userId);
                if (user == null) {
                    return ResultVO.error("用户不存在");
                }
                
                // 检查用户是否有密码
                if (user.getPassword() == null || user.getPassword().isEmpty()) {
                    // 首次设置密码，不需要验证原密码
                    log.info("用户{}首次设置密码", userId);
                } else {
                    // 验证原密码
                    if (!PasswordEncoder.verify(changePasswordDTO.getOldPassword(), user.getPassword())) {
                        return ResultVO.error("原密码错误");
                    }
                }
            }
            
            // 更新密码
            userService.resetUserPassword(userId, changePasswordDTO.getNewPassword());
            
            log.info("用户{}密码修改成功", userId);
            return ResultVO.success("密码修改成功", null);
            
        } catch (Exception e) {
            log.error("用户{}修改密码失败：{}", userId, e.getMessage(), e);
            return ResultVO.error("修改失败：" + e.getMessage());
        }
    }
    
    /**
     * 修改手机号
     */
    @PostMapping("/change-phone")
    public ResultVO<Void> changePhone(@Valid @RequestBody ChangePhoneDTO changePhoneDTO,
                                     HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        
        try {
            // 获取当前用户信息
            User user = userMapper.selectById(userId);
            if (user == null) {
                return ResultVO.error("用户不存在");
            }
            
            String currentPhone = changePhoneDTO.getCurrentPhone();
            String newPhone = changePhoneDTO.getNewPhone();
            boolean isFirstBind = (user.getPhone() == null || user.getPhone().isEmpty());
            
            log.info("用户{}开始{}手机号：{} -> {}", userId, isFirstBind ? "绑定" : "修改", currentPhone, newPhone);
            
            // 如果不是首次绑定，需要验证当前手机号
            if (!isFirstBind) {
                if (currentPhone == null || currentPhone.isEmpty()) {
                    return ResultVO.error("请输入当前手机号以验证身份");
                }
                
                // 验证当前手机号是否正确
                if (!currentPhone.equals(user.getPhone())) {
                    return ResultVO.error("当前手机号不正确，请重新输入");
                }
                
                // 验证新手机号是否与当前手机号相同
                if (currentPhone.equals(newPhone)) {
                    return ResultVO.error("新手机号不能与当前手机号相同");
                }
            } else {
                // 首次绑定，只需检查新手机号格式
                log.info("用户{}首次绑定手机号：{}", userId, newPhone);
            }
            
            // 检查新手机号是否已被其他账号使用
            User existingPhoneUser = userMapper.selectByPhone(newPhone);
            if (existingPhoneUser != null && !existingPhoneUser.getUserId().equals(userId)) {
                return ResultVO.error("该手机号已被其他账号绑定");
            }
            
            // 更新手机号
            user.setPhone(newPhone);
            user.setUpdateTime(LocalDateTime.now());
            
            int result = userMapper.update(user);
            if (result <= 0) {
                return ResultVO.error(isFirstBind ? "绑定手机号失败" : "修改手机号失败");
            }
            
            log.info("用户{}手机号{}成功：{}", userId, isFirstBind ? "绑定" : "修改", newPhone);
            return ResultVO.success(isFirstBind ? "手机号绑定成功" : "手机号修改成功", null);
            
        } catch (Exception e) {
            log.error("用户{}修改手机号失败：{}", userId, e.getMessage(), e);
            return ResultVO.error("修改失败：" + e.getMessage());
        }
    }
    
    /**
     * 从请求头中获取当前用户 ID
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
