package cn.edu.ccst.communitysocialmain.service;

import cn.edu.ccst.communitysocialmain.dto.SystemConfigDTO;
import cn.edu.ccst.communitysocialmain.entity.SystemConfig;

import java.util.List;

public interface SystemConfigService {
    
    /**
     * 获取所有系统配置
     */
    List<SystemConfig> getAllConfigs();
    
    /**
     * 根据配置键获取配置
     */
    SystemConfig getConfigByKey(String configKey);
    
    /**
     * 更新系统配置
     */
    SystemConfig updateConfig(SystemConfigDTO configDTO);
    
    /**
     * 重置配置为默认值
     */
    void resetConfig(String configKey);
    
    /**
     * 批量更新配置
     */
    void batchUpdateConfigs(List<SystemConfigDTO> configDTOs);
    
    /**
     * 获取配置值（带默认值）
     */
    String getConfigValue(String configKey, String defaultValue);
    
    /**
     * 初始化默认配置
     */
    void initializeDefaultConfigs();
}