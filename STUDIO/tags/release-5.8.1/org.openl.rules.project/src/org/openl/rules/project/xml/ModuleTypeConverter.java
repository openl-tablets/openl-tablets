package org.openl.rules.project.xml;

import org.openl.rules.project.model.ModuleType;

import com.thoughtworks.xstream.converters.SingleValueConverter;

public class ModuleTypeConverter implements SingleValueConverter {

    public String toString(Object obj) {
        return ((ModuleType) obj).toString().toLowerCase();
    }

    public Object fromString(String name) {
        return ModuleType.valueOf(name.toUpperCase());
    }

    public boolean canConvert(Class type) {
        return type.equals(ModuleType.class);
    }

}