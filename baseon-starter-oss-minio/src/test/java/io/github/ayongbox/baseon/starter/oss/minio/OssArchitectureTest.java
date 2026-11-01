package io.github.ayongbox.baseon.starter.oss.minio;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * starter-oss-minio 架构约束：规则一旦不满足即构建失败，不依赖人工自觉。
 * 本模块不依赖任何其它 starter。
 */
@AnalyzeClasses(
        packages = "io.github.ayongbox.baseon.starter.oss.minio",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class OssArchitectureTest {

    private OssArchitectureTest() {
    }

    @ArchTest
    static final ArchRule should_not_depend_on_demo =
            noClasses()
                    .that().resideInAnyPackage("io.github.ayongbox.baseon.starter.oss.minio..")
                    .should().dependOnClassesThat().resideInAnyPackage("io.github.ayongbox.baseon.demo..")
                    .because("Starter 是可复用组件，不得反向依赖 samples 示例代码");

    @ArchTest
    static final ArchRule should_not_depend_on_other_starters =
            noClasses()
                    .that().resideInAnyPackage("io.github.ayongbox.baseon.starter.oss.minio..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "io.github.ayongbox.baseon.starter.web..",
                            "io.github.ayongbox.baseon.starter.trace..",
                            "io.github.ayongbox.baseon.starter.cloud.nacos..",
                            "io.github.ayongbox.baseon.starter.cloud.feign..",
                            "io.github.ayongbox.baseon.starter.loadbalance..",
                            "io.github.ayongbox.baseon.starter.datasource..",
                            "io.github.ayongbox.baseon.starter.mybatisplus..",
                            "io.github.ayongbox.baseon.starter.redis..",
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
                            "io.github.ayongbox.baseon.starter.mq..")
                    .because("依赖方向受控：对象存储不依赖任何其它 starter，保持最小闭包");
}
