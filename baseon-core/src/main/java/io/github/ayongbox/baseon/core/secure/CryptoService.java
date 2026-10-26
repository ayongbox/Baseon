package io.github.ayongbox.baseon.core.secure;

/**
 * 加解密与摘要契约：口令散列、字段级加解密的统一入口。
 *
 * <p>内核不内置实现，避免把具体算法与密钥管理锁进内核。
 * 简单场景由业务侧自行实现（如 BCrypt / AES-GCM）；
 * 企业级密钥托管与轮换由后续 starter-kms（S-22）实现本契约。
 */
public interface CryptoService {

    /**
     * 对明文做不可逆摘要（口令存储场景）。
     */
    String digest(String plainText);

    /**
     * 校验明文与摘要是否匹配。
     */
    boolean matches(String plainText, String digest);

    /**
     * 对称加密（字段级敏感数据落库场景）。
     */
    String encrypt(String plainText);

    /**
     * 对称解密。
     */
    String decrypt(String cipherText);
}
