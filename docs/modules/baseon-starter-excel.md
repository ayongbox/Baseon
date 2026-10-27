# baseon-starter-excel · Excel 导入导出

## 定位

基于 FastExcel（EasyExcel 停更后的社区续作，注解 `@ExcelProperty` 与 API 完全兼容）的 xlsx 读写薄封装。导出负责写死响应头与中文文件名编码，导入负责逐行容错——单行转换失败不中断整批，成功行与失败行分组返回。业务只需要一个 `@ExcelProperty` 标注的 DTO。

引入方式（版本由 BOM 管控，传递引入 baseon-core 与 spring-boot-starter-web）：

```xml
<dependency>
    <groupId>io.github.ayongbox</groupId>
    <artifactId>baseon-starter-excel</artifactId>
</dependency>
```

## 自动装配清单

| 组件 | 作用 | 默认行为 |
|---|---|---|
| `ExcelTemplate` | 导出/导入入口：`export` 写 xlsx 到 `HttpServletResponse`，`importExcel` 逐行容错解析 `MultipartFile` | 默认装配，`@ConditionalOnMissingBean` 可被业务整体替换 |
| `ImportResult` / `ImportFailRow` | 导入结果载体：成功行列表 + 失败行明细（物理行号/原始内容摘要/原因） | 纯 POJO，无 Bean |

注册方式：`AutoConfiguration.imports`。

## 配置项

```yaml
baseon:
  excel:
    enabled: true   # 总开关：false 时不注册 ExcelTemplate
```

| 配置项 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| `baseon.excel.enabled` | boolean | `true` | 总开关，关闭后 `ExcelTemplate` Bean 不装配 |

## 快速上手

### 导出

```java
@RestController
@RequestMapping("/demo/user")
public class UserExportController {

    private final ExcelTemplate excelTemplate;

    public UserExportController(ExcelTemplate excelTemplate) {
        this.excelTemplate = excelTemplate;
    }

    // GET /demo/user/export → 下载「用户列表.xlsx」
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        List<UserExcelRow> rows = List.of(
                new UserExcelRow("张三", 20),
                new UserExcelRow("李四", 30));
        // fileName 不含扩展名，方法自动补 .xlsx 并对中文做 UTF-8 编码
        excelTemplate.export(response, "用户列表", UserExcelRow.class, rows);
    }
}
```

### 导入

```java
// POST /demo/user/import（multipart/form-data，字段名 file）
@PostMapping("/import")
public R<String> importExcel(@RequestParam("file") MultipartFile file) {
    ImportResult<UserExcelRow> result = excelTemplate.importExcel(file, UserExcelRow.class);
    if (!result.getFailRows().isEmpty()) {
        // 部分失败：把失败行号与原因返回前端，由用户改完重新上传
        String detail = result.getFailRows().stream()
                .map(f -> "第" + f.getRowIndex() + "行：" + f.getReason())
                .collect(Collectors.joining("；"));
        return R.fail(CommonErrorCode.PARAM_INVALID, "导入存在失败行：" + detail);
    }
    // 全量成功：批量入库
    userService.saveBatch(result.getSuccessRows());
    return R.ok("导入成功 " + result.getSuccessRows().size() + " 条");
}
```

### 行 DTO

读写共用同一个 DTO，字段用 `@ExcelProperty` 标注列名，**必须提供 getter/setter**（FastExcel 写侧按 JavaBean 属性取值）：

```java
public class UserExcelRow {

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("年龄")
    private Integer age;

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
}
```

## 能力边界

1. **只支持 xlsx**：导入强制校验 `.xlsx` 扩展名（不区分大小写），并嗅探 ZIP 魔数拦截「文本文件改名」——FastExcel 对非 zip 内容会静默降级为 CSV 解析，不报错也读不到行，必须自行拦截。旧版 .xls、.csv 请直接用 FastExcel 原生 API。
2. **逐行容错的边界**：行级转换异常（如数字列填了文本）进入 `failRows`，解析继续；但文件级故障（内容非 zip、zip 结构损坏、流读取失败）无法逐行恢复，直接抛 `BizException(PARAM_INVALID)`。
3. **只读第一个 sheet、单行表头**：多 sheet、合并单元格、复杂表头不在封装范围内，请使用 FastExcel 原生 API。
4. **失败行内容只是摘要**：FastExcel 转换异常回调只暴露失败单元格，`ImportFailRow.rawContent` 形如「第2列[abc]」，不是整行原始数据。
5. **DTO 必须是标准 JavaBean**：缺 getter 时导出只写得出表头（数据行静默丢失），这是 FastExcel 写侧的固有行为。

## 注意事项

1. **导入文件前置校验顺序**：空文件 → 扩展名 → ZIP 魔数 → 逐行解析，前三关任一不过抛 `BizException`，错误码均为 `CommonErrorCode.PARAM_INVALID`。
2. **物理行号含表头**：`ImportFailRow.rowIndex` 从 1 起算，表头为第 1 行、首行数据为第 2 行，与 Excel 界面显示的行号一致，可直接回显给用户。
3. **`ImportResult` 没有 hasFail()**：项目硬约束 POJO 非属性方法禁止 is/get 前缀（Jackson 会当属性序列化），判断失败用 `!result.getFailRows().isEmpty()`。
4. **导出文件名编码**：`Content-Disposition` 采用 `filename*=UTF-8''` 形式，现代浏览器均支持；fileName 传不含扩展名的纯名称，扩展名由方法补齐。
5. **大文件流式**：FastExcel 本身为 SAX 流式读写，封装未引入全量缓存（导入仅缓存解析结果对象），百万行级导出不会 OOM，但 `rows` 参数本身建议分页查询分批提供时需自行控制内存。
