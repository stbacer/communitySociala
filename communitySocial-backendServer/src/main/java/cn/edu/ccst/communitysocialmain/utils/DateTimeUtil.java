package cn.edu.ccst.communitysocialmain.utils;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 日期时间工具类
 */
@Slf4j
@Component
public class DateTimeUtil {
    
    /**
     * 获取当前时间戳
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
    
    /**
     * 获取当前日期字符串 (yyyy-MM-dd)
     */
    public static String getCurrentDate() {
        return DateUtil.format(new Date(), "yyyy-MM-dd");
    }
    
    /**
     * 获取当前时间字符串 (yyyy-MM-dd HH:mm:ss)
     */
    public static String getCurrentDateTime() {
        return DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss");
    }
    
    /**
     * 格式化日期
     */
    public static String formatDate(Date date, String pattern) {
        if (date == null || StrUtil.isBlank(pattern)) {
            return "";
        }
        return DateUtil.format(date, pattern);
    }
    
    /**
     * 解析日期字符串
     */
    public static Date parseDate(String dateStr, String pattern) {
        if (StrUtil.isBlank(dateStr) || StrUtil.isBlank(pattern)) {
            return null;
        }
        try {
            return DateUtil.parse(dateStr, pattern);
        } catch (Exception e) {
            log.error("解析日期失败: dateStr={}, pattern={}", dateStr, pattern, e);
            return null;
        }
    }
    
    /**
     * 计算两个日期相差的天数
     */
    public static long daysBetween(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return DateUtil.betweenDay(startDate, endDate, false);
    }
    
    /**
     * 计算两个日期相差的小时数
     */
    public static long hoursBetween(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return DateUtil.between(startDate, endDate, DateUnit.HOUR);
    }
    
    /**
     * 计算两个日期相差的分钟数
     */
    public static long minutesBetween(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return DateUtil.between(startDate, endDate, DateUnit.MINUTE);
    }
    
    /**
     * 判断是否为今天
     */
    public static boolean isToday(Date date) {
        if (date == null) {
            return false;
        }
        return DateUtil.isSameDay(date, new Date());
    }
    
    /**
     * 判断是否为昨天
     */
    public static boolean isYesterday(Date date) {
        if (date == null) {
            return false;
        }
        Date yesterday = DateUtil.yesterday();
        return DateUtil.isSameDay(date, yesterday);
    }
    
    /**
     * 获取今天的开始时间
     */
    public static Date getTodayStart() {
        return DateUtil.beginOfDay(new Date());
    }
    
    /**
     * 获取今天的结束时间
     */
    public static Date getTodayEnd() {
        return DateUtil.endOfDay(new Date());
    }
    
    /**
     * 获取本周开始时间
     */
    public static Date getWeekStart() {
        return DateUtil.beginOfWeek(new Date());
    }
    
    /**
     * 获取本月开始时间
     */
    public static Date getMonthStart() {
        return DateUtil.beginOfMonth(new Date());
    }
    
    /**
     * 获取本年开始时间
     */
    public static Date getYearStart() {
        return DateUtil.beginOfYear(new Date());
    }
    
    /**
     * 在指定日期上增加天数
     */
    public static Date addDays(Date date, int days) {
        if (date == null) {
            return null;
        }
        return DateUtil.offsetDay(date, days);
    }
    
    /**
     * 在指定日期上增加小时数
     */
    public static Date addHours(Date date, int hours) {
        if (date == null) {
            return null;
        }
        return DateUtil.offsetHour(date, hours);
    }
    
    /**
     * 在指定日期上增加分钟数
     */
    public static Date addMinutes(Date date, int minutes) {
        if (date == null) {
            return null;
        }
        return DateUtil.offsetMinute(date, minutes);
    }
    
    /**
     * 将时间转换为友好的显示格式
     */
    public static String formatFriendlyTime(Date date) {
        if (date == null) {
            return "";
        }
        
        long between = System.currentTimeMillis() - date.getTime();
        long minute = between / (1000 * 60);
        long hour = between / (1000 * 60 * 60);
        long day = between / (1000 * 60 * 60 * 24);
        
        if (minute < 1) {
            return "刚刚";
        } else if (minute < 60) {
            return minute + "分钟前";
        } else if (hour < 24) {
            return hour + "小时前";
        } else if (day < 30) {
            return day + "天前";
        } else {
            return formatDate(date, "yyyy-MM-dd");
        }
    }
}