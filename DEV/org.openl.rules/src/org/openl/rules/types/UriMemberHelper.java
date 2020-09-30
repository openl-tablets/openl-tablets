package org.openl.rules.types;

import java.util.ArrayList;
import java.util.List;

import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.types.IModuleInfo;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

public final class UriMemberHelper {

    private UriMemberHelper() {
    }

    private static boolean isNotTheSame(IUriMember uriMember1, IUriMember uriMember2) {
        return !uriMember1.getUri().equals(uriMember2.getUri());
    }

    /**
     * Throw the error with the right message for the case when the methods are equal
     */
    public static void validateMethodDuplication(IOpenMethod method, IOpenMethod existedMethod) {
        if (method instanceof IUriMember && existedMethod instanceof IUriMember) {
            if (isNotTheSame((IUriMember) method, (IUriMember) existedMethod)) {
                String message = getDuplicatedMethodMessage(existedMethod, method);
                throw new DuplicatedMethodException(message, existedMethod, method);
            }
        } else {
            throw new IllegalStateException("The implementation supports only IUriMember.");
        }
    }

    public static void validateFieldDuplication(IOpenField openField, IOpenField existedField) {
        if (openField instanceof IUriMember && existedField instanceof IUriMember) {
            if (isNotTheSame((IUriMember) openField, (IUriMember) existedField)) {
                throw new DuplicatedFieldException("", openField.getName());
            }
        } else {
            throw new IllegalStateException("The implementation supports only IUriMember.");
        }
    }

    private static String getDuplicatedMethodMessage(IOpenMethod existedMethod, IOpenMethod newMethod) {
        String message;// Modules to which methods belongs to
        List<String> modules = new ArrayList<>();
        if (newMethod instanceof IModuleInfo) {
            // Get the name of the module for the newMethod
            String moduleName = ((IModuleInfo) newMethod).getModuleName();
            if (moduleName != null) {
                modules.add(moduleName);
            }
        }
        if (existedMethod instanceof IModuleInfo) {
            // Get the name of the module for the existedMethod
            String moduleName = ((IModuleInfo) existedMethod).getModuleName();
            if (moduleName != null) {
                modules.add(moduleName);
            }
        }

        boolean canBeDispatched = !(existedMethod instanceof TestSuiteMethod);

        if (modules.isEmpty()) {
            // Case module names where not set to the methods
            if (canBeDispatched) {
                message = String.format(
                    "Method '%s' is already used with the same version, active status, properties set.",
                    MethodUtil.printSignature(existedMethod, INamedThing.REGULAR));
            } else {
                message = String.format("Method '%s' is already used.",
                    MethodUtil.printSignature(existedMethod, INamedThing.REGULAR));
            }
        } else {
            // Case when the module names where set to the methods
            String modulesString = modules.get(0);
            if (modules.size() > 1) {
                if (canBeDispatched) {
                    message = String.format(
                        "Method '%s' is already used in modules '%s' and '%s' with the same version, active status, properties set.",
                        MethodUtil.printSignature(existedMethod, INamedThing.REGULAR),
                        modulesString,
                        modules.get(1));
                } else {
                    message = String.format("Method '%s' is already used in modules '%s' and '%s'.",
                        existedMethod.getName(),
                        modulesString,
                        modules.get(1));
                }
            } else {
                if (canBeDispatched) {
                    message = String.format(
                        "Method '%s' is already used in module '%s' with the same version, active status, properties set.",
                        MethodUtil.printSignature(existedMethod, INamedThing.REGULAR),
                        modulesString);
                } else {
                    message = String.format("Method '%s' is already used in module '%s'.",
                        MethodUtil.printSignature(existedMethod, INamedThing.REGULAR),
                        modulesString);
                }
            }
        }
        return message;
    }
}
