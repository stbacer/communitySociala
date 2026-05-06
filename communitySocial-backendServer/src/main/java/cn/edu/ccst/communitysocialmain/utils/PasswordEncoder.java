package cn.edu.ccst.communitysocialmain.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码编码器
 */
public class PasswordEncoder {
    
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    /**
     * 加密密码
     * @param rawPassword 明文密码
     * @return BCrypt 加密后的密码
     */
    public static String encrypt(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return encoder.encode(rawPassword);
    }
    
    /**
     * 验证密码
     * @param rawPassword 明文密码
     * @param encodedPassword BCrypt 加密后的密码
     * @return 是否匹配
     */
    public static boolean verify(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword, encodedPassword);
    }
}
