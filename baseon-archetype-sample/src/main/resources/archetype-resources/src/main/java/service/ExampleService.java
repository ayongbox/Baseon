package ${package}.service;

import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import io.github.ayongbox.baseon.core.model.PageQuery;
import io.github.ayongbox.baseon.core.model.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.LongStream;

/**
 * 示例业务服务：演示统一分页封装与业务异常抛出。
 * 使用内存数据模拟，接入真实持久层（MyBatis-Plus 等）后替换实现即可。
 */
@Service
public class ExampleService {

    /** 内存模拟数据，仅演示用 */
    private final List<String> items = LongStream.rangeClosed(1, 95)
            .mapToObj(i -> "example-" + i)
            .toList();

    /**
     * 分页查询：入参继承 {@link PageQuery}（禁止自定义 pageNum/pageSize，size 上限 500），
     * 出参统一 {@link PageResult}，字段命名与 MyBatis-Plus Page 一致。
     */
    public PageResult<String> page(PageQuery query) {
        List<String> records = items.stream()
                .skip(query.offset())
                .limit(query.getSize())
                .toList();
        return PageResult.of(items.size(), query.getCurrent(), query.getSize(), records);
    }

    /**
     * 详情查询：数据不存在时抛 {@link BizException}，
     * 由 starter-web 的全局异常处理器收敛为统一错误返回（业务异常只记 WARN，不打印堆栈）。
     */
    public String detail(long id) {
        if (id < 1 || id > items.size()) {
            throw new BizException(CommonErrorCode.NOT_FOUND, "示例数据不存在: " + id);
        }
        return items.get((int) id - 1);
    }
}
