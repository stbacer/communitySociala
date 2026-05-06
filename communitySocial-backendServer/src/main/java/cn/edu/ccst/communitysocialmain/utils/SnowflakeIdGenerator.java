package cn.edu.ccst.communitysocialmain.utils;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 分布式ID生成器（基于雪花算法）
 * 
 * 生成的ID特点：
 * 1. 全局唯一
 * 2. 趋势递增（按时间有序）
 * 3. 纯数字（Long类型）
 * 4. 高性能（本地生成，无需网络请求）
 */
@Slf4j
@Component
public class SnowflakeIdGenerator {
    
    /**
     * 雪花算法实例
     * workerId: 工作机器ID (0-31)
     * datacenterId: 数据中心ID (0-31)
     * 
     * 单节点部署：workerId=1, datacenterId=1
     * 多节点部署：每个节点配置不同的 workerId
     */
    private static Snowflake snowflake;
    
    @PostConstruct
    public void init() {
        // 从配置文件读取 workerId 和 datacenterId，默认为 1
        long workerId = 1L;
        long datacenterId = 1L;
        
        snowflake = IdUtil.getSnowflake(workerId, datacenterId);
        log.info("雪花算法ID生成器初始化成功 - workerId: {}, datacenterId: {}", workerId, datacenterId);
    }
    
    /**
     * 生成下一个ID（Long类型）
     * 
     * @return Long类型的唯一ID
     */
    public static Long nextId() {
        return snowflake.nextId();
    }
    
    /**
     * 生成下一个ID（String类型）
     * 
     * @return String类型的唯一ID
     */
    public static String nextIdStr() {
        return snowflake.nextIdStr();
    }
    
    /**
     * 解析ID中的时间戳
     * 
     * @param id 雪花ID
     * @return 时间戳（毫秒）
     */
    public static long getTimestamp(long id) {
        return snowflake.getGenerateDateTime(id);
    }
}
