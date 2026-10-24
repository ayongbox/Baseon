package io.github.ayongbox.baseon.starter.mybatisplus.config;

import io.github.ayongbox.baseon.core.context.SecurityUtil;
import io.github.ayongbox.baseon.core.context.TenantIgnoreContext;
import io.github.ayongbox.baseon.core.model.BaseEntity;
import io.github.ayongbox.baseon.core.model.LoginUser;
import io.github.ayongbox.baseon.core.model.TenantEntity;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MyMetaObjectHandlerTest {

    private final MyMetaObjectHandler handler = new MyMetaObjectHandler();

    @AfterEach
    void tearDown() {
        SecurityUtil.clear();
        TenantIgnoreContext.clearSqlIgnored();
    }

    @Test
    void insertFillFillsAuditTenantAndDeleted() {
        SecurityUtil.set(loginUser(1001L));
        TenantEntityDemo entity = new TenantEntityDemo();

        handler.insertFill(SystemMetaObject.forObject(entity));

        assertThat(entity.getCreateTime()).isNotNull();
        assertThat(entity.getUpdateTime()).isNotNull();
        assertThat(entity.getTenantId()).isEqualTo(1001L);
        assertThat(entity.getDeleted()).isEqualTo(0);
    }

    @Test
    void insertFillNeverOverridesManualAssignedValues() {
        SecurityUtil.set(loginUser(1001L));
        TenantEntityDemo entity = new TenantEntityDemo();
        LocalDateTime manualCreateTime = LocalDateTime.of(2024, 1, 1, 0, 0);
        entity.setCreateTime(manualCreateTime);
        entity.setTenantId(2002L);
        entity.setDeleted(1);

        handler.insertFill(SystemMetaObject.forObject(entity));

        assertThat(entity.getCreateTime()).isEqualTo(manualCreateTime);
        assertThat(entity.getTenantId()).isEqualTo(2002L);
        assertThat(entity.getDeleted()).isEqualTo(1);
        assertThat(entity.getUpdateTime()).isNotNull();
    }

    @Test
    void insertFillSkipsTenantWhenSqlIgnored() {
        SecurityUtil.set(loginUser(1001L));
        TenantIgnoreContext.markSqlIgnored();
        TenantEntityDemo entity = new TenantEntityDemo();

        handler.insertFill(SystemMetaObject.forObject(entity));

        assertThat(entity.getTenantId()).isNull();
        assertThat(entity.getDeleted()).isEqualTo(0);
    }

    @Test
    void insertFillSkipsTenantWhenNotLogin() {
        TenantEntityDemo entity = new TenantEntityDemo();

        handler.insertFill(SystemMetaObject.forObject(entity));

        assertThat(entity.getTenantId()).isNull();
    }

    @Test
    void insertFillWorksForEntityWithoutTenantField() {
        PlainEntityDemo entity = new PlainEntityDemo();

        handler.insertFill(SystemMetaObject.forObject(entity));

        assertThat(entity.getCreateTime()).isNotNull();
        assertThat(entity.getUpdateTime()).isNotNull();
        assertThat(entity.getDeleted()).isEqualTo(0);
    }

    @Test
    void updateFillOnlyFillsUpdateTime() {
        SecurityUtil.set(loginUser(1001L));
        TenantEntityDemo entity = new TenantEntityDemo();

        handler.updateFill(SystemMetaObject.forObject(entity));

        assertThat(entity.getUpdateTime()).isNotNull();
        assertThat(entity.getCreateTime()).isNull();
        assertThat(entity.getTenantId()).isNull();
        assertThat(entity.getDeleted()).isNull();
    }

    private LoginUser loginUser(Long tenantId) {
        LoginUser user = new LoginUser();
        user.setUserId(9L);
        user.setUsername("tester");
        user.setTenantId(tenantId);
        return user;
    }

    /**
     * 租户表实体：走 createTime / updateTime / tenantId / deleted 全量填充。
     */
    private static class TenantEntityDemo extends TenantEntity {
    }

    /**
     * 非租户表实体：无 tenantId 字段，填充不得报错。
     */
    private static class PlainEntityDemo extends BaseEntity {
    }
}
