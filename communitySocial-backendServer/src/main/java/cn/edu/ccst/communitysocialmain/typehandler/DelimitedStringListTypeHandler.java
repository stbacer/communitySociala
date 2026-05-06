package cn.edu.ccst.communitysocialmain.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分隔符字符串列表类型处理器
 * 用于处理 List<String> 与数据库分隔符字符串之间的转换
 * 
 * 使用示例:
 * - 图片URL列表: "url1|url2|url3" ↔ List<String>
 * - 标签列表: "tag1,tag2,tag3" ↔ List<String>
 */
public class DelimitedStringListTypeHandler extends BaseTypeHandler<List<String>> {
    
    private static final String DELIMITER = "|";  // 使用竖线作为分隔符
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.isEmpty()) {
            ps.setString(i, "");
        } else {
            // 过滤空字符串并拼接
            String result = parameter.stream()
                    .filter(url -> url != null && !url.trim().isEmpty())
                    .collect(Collectors.joining(DELIMITER));
            ps.setString(i, result);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return parseStringToList(value);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return parseStringToList(value);
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return parseStringToList(value);
    }

    /**
     * 将分隔符字符串解析为List
     */
    private List<String> parseStringToList(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // 按分隔符拆分,过滤空字符串
        return Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
