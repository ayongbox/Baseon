package io.github.ayongbox.baseon.starter.datasource.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 显式指定数据源。标注在方法上优先于标注在类上。
 *
 * <p><b>必须在事务边界外使用</b>：Spring 事务开启时连接已绑定到当前线程，
 * 事务内切换数据源不会重新获取连接，注解将静默失效。正确用法是标注在
 * 非事务的入口方法（或外层 Facade）上，再调用内部的 @Transactional 方法。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DS {

    /**
     * 数据源名，如 "master"、"slave1"。
     */
    String value();
}
