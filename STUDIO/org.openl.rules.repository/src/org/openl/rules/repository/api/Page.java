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

    private final int pageNumber;

    private Page(int page, int size) {
        super(size);
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be greater or equal 0.");
        }
        this.pageNumber = page;
    }

    /**
     * Returns the page to be returned.
     *
     * @return page number
     */
    @Override
    public int getPageNumber() {
        return pageNumber;
    }

    /**
     * Returns the offset to be taken according to the underlying page and page size.
     *
     * @return data offcet
     */
    @Override
    public int getOffset() {
        return pageNumber * getPageSize();
    }

    /**
     * Returns whether the current Page does not contain pagination information.
     *
     * @return true or false
     */
    @Override
    public boolean isUnpaged() {
        return false;
    }

    public static Page of(int page, int pageSize) {
        return new Page(page, pageSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Page page1)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return pageNumber == page1.pageNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), pageNumber);
    }
}
