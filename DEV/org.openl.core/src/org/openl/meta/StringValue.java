package org.openl.meta;

import java.net.URL;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openl.meta.StringValue.StringValueAdapter;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.util.ArrayTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlRootElement
@XmlJavaTypeAdapter(StringValueAdapter.class)
public class StringValue implements IMetaHolder, CharSequence, Comparable<StringValue> {
    private final Logger log = LoggerFactory.getLogger(StringValue.class);
    private ValueMetaInfo metaInfo;
    private final String value;

    public static class StringValueAdapter extends XmlAdapter<String, StringValue> {
        @Override
        public StringValue unmarshal(String val) throws Exception {
            return new StringValue(val);
        }

        @Override
        public String marshal(StringValue val) throws Exception {
            return val.getValue();
        }
    }

    public StringValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException(
                "Error initializing StringValue class. Parameter \"value\" can't be null.");
        }
        this.value = value;
        metaInfo = new ValueMetaInfo();
    }

    public StringValue(String value, String shortName, String fullName, IOpenSourceCodeModule source) {
        if (value == null) {
            throw new IllegalArgumentException(
                "Error initializing StringValue class. Parameter \"value\" can't be null.");
        }
        this.value = value;
        metaInfo = new ValueMetaInfo(shortName, fullName, source);
    }

    public IOpenSourceCodeModule asSourceCodeModule() {
        return new StringSourceCodeModule(value, getMetaInfo().getSourceUrl());
    }

    /**
     * Returns a character at position 'index' of current StringValue variable
     */
    @Override
    public char charAt(int index) {
        return value.charAt(index);
    }

    /**
     * Compares StringValue v with current StringValue variable
     */
    @Override
    public int compareTo(StringValue v) {
        return value.compareTo(v.value);
    }

    @Override
    /**
     * Indicates whether some other object is "equal to" this org.openl.meta.IntValue variable.
     */
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

    /**
     * Returns the metainfo of current StringValue variable
     */
    @Override
    public ValueMetaInfo getMetaInfo() {
        return metaInfo;
    }

    /**
     * @return the value of current StringValue variable
     */
    public String getValue() {
        return value;
    }

    @Override
    /**
     * Returns the hash code of the value
     */
    public int hashCode() {
        return value.hashCode();
    }

    /**
     * @return true if value is empty, and false if not
     */
    public boolean isEmpty() {
        return value.trim().length() == 0;
    }

    /**
     * Return the length of the value
     */
    @Override
    public int length() {
        return value.length();
    }

    /**
     * Sets the metainfo for the value
     */
    @Override
    public void setMetaInfo(IMetaInfo metaInfo) {
        if (metaInfo instanceof ValueMetaInfo) {
            setMetaInfo((ValueMetaInfo) metaInfo);
        } else {
            try {
                ValueMetaInfo valueMetaInfo = new ValueMetaInfo(metaInfo.getDisplayName(IMetaInfo.SHORT),
                    metaInfo.getDisplayName(IMetaInfo.LONG),
                    new URLSourceCodeModule(new URL(metaInfo.getSourceUrl())));
                setMetaInfo(valueMetaInfo);
            } catch (Exception e) {
                log.debug("Failed to set meta info for StringValue \"{}\"", value, e);
                setMetaInfo((ValueMetaInfo) null);
            }
        }
    }

    /**
     * Sets the metainfo for the value
     */
    public void setMetaInfo(ValueMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    /**
     * Returns a new character sequence that is a subsequence of this sequence.
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        return value.subSequence(start, end);
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Sorts the StringValue array
     *
     * @param values array for sorting
     * @return the sorted array
     */
    public static StringValue[] sort(StringValue[] values) {
        StringValue[] sortedArray = null;
        if (values != null) {
            sortedArray = new StringValue[values.length];
            StringValue[] notNullArray = ArrayTool.removeNulls(values);

            Arrays.sort(notNullArray);

            /* Filling sortedArray by sorted and null values */
            System.arraycopy(notNullArray, 0, sortedArray, 0, notNullArray.length);
        }
        return sortedArray;
    }

    public static StringValue autocast(String x, StringValue y) {
        return new StringValue(x);
    }

    public static String autocast(StringValue x, String y) {
        return x.getValue();
    }
}
