package org.openl.rules.repository.api;

import java.util.Objects;

/**
 * Vanilla implementation of pagination that requires {@code page} and {@code size} parameters. Page offset is
 * auto-calculated from {@code page * size}.
 *
 * @author Vladyslav Pikus
 * @see Offset
 */
public final class Page extends Pageable {

    private final int page;

    private Page(int page, int size) {
        super(size);
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be greater or equal 0.");
        }
        this.page = page;
    }

    /**
     * Returns the page to be returned.
     *
     * @return page number
     */
    @Override
    public int getPageNumber() {
        return page;
    }

    /**
     * Returns the offset to be taken according to the underlying page and page size.
     *
     * @return data offcet
     */
    @Override
    public int getOffset() {
        return page * getPageSize();
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
        return new Page(page, getPageSize());
    }

    /**
     * Create a new {@link Page} for the first page and given {@code pageSize}
     *
     * @param pageSize the number of elements per page
     * @return new {@link Page} instance
     */
    public static Page ofSize(int pageSize) {
        return new Page(0, pageSize);
    }

    public static Page of(int page, int pageSize) {
        return new Page(page, pageSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Page)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Page page1 = (Page) o;
        return page == page1.page;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), page);
    }
}
