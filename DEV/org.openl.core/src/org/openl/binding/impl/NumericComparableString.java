package org.openl.binding.impl;

import org.openl.util.StringPool;

import javax.xml.bind.annotation.XmlRootElement;
import java.math.BigInteger;
import java.util.Objects;

@XmlRootElement
public class NumericComparableString implements Comparable<NumericComparableString> {

    private final String value;

    public static NumericComparableString valueOf(String value) {
        if (value == null) {
            return null;
        }
        return new NumericComparableString(value);
    }

    public static NumericComparableString valueOf(CharSequence value) {
        if (value == null) {
            return null;
        }
        return new NumericComparableString(value.toString());
    }

    private NumericComparableString(String value) {
        this.value = Objects.requireNonNull(value, "Error initializing StringRangeValue class. Parameter 'value' cannot be null");
    }

    @Override
    public int compareTo(NumericComparableString v) {
        return NumericStringComparator.INSTANCE.compare(value, v.value);
    }

    public String getValue() {
        return value;
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
        return this.compareTo((NumericComparableString) obj) == 0;
    }

    @Override
    public int hashCode() {
        return NumericStringComparator.hashCode(value);
    }

    @Override
    public String toString() {
        return getValue();
    }

    public NumericComparableString incrementAndGet() {
        return valueOf(increment(value));
    }

    public static String increment(String value) {
        if (value.isEmpty()) {
            return "\u0000";
        }
        String result;
        int i = value.length() - 1;
        char lastChar = value.charAt(i);
        while (i > 0 && Character.digit(lastChar, 10) == 9) {
            // searching the position where a trailing number can be increased
            i--;
            lastChar = value.charAt(i);
        }
        if (i == value.length() - 1) {
            if (Character.isDigit(lastChar)) {
                // "abc08" => "abc0" + (8+1) => "abc09"
                // "9" => "" + (9+1) => "10"
                result = value.substring(0, i) + (Character.digit(lastChar, 10) + 1);
            } else {
                // "abc" => "abc\u0000"
                result = value + Character.MIN_VALUE;
            }
        } else {
            if (Character.isDigit(lastChar)) {
                // "789" => "7" + (89+1) => "790"
                // "99" => "" + (99+1) => "100"
                result = value.substring(0, i) + new BigInteger(value.substring(i)).add(BigInteger.ONE);
            } else {
                // "abc99" => "abc" + (99+1) => "abc100"
                result = value.substring(0, i + 1) + new BigInteger(value.substring(i + 1)).add(BigInteger.ONE);
            }
        }
        return StringPool.intern(result);
    }
}
