package io.github.ayongbox.baseon.core.spi;

/**
 * 缓存 key 前缀提供者 SPI：starter-cache 在拼 key 时调用，返回空前缀表示不追加。
 *
 * <p>默认实现不追加任何前缀；starter-tenant 提供租户前缀实现（如 {@code "t1001:"}），
 * 使同一逻辑 key 在不同租户下落到不同的物理 key，实现缓存隔离。
 */
public interface CacheKeyPrefixProvider {

    /**
     * 当前请求的缓存 key 前缀，含分隔符；返回空串表示不追加。
     */
    String prefix();
}
