package io.github.ayongbox.baseon.starter.seata.support;

import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import org.apache.seata.tm.api.TransactionalExecutor;
import org.apache.seata.tm.api.TransactionalTemplate;
import org.apache.seata.tm.api.transaction.TransactionInfo;

import java.util.function.Supplier;

/**
 * 编程式全局事务：把一段业务代码包在 Seata 全局事务里执行。
 *
 * <p>注解式 {@code @GlobalTransactional} 依赖 AOP 代理，跨线程、非 Spring Bean、
 * 或需要在方法内部按分支决定是否开启全局事务时使用本执行器。事务失败即回滚，
 * 业务抛出的 {@link BizException} 原样透传，其余异常包装为 {@link CommonErrorCode#SYSTEM_ERROR}。
 */
public class SeataGlobalTransactionExecutor {

    private static final String DEFAULT_TRANSACTION_NAME = "baseon-global-tx";

    private final TransactionalTemplate transactionalTemplate;

    public SeataGlobalTransactionExecutor(TransactionalTemplate transactionalTemplate) {
        this.transactionalTemplate = transactionalTemplate;
    }

    /**
     * 在全局事务中执行并返回结果。
     *
     * @param transactionName 事务名，用于 TC 侧排查，空则用默认名
     * @param action          业务逻辑
     * @param <T>             返回类型
     * @return 业务返回值
     */
    public <T> T execute(String transactionName, Supplier<T> action) {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setName(isBlank(transactionName) ? DEFAULT_TRANSACTION_NAME : transactionName);
        TransactionalExecutor executor = new TransactionalExecutor() {
            @Override
            public Object execute() {
                return action.get();
            }

            @Override
            public TransactionInfo getTransactionInfo() {
                return transactionInfo;
            }
        };
        try {
            @SuppressWarnings("unchecked")
            T result = (T) transactionalTemplate.execute(executor);
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Throwable e) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, e);
        }
    }

    /**
     * 无返回值版本。
     */
    public void execute(String transactionName, Runnable action) {
        execute(transactionName, () -> {
            action.run();
            return null;
        });
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
