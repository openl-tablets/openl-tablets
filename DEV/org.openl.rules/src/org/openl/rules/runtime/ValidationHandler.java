package org.openl.rules.runtime;

import java.lang.reflect.Array;

import org.openl.binding.impl.cast.OutsideOfValidDomainException;
import org.openl.domain.IDomain;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DomainOpenClass;
import org.openl.util.DomainUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * Finds alias values inside argument objects and checks if its value belongs to the corresponding {@code IDomain}
 *
 * @author Vladyslav Pikus
 */
class ValidationHandler {

    void validateProxyArguments(IMethodSignature methodSignature, IRuntimeEnv env, Object[] args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                doValidate(env, args[i], methodSignature.getParameterType(i));
            }
        }
    }

    private void doValidate(IRuntimeEnv env, Object obj, IOpenClass type) {
        if (obj != null) {
            if (type.isArray()) {
                doValidateArray(env, obj, type);
            } else if (type instanceof DomainOpenClass) {
                validateAliasValue(obj, (DomainOpenClass) type);
            } else if (type instanceof DatatypeOpenClass) {
                doValidateDatatype(env, obj, (DatatypeOpenClass) type);
            }
        }
    }

    private void doValidateArray(IRuntimeEnv env, Object objs, IOpenClass openType) {
        if (objs != null) {
            int length = Array.getLength(objs);
            for (int i = 0; i < length; i++) {
                Object obj = Array.get(objs, i);
                doValidate(env, obj, openType.getComponentClass());
            }
        }
    }

    private void doValidateDatatype(IRuntimeEnv env, Object obj, DatatypeOpenClass datatypeOpenClass) {
        if (obj.getClass().equals(datatypeOpenClass.getInstanceClass())) {
            for (IOpenField openField : datatypeOpenClass.getFields()) {
                Object value = openField.get(obj, env);
                doValidate(env, value, openField.getType());
            }
        }
    }

    private void validateAliasValue(Object value, DomainOpenClass domainOpenClass) {
        IDomain domain = domainOpenClass.getDomain();
        boolean isInDomain = domain.selectObject(value);
        if (!isInDomain) {
            throw new OutsideOfValidDomainException(
                String.format("Object '%s' is outside of valid domain '%s'. Valid values: %s",
                    value,
                    domainOpenClass.getName(),
                    DomainUtils.toString(domain)));
        }
    }
}
