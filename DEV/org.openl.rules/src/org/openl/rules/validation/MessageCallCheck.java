package org.openl.rules.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.binding.MessageCall;
import org.openl.rules.binding.OpenLMessageSource;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;

/**
 * Checks the calls of {@code msg} against the messages they read at runtime.
 *
 * <p>A call reads the messages of the module a rule is called through, not those of the module the call is written
 * in. A rule of a dependency project may so ask for a key that only the project using it defines. The calls of every
 * module a project compiles, those of its dependencies included, are therefore checked against the messages of the
 * project: those of its own files, its dependencies and its libraries.
 *
 * <p>The calls are the ones the compilation of each module kept, so the check needs no compilation of its own: it is
 * run again whenever the localization files change.
 *
 * <p>Only a key written as text is checked: a key built from parts, passed through a variable or returned by
 * another rule is known at runtime alone. Both checks warn, as the call still runs. A key no default localization
 * file has returns the key itself. A message given values takes as many as its placeholders count, the highest
 * number among them plus one; a message given none is returned as written.
 *
 * <p>A compilation in the execution mode, such as the one of OpenL Rule Services, keeps no calls, so nothing is
 * checked there.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MessageCallCheck {

    /**
     * Checks the calls of one module, without the modules it depends on.
     *
     * @param module the module
     * @param source the messages the calls read: those of the project, its dependencies and its libraries
     * @return the warnings about the calls
     */
    public static List<OpenLMessage> checkModule(XlsModuleOpenClass module, OpenLMessageSource source) {
        var defaults = source.getMessageBundle(Locale.ROOT);
        var placeholders = new HashMap<String, Integer>();
        var messages = new ArrayList<OpenLMessage>();
        module.getMessageCalls().forEach(call -> check(call, defaults, placeholders).ifPresent(messages::add));
        return messages;
    }

    /** The warning about the call, when its key has no default text or it passes a wrong number of values. */
    private static Optional<OpenLMessage> check(MessageCall call,
                                                OpenLMessageSource.MessageBundle defaults,
                                                Map<String, Integer> placeholdersOf) {
        var key = call.key();
        if (!defaults.has(key)) {
            return Optional.of(OpenLMessagesUtils.newWarnMessage(
                    "Key '%s' has no default text: a language without a text of its own returns the key."
                            .formatted(key),
                    call.keyNode()));
        }
        var values = call.values();
        if (values == 0) {
            return Optional.empty();
        }
        // A key is read by many calls, and its text is read once.
        var placeholders = placeholdersOf.computeIfAbsent(key, read -> {
            Set<Integer> indices = MessagePattern.of(defaults.msg(read)).indices();
            return indices.isEmpty() ? 0 : Collections.max(indices) + 1;
        });
        if (placeholders == values) {
            return Optional.empty();
        }
        return Optional.of(OpenLMessagesUtils.newWarnMessage("'%s' has %d placeholder%s, but %d value%s passed."
                .formatted(key, placeholders, placeholders == 1 ? "" : "s", values, values == 1 ? " is" : "s are"),
                call.node()));
    }
}
