package cn.edu.ccst.communitysocialmain.dto;

import javax.validation.constraints.Size;

/**
 * 用户资料更新DTO
 */
public class UserProfileUpdateDTO {
    
    /**
     * 昵称
     */
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;
    
    /**
     * 头像URL
     */
    @Size(max = 255, message = "头像URL长度不能超过255个字符")
    private String avatarUrl;
    
    /**
     * 性别：0未知，1男，2女
     */
    private Integer gender;
    
    /**
     * 个性签名
     */
    @Size(max = 200, message = "个性签名长度不能超过200个字符")
    private String signature;
    
    /**
     * 手机号
     */
    @Size(max = 20, message = "手机号长度不能超过 20 个字符")
    private String phone;
        
    // Getters and Setters
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
    
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}