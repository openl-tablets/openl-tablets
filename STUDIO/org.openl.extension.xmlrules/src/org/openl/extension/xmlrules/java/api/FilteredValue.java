package org.openl.extension.xmlrules.java.api;

public class FilteredValue {
    private Object value;

    public FilteredValue() {

    }

    public FilteredValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
