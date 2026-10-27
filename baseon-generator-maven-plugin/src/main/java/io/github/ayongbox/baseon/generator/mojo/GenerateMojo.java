package io.github.ayongbox.baseon.generator.mojo;

import io.github.ayongbox.baseon.generator.core.CrudRenderer;
import io.github.ayongbox.baseon.generator.core.GeneratedFile;
import io.github.ayongbox.baseon.generator.core.GeneratorContext;
import io.github.ayongbox.baseon.generator.core.TableMeta;
import io.github.ayongbox.baseon.generator.core.TableMetaReader;
import io.github.ayongbox.baseon.generator.meta.MybatisPlusTableMetaReader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * CRUD 代码骨架生成入口：{@code mvn baseon:generate -Dtable=t_demo_user}。
 *
 * <p>薄壳职责：解析参数 → 读表元数据 → 渲染 → 落盘；全部业务逻辑在 core/meta 包。
 */
@Mojo(name = "generate", threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    /** 目标表名，必填，逗号分隔多张表 */
    @Parameter(property = "table", required = true)
    private String table;

    /** 子模块名，可空；非空时输出到 {@code <outputDir>/<module>/src/main/java/...} 且包名追加模块段 */
    @Parameter(property = "module")
    private String module;

    /** 生成代码的根包 */
    @Parameter(property = "basePackage", defaultValue = "com.example.demo")
    private String basePackage;

    /** 输出根目录 */
    @Parameter(property = "outputDir", defaultValue = "${project.build.directory}/baseon-generated")
    private File outputDir;

    /** 数据库连接，默认对齐本地 demo 环境 */
    @Parameter(property = "dbUrl", defaultValue = "jdbc:mysql://127.0.0.1:3306/baseon_master")
    private String dbUrl;

    @Parameter(property = "dbUser", defaultValue = "root")
    private String dbUser;

    @Parameter(property = "dbPassword", defaultValue = "root")
    private String dbPassword;

    @Override
    public void execute() throws MojoExecutionException {
        List<String> tableNames = Arrays.stream(table.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
        if (tableNames.isEmpty()) {
            throw new MojoExecutionException("参数 -Dtable 未提供有效表名，示例: mvn baseon:generate -Dtable=t_demo_user");
        }

        TableMetaReader reader = new MybatisPlusTableMetaReader(dbUrl, dbUser, dbPassword);
        List<TableMeta> tables;
        try {
            tables = reader.readTables(tableNames);
        } catch (RuntimeException e) {
            throw new MojoExecutionException("读取表元数据失败: " + e.getMessage(), e);
        }

        CrudRenderer renderer = new CrudRenderer();
        GeneratorContext context = new GeneratorContext(basePackage, module);
        for (TableMeta tableMeta : tables) {
            for (GeneratedFile file : renderer.render(tableMeta, context)) {
                write(file);
            }
        }
        getLog().info("生成完成，输出目录: " + outputDir.getAbsolutePath());
    }

    /**
     * 落盘：已存在的文件直接覆盖，保证生成结果与库表现状一致。
     */
    private void write(GeneratedFile file) throws MojoExecutionException {
        Path target = outputDir.toPath().resolve(file.relativePath());
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, file.content(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("写入生成文件失败: " + target, e);
        }
        getLog().info("已生成: " + target);
    }
}
