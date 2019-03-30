package org.openl.rules.runtime;

import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.domain.IDomain;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.ComponentTypeArrayOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.util.DomainUtils;
import org.openl.vm.IRuntimeEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds alias values inside argument objects and checks if its value belongs to the corresponding {@code IDomain}
 *
 * @author Vladyslav Pikus
 */
class ValidationHandler {

    void validateProxyArguments(IMethodSignature methodSignature, IRuntimeEnv env, Object[] args) {
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
                doValidate(env, (Object[]) arg, generatedType.getComponentClass());
            } else if (generatedType instanceof DatatypeOpenClass) {
                doValidate(env, arg, generatedType);
            }
        }
    }

    private void doValidate(IRuntimeEnv env, Object[] objs, IOpenClass openType) {
        for (Object obj : objs) {
            if (obj == null) {
                continue;
            }
            if (openType instanceof ComponentTypeArrayOpenClass) {
                doValidate(env, (Object[]) obj, openType.getComponentClass());
            } else if (openType instanceof DatatypeOpenClass) {
                doValidate(env, obj, openType);
            }
        }
    }

    private void doValidate(IRuntimeEnv env, Object obj, IOpenClass openType) {
        if (!obj.getClass().equals(openType.getInstanceClass())) {
            return;
        }
        for (IOpenField openField : openType.getFields().values()) {
            IOpenClass openClass = openField.getType();
            if (openClass instanceof ComponentTypeArrayOpenClass) {
                Object value = openField.get(obj, env);
                if (value == null) {
                    continue;
                }
                doValidate(env, (Object[]) value, openClass.getComponentClass());
            } else if (openClass instanceof DatatypeOpenClass) {
                Object value = openField.get(obj, env);
                if (value == null) {
                    continue;
                }
                doValidate(env, value, openClass);
            } else if (openClass instanceof DomainOpenClass) {
                Object value = openField.get(obj, env);
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

}
