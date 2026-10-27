package io.github.ayongbox.baseon.starter.excel.support;

/**
 * 导入失败行：记录一行解析/转换失败的明细，供调用方汇总反馈给前端或落库重试。
 */
public class ImportFailRow {

    /**
     * Excel 物理行号：含表头从 1 起算，首行数据为 2。
     * 取不到行号的异常为 -1。
     */
    private final int rowIndex;

    /**
     * 该行原始内容摘要。FastExcel 回调拿不到整行原始数据，
     * 单元格转换异常时只含失败单元格的列号与原始值。
     */
    private final String rawContent;

    /**
     * 失败原因（底层转换异常消息，含目标字段与期望值信息）。
     */
    private final String reason;

    public ImportFailRow(int rowIndex, String rawContent, String reason) {
        this.rowIndex = rowIndex;
        this.rawContent = rawContent;
        this.reason = reason;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public String getRawContent() {
        return rawContent;
    }

    public String getReason() {
        return reason;
    }
}
