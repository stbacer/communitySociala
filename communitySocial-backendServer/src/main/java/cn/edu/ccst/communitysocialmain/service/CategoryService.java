package cn.edu.ccst.communitysocialmain.service;

import cn.edu.ccst.communitysocialmain.entity.Category;

import java.util.List;

public interface CategoryService {
    
    /**
     * 获取所有分类
     */
    List<Category> getAllCategories();
    
    /**
     * 根据ID获取分类
     */
    Category getCategoryById(Integer categoryId);
    
    /**
     * 创建分类
     */
    Category createCategory(Category category);
    
    /**
     * 更新分类
     */
    Category updateCategory(Category category);
    
    /**
     * 删除分类
     */
    void deleteCategory(Integer categoryId);
    
    /**
     * 根据名称获取分类
     */
    Category getCategoryByName(String categoryName);
    
    /**
     * 获取启用的分类列表
     */
    List<Category> getEnabledCategories();
}