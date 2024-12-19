package org.openl.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public final class OpenClassUtils {

    private OpenClassUtils() {
    }

    public static IOpenClass getRootComponentClass(IOpenClass fieldType) {
        if (!fieldType.isArray()) {
            return fieldType;
        }
        // Get the component type of the array
        //
        return getRootComponentClass(fieldType.getComponentClass());
    }

    /**
     * The method calculates the dimension of the given type.
     *
     * @param type The type to calculate the dimension. If the parameter is null, the method fails with NPE.
     * @return the dimension
     */
    public static int getDimension(IOpenClass type) {
        int dim = 0;
        while (type.isArray()) {
            type = type.getComponentClass();
            dim++;
        }
        return dim;
    }

    /**
     * If provided open class is a primitive then the method returns wrapper class for the provided primitive class,
     * otherwise returns provided object as input parameter.
     *
     * @param openClass the open class
     * @return
     */
    public static IOpenClass toWrapperIfPrimitive(IOpenClass openClass) {
        if (openClass.getInstanceClass() != null && openClass.getInstanceClass().isPrimitive()) {
            return JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(openClass.getInstanceClass()));
        }
        return openClass;
    }

    public static boolean isVoid(IOpenClass type) {
        return type == JavaOpenClass.VOID || type == JavaOpenClass.CLS_VOID;
    }

    @SuppressWarnings("unchecked")
    public static String isValidValue(Object value, IOpenClass paramType) {
        var domain = (IDomain<Object>) paramType.getDomain();

        if (domain != null) {
            return validateDomain(value, domain, paramType);
        }
        return null;
    }

    private static String validateDomain(Object value,
                                         IDomain<Object> domain,
                                         IOpenClass paramType) {
        String validationMessage = null;
        if (value == null) {
            return validationMessage;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length && validationMessage == null; i++) {
                var element = Array.get(value, i);
                validationMessage = validateDomain(element, domain, paramType);
            }
        } else if (value instanceof Iterable) {
            var list = (Iterable) value;
            for (var iterator = list.iterator(); iterator.hasNext() && validationMessage == null; ) {
                var element = iterator.next();
                validationMessage = validateDomain(element, domain, paramType);
            }
        } else {
            // block is surrounded by try block, as EnumDomain
            // implementation throws a
            // RuntimeException when value doesn`t belong to domain.
            //
            var contains = true;
            if (domain instanceof EnumDomain) {
                contains = belongsToEnum(((EnumDomain) domain).getAllObjects(), value.toString());
            } else {
                contains = domain.selectObject(value);
            }

            if (!contains) {
                validationMessage = String.format("The value '%s' is outside of valid domain '%s'. Valid values: %s",
                        value,
                        paramType.getName(),
                        DomainUtils.toString(domain));
            }
        }
        return validationMessage;
    }


    public static boolean belongsToEnum(Object[] arrayEnum, String inputKey) {
        String generatedEnumKey = null;
        for (int i = 0; i < arrayEnum.length && !inputKey.equals(generatedEnumKey); i++) {
            if (arrayEnum[i] instanceof Object[]) {
                var arrayExtracted = (Object[]) arrayEnum[i];
                generatedEnumKey = generateKey(arrayExtracted);
            } else {
                generatedEnumKey = arrayEnum[i].toString();
            }
        }
        return inputKey.equals(generatedEnumKey);
    }

    public static String generateKey(Object[] array) {
        return Arrays.stream(array)
                .filter(Objects::nonNull)
                .map(Object::toString)  // Convert each element to a string
                .collect(Collectors.joining(","));
    }
}
