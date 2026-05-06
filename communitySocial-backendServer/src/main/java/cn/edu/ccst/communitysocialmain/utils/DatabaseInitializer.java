package cn.edu.ccst.communitysocialmain.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据库初始化工具类
 * 用于初始化和更新数据库表结构
 */
@Slf4j
@Component
public class DatabaseInitializer {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @PostConstruct
    public void initDatabase() {
        // 立即执行数据库初始化
        initializeNow();
    }
    
    /**
     * 立即执行数据库初始化
     */
    public void initializeNow() {
        try {
            // 检查并添加message_type字段
            addMessageTypeColumn();
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
        }
    }
    
    /**
     * 添加message_type字段到private_message表
     */
    private void addMessageTypeColumn() {
        try {
            // 检查字段是否存在
            String checkSql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                             "WHERE TABLE_SCHEMA = DATABASE() " +
                             "AND TABLE_NAME = 'private_message' " +
                             "AND COLUMN_NAME = 'message_type'";
            
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
            
            if (count == null || count == 0) {
                log.info("添加message_type字段到private_message表");
                
                // 添加字段
                String addColumnSql = "ALTER TABLE private_message " +
                                    "ADD COLUMN message_type INT DEFAULT 1 " +
                                    "COMMENT '消息类型：1文本，2图片，3语音' " +
                                    "AFTER image_url";
                jdbcTemplate.execute(addColumnSql);
                
                // 更新现有数据
                String updateSql = "UPDATE private_message SET message_type = 1 WHERE message_type IS NULL";
                jdbcTemplate.update(updateSql);
                
                log.info("message_type字段添加成功");
            }
        } catch (Exception e) {
            log.error("添加message_type字段失败", e);
        }
    }
}