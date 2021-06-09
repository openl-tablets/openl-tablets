package org.openl.codegen.tools;

import static java.lang.reflect.Modifier.isPublic;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.properties.expressions.match.MatchingExpression;
import org.openl.types.java.JavaOpenClass;

/**
 * Class used by Velocity engine as external tools.
 */
public final class VelocityTool {

    private static final Class<?>[] EMPTY = new Class<?>[] {};
    private static final Class<?>[] STRING = new Class<?>[] { String.class };
    private static final Class<?>[] STRING_STRING = new Class<?>[] { String.class, String.class };

    public String getTypeName(Class<?> clazz) {

        if (clazz.isArray()) {
            return String.format("%s[]", clazz.getComponentType().getName());
        }
        return clazz.getName();
    }

    public String getVarArgTypeName(Class<?> clazz) {

        if (clazz.isArray()) {
            return String.format("%s...", clazz.getComponentType().getName());
        }
        return clazz.getName();
    }

    public boolean hasConstructorWithoutParams(Class<?> clazz) {
        return hasPublicConstructor(clazz, EMPTY);
    }

    public boolean hasConstructorWithPropertyName(Class<?> clazz) {
        return hasPublicConstructor(clazz, STRING);
    }

    public boolean hasConstructorWithConstraintForProperty(Class<?> clazz) {
        return hasPublicConstructor(clazz, STRING_STRING);
    }

    private static boolean hasPublicConstructor(Class<?> clazz, Class<?>[] types) {
        try {
            return isPublic(clazz.getModifiers()) && isPublic(clazz.getConstructor(types).getModifiers());
        } catch (Exception e) {
            return false;
        }
    }

    public String formatAccessorName(String name) {

        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    public List<PropertyDescriptor> getPropertyDescriptors(Object bean) throws Exception {
        BeanInfo info;

        info = Introspector.getBeanInfo(bean.getClass());

        PropertyDescriptor[] pdd = info.getPropertyDescriptors();
        List<PropertyDescriptor> pdlist = new ArrayList<>();

        for (PropertyDescriptor pd : pdd) {
            if (pd.getWriteMethod() == null || pd.getReadMethod() == null) {
                continue;
            }

            pdlist.add(pd);
        }
        return pdlist;
    }

    public Object value(Object bean, PropertyDescriptor propertyDescriptor) throws Exception {
        Object value;

        value = propertyDescriptor.getReadMethod().invoke(bean);
        value = value == null ? null : value(value);
        return value;
    }

    private String value(Object value) {

        if (value == null) {
            return "null";
        }

        Class<?> c = value.getClass();

        if (c.isArray()) {
            StringBuilder sb = new StringBuilder();
            sb.append("new ").append(c.getSimpleName()).append(" { ");

            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(value(Array.get(value, i)));
            }

            return sb.append(" }").toString();
        } else if (value instanceof String) {
            return "\"" + value + "\"";
        } else if (c.isEnum()) {
            return c.getSimpleName() + '.' + ((Enum) value).name();
        } else if (value instanceof JavaOpenClass) {
            return "org.openl.types.java.JavaOpenClass.getOpenClass(" + value + ".class)";
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Constraints) {
            return "new Constraints(\"" + ((Constraints) value).getConstraintsStr() + "\")";
        } else if (value instanceof MatchingExpression) {
            return "new MatchingExpression(\"" + ((MatchingExpression) value).getMatchExpressionStr() + "\")";
        }

        throw new RuntimeException("Cannot process literal class: " + c.getName());
    }
}
