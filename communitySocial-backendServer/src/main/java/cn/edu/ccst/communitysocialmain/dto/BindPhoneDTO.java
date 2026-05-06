package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 绑定手机号DTO
 */
@Data
public class BindPhoneDTO {
    
    /**
     * getPhoneNumber 获取的 code
     */
    @NotBlank(message = "手机号授权码不能为空")
    private String phoneCode;
}
