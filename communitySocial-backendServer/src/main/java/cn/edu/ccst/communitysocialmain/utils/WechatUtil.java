package cn.edu.ccst.communitysocialmain.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信工具类
 */
@Slf4j
@Component
public class WechatUtil {
    
    @Value("${wechat.app-id}")
    private String appId;
    
    @Value("${wechat.app-secret}")
    private String appSecret;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 微信登录凭证校验响应
     */
    public static class WechatSession {
        private String openid;
        private String session_key;
        private String unionid;
        private Integer errcode;
        private String errmsg;
        
        // Getters and Setters
        public String getOpenid() { return openid; }
        public void setOpenid(String openid) { this.openid = openid; }
        
        public String getSession_key() { return session_key; }
        public void setSession_key(String session_key) { this.session_key = session_key; }
        
        public String getUnionid() { return unionid; }
        public void setUnionid(String unionid) { this.unionid = unionid; }
        
        public Integer getErrcode() { return errcode; }
        public void setErrcode(Integer errcode) { this.errcode = errcode; }
        
        public String getErrmsg() { return errmsg; }
        public void setErrmsg(String errmsg) { this.errmsg = errmsg; }
    }
    
    /**
     * 通过code获取微信session
     */
    public WechatSession getSessionByCode(String code) {
        try {
            String url = "https://api.weixin.qq.com/sns/jscode2session?" +
                        "appid=" + appId +
                        "&secret=" + appSecret +
                        "&js_code=" + code +
                        "&grant_type=authorization_code";
            
            log.debug("调用微信API: {}", url);
            
            // 使用ResponseEntity接收原始响应
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String responseBody = response.getBody();
            
            log.debug("微信API响应: {}", responseBody);
            
            if (responseBody == null || responseBody.isEmpty()) {
                log.error("微信API返回空响应");
                return null;
            }
            
            // 手动解析JSON响应
            WechatSession session = objectMapper.readValue(responseBody, WechatSession.class);
            
            // 检查是否有错误码
            if (session.getErrcode() != null && session.getErrcode() != 0) {
                log.error("微信API返回错误: errcode={}, errmsg={}", 
                         session.getErrcode(), session.getErrmsg());
                return null;
            }
            
            if (session.getOpenid() != null) {
                log.info("微信登录成功，openid: {}", session.getOpenid());
                return session;
            } else {
                log.error("微信API响应缺少openid");
                return null;
            }
            
        } catch (Exception e) {
            log.error("调用微信API失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 解密微信用户信息（需要session_key和encryptedData）
     * 这里简化处理，实际开发中需要使用微信提供的解密算法
     */
    public Map<String, Object> decryptUserInfo(String encryptedData, String sessionKey, String iv) {
        // 实际开发中需要实现微信数据解密算法
        // 这里返回模拟数据
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("nickName", "微信用户");
        userInfo.put("avatarUrl", "");
        userInfo.put("gender", 0);
        return userInfo;
    }
    
    /**
     * 通过code获取微信手机号
     * @param code 手机号授权码
     * @return 手机号
     */
    public String getPhoneNumber(String code) {
        try {
            // 首先获取 access_token
            String tokenUrl = "https://api.weixin.qq.com/cgi-bin/token?" +
                            "grant_type=client_credential" +
                            "&appid=" + appId +
                            "&secret=" + appSecret;
            
            log.debug("调用微信API获取access_token: {}", tokenUrl);
            
            ResponseEntity<String> tokenResponse = restTemplate.getForEntity(tokenUrl, String.class);
            String tokenBody = tokenResponse.getBody();
            
            if (tokenBody == null || tokenBody.isEmpty()) {
                log.error("微信API获取access_token返回空响应");
                throw new RuntimeException("获取access_token失败");
            }
            
            Map<String, Object> tokenMap = objectMapper.readValue(tokenBody, Map.class);
            if (tokenMap.containsKey("errcode") && (Integer) tokenMap.get("errcode") != 0) {
                log.error("微信API获取access_token失败: {}", tokenBody);
                throw new RuntimeException("获取access_token失败: " + tokenMap.get("errmsg"));
            }
            
            String accessToken = (String) tokenMap.get("access_token");
            log.info("获取access_token成功");
            
            // 使用access_token获取手机号
            String phoneUrl = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("code", code);
            
            log.debug("调用微信API获取手机号: {}", phoneUrl);
            
            ResponseEntity<String> phoneResponse = restTemplate.postForEntity(phoneUrl, requestBody, String.class);
            String phoneBody = phoneResponse.getBody();
            
            if (phoneBody == null || phoneBody.isEmpty()) {
                log.error("微信API获取手机号返回空响应");
                throw new RuntimeException("获取手机号失败");
            }
            
            log.debug("微信API获取手机号响应: {}", phoneBody);
            
            Map<String, Object> phoneMap = objectMapper.readValue(phoneBody, Map.class);
            if (phoneMap.containsKey("errcode") && (Integer) phoneMap.get("errcode") != 0) {
                log.error("微信API获取手机号失败: {}", phoneBody);
                throw new RuntimeException("获取手机号失败: " + phoneMap.get("errmsg"));
            }
            
            Map<String, Object> phoneInfo = (Map<String, Object>) phoneMap.get("phone_info");
            if (phoneInfo != null && phoneInfo.containsKey("phoneNumber")) {
                String phoneNumber = (String) phoneInfo.get("phoneNumber");
                log.info("获取手机号成功: {}", phoneNumber);
                return phoneNumber;
            } else {
                log.error("微信API响应缺少手机号信息");
                throw new RuntimeException("获取手机号失败");
            }
            
        } catch (Exception e) {
            log.error("调用微信API获取手机号失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取手机号失败: " + e.getMessage());
        }
    }
}