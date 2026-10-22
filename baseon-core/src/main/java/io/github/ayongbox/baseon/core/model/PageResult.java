package io.github.ayongbox.baseon.core.model;

import java.util.List;

/**
 * 统一分页返回体，字段命名与 MyBatis-Plus Page 保持一致，前端无需适配两套结构。
 *
 * @param <T> 记录类型
 */
public class PageResult<T> {

    private long total;
    private long pages;
    private long current;
    private long size;
    private List<T> records;

    public PageResult() {
    }

    private PageResult(long total, long pages, long current, long size, List<T> records) {
        this.total = total;
        this.pages = pages;
        this.current = current;
        this.size = size;
        this.records = records;
    }

    public static <T> PageResult<T> of(long total, long current, long size, List<T> records) {
        long pages = size <= 0 ? 0 : (total + size - 1) / size;
        return new PageResult<>(total, pages, current, size, records);
    }

    public static <T> PageResult<T> empty(long current, long size) {
        return of(0, current, size, List.of());
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPages() {
        return pages;
    }

    public void setPages(long pages) {
        this.pages = pages;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}
