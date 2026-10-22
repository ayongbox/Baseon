package io.github.ayongbox.baseon.core.tool;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额工具：统一 scale=2、HALF_UP，禁止 double/float 参与金额计算。
 */
public final class MoneyUtil {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    private MoneyUtil() {
    }

    /**
     * 归一化到两位小数，入参为 null 时返回 null。
     */
    public static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, ROUNDING_MODE);
    }

    public static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(SCALE) : value;
    }

    /**
     * 金额比较必须用 compareTo，不能用 equals（1.0 与 1.00 的 equals 为 false）。
     */
    public static boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
