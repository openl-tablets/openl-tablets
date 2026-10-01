package org.openl.rules.repository.api;

import java.util.Objects;


/**
 * An implementation of pagination that requires {@code offset} and {@code size} parameters. Page number is
 * auto-calculated from {@code offset / size}. If vanilla pagination is required, {@link Page} implementation can be
 * used
 *
 * @author Vladyslav Pikus
 * @see Page
 */
public class Offset extends Pageable {

    private final int start;

    public Offset(int offset, int size) {
        super(size);
        if (offset < 0) {
            throw new IllegalArgumentException("Page offset must be greater or equal 0.");
        }
        this.start = offset;
    }

    @Override
    public int getOffset() {
        return start;
    }

    @Override
    public int getPageNumber() {
        return start / getPageSize();
    }

    public static Offset of(int offset, int pageSize) {
        return new Offset(offset, pageSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Offset offset1)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        return start == offset1.start;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), start);
    }
}
