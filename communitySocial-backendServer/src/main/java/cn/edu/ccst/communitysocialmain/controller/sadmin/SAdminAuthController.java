package cn.edu.ccst.communitysocialmain.controller.sadmin;

import cn.edu.ccst.communitysocialmain.dto.UserLoginDTO;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.service.UserService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.GifCaptcha;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.ShearCaptcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.validation.Valid;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 超级管理员认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/sadmin/auth")
public class SAdminAuthController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 超级管理员登录
     */
    @PostMapping("/login")
    public ResultVO<Object> sadminLogin(@Valid @RequestBody UserLoginDTO loginDTO) {
        try {
            log.info("超级管理员登录请求：phone={}", loginDTO.getPhone());
                
            // 1. 校验验证码
            if (loginDTO.getCaptchaId() == null || loginDTO.getCaptchaCode() == null) {
                log.warn("验证码不能为空：phone={}", loginDTO.getPhone());
                return ResultVO.error("验证码不能为空");
            }
            
            String storedCaptcha = redisTemplate.opsForValue().get("captcha:" + loginDTO.getCaptchaId());
            if (storedCaptcha == null) {
                log.warn("验证码已过期：phone={}, captchaId={}", loginDTO.getPhone(), loginDTO.getCaptchaId());
                return ResultVO.error("验证码已过期，请重新获取");
            }
            
            if (!storedCaptcha.equalsIgnoreCase(loginDTO.getCaptchaCode())) {
                log.warn("验证码错误：phone={}, input={}, correct={}", loginDTO.getPhone(), loginDTO.getCaptchaCode(), storedCaptcha);
                return ResultVO.error("验证码错误");
            }
            
            // 2. 删除已使用的验证码
            redisTemplate.delete("captcha:" + loginDTO.getCaptchaId());
                
            // 3. 先进行普通登录验证
            log.info("开始验证用户凭据：phone={}", loginDTO.getPhone());
            String token = userService.login(loginDTO);
            log.info("用户凭据验证成功，生成token：phone={}", loginDTO.getPhone());
                
            // 4. 验证用户是否为超级管理员
            String userId = jwtUtil.getUserIdFromToken(token);
            User user = userService.getUserById(Long.parseLong(userId));
                
            if (user == null) {
                log.warn("登录失败：用户不存在 phone={}", loginDTO.getPhone());
                return ResultVO.error("用户不存在");
            }
                
            // 5. 检查用户角色是否为超级管理员
            if (user.getUserRole() != 3) {
                log.warn("登录失败：权限不足 phone={}, role={}, userRole名称={}", 
                    loginDTO.getPhone(), user.getUserRole(), 
                    getUserRoleName(user.getUserRole()));
                return ResultVO.error("权限不足，仅超级管理员可登录");
            }
                
            // 6. 检查用户状态
            if (user.getStatus() != 1) {
                log.warn("登录失败：用户状态异常 phone={}, status={}", loginDTO.getPhone(), user.getStatus());
                return ResultVO.error("用户状态异常");
            }
            
            // 7. 构造返回数据
            java.util.Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("token", token);
            responseData.put("userInfo", user);
            
            log.info("超级管理员登录成功：phone={}, userId={}", loginDTO.getPhone(), userId);
            return ResultVO.success("登录成功", responseData);
            
        } catch (Exception e) {
            log.error("超级管理员登录失败：phone={}", loginDTO.getPhone(), e);
            return ResultVO.error(e.getMessage());
        }
    }

    /**
     * 超级管理员登出
     */
    @PostMapping("/logout")
    public ResultVO<Void> sadminLogout() {
        try {
            // 前端清除token即可，后端无需特殊处理
            log.info("超级管理员登出成功");
            return ResultVO.success("登出成功", null);
        } catch (Exception e) {
            log.error("超级管理员登出失败", e);
            return ResultVO.error("登出失败: " + e.getMessage());
        }
    }

    /**
     * 刷新token
     */
    @PostMapping("/refresh")
    public ResultVO<String> refreshToken(@RequestHeader("Authorization") String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResultVO.error("无效的认证信息");
            }
            
            String oldToken = authorization.substring(7);
            String userId = jwtUtil.getUserIdFromToken(oldToken);
            
            // 验证用户是否仍然存在且为超级管理员
            User user = userService.getUserById(Long.parseLong(userId));
            if (user == null || user.getUserRole() != 3 || user.getStatus() != 1) {
                return ResultVO.error("用户权限异常");
            }
            
            String newToken = jwtUtil.generateToken(userId);
            
            log.info("超级管理员token刷新成功: userId={}", userId);
            return ResultVO.success("token刷新成功", newToken);
            
        } catch (Exception e) {
            log.error("token刷新失败", e);
            return ResultVO.error("token刷新失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前登录的超级管理员信息
     */
    @GetMapping("/info")
    public ResultVO<User> getCurrentSAdminInfo(@RequestHeader("Authorization") String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResultVO.error("未登录");
            }
            
            String token = authorization.substring(7);
            String userId = jwtUtil.getUserIdFromToken(token);
            
            User user = userService.getUserById(Long.parseLong(userId));
            if (user == null || user.getUserRole() != 3) {
                return ResultVO.error("用户信息异常");
            }
            
            user.setPassword(null);
            
            log.info("获取超级管理员信息成功：userId={}", userId);
            return ResultVO.success("获取用户信息成功", user);
            
        } catch (Exception e) {
            log.error("获取超级管理员信息失败", e);
            return ResultVO.error("获取用户信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取图片验证码
     */
    @GetMapping("/captcha/image")
    public ResultVO<Map<String, Object>> getCaptchaImage() {
        try {
            // 使用 Hutool 创建线条验证码，宽 100，高 50，4 位验证码，10 条干扰线
            ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(100, 50, 4,2);
            
            // 设置验证码样式
            captcha.setBackground(Color.WHITE);
            captcha.setFont(new Font("Arial", Font.BOLD, 36));
            
            // 生成验证码文本
            String captchaCode = captcha.getCode();
            
            // 生成唯一标识符
            String captchaId = UUID.randomUUID().toString();
            
            // 将验证码存入 Redis，有效期 5 分钟
            redisTemplate.opsForValue().set(
                "captcha:" + captchaId, 
                captchaCode, 
                5, 
                TimeUnit.MINUTES
            );
            
            // 将验证码图片转换为 Base64
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(captcha.getImage(), "PNG", outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            String base64Image = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
            
            // 构造返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("captchaId", captchaId);
            result.put("captchaImage", base64Image);
            
            log.info("生成验证码：captchaId={}, code={}", captchaId, captchaCode);
            
            return ResultVO.success("获取验证码成功", result);
        } catch (Exception e) {
            log.error("获取验证码失败", e);
            return ResultVO.error("获取验证码失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户角色名称
     */
    private String getUserRoleName(Integer role) {
        if (role == null) return "未知";
        switch (role) {
            case 1: return "社区居民";
            case 2: return "社区管理员";
            case 3: return "后台管理员（超级管理员）";
            default: return "未知(" + role + ")";
        }
    }
}