package io.github.ayongbox.baseon.starter.datasource.support;

import io.github.ayongbox.baseon.starter.datasource.config.BaseonDatasourceProperties;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 动态路由数据源，按以下优先级决定当前线程使用哪个数据源：
 *
 * <ol>
 *     <li>{@link DataSourceContextHolder} 中有显式值（@DS 注解写入）——直接使用；</li>
 *     <li>读写分离开启、当前事务只读、且配置了从库——轮询选一个从库；</li>
 *     <li>兜底——主库。</li>
 * </ol>
 */
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    /**
     * 主库在路由表中的固定 key。
     */
    public static final String MASTER = "master";

    private final BaseonDatasourceProperties properties;
    private final List<String> slaveNames;
    private final AtomicInteger counter = new AtomicInteger();

    public DynamicRoutingDataSource(BaseonDatasourceProperties properties, List<String> slaveNames) {
        this.properties = properties;
        this.slaveNames = List.copyOf(slaveNames);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return resolveKey(DataSourceContextHolder.get(),
                TransactionSynchronizationManager.isCurrentTransactionReadOnly());
    }

    /**
     * 路由决策，包私有便于单测直接覆盖分支。
     *
     * @param explicit   上下文中的数据源名：@DS 显式指定，或
     *                   {@link DataSourceContextHolder#READ_ONLY} 只读标记，未指定为 null
     * @param readOnlyTx 当前事务是否只读（仅对非 Spring 事务管理的路径兜底）
     * @return 路由表中的数据源 key
     */
    String resolveKey(String explicit, boolean readOnlyTx) {
        if (explicit != null && !explicit.isBlank() && !DataSourceContextHolder.READ_ONLY.equals(explicit)) {
            return explicit;
        }
        boolean wantSlave = DataSourceContextHolder.READ_ONLY.equals(explicit) || readOnlyTx;
        if (wantSlave && properties.isReadWriteSplitEnabled() && !slaveNames.isEmpty()) {
            int index = Math.floorMod(counter.getAndIncrement(), slaveNames.size());
            return slaveNames.get(index);
        }
        return MASTER;
    }
}
