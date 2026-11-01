package io.github.ayongbox.baseon.core.boot;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 编程式事务模板：用于无法声明 {@code @Transactional} 的场景（静态方法、Lambda、框架回调）。
 *
 * <p>事务管理器从 {@link SpringContextHolder} 中按类型获取，因此业务应用必须有且仅有一个
 * {@link PlatformTransactionManager}（Boot 自动装配默认满足，多数据源场景需标记 {@code @Primary}）。
 *
 * <p>传播行为语义：
 * <ul>
 *   <li>{@link #execute}：REQUIRED，加入当前事务，没有则新建；</li>
 *   <li>{@link #executeInNewTransaction}：REQUIRES_NEW，挂起当前事务并独立提交/回滚；</li>
 *   <li>{@link #executeInNestedTransaction}：NESTED，保存点方式，内层回滚不影响外层。</li>
 * </ul>
 *
 * <p>未捕获的运行时异常与 {@link Error} 自动回滚；受检异常默认**不回滚**（与
 * {@code @Transactional} 默认行为一致），需要回滚时在回调内调用
 * {@link TransactionStatus#setRollbackOnly()}（回调参数即当前事务状态）。
 */
public final class TransactionUtil {

    private TransactionUtil() {
    }

    public static <T> T execute(Function<TransactionStatus, T> action) {
        return execute(action, TransactionDefinition.PROPAGATION_REQUIRED);
    }

    public static void executeWithoutResult(Consumer<TransactionStatus> action) {
        Objects.requireNonNull(action, "action");
        execute(status -> {
            action.accept(status);
            return null;
        });
    }

    public static <T> T executeInNewTransaction(Function<TransactionStatus, T> action) {
        return execute(action, TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public static void executeInNewTransactionWithoutResult(Consumer<TransactionStatus> action) {
        Objects.requireNonNull(action, "action");
        executeInNewTransaction(status -> {
            action.accept(status);
            return null;
        });
    }

    public static <T> T executeInNestedTransaction(Function<TransactionStatus, T> action) {
        return execute(action, TransactionDefinition.PROPAGATION_NESTED);
    }

    public static void executeInNestedTransactionWithoutResult(Consumer<TransactionStatus> action) {
        Objects.requireNonNull(action, "action");
        executeInNestedTransaction(status -> {
            action.accept(status);
            return null;
        });
    }

    /**
     * 当前线程是否存在真实事务（不含只读空事务）。
     */
    public static boolean isActive() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /**
     * 注册提交后回调。无活动事务时立即执行，避免调用方为「可能没有事务」额外写分支。
     */
    public static void afterCommit(Runnable action) {
        Objects.requireNonNull(action, "action");
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    /**
     * 注册回滚后回调。无活动事务时立即执行。
     */
    public static void afterRollback(Runnable action) {
        Objects.requireNonNull(action, "action");
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    action.run();
                }
            }
        });
    }

    private static <T> T execute(Function<TransactionStatus, T> action, int propagationBehavior) {
        Objects.requireNonNull(action, "action");
        TransactionTemplate template = new TransactionTemplate(
                SpringContextHolder.getBean(PlatformTransactionManager.class));
        template.setPropagationBehavior(propagationBehavior);
        return template.execute(action::apply);
    }
}
