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

    private final int offset;

    public Offset(int offset, int size) {
        super(size);
        if (offset < 0) {
            throw new IllegalArgumentException("Page offset must be greater or equal 0.");
        }
        this.offset = offset;
    }

    @Override
    public int getPageNumber() {
        return offset / getPageSize();
    }

    @Override
    public int getOffset() {
        return offset;
    }

    public static Offset of(int offset, int pageSize) {
        return new Offset(offset, pageSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Offset)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Offset offset1 = (Offset) o;
        return offset == offset1.offset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), offset);
    }
}