package org.openl.rules.validation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.binding.MessageCall;
import org.openl.rules.binding.OpenLMessageSource;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenClass;
import org.openl.validation.IOpenLValidator;
import org.openl.validation.ValidationResult;

/**
 * Checks the calls of {@code msg} against the messages they read at runtime.
 *
 * <p>A call reads the messages of the module a rule is called through, not those of the module the call is written
 * in. A rule of a dependency project may so ask for a key that only the project using it defines. Every call of the
 * compiled module and of all the modules it depends on is therefore checked against the messages of the compiled
 * module: those of its project, its dependencies and its libraries.
 *
 * <p>Only a key written as text is checked: a key built from parts, passed through a variable or returned by
 * another rule is known at runtime alone. Both checks warn, as the call still runs. A key no default localization
 * file has returns the key itself. A message given values takes as many as its placeholders count, the highest
 * number among them plus one; a message given none is returned as written.
 */
public final class MessageCallValidator implements IOpenLValidator {

    @Override
    public ValidationResult validate(IOpenClass openClass) {
        if (!(openClass instanceof XlsModuleOpenClass compiled)) {
            return ValidationUtils.validationSuccess();
        }
        var defaults = compiled.getMessageSource().getMessageBundle(Locale.ROOT);
        var messages = new ArrayList<OpenLMessage>();
        for (var module : modulesOf(compiled)) {
            module.getMessageCalls().forEach(call -> check(call, defaults).ifPresent(messages::add));
        }
        return ValidationUtils.withMessages(messages);
    }

    /** The compiled module and every module it depends on, directly or through others, each once. */
    private static List<XlsModuleOpenClass> modulesOf(XlsModuleOpenClass compiled) {
        var seen = Collections.newSetFromMap(new IdentityHashMap<XlsModuleOpenClass, Boolean>());
        var modules = new ArrayList<XlsModuleOpenClass>();
        var pending = new ArrayDeque<XlsModuleOpenClass>();
        pending.add(compiled);
        while (!pending.isEmpty()) {
            var module = pending.poll();
            if (seen.add(module)) {
                modules.add(module);
                module.getDependencies().forEach(dependency -> {
                    if (dependency.getCompiledOpenClass().getOpenClassWithErrors() instanceof XlsModuleOpenClass used) {
                        pending.add(used);
                    }
                });
            }
        }
        return modules;
    }

    /** The warning about the call, when its key has no default text or it passes a wrong number of values. */
    private static Optional<OpenLMessage> check(MessageCall call, OpenLMessageSource.MessageBundle defaults) {
        var key = call.key();
        if (!defaults.has(key)) {
            return Optional.of(OpenLMessagesUtils.newWarnMessage(
                    "Message key '%s' is not found in the default localization file.".formatted(key), call.keyNode()));
        }
        var values = call.values();
        if (values == 0) {
            return Optional.empty();
        }
        var indices = MessagePattern.of(defaults.msg(key)).indices();
        var placeholders = indices.isEmpty() ? 0 : Collections.max(indices) + 1;
        if (placeholders == values) {
            return Optional.empty();
        }
        return Optional.of(OpenLMessagesUtils.newWarnMessage("Message '%s' has %d placeholder%s, but %d value%s passed."
                .formatted(key, placeholders, placeholders == 1 ? "" : "s", values, values == 1 ? " is" : "s are"),
                call.node()));
    }
}
