package org.openl.rules.runtime;

import java.lang.reflect.Array;

import org.jspecify.annotations.Nullable;

import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.domain.IDomain;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.DomainUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * Checks that argument values belong to their declared vocabulary domains.
 *
 * <p>Checks vocabulary parameters and fields of OpenL datatypes, including nested objects and array elements.
 * Null values are accepted. Values without a vocabulary domain are left unchanged.
 *
 * <p>A datatype subclass is checked with its own fields when the module knows it, including a subclass declared in
 * a dependent module. A bean of the same datatype from another class loader is checked by its readable properties;
 * a field it does not have is not checked.
 */
public final class ValidationHandler {

    private ValidationHandler() {
    }

    /**
     * Checks the arguments of a rule before it runs.
     *
     * @param module the module that runs the rule, used to find datatype subclasses; {@code null} to use only the
     *               module of each declared datatype
     * @throws OutsideOfValidDomainException when an argument or a datatype field is outside its vocabulary domain
     */
    public static void validateArguments(IMethodSignature methodSignature,
                                         @Nullable IOpenClass module,
                                         IRuntimeEnv env,
                                         @Nullable Object @Nullable [] args) {
        if (args != null) {
            // An argument the signature does not declare has no vocabulary to check.
            var count = Math.min(args.length, methodSignature.getNumberOfParameters());
            for (int i = 0; i < count; i++) {
                doValidate(env, module, args[i], methodSignature.getParameterType(i));
            }
        }
    }

    private static void doValidate(IRuntimeEnv env,
                                   @Nullable IOpenClass module,
                                   @Nullable Object obj,
                                   IOpenClass type) {
        if (obj != null) {
            if (type.isArray()) {
                doValidateArray(env, module, obj, type);
            } else if (type instanceof DomainOpenClass class2) {
                validateAliasValue(obj, class2);
            } else if (type instanceof DatatypeOpenClass class1) {
                doValidateDatatype(env, module, obj, class1);
            }
        }
    }

    private static void doValidateArray(IRuntimeEnv env,
                                        @Nullable IOpenClass module,
                                        Object objs,
                                        IOpenClass openType) {
        int length = Array.getLength(objs);
        for (int i = 0; i < length; i++) {
            doValidate(env, module, Array.get(objs, i), openType.getComponentClass());
        }
    }

    private static void doValidateDatatype(IRuntimeEnv env,
                                           @Nullable IOpenClass module,
                                           Object obj,
                                           DatatypeOpenClass datatypeOpenClass) {
        if (!isDatatypeValue(datatypeOpenClass.getInstanceClass(), obj)) {
            return;
        }
        var actualType = actualDatatype(datatypeOpenClass, module, obj);
        // A bean from another class loader is read by its own properties.
        var beanClass = actualType.getInstanceClass().isInstance(obj)
                ? null
                : JavaOpenClass.getOpenClass(obj.getClass());
        for (IOpenField openField : actualType.getFields()) {
            var reader = beanClass == null ? openField : beanField(beanClass, openField.getName());
            if (reader != null) {
                doValidate(env, module, reader.get(obj, env), openField.getType());
            }
        }
    }

    /** A value of the datatype or of its subclass, loaded by this or by another class loader. */
    private static boolean isDatatypeValue(Class<?> expectedClass, Object value) {
        if (expectedClass.isInstance(value)) {
            return true;
        }
        try {
            return Class.forName(expectedClass.getName(), false, value.getClass().getClassLoader()).isInstance(value);
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    private static DatatypeOpenClass actualDatatype(DatatypeOpenClass declaredType,
                                                    @Nullable IOpenClass module,
                                                    Object value) {
        var className = value.getClass().getName();
        if (declaredType.getInstanceClass().getName().equals(className)) {
            return declaredType;
        }
        for (var scope : new @Nullable IOpenClass[]{module, declaredType.getModule()}) {
            if (scope != null && scope.findType(className) instanceof DatatypeOpenClass type
                    && type.getInstanceClass().getName().equals(className)) {
                return type;
            }
        }
        return declaredType;
    }

    /** Finds a bean property by a datatype field name, ignoring the case of the name. */
    private static @Nullable IOpenField beanField(IOpenClass beanClass, String name) {
        try {
            return beanClass.getField(name, false);
        } catch (AmbiguousFieldException e) {
            return null;
        }
    }

    private static void validateAliasValue(Object value, DomainOpenClass domainOpenClass) {
        IDomain domain = domainOpenClass.getDomain();
        boolean isInDomain = domain.selectObject(value);
        if (!isInDomain) {
            throw new OutsideOfValidDomainException(value, domainOpenClass.getName(), DomainUtils.toString(domain));
        }
    }
}
