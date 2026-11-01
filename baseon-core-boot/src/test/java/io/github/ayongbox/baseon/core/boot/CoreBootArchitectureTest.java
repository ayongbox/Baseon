package io.github.ayongbox.baseon.core.boot;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * core.boot 架构约束：只允许依赖 spring-context / spring-tx / spring-boot-autoconfigure，
 * 不得触碰服务治理与 Starter 层。
 */
@AnalyzeClasses(
        packages = "io.github.ayongbox.baseon.core.boot",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class CoreBootArchitectureTest {

    private CoreBootArchitectureTest() {
    }

    @ArchTest
    static final ArchRule should_not_depend_on_starters =
            noClasses()
                    .that().resideInAPackage("io.github.ayongbox.baseon.core.boot..")
                    .should().dependOnClassesThat().resideInAnyPackage("io.github.ayongbox.baseon.starter..")
                    .because("依赖方向单向：Starter 依赖内核，core.boot 不得反向依赖 Starter");

    @ArchTest
    static final ArchRule should_not_depend_on_spring_cloud =
            noClasses()
                    .that().resideInAPackage("io.github.ayongbox.baseon.core.boot..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework.cloud..")
                    .because("core.boot 只承载单体容器能力，微服务治理属于 Starter 层");
}
