package cn.edu.ccst.communitysocialmain.mapper;

import cn.edu.ccst.communitysocialmain.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 版块分类Mapper接口
 */
@Mapper
public interface CategoryMapper {
    
    /**
     * 根据分类ID查询分类
     */
    Category selectById(@Param("categoryId") Integer categoryId);
    
    /**
     * 根据分类名称查询分类
     */
    Category selectByName(@Param("name") String name);
    
    /**
     * 查询所有分类
     */
    List<Category> selectAll();
    
    /**
     * 查询所有启用的分类
     */
    List<Category> selectEnabled();
    
    /**
     * 统计某个分类下的帖子数量
     */
    Long countPostsByCategoryId(@Param("categoryId") Integer categoryId);
    
    /**
     * 统计分类总数
     */
    Long countAll();
    
    /**
     * 插入分类
     */
    int insert(Category category);
    
    /**
     * 更新分类
     */
    int update(Category category);
    
    /**
     * 更新分类状态
     */
    int updateStatus(@Param("categoryId") Integer categoryId, 
                    @Param("status") Integer status);
    
    /**
     * 删除分类
     */
    int deleteById(@Param("categoryId") Integer categoryId);
}