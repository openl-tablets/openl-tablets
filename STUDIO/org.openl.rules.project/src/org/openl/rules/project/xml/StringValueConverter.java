package org.openl.rules.project.xml;

import com.thoughtworks.xstream.converters.SingleValueConverter;

public class StringValueConverter implements SingleValueConverter {

    public String toString(Object obj) {
        return ((String) obj).trim();
    }

    public Object fromString(String name) {
        return name.trim();
    }

    public boolean canConvert(Class type) {
        return type.equals(String.class);
    }

}