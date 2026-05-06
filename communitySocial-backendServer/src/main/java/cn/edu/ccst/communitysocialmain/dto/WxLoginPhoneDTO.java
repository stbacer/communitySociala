package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 微信手机号授权登录DTO
 */
@Data
public class WxLoginPhoneDTO {
    
    /**
     * wx.login 获取的 code
     */
    @NotBlank(message = "登录凭证不能为空")
    private String loginCode;
    
    /**
     * getPhoneNumber 获取的 code
     */
    @NotBlank(message = "手机号授权码不能为空")
    private String phoneCode;
}
