package cn.edu.ccst.communitysocialmain.mapper;

import cn.edu.ccst.communitysocialmain.entity.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置Mapper接口
 */
@Mapper
public interface SystemConfigMapper {
    
    /**
     * 根据配置键查询配置
     */
    SystemConfig selectByKey(@Param("configKey") String configKey);
    
    /**
     * 查询所有启用的配置
     */
    List<SystemConfig> selectAllEnabled();
    
    /**
     * 查询所有配置
     */
    List<SystemConfig> selectAll();
    
    /**
     * 统计配置总数
     */
    Long countAll();
    
    /**
     * 插入配置
     */
    int insert(SystemConfig systemConfig);
    
    /**
     * 更新配置
     */
    int update(SystemConfig systemConfig);
    
    /**
     * 更新配置状态
     */
    int updateStatus(@Param("configId") Integer configId, 
                    @Param("status") Integer status);
    
    /**
     * 根据配置键删除配置
     */
    int deleteByKey(@Param("configKey") String configKey);
    
    /**
     * 删除配置
     */
    int deleteById(@Param("configId") Integer configId);
}