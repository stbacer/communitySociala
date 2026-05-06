package cn.edu.ccst.communitysocialmain.service.impl;

import cn.edu.ccst.communitysocialmain.entity.Category;
import cn.edu.ccst.communitysocialmain.mapper.CategoryMapper;
import cn.edu.ccst.communitysocialmain.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<Category> getAllCategories() {
        return categoryMapper.selectAll();
    }

    @Override
    public Category getCategoryById(Integer categoryId) {
        return categoryMapper.selectById(categoryId);
    }

    @Override
    @Transactional
    public Category createCategory(Category category) {
        // 检查分类名称是否已存在
        Category existingCategory = categoryMapper.selectByName(category.getName());
        if (existingCategory != null) {
            throw new RuntimeException("分类名称已存在");
        }
        
        category.setCategoryId(null);
        category.setCreateTime(LocalDateTime.now());
        category.setStatus(1); // 默认启用
        
        int result = categoryMapper.insert(category);
        if (result <= 0) {
            throw new RuntimeException("创建分类失败");
        }
        
        return category;
    }

    @Override
    @Transactional
    public Category updateCategory(Category category) {
        Category existingCategory = categoryMapper.selectById(category.getCategoryId());
        if (existingCategory == null) {
            throw new RuntimeException("分类不存在");
        }
        
        // 检查名称是否与其他分类冲突
        if (!existingCategory.getName().equals(category.getName())) {
            Category conflictCategory = categoryMapper.selectByName(category.getName());
            if (conflictCategory != null && !conflictCategory.getCategoryId().equals(category.getCategoryId())) {
                throw new RuntimeException("分类名称已存在");
            }
        }
        

        
        int result = categoryMapper.update(category);
        if (result <= 0) {
            throw new RuntimeException("更新分类失败");
        }
        
        return category;
    }

    @Override
    @Transactional
    public void deleteCategory(Integer categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        
        // 检查是否有帖子使用该分类
        Long postCount = categoryMapper.countPostsByCategoryId(categoryId);
        if (postCount > 0) {
            throw new RuntimeException("该分类下还有帖子，无法删除");
        }
        
        int result = categoryMapper.deleteById(categoryId);
        if (result <= 0) {
            throw new RuntimeException("删除分类失败");
        }
    }

    @Override
    public Category getCategoryByName(String categoryName) {
        return categoryMapper.selectByName(categoryName);
    }

    @Override
    public List<Category> getEnabledCategories() {
        return categoryMapper.selectEnabled();
    }
}