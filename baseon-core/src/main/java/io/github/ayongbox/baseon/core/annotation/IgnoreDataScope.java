package io.github.ayongbox.baseon.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法/类不参与数据范围 SQL 过滤（跳过 @DataScope 注入）。
 *
 * <p>典型场景：跨部门统计、系统任务全量扫描、管理员后台。
 * 由 starter-data-scope 的切面识别，未引入该 starter 时注解不生效。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreDataScope {
}
