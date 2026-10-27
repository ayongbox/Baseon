package io.github.ayongbox.baseon.starter.tracing.otel;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * starter-tracing-otel 架构约束：规则一旦不满足即构建失败，不依赖人工自觉。
 * 本 starter 仅依赖 starter-trace（协同统一 traceId），禁止依赖其余 starter 包。
 */
@AnalyzeClasses(
        packages = "io.github.ayongbox.baseon.starter.tracing.otel",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class TracingOtelArchitectureTest {

    private TracingOtelArchitectureTest() {
    }

    @ArchTest
    static final ArchRule should_not_depend_on_demo =
            noClasses()
                    .that().resideInAnyPackage("io.github.ayongbox.baseon.starter.tracing.otel..")
                    .should().dependOnClassesThat().resideInAnyPackage("io.github.ayongbox.baseon.demo..")
                    .because("Starter 是可复用组件，不得反向依赖 samples 示例代码");

    @ArchTest
    static final ArchRule should_not_depend_on_other_starters =
            noClasses()
                    .that().resideInAnyPackage("io.github.ayongbox.baseon.starter.tracing.otel..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "io.github.ayongbox.baseon.starter.web..",
                            "io.github.ayongbox.baseon.starter.cloud.nacos..",
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
                    .because("本 starter 仅依赖 starter-trace，禁止依赖其它 starter 包");
}
