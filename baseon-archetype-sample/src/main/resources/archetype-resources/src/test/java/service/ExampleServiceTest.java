package ${package}.service;

import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.PageQuery;
import io.github.ayongbox.baseon.core.model.PageResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ExampleService 单元测试：不启动 Spring 容器，保证 mvn test 秒级完成。
 */
class ExampleServiceTest {

    private final ExampleService service = new ExampleService();

    @Test
    void pageReturnsRequestedSlice() {
        PageQuery query = new PageQuery();
        query.setCurrent(2);
        query.setSize(10);

        PageResult<String> result = service.page(query);

        assertEquals(95, result.getTotal());
        assertEquals(10, result.getPages());
        assertEquals(10, result.getRecords().size());
        assertEquals("example-11", result.getRecords().get(0));
    }

    @Test
    void pageBeyondDataReturnsEmptyRecords() {
        PageQuery query = new PageQuery();
        query.setCurrent(99);

        PageResult<String> result = service.page(query);

        assertEquals(95, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void detailReturnsItem() {
        assertEquals("example-1", service.detail(1));
    }

    @Test
    void detailNotFoundThrowsBizException() {
        BizException exception = assertThrows(BizException.class, () -> service.detail(999));
        assertEquals(CommonErrorCode.NOT_FOUND.getCode(), exception.getCode());
    }
}
