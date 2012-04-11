package org.openl.meta;

import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;

public class StringValue implements IMetaHolder, CharSequence, Comparable<StringValue> {
    private IMetaInfo metaInfo;
    private String value;

    public StringValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Error initializing StringValue class. Parameter \"value\" can't be null.");
        }
        this.value = value;
        metaInfo = new ValueMetaInfo();
    }

    public StringValue(String value, String shortName, String fullName, IOpenSourceCodeModule source) {
        if (value == null) {
            throw new IllegalArgumentException("Error initializing StringValue class. Parameter \"value\" can't be null.");
        }
        this.value = value;
        metaInfo = new ValueMetaInfo(shortName, fullName, source);
    }

    public IOpenSourceCodeModule asSourceCodeModule() {
        return new StringSourceCodeModule(value, getMetaInfo().getSourceUrl());
    }

    public char charAt(int index) {
        return value.charAt(index);
    }

    public int compareTo(StringValue v) {
        return value.compareTo(v.value);
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof StringValue) {
            StringValue v = (StringValue) obj;
            return value.equals(v.value);
        }
        if (obj instanceof String) {
            String s = (String) obj;
            return value.equals(s);
        }

        return false;
    }

    public IMetaInfo getMetaInfo() {
        return metaInfo;
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public boolean isEmpty() {
        return value.trim().length() == 0;
    }

    public int length() {
        return value.length();
    }

    public void setMetaInfo(IMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end);
    }

    @Override
    public String toString() {
        return value;
    }

}
