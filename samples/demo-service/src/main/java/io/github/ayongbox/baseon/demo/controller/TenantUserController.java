package io.github.ayongbox.baseon.demo.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.ayongbox.baseon.core.annotation.Idempotent;
import io.github.ayongbox.baseon.core.annotation.RequiresPermission;
import io.github.ayongbox.baseon.core.model.R;
import io.github.ayongbox.baseon.demo.entity.TenantUser;
import io.github.ayongbox.baseon.demo.service.TenantUserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 4 期验证接口：多租户读写、缓存隔离、数据源路由。
 * 租户身份由请求头 X-Tenant-Id 经 starter-web 拦截器写入登录上下文。
 */
@RestController
@RequestMapping("/demo/tenant/users")
public class TenantUserController {

    private final TenantUserService service;

    public TenantUserController(TenantUserService service) {
        this.service = service;
    }

    /** 创建：同名占位窗口内重复提交被幂等拦截；需 user:create 权限 */
    @Idempotent(key = "#name")
    @RequiresPermission("user:create")
    @PostMapping
    public R<TenantUser> create(@RequestParam String name) {
        return R.ok(service.create(name));
    }

    /** 当前租户数据列表（MP 自动追加 tenant_id 条件）；需 user:list 权限 */
    @RequiresPermission("user:list")
    @GetMapping
    public R<List<TenantUser>> list() {
        return R.ok(service.listCurrentTenant());
    }

    /** 两级缓存查询，验证同 id 在不同租户下缓存隔离 */
    @GetMapping("/{id}/cached")
    public R<TenantUser> cachedGet(@PathVariable Long id) {
        return R.ok(service.cachedGet(id));
    }

    @DeleteMapping("/{id}/cache")
    public R<Void> evictCache(@PathVariable Long id) {
        service.evictCache(id);
        return R.ok(null);
    }

    /** 跨租户总数（@IgnoreTenant） */
    @GetMapping("/cross-count")
    public R<Long> countAcrossTenants() {
        return R.ok(service.countAcrossTenants());
    }

    /** 显式 @DS 路由从库 */
    @GetMapping("/slave")
    public R<List<TenantUser>> listFromSlave() {
        return R.ok(service.listFromSlave());
    }

    /** 只读事务自动路由从库 */
    @GetMapping("/ro")
    public R<List<TenantUser>> listReadOnlyTx() {
        return R.ok(service.listReadOnlyTx());
    }

    @GetMapping("/page")
    public R<Page<TenantUser>> page(@RequestParam(defaultValue = "1") long current,
                                    @RequestParam(defaultValue = "10") long size) {
        return R.ok(service.page(current, size));
    }
}
