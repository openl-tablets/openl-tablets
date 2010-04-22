package org.openl.codegen.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.codegen.tools.type.EnumerationDescriptor;
import org.openl.rules.enumeration.properties.EnumPropertyDefinition;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class EnumHelper {

    public static String getEnumName(String sourceName) {

        return String.format("%s%sEnum", sourceName.substring(0, 1).toUpperCase(), sourceName.substring(1));
    }

    public static List<IOpenField> findEnumerationFields(IOpenClass openClass) {

        List<IOpenField> enumerations = new ArrayList<IOpenField>();
        Iterator<IOpenField> iterator = openClass.fields();

        while (iterator.hasNext()) {
            IOpenField field = iterator.next();

            if (isEnumeration(field)) {
                enumerations.add(field);
            }
        }

        return enumerations;
    }

    public static boolean isEnumeration(IOpenField field) {

        IOpenClass type = field.getType();
        Class<?> clazz = type.getInstanceClass();

        return clazz.equals(EnumPropertyDefinition[].class);
    }

    public static EnumerationDescriptor createDescriptor(String enumName, EnumPropertyDefinition[] values) {

        EnumerationDescriptor descriptor = new EnumerationDescriptor();
        descriptor.setEnumName(enumName);
        descriptor.setValues(values);

        return descriptor;
    }
    
}
