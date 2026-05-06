package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 修改手机号 DTO
 */
@Data
public class ChangePhoneDTO {
    
    /**
     * 当前手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "当前手机号格式不正确")
    private String currentPhone;
    
    /**
     * 新手机号
     */
    @NotBlank(message = "新手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "新手机号格式不正确")
    private String newPhone;
}
