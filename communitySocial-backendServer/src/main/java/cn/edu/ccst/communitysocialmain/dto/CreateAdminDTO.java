package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建管理员DTO
 */
@Data
public class CreateAdminDTO {
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;
    
    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    private String nickname;
    
    /**
     * 手机号
     */
    private String phone;
        
    /**
     * 用户角色：2 管理员，3 超级管理员
     */
    @NotNull(message = "用户角色不能为空")
    private Integer userRole;
}