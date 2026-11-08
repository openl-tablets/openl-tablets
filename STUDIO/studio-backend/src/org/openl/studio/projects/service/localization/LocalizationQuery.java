package org.openl.studio.projects.service.localization;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.openl.studio.common.model.SortOrder;

/**
 * What the keys of the messages are narrowed down to, and the order they are answered in.
 *
 * @param search       a piece of a key or of a text of the project in any language, in any case; {@code null} for any
 * @param problems     only the keys the check of the files warns about
 * @param untranslated only the keys each of these languages shows the default text of the project for, having no text
 *                     of its own nor of a parent language; empty for any
 * @param order        the keys by their names, or {@code null} in the order they are written
 */
public record LocalizationQuery(@Nullable String search,
                                boolean problems,
                                List<String> untranslated,
                                @Nullable SortOrder order) {

    /** Every key, in the order they are written. */
    public static final LocalizationQuery ALL = new LocalizationQuery(null, false, List.of(), null);

    /** The keys by their names, ignoring case first. */
    private static final Comparator<String> BY_NAME = Comparator.<String, String>comparing(
            key -> key.toLowerCase(Locale.ROOT)).thenComparing(Comparator.naturalOrder());

    /** The keys the query leaves, in its order. */
    List<String> keysOf(ProjectMessages messages, Set<String> withProblems) {
        var wanted = search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
        var left = messages.keys().stream().filter(key -> leaves(messages, key, wanted, withProblems));
        return (order == null ? left : left.sorted(order.apply(BY_NAME))).toList();
    }

    private boolean leaves(ProjectMessages messages, String key, @Nullable String wanted, Set<String> withProblems) {
        return (wanted == null || matches(messages, key, wanted))
                && (!problems || withProblems.contains(key))
                && untranslated.stream().allMatch(language -> messages.showsDefaultText(language, key));
    }

    /** Whether the key or a text of it holds the piece searched for, in lower case. */
    private static boolean matches(ProjectMessages messages, String key, String wanted) {
        return key.toLowerCase(Locale.ROOT).contains(wanted)
                || messages.textsOf(key)
                .values()
                .stream()
                .anyMatch(text -> text != null && text.toLowerCase(Locale.ROOT).contains(wanted));
    }
}
