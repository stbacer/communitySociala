package cn.edu.ccst.communitysocialmain.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtUtil {
    
    @Value("${jwt.secret:0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF}")
    private String secret;
    
    @Value("${jwt.expiration:86400000}")
    private Long expiration;
    
    /**
     * 生成JWT token
     */
    public String generateToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        String token = createToken(claims, userId);
        log.debug("Generated JWT token for userId {}: {}", userId, token);
        return token;
    }
    
    /**
     * 从token中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return (String) claims.get("userId");
        } catch (Exception e) {
            log.error("解析token失败: {}", token, e);
            return null;
        }
    }
    
    /**
     * 验证token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !isTokenExpired(claims);
        } catch (Exception e) {
            log.error("验证token失败: {}", token, e);
            return false;
        }
    }
    
    /**
     * 创建token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }
    
    /**
     * 从token中获取声明
     */
    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .setAllowedClockSkewSeconds(600) // 允许10分钟的时钟偏移，处理时区差异和服务器时间不同步
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 检查token是否过期
     */
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration.before(new Date());
    }
}