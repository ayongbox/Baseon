package io.github.ayongbox.baseon.generator.core;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * CRUD 渲染器：按表元数据渲染 9 个代码骨架文件（Entity / Mapper / Service /
 * ServiceImpl / Controller / Converter / Req / VO / Resp）。
 *
 * <p>纯库实现：不感知 Maven 与数据库，模板数据模型在本类内一次性装配，
 * freemarker 模板只负责排版，不允许出现命名或类型推断逻辑。
 */
public class CrudRenderer {

    /** 内核实体父类包（生成代码依赖 baseon-core） */
    private static final String CORE_MODEL_PACKAGE = "io.github.ayongbox.baseon.core.model";

    /**
     * 渲染产物清单：模板名 + 输出子包 + 类名后缀。
     */
    private enum Artifact {
        ENTITY("entity.java.ftl", "entity", ""),
        MAPPER("mapper.java.ftl", "mapper", "Mapper"),
        SERVICE("service.java.ftl", "service", "Service"),
        SERVICE_IMPL("serviceImpl.java.ftl", "service/impl", "ServiceImpl"),
        CONTROLLER("controller.java.ftl", "controller", "Controller"),
        CONVERTER("converter.java.ftl", "converter", "Converter"),
        REQ("req.java.ftl", "req", "Req"),
        VO("vo.java.ftl", "vo", "VO"),
        RESP("resp.java.ftl", "resp", "Resp");

        private final String templateName;
        private final String subPackage;
        private final String classSuffix;

        Artifact(String templateName, String subPackage, String classSuffix) {
            this.templateName = templateName;
            this.subPackage = subPackage;
            this.classSuffix = classSuffix;
        }
    }

    private final Configuration configuration;

    public CrudRenderer() {
        this.configuration = new Configuration(Configuration.VERSION_2_3_31);
        this.configuration.setClassForTemplateLoading(CrudRenderer.class, "/templates");
        this.configuration.setDefaultEncoding("UTF-8");
        this.configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        this.configuration.setLogTemplateExceptions(false);
        this.configuration.setWrapUncheckedExceptions(true);
    }

    /**
     * 渲染一张表的全部骨架文件。
     *
     * @param table   表元数据
     * @param context 生成上下文（根包 / 子模块）
     * @return 9 个渲染产物，相对路径已含模块与包目录
     */
    public List<GeneratedFile> render(TableMeta table, GeneratorContext context) {
        Map<String, Object> baseModel = buildBaseModel(table, context);
        List<GeneratedFile> files = new ArrayList<>();
        for (Artifact artifact : Artifact.values()) {
            Map<String, Object> model = new HashMap<>(baseModel);
            model.put("imports", importsOf(artifact, table, context));
            String className = table.getEntityName() + artifact.classSuffix;
            String relativePath = context.sourcePrefix() + "/" + artifact.subPackage + "/" + className + ".java";
            files.add(new GeneratedFile(relativePath, renderTemplate(artifact.templateName, model)));
        }
        return files;
    }

    /**
     * 装配模板公共数据模型：包名、类名、字段清单等排版所需信息。
     */
    private Map<String, Object> buildBaseModel(TableMeta table, GeneratorContext context) {
        String rootPackage = context.rootPackage();
        String entityName = table.getEntityName();
        Map<String, Object> model = new HashMap<>();
        model.put("rootPackage", rootPackage);
        model.put("tableName", table.getTableName());
        model.put("tableComment", table.getTableComment() == null ? "" : table.getTableComment());
        model.put("entityName", entityName);
        model.put("tenantTable", table.isTenantTable());
        model.put("entityParent", table.isTenantTable() ? "TenantEntity" : "BaseEntity");
        model.put("varName", NamingConverter.toVariableName(entityName));
        model.put("requestPath", NamingConverter.toRequestPath(entityName));
        model.put("entityFields", table.getEntityColumns());
        model.put("entityPackage", rootPackage + ".entity");
        model.put("mapperPackage", rootPackage + ".mapper");
        model.put("servicePackage", rootPackage + ".service");
        model.put("serviceImplPackage", rootPackage + ".service.impl");
        model.put("controllerPackage", rootPackage + ".controller");
        model.put("converterPackage", rootPackage + ".converter");
        model.put("reqPackage", rootPackage + ".req");
        model.put("voPackage", rootPackage + ".vo");
        model.put("respPackage", rootPackage + ".resp");
        return model;
    }

