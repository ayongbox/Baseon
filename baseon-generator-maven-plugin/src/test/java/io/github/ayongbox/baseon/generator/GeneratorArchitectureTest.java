package io.github.ayongbox.baseon.generator;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 生成器架构约束：core 必须是可独立单测的纯库，规则不满足即构建失败。
 */
@AnalyzeClasses(
        packages = "io.github.ayongbox.baseon.generator",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class GeneratorArchitectureTest {

    private GeneratorArchitectureTest() {
    }

    @ArchTest
    static final ArchRule core_should_not_depend_on_maven =
            noClasses()
                    .that().resideInAnyPackage("..generator.core..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.apache.maven..")
                    .because("core 是纯库，Maven API 只允许出现在 mojo 薄壳层");

    @ArchTest
    static final ArchRule core_should_not_depend_on_meta_or_mojo =
            noClasses()
                    .that().resideInAnyPackage("..generator.core..")
                    .should().dependOnClassesThat().resideInAnyPackage("..generator.meta..", "..generator.mojo..")
                    .because("依赖方向单向：mojo → meta → core，core 不得反向依赖适配层");

    @ArchTest
    static final ArchRule core_should_not_depend_on_mybatis_plus =
            noClasses()
                    .that().resideInAnyPackage("..generator.core..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.baomidou..")
                    .because("元数据来源可替换，core 不允许绑定 MyBatis-Plus API");
}
