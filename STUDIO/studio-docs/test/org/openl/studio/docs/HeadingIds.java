package org.openl.studio.docs;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The ids the headings of one page get, which a link names after {@code #}.
 *
 * <p>An id follows the GitHub rules, which the documentation site and the OpenL Studio viewer both apply: the heading
 * text in lower case, without punctuation and symbols, and with every space turned into a hyphen.
 *
 * <p>A heading whose id is taken by an earlier one gets the next free number appended: {@code -1}, {@code -2} and so
 * on.
 *
 * @author Yury Molchan
 */
final class HeadingIds {

    /** Everything but letters, marks, numbers, connector punctuation, hyphens and spaces. */
    private static final Pattern DROPPED = Pattern.compile("[^\\p{L}\\p{M}\\p{N}\\p{Pc} -]");

    private final Map<String, Integer> taken = new HashMap<>();

    /** The id of the next heading of the page. */
    String next(String heading) {
        var base = DROPPED.matcher(heading.toLowerCase(Locale.ROOT)).replaceAll("").replace(' ', '-');
        var id = base;
        while (taken.containsKey(id)) {
            id = base + "-" + taken.merge(base, 1, Integer::sum);
        }
        taken.put(id, 0);
        return id;
    }
}
