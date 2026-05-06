package cn.edu.ccst.communitysocialmain.security;

import cn.edu.ccst.communitysocialmain.entity.User;
import cn.edu.ccst.communitysocialmain.mapper.UserMapper;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义用户详情服务
 */
@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        // 根据用户ID查询用户
        Long userIdLong = Long.parseLong(userId);
        User user = userMapper.selectById(userIdLong);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }
        
        // 检查用户状态
        if (user.getStatus() == 0 || user.getStatus() == 2 || user.getStatus() == 4) {
            throw new RuntimeException("用户状态异常");
        }
        
        // 根据用户角色设置权限
        String role;
        switch (user.getUserRole()) {
            case 1:
                role = "ROLE_RESIDENT";
                break;
            case 2:
                role = "ROLE_ADMIN";
                break;
            case 3:
                role = "ROLE_SADMIN";
                break;
            default:
                throw new RuntimeException("用户角色异常");
        }
        
        // 创建UserDetails对象
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserId().toString())
                .password(user.getPassword() != null ? user.getPassword() : "")
                .authorities(role)
                .accountExpired(false)
                .accountLocked(user.getStatus() != null && user.getStatus() == 0)
                .credentialsExpired(false)
                .disabled(user.getStatus() != null && user.getStatus() == 0)
                .build();
    }
}