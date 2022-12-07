package org.openl.rules.range;

import java.text.ParseException;
import java.util.Objects;

/**
 * Base class of ranges with utility methods.
 *
 * @param <T> type of range
 * @author Yury Molchan
 */
public abstract class Range<T> {

    public enum Bound {
        OPEN, // Not inclusive
        CLOSED, // Inclusive
        UNBOUND // Infinity
    }

    public enum Type {
        OPEN(Bound.OPEN, Bound.OPEN), // (x; y)
        CLOSED(Bound.CLOSED, Bound.CLOSED), // [x; y]

        OPEN_CLOSED(Bound.OPEN, Bound.CLOSED), // (x; y]
        CLOSED_OPEN(Bound.CLOSED, Bound.OPEN), // [x; y)

        LEFT_OPEN(Bound.OPEN, Bound.UNBOUND), // (x; ∞)
        LEFT_CLOSED(Bound.CLOSED, Bound.UNBOUND), // [x; ∞)
        RIGHT_OPEN(Bound.UNBOUND, Bound.OPEN), // (-∞; y)
        RIGHT_CLOSED(Bound.UNBOUND, Bound.CLOSED), // (-∞; y]

        DEGENERATE(Bound.CLOSED, Bound.CLOSED); // [x; x]


        public final Bound left, right;

        Type(Bound left, Bound right) {
            this.left = left;
            this.right = right;
        }
    }

    protected RangeParser parse(String text) {
        try {
            return RangeParser.parse(text);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Cannot parse a range", e);
        }
    }

    protected void validate() {
        T left = getLeft();
        T right = getRight();
        if (left != null && right != null && compare(left, right) > 0) {
            var sb = new StringBuilder(96);
            sb.append("The right bound '");
            format(sb, right);
            sb.append("' must be greater or equal than the left bound '");
            format(sb, left);
            sb.append("'.");
            throw new IllegalArgumentException(sb.toString());
        }
        Objects.requireNonNull(getType());
    }

    protected abstract Type getType();

    protected abstract T getLeft();

    protected abstract T getRight();

    protected abstract int compare(T left, T right);

    protected void format(StringBuilder sb, T value) {
        sb.append(value);
    }

    /**
     * WARNING:
     * Due limitation of OpenL which cannot work correctly with class level generics, so it is declared as protected.
     * It needs to define public method in children classes with type of generic parameter.
     * OpenL can correctly select required method or show an error.
     *
     * @param value value to check it is in the range (interval)
     * @return true if the value is in the interval
     */
    protected boolean contains(T value) {
        if (value == null) {
            return false;
        }
        Type type = getType();
        if (type.left != Bound.UNBOUND) {
            int comparison = compare(getLeft(), value);
            if (comparison > 0 || comparison == 0 && type.left == Bound.OPEN) {
                // less than the left bound
                return false;
            }
        }
        if (type.right != Bound.UNBOUND) {
            int comparison = compare(value, getRight());
            if (comparison > 0 || comparison == 0 && type.right == Bound.OPEN) {
                // greater than the right bound
                return false;
            }
        }

        return true;
    }

    public boolean contains(Range<T> range) {
        if (this == range) {
            return true;
        }
        if (range == null) {
            return false;
        }
        if (getClass() != range.getClass()) {
            return false;
        }

        return compareLeft(range) <= 0 && compareRight(range) >= 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Range<?> other = (Range<?>) obj;

        return getType().left == other.getType().left &&
                getType().right == other.getType().right &&
                Objects.equals(getLeft(), other.getLeft()) &&
                Objects.equals(getRight(), other.getRight());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType().left, getType().right, getLeft(), getRight());
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(20);
        Type type = getType();
        switch (type) {
            case OPEN:
            case CLOSED:
            case OPEN_CLOSED:
            case CLOSED_OPEN:
                s.append(type.left == Bound.OPEN ? '(' : '[');
                format(s, getLeft());
                s.append("; ");
                format(s, getRight());
                s.append(type.right == Bound.OPEN ? ')' : ']');
                break;
            case LEFT_OPEN:
                s.append("> ");
                format(s, getLeft());
                break;
            case LEFT_CLOSED:
                s.append(">= ");
                format(s, getLeft());
                break;
            case RIGHT_OPEN:
                s.append("< ");
                format(s, getRight());
                break;
            case RIGHT_CLOSED:
                s.append("<= ");
                format(s, getRight());
                break;
            case DEGENERATE:
                format(s, getLeft());
                break;
            default:
                s.append(type);
        }

        return s.toString();
    }


    private int compareLeft(Range<T> range) {
        Type type = getType();
        Type otherType = range.getType();
        if (type.left != Bound.UNBOUND) {
            if (otherType.left == Bound.UNBOUND) {
                // -Infinity is always less than any bound
                return 1;
            }
            int comparison = compare(getLeft(), range.getLeft());
            if (comparison == 0 && type.left == otherType.left) {
                return 0;
            } else {
                // Open left bound is greater than closed.
                return type.left == Bound.OPEN ? 1 : -1;
            }
        }
        // -Infinity is always less than any bound
        return otherType.left != Bound.UNBOUND ? -1 : 0;
    }


    private int compareRight(Range<T> range) {
        Type type = getType();
        Type otherType = range.getType();
        if (type.right != Bound.UNBOUND) {
            if (otherType.right == Bound.UNBOUND) {
                // Infinity is always greater than any bound
                return -1;
            }
            int comparison = compare(getRight(), range.getRight());
            if (comparison == 0 && type.right == otherType.right) {
                return 0;
            } else {
                // Open right bound is less than closed.
                return type.right == Bound.OPEN ? -1 : 1;
            }
        }
        // Infinity is always greater than any bound
        return otherType.right != Bound.UNBOUND ? 1 : 0;
    }
}
