package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 重置密码 DTO
 */
@Data
public class ResetPasswordDTO {
    
    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    /**
     * 验证码 ID
     */
    @NotBlank(message = "验证码 ID 不能为空")
    private String captchaId;
    
    /**
     * 验证码
     */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 4, message = "验证码为 4 位")
    private String captchaCode;
}