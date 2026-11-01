package io.github.ayongbox.baseon.starter.seata.support;

import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import org.apache.seata.tm.api.TransactionalExecutor;
import org.apache.seata.tm.api.TransactionalTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 编程式全局事务执行器：业务返回值透传、事务名注入、异常语义（业务异常原样抛出）。
 */
class SeataGlobalTransactionExecutorTest {

    private final TransactionalTemplate template = mock(TransactionalTemplate.class);

    private final SeataGlobalTransactionExecutor executor = new SeataGlobalTransactionExecutor(template);

    @Test
    void returnsBusinessResult() throws Throwable {
        when(template.execute(any())).thenAnswer(
                invocation -> ((TransactionalExecutor) invocation.getArgument(0)).execute());

        String result = executor.execute("deduct-stock", () -> "ok");

        assertThat(result).isEqualTo("ok");
        ArgumentCaptor<TransactionalExecutor> captor = ArgumentCaptor.forClass(TransactionalExecutor.class);
        verify(template).execute(captor.capture());
        assertThat(captor.getValue().getTransactionInfo().getName()).isEqualTo("deduct-stock");
    }

    @Test
    void usesDefaultTransactionNameWhenBlank() throws Throwable {
        when(template.execute(any())).thenAnswer(
                invocation -> ((TransactionalExecutor) invocation.getArgument(0)).execute());

        executor.execute(" ", () -> null);

        ArgumentCaptor<TransactionalExecutor> captor = ArgumentCaptor.forClass(TransactionalExecutor.class);
        verify(template).execute(captor.capture());
        assertThat(captor.getValue().getTransactionInfo().getName()).isEqualTo("baseon-global-tx");
    }

    @Test
    void propagatesBizExceptionAsIs() throws Throwable {
        when(template.execute(any())).thenAnswer(invocation -> {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR);
        });

        assertThatThrownBy(() -> executor.execute("tx", () -> "should-not-return"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void wrapsOtherThrowablesAsSystemError() throws Throwable {
        when(template.execute(any())).thenThrow(new IllegalStateException("tc unreachable"));

        assertThatThrownBy(() -> executor.execute("tx", () -> "should-not-return"))
                .isInstanceOf(BizException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .extracting("code")
                .isEqualTo(CommonErrorCode.SYSTEM_ERROR.getCode());
    }

    @Test
    void runnableOverloadReturnsVoid() throws Throwable {
        when(template.execute(any())).thenAnswer(
                invocation -> ((TransactionalExecutor) invocation.getArgument(0)).execute());

        executor.execute("tx", () -> {
        });

        verify(template).execute(any());
    }
}
