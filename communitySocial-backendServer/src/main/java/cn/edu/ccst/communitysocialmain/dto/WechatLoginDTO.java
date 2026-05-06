package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;

/**
 * 微信登录DTO
 */
@Data
public class WechatLoginDTO {
    /**
     * 微信登录凭证code
     */
    @NotBlank(message = "微信登录凭证不能为空")
    private String code;
    
    /**
     * 加密数据
     */
    private String encryptedData;
    
    /**
     * 初始向量
     */
    private String iv;
    
    /**
     * 用户头像URL
     */
    private String avatarUrl;
    
    /**
     * 用户性别
     */
    private Integer gender;
}