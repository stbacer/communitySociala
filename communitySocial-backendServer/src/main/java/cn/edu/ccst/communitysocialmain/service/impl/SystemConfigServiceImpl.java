package cn.edu.ccst.communitysocialmain.service.impl;

import cn.edu.ccst.communitysocialmain.dto.SystemConfigDTO;
import cn.edu.ccst.communitysocialmain.entity.SystemConfig;
import cn.edu.ccst.communitysocialmain.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    // 使用内存存储配置
    private final Map<String, SystemConfig> configStore = new ConcurrentHashMap<>();

    // 默认配置项
    private static final Map<String, String> DEFAULT_CONFIGS = new HashMap<String, String>() {{
        put("site_name", "社区社交平台");
        put("site_description", "基于微信小程序的社区社交平台");
        put("max_upload_size", "10485760"); // 10MB
        put("allowed_file_types", "jpg,jpeg,png,gif");
        put("sensitive_check_enabled", "true");
        put("auto_review_enabled", "false");
        put("max_daily_posts", "10");
        put("max_daily_comments", "50");
    }};
    
    /**
     * 初始化默认配置
     */
    @PostConstruct
    public void init() {
        initializeDefaultConfigs();
    }

    @Override
    public List<SystemConfig> getAllConfigs() {
        return new ArrayList<>(configStore.values());
    }

    @Override
    public SystemConfig getConfigByKey(String configKey) {
        return configStore.get(configKey);
    }

    @Override
    public SystemConfig updateConfig(SystemConfigDTO configDTO) {
        SystemConfig existingConfig = configStore.get(configDTO.getConfigKey());
        
        if (existingConfig != null) {
            // 更新现有配置
            existingConfig.setConfigValue(configDTO.getConfigValue());
            existingConfig.setDescription(configDTO.getDescription());
            existingConfig.setUpdateTime(LocalDateTime.now());
        } else {
            // 创建新配置
            SystemConfig newConfig = new SystemConfig();
            BeanUtils.copyProperties(configDTO, newConfig);
            newConfig.setCreateTime(LocalDateTime.now());
            newConfig.setUpdateTime(LocalDateTime.now());
            configStore.put(newConfig.getConfigKey(), newConfig);
            existingConfig = newConfig;
        }
        
        return existingConfig;
    }

    @Override
    public void resetConfig(String configKey) {
        String defaultValue = DEFAULT_CONFIGS.get(configKey);
        if (defaultValue == null) {
            throw new RuntimeException("该配置项没有默认值");
        }
        
        SystemConfig config = configStore.get(configKey);
        if (config != null) {
            config.setConfigValue(defaultValue);
            config.setUpdateTime(LocalDateTime.now());
        } else {
            // 如果配置不存在，则创建
            SystemConfig newConfig = new SystemConfig();
            newConfig.setConfigKey(configKey);
            newConfig.setConfigValue(defaultValue);
            newConfig.setDescription("系统默认配置");
            newConfig.setCreateTime(LocalDateTime.now());
            newConfig.setUpdateTime(LocalDateTime.now());
            configStore.put(configKey, newConfig);
        }
    }

    @Override
    public void batchUpdateConfigs(List<SystemConfigDTO> configDTOs) {
        if (configDTOs == null || configDTOs.isEmpty()) {
            return;
        }
        
        for (SystemConfigDTO configDTO : configDTOs) {
            updateConfig(configDTO);
        }
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        SystemConfig config = configStore.get(configKey);
        return config != null ? config.getConfigValue() : defaultValue;
    }

    @Override
    public void initializeDefaultConfigs() {
        for (Map.Entry<String, String> entry : DEFAULT_CONFIGS.entrySet()) {
            SystemConfig existingConfig = configStore.get(entry.getKey());
            if (existingConfig == null) {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(entry.getKey());
                config.setConfigValue(entry.getValue());
                config.setDescription("系统默认配置");
                config.setCreateTime(LocalDateTime.now());
                config.setUpdateTime(LocalDateTime.now());
                configStore.put(entry.getKey(), config);
            }
        }
    }
}