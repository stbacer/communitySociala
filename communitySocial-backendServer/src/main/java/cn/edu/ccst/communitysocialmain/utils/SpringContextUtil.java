package cn.edu.ccst.communitysocialmain.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 上下文工具类
 * 用于在非 Spring 管理的类中获取 Bean
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    /**
     * 获取 ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 根据名称获取 Bean
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String name) {
        if (applicationContext == null) {
            throw new RuntimeException("ApplicationContext 未初始化");
        }
        return (T) applicationContext.getBean(name);
    }

    /**
     * 根据类型获取 Bean
     */
    public static <T> T getBean(Class<T> clazz) {
        if (applicationContext == null) {
            throw new RuntimeException("ApplicationContext 未初始化");
        }
        return applicationContext.getBean(clazz);
    }

    /**
     * 根据名称和类型获取 Bean
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        if (applicationContext == null) {
            throw new RuntimeException("ApplicationContext 未初始化");
        }
        return applicationContext.getBean(name, clazz);
    }
}
