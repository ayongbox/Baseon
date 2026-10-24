package io.github.ayongbox.baseon.demo.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.ayongbox.baseon.core.annotation.IgnoreTenant;
import io.github.ayongbox.baseon.demo.entity.TenantUser;
import io.github.ayongbox.baseon.demo.mapper.TenantUserMapper;
import io.github.ayongbox.baseon.starter.cache.support.CacheHelper;
import io.github.ayongbox.baseon.starter.datasource.annotation.DS;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 租户演示服务：覆盖 4 期出口标准所需的关键路径。
 */
@Service
public class TenantUserService {

    private static final String CACHE_KEY_PREFIX = "demo:tenant-user:";

    private final TenantUserMapper mapper;
    private final CacheHelper cacheHelper;

    public TenantUserService(TenantUserMapper mapper, CacheHelper cacheHelper) {
        this.mapper = mapper;
        this.cacheHelper = cacheHelper;
    }

    /** 写入：tenantId / createTime / deleted 由 MetaObjectHandler 自动填充 */
    public TenantUser create(String name) {
        TenantUser user = new TenantUser();
        user.setName(name);
        mapper.insert(user);
        return user;
    }

    /** 查询：MP 租户插件自动追加 tenant_id 条件 */
    public List<TenantUser> listCurrentTenant() {
        return mapper.selectList(null);
    }

    /** 缓存查询：同逻辑 key 在不同租户下隔离（key 前缀由 starter-tenant 提供） */
    public TenantUser cachedGet(Long id) {
        return cacheHelper.getOrLoad(CACHE_KEY_PREFIX + id, TenantUser.class, 300,
                () -> mapper.selectById(id));
    }

    /** 跨租户统计：@IgnoreTenant 关闭租户过滤 */
    @IgnoreTenant
    public long countAcrossTenants() {
        return mapper.selectCount(null);
    }

    /** 缓存清理 */
    public void evictCache(Long id) {
        cacheHelper.evict(CACHE_KEY_PREFIX + id);
    }

    /** 显式路由从库（@DS 注解切换） */
    @DS("slave1")
    public List<TenantUser> listFromSlave() {
        return mapper.selectList(null);
    }

    /** 只读事务自动路由从库（读写分离） */
    @Transactional(readOnly = true)
    public List<TenantUser> listReadOnlyTx() {
        return mapper.selectList(null);
    }

    /** 分页：验证租户插件排在分页插件之前（count SQL 也带租户条件） */
    public Page<TenantUser> page(long current, long size) {
        return mapper.selectPage(new Page<>(current, size), null);
    }
}
