package io.github.ayongbox.baseon.core;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 内核架构约束：规则一旦不满足即构建失败，不依赖人工自觉。
 */
@AnalyzeClasses(
        packages = "io.github.ayongbox.baseon.core",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class CoreArchitectureTest {

    private CoreArchitectureTest() {
    }

    @ArchTest
    static final ArchRule pure_kernel_should_not_depend_on_spring =
            noClasses()
                    .that().resideInAnyPackage("..core.model..", "..core.tool..", "..core.context..", "..core.secure..",
                            "..core.db..", "..core.annotation..", "..core.cloud..", "..core.spi..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                    .because("内核必须零 Spring 依赖，容器能力全部下沉到 Starter 层");

    @ArchTest
    static final ArchRule core_should_not_depend_on_starters =
            noClasses()
                    .that().resideInAnyPackage("io.github.ayongbox.baseon.core..")
                    .should().dependOnClassesThat().resideInAnyPackage("io.github.ayongbox.baseon.starter..")
                    .because("依赖方向单向：Starter 依赖内核，内核不得反向依赖 Starter");

    @ArchTest
    static final ArchRule model_should_not_depend_on_tool =
            noClasses()
                    .that().resideInAnyPackage("..core.model..")
                    .should().dependOnClassesThat().resideInAnyPackage("..core.tool..", "..core.context..")
                    .because("model 是最底层模型，不允许反向依赖工具与上下文");

    @ArchTest
    static final ArchRule db_should_not_depend_on_tool_or_orm =
            noClasses()
                    .that().resideInAnyPackage("..core.db..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..core.tool..", "..core.context..", "com.baomidou..", "jakarta.persistence..")
                    .because("core.db 只放分页与持久化 PO 纯契约，不引入工具、上下文与任何 ORM 依赖");
}
