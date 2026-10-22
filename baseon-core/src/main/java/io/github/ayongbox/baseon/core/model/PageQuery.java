package io.github.ayongbox.baseon.core.model;

/**
 * 分页请求基类：所有分页查询对象继承本类，禁止各自定义 pageNum / pageSize。
 *
 * <p>size 上限固定 500，防止恶意大分页拖垮数据库；如需更大的导出场景，
 * 走独立的导出接口，不要放宽分页上限。
 */
public class PageQuery {

    public static final long DEFAULT_CURRENT = 1;
    public static final long DEFAULT_SIZE = 10;
    public static final long MAX_SIZE = 500;

    private long current = DEFAULT_CURRENT;
    private long size = DEFAULT_SIZE;

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current < 1 ? DEFAULT_CURRENT : current;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        if (size < 1) {
            this.size = DEFAULT_SIZE;
        } else {
            this.size = Math.min(size, MAX_SIZE);
        }
    }

    /**
     * 数据库分页偏移量，供持久层直接使用。
     */
    public long offset() {
        return (current - 1) * size;
    }
}
