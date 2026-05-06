package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 用户注册 DTO
 */
@Data
public class UserRegisterDTO {
    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 个字符之间")
    private String password;
    
    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过 50 个字符")
    private String nickname;
    
    /**
     * 手机号（已弃用，使用 phone 字段）
     */
    @Deprecated
    private String oldPhone;
    
    /**
     * 所在社区
     */
    private String community;
    
    /**
     * 所在省份
     */
    private String province;
    
    /**
     * 所在城市
     */
    private String city;
    
    /**
     * 所在区县
     */
    private String district;
    
    /**
     * 身份证号
     */
    private String idCard;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 验证码ID
     */
    private String captchaId;
    
    /**
     * 验证码
     */
    private String captchaCode;
}