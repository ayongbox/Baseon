package io.github.ayongbox.baseon.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计日志：标注在需要记录「谁、在什么时间、对哪个模块做了什么操作」的入口方法上。
 *
 * <p>由 baseon-starter-log 的切面消费，与 starter-web 的全量请求日志分工不同：
 * 请求日志是访问流水（每个请求一条），操作日志是业务语义审计（只记标注了本注解的关键操作），
 * 两者互补，不互相替代。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperateLog {

    /** 业务模块名，如 "user"、"order"，用于审计检索分类 */
    String module();

    /** 操作描述，如 "创建用户"，面向审计阅读者的人类语言 */
    String action();

    /**
     * 是否记录方法入参摘要，默认 true。
     * 涉及口令、密钥等敏感入参的方法应显式关闭，由业务自行保证不入日志。
     */
    boolean recordArgs() default true;
}
