package cn.edu.ccst.communitysocialmain.config;

import cn.edu.ccst.communitysocialmain.security.JwtAuthenticationFilter;
import cn.edu.ccst.communitysocialmain.security.UnauthorizedEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置类
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private UnauthorizedEntryPoint unauthorizedEntryPoint;
    
    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * 安全过滤链配置
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF保护
            .csrf().disable()
            
            // 配置CORS
            .cors().configurationSource(corsConfigurationSource())
            .and()
            
            // 配置会话管理
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            
            // 配置异常处理
            .exceptionHandling()
            .authenticationEntryPoint(unauthorizedEntryPoint)
            .and()
            
            // 配置请求授权
            .authorizeHttpRequests(authz -> authz
                // 公开接口
                .antMatchers("/resident/auth/**").permitAll()
                .antMatchers("/resident/category/list").permitAll()
                .antMatchers("/admin/auth/login").permitAll()
                .antMatchers("/admin/auth/register").permitAll()
                .antMatchers("/admin/captcha/**").permitAll()
                .antMatchers("/sadmin/auth/login").permitAll()
                .antMatchers("/sadmin/auth/logout").permitAll()
                .antMatchers("/sadmin/system/login").permitAll()
                .antMatchers("/sadmin/auth/captcha/**").permitAll()
                .antMatchers("/resident/auth/captcha/**").permitAll()
                
                // WebSocket 端点（需要放行，WebSocket 有自己的认证机制）
                .antMatchers("/ws/**").permitAll()
                
                // Swagger相关接口（开发环境）
                .antMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // 健康检查接口
                .antMatchers("/actuator/health").permitAll()
                
                // 图片资源接口
                .antMatchers("/image/**").permitAll()
                
                // 居民端需要认证的接口
                .antMatchers("/resident/**").authenticated()
                
                // 管理员端接口需要管理员权限
                .antMatchers("/admin/**").authenticated()
                
                // 后台管理端接口需要超级管理员权限
                .antMatchers("/sadmin/**").authenticated()
                
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            
            // 添加JWT认证过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}