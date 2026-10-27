package io.github.ayongbox.baseon.starter.excel.support;

import cn.idev.excel.FastExcel;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ExcelTemplate 导出/导入行为测试。
 */
class ExcelTemplateTest {

    private final ExcelTemplate excelTemplate = new ExcelTemplate();

    @Test
    void exportWritesReadableXlsxWithEncodedFileName() throws Exception {
        // 给定：两行合法数据与中文文件名
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<UserImportRow> rows = List.of(new UserImportRow("张三", 20), new UserImportRow("李四", 30));

        // 当：执行导出
        excelTemplate.export(response, "用户列表", UserImportRow.class, rows);

        // 则：响应头为 xlsx 附件（字符集说明可追加在 MIME 后），中文文件名按 UTF-8 URL 编码
        assertThat(response.getContentType())
                .startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getHeader("Content-Disposition"))
                .isEqualTo("attachment;filename*=UTF-8''%E7%94%A8%E6%88%B7%E5%88%97%E8%A1%A8.xlsx");

        // 则：字节流非空，且能被 FastExcel 读回表头与数据
        byte[] bytes = response.getContentAsByteArray();
        assertThat(bytes).isNotEmpty();
        HeadCapturingListener listener = new HeadCapturingListener();
        FastExcel.read(new ByteArrayInputStream(bytes), UserImportRow.class, listener).sheet().doRead();
        assertThat(listener.headMap).containsEntry(0, "姓名").containsEntry(1, "年龄");
        assertThat(listener.rows).containsExactly(new UserImportRow("张三", 20), new UserImportRow("李四", 30));
    }

    @Test
    void importExcelParsesAllValidRows() {
        // 给定：三行全部合法的 xlsx
        byte[] bytes = writeRows(List.of(
                new UserRawRow("张三", "20"),
                new UserRawRow("李四", "30"),
                new UserRawRow("王五", "40")));
        MockMultipartFile file = new MockMultipartFile("file", "user.xlsx", null, bytes);

        // 当：执行导入
        ImportResult<UserImportRow> result = excelTemplate.importExcel(file, UserImportRow.class);

        // 则：全部进入成功行，失败行为空
        assertThat(result.getSuccessRows()).containsExactly(
                new UserImportRow("张三", 20),
                new UserImportRow("李四", 30),
                new UserImportRow("王五", 40));
        assertThat(result.getFailRows()).isEmpty();
    }

    @Test
    void importExcelCollectsFailRowWithoutBreakingBatch() {
        // 给定：中间行年龄列填了文本 "abc"，无法转成 Integer
        byte[] bytes = writeRows(List.of(
                new UserRawRow("张三", "20"),
                new UserRawRow("李四", "abc"),
                new UserRawRow("王五", "40")));
        MockMultipartFile file = new MockMultipartFile("file", "user.xlsx", null, bytes);

        // 当：执行导入
        ImportResult<UserImportRow> result = excelTemplate.importExcel(file, UserImportRow.class);

        // 则：合法行正常解析，非法行带物理行号（表头为第 1 行）、原始值与原因
        assertThat(result.getSuccessRows()).containsExactly(
                new UserImportRow("张三", 20),
                new UserImportRow("王五", 40));
        assertThat(result.getFailRows()).hasSize(1);
        ImportFailRow failRow = result.getFailRows().get(0);
        assertThat(failRow.getRowIndex()).isEqualTo(3);
        assertThat(failRow.getRawContent()).contains("abc");
        assertThat(failRow.getReason()).isNotBlank();
    }

    @Test
    void importExcelRejectsEmptyFile() {
        // 给定：空内容的上传文件
        MockMultipartFile file = new MockMultipartFile("file", "user.xlsx", null, new byte[0]);

        // 当/则：抛出参数类业务异常
        assertThatThrownBy(() -> excelTemplate.importExcel(file, UserImportRow.class))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(CommonErrorCode.PARAM_INVALID.getCode()));
    }

    @Test
    void importExcelRejectsNonXlsxExtension() {
        // 给定：扩展名为 .txt 的上传文件
        MockMultipartFile file = new MockMultipartFile("file", "user.txt", null, "not excel".getBytes());

        // 当/则：抛出参数类业务异常
        assertThatThrownBy(() -> excelTemplate.importExcel(file, UserImportRow.class))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(CommonErrorCode.PARAM_INVALID.getCode()));
    }

    @Test
    void importExcelRejectsCorruptedXlsxContent() {
        // 给定：扩展名合法但内容是纯文本的假 xlsx
        MockMultipartFile file = new MockMultipartFile("file", "user.xlsx", null, "not excel".getBytes());

        // 当/则：解析级故障包装为参数类业务异常
        assertThatThrownBy(() -> excelTemplate.importExcel(file, UserImportRow.class))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(CommonErrorCode.PARAM_INVALID.getCode()));
    }

    @Test
    void importExcelRejectsUnreadableFile() throws IOException {
        // 给定：流读取即失败的文件
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("user.xlsx");
        when(file.getInputStream()).thenThrow(new IOException("broken stream"));

        // 当/则：抛出参数类业务异常
        assertThatThrownBy(() -> excelTemplate.importExcel(file, UserImportRow.class))
                .isInstanceOfSatisfying(BizException.class, e ->
                        assertThat(e.getCode()).isEqualTo(CommonErrorCode.PARAM_INVALID.getCode()));
    }

    /**
     * 以「年龄列为字符串」的原始行类型写文件，从而能构造出数字列填文本的非法行。
     */
    private byte[] writeRows(List<UserRawRow> rows) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FastExcel.write(outputStream, UserRawRow.class).sheet().doWrite(rows);
        return outputStream.toByteArray();
    }

    /**
     * 导入目标行：年龄为 Integer，文本值转换失败。
     * 注意：FastExcel 写侧按 JavaBean 属性取值，DTO 必须提供 getter/setter。
     */
    public static class UserImportRow {

        @ExcelProperty("姓名")
        private String name;

        @ExcelProperty("年龄")
        private Integer age;

        public UserImportRow() {
        }

        public UserImportRow(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof UserImportRow that)) {
                return false;
            }
            return java.util.Objects.equals(name, that.name) && java.util.Objects.equals(age, that.age);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(name, age);
        }
    }

    /**
     * 写文件用的原始行：年龄为 String，可写入 "abc" 这类非法值。
     */
    public static class UserRawRow {

        @ExcelProperty("姓名")
        private String name;

        @ExcelProperty("年龄")
        private String age;

        public UserRawRow() {
        }

        public UserRawRow(String name, String age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAge() {
            return age;
        }

        public void setAge(String age) {
            this.age = age;
        }
    }

    /**
     * 读回校验用监听器：同时捕获表头与数据行。
     */
    static class HeadCapturingListener extends AnalysisEventListener<UserImportRow> {

        private final Map<Integer, String> headMap = new LinkedHashMap<>();
        private final List<UserImportRow> rows = new ArrayList<>();

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            this.headMap.putAll(headMap);
        }

        @Override
        public void invoke(UserImportRow data, AnalysisContext context) {
            rows.add(data);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 测试内联收集，无需收尾
        }
    }
}
