package org.openl.rules.project.xml;

import com.thoughtworks.xstream.converters.SingleValueConverter;

public class StringValueConverter implements SingleValueConverter {

    @Override
    public String toString(Object obj) {
        return ((String) obj).trim();
    }

    @Override
    public Object fromString(String name) {
        return name.trim();
    }

    @Override
    public boolean canConvert(Class type) {
        return type.equals(String.class);
    }

}