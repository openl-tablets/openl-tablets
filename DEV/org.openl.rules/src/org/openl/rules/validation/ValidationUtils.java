package org.openl.rules.validation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenClass;
import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    static ValidationResult withMessages(Collection<OpenLMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return validationSuccess();
        }
        var errorMessages = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.ERROR);
        if (errorMessages.isEmpty()) {
            return new ValidationResult(ValidationStatus.SUCCESS, messages);
        } else {
            return new ValidationResult(ValidationStatus.FAIL, messages);
        }
    }

    public static ValidationResult validationSuccess() {
        return new ValidationResult(ValidationStatus.SUCCESS);
    }

    /**
     * The compiled module and every module it depends on, directly or through others.
     *
     * <p>Each module comes once, the compiled one first, even when several modules depend on it. A dependency that
     * is not a module of rules is left out, and so are the modules it depends on.
     *
     * @param compiled the class a compilation built
     * @return the modules, or nothing when the class is not a module of rules
     */
    public static List<XlsModuleOpenClass> modulesOf(IOpenClass compiled) {
        var modules = new ArrayList<XlsModuleOpenClass>();
        if (!(compiled instanceof XlsModuleOpenClass top)) {
            return modules;
        }
        var seen = Collections.newSetFromMap(new IdentityHashMap<XlsModuleOpenClass, Boolean>());
        var pending = new ArrayDeque<XlsModuleOpenClass>();
        pending.add(top);
        while (!pending.isEmpty()) {
            var module = pending.poll();
            if (seen.add(module)) {
                modules.add(module);
                module.getDependencies()
                        .stream()
                        .map(dependency -> moduleOf(dependency.getCompiledOpenClass()))
                        .filter(Objects::nonNull)
                        .forEach(pending::add);
            }
        }
        return modules;
    }

    /**
     * The module of rules a compilation built, with its errors.
     *
     * @param compiled what a compilation built, or {@code null} for nothing compiled yet
     * @return the module, or {@code null} when the compilation built no module of rules
     */
    public static @Nullable XlsModuleOpenClass moduleOf(@Nullable CompiledOpenClass compiled) {
        return compiled != null && compiled.getOpenClassWithErrors() instanceof XlsModuleOpenClass module ? module
                : null;
    }

}
