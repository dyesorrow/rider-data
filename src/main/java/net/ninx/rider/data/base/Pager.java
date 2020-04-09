package net.ninx.rider.data.base;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * PageResult
 */
@Getter
@Setter
public class Pager<T> {
    private long pageAt;
    private long pageSize;
    private long totalPage;
    private long totalCount;
    private List<T> data;

    private Pager() {
    }

    @SuppressWarnings("rawtypes")
    public static Pager of(Long pageAt, Long pageSize) {
        pageAt = pageAt == null ? 1 : pageAt;
        pageSize = pageSize == null ? 10 : pageSize;

        Pager pageResult = new Pager();
        pageResult.setPageAt(pageAt);
        pageResult.setPageSize(pageSize);
        return pageResult;
    }

}