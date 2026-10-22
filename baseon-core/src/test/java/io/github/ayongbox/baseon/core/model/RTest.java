package io.github.ayongbox.baseon.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RTest {

    @Test
    void ok_without_data() {
        R<Void> result = R.ok();
        assertEquals(R.SUCCESS_CODE, result.getCode());
        assertNull(result.getData());
        assertTrue(result.success());
    }

    @Test
    void ok_with_data() {
        R<String> result = R.ok("payload");
        assertEquals(R.SUCCESS_CODE, result.getCode());
        assertEquals("payload", result.getData());
        assertTrue(result.success());
    }

    @Test
    void fail_with_error_code() {
        R<Void> result = R.fail(CommonErrorCode.PARAM_INVALID);
        assertEquals(1001, result.getCode());
        assertEquals("参数校验失败", result.getMsg());
        assertNull(result.getData());
        assertFalse(result.success());
    }

    @Test
    void fail_with_biz_exception() {
        R<Void> result = R.fail(new BizException(CommonErrorCode.NOT_FOUND, "用户不存在"));
        assertEquals(1004, result.getCode());
        assertEquals("用户不存在", result.getMsg());
        assertFalse(result.success());
    }

    @Test
    void fail_with_raw_code() {
        R<Void> result = R.fail(20101, "库存不足");
        assertEquals(20101, result.getCode());
        assertEquals("库存不足", result.getMsg());
        assertFalse(result.success());
    }

    @Test
    void biz_exception_should_not_collect_stack_trace() {
        BizException exception = new BizException(CommonErrorCode.PARAM_INVALID);
        assertEquals(0, exception.getStackTrace().length);
    }
}
