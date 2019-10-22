package org.openl.rules.types;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.types.IModuleInfo;
import org.openl.types.IOpenMethod;

public final class ValidationMessages {
    private ValidationMessages() {
    }

    public static String getDuplicatedMethodMessage(IOpenMethod existedMethod, IOpenMethod newMethod) {
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
                    existedMethod.getName());
            } else {
                message = String.format("Method '%s' is already used.", existedMethod.getName());
            }
        } else {
            // Case when the module names where set to the methods
            String modulesString = modules.get(0);
            if (modules.size() > 1) {
                if (canBeDispatched) {
                    message = String.format(
                        "Method '%s' is already used in modules '%s' and '%s' with the same version, active status, properties set.",
                        existedMethod.getName(),
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
                        existedMethod.getName(),
                        modulesString);
                } else {
                    message = String
                        .format("Method '%s' is already used in module '%s'.", existedMethod.getName(), modulesString);
                }
            }
        }
        return message;
    }
}
