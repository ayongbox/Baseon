package io.github.ayongbox.baseon.starter.excel.support;

import cn.idev.excel.FastExcel;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.exception.ExcelAnalysisException;
import cn.idev.excel.exception.ExcelDataConvertException;
import cn.idev.excel.metadata.data.CellData;
import cn.idev.excel.read.listener.ReadListener;
import io.github.ayongbox.baseon.core.model.BizException;
import io.github.ayongbox.baseon.core.model.CommonErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Excel 导入导出模板：对 FastExcel 的两层薄封装。
 *
 * <p>导出：写死 xlsx 响应头（文件名 UTF-8 URL 编码，避免中文乱码），流式写出；
 * 导入：逐行解析，单行转换异常不中断整批，成功行与失败行分组返回。
 *
 * <p>读写都只处理第一个 sheet、单行表头（FastExcel 默认 headRowNumber=1），
 * 复杂模板请直接使用 FastExcel 原生 API。
 */
public class ExcelTemplate {

    /**
     * xlsx 的 MIME 类型。
     */
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * xlsx 扩展名，导入强制校验、导出自动补齐。
     */
    private static final String XLSX_SUFFIX = ".xlsx";

    /**
     * 导出 xlsx 到 HTTP 响应。
     *
     * @param response HTTP 响应，本方法设置 Content-Type 与 Content-Disposition
     * @param fileName 文件名（不含扩展名），中文做 UTF-8 URL 编码，自动补 {@code .xlsx}
     * @param head     行 DTO 类型，字段用 {@code @ExcelProperty} 标注列名
     * @param rows     数据行，null 视为空表（只导出表头）
     * @param <T>      行 DTO 类型
     */
    public <T> void export(HttpServletResponse response, String fileName, Class<T> head, List<T> rows) {
        response.setContentType(XLSX_CONTENT_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // URLEncoder 把空格编成 +，按 Content-Disposition 语义还原为 %20
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + encodedFileName + XLSX_SUFFIX);
        try {
            FastExcel.write(response.getOutputStream(), head).sheet().doWrite(rows);
        } catch (IOException e) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "Excel 导出失败：" + e.getMessage());
        }
    }

    /**
     * 导入 xlsx：逐行解析，单行失败不中断整批。
     *
     * @param file 上传文件，必须非空且扩展名为 {@code .xlsx}（不区分大小写）
     * @param head 行 DTO 类型，字段用 {@code @ExcelProperty} 标注列名
     * @param <T>  行 DTO 类型
     * @return 成功行与失败行分组结果
     * @throws BizException 文件为空、非 xlsx 扩展名或内容无法解析时抛出，错误码 {@code PARAM_INVALID}
     */
    public <T> ImportResult<T> importExcel(MultipartFile file, Class<T> head) {
        validateFile(file);
        CollectingReadListener<T> listener = new CollectingReadListener<>();
        try (InputStream inputStream = file.getInputStream()) {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            assertZipFormat(bufferedInputStream);
            FastExcel.read(bufferedInputStream, head, listener).sheet().doRead();
        } catch (IOException e) {
            throw new BizException(CommonErrorCode.PARAM_INVALID, "文件读取失败：" + e.getMessage());
        } catch (ExcelAnalysisException e) {
            // zip 外壳合法但内部结构损坏，属于解析级故障而非行级故障
            throw new BizException(CommonErrorCode.PARAM_INVALID, "文件解析失败，请确认是有效的 xlsx 文件");
        }
        return new ImportResult<>(listener.successRows, listener.failRows);
    }

    /**
     * 上传文件前置校验：空文件与非 xlsx 扩展名直接拒绝。
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(CommonErrorCode.PARAM_INVALID, "导入文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null
                || !originalFilename.toLowerCase(Locale.ROOT).endsWith(XLSX_SUFFIX)) {
            throw new BizException(CommonErrorCode.PARAM_INVALID, "仅支持 .xlsx 格式的文件");
        }
    }

    /**
     * 嗅探 ZIP 魔数：xlsx 本质是 zip 包（前两字节 PK）。
     * FastExcel 对非 zip 内容会静默降级为 CSV 解析（不报错、读不到行），
     * 必须自行拦截，否则「文本文件改名 xlsx」会返回空结果而非报错。
     */
    private void assertZipFormat(BufferedInputStream inputStream) throws IOException {
        inputStream.mark(2);
        int first = inputStream.read();
        int second = inputStream.read();
        inputStream.reset();
        if (first != 'P' || second != 'K') {
            throw new BizException(CommonErrorCode.PARAM_INVALID, "文件内容不是有效的 xlsx 格式");
        }
    }

    /**
     * 逐行收集监听器：{@link #invoke} 收集成功行，{@link #onException} 记录失败行后吞掉异常，
     * 让 FastExcel 继续解析下一行（默认实现会重抛导致整批中断）。
     */
    private static class CollectingReadListener<T> implements ReadListener<T> {

        private final List<T> successRows = new ArrayList<>();
        private final List<ImportFailRow> failRows = new ArrayList<>();

        @Override
        public void invoke(T data, AnalysisContext context) {
            successRows.add(data);
        }

        @Override
        public void onException(Exception exception, AnalysisContext context) {
            if (exception instanceof ExcelDataConvertException convertException) {
                failRows.add(new ImportFailRow(
                        toPhysicalRow(convertException.getRowIndex()),
                        summarizeCell(convertException),
                        exception.getMessage()));
            } else {
                // 非转换类异常（罕见）：行号从上下文兜底
                Integer rowIndex = context == null || context.readRowHolder() == null
                        ? null : context.readRowHolder().getRowIndex();
                failRows.add(new ImportFailRow(toPhysicalRow(rowIndex), "", exception.getMessage()));
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 结果已通过 getter 暴露，无需收尾
        }

        /**
         * FastExcel 行号从 0 起算，换算为含表头从 1 起算的物理行号；取不到则为 -1。
         */
        private static int toPhysicalRow(Integer zeroBasedRowIndex) {
            return zeroBasedRowIndex == null ? -1 : zeroBasedRowIndex + 1;
        }

        /**
         * 转换异常只能拿到失败单元格，摘要为「第 N 列[原始值]」。
         */
        private static String summarizeCell(ExcelDataConvertException exception) {
            CellData<?> cellData = exception.getCellData();
            if (cellData == null) {
                return "";
            }
            Object data = cellData.getData();
            String value = data != null ? String.valueOf(data) : cellData.getStringValue();
            Integer columnIndex = exception.getColumnIndex();
            return "第" + (columnIndex == null ? -1 : columnIndex + 1) + "列["
                    + (value == null ? "" : value) + "]";
        }
    }
}
