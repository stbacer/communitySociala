package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 用户登录 DTO
 */
@Data
public class UserLoginDTO {
    /**
     * 手机号/用户名（优先使用手机号）
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 个字符之间")
    private String password;
    
    /**
     * 验证码 ID
     */
    private String captchaId;
    
    /**
     * 验证码
     */
    private String captchaCode;
}