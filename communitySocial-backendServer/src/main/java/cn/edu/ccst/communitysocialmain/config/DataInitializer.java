package cn.edu.ccst.communitysocialmain.config;

import cn.edu.ccst.communitysocialmain.entity.Category;
import cn.edu.ccst.communitysocialmain.mapper.CategoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据初始化配置
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public void run(String... args) throws Exception {
        initCategories();
    }

    /**
     * 初始化分类数据
     */
    private void initCategories() {
        try {
            List<Category> categories = categoryMapper.selectAll();
            if (categories == null || categories.isEmpty()) {
                log.info("开始初始化分类数据...");
                
                Category[] categoryArray = new Category[]{
                    createCategory("二手", "二手物品", 1),
                    createCategory("互助", "邻里互助", 2),
                    createCategory("活动", "活动公告", 3)
                };
                
                for (Category category : categoryArray) {
                    categoryMapper.insert(category);
                    log.info("插入分类：{}", category.getName());
                }
                
                log.info("分类数据初始化完成，共{}个分类", categoryArray.length);
            } else {
                log.info("分类数据已存在，跳过初始化");
            }
        } catch (Exception e) {
            log.error("初始化分类数据失败", e);
        }
    }

    /**
     * 创建分类对象
     */
    private Category createCategory(String name, String description, Integer sortOrder) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setSortOrder(sortOrder);
        category.setStatus(1); // 启用状态
        category.setCreateTime(java.time.LocalDateTime.now());
        return category;
    }
}
