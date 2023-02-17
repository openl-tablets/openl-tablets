package org.openl.rules.repository.api;

import java.util.Objects;

/**
 * Page search DTO
 *
 * @author Vladyslav Pikus
 */
public class Page {

    private static final Page UNPAGED = new Page(-1, -1) {
        @Override
        public boolean isUnpaged() {
            return true;
        }

        @Override
        public Page withPage(int page) {
            return this;
        }
    };

    private final int page;
    private final int size;

    private Page(int page, int size) {
        this.page = page;
        this.size = size;
    }

    /**
     * Returns the page to be returned.
     *
     * @return page number
     */
    public int getPageNumber() {
        return page;
    }

    /**
     * Returns the number of items to be returned.
     *
     * @return page size
     */
    public int getPageSize() {
        return size;
    }

    /**
     * Returns the offset to be taken according to the underlying page and page size.
     *
     * @return data offcet
     */
    public int getOffset() {
        return page * size;
    }

    /**
     * Returns whether the current Page does not contain pagination information.
     *
     * @return true or false
     */
    public boolean isUnpaged() {
        return false;
    }

    public Page withPage(int page) {
        return new Page(page, size);
    }

    /**
     * Returns a {@link Page} instance representing no pagination setup.
     *
     * @return unpageable
     */
    public static Page unpaged() {
        return UNPAGED;
    }

    /**
     * Create a new {@link Page} for the first page and given {@code pageSize}
     *
     * @param pageSize the number of elements per page
     * @return new {@link Page} instance
     */
    public static Page ofSize(int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be greater than 0.");
        }
        return new Page(0, pageSize);
    }

    public static Page of(int page, int pageSize) {
        if (page < 0) {
            throw new IllegalArgumentException("Page size must be greater than 1.");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be greater than 0.");
        }
        return new Page(page, pageSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Page page1 = (Page) o;
        return page == page1.page && size == page1.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, size);
    }
}
