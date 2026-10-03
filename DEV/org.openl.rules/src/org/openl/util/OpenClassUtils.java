package org.openl.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.openl.binding.ICastFactory;
import org.openl.domain.EnumDomain;
import org.openl.domain.IDomain;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
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
        var dim = 0;
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
     * @return wrapper class for the provided primitive class or provided object as input parameter
     */
    public static IOpenClass toWrapperIfPrimitive(IOpenClass openClass) {
        if (isPrimitive(openClass)) {
            return JavaOpenClass.getOpenClass(ClassUtils.primitiveToWrapper(openClass.getInstanceClass()));
        }
        return openClass;
    }

    /**
     * Returns the closest common class of the classes, as the element class of an array that holds a value of each.
     * <p>
     * The class is primitive only when every class is primitive. A wrapper, another class or an empty value among
     * them makes it a wrapper, so that an empty element keeps its place: {@code int} and {@code double} give
     * {@code double}, while {@code Integer} and {@code double} give {@code Double}.
     *
     * @param castFactory the casts that give the closest class of two classes
     * @param classes     the classes of the values, {@link NullOpenClass} for an empty value
     * @return the element class, or {@code null} when no class is given or every value is empty
     */
    public static @Nullable IOpenClass findClosestElementClass(ICastFactory castFactory, Iterable<IOpenClass> classes) {
        IOpenClass closest = null;
        var primitive = true;
        for (var openClass : classes) {
            closest = closest == null ? openClass : castFactory.findClosestClass(closest, openClass);
            primitive &= isPrimitive(openClass);
        }
        if (closest == null || NullOpenClass.isAnyNull(closest)) {
            return null;
        }
        return primitive ? closest : toWrapperIfPrimitive(closest);
    }

    private static boolean isPrimitive(IOpenClass openClass) {
        return openClass.getInstanceClass() != null && openClass.getInstanceClass().isPrimitive();
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
        if (value == null) {
            return null;
        }
        String validationMessage = null;
        if (value.getClass().isArray()) {
            var length = Array.getLength(value);
            for (var i = 0; i < length && validationMessage == null; i++) {
                var element = Array.get(value, i);
                validationMessage = validateDomain(element, domain, paramType);
            }
        } else if (value instanceof Iterable<?> list && !(value instanceof org.openl.rules.helpers.INumberRange)) {
            for (var iterator = list.iterator(); iterator.hasNext() && validationMessage == null; ) {
                var element = iterator.next();
                validationMessage = validateDomain(element, domain, paramType);
            }
        } else {
            validationMessage = validateDomainValue(value, domain, paramType);
        }
        return validationMessage;
    }

    private static String validateDomainValue(Object value, IDomain<Object> domain, IOpenClass paramType) {
        // block is surrounded by try block, as EnumDomain
        // implementation throws a
        // RuntimeException when value doesn`t belong to domain.
        //
        boolean contains;
        if (domain instanceof EnumDomain<?> enumDomain) {
            contains = belongsToEnum(enumDomain.getAllObjects(), value.toString());
        } else {
            contains = domain.selectObject(value);
        }

        if (!contains) {
            return "The value '%s' is outside of valid domain '%s'. Valid values: %s".formatted(
                    value,
                    paramType.getName(),
                    DomainUtils.toString(domain));
        }
        return null;
    }


    public static boolean belongsToEnum(Object[] arrayEnum, String inputKey) {
        for (Object entry : arrayEnum) {
            if (entry == null) {
                continue;
            }
            String generatedEnumKey;
            if (entry instanceof Object[] arrayEntry) {
                generatedEnumKey = generateKey(arrayEntry);
            } else {
                generatedEnumKey = entry.toString();
            }
            if (inputKey.equals(generatedEnumKey)) {
                return true;
            }
        }
        return false;
    }

    public static String generateKey(Object[] array) {
        return Arrays.stream(array)
                .filter(Objects::nonNull)
                .map(Object::toString)  // Convert each element to a string
                .collect(Collectors.joining(","));
    }
}
