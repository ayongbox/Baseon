package io.github.ayongbox.baseon.demo.controller;

import io.github.ayongbox.baseon.core.annotation.RequiresPermission;
import io.github.ayongbox.baseon.core.model.R;
import io.github.ayongbox.baseon.demo.dto.TenantUserExcelRow;
import io.github.ayongbox.baseon.demo.entity.TenantUser;
import io.github.ayongbox.baseon.demo.service.TenantUserService;
import io.github.ayongbox.baseon.starter.excel.support.ExcelTemplate;
import io.github.ayongbox.baseon.starter.excel.support.ImportResult;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 6 期验证接口：Excel 导出/导入。
 * 导出演示「实体 → 行模型」转换；导入演示逐行容错（脏行不中断整批），
 * 成功行直接落库（租户字段仍由 MP 自动填充）。
 */
@RestController
@RequestMapping("/demo/excel")
public class ExcelDemoController {

    private final ExcelTemplate excelTemplate;
    private final TenantUserService service;

    public ExcelDemoController(ExcelTemplate excelTemplate, TenantUserService service) {
        this.excelTemplate = excelTemplate;
        this.service = service;
    }

    /** 导出当前租户用户列表为 xlsx；需 user:list 权限 */
    @RequiresPermission("user:list")
    @GetMapping("/export")
    public void export(HttpServletResponse response) throws IOException {
        List<TenantUserExcelRow> rows = service.listCurrentTenant().stream()
                .map(this::toRow)
                .toList();
        excelTemplate.export(response, "租户用户列表", TenantUserExcelRow.class, rows);
    }

    /**
     * 导入 xlsx：姓名列逐行解析，成功行落库，失败行带行号与原因返回。
     * 导入行为本身也走权限与审计（复用 create 链路的权限要求）。
     */
    @RequiresPermission("user:create")
    @PostMapping("/import")
    public R<ImportResult<TenantUserExcelRow>> importUsers(@RequestParam("file") MultipartFile file) throws IOException {
        ImportResult<TenantUserExcelRow> result = excelTemplate.importExcel(file, TenantUserExcelRow.class);
        // 成功行落库（空白姓名行跳过：演示场景不引入业务校验，真实业务应在此追加规则）
        result.getSuccessRows().forEach(row -> {
            if (row.getName() != null && !row.getName().isBlank()) {
                service.create(row.getName());
            }
        });
        return R.ok(result);
    }

    private TenantUserExcelRow toRow(TenantUser user) {
        TenantUserExcelRow row = new TenantUserExcelRow();
        row.setId(user.getId());
        row.setName(user.getName());
        row.setCreateTime(user.getCreateTime());
        return row;
    }
}
