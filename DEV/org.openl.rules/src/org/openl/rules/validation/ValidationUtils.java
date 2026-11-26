package org.openl.rules.validation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.validation.ValidationResult;
import org.openl.validation.ValidationStatus;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ValidationUtils {

    static ValidationResult withMessages(Collection<OpenLMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return validationSuccess();
        }
        var errorMessages = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.ERROR);
        if (errorMessages.isEmpty()) {
            return new ValidationResult(ValidationStatus.SUCCESS, messages);
        }
        return new ValidationResult(ValidationStatus.FAIL, messages);
    }

    public static ValidationResult validationSuccess() {
        return new ValidationResult(ValidationStatus.SUCCESS);
    }

    /**
     * The severity of a value that breaks the constraint of a table property.
     *
     * @param propertyName the name of the property
     * @param otherwise    the severity used when the property declares none
     * @return the severity the property declares, or the given one
     */
    static Severity severityOf(String propertyName, Severity otherwise) {
        var property = TablePropertyDefinitionUtils.getPropertyByName(propertyName);
        var severity = property == null ? null : property.getErrorSeverity();
        return severity == null ? otherwise : severity;
    }

    /**
     * A message about a table: an error for the {@code ERROR} severity, and a warning for any other one.
     *
     * @param message the text of the message
     * @param severity the severity of the message
     * @param node the table the message is about, or {@code null} for none
     */
    static OpenLMessage messageOf(String message, Severity severity, @Nullable TableSyntaxNode node) {
        if (severity == Severity.ERROR) {
            return new OpenLErrorMessage(SyntaxNodeExceptionUtils.createError(message, node));
        }
        return node == null ? OpenLMessagesUtils.newWarnMessage(message) : new OpenLWarnMessage(message, node);
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
        return modulesOf(compiled, dependency -> true);
    }

    /**
     * The compiled module and every module it depends on through the dependencies the filter follows.
     *
     * <p>Each module comes once, the compiled one first. A dependency the filter leaves out is not looked into, and
     * neither is a dependency that is not a module of rules.
     *
     * @param compiled the class a compilation built
     * @param followed whether a dependency is followed
     * @return the modules, or nothing when the class is not a module of rules
     */
    static List<XlsModuleOpenClass> modulesOf(IOpenClass compiled, Predicate<CompiledDependency> followed) {
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
                        .filter(followed)
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
