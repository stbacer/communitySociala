package cn.edu.ccst.communitysocialmain.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置工具类
 */
@Slf4j
@Component
public class ConfigUtil {
    
    @Autowired
    private Environment environment;
    
    // 配置缓存
    private final ConcurrentHashMap<String, String> configCache = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initConfigCache() {
        loadAllConfigs();
    }
    
    /**
     * 加载所有配置到缓存
     */
    public void loadAllConfigs() {
        try {
            // 不需要从数据库加载，配置已经从 application.properties 读取
            configCache.clear();
            
            // 预加载一些常用配置到缓存
            cacheConfig("wechat.app-id", environment.getProperty("wechat.app-id"));
            cacheConfig("wechat.app-secret", environment.getProperty("wechat.app-secret"));
            cacheConfig("jwt.secret", environment.getProperty("jwt.secret"));
            cacheConfig("jwt.expiration", environment.getProperty("jwt.expiration"));
            cacheConfig("aliyun.oss.endpoint", environment.getProperty("aliyun.oss.endpoint"));
            cacheConfig("aliyun.oss.access-key-id", environment.getProperty("aliyun.oss.access-key-id"));
            cacheConfig("aliyun.oss.access-key-secret", environment.getProperty("aliyun.oss.access-key-secret"));
            cacheConfig("aliyun.oss.bucket-name", environment.getProperty("aliyun.oss.bucket-name"));
            cacheConfig("auto.review.enabled", environment.getProperty("auto.review.enabled"));
            cacheConfig("sensitive.check.enabled", environment.getProperty("sensitive.check.enabled"));
        } catch (Exception e) {
            log.error("加载系统配置失败", e);
        }
    }
    
    /**
     * 缓存配置项
     */
    private void cacheConfig(String key, String value) {
        if (value != null) {
            configCache.put(key, value);
        }
    }
    
    /**
     * 获取配置值
     */
    public String getConfig(String key) {
        // 先从缓存中获取，如果没有则直接从 environment 读取
        String value = configCache.get(key);
        if (value == null) {
            value = environment.getProperty(key);
            if (value != null) {
                configCache.put(key, value);
            }
        }
        return value;
    }
    
    /**
     * 获取配置值，带默认值
     */
    public String getConfig(String key, String defaultValue) {
        return configCache.getOrDefault(key, defaultValue);
    }
    
    /**
     * 获取整数配置值
     */
    public Integer getIntConfig(String key) {
        String value = getConfig(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置值不是有效的整数: key={}, value={}", key, value);
            return null;
        }
    }
    
    /**
     * 获取整数配置值，带默认值
     */
    public Integer getIntConfig(String key, Integer defaultValue) {
        String value = getConfig(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置值不是有效的整数: key={}, value={}", key, value);
            return defaultValue;
        }
    }
    
    /**
     * 获取布尔配置值
     */
    public Boolean getBooleanConfig(String key) {
        String value = getConfig(key);
        if (value == null) {
            return null;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
    
    /**
     * 获取布尔配置值，带默认值
     */
    public Boolean getBooleanConfig(String key, Boolean defaultValue) {
        String value = getConfig(key);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }
    
    /**
     * 设置配置值
     */
    public void setConfig(String key, String value) {
        try {
            // 直接更新缓存，不再更新数据库
            configCache.put(key, value);
            log.info("更新配置：{} = {}", key, value);
        } catch (Exception e) {
            log.error("设置配置失败：key={}, value={}", key, value, e);
            throw new RuntimeException("设置配置失败");
        }
    }
    
    /**
     * 删除配置
     */
    public void removeConfig(String key) {
        try {
            // 直接从缓存删除，不再操作数据库
            configCache.remove(key);
            log.info("删除配置：{}", key);
        } catch (Exception e) {
            log.error("删除配置失败：{}", key, e);
            throw new RuntimeException("删除配置失败");
        }
    }
    
    /**
     * 刷新配置缓存
     */
    public void refreshCache() {
        loadAllConfigs();
        log.info("刷新配置缓存完成");
    }
    
    /**
     * 获取所有配置键
     */
    public java.util.Set<String> getAllKeys() {
        return configCache.keySet();
    }
    
    /**
     * 检查配置是否存在
     */
    public boolean containsKey(String key) {
        return configCache.containsKey(key);
    }
}