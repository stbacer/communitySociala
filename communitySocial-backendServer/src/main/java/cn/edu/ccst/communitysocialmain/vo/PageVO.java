package cn.edu.ccst.communitysocialmain.vo;

import lombok.Data;

import java.util.List;

/**
 * 分页结果VO
 */
@Data
public class PageVO<T> {
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer size;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 总页数
     */
    private Integer totalPages;
    
    /**
     * 数据列表
     */
    private List<T> records;
    
    public PageVO() {}
    
    public PageVO(Integer page, Integer size, Long total, List<T> records) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.records = records;
        this.totalPages = (int) Math.ceil((double) total / size);
    }
}