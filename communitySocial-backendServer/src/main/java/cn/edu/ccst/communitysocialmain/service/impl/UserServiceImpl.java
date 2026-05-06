package cn.edu.ccst.communitysocialmain.service.impl;

import cn.edu.ccst.communitysocialmain.dto.*;
import cn.edu.ccst.communitysocialmain.entity.Category;
import cn.edu.ccst.communitysocialmain.entity.Follow;
import cn.edu.ccst.communitysocialmain.entity.OperationLog;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.mapper.CategoryMapper;
import cn.edu.ccst.communitysocialmain.mapper.FollowMapper;
import cn.edu.ccst.communitysocialmain.mapper.PostMapper;
import cn.edu.ccst.communitysocialmain.mapper.UserMapper;
import cn.edu.ccst.communitysocialmain.service.MessageService;
import cn.edu.ccst.communitysocialmain.service.OperationLogService;
import cn.edu.ccst.communitysocialmain.service.UserService;
import cn.edu.ccst.communitysocialmain.utils.CommonUtil;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.utils.PasswordEncoder;
import cn.edu.ccst.communitysocialmain.utils.SensitiveWordFilter;
import cn.edu.ccst.communitysocialmain.utils.SnowflakeIdGenerator;
import cn.edu.ccst.communitysocialmain.utils.WechatUtil;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import cn.edu.ccst.communitysocialmain.vo.PostVO;
import cn.edu.ccst.communitysocialmain.vo.UserInfoVO;
import cn.edu.ccst.communitysocialmain.vo.UserProfileCompleteVO;
import cn.edu.ccst.communitysocialmain.vo.UserStatisticsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private CategoryMapper categoryMapper;
    
    @Autowired
    private FollowMapper followMapper;
    
    @Autowired
    private PostMapper postMapper;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;
    
    @Autowired
    private WechatUtil wechatUtil;
    
    @Autowired
    private OperationLogService operationLogService;
    
    @Autowired
    private MessageService messageService;
    
    @Override
    public String register(UserRegisterDTO registerDTO) {
        // 检查手机号是否已存在
        if (registerDTO.getPhone() != null && !registerDTO.getPhone().isEmpty()) {
            User existingPhoneUser = userMapper.selectByPhone(registerDTO.getPhone());
            if (existingPhoneUser != null) {
                throw new RuntimeException("手机号已存在");
            }
        }
        
        // 创建用户
        User user = new User();
        user.setUserId(SnowflakeIdGenerator.nextId());
        user.setOpenid(""); // 微信注册时会设置
        user.setNickname(registerDTO.getNickname());
        // 使用 BCrypt 加密密码
        user.setPassword(PasswordEncoder.encrypt(registerDTO.getPassword()));
        user.setAvatarUrl("");
        user.setGender(0);
        user.setPhone(registerDTO.getPhone());
        user.setUserRole(1);
        user.setAuthStatus(0);
        user.setRealName("");
        user.setIdCard("");
        user.setAddress("");
        user.setStatus(1);
        user.setRegisterTime(LocalDateTime.now());
        user.setLastLoginTime(null);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.insert(user);
        if (result > 0) {
            return String.valueOf(user.getUserId());
        }
        throw new RuntimeException("注册失败");
    }
    
    @Override
    @Transactional
    public void registerAdmin(UserRegisterDTO registerDTO) {
        // 检查手机号是否已存在
        if (registerDTO.getPhone() != null && !registerDTO.getPhone().isEmpty()) {
            User existingPhoneUser = userMapper.selectByPhone(registerDTO.getPhone());
            if (existingPhoneUser != null) {
                throw new RuntimeException("手机号已存在");
            }
        }
        
        // 创建管理员账号（未认证状态）
        User user = new User();
        user.setUserId(SnowflakeIdGenerator.nextId());
        user.setNickname(registerDTO.getNickname());
        // 使用 BCrypt 加密密码
        user.setPassword(PasswordEncoder.encrypt(registerDTO.getPassword()));
        user.setAvatarUrl("");
        user.setGender(0);
        user.setPhone(registerDTO.getPhone());
        user.setRealName(registerDTO.getRealName() != null ? registerDTO.getRealName() : "");
        user.setIdCard(registerDTO.getIdCard() != null ? registerDTO.getIdCard() : "");
        user.setProvince(registerDTO.getProvince() != null ? registerDTO.getProvince() : "");
        user.setCity(registerDTO.getCity() != null ? registerDTO.getCity() : "");
        user.setDistrict(registerDTO.getDistrict() != null ? registerDTO.getDistrict() : "");
        user.setCommunity(registerDTO.getCommunity() != null ? registerDTO.getCommunity() : "");
        user.setUserRole(2); // 社区管理员
        user.setAuthStatus(0); // 未认证，需要后续实名认证
        user.setStatus(1); // 正常
        user.setRegisterTime(LocalDateTime.now());
        user.setLastLoginTime(null);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new RuntimeException("注册失败");
        }
    }
    
    @Override
    public List<UserInfoVO> getPendingAdmins(Integer userRole, Integer authStatus) {
        // 默认查询社区管理员（userRole=2）且认证状态为审核中（authStatus=1）
        Integer targetUserRole = (userRole != null) ? userRole : 2;
        Integer targetAuthStatus = (authStatus != null) ? authStatus : 1;
        
        // 查询待审核管理员列表 - 直接查询所有记录
        List<User> pendingAdmins = userMapper.selectAll(0, Integer.MAX_VALUE);
        
        // 在内存中过滤符合条件的用户
        return pendingAdmins.stream()
            .filter(user -> user.getUserRole() != null && user.getUserRole() == targetUserRole)
            .filter(user -> user.getAuthStatus() != null && user.getAuthStatus() == targetAuthStatus)
            .map(user -> {
                UserInfoVO vo = new UserInfoVO();
                BeanUtils.copyProperties(user, vo);
                return vo;
            })
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public void approveAdmin(Long userId, Long adminUserId) {
        long startTime = System.currentTimeMillis();
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 获取审核管理员信息
        User admin = userMapper.selectById(adminUserId);
        
        // 更新用户状态为已认证、正常
        user.setAuthStatus(2); // 已认证
        user.setStatus(1); // 正常
        user.setUpdateTime(LocalDateTime.now());
        
        userMapper.update(user);
        
        // 记录操作日志
        try {
            OperationLog log = new OperationLog();
            log.setUserId(adminUserId);
            log.setNickname(admin != null ? admin.getNickname() : "");
            log.setOperatorName(admin != null ? admin.getNickname() : "管理员");
            log.setOperation("APPROVE"); // 通过审核操作
            log.setContent(String.format("%s（id:%s）通过了用户%s(id:%s) 的管理员申请", 
                admin != null ? admin.getNickname() : "管理员", 
                adminUserId,
                user.getNickname(),
                userId));
            log.setModule("USER");
            log.setSubModule("ADMIN_APPROVE");
            log.setClientType(2); // 社区管理员端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(log);
            System.out.println("管理员审核通过日志记录成功");
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            System.err.println("记录管理员审核通过日志失败：" + e.getMessage());
        }
    }

    /**
     * 获取待审核管理员实名认证列表
     */
    @Override
    public PageVO<UserInfoVO> getPendingAdminAuthList(Integer page, Integer size, String keyword) {
        int offset = (page - 1) * size;
        
        // 使用数据库层面过滤和分页
        List<User> users = userMapper.selectPendingAdminAuthList(offset, size, keyword);
        long total = userMapper.countPendingAdminAuthList(keyword);
        
        // 转换为 UserInfoVO
        List<UserInfoVO> userVOs = users.stream()
            .map(user -> {
                UserInfoVO vo = new UserInfoVO();
                BeanUtils.copyProperties(user, vo);
                return vo;
            })
            .collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, userVOs);
    }

    /**
     * 通过管理员实名认证
     */
    @Override
    @Transactional
    public void approveAdminAuth(Long userId, Long adminUserId) {
        long startTime = System.currentTimeMillis();
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证用户是否为社区管理员且处于审核中状态
        if (user.getUserRole() != 2) {
            throw new RuntimeException("该用户不是社区管理员");
        }
        if (user.getAuthStatus() != 1) {
            throw new RuntimeException("该用户不在审核中状态");
        }
        
        // 获取审核管理员信息
        User admin = userMapper.selectById(adminUserId);
        
        // 更新用户状态为已认证、正常
        user.setAuthStatus(2); // 已认证
        user.setStatus(1); // 正常
        user.setUpdateTime(LocalDateTime.now());
        
        log.info("准备更新用户认证状态 - userId: {}, authStatus: {}, status: {}", userId, user.getAuthStatus(), user.getStatus());
        int result = userMapper.update(user);
        log.info("用户认证状态更新结果 - userId: {}, 影响行数: {}", userId, result);
        
        // 发送系统消息通知用户
        try {
            sendSystemMessage(userId, "恭喜您，您的社区管理员实名认证已通过审核！", 3);
        } catch (Exception e) {
            log.error("发送系统消息失败：{}", e.getMessage());
        }
        
        // 记录操作日志
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setUserId(adminUserId);
            operationLog.setNickname(admin != null ? admin.getNickname() : "");
            operationLog.setOperatorName(admin != null ? admin.getNickname() : "管理员");
            operationLog.setOperation("APPROVE_AUTH"); // 通过实名认证审核操作
            operationLog.setContent(String.format("%s（id:%s）通过了管理员%s(id:%s) 的实名认证申请", 
                admin != null ? admin.getNickname() : "管理员", 
                adminUserId,
                user.getNickname() != null ? user.getNickname() : user.getRealName(),
                userId));
            operationLog.setModule("USER");
            operationLog.setSubModule("AUTH_APPROVE");
            operationLog.setClientType(3); // 后台管理端
            operationLog.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(operationLog);
            log.info("管理员实名认证通过日志记录成功");
        } catch (Exception e) {
            log.error("记录管理员实名认证通过日志失败：{}", e.getMessage());
        }
    }

    /**
     * 驳回管理员实名认证
     */
    @Override
    @Transactional
    public void rejectAdminAuth(Long userId, String reason, Long adminUserId) {
        long startTime = System.currentTimeMillis();
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证用户是否为社区管理员且处于审核中状态
        if (user.getUserRole() != 2) {
            throw new RuntimeException("该用户不是社区管理员");
        }
        if (user.getAuthStatus() != 1) {
            throw new RuntimeException("该用户不在审核中状态");
        }
        
        // 获取审核管理员信息
        User admin = userMapper.selectById(adminUserId);
        
        // 更新用户状态为认证失败、禁用
        user.setAuthStatus(3); // 认证失败
        user.setStatus(0); // 禁用
        user.setUpdateTime(LocalDateTime.now());
        
        userMapper.update(user);
        
        // 发送系统消息通知用户
        try {
            String messageContent = "很抱歉，您的社区管理员实名认证未通过审核。" + 
                                   (reason != null && !reason.isEmpty() ? "\n驳回原因：" + reason : "");
            sendSystemMessage(userId, messageContent, 3);
        } catch (Exception e) {
            log.error("发送系统消息失败：{}", e.getMessage());
        }
        
        // 记录操作日志
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setUserId(adminUserId);
            operationLog.setNickname(admin != null ? admin.getNickname() : "");
            operationLog.setOperatorName(admin != null ? admin.getNickname() : "管理员");
            operationLog.setOperation("REJECT_AUTH"); // 驳回实名认证审核操作
            operationLog.setContent(String.format("%s（id:%s）驳回了管理员%s(id:%s) 的实名认证申请，原因：%s", 
                admin != null ? admin.getNickname() : "管理员", 
                adminUserId,
                user.getNickname() != null ? user.getNickname() : user.getRealName(),
                userId,
                reason != null && !reason.isEmpty() ? reason : "未填写"));
            operationLog.setModule("USER");
            operationLog.setSubModule("AUTH_REJECT");
            operationLog.setClientType(3); // 后台管理端
            operationLog.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(operationLog);
            log.info("管理员实名认证驳回日志记录成功");
        } catch (Exception e) {
            log.error("记录管理员实名认证驳回日志失败：{}", e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public void rejectAdmin(Long userId, Long adminUserId, String reason) {
        long startTime = System.currentTimeMillis();
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 获取审核管理员信息
        User admin = userMapper.selectById(adminUserId);
        
        // 更新用户状态为认证失败
        user.setAuthStatus(3); // 认证失败
        user.setStatus(0); // 禁用
        user.setUpdateTime(LocalDateTime.now());
        
        userMapper.update(user);
        
        // 记录操作日志
        try {
            OperationLog log = new OperationLog();
            log.setUserId(adminUserId);
            log.setNickname(admin != null ? admin.getNickname() : "");
            log.setOperatorName(admin != null ? admin.getNickname() : "管理员");
            log.setOperation("REJECT"); // 拒绝审核操作
            log.setContent(String.format("%s（id:%s）拒绝了用户%s(id:%s) 的管理员申请，原因：%s", 
                admin != null ? admin.getNickname() : "管理员", 
                adminUserId,
                user.getNickname(),
                userId,
                reason != null && !reason.isEmpty() ? reason : "未填写"));
            log.setModule("USER");
            log.setSubModule("ADMIN_REJECT");
            log.setClientType(2); // 社区管理员端
            log.setDuration(System.currentTimeMillis() - startTime);
            
            operationLogService.logSuccess(log);
            System.out.println("管理员审核拒绝日志记录成功");
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            System.err.println("记录管理员审核拒绝日志失败：" + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public String login(UserLoginDTO loginDTO) {
        long startTime = System.currentTimeMillis();
            
        // 根据手机号查询用户（优先使用 phone 字段）
        User user = null;
        
        // 优先按手机号查询
        if (loginDTO.getPhone() != null && !loginDTO.getPhone().isEmpty()) {
            user = userMapper.selectByPhone(loginDTO.getPhone());
            log.info("根据手机号查询用户：phone={}, 找到用户={}", loginDTO.getPhone(), user != null);
        }
        
        // 如果手机号查询失败，直接返回 null（不再兼容用户名查询）
        if (user == null) {
            log.error("登录失败：用户不存在，phone={}", loginDTO.getPhone());
            throw new RuntimeException("用户不存在");
        }
            
        // 验证密码（使用 BCrypt 校验）
        boolean passwordMatch = PasswordEncoder.verify(loginDTO.getPassword(), user.getPassword());
        log.info("密码验证：phone={}, 输入密码长度={}, 数据库密码前缀={}, 匹配结果={}", 
            loginDTO.getPhone(), 
            loginDTO.getPassword() != null ? loginDTO.getPassword().length() : 0,
            user.getPassword() != null && user.getPassword().length() > 10 ? user.getPassword().substring(0, 10) : "null",
            passwordMatch);
        
        if (!passwordMatch) {
            log.error("登录失败：密码错误，phone={}", loginDTO.getPhone());
            throw new RuntimeException("密码错误");
        }
            
        // 检查用户状态
        if (user.getStatus() != 1) {
            throw new RuntimeException("用户状态异常");
        }
            
        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.update(user);
            
        // 生成 JWT token
        String token = jwtUtil.generateToken(String.valueOf(user.getUserId()));
            
        // 记录登录日志 (仅记录成功)
        try {
            long duration = System.currentTimeMillis() - startTime;
            OperationLog log = new OperationLog();
            log.setUserId(user.getUserId());
            log.setNickname(user.getNickname());
            log.setOperatorName(user.getNickname());
            log.setOperation("LOGIN"); // 登录操作
            // 统一格式：[用户昵称]（id:[用户 id]）+ [动作描述] + [目标对象]（id:[目标 id]）时间：xxxx.xx.xx xx:xx:xx
            log.setContent(String.format("%s（id:%s）执行了登录操作", 
                user.getNickname(), user.getUserId()));
            log.setModule("USER");
            log.setSubModule("LOGIN");
            log.setClientType(3); // 居民端
            log.setDuration(duration);
                        
            operationLogService.logSuccess(log);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            log.warn("记录登录日志失败：{}", e.getMessage());
        }
            
        return token;
    }
    
    @Override
    @Transactional
    public String wechatLogin(WechatLoginDTO wechatLoginDTO) {
        long startTime = System.currentTimeMillis();
            
        // 调用微信接口获取 session
        WechatUtil.WechatSession session = wechatUtil.getSessionByCode(wechatLoginDTO.getCode());
        if (session == null || session.getOpenid() == null) {
            throw new RuntimeException("微信登录失败");
        }
            
        // 根据 openid 查找用户
        User user = userMapper.selectByOpenId(session.getOpenid());
            
        if (user == null) {
            // 新用户，创建账号
            user = new User();
            user.setUserId(SnowflakeIdGenerator.nextId());
            user.setOpenid(session.getOpenid());
            user.setNickname("微信用户");
            user.setPassword(""); // 微信用户不需要密码
            // 不设置头像，让前端处理或使用默认头像
            user.setAvatarUrl("");
            user.setAvatarSource(2); // 2-系统默认头像
            user.setGender(wechatLoginDTO.getGender() != null ? wechatLoginDTO.getGender() : 0);
            user.setPhone("");
            user.setSignature("");
            user.setUserRole(1);
            user.setAuthStatus(0);
            user.setRealName("");
            user.setIdCard("");
            user.setAddress("");
            user.setCommunity("");
            user.setStatus(1);
            user.setRegisterTime(LocalDateTime.now());
            user.setLastLoginTime(LocalDateTime.now());
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
                
            userMapper.insert(user);
        } else {
            // 更新用户信息（不处理头像，只从数据库提取现有数据）
            log.info("用户{}登录，跳过头像验证，使用数据库中现有头像：{}", 
                     user.getUserId(), user.getAvatarUrl());
                
            // 注意：不再更新性别信息，保留用户在平台设置的性别
            // 微信传来的性别可能不准确，不应该覆盖用户手动设置的值
            // if (wechatLoginDTO.getGender() != null) {
            //     user.setGender(wechatLoginDTO.getGender());
            // }
                
            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.update(user);
        }
            
        // 生成 JWT token
        String token = jwtUtil.generateToken(String.valueOf(user.getUserId()));
            
        // 记录登录日志 (仅记录成功)
        try {
            long duration = System.currentTimeMillis() - startTime;
            OperationLog log = new OperationLog();
            log.setUserId(user.getUserId());
            log.setNickname(user.getNickname());
            log.setOperatorName(user.getNickname() != null ? user.getNickname() : "微信用户");
            log.setOperation("LOGIN"); // 登录操作
            // 统一格式：[用户昵称]（id:[用户 id]）+ [动作描述] + [目标对象]（id:[目标 id]）时间：xxxx.xx.xx xx:xx:xx
            log.setContent(String.format("%s（id:%s）执行了登录操作", 
                user.getNickname() != null ? user.getNickname() : "微信用户", user.getUserId()));
            log.setModule("USER");
            log.setSubModule("LOGIN");
            log.setClientType(3); // 居民端
            log.setDuration(duration);
                        
            operationLogService.logSuccess(log);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            log.warn("记录微信登录日志失败：{}", e.getMessage());
        }
            
        return token;
    }
    
    @Override
    @Transactional
    public java.util.Map<String, Object> wxLoginWithPhone(WxLoginPhoneDTO dto) {
        long startTime = System.currentTimeMillis();
        
        log.info("开始微信手机号授权登录，loginCode: {}, phoneCode: {}", 
                 dto.getLoginCode(), dto.getPhoneCode());
        
        // 1. 通过 loginCode 获取 openid 和 session_key
        WechatUtil.WechatSession session = wechatUtil.getSessionByCode(dto.getLoginCode());
        if (session == null || session.getOpenid() == null) {
            throw new RuntimeException("微信登录失败，无法获取openid");
        }
        
        log.info("获取到openid: {}", session.getOpenid());
        
        // 2. 通过 phoneCode 获取手机号
        String phone = wechatUtil.getPhoneNumber(dto.getPhoneCode());
        if (phone == null || phone.isEmpty()) {
            throw new RuntimeException("获取手机号失败");
        }
        
        log.info("获取到手机号: {}", phone);
        
        // 3. 根据 openid 查找用户
        User user = userMapper.selectByOpenId(session.getOpenid());
        
        if (user == null) {
            // 新用户，创建账号并绑定手机号
            user = new User();
            user.setUserId(SnowflakeIdGenerator.nextId());
            user.setOpenid(session.getOpenid());
            user.setNickname("微信用户");
            user.setPassword(""); // 微信用户不需要密码
            user.setAvatarUrl("");
            user.setAvatarSource(2); // 2-系统默认头像
            user.setGender(0);
            user.setPhone(phone); // 直接绑定手机号
            user.setSignature("");
            user.setUserRole(1);
            user.setAuthStatus(0);
            user.setRealName("");
            user.setIdCard("");
            user.setAddress("");
            user.setCommunity("");
            user.setStatus(1);
            user.setRegisterTime(LocalDateTime.now());
            user.setLastLoginTime(LocalDateTime.now());
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            
            userMapper.insert(user);
            log.info("创建新用户，userId: {}, phone: {}", user.getUserId(), phone);
        } else {
            // 老用户，更新手机号和最后登录时间
            user.setPhone(phone);
            user.setLastLoginTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.update(user);
            log.info("更新用户信息，userId: {}, phone: {}", user.getUserId(), phone);
        }
        
        // 4. 生成 JWT token
        String token = jwtUtil.generateToken(String.valueOf(user.getUserId()));
        
        // 5. 构造返回结果
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("token", token);
        result.put("userId", user.getUserId());
        result.put("nickname", user.getNickname());
        result.put("avatarUrl", user.getAvatarUrl());
        result.put("phone", user.getPhone());
        result.put("userRole", user.getUserRole());
        result.put("authStatus", user.getAuthStatus());
        
        // 6. 记录登录日志
        try {
            long duration = System.currentTimeMillis() - startTime;
            OperationLog log = new OperationLog();
            log.setUserId(user.getUserId());
            log.setNickname(user.getNickname());
            log.setOperatorName(user.getNickname() != null ? user.getNickname() : "微信用户");
            log.setOperation("LOGIN");
            log.setContent(String.format("%s（id:%s）通过微信手机号授权执行了登录操作", 
                user.getNickname() != null ? user.getNickname() : "微信用户", user.getUserId()));
            log.setModule("USER");
            log.setSubModule("LOGIN");
            log.setClientType(3); // 居民端
            log.setDuration(duration);
            
            operationLogService.logSuccess(log);
        } catch (Exception e) {
            log.warn("记录微信手机号登录日志失败：{}", e.getMessage());
        }
        
        log.info("微信手机号授权登录成功，userId: {}", user.getUserId());
        return result;
    }
    
    @Override
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO);
        
        // 设置 hasPassword 字段：判断用户是否有密码
        // 如果 password 字段为空或 null，则认为没有设置密码
        boolean hasPasswordValue = user.getPassword() != null && !user.getPassword().isEmpty();
        userInfoVO.setHasPassword(hasPasswordValue);
        
        log.info("=== getUserInfo 返回用户信息 ===");
        log.info("userId: {}", userId);
        log.info("password 字段：{}", user.getPassword() != null ? "不为空" : "为空");
        log.info("password 长度：{}", user.getPassword() != null ? user.getPassword().length() : 0);
        log.info("hasPassword: {}", hasPasswordValue);
        
        return userInfoVO;
    }
    
    @Override
    @Transactional
    public UserInfoVO updateUserInfo(Long userId, User user) {
        User existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新用户信息
        user.setUserId(userId);
        user.setUpdateTime(LocalDateTime.now());
        
        // 如果更新了昵称，进行敏感词过滤
        if (StringUtils.hasText(user.getNickname())) {
            user.setNickname(sensitiveWordFilter.filter(user.getNickname()));
        }
        
        int result = userMapper.update(user);
        if (result <= 0) {
            throw new RuntimeException("更新用户信息失败");
        }
        
        return getUserInfo(userId);
    }
    
    @Override
    @Transactional
    public int updateUser(User user) {
        user.setUpdateTime(LocalDateTime.now());
        return userMapper.update(user);
    }
    
    @Override
    @Transactional
    public void submitAuth(Long userId, AuthSubmitDTO authSubmitDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查是否已经认证成功
        if (user.getAuthStatus() == 2) {
            throw new RuntimeException("您已完成实名认证，无需重复提交");
        }
        
        log.info("=== 开始处理认证提交 ===");
        log.info("用户ID: {}", userId);
        log.info("认证数据: {}", authSubmitDTO);
        log.info("图片 URL 列表：{}", authSubmitDTO.getIdentityImages());
        
        // 更新用户认证信息
     user.setRealName(authSubmitDTO.getRealName());
     user.setIdCard(authSubmitDTO.getIdCard());
     user.setCommunity(authSubmitDTO.getCommunity());
        
        // 保存省市区信息
     user.setProvince(authSubmitDTO.getProvince());
     user.setCity(authSubmitDTO.getCity());
     user.setDistrict(authSubmitDTO.getDistrict());
        
     log.info("=== 省市区数据 ===");
     log.info("省份：{}", authSubmitDTO.getProvince());
     log.info("城市：{}", authSubmitDTO.getCity());
     log.info("区县：{}", authSubmitDTO.getDistrict());
        
        // TypeHandler会自动处理List到分隔符字符串的转换
        user.setIdentityImages(authSubmitDTO.getIdentityImages());
        log.info("存储的图片列表: {}", authSubmitDTO.getIdentityImages());
        
        user.setAuthStatus(1); // 审核中
        user.setUpdateTime(LocalDateTime.now());
        
        log.info("准备更新用户信息...");
        int result = userMapper.update(user);
        log.info("数据库更新结果: {}", result);
        
        if (result <= 0) {
            throw new RuntimeException("提交认证申请失败");
        }
        
        log.info("=== 认证提交处理完成 ===");
    }
    
    @Override
    @Transactional
    public void reviewAuth(Long adminUserId, ReviewDTO reviewDTO) {
        long startTime = System.currentTimeMillis();
            
        User user = userMapper.selectById(reviewDTO.getTargetId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
            
        // 获取审核管理员信息
        User admin = userMapper.selectById(adminUserId);
            
        // 更新认证状态
        user.setAuthStatus(reviewDTO.getStatus());
        user.setUpdateTime(LocalDateTime.now());
            
        int result = userMapper.update(user);
        if (result <= 0) {
            throw new RuntimeException("审核操作失败");
        }
            
        // 发送系统消息通知用户
        try {
            String messageContent;
            if (reviewDTO.getStatus() == 2) {
                // 审核通过
                messageContent = String.format("恭喜您，您的实名认证申请已通过审核！审核管理员：%s，备注：%s", 
                    admin != null ? admin.getNickname() : "管理员",
                    reviewDTO.getRemark() != null ? reviewDTO.getRemark() : "无");
            } else {
                // 审核拒绝
                messageContent = String.format("很抱歉，您的实名认证申请未通过审核。审核管理员：%s，原因：%s", 
                    admin != null ? admin.getNickname() : "管理员",
                    reviewDTO.getRemark() != null ? reviewDTO.getRemark() : "请检查提交的信息后重新提交");
            }
                
            // 调用发送系统消息的方法
            sendSystemMessage(reviewDTO.getTargetId(), messageContent, 1); // messageType=1 表示文本消息
            log.info("已发送实名认证审核结果系统消息给用户：{}", user.getUserId());
        } catch (Exception e) {
            // 发送消息失败不影响主业务
            log.error("发送实名认证审核系统消息失败", e);
        }
            
        // 记录操作日志
        try {
            OperationLog log = new OperationLog();
            log.setUserId(adminUserId);
            log.setNickname(admin != null ? admin.getNickname() : "");
            log.setOperatorName(admin != null ? admin.getNickname() : "管理员");
            log.setOperation("实名审核"); // 实名认证审核操作
                
            // 根据审核状态生成不同的描述
            String actionDesc = reviewDTO.getStatus() == 2 ? "通过" : "拒绝";
            log.setContent(String.format("%s（id:%s）%s了用户%s(id:%s) 的实名认证申请，备注：%s", 
                admin != null ? admin.getNickname() : "管理员", 
                adminUserId,
                actionDesc,
                user.getRealName(),
                user.getUserId(),
                reviewDTO.getRemark() != null ? reviewDTO.getRemark() : "无"));
                
            log.setModule("USER");
            log.setSubModule("AUTH_REVIEW");
            log.setClientType(2); // 社区管理员端
            log.setDuration(System.currentTimeMillis() - startTime);
                
            System.out.println("=== 准备记录认证审核日志 ===");
            System.out.println("operation 值：" + log.getOperation());
            System.out.println("content 值：" + log.getContent());
            System.out.println("审核结果：" + (reviewDTO.getStatus() == 2 ? "通过" : "拒绝"));
                
            operationLogService.logSuccess(log);
            System.out.println("认证审核日志记录成功");
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            System.err.println("记录认证审核日志失败：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 发送系统消息
     * @param receiverId 接收者 ID
     * @param content 消息内容
     * @param messageType 消息类型（1-文本消息）
     */
    private void sendSystemMessage(Long receiverId, String content, Integer messageType) {
        try {
            // 使用 MessageService 发送系统消息
            messageService.sendSystemMessage(receiverId, content, messageType);
        } catch (Exception e) {
            log.error("发送系统消息失败：receiverId={}, content={}", receiverId, content, e);
            throw new RuntimeException("发送系统消息失败", e);
        }
    }
    
    @Override
    public PageVO<UserInfoVO> getPendingAuthUsers(Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<User> users = userMapper.selectPendingAuthUsers(offset, size);
        
        // 查询待审核用户总数（使用相同的条件）
        Long total = userMapper.countPendingAuthUsers();
        
        List<UserInfoVO> userVOs = users.stream().map(user -> {
            UserInfoVO vo = new UserInfoVO();
            BeanUtils.copyProperties(user, vo);
            return vo;
        }).collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, userVOs);
    }
    
    @Override
    public PageVO<UserInfoVO> getPendingAuthUsersByCommunity(String community, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<User> users = userMapper.selectPendingAuthUsersByCommunity(community, offset, size);
        
        // 查询该社区待审核用户总数
        Long total = userMapper.countPendingAuthUsersByCommunity(community);
        
        List<UserInfoVO> userVOs = users.stream().map(user -> {
            UserInfoVO vo = new UserInfoVO();
            BeanUtils.copyProperties(user, vo);
            return vo;
        }).collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, userVOs);
    }
    
    @Override
    public PageVO<UserInfoVO> getUsersByCondition(User condition, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<User> users = userMapper.selectByCondition(condition, offset, size);
        Long total = userMapper.countByCondition(condition);
        
        List<UserInfoVO> userVOs = users.stream().map(user -> {
            UserInfoVO vo = new UserInfoVO();
            BeanUtils.copyProperties(user, vo);
            return vo;
        }).collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, userVOs);
    }
    
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        int result = userMapper.deleteById(userId);
        if (result <= 0) {
            throw new RuntimeException("删除用户失败");
        }
    }
    
    @Override
    public User getUserByOpenid(String openid) {
        return userMapper.selectByOpenId(openid);
    }
        
    @Override
    public User getUserByPhone(String phone) {
        return userMapper.selectByPhone(phone);
    }
    
    @Override
    public UserInfoVO getUserByPhoneInfo(String phone) {
        User user = userMapper.selectByPhone(phone);
        if (user == null) {
            return null;
        }
        // 使用 getUserInfo 方法来转换
        return getUserInfo(user.getUserId());
    }
    
    @Override
    @Transactional
    public void bindPhone(Long userId, String phoneCode) {
        log.info("开始绑定手机号，userId: {}, phoneCode: {}", userId, phoneCode);
        
        // 1. 调用微信接口获取手机号
        String phone = wechatUtil.getPhoneNumber(phoneCode);
        
        if (phone == null || phone.isEmpty()) {
            throw new RuntimeException("获取手机号失败");
        }
        
        log.info("从微信获取到手机号: {}", phone);
        
        // 2. 检查手机号是否已被其他用户绑定
        User existingUser = userMapper.selectByPhone(phone);
        if (existingUser != null && !existingUser.getUserId().equals(userId)) {
            throw new RuntimeException("该手机号已被其他用户绑定");
        }
        
        // 3. 更新当前用户的手机号
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        user.setPhone(phone);
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.update(user);
        if (result <= 0) {
            throw new RuntimeException("绑定手机号失败");
        }
        
        log.info("用户{}成功绑定手机号: {}", userId, phone);
    }
    
    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        // 验证新密码强度
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("密码长度不能少于 6 位");
        }
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 使用 BCrypt 加密密码
        user.setPassword(PasswordEncoder.encrypt(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.update(user);
        if (result <= 0) {
            throw new RuntimeException("重置密码失败");
        }
        
        log.info("用户{}的密码已重置", userId);
    }
    
    @Override
    public Long generateUserId() {
        return SnowflakeIdGenerator.nextId();
    }
    
    @Override
    public User getUserById(Long userId) {
        return userMapper.selectById(userId);
    }
    
    @Override
    public User getUserByUserId(Long userId) {
        return userMapper.selectById(userId);
    }
    
    @Override
    @Transactional
    public String createUserByAdmin(UserCreateDTO userCreateDTO) {
        // 检查手机号是否已存在
        if (userCreateDTO.getPhone() != null && !userCreateDTO.getPhone().isEmpty()) {
            User existingPhoneUser = userMapper.selectByPhone(userCreateDTO.getPhone());
            if (existingPhoneUser != null) {
                throw new RuntimeException("手机号已存在");
            }
        }
        
        // 创建用户
        User user = new User();
        user.setUserId(SnowflakeIdGenerator.nextId());
        // 使用 BCrypt 加密密码
        user.setPassword(PasswordEncoder.encrypt(userCreateDTO.getPassword()));
        user.setNickname(userCreateDTO.getNickname());
        user.setPhone(userCreateDTO.getPhone());
        user.setUserRole(userCreateDTO.getUserRole());
        user.setCommunity(userCreateDTO.getCommunity());
        user.setStatus(userCreateDTO.getStatus());
        user.setAuthStatus(0);
        user.setAvatarUrl("");
        user.setGender(0);
        user.setRealName("");
        user.setIdCard("");
        user.setAddress("");
        user.setRegisterTime(LocalDateTime.now());
        user.setLastLoginTime(null);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new RuntimeException("创建用户失败");
        }
        
        return String.valueOf(user.getUserId());
    }
    
    @Override
    @Transactional
    public UserInfoVO updateUserByAdmin(Long userId, UserUpdateDTO userUpdateDTO) {
        User existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新用户信息
        if (userUpdateDTO.getNickname() != null) {
            existingUser.setNickname(userUpdateDTO.getNickname());
        }
        if (userUpdateDTO.getAvatarUrl() != null) {
            existingUser.setAvatarUrl(userUpdateDTO.getAvatarUrl());
        }
        if (userUpdateDTO.getGender() != null) {
            existingUser.setGender(userUpdateDTO.getGender());
        }
        if (userUpdateDTO.getPhone() != null) {
            existingUser.setPhone(userUpdateDTO.getPhone());
        }
        if (userUpdateDTO.getSignature() != null) {
            existingUser.setSignature(userUpdateDTO.getSignature());
        }
        if (userUpdateDTO.getCommunity() != null) {
            existingUser.setCommunity(userUpdateDTO.getCommunity());
        }
        if (userUpdateDTO.getUserRole() != null) {
            existingUser.setUserRole(userUpdateDTO.getUserRole());
        }
        if (userUpdateDTO.getStatus() != null) {
            existingUser.setStatus(userUpdateDTO.getStatus());
        }
        
        existingUser.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.update(existingUser);
        if (result <= 0) {
            throw new RuntimeException("更新用户信息失败");
        }
        
        return getUserInfo(userId);
    }
    
    @Override
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.update(user);
        if (result <= 0) {
            throw new RuntimeException("更新用户状态失败");
        }
    }
    
    @Override
    @Transactional
    public void resetUserPassword(Long userId, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证新密码强度
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("密码长度不能少于 6 位");
        }
                
        // 使用 BCrypt 加密密码
        user.setPassword(PasswordEncoder.encrypt(newPassword));
        user.setUpdateTime(LocalDateTime.now());
                
        int result = userMapper.update(user);
        if (result <= 0) {
            throw new RuntimeException("重置密码失败");
        }
        
        log.info("管理员重置了用户{}的密码", userId);
    }
    
    @Override
    public UserStatisticsVO getUserPersonalStats(Long userId) {
        UserStatisticsVO stats = new UserStatisticsVO();
            
        try {
            // 获取用户发布的帖子数量
            stats.setPostCount(userMapper.countUserPosts(userId));
                
            // 获取用户获得的点赞数量
            stats.setLikeCount(userMapper.countUserReceivedLikes(userId));
                
            // 获取用户发表的评论数量
            stats.setCommentCount(userMapper.countUserComments(userId));
                
            // 获取用户收藏的帖子数量
            stats.setCollectCount(userMapper.countUserCollections(userId));
                
            // 获取粉丝数量
            Long followerCount = followMapper.countFollowerByUserId(userId);
            stats.setFollowerCount(followerCount != null ? followerCount : 0L);
                
            // 获取关注数量
            Long followingCount = followMapper.countFollowingByUserId(userId);
            stats.setFollowingCount(followingCount != null ? followingCount : 0L);
                
            log.info("获取用户{}个人统计数据成功", userId);
        } catch (Exception e) {
            log.error("获取用户{}个人统计数据失败：{}", userId, e.getMessage(), e);
            // 出错时返回默认值
            stats.setPostCount(0L);
            stats.setLikeCount(0L);
            stats.setCommentCount(0L);
            stats.setCollectCount(0L);
            stats.setFollowerCount(0L);
            stats.setFollowingCount(0L);
        }
            
        return stats;
    }
    
    @Override
    @Transactional
    public void updateProfile(Long userId, UserProfileUpdateDTO profileUpdateDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新用户信息
        if (profileUpdateDTO.getNickname() != null) {
            user.setNickname(profileUpdateDTO.getNickname());
        }
        if (profileUpdateDTO.getAvatarUrl() != null) {
            user.setAvatarUrl(profileUpdateDTO.getAvatarUrl());
        }
        if (profileUpdateDTO.getGender() != null) {
            user.setGender(profileUpdateDTO.getGender());
        }
        if (profileUpdateDTO.getSignature() != null) {
            // 注意：User实体中没有signature字段，这里暂时跳过
        }
        
        userMapper.update(user);
    }
    
    @Override
    @Transactional
    public UserInfoVO updateUserProfile(Long userId, UserProfileUpdateDTO profileUpdateDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        log.info("=== 开始更新用户个人信息 ===");
        log.info("用户ID: {}", userId);
        log.info("更新数据: {}", profileUpdateDTO);
        
        // 更新用户信息
        boolean hasChanges = false;
        
        if (profileUpdateDTO.getNickname() != null && !profileUpdateDTO.getNickname().trim().isEmpty()) {
            String newNickname = profileUpdateDTO.getNickname().trim();
            // 检查昵称是否发生变化
            if (!newNickname.equals(user.getNickname())) {
                // 进行敏感词过滤
                newNickname = sensitiveWordFilter.filter(newNickname);
                user.setNickname(newNickname);
                hasChanges = true;
                log.info("更新昵称: {} -> {}", user.getNickname(), newNickname);
            }
        }
        
        if (profileUpdateDTO.getGender() != null) {
            if (!profileUpdateDTO.getGender().equals(user.getGender())) {
                user.setGender(profileUpdateDTO.getGender());
                hasChanges = true;
                log.info("更新性别: {} -> {}", user.getGender(), profileUpdateDTO.getGender());
            }
        }
        
        if (profileUpdateDTO.getPhone() != null && !profileUpdateDTO.getPhone().trim().isEmpty()) {
            String newPhone = profileUpdateDTO.getPhone().trim();
            // 验证手机号格式
            if (CommonUtil.isPhone(newPhone)) {
                if (!newPhone.equals(user.getPhone())) {
                    // 检查手机号是否已被其他用户使用
                    User existingPhoneUser = userMapper.selectByPhone(newPhone);
                    if (existingPhoneUser != null && !existingPhoneUser.getUserId().equals(userId)) {
                        throw new RuntimeException("该手机号已被其他用户绑定");
                    }
                    user.setPhone(newPhone);
                    hasChanges = true;
                    log.info("更新手机号: {} -> {}", user.getPhone(), newPhone);
                }
            } else {
                throw new RuntimeException("手机号格式不正确");
            }
        }
        
        if (profileUpdateDTO.getSignature() != null) {
            String newSignature = profileUpdateDTO.getSignature().trim();
            if (!newSignature.equals(user.getSignature())) {
                // 进行敏感词过滤
                newSignature = sensitiveWordFilter.filter(newSignature);
                user.setSignature(newSignature);
                hasChanges = true;
                log.info("更新个性签名: {} -> {}", user.getSignature(), newSignature);
            }
        }
        
        // 如果有变更，更新时间和保存
        if (hasChanges) {
            user.setUpdateTime(LocalDateTime.now());
            int result = userMapper.update(user);
            if (result <= 0) {
                throw new RuntimeException("更新用户信息失败");
            }
            log.info("用户信息更新成功");
        } else {
            log.info("用户信息无变更");
        }
        
        // 返回更新后的用户信息
        return getUserInfo(userId);
    }
    
    @Override
    @Transactional
    public void updateUserAvatarSource(Long userId, Integer avatarSource) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        user.setAvatarSource(avatarSource);
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.update(user);
        if (result <= 0) {
            throw new RuntimeException("更新头像来源失败");
        }
        
        log.info("用户{}头像来源更新成功: {}", userId, avatarSource);
    }
    
    // ===== 超级管理员专用方法实现 =====
    
    @Override
    public PageVO<UserInfoVO> getAdminList(Integer page, Integer size, String keyword) {
        // 实现获取管理员列表逻辑
        int offset = (page - 1) * size;
        List<User> admins = userMapper.selectAdmins(offset, size, keyword);
        Long total = userMapper.countAdmins(keyword);
        
        List<UserInfoVO> adminVOs = admins.stream().map(user -> {
            UserInfoVO vo = new UserInfoVO();
            BeanUtils.copyProperties(user, vo);
            return vo;
        }).collect(Collectors.toList());
        
        return new PageVO<>(page, size, total, adminVOs);
    }
    
    @Override
    @Transactional
    public UserInfoVO createAdmin(CreateAdminDTO createAdminDTO) {
        // 检查手机号是否已存在
        if (createAdminDTO.getPhone() != null && !createAdminDTO.getPhone().isEmpty()) {
            User existingPhoneUser = userMapper.selectByPhone(createAdminDTO.getPhone());
            if (existingPhoneUser != null) {
                throw new RuntimeException("手机号已存在");
            }
        }
        
        // 创建管理员账户
        User user = new User();
        user.setUserId(SnowflakeIdGenerator.nextId());
        // 使用 BCrypt 加密密码
        user.setPassword(PasswordEncoder.encrypt(createAdminDTO.getPassword()));
        user.setNickname(createAdminDTO.getNickname());
        user.setPhone(createAdminDTO.getPhone());
        user.setUserRole(createAdminDTO.getUserRole());
        user.setStatus(1);
        user.setAuthStatus(0);
        user.setAvatarUrl("");
        user.setGender(0);
        user.setRealName("");
        user.setIdCard("");
        user.setAddress("");
        user.setRegisterTime(LocalDateTime.now());
        user.setLastLoginTime(null);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.insert(user);
        if (result <= 0) {
            throw new RuntimeException("创建管理员失败");
        }
        
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO);
        return userInfoVO;
    }
    
    @Override
    @Transactional
    public UserInfoVO updateAdmin(Long userId, UpdateAdminDTO updateAdminDTO) {
        User existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            throw new RuntimeException("管理员不存在");
        }
        
        // 更新管理员信息
        if (updateAdminDTO.getNickname() != null) {
            existingUser.setNickname(updateAdminDTO.getNickname());
        }
        if (updateAdminDTO.getPhone() != null) {
            existingUser.setPhone(updateAdminDTO.getPhone());
        }
        if (updateAdminDTO.getUserRole() != null) {
            existingUser.setUserRole(updateAdminDTO.getUserRole());
        }
        if (updateAdminDTO.getStatus() != null) {
            existingUser.setStatus(updateAdminDTO.getStatus());
        }
        
        existingUser.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.update(existingUser);
        if (result <= 0) {
            throw new RuntimeException("更新管理员信息失败");
        }
        
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(existingUser, userInfoVO);
        return userInfoVO;
    }
    
    @Override
    public void deleteAdmin(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("管理员不存在");
        }
        
        // 验证是否为管理员账户
        if (user.getUserRole() != 2 && user.getUserRole() != 3) {
            throw new RuntimeException("该用户不是管理员");
        }
        
        // 执行软删除（将状态设为0）
        user.setStatus(0);
        user.setUpdateTime(LocalDateTime.now());
        
        int result = userMapper.update(user);
        if (result <= 0) {
            throw new RuntimeException("删除管理员失败");
        }
        
        log.info("管理员{}已被删除（软删除）", userId);
    }
    
    @Override
    public java.util.List<SystemConfigDTO> getSystemConfig() {
        // TODO: 实现获取系统配置逻辑
        return java.util.Collections.emptyList();
    }
    
    @Override
    public void updateSystemConfig(java.util.List<SystemConfigDTO> configList) {
        // TODO: 实现更新系统配置逻辑
    }
    
    @Override
    public Long getTotalUserCount() {
        return userMapper.countAll();
    }
    
    @Override
    public Long getTotalPostCount() {
        // TODO: 需要注入PostMapper来获取帖子总数
        return 0L;
    }
    
    @Override
    public Long getOnlineUserCount() {
        // TODO: 实现获取在线用户数逻辑
        return 0L;
    }
    
    @Override
    public java.util.List<java.util.Map<String, Object>> getRecentActivities() {
        // TODO: 实现获取最近活动逻辑
        return java.util.Collections.emptyList();
    }
    
    @Override
    public PageVO<Object> getSystemLogs(Integer page, Integer size, String level, String keyword) {
        // TODO: 实现获取系统日志逻辑
        return new PageVO<>(page, size, 0L, java.util.Collections.emptyList());
    }
    
    @Override
    public void clearSystemCache() {
        // TODO: 实现清理系统缓存逻辑
        log.info("系统缓存已清理");
    }
    
    @Override
    public java.util.Map<String, Object> getAdminRegions() {
        // 查询所有社区管理员（userRole=2）且已认证（authStatus=2）的用户
        List<User> admins = userMapper.selectAll(0, Integer.MAX_VALUE).stream()
            .filter(user -> user.getUserRole() != null && user.getUserRole() == 2)
            .filter(user -> user.getAuthStatus() != null && user.getAuthStatus() == 2)
            .collect(Collectors.toList());
        
        log.info("查询到{}个社区管理员", admins.size());
        
        // 提取所有不同的省份
        java.util.Set<String> provincesSet = admins.stream()
            .map(User::getProvince)
            .filter(province -> province != null && !province.isEmpty())
            .distinct()
            .collect(java.util.stream.Collectors.toSet());
        
        log.info("共有{}个不同的省份", provincesSet.size());
        
        // 构建省份列表（带代码）
        List<java.util.Map<String, String>> provinces = provincesSet.stream().map(province -> {
            java.util.Map<String, String> provinceMap = new java.util.HashMap<>();
            provinceMap.put("name", province);
            provinceMap.put("code", province); // 使用省份名称作为代码
            return provinceMap;
        }).collect(Collectors.toList());
        
        // 按省份分组，构建省份 -> 城市的映射关系
        java.util.Map<String, List<String>> citiesByProvinceMap = admins.stream()
            .filter(user -> user.getProvince() != null && !user.getProvince().isEmpty())
            .filter(user -> user.getCity() != null && !user.getCity().isEmpty())
            .collect(Collectors.groupingBy(
                User::getProvince,
                Collectors.mapping(User::getCity, Collectors.toList())
            ));
        
        // 构建城市列表（包含所属省份信息）
        List<java.util.Map<String, String>> cities = new java.util.ArrayList<>();
        citiesByProvinceMap.forEach((province, cityList) -> {
            cityList.stream().distinct().forEach(city -> {
                java.util.Map<String, String> cityMap = new java.util.HashMap<>();
                cityMap.put("name", city);
                cityMap.put("province", province); // 添加所属省份
                cityMap.put("code", city);
                cities.add(cityMap);
            });
        });
        
        log.info("共有{}个城市", cities.size());
        
        // 按省份和城市分组，构建 (省份 + 城市) -> 区县的映射关系
        java.util.Map<String, List<String>> districtsByCityMap = admins.stream()
            .filter(user -> user.getProvince() != null && !user.getProvince().isEmpty())
            .filter(user -> user.getCity() != null && !user.getCity().isEmpty())
            .filter(user -> user.getDistrict() != null && !user.getDistrict().isEmpty())
            .collect(Collectors.groupingBy(
                user -> user.getProvince() + "|" + user.getCity(),
                Collectors.mapping(User::getDistrict, Collectors.toList())
            ));
        
        // 构建区县列表（包含所属省份和城市信息）
        List<java.util.Map<String, String>> districts = new java.util.ArrayList<>();
        districtsByCityMap.forEach((key, districtList) -> {
            String[] parts = key.split("\\|");
            String province = parts[0];
            String city = parts[1];
            districtList.stream().distinct().forEach(district -> {
                java.util.Map<String, String> districtMap = new java.util.HashMap<>();
                districtMap.put("name", district);
                districtMap.put("province", province); // 添加所属省份
                districtMap.put("city", city); // 添加所属城市
                districtMap.put("code", district);
                districts.add(districtMap);
            });
        });
        
        log.info("共有{}个区县", districts.size());
        
        // 提取所有不同的社区（带省市区信息）
        List<java.util.Map<String, String>> communities = admins.stream()
            .filter(user -> user.getCommunity() != null && !user.getCommunity().isEmpty())
            .map(user -> {
                java.util.Map<String, String> communityMap = new java.util.HashMap<>();
                communityMap.put("name", user.getCommunity());
                communityMap.put("province", user.getProvince());
                communityMap.put("city", user.getCity());
                communityMap.put("district", user.getDistrict());
                return communityMap;
            })
            .distinct()
            .collect(Collectors.toList());
        
        log.info("共有{}个不同的社区", communities.size());
        
        // 构建返回结果
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("provinces", provinces);
        result.put("cities", cities);
        result.put("districts", districts);
        result.put("communities", communities);
        
        log.info("获取到{}个省份、{}个城市、{}个区县和{}个社区的行政区域信息", 
            provinces.size(), cities.size(), districts.size(), communities.size());
        return result;
    }
    
    @Override
    public PageVO<UserInfoVO> getFollowingList(Long userId, Integer page, Integer size) {
        log.info("获取用户{}的关注列表，页码{}，每页数量{}", userId, page, size);
        
        int offset = (page - 1) * size;
        
        // 从 FollowMapper 中查询关注列表
        List<Follow> followRecords = followMapper.selectFollowingByUserId(userId, offset, size);
        Long total = followMapper.countFollowingByUserId(userId);
        
        // 获取被关注用户的详细信息
        List<UserInfoVO> followingList = followRecords.stream()
            .map(follow -> getUserInfo(follow.getFollowedId()))
            .collect(Collectors.toList());
        
        log.info("获取关注列表成功，共{}条", total);
        return new PageVO<>(page, size, total, followingList);
    }
    
    @Override
    public PageVO<UserInfoVO> getFollowerList(Long userId, Integer page, Integer size) {
        log.info("获取用户{}的粉丝列表，页码{}，每页数量{}", userId, page, size);
        
        int offset = (page - 1) * size;
        
        // 从 FollowMapper 中查询粉丝列表
        List<Follow> followRecords = followMapper.selectFollowerByUserId(userId, offset, size);
        Long total = followMapper.countFollowerByUserId(userId);
        
        // 获取粉丝用户的详细信息
        List<UserInfoVO> followerList = followRecords.stream()
            .map(follow -> getUserInfo(follow.getFollowerId()))
            .collect(Collectors.toList());
        
        log.info("获取粉丝列表成功，共{}条", total);
        return new PageVO<>(page, size, total, followerList);
    }
    
    @Override
    public UserProfileCompleteVO getUserProfileComplete(Long userId, Integer page, Integer size) {
        log.info("获取用户完整主页信息，userId: {}, page: {}, size: {}", userId, page, size);
        
        try {
            // 1. 获取用户基本信息
            UserInfoVO userInfo = getUserInfo(userId);
            if (userInfo == null) {
                log.warn("用户不存在：{}", userId);
                return null;
            }
            
            // 2. 获取统计数据
            UserStatisticsVO statistics = getUserPersonalStats(userId);
            
            // 3. 并发获取帖子列表、关注列表、粉丝列表（第一页）
            PageVO<PostVO> posts = getUserPostsPage(userId, page, size);
            PageVO<UserInfoVO> followingList = getFollowingList(userId, page, size);
            PageVO<UserInfoVO> followerList = getFollowerList(userId, page, size);
            
            // 4. 组装完整数据
            UserProfileCompleteVO completeVO = new UserProfileCompleteVO();
            completeVO.setUserInfo(userInfo);
            completeVO.setStatistics(statistics);
            completeVO.setPosts(posts);
            completeVO.setFollowingList(followingList);
            completeVO.setFollowerList(followerList);
            
            log.info("获取用户完整主页信息成功：{}", userId);
            return completeVO;
            
        } catch (Exception e) {
            log.error("获取用户完整主页信息失败：{}", e.getMessage(), e);
            throw new RuntimeException("获取用户完整主页信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户帖子分页数据（用于用户主页，只显示审核已通过的帖子）
     */
    private PageVO<PostVO> getUserPostsPage(Long userId, Integer page, Integer size) {
        try {
            int offset = (page - 1) * size;
            
            // 查询用户帖子（只查询审核已通过的帖子，status=2）
            List<cn.edu.ccst.communitysocialmain.entity.Post> postEntities = 
                postMapper.selectByUserId(userId, offset, size);
            Long total = postMapper.countByUserId(userId);
            
            // 转换为 PostVO
            List<PostVO> postVOs = postEntities.stream()
                .map(post -> {
                    PostVO postVO = new PostVO();
                    BeanUtils.copyProperties(post, postVO);
                    
                    // 设置板块分类名称和类型
                    if (post.getCategoryId() != null) {
                        Category category = categoryMapper.selectById(post.getCategoryId());
                        if (category != null) {
                            postVO.setCategoryName(category.getName());
                            // 根据分类 ID 映射到前端显示的类型
                            // categoryId 1-3: 求助, 4-7: 分享, 8-10: 资讯
                            Integer categoryId = post.getCategoryId();
                            if (categoryId >= 1 && categoryId <= 3) {
                                postVO.setType(1); // 求助
                            } else if (categoryId >= 4 && categoryId <= 7) {
                                postVO.setType(2); // 分享
                            } else {
                                postVO.setType(3); // 资讯
                            }
                        }
                    }
                    
                    return postVO;
                })
                .collect(Collectors.toList());
            
            return new PageVO<>(page, size, total, postVOs);
        } catch (Exception e) {
            log.error("获取用户帖子列表失败：{}", e.getMessage());
            // 返回空列表，不影响其他数据加载
            return new PageVO<>(page, size, 0L, java.util.Collections.emptyList());
        }
    }
    
    @Override
    public java.util.List<String> getAllCommunities() {
        // 查询所有不同的社区名称
        return userMapper.selectDistinctCommunities();
    }
    
    @Override
    public java.util.List<User> getUsersByRole(Integer userRole) {
        log.info("根据角色获取用户列表，userRole: {}", userRole);
        try {
            // 使用 MyBatis 的 Example 或者自定义 SQL 查询
            // 这里直接使用 UserMapper 的方法
            return userMapper.selectByUserRole(userRole);
        } catch (Exception e) {
            log.error("根据角色获取用户列表失败：{}", e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }
}