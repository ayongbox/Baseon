package io.github.ayongbox.baseon.core.launch;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * core.launch 架构约束：只允许依赖 Spring Boot 生命周期，不得触碰服务治理与 Starter 层。
 */
@AnalyzeClasses(
        packages = "io.github.ayongbox.baseon.core.launch",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class CoreLaunchArchitectureTest {

    private CoreLaunchArchitectureTest() {
    }

    @ArchTest
    static final ArchRule should_not_depend_on_starters =
            noClasses()
                    .that().resideInAPackage("io.github.ayongbox.baseon.core.launch..")
                    .should().dependOnClassesThat().resideInAnyPackage("io.github.ayongbox.baseon.starter..")
                    .because("依赖方向单向：Starter 依赖内核扩展，core.launch 不得反向依赖 Starter");

    @ArchTest
    static final ArchRule should_not_depend_on_spring_cloud =
            noClasses()
                    .that().resideInAPackage("io.github.ayongbox.baseon.core.launch..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework.cloud..")
                    .because("core.launch 只承载启动治理能力，微服务治理属于 Starter 层");
}
