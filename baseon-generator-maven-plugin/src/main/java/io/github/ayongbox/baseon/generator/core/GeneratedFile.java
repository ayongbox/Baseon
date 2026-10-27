package io.github.ayongbox.baseon.generator.core;

/**
 * 渲染产物：相对输出根目录的文件路径与文件内容，落盘动作由调用方负责。
 *
 * @param relativePath 相对路径，如 system/src/main/java/com/example/demo/system/entity/DemoUser.java
 * @param content      文件内容（UTF-8 文本）
 */
public record GeneratedFile(String relativePath, String content) {
}
