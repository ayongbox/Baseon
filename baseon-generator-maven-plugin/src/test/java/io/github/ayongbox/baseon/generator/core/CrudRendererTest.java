package io.github.ayongbox.baseon.generator.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrudRendererTest {

    private final CrudRenderer renderer = new CrudRenderer();

    /**
     * 假表：覆盖租户列、主键、字符串/整数/金额/日期四类常见类型与审计列。
     */
    private TableMeta fakeTable() {
        List<ColumnMeta> columns = List.of(
                new ColumnMeta("id", "id", "Long", "主键", true),
                new ColumnMeta("tenant_id", "tenantId", "Long", "租户 ID", false),
                new ColumnMeta("name", "name", "String", "姓名", false),
                new ColumnMeta("age", "age", "Integer", "年龄", false),
                new ColumnMeta("balance", "balance", "BigDecimal", "余额", false),
                new ColumnMeta("birthday", "birthday", "LocalDate", "生日", false),
                new ColumnMeta("create_time", "createTime", "LocalDateTime", "创建时间", false));
        return new TableMeta("t_demo_user", NamingConverter.toEntityName("t_demo_user"),
                "演示用户表", columns, true);
    }

    private Map<String, GeneratedFile> renderByFileName(TableMeta table, GeneratorContext context) {
        return renderer.render(table, context).stream()
                .collect(Collectors.toMap(
                        file -> file.relativePath().substring(file.relativePath().lastIndexOf('/') + 1),
                        Function.identity()));
    }

    @Test
    void should_render_nine_files_per_table() {
        assertEquals(9, renderer.render(fakeTable(), new GeneratorContext("com.example.demo", null)).size());
    }

    @Test
    void entity_should_extend_tenant_parent_and_skip_audit_columns() {
        GeneratedFile entity = renderByFileName(fakeTable(), new GeneratorContext("com.example.demo", null))
                .get("DemoUser.java");
        assertEquals("src/main/java/com/example/demo/entity/DemoUser.java", entity.relativePath());
        assertTrue(entity.content().contains("package com.example.demo.entity;"));
        assertTrue(entity.content().contains("@TableName(\"t_demo_user\")"));
        assertTrue(entity.content().contains("extends TenantEntity"));
        assertTrue(entity.content().contains("import io.github.ayongbox.baseon.core.model.TenantEntity;"));
        assertTrue(entity.content().contains("private String name;"));
        assertTrue(entity.content().contains("private BigDecimal balance;"));
        assertTrue(entity.content().contains("import java.math.BigDecimal;"));
        // 审计字段由父类承载，实体禁止重复声明
        assertFalse(entity.content().contains("private Long id;"));
        assertFalse(entity.content().contains("createTime"));
        assertFalse(entity.content().contains("tenantId"));
        assertFalse(entity.content().contains("import java.time.LocalDateTime;"));
    }

    @Test
    void mapper_should_extend_base_mapper() {
        GeneratedFile mapper = renderByFileName(fakeTable(), new GeneratorContext("com.example.demo", null))
                .get("DemoUserMapper.java");
        assertTrue(mapper.content().contains("public interface DemoUserMapper extends BaseMapper<DemoUser>"));
        assertTrue(mapper.content().contains("import com.baomidou.mybatisplus.core.mapper.BaseMapper;"));
    }

    @Test
    void controller_should_return_r_and_page_result() {
        GeneratedFile controller = renderByFileName(fakeTable(), new GeneratorContext("com.example.demo", null))
                .get("DemoUserController.java");
        assertTrue(controller.content().contains("import io.github.ayongbox.baseon.core.model.R;"));
        assertTrue(controller.content().contains("import io.github.ayongbox.baseon.core.model.PageResult;"));
        assertTrue(controller.content().contains("@RequestMapping(\"/demo-users\")"));
        assertTrue(controller.content().contains("R<PageResult<DemoUserVO>> page(PageQuery query)"));
    }

    @Test
    void converter_should_use_spring_component_model() {
        GeneratedFile converter = renderByFileName(fakeTable(), new GeneratorContext("com.example.demo", null))
                .get("DemoUserConverter.java");
        assertTrue(converter.content().contains("@Mapper(componentModel = \"spring\")"));
        assertTrue(converter.content().contains("DemoUser toEntity(DemoUserReq req);"));
    }

    @Test
    void service_impl_should_implement_service_interface() {
        GeneratedFile serviceImpl = renderByFileName(fakeTable(), new GeneratorContext("com.example.demo", null))
                .get("DemoUserServiceImpl.java");
        assertEquals("src/main/java/com/example/demo/service/impl/DemoUserServiceImpl.java",
                serviceImpl.relativePath());
        assertTrue(serviceImpl.content().contains("public class DemoUserServiceImpl implements DemoUserService"));
    }

    @Test
    void module_should_affect_both_package_and_directory() {
        Map<String, GeneratedFile> files = renderByFileName(fakeTable(),
                new GeneratorContext("com.example.demo", "system"));
        GeneratedFile entity = files.get("DemoUser.java");
        assertEquals("system/src/main/java/com/example/demo/system/entity/DemoUser.java", entity.relativePath());
        assertTrue(entity.content().contains("package com.example.demo.system.entity;"));
    }

    @Test
    void non_tenant_table_should_extend_base_entity() {
        TableMeta plain = new TableMeta("t_dict", "Dict", "字典表",
                List.of(new ColumnMeta("id", "id", "Long", "主键", true),
                        new ColumnMeta("label", "label", "String", "显示名", false)),
                false);
        GeneratedFile entity = renderByFileName(plain, new GeneratorContext("com.example.demo", null))
                .get("Dict.java");
        assertTrue(entity.content().contains("extends BaseEntity"));
    }
}
