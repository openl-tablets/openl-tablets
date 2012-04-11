package org.openl.meta;

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;

public class StringValue implements IMetaHolder, CharSequence, Comparable<StringValue> {
    private static final Log LOG = LogFactory.getLog(StringValue.class);
    private ValueMetaInfo metaInfo;
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

    public ValueMetaInfo getMetaInfo() {
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
        if(metaInfo instanceof ValueMetaInfo){
            setMetaInfo((ValueMetaInfo)metaInfo);
        }else{
            try {
                ValueMetaInfo valueMetaInfo = new ValueMetaInfo(metaInfo.getDisplayName(IMetaInfo.SHORT),
                        metaInfo.getDisplayName(IMetaInfo.LONG), new URLSourceCodeModule(new URL(metaInfo.getSourceUrl())));
                setMetaInfo(valueMetaInfo);
            } catch (Exception e) {
                LOG.debug(String.format("Failed to set meta info for StringValue \"%s\"", value), e);
                setMetaInfo((ValueMetaInfo)null);
            }
        }
    }

    public void setMetaInfo(ValueMetaInfo metaInfo) {
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
