package cn.edu.ccst.communitysocialmain.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 */
@Slf4j
@Component
public class RedisUtil {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 设置缓存
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Redis缓存设置成功: {}", key);
        } catch (Exception e) {
            log.error("设置缓存失败: {}", key, e);
        }
    }
    
    /**
     * 设置缓存并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("Redis缓存设置成功（带过期时间）: {}, 过期时间: {} {}", key, timeout, unit);
        } catch (Exception e) {
            log.error("设置缓存失败: {}", key, e);
        }
    }
    
    /**
     * 获取缓存
     */
    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("获取缓存失败: {}", key, e);
            return null;
        }
    }
    
    /**
     * 删除缓存
     */
    public Boolean delete(String key) {
        try {
            return redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("删除缓存失败: {}", key, e);
            return false;
        }
    }
    
    /**
     * 判断缓存是否存在
     */
    public Boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("检查缓存存在失败: {}", key, e);
            return false;
        }
    }
    
    /**
     * 设置过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.error("设置过期时间失败: {}", key, e);
            return false;
        }
    }
}

