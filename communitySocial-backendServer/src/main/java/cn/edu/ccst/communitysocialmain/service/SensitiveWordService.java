package cn.edu.ccst.communitysocialmain.service;

import cn.edu.ccst.communitysocialmain.dto.SensitiveWordDTO;
import cn.edu.ccst.communitysocialmain.entity.SensitiveWord;
import cn.edu.ccst.communitysocialmain.vo.PageVO;

import java.util.List;

public interface SensitiveWordService {
    
    /**
     * 获取敏感词列表（分页）
     */
    PageVO<SensitiveWord> getSensitiveWords(Integer page, Integer size, String keyword, String type, Integer status);
    
    /**
     * 根据ID获取敏感词
     */
    SensitiveWord getSensitiveWordById(Integer wordId);
    
    /**
     * 创建敏感词
     */
    SensitiveWord createSensitiveWord(SensitiveWordDTO sensitiveWordDTO);
    
    /**
     * 更新敏感词
     */
    SensitiveWord updateSensitiveWord(Integer wordId, SensitiveWordDTO sensitiveWordDTO);
    
    /**
     * 删除敏感词（软删除）
     */
    void deleteSensitiveWord(Integer wordId);
    
    /**
     * 硬删除敏感词（物理删除）
     */
    void hardDeleteSensitiveWord(Integer wordId);
    
    /**
     * 批量删除敏感词
     */
    void batchDeleteSensitiveWords(List<Integer> wordIds);
    
    /**
     * 启用/禁用敏感词
     */
    void updateSensitiveWordStatus(Integer wordId, Integer status);
    
    /**
     * 获取所有启用的敏感词
     */
    List<SensitiveWord> getAllEnabledWords();
}