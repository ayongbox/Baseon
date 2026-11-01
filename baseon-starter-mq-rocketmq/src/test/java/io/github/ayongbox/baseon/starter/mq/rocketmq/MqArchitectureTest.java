package io.github.ayongbox.baseon.starter.mq.rocketmq;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * starter-mq-rocketmq 架构约束：规则一旦不满足即构建失败，不依赖人工自觉。
 * 允许清单（含传递闭包）：starter-redis（optional，消费幂等）。
 */
@AnalyzeClasses(
        packages = "io.github.ayongbox.baseon.starter.mq.rocketmq",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class MqArchitectureTest {

    private MqArchitectureTest() {
    }

    @ArchTest
    static final ArchRule should_not_depend_on_demo =
            noClasses()
                    .that().resideInAnyPackage("io.github.ayongbox.baseon.starter.mq.rocketmq..")
                    .should().dependOnClassesThat().resideInAnyPackage("io.github.ayongbox.baseon.demo..")
                    .because("Starter 是可复用组件，不得反向依赖 samples 示例代码");

    @ArchTest
    static final ArchRule should_not_depend_on_starters_outside_closure =
            noClasses()
                    .that().resideInAnyPackage("io.github.ayongbox.baseon.starter.mq.rocketmq..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "io.github.ayongbox.baseon.starter.web..",
                            "io.github.ayongbox.baseon.starter.trace..",
                            "io.github.ayongbox.baseon.starter.cloud.nacos..",
                            "io.github.ayongbox.baseon.starter.cloud.feign..",
                            "io.github.ayongbox.baseon.starter.loadbalance..",
                            "io.github.ayongbox.baseon.starter.datasource..",
                            "io.github.ayongbox.baseon.starter.mybatisplus..",
                            "io.github.ayongbox.baseon.starter.cache..",
                            "io.github.ayongbox.baseon.starter.tenant..",
                            "io.github.ayongbox.baseon.starter.datascope..",
                            "io.github.ayongbox.baseon.starter.auth..",
                            "io.github.ayongbox.baseon.starter.xss..",
                            "io.github.ayongbox.baseon.starter.idempotent..",
                            "io.github.ayongbox.baseon.starter.sentinel..",
                            "io.github.ayongbox.baseon.starter.resilience..",
                            "io.github.ayongbox.baseon.starter.excel..",
                            "io.github.ayongbox.baseon.starter.swagger..",
                            "io.github.ayongbox.baseon.starter.log..",
                            "io.github.ayongbox.baseon.starter.debug..",
                            "io.github.ayongbox.baseon.starter.tracing..",
                            "io.github.ayongbox.baseon.starter.oss..")
                    .because("依赖方向受控：本 starter 仅允许依赖 starter-redis（optional），其余 starter 一律禁止");
}
