package cn.edu.ccst.communitysocialmain.service;

import cn.edu.ccst.communitysocialmain.dto.*;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.PostVO;
import cn.edu.ccst.communitysocialmain.vo.UserInfoVO;
import cn.edu.ccst.communitysocialmain.vo.UserProfileCompleteVO;
import cn.edu.ccst.communitysocialmain.vo.UserStatisticsVO;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户注册
     */
    String register(UserRegisterDTO registerDTO);
    
    /**
     * 社区管理员注册
     */
    void registerAdmin(UserRegisterDTO registerDTO);
    
    /**
     * 获取待审核的管理员列表
     */
    java.util.List<UserInfoVO> getPendingAdmins(Integer userRole, Integer authStatus);
    
    /**
     * 通过管理员审核
     */
    void approveAdmin(Long userId, Long adminUserId);
    
    /**
     * 拒绝管理员审核
     */
    void rejectAdmin(Long userId, Long adminUserId, String reason);
    
    /**
     * 获取待审核管理员实名认证列表
     */
    PageVO<UserInfoVO> getPendingAdminAuthList(Integer page, Integer size, String keyword);
    
    /**
     * 通过管理员实名认证
     */
    void approveAdminAuth(Long userId, Long adminUserId);
    
    /**
     * 驳回管理员实名认证
     */
    void rejectAdminAuth(Long userId, String reason, Long adminUserId);
    
    /**
     * 用户名密码登录
     */
    String login(UserLoginDTO loginDTO);
    
    /**
     * 微信授权登录
     */
    String wechatLogin(WechatLoginDTO wechatLoginDTO);
    
    /**
     * 微信手机号授权登录
     */
    java.util.Map<String, Object> wxLoginWithPhone(WxLoginPhoneDTO dto);
    
    /**
     * 绑定手机号
     */
    void bindPhone(Long userId, String phoneCode);
    
    /**
     * 根据用户ID获取用户信息
     */
    UserInfoVO getUserInfo(Long userId);
    
    /**
     * 更新用户信息
     */
    UserInfoVO updateUserInfo(Long userId, User user);
    
    /**
     * 更新用户（简单更新）
     */
    int updateUser(User user);
    
    /**
     * 提交实名认证
     */
    void submitAuth(Long userId, AuthSubmitDTO authSubmitDTO);
    
    /**
     * 审核实名认证
     */
    void reviewAuth(Long userId, ReviewDTO reviewDTO);
    
    /**
     * 获取待审核的实名认证用户列表
     */
    PageVO<UserInfoVO> getPendingAuthUsers(Integer page, Integer size);
    
    /**
     * 根据社区获取待审核的实名认证用户列表
     */
    PageVO<UserInfoVO> getPendingAuthUsersByCommunity(String community, Integer page, Integer size);
    
    /**
     * 根据条件查询用户列表
     */
    PageVO<UserInfoVO> getUsersByCondition(User user, Integer page, Integer size);
    
    /**
     * 删除用户
     */
    void deleteUser(Long userId);
    
    /**
     * 根据微信 OpenID 获取用户
     */
    User getUserByOpenid(String openid);
    
    /**
     * 根据手机号获取用户
     */
    User getUserByPhone(String phone);
    
    /**
     * 根据手机号获取用户信息（对外接口）
     */
    UserInfoVO getUserByPhoneInfo(String phone);
    
    /**
     * 重置密码
     */
    void resetPassword(Long userId, String newPassword);
    
    /**
     * 更新用户资料
     */
    void updateProfile(Long userId, UserProfileUpdateDTO profileUpdateDTO);
    
    /**
     * 更新用户个人信息（对外接口）
     */
    UserInfoVO updateUserProfile(Long userId, UserProfileUpdateDTO profileUpdateDTO);
    
    /**
     * 直接更新用户头像来源
     */
    void updateUserAvatarSource(Long userId, Integer avatarSource);
    
    /**
     * 生成用户ID
     */
    Long generateUserId();
    
    /**
     * 根据用户 ID 获取用户（用于权限检查）
     */
    User getUserById(Long userId);
        
    /**
     * 根据用户 ID 获取用户实体
     */
    User getUserByUserId(Long userId);
    
    /**
     * 管理员创建用户
     */
    String createUserByAdmin(UserCreateDTO userCreateDTO);
    
    /**
     * 管理员更新用户信息
     */
    UserInfoVO updateUserByAdmin(Long userId, UserUpdateDTO userUpdateDTO);
    
    /**
     * 获取管理员列表
     */
    PageVO<UserInfoVO> getAdminList(Integer page, Integer size, String keyword);
    
    /**
     * 创建管理员
     */
    UserInfoVO createAdmin(CreateAdminDTO createAdminDTO);
    
    /**
     * 更新管理员
     */
    UserInfoVO updateAdmin(Long userId, UpdateAdminDTO updateAdminDTO);
    
    /**
     * 删除管理员
     */
    void deleteAdmin(Long userId);
    
    /**
     * 更新用户状态
     */
    void updateUserStatus(Long userId, Integer status);
    
    /**
     * 重置用户密码
     */
    void resetUserPassword(Long userId, String newPassword);
    
    /**
     * 获取用户个人统计数据
     */
    UserStatisticsVO getUserPersonalStats(Long userId);
    
    // ===== 超级管理员专用方法 =====
    
    /**
     * 获取系统配置
     */
    java.util.List<SystemConfigDTO> getSystemConfig();
    
    /**
     * 更新系统配置
     */
    void updateSystemConfig(java.util.List<SystemConfigDTO> configList);
    
    /**
     * 获取总用户数
     */
    Long getTotalUserCount();
    
    /**
     * 获取总帖子数
     */
    Long getTotalPostCount();
    
    /**
     * 获取在线用户数
     */
    Long getOnlineUserCount();
    
    /**
     * 获取最近活动
     */
    java.util.List<java.util.Map<String, Object>> getRecentActivities();
    
    /**
     * 获取系统日志
     */
    PageVO<Object> getSystemLogs(Integer page, Integer size, String level, String keyword);
    
    /**
     * 清理系统缓存
     */
    void clearSystemCache();
    
    /**
     * 获取社区管理员的省市区和社区列表（用于居民端实名认证）
     */
    java.util.Map<String, Object> getAdminRegions();
    
    /**
     * 获取用户的关注列表
     */
    PageVO<UserInfoVO> getFollowingList(Long userId, Integer page, Integer size);
    
    /**
     * 获取用户的粉丝列表
     */
    PageVO<UserInfoVO> getFollowerList(Long userId, Integer page, Integer size);
    
    /**
     * 获取所有社区列表
     */
    java.util.List<String> getAllCommunities();
    
    /**
     * 获取用户完整主页信息（一次性返回所有数据）
     */
    UserProfileCompleteVO getUserProfileComplete(Long userId, Integer page, Integer size);
    
    /**
     * 根据角色获取用户列表
     * @param userRole 用户角色（1-普通用户，2-社区管理员，3-超级管理员）
     * @return 用户列表
     */
    java.util.List<User> getUsersByRole(Integer userRole);
}