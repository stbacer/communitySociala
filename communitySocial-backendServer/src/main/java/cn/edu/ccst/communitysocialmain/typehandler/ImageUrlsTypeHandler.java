package cn.edu.ccst.communitysocialmain.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import com.alibaba.fastjson.JSON;
import java.sql.*;
import java.util.List;

/**
 * 图片URL列表类型处理器
 * 处理List<String>与数据库JSON字符串之间的转换
 */
public class ImageUrlsTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null || parameter.isEmpty()) {
            ps.setString(i, "[]");
        } else {
            ps.setString(i, JSON.toJSONString(parameter));
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJsonToList(json);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJsonToList(json);
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJsonToList(json);
    }

    private List<String> parseJsonToList(String json) {
        if (json == null || json.trim().isEmpty() || "null".equals(json)) {
            return new java.util.ArrayList<>();
        }
        try {
            return JSON.parseArray(json, String.class);
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }
}