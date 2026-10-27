package io.github.ayongbox.baseon.starter.excel.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ImportResult 字段语义测试。
 */
class ImportResultTest {

    @Test
    void exposesSuccessAndFailListsAsConstructed() {
        // 给定：两条成功行与一条失败行
        List<String> successRows = List.of("a", "b");
        List<ImportFailRow> failRows = List.of(new ImportFailRow(3, "第2列[abc]", "转换失败"));

        // 当：构造结果对象
        ImportResult<String> result = new ImportResult<>(successRows, failRows);

        // 则：两个列表原样暴露，调用方按 isEmpty 判断是否存在失败
        assertThat(result.getSuccessRows()).isSameAs(successRows);
        assertThat(result.getFailRows()).isSameAs(failRows);
        assertThat(result.getFailRows()).isNotEmpty();
    }

    @Test
    void failRowCarriesRowIndexRawContentAndReason() {
        // 当：构造失败行
        ImportFailRow failRow = new ImportFailRow(5, "第3列[x]", "数字转换失败");

        // 则：三个字段语义完整
        assertThat(failRow.getRowIndex()).isEqualTo(5);
        assertThat(failRow.getRawContent()).isEqualTo("第3列[x]");
        assertThat(failRow.getReason()).isEqualTo("数字转换失败");
    }

    @Test
    void emptyFailListMeansFullSuccess() {
        // 当：失败列表为空
        ImportResult<String> result = new ImportResult<>(List.of("a"), List.of());

        // 则：调用方 isEmpty 判断为整批成功
        assertThat(result.getFailRows()).isEmpty();
        assertThat(result.getSuccessRows()).containsExactly("a");
    }
}
