package org.openl.studio.projects.service.tables.read;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.openl.rules.binding.MessageCall;
import org.openl.rules.binding.OpenLMessageSource;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.validation.ValidationUtils;
import org.openl.syntax.ISyntaxNode;

/**
 * The keys of the localized messages a read of a table marks the texts of the cells by.
 *
 * <p>A text a call of {@code msg} gives as its key, directly or through a choice, is a key for sure. Any other text
 * that equals a key of the default messages may be one: a table can choose the key a later call of {@code msg}
 * reads, which the compiler cannot follow.
 *
 * <p>A key leads to the project its texts are written in: the project read, or a project it depends on that gives the
 * key while the project read writes it in no language. A key whose default text no file of the workspace writes, such
 * as one a library of the project gives, leads nowhere.
 */
public final class MessageKeys {

    /** Where the texts of a key are written, which a reader is sent to. */
    public sealed interface Origin {

        /** The project read writes the key, or nothing gives it a default text: the reader can add it there. */
        record Here() implements Origin {
        }

        /**
         * A project the project read depends on gives the key, which the project read writes in no language.
         *
         * @param projectId the id of that project, {@code null} when it is not open
         */
        record Dependency(@Nullable String projectId) implements Origin {
        }

        /** No file of the workspace writes the key: a library of the project, or another resource, gives its text. */
        record External() implements Origin {
        }
    }

    private static final Origin HERE = new Origin.Here();
    private static final MessageKeys NONE = new MessageKeys(null, Set.of(), () -> key -> HERE);

    private final OpenLMessageSource.@Nullable MessageBundle defaults;

    /** The texts calls of {@code msg} give as their keys, told apart from the same texts written elsewhere. */
    private final Set<ISyntaxNode> callKeys;

    private final Supplier<Function<String, Origin>> originsFrom;

    /** Where each key is written, found the first time a key asks: a table of no key reads no localization file. */
    private @Nullable Function<String, Origin> origins;

    private MessageKeys(OpenLMessageSource.@Nullable MessageBundle defaults,
                        Set<ISyntaxNode> callKeys,
                        Supplier<Function<String, Origin>> originsFrom) {
        this.defaults = defaults;
        this.callKeys = callKeys;
        this.originsFrom = originsFrom;
    }

    /** No keys at all: a table read outside a compiled project. */
    public static MessageKeys none() {
        return NONE;
    }

    /**
     * The keys of a compiled project: the default messages its rules read at runtime, and the keys the calls of
     * {@code msg} of its modules and their dependencies give.
     *
     * @param model   the project, compiled
     * @param origins makes the lookup of where a key with a default text is written. It is made only once such a key
     *                asks for it
     */
    public static MessageKeys of(ProjectModel model, Supplier<Function<String, Origin>> origins) {
        var compiled = ValidationUtils.moduleOf(model.getCompiledOpenClass());
        if (compiled == null) {
            return NONE;
        }
        var callKeys = Collections.newSetFromMap(new IdentityHashMap<ISyntaxNode, Boolean>());
        Stream.of(model.getCompiledOpenClass(), model.getOpenedModuleCompiledOpenClass())
                .map(ValidationUtils::moduleOf)
                .filter(Objects::nonNull)
                .flatMap(module -> ValidationUtils.modulesOf(module).stream())
                .flatMap(module -> module.getMessageCalls().stream())
                .map(MessageCall::keyNode)
                .forEach(callKeys::add);
        return new MessageKeys(compiled.getMessageSource().getMessageBundle(Locale.ROOT), callKeys, origins);
    }

    /** Whether a call of {@code msg} gives the text of the node as its key. */
    boolean isCallKey(ISyntaxNode literal) {
        return callKeys.contains(literal);
    }

    /** Where the texts of the key are written. A key without a default text is to be added to the project read. */
    Origin originOf(String key) {
        return defaultTextOf(key) == null ? HERE : origins().apply(key);
    }

    /** Where each key is written, looked up once. A table is read by one request, so no other thread asks. */
    private Function<String, Origin> origins() {
        var found = origins;
        if (found == null) {
            found = originsFrom.get();
            origins = found;
        }
        return found;
    }

    /** The default text of the key, or {@code null} when the default messages have none. */
    @Nullable String defaultTextOf(String key) {
        return defaults != null && defaults.has(key) ? defaults.msg(key) : null;
    }
}
