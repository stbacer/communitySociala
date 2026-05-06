package cn.edu.ccst.communitysocialmain.service.impl;

import cn.edu.ccst.communitysocialmain.dto.SensitiveWordDTO;
import cn.edu.ccst.communitysocialmain.entity.SensitiveWord;
import cn.edu.ccst.communitysocialmain.mapper.SensitiveWordMapper;
import cn.edu.ccst.communitysocialmain.service.SensitiveWordService;
import cn.edu.ccst.communitysocialmain.utils.CommonUtil;
import cn.edu.ccst.communitysocialmain.utils.SensitiveWordFilter;
import cn.edu.ccst.communitysocialmain.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;
    
    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;

    @Override
    public PageVO<SensitiveWord> getSensitiveWords(Integer page, Integer size, String keyword, String type, Integer status) {
        int offset = (page - 1) * size;
        List<SensitiveWord> words = sensitiveWordMapper.selectByCondition(keyword, type, status, offset, size);
        Long total = sensitiveWordMapper.countByCondition(keyword, type, status);
        
        return new PageVO<>(page, size, total, words);
    }

    @Override
    public SensitiveWord getSensitiveWordById(Integer wordId) {
        return sensitiveWordMapper.selectById(wordId);
    }

    @Override
    @Transactional
    public SensitiveWord createSensitiveWord(SensitiveWordDTO sensitiveWordDTO) {
        // 检查敏感词是否已存在
        SensitiveWord existingWord = sensitiveWordMapper.selectByWord(sensitiveWordDTO.getWord());
        if (existingWord != null) {
            throw new RuntimeException("敏感词已存在");
        }
        
        SensitiveWord word = new SensitiveWord();
        BeanUtils.copyProperties(sensitiveWordDTO, word);
        word.setWordId(null);
        word.setCreateTime(LocalDateTime.now());
        word.setUpdateTime(LocalDateTime.now());
        
        int result = sensitiveWordMapper.insert(word);
        if (result <= 0) {
            throw new RuntimeException("添加敏感词失败");
        }
        
        // 刷新敏感词缓存
        sensitiveWordFilter.loadSensitiveWords();
        
        return word;
    }

    @Override
    @Transactional
    public SensitiveWord updateSensitiveWord(Integer wordId, SensitiveWordDTO sensitiveWordDTO) {
        SensitiveWord existingWord = sensitiveWordMapper.selectById(wordId);
        if (existingWord == null) {
            throw new RuntimeException("敏感词不存在");
        }
        
        // 检查是否与其他敏感词冲突
        if (!existingWord.getWord().equals(sensitiveWordDTO.getWord())) {
            SensitiveWord conflictWord = sensitiveWordMapper.selectByWord(sensitiveWordDTO.getWord());
            if (conflictWord != null && !conflictWord.getWordId().equals(wordId)) {
                throw new RuntimeException("敏感词已存在");
            }
        }
        
        BeanUtils.copyProperties(sensitiveWordDTO, existingWord);
        existingWord.setWordId(wordId);
        existingWord.setUpdateTime(LocalDateTime.now());
        
        int result = sensitiveWordMapper.update(existingWord);
        if (result <= 0) {
            throw new RuntimeException("更新敏感词失败");
        }
        
        // 刷新敏感词缓存
        sensitiveWordFilter.loadSensitiveWords();
        
        return existingWord;
    }

    @Override
    @Transactional
    public void deleteSensitiveWord(Integer wordId) {
        SensitiveWord word = sensitiveWordMapper.selectById(wordId);
        if (word == null) {
            throw new RuntimeException("敏感词不存在");
        }
        
        int result = sensitiveWordMapper.deleteById(wordId);
        if (result <= 0) {
            throw new RuntimeException("删除敏感词失败");
        }
        
        // 刷新敏感词缓存
        sensitiveWordFilter.loadSensitiveWords();
    }
    
    @Override
    @Transactional
    public void hardDeleteSensitiveWord(Integer wordId) {
        SensitiveWord word = sensitiveWordMapper.selectById(wordId);
        if (word == null) {
            throw new RuntimeException("敏感词不存在");
        }
        
        int result = sensitiveWordMapper.hardDeleteById(wordId);
        if (result <= 0) {
            throw new RuntimeException("硬删除敏感词失败");
        }
        
        // 刷新敏感词缓存
        sensitiveWordFilter.loadSensitiveWords();
    }

    @Override
    @Transactional
    public void batchDeleteSensitiveWords(List<Integer> wordIds) {
        if (wordIds == null || wordIds.isEmpty()) {
            return;
        }
        
        int result = sensitiveWordMapper.batchDelete(wordIds);
        if (result <= 0) {
            throw new RuntimeException("批量删除敏感词失败");
        }
        
        // 刷新敏感词缓存
        sensitiveWordFilter.loadSensitiveWords();
    }

    @Override
    @Transactional
    public void updateSensitiveWordStatus(Integer wordId, Integer status) {
        SensitiveWord word = sensitiveWordMapper.selectById(wordId);
        if (word == null) {
            throw new RuntimeException("敏感词不存在");
        }
        
        word.setStatus(status);
        word.setUpdateTime(LocalDateTime.now());
        
        int result = sensitiveWordMapper.update(word);
        if (result <= 0) {
            throw new RuntimeException("更新敏感词状态失败");
        }
        
        // 刷新敏感词缓存
        sensitiveWordFilter.loadSensitiveWords();
    }

    @Override
    public List<SensitiveWord> getAllEnabledWords() {
        return sensitiveWordMapper.selectAllEnabled();
    }
}