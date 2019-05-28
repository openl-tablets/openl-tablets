package org.openl.rules.helpers;

import java.math.BigInteger;
import java.util.regex.Pattern;

public class StringRangeValue implements Comparable<StringRangeValue> {
    private static final Pattern NUMBERS = Pattern.compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

    private final String[] splits;
    private final BigInteger[] splitsNumbers;
    private final String value;

    public static StringRangeValue valueOf(String value) {
        return new StringRangeValue(value);
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

    private StringRangeValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException(
                "Error initializing StringRangeValue class. Parameter \"value\" can't be null.");
        }
        this.value = value;
        splits = NUMBERS.split(value);
        splitsNumbers = new BigInteger[splits.length];
        initSplitsNumbers();
    }

    @Override
    public int compareTo(StringRangeValue v) {
        int length = Math.min(splits.length, v.splits.length);

        for (int i = 0; i < length; i++) {
            int cmp = 0;
            if (splitsNumbers[i] != null && v.splitsNumbers[i] != null) {
                cmp = splitsNumbers[i].compareTo(v.splitsNumbers[i]);
            }
            if (cmp == 0) {
                cmp = splits[i].compareTo(v.splits[i]);
            }
            if (cmp != 0)
                return cmp;
        }
        return splits.length - v.splits.length;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue().toString();
    }
}
