package cn.edu.ccst.communitysocialmain.controller.sadmin;

import cn.edu.ccst.communitysocialmain.dto.UserLoginDTO;
import cn.edu.ccst.communitysocialmain.dto.UserProfileUpdateDTO;
import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.service.UserService;
import cn.edu.ccst.communitysocialmain.utils.JwtUtil;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import cn.edu.ccst.communitysocialmain.vo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 超级管理员登录控制器
 */
@Slf4j
@RestController
@RequestMapping("/sadmin/system")
public class SAdminLoginController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 超级管理员登录接口
     */
    @PostMapping("/login")
    public ResultVO<SAdminLoginResponse> sadminLogin(@Valid @RequestBody UserLoginDTO loginDTO) {
        try {
            log.info("超级管理员登录请求：phone={}", loginDTO.getPhone());
                
            String token = userService.login(loginDTO);
                
            String userId = jwtUtil.getUserIdFromToken(token);
            User user = userService.getUserById(Long.parseLong(userId));
                
            if (user == null) {
                log.warn("登录失败：用户不存在，phone={}", loginDTO.getPhone());
                return ResultVO.error("用户不存在");
            }
                
            if (user.getUserRole() != 3) {
                log.warn("登录拒绝：非超级管理员尝试登录，phone={}, role={}", 
                        loginDTO.getPhone(), user.getUserRole());
                return ResultVO.error("权限不足：仅超级管理员可登录后端管理系统");
            }
                
            if (user.getStatus() != 1) {
                log.warn("登录拒绝：用户状态异常，phone={}, status={}", 
                        loginDTO.getPhone(), user.getStatus());
                return ResultVO.error("账户状态异常，请联系系统管理员");
            }
            
            UserInfoVO userInfoVO = new UserInfoVO();
            BeanUtils.copyProperties(user, userInfoVO);
            
            SAdminLoginResponse response = new SAdminLoginResponse();
            response.setToken(token);
            response.setUserInfo(userInfoVO);
            
            log.info("超级管理员登录成功：phone={}, userId={}", 
                    loginDTO.getPhone(), userId);
            
            return ResultVO.success("登录成功", response);
            
        } catch (Exception e) {
            log.error("超级管理员登录失败：phone={}, error={}", 
                    loginDTO.getPhone(), e.getMessage(), e);
            return ResultVO.error(e.getMessage());
        }
    }
    
    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current-user")
    public ResultVO<UserInfoVO> getCurrentUser(HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                String userId = jwtUtil.getUserIdFromToken(token);
                UserInfoVO userInfo = userService.getUserInfo(Long.parseLong(userId));
                return ResultVO.success("获取用户信息成功", userInfo);
            }
            return ResultVO.error("未登录");
        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            return ResultVO.error("获取用户信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新当前用户信息
     */
    @PutMapping("/current-user")
    public ResultVO<UserInfoVO> updateCurrentUser(@RequestBody UserProfileUpdateDTO profileUpdateDTO,
                                                  HttpServletRequest request) {
        try {
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                String userId = jwtUtil.getUserIdFromToken(token);
                UserInfoVO updatedUser = userService.updateUserProfile(Long.parseLong(userId), profileUpdateDTO);
                return ResultVO.success("更新用户信息成功", updatedUser);
            }
            return ResultVO.error("未登录");
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return ResultVO.error("更新用户信息失败：" + e.getMessage());
        }
    }
    
    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public ResultVO<Void> logout(HttpServletRequest request) {
        try {
            log.info("用户退出登录成功");
            return ResultVO.success("退出登录成功", null);
        } catch (Exception e) {
            log.error("退出登录失败", e);
            return ResultVO.error("退出登录失败：" + e.getMessage());
        }
    }
    
    /**
     * 超级管理员登录响应 VO
     */
    public static class SAdminLoginResponse {
        private String token;
        private UserInfoVO userInfo;
        
        // Getters and Setters
        public String getToken() {
            return token;
        }
        
        public void setToken(String token) {
            this.token = token;
        }
        
        public UserInfoVO getUserInfo() {
            return userInfo;
        }
        
        public void setUserInfo(UserInfoVO userInfo) {
            this.userInfo = userInfo;
        }
    }
}