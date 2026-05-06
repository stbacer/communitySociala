package cn.edu.ccst.communitysocialmain.dto;

import lombok.Data;
import javax.validation.constraints.Size;

@Data
public class UserUpdateDTO {
    /**
     * 昵称
     */
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 性别：0未知，1男，2女
     */
    private Integer gender;

    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 个性签名
     */
    @Size(max = 255, message = "个性签名长度不能超过 255 个字符")
    private String signature;

    /**
     * 所在社区
     */
    private String community;

    /**
     * 用户角色：1普通用户，2管理员，3超级管理员
     */
    private Integer userRole;

    /**
     * 状态：0禁用，1正常，2待审核
     */
    private Integer status;
}