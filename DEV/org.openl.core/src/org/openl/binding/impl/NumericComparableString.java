package org.openl.binding.impl;

import java.math.BigInteger;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NumericComparableString implements Comparable<NumericComparableString> {
    private static final Pattern NUMBERS = Pattern.compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

    private final String[] splits;
    private final BigInteger[] splitsNumbers;
    private final String value;

    public static NumericComparableString valueOf(String value) {
        return new NumericComparableString(value);
    }

    public static NumericComparableString valueOf(CharSequence value) {
        return new NumericComparableString(value.toString());
    }

    private void initSplitsNumbers() {
        int i = 0;
        for (String s : splits) {
            if (s.charAt(0) >= '0' && s.charAt(0) <= '9') {
                splitsNumbers[i] = new BigInteger(s);
            }
            i++;
        }
    }

    private NumericComparableString(String value) {
        this.value = Objects.requireNonNull(value,
            "Error initializing StringRangeValue class. Parameter \"value\" can't be null.");
        splits = NUMBERS.split(value);
        splitsNumbers = new BigInteger[splits.length];
        initSplitsNumbers();
    }

    @Override
    public int compareTo(NumericComparableString v) {
        int length = Math.min(splits.length, v.splits.length);

        for (int i = 0; i < length; i++) {
            int cmp = 0;
            if (splitsNumbers[i] != null && v.splitsNumbers[i] != null) {
                cmp = splitsNumbers[i].compareTo(v.splitsNumbers[i]);
            }
            if (cmp == 0) {
                cmp = splits[i].compareTo(v.splits[i]);
            }
            if (cmp != 0) {
                return cmp;
            }
        }
        return splits.length - v.splits.length;
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
        NumericComparableString other = (NumericComparableString) obj;
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (this.compareTo(other) != 0) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return getValue().toString();
    }
}
