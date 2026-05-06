package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class UserCreateDTO {
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
     * 手机号
     */
    private String phone;
    
    /**
     * 用户角色：1 普通用户，2 管理员，3 超级管理员
     */
    @NotNull(message = "用户角色不能为空")
    private Integer userRole;

    /**
     * 所在社区
     */
    private String community;

    /**
     * 状态：0禁用，1正常，2待审核
     */
    @NotNull(message = "用户状态不能为空")
    private Integer status = 1;
}