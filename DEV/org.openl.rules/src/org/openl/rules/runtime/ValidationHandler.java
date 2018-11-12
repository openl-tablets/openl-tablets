package org.openl.rules.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.domain.IDomain;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.ComponentTypeArrayOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.DomainUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds alias values inside argument objects and checks if its value belongs to the corresponding {@code IDomain}
 *
 * @author Vladyslav Pikus
 */
class ValidationHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    void validateProxyArguments(IMethodSignature methodSignature, Object[] args) {
        if (args == null || args.length == 0) {
            return;
        }
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                continue;
            }
            IOpenClass generatedType = methodSignature.getParameterType(i);
            if (generatedType instanceof DomainOpenClass) {
                validateAliasValue(arg, generatedType);
            } else if (generatedType instanceof ComponentTypeArrayOpenClass) {
                doValidate((Object[]) arg, generatedType.getComponentClass());
            } else if (generatedType instanceof DatatypeOpenClass) {
                doValidate(arg, generatedType);
            }
        }
    }

    private void doValidate(Object[] objs, IOpenClass openType) {
        for (Object obj : objs) {
            if (obj == null) {
                continue;
            }
            if (obj.getClass().isArray()) {
                doValidate((Object[]) obj, openType);
            } else {
                doValidate(obj, openType);
            }
        }
    }

    private void doValidate(Object obj, IOpenClass openType) {
        if (!obj.getClass().equals(openType.getInstanceClass())) {
            return;
        }
        for (Map.Entry<String, IOpenField> openField : openType.getFields().entrySet()) {
            IOpenClass openClass = openField.getValue().getType();
            boolean openArrayType = openClass.isArray();
            if (openClass instanceof ComponentTypeArrayOpenClass) {
                openClass = openClass.getComponentClass();
            }
            if (openClass instanceof DatatypeOpenClass) {
                Object value = getValue(obj, openField.getKey());
                if (value == null) {
                    continue;
                }
                if (openArrayType) {
                    doValidate((Object[]) value, openClass);
                } else {
                    doValidate(value, openClass);
                }
            } else if (openClass instanceof DomainOpenClass) {
                Object value = getValue(obj, openField.getKey());
                if (value == null) {
                    continue;
                }
                validateAliasValue(value, openClass);
            }
        }
    }

    private void validateAliasValue(Object value, IOpenClass openClass) {
        if (openClass.isArray()) {
            Object[] values = (Object[]) value;
            for (Object o : values) {
                testLookupValue(o, openClass);
            }
        } else {
            testLookupValue(value, openClass);
        }
    }

    @SuppressWarnings("unchecked")
    private void testLookupValue(Object o, IOpenClass openClass) {
        IDomain domain = openClass.getDomain();
        boolean isInDomain = domain.selectObject(o);
        if (!isInDomain) {
            throw new OutsideOfValidDomainException(
                String.format("Object '%s' is outside of valid domain '%s'. Valid values: %s",
                    o,
                    openClass.getName(),
                    DomainUtils.toString(domain)));
        }
    }

    private Object getValue(Object obj, String propName) {
        Class<?> clazz = obj.getClass();
        String getterName = "get" + ClassUtils.capitalize(propName);
        try {
            Method getMethod = clazz.getMethod(getterName);
            return getMethod.invoke(obj);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            log.error("Cannot get value from {}.{}", clazz.getSimpleName(), propName);
        }
        return null;
    }

}
