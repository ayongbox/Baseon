package io.github.ayongbox.baseon.starter.cloud.nacos;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * starter-cloud-nacos 架构约束：规则一旦不满足即构建失败，不依赖人工自觉。
 * 本 starter 无任何 starter 依赖（允许清单为空），禁止依赖其余全部 starter 包。
 */
@AnalyzeClasses(
        packages = "io.github.ayongbox.baseon.starter.cloud.nacos",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class CloudNacosArchitectureTest {

    private CloudNacosArchitectureTest() {
    }

    @ArchTest
    static final ArchRule should_not_depend_on_demo =
            noClasses()
                    .that().resideInAnyPackage("io.github.ayongbox.baseon.starter.cloud.nacos..")
                    .should().dependOnClassesThat().resideInAnyPackage("io.github.ayongbox.baseon.demo..")
                    .because("Starter 是可复用组件，不得反向依赖 samples 示例代码");

    @ArchTest
    static final ArchRule should_not_depend_on_other_starters =
            noClasses()
                    .that().resideInAnyPackage("io.github.ayongbox.baseon.starter.cloud.nacos..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "io.github.ayongbox.baseon.starter.web..",
                            "io.github.ayongbox.baseon.starter.trace..",
                            "io.github.ayongbox.baseon.starter.cloud.feign..",
                            "io.github.ayongbox.baseon.starter.loadbalance..",
                            "io.github.ayongbox.baseon.starter.datasource..",
                            "io.github.ayongbox.baseon.starter.mybatisplus..",
                            "io.github.ayongbox.baseon.starter.redis..",
                            "io.github.ayongbox.baseon.starter.cache..",
                            "io.github.ayongbox.baseon.starter.tenant..",
                            "io.github.ayongbox.baseon.starter.auth..",
                            "io.github.ayongbox.baseon.starter.xss..",
                            "io.github.ayongbox.baseon.starter.idempotent..",
                            "io.github.ayongbox.baseon.starter.sentinel..",
                            "io.github.ayongbox.baseon.starter.resilience..",
                            "io.github.ayongbox.baseon.starter.excel..",
                            "io.github.ayongbox.baseon.starter.swagger..",
                            "io.github.ayongbox.baseon.starter.log..",
                            "io.github.ayongbox.baseon.starter.debug..")
                    .because("本 starter 无 starter 依赖，禁止依赖其它 starter 包");
}
