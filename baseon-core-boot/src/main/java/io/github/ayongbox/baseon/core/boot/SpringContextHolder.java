package io.github.ayongbox.baseon.core.boot;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Objects;

/**
 * Spring 容器静态持有者：供无法注入 Bean 的场景（工具类、静态方法、第三方回调）获取容器能力。
 *
 * <p>由 {@code BaseonCoreBootAutoConfiguration} 注册为 Bean，容器启动时自动回填；
 * 业务代码优先使用构造器注入，只有在无法注入时才使用本类。
 *
 * <p>容器未初始化时除 {@link #getBeanOrNull(Class)} / {@link #containsBean(String)} 外均抛
 * {@link IllegalStateException}，避免静态调用拿到 null 后静默失败。
 */
public class SpringContextHolder implements ApplicationContextAware {

    private static final String NOT_READY_MESSAGE =
            "Spring 容器尚未初始化：请确认 baseon-core-boot 的自动装配已生效（baseon.boot.enabled=true）";

    private static volatile ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /**
     * 获取当前容器，未初始化直接抛异常。
     */
    public static ApplicationContext getApplicationContext() {
        ApplicationContext current = context;
        if (current == null) {
            throw new IllegalStateException(NOT_READY_MESSAGE);
        }
        return current;
    }

    public static <T> T getBean(Class<T> type) {
        return getApplicationContext().getBean(type);
    }

    public static <T> T getBean(String name, Class<T> type) {
        return getApplicationContext().getBean(name, type);
    }

    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    /**
     * 获取可选 Bean：容器未就绪或 Bean 不存在时返回 {@code null}。
     */
    public static <T> T getBeanOrNull(Class<T> type) {
        Objects.requireNonNull(type, "type");
        ApplicationContext current = context;
        if (current == null) {
            return null;
        }
        try {
            return current.getBean(type);
        } catch (NoSuchBeanDefinitionException ex) {
            return null;
        }
    }

    public static boolean containsBean(String name) {
        ApplicationContext current = context;
        return current != null && current.containsBean(name);
    }

    /**
     * 发布事件，监听方使用 {@code @EventListener} 接收。
     */
    public static void publishEvent(Object event) {
        getApplicationContext().publishEvent(event);
    }

    public static String[] getActiveProfiles() {
        return getApplicationContext().getEnvironment().getActiveProfiles();
    }

    public static String getProperty(String key) {
        return getApplicationContext().getEnvironment().getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return getApplicationContext().getEnvironment().getProperty(key, defaultValue);
    }

    public static <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        return getApplicationContext().getEnvironment().getProperty(key, targetType, defaultValue);
    }
}
