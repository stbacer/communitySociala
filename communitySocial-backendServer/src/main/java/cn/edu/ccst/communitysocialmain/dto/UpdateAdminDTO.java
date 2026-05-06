package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;

/**
 * 更新管理员DTO
 */
@Data
public class UpdateAdminDTO {
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 手机号
     */
    private String phone;
        
    /**
     * 用户角色：2 管理员，3 超级管理员
     */
    private Integer userRole;
    
    /**
     * 状态：0禁用，1正常
     */
    private Integer status;
}