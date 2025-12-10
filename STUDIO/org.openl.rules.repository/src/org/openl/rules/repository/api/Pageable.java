package org.openl.rules.repository.api;

import java.util.Objects;

/**
 * Abstract pagination implementation
 *
 * @author Vladyslav Pikus
 * @see Offset
 * @see Page
 */
public abstract class Pageable {

    private static final Pageable UNPAGED = new Pageable(1) {
        @Override
        public boolean isUnpaged() {
            return true;
        }

        @Override
        public int getPageNumber() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getPageSize() {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getOffset() {
            return 0;
        }
    };

    private final int size;

    public Pageable(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be greater than 0.");
        }
        this.size = size;
    }

    /**
     * Returns the page to be returned.
     *
     * @return page number
     */
    public abstract int getPageNumber();

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
    public abstract int getOffset();

    /**
     * Returns whether the current Page does not contain pagination information.
     *
     * @return true or false
     */
    public boolean isUnpaged() {
        return false;
    }

    /**
     * Returns a {@link Page} instance representing no pagination setup.
     *
     * @return unpageable
     */
    public static Pageable unpaged() {
        return UNPAGED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Pageable pageable = (Pageable) o;
        return size == pageable.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(size);
    }
}
