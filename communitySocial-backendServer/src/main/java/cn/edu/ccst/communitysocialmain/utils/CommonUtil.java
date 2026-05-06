package cn.edu.ccst.communitysocialmain.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

/**
 * 通用工具类
 */
@Slf4j
@Component
public class CommonUtil {
    
    private static final String[] CHARS = new String[]{"a", "b", "c", "d", "e", "f",
            "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s",
            "t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I",
            "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
            "W", "X", "Y", "Z"};
    
    /**
     * 生成UUID（去除横线并截取前16位）
     */
    public static String generateUUID() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid.substring(0, 16);  // 截取前16位以适应数据库字段长度
    }
    
    /**
     * 生成指定位数的随机字符串
     */
    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }
    
    /**
     * 生成6位数字验证码
     */
    public static String generateVerifyCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }
    
    /**
     * 格式化日期时间
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }
    
    /**
     * 格式化日期
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }
    
    /**
     * 计算两个时间的间隔（分钟）
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return java.time.Duration.between(start, end).toMinutes();
    }
    
    /**
     * 隐藏手机号中间四位
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    /**
     * 隐藏身份证号中间部分
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        int length = idCard.length();
        return idCard.substring(0, 4) + "**********" + idCard.substring(length - 4);
    }
    
    /**
     * 判断是否为手机号
     */
    public static boolean isPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return phone.matches("^1[3-9]\\d{9}$");
    }
    
    /**
     * 判断是否为身份证号
     */
    public static boolean isIdCard(String idCard) {
        if (idCard == null || idCard.isEmpty()) {
            return false;
        }
        return idCard.matches("^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$");
    }
    
    /**
     * 获取客户端真实IP地址
     */
    public static String getClientIp(javax.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个IP地址的情况，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }
        return ip;
    }
}