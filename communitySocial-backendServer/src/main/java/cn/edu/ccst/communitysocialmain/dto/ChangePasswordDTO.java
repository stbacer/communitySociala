package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 修改密码 DTO
 */
@Data
public class ChangePasswordDTO {
    
    /**
     * 原密码
     */
    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 个字符之间")
    private String oldPassword;
    
    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在 6-20 个字符之间")
    private String newPassword;
}
