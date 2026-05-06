package cn.edu.ccst.communitysocialmain.mapper;

import cn.edu.ccst.communitysocialmain.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 敏感词Mapper接口
 */
@Mapper
public interface SensitiveWordMapper {
    
    /**
     * 根据敏感词ID查询敏感词
     */
    SensitiveWord selectById(@Param("wordId") Integer wordId);
    
    /**
     * 根据敏感词查询
     */
    SensitiveWord selectByWord(@Param("word") String word);
    
    /**
     * 查询所有启用的敏感词
     */
    List<SensitiveWord> selectAllEnabled();
    
    /**
     * 查询敏感词列表（分页，支持关键词搜索）
     */
    List<SensitiveWord> selectByCondition(@Param("keyword") String keyword,
                                         @Param("type") String type,
                                         @Param("status") Integer status,
                                         @Param("offset") Integer offset,
                                         @Param("limit") Integer limit);
    
    /**
     * 统计符合条件的敏感词数量
     */
    Long countByCondition(@Param("keyword") String keyword,
                         @Param("type") String type,
                         @Param("status") Integer status);
    
    /**
     * 批量删除敏感词
     */
    int batchDelete(@Param("wordIds") List<Integer> wordIds);
    
    /**
     * 统计敏感词总数
     */
    Long countAll();
    
    /**
     * 插入敏感词
     */
    int insert(SensitiveWord sensitiveWord);
    
    /**
     * 更新敏感词
     */
    int update(SensitiveWord sensitiveWord);
    
    /**
     * 更新敏感词状态
     */
    int updateStatus(@Param("wordId") Integer wordId,
                    @Param("status") Integer status);
    
    /**
     * 删除敏感词（软删除）
     */
    int deleteById(@Param("wordId") Integer wordId);
    
    /**
     * 硬删除敏感词（物理删除）
     */
    int hardDeleteById(@Param("wordId") Integer wordId);
}