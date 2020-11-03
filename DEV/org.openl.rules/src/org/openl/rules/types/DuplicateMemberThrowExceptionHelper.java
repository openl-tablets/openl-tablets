package org.openl.rules.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.types.IModuleInfo;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

public final class DuplicateMemberThrowExceptionHelper {

    private DuplicateMemberThrowExceptionHelper() {
    }

    private static boolean isNotTheSameByUri(IUriMember uriMember1, IUriMember uriMember2) {
        return !uriMember1.getUri().equals(uriMember2.getUri());
    }

    /**
     * Throw the error with the right message for the case when the methods are equal
     */
    public static void throwDuplicateMethodExceptionIfMethodsAreNotTheSame(IOpenMethod newOpenMethod,
            IOpenMethod existedOpenMethod) {
        if (newOpenMethod instanceof IUriMember && existedOpenMethod instanceof IUriMember) {
            if (isNotTheSameByUri((IUriMember) newOpenMethod, (IUriMember) existedOpenMethod)) {
                String message = buildDuplicatedMethodMessage(newOpenMethod, existedOpenMethod);
                throw new DuplicatedMethodException(message, existedOpenMethod, newOpenMethod);
            }
        } else {
            throw new IllegalStateException("The implementation supports only IUriMember.");
        }
    }

    public static void throwDuplicateFieldExceptionIfFieldsAreNotTheSame(IOpenField newOpenField,
            IOpenField existedOpenField) {
        if (newOpenField instanceof IUriMember && existedOpenField instanceof IUriMember) {
            if (isNotTheSameByUri((IUriMember) newOpenField, (IUriMember) existedOpenField)) {
                throw new DuplicatedFieldException("", newOpenField.getName());
            }
        } else {
            throw new IllegalStateException("The implementation supports only IUriMember.");
        }
    }

    private static String buildDuplicatedMethodMessage(IOpenMethod newOpenMethod, IOpenMethod existedOpenMethod) {
        if (existedOpenMethod.getSignature().getNumberOfParameters() != existedOpenMethod.getSignature()
            .getNumberOfParameters()) {
            throw new IllegalStateException("Method signatures are not the same");
        }
        for (int i = 0; i < existedOpenMethod.getSignature().getNumberOfParameters(); i++) {
            if (!Objects.equals(existedOpenMethod.getSignature().getParameterType(i),
                newOpenMethod.getSignature().getParameterType(i))) {
                throw new IllegalStateException("Method signatures are not the same");
            }
        }
        String message;// Modules to which methods belongs to
        List<String> modules = new ArrayList<>();
        if (newOpenMethod instanceof IModuleInfo) {
            // Get the name of the module for the newOpenMethod
            String moduleName = ((IModuleInfo) newOpenMethod).getModuleName();
            if (moduleName != null) {
                modules.add(moduleName);
            }
        }
        if (existedOpenMethod instanceof IModuleInfo) {
            // Get the name of the module for the existedOpenMethod
            String moduleName = ((IModuleInfo) existedOpenMethod).getModuleName();
            if (moduleName != null) {
                modules.add(moduleName);
            }
        }

        boolean canBeDispatched = !(existedOpenMethod instanceof TestSuiteMethod);

        if (modules.isEmpty()) {
            // Case module names where not set to the methods
            if (canBeDispatched) {
                message = String.format(
                    "Method '%s' is already used with the same version, active status, properties set.",
                    MethodUtil.printSignature(existedOpenMethod, INamedThing.REGULAR));
            } else {
                message = String.format("Method '%s' is already used.",
                    MethodUtil.printSignature(existedOpenMethod, INamedThing.REGULAR));
            }
        } else {
            // Case when the module names where set to the methods
            String modulesString = modules.get(0);
            if (modules.size() > 1) {
                if (canBeDispatched) {
                    message = String.format(
                        "Method '%s' is already used in modules '%s' and '%s' with the same version, active status, properties set.",
                        MethodUtil.printSignature(existedOpenMethod, INamedThing.REGULAR),
                        modulesString,
                        modules.get(1));
                } else {
                    message = String.format("Method '%s' is already used in modules '%s' and '%s'.",
                        existedOpenMethod.getName(),
                        modulesString,
                        modules.get(1));
                }
            } else {
                if (canBeDispatched) {
                    message = String.format(
                        "Method '%s' is already used in module '%s' with the same version, active status, properties set.",
                        MethodUtil.printSignature(existedOpenMethod, INamedThing.REGULAR),
                        modulesString);
                } else {
                    message = String.format("Method '%s' is already used in module '%s'.",
                        MethodUtil.printSignature(existedOpenMethod, INamedThing.REGULAR),
                        modulesString);
                }
            }
        }
        return message;
    }
}