    /**
     * 按产物类型计算 import 清单，生成排序后的稳定输出。
     */
    private List<String> importsOf(Artifact artifact, TableMeta table, GeneratorContext context) {
        String rootPackage = context.rootPackage();
        String entityName = table.getEntityName();
        TreeSet<String> imports = new TreeSet<>();
        switch (artifact) {
            case ENTITY -> {
                imports.add("com.baomidou.mybatisplus.annotation.TableName");
                imports.add(CORE_MODEL_PACKAGE + "." + (table.isTenantTable() ? "TenantEntity" : "BaseEntity"));
                imports.addAll(typeImports(table.getEntityColumns()));
            }
            case MAPPER -> {
                imports.add("com.baomidou.mybatisplus.core.mapper.BaseMapper");
                imports.add("org.apache.ibatis.annotations.Mapper");
                imports.add(rootPackage + ".entity." + entityName);
            }
            case SERVICE -> {
                imports.add(CORE_MODEL_PACKAGE + ".PageResult");
                imports.add(CORE_MODEL_PACKAGE + ".PageQuery");
                imports.add(rootPackage + ".req." + entityName + "Req");
                imports.add(rootPackage + ".resp." + entityName + "Resp");
                imports.add(rootPackage + ".vo." + entityName + "VO");
                imports.add("java.util.List");
            }
            case SERVICE_IMPL -> {
                imports.add("com.baomidou.mybatisplus.extension.plugins.pagination.Page");
                imports.add(CORE_MODEL_PACKAGE + ".PageResult");
                imports.add(CORE_MODEL_PACKAGE + ".PageQuery");
                imports.add(rootPackage + ".converter." + entityName + "Converter");
                imports.add(rootPackage + ".entity." + entityName);
                imports.add(rootPackage + ".mapper." + entityName + "Mapper");
                imports.add(rootPackage + ".req." + entityName + "Req");
                imports.add(rootPackage + ".resp." + entityName + "Resp");
                imports.add(rootPackage + ".service." + entityName + "Service");
                imports.add(rootPackage + ".vo." + entityName + "VO");
                imports.add("org.springframework.stereotype.Service");
                imports.add("java.util.List");
            }
            case CONTROLLER -> {
                imports.add(CORE_MODEL_PACKAGE + ".PageResult");
                imports.add(CORE_MODEL_PACKAGE + ".PageQuery");
                imports.add(CORE_MODEL_PACKAGE + ".R");
                imports.add(rootPackage + ".req." + entityName + "Req");
                imports.add(rootPackage + ".resp." + entityName + "Resp");
                imports.add(rootPackage + ".service." + entityName + "Service");
                imports.add(rootPackage + ".vo." + entityName + "VO");
                imports.add("org.springframework.web.bind.annotation.DeleteMapping");
                imports.add("org.springframework.web.bind.annotation.GetMapping");
                imports.add("org.springframework.web.bind.annotation.PathVariable");
                imports.add("org.springframework.web.bind.annotation.PostMapping");
                imports.add("org.springframework.web.bind.annotation.PutMapping");
                imports.add("org.springframework.web.bind.annotation.RequestBody");
                imports.add("org.springframework.web.bind.annotation.RequestMapping");
                imports.add("org.springframework.web.bind.annotation.RestController");
                imports.add("java.util.List");
            }
            case CONVERTER -> {
                imports.add("org.mapstruct.Mapper");
                imports.add(rootPackage + ".entity." + entityName);
                imports.add(rootPackage + ".req." + entityName + "Req");
                imports.add(rootPackage + ".resp." + entityName + "Resp");
                imports.add(rootPackage + ".vo." + entityName + "VO");
                imports.add("java.util.List");
            }
            case REQ -> imports.addAll(typeImports(table.getEntityColumns()));
            case VO, RESP -> imports.addAll(typeImports(table.getEntityColumns()));
            // Artifact 为私有枚举，分支已穷尽，default 永不命中
            default -> {
            }
        }
        return List.copyOf(imports);
    }

    /**
     * 收集字段类型需要 import 的全限定名（Long / String 等 java.lang 类型自然缺席）。
     */
    private List<String> typeImports(List<ColumnMeta> fields) {
        TreeSet<String> imports = new TreeSet<>();
        for (ColumnMeta field : fields) {
            JdbcTypeMapper.importOf(field.getJavaType()).ifPresent(imports::add);
        }
        return List.copyOf(imports);
    }

    private String renderTemplate(String templateName, Map<String, Object> model) {
        try {
            Template template = configuration.getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("模板加载失败: " + templateName, e);
        } catch (TemplateException e) {
            throw new IllegalStateException("模板渲染失败: " + templateName + "，" + e.getMessage(), e);
        }
    }
}
