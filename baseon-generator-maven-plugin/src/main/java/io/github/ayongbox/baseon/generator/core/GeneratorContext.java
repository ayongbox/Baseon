package io.github.ayongbox.baseon.generator.core;

/**
 * 生成上下文：渲染一次所需的外部参数，与具体表无关。
 *
 * @param basePackage 根包，如 com.example.demo
 * @param module      子模块名，可空；非空时同时影响输出目录（&lt;module&gt;/src/main/java/...）
 *                    与生成包名（basePackage.module）
 */
public record GeneratorContext(String basePackage, String module) {

    /**
     * 生成代码的根包：带模块时追加模块段（模块名中的连字符对包名非法，直接剔除）。
     */
    public String rootPackage() {
        if (module == null || module.isBlank()) {
            return basePackage;
        }
        return basePackage + "." + module.replace("-", "");
    }

    /**
     * 生成文件相对输出根目录的源码前缀，如 system/src/main/java/com/example/demo/system。
     */
    public String sourcePrefix() {
        String packagePath = rootPackage().replace('.', '/');
        if (module == null || module.isBlank()) {
            return "src/main/java/" + packagePath;
        }
        return module + "/src/main/java/" + packagePath;
    }
}
