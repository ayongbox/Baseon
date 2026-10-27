package io.github.ayongbox.baseon.starter.excel.support;

import java.util.List;

/**
 * 导入结果：成功行与失败行分组返回，由调用方判断 {@code getFailRows().isEmpty()}
 * 决定全量落库还是部分放行。
 *
 * <p>刻意不提供 hasFail 之类的派生方法：POJO 中非属性方法禁止 is/get 前缀
 * （Jackson 会当作属性序列化），两个 List 属性本身已足够表达。
 *
 * @param <T> 行 DTO 类型
 */
public class ImportResult<T> {

    /**
     * 解析成功的行，按 Excel 出现顺序排列。
     */
    private final List<T> successRows;

    /**
     * 解析/转换失败的行明细，按 Excel 出现顺序排列。
     */
    private final List<ImportFailRow> failRows;

    public ImportResult(List<T> successRows, List<ImportFailRow> failRows) {
        this.successRows = successRows;
        this.failRows = failRows;
    }

    public List<T> getSuccessRows() {
        return successRows;
    }

    public List<ImportFailRow> getFailRows() {
        return failRows;
    }
}
