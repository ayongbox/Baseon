package io.github.ayongbox.baseon.starter.mybatisplus.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;

/**
 * 数据权限行级拦截器标记接口：starter-mybatis-plus 通过 {@code ObjectProvider<DataScopeInnerInterceptor>}
 * 发现并挂载数据权限插件，避免泛型 {@code ObjectProvider<InnerInterceptor>} 命中
 * 第三方插件（如租户插件自身、动态表名插件）造成重复挂载。
 *
 * <p>该接口由 starter-data-scope 实现；本模块仅作类型锚点，不感知具体实现，
 * 保证依赖方向单向（data-scope → mybatis-plus）。
 */
public interface DataScopeInnerInterceptor extends InnerInterceptor {
}
