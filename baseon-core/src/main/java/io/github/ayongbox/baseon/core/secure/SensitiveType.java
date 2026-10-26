package io.github.ayongbox.baseon.core.secure;

/**
 * 敏感数据分级：决定日志脱敏、存储加密、传输保护的处置强度。
 *
 * <p>业务字段定级后，日志输出按级别脱敏（L3/L4 默认全掩码），
 * 落库加密与 KMS 托管按级别逐级收紧。
 */
public enum SensitiveType {

    /** L1 公开：可自由流转，如商品名称 */
    L1_PUBLIC,

    /** L2 内部：内部可见，离职/外包受限，如部门名 */
    L2_INTERNAL,

    /** L3 秘密：个人身份与联系方式，日志脱敏 + 权限收敛，如手机号、身份证号 */
    L3_CONFIDENTIAL,

    /** L4 机密：凭证与金融数据，日志全掩码 + 落库加密 + KMS 托管，如口令、银行卡号 */
    L4_SECRET
}
