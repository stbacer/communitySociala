package cn.edu.ccst.communitysocialmain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户信息 VO
 */
@Data
public class UserInfoVO {
    /**
     * 用户 ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 头像 URL
     */
    private String avatarUrl;
    
    /**
     * 性别
     */
    private Integer gender;
    
    /**
     * 个性签名
     */
    private String signature;
    
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
     * 用户角色
     */
    private Integer userRole;
    
    /**
     * 认证状态
     */
    private Integer authStatus;
    
    /**
     * 真实姓名
     */
    private String realName;
    
    /**
     * 身份证号
     */
    private String idCard;
    
    /**
     * 身份证明图片 URL 列表（JSON 格式）
     */
    private String identityImages;
    
    /**
     * 用户状态：0 禁用，1 正常，2 待审核
     */
    private Integer status;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 是否已设置密码（用于前端判断）
     */
    private Boolean hasPassword;
}