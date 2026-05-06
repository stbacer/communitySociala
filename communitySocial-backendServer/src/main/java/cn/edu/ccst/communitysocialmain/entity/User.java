package cn.edu.ccst.communitysocialmain.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户实体类
 */
public class User {
    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    
    /**
     * 微信 OpenID
     */
    private String openId;

    /**
     * 用户名（已弃用，使用手机号登录）
     * @deprecated 请使用 phone 字段
     */
    @Deprecated
    private String username;
    
    /**
     * 登录密码
     */
    private String password;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatarUrl;
    
    /**
     * 头像来源：0微信默认，1用户上传，2系统默认
     */
    private Integer avatarSource;
    
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
    private String signature;
    
    /**
     * 用户角色：1普通用户，2管理员，3超级管理员
     */
    private Integer userRole;
    
    /**
     * 实名认证状态：0未认证，1认证中，2已认证，3认证失败
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
     * 居住地址
     */
    private String address;
    
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
     * 身份证明图片 URL 列表（竖线分隔存储）
     */
    private List<String> identityImages;
    
    /**
     * 用户状态：0禁用，1正常，2待审核
     */
    private Integer status;
    
    /**
     * 注册时间
     */
    private LocalDateTime registerTime;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getOpenid() { return openId; }
    public void setOpenid(String openId) { this.openId = openId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public Integer getAvatarSource() { return avatarSource; }
    public void setAvatarSource(Integer avatarSource) { this.avatarSource = avatarSource; }
    
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    
    public Integer getUserRole() { return userRole; }
    public void setUserRole(Integer userRole) { this.userRole = userRole; }
    
    public Integer getAuthStatus() { return authStatus; }
    public void setAuthStatus(Integer authStatus) { this.authStatus = authStatus; }
    
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }
    
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    
    public List<String> getIdentityImages() { return identityImages; }
    public void setIdentityImages(List<String> identityImages) { this.identityImages = identityImages; }
    
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    
    public LocalDateTime getRegisterTime() { return registerTime; }
    public void setRegisterTime(LocalDateTime registerTime) { this.registerTime = registerTime; }
    
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}