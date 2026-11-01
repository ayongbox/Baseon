package io.github.ayongbox.baseon.core.boot;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionUtilTest {

    private static final AtomicInteger DB_SEQUENCE = new AtomicInteger();

    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:transaction-" + DB_SEQUENCE.incrementAndGet() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        transactionManager.setNestedTransactionAllowed(true);

        context = new AnnotationConfigApplicationContext();
        context.registerBean("dataSource", JdbcDataSource.class, () -> dataSource);
        context.registerBean("transactionManager", PlatformTransactionManager.class, () -> transactionManager);
        context.registerBean("springContextHolder", SpringContextHolder.class, SpringContextHolder::new);
        context.registerBean("jdbcTemplate", JdbcTemplate.class, () -> new JdbcTemplate(dataSource));
        context.refresh();

        jdbcTemplate = context.getBean(JdbcTemplate.class);
        jdbcTemplate.execute("create table t_order(id bigint primary key, name varchar(32))");
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void should_commit_and_invoke_after_commit() {
        AtomicBoolean afterCommit = new AtomicBoolean();
        TransactionUtil.execute(status -> {
            assertTrue(TransactionUtil.isActive());
            insert(1L);
            TransactionUtil.afterCommit(() -> afterCommit.set(true));
            return null;
        });

        assertTrue(afterCommit.get());
        assertEquals(1, count(1L));
        assertFalse(TransactionUtil.isActive());
    }

    @Test
    void should_rollback_on_runtime_exception_and_invoke_after_rollback() {
        AtomicBoolean afterRollback = new AtomicBoolean();

        assertThrows(IllegalStateException.class, () -> TransactionUtil.execute(status -> {
            insert(1L);
            TransactionUtil.afterRollback(() -> afterRollback.set(true));
            throw new IllegalStateException("boom");
        }));

        assertTrue(afterRollback.get());
        assertEquals(0, count(1L));
    }

    @Test
    void requires_new_should_commit_independently_from_outer_rollback() {
        assertThrows(IllegalStateException.class, () -> TransactionUtil.execute(status -> {
            insert(1L);
            TransactionUtil.executeInNewTransaction(inner -> {
                insert(2L);
                return null;
            });
            throw new IllegalStateException("outer");
        }));

        assertEquals(0, count(1L));
        assertEquals(1, count(2L));
    }

    @Test
    void nested_should_rollback_only_inner_changes() {
        TransactionUtil.execute(status -> {
            insert(1L);
            assertThrows(IllegalStateException.class, () -> TransactionUtil.executeInNestedTransaction(inner -> {
                insert(2L);
                throw new IllegalStateException("inner");
            }));
            return null;
        });

        assertEquals(1, count(1L));
        assertEquals(0, count(2L));
    }

    @Test
    void rollback_only_status_should_rollback_current_transaction() {
        TransactionUtil.executeWithoutResult(status -> {
            insert(1L);
            status.setRollbackOnly();
        });

        assertEquals(0, count(1L));
    }

    @Test
    void callbacks_should_run_immediately_without_transaction() {
        assertFalse(TransactionUtil.isActive());
        AtomicBoolean afterCommit = new AtomicBoolean();
        TransactionUtil.afterCommit(() -> afterCommit.set(true));
        assertTrue(afterCommit.get());

        AtomicBoolean afterRollback = new AtomicBoolean();
        TransactionUtil.afterRollback(() -> afterRollback.set(true));
        assertTrue(afterRollback.get());
    }

    private void insert(long id) {
        jdbcTemplate.update("insert into t_order(id, name) values (?, ?)", id, "order-" + id);
    }

    private int count(long id) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from t_order where id = ?", Integer.class, id);
        return count == null ? 0 : count;
    }
}
