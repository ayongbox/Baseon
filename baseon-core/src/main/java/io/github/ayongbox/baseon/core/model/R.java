package io.github.ayongbox.baseon.core.model;

/**
 * 统一返回体。
 *
 * <p>内核零依赖，因此不携带任何 JSON 注解；序列化策略（null 字段省略、Long 转 String、
 * 枚举按 code 输出）统一由 starter-web 的 Jackson 配置控制。
 *
 * @param <T> 业务数据类型
 */
public class R<T> {

    /** 成功码固定为 0 */
    public static final int SUCCESS_CODE = 0;

    private int code;
    private String msg;
    private T data;

    /** 供反序列化框架使用 */
    public R() {
    }

    private R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> R<T> ok() {
        return new R<>(SUCCESS_CODE, CommonErrorCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(SUCCESS_CODE, CommonErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> R<T> fail(BizException exception) {
        return new R<>(exception.getCode(), exception.getMessage(), null);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    /**
     * 不依赖序列化框架的成功判断，业务分支判断统一用它，禁止手写 code == 0。
     *
     * <p>刻意不命名为 isSuccess：Jackson 会把 isXxx 当作属性输出，
     * 而内核不允许引入 @JsonIgnore，故用非 getter 形式命名，保证响应体只含 code/msg/data。
     */
    public boolean success() {
        return code == SUCCESS_CODE;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
