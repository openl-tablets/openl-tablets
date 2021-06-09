package org.openl.rules.project.xml.v5_11;

import org.openl.rules.project.model.v5_11.ModuleType_v5_11;

import com.thoughtworks.xstream.converters.SingleValueConverter;

public class ModuleTypeConverter_v5_11 implements SingleValueConverter {

    @Override
    public String toString(Object obj) {
        return obj.toString().toLowerCase();
    }

    @Override
    public Object fromString(String name) {
        return ModuleType_v5_11.valueOf(name.toUpperCase());
    }

    @Override
    public boolean canConvert(Class type) {
        return type == ModuleType_v5_11.class;
    }

}