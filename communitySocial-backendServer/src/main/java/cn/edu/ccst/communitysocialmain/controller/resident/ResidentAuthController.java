package cn.edu.ccst.communitysocialmain.controller.resident;

import cn.edu.ccst.communitysocialmain.dto.*;
import cn.edu.ccst.communitysocialmain.service.UserService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import cn.edu.ccst.communitysocialmain.vo.UserInfoVO;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
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
 * 居民端用户认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/resident/auth")
public class ResidentAuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResultVO<String> register(@RequestBody @Valid UserRegisterDTO registerDTO) {
        try {
            String userId = userService.register(registerDTO);
            return ResultVO.success("注册成功", userId);
        } catch (Exception e) {
            return ResultVO.error(e.getMessage());
        }
    }
    
    /**
     * 用户名密码登录
     */
    @PostMapping("/login")
    public ResultVO<Map<String, Object>> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        try {
            // 1. 校验验证码
            if (loginDTO.getCaptchaId() == null || loginDTO.getCaptchaCode() == null) {
                return ResultVO.error("验证码不能为空");
            }
            
            String storedCaptcha = redisTemplate.opsForValue().get("captcha:" + loginDTO.getCaptchaId());
            if (storedCaptcha == null) {
                return ResultVO.error("验证码已过期，请重新获取");
            }
            
            if (!storedCaptcha.equalsIgnoreCase(loginDTO.getCaptchaCode())) {
                return ResultVO.error("验证码错误");
            }
            
            // 2. 删除已使用的验证码
            redisTemplate.delete("captcha:" + loginDTO.getCaptchaId());
            
            // 3. 进行登录验证
            String token = userService.login(loginDTO);
            
            // 4. 获取用户信息
            String userId = jwtUtil.getUserIdFromToken(token);
            UserInfoVO userInfo = userService.getUserInfo(Long.parseLong(userId));
            
            // 5. 构造返回数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", token);
            responseData.put("userInfo", userInfo);
            
            return ResultVO.success("登录成功", responseData);
        } catch (Exception e) {
            log.error("登录失败", e);
            return ResultVO.error(e.getMessage());
        }
    }
    
    /**
     * 微信授权登录
     */
    @PostMapping("/wechat-login")
    public ResultVO<Map<String, Object>> wechatLogin(@Valid @RequestBody WechatLoginDTO wechatLoginDTO) {
        String token = userService.wechatLogin(wechatLoginDTO);
        
        // 获取用户信息
        String userId = jwtUtil.getUserIdFromToken(token);
        UserInfoVO userInfo = userService.getUserInfo(Long.parseLong(userId));
        
        // 构造返回数据
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("token", token);
        responseData.put("userInfo", userInfo);
        
        return ResultVO.success("登录成功", responseData);
    }
    
    /**
     * 微信手机号授权登录
     */
    @PostMapping("/wx-login-with-phone")
    public ResultVO<Map<String, Object>> wxLoginWithPhone(@Valid @RequestBody WxLoginPhoneDTO dto) {
        try {
            // 调用服务层处理手机号授权登录
            Map<String, Object> result = userService.wxLoginWithPhone(dto);
            return ResultVO.success("登录成功", result);
        } catch (Exception e) {
            log.error("微信手机号授权登录失败", e);
            return ResultVO.error(e.getMessage());
        }
    }
    
    /**
     * 绑定手机号（已登录用户）
     */
    @PostMapping("/bind-phone")
    public ResultVO<String> bindPhone(@Valid @RequestBody BindPhoneDTO dto,
                                       @RequestHeader("Authorization") String authorization) {
        try {
            // 从 Token 中获取用户ID
            String token = authorization.replace("Bearer ", "");
            String userId = jwtUtil.getUserIdFromToken(token);
            
            // 调用服务层绑定手机号
            userService.bindPhone(Long.parseLong(userId), dto.getPhoneCode());
            
            return ResultVO.success("绑定成功");
        } catch (Exception e) {
            log.error("绑定手机号失败", e);
            return ResultVO.error(e.getMessage());
        }
    }
    
    /**
     * 获取图片验证码
     */
    @GetMapping("/captcha/image")
    public ResultVO<Map<String, Object>> getCaptchaImage() {
        try {
            // 使用 Hutool 创建线条验证码，宽 100，高 50，4 位验证码，10 条干扰线
            LineCaptcha captcha = CaptchaUtil.createLineCaptcha(100, 50, 4, 30);
            
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
     * 重置密码
     */
    @PostMapping("/reset-password")
    public ResultVO<String> resetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO) {
        try {
            String phone = resetPasswordDTO.getPhone();
            String captchaId = resetPasswordDTO.getCaptchaId();
            String captchaCode = resetPasswordDTO.getCaptchaCode();
            
            // 1. 校验验证码
            String storedCaptcha = redisTemplate.opsForValue().get("captcha:" + captchaId);
            if (storedCaptcha == null) {
                return ResultVO.error("验证码已过期，请重新获取");
            }
            
            if (!storedCaptcha.equalsIgnoreCase(captchaCode)) {
                return ResultVO.error("验证码错误");
            }
            
            // 2. 验证手机号是否存在
            UserInfoVO userInfo = userService.getUserByPhoneInfo(phone);
            if (userInfo == null) {
                return ResultVO.error("该手机号不存在");
            }
            
            // 3. 新密码为手机号后 6 位
            String newPassword = phone.substring(phone.length() - 6);
            
            // 4. 更新密码
            userService.resetPassword(userInfo.getUserId(), newPassword);
            
            // 5. 删除已使用的验证码
            redisTemplate.delete("captcha:" + captchaId);
            
            log.info("用户{}重置密码成功", phone);
            
            return ResultVO.success("密码重置成功");
        } catch (Exception e) {
            log.error("重置密码失败", e);
            return ResultVO.error("重置密码失败：" + e.getMessage());
        }
    }
}