package io.github.ayongbox.baseon.starter.mybatisplus.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.context.TenantIgnoreContext;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * 公共字段自动填充：审计时间、租户、逻辑删除标记。
 *
 * <p>字段约定见 {@code baseon-core} 的 {@code BaseEntity} / {@code TenantEntity}。
 * 内核实体刻意不携带 MP 注解，因此这里不能用基于 {@code TableInfo} 的
 * {@code strictInsertFill}（它只填充标了 {@code @TableField(fill=...)} 的字段，
 * 无注解时静默跳过），改用逐字段的 {@link #strictFillStrategy}：
 * 字段存在且当前值为 {@code null} 才填充，业务手工赋值永不覆盖。
 */
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictFillIfAbsent(metaObject, "createTime", () -> now);
        strictFillIfAbsent(metaObject, "updateTime", () -> now);
        // 租户填充以登录上下文为准；@IgnoreTenant 方法（SQL 忽略标记生效期间）不填，避免污染跨租户数据
        Long tenantId = SecurityUtil.getTenantId();
        if (tenantId != null && !TenantIgnoreContext.isSqlIgnored()) {
            strictFillIfAbsent(metaObject, "tenantId", () -> tenantId);
        }
        strictFillIfAbsent(metaObject, "deleted", () -> 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictFillIfAbsent(metaObject, "updateTime", LocalDateTime::now);
    }

    /**
     * 字段存在 setter 时才按“仅 null 填充”策略写入；
     * 无该字段的实体（如不继承 TenantEntity 的表无 tenantId）直接跳过。
     */
    private void strictFillIfAbsent(MetaObject metaObject, String fieldName, Supplier<Object> valueSupplier) {
        if (metaObject.hasSetter(fieldName)) {
            strictFillStrategy(metaObject, fieldName, valueSupplier);
        }
    }
}
