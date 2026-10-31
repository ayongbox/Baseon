package io.github.ayongbox.baseon.starter.datascope.it;

import io.github.ayongbox.baseon.core.annotation.DataScope;
import io.github.ayongbox.baseon.core.annotation.IgnoreDataScope;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 集成测试服务：覆盖 @DataScope 注入、@IgnoreDataScope 豁免、空可见集查不到数据三条路径。
 */
@Service
public class DataScopeDemoService {

    private final DataScopeDemoMapper mapper;

    public DataScopeDemoService(DataScopeDemoMapper mapper) {
        this.mapper = mapper;
    }

    /** 行级过滤：应仅返回当前用户可见部门的数据 */
    @DataScope
    public List<DataScopeDemoEntity> listScoped() {
        return mapper.selectList(null);
    }

    /** 自定义列名：按 project_id 过滤 */
    @DataScope(column = "dept_id", tableAlias = "")
    public List<DataScopeDemoEntity> listScopedWithExplicitColumn() {
        return mapper.selectList(null);
    }

    /** 忽略数据权限：应返回全表数据 */
    @IgnoreDataScope
    public List<DataScopeDemoEntity> listIgnoringScope() {
        return mapper.selectList(null);
    }

    /** 无注解：上下文为空，拦截器应放行 */
    public List<DataScopeDemoEntity> listWithoutAnnotation() {
        return mapper.selectList(null);
    }
}
