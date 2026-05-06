package cn.edu.ccst.communitysocialmain.controller.resident;

import cn.edu.ccst.communitysocialmain.entity.Category;
import cn.edu.ccst.communitysocialmain.service.CategoryService;
import cn.edu.ccst.communitysocialmain.vo.ResultVO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 居民端分类控制器
 * 提供分类相关的公开接口
 */
@Slf4j
@RestController
@RequestMapping("/resident/category")

public class ResidentCategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 获取分类列表
     */
    @GetMapping("/list")

    public ResultVO<List<Category>> getCategoryList(HttpServletRequest request) {
        try {
            List<Category> categories = categoryService.getEnabledCategories();
            return ResultVO.success(categories);
        } catch (Exception e) {
            log.error("获取分类列表失败", e);
            return ResultVO.error("获取分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取分类详情
     */
    @GetMapping("/{categoryId}")

    public ResultVO<Category> getCategoryById(@PathVariable Integer categoryId,
                                            HttpServletRequest request) {
        try {
            Category category = categoryService.getCategoryById(categoryId);
            if (category == null) {
                return ResultVO.error("分类不存在");
            }
            return ResultVO.success(category);
        } catch (Exception e) {
            log.error("获取分类详情失败", e);
            return ResultVO.error("获取分类详情失败: " + e.getMessage());
        }
    }

    /**
     * 根据名称获取分类
     */
    @GetMapping("/name/{categoryName}")

    public ResultVO<Category> getCategoryByName(@PathVariable String categoryName,
                                              HttpServletRequest request) {
        try {
            Category category = categoryService.getCategoryByName(categoryName);
            if (category == null) {
                return ResultVO.error("分类不存在");
            }
            return ResultVO.success(category);
        } catch (Exception e) {
            log.error("根据名称获取分类失败", e);
            return ResultVO.error("获取分类失败: " + e.getMessage());
        }
    }
}