package org.openl.rules.maven.migration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.openl.rules.maven.OpenLPackagings;

/**
 * Removes parameters from the {@code openl-maven-plugin} configuration of a {@code pom.xml} text.
 * <p>
 * Both the configuration of the plugin and the configurations of its executions are cleaned, in
 * {@code <plugins>} and {@code <pluginManagement>} alike, profiles included. Other plugins are left alone, even when
 * they declare parameters of the same names. A configuration left without parameters is removed as a whole, unless
 * its attributes, such as {@code combine.self}, change how Maven merges it with the inherited one.
 * <p>
 * The rest of the text keeps its formatting: a parameter that occupies its own lines is removed together with these
 * lines, the comments stay.
 *
 * @author Yury Molchan
 */
final class PomPluginConfig {

    // Comments and CDATA sections are matched whole, so the tags inside them are skipped. Declarations, such as
    // <?xml ...?>, never match: a tag name cannot start with '?' or '!'.
    private static final Pattern TOKEN = Pattern.compile("<!--.*?-->|<!\\[CDATA\\[.*?]]>|<(/?)([\\w.:-]++)([^>]*+)>",
            Pattern.DOTALL);

    private PomPluginConfig() {
    }

    /**
     * Removes every parameter of the {@code openl-maven-plugin} configurations that {@code kept} rejects.
     *
     * @return the edited text, or the same text when there is nothing to remove
     */
    static String removeParameters(String pom, Predicate<String> kept) {
        var removals = new ArrayList<Range>();
        for (var plugin : parse(pom).descendants("plugin")) {
            if (isOpenLPlugin(pom, plugin)) {
                for (var config : configurations(plugin)) {
                    removals.addAll(removals(pom, config, kept));
                }
            }
        }
        if (removals.isEmpty()) {
            return pom;
        }
        removals.sort(Comparator.comparingInt(Range::start).reversed());
        var edited = new StringBuilder(pom);
        for (var range : removals) {
            edited.delete(range.start(), range.end());
        }
        return edited.toString();
    }

    private static boolean isOpenLPlugin(String pom, Node plugin) {
        return OpenLPackagings.isOpenLPlugin(plugin.childText(pom, "groupId"), plugin.childText(pom, "artifactId"));
    }

    private static List<Node> configurations(Node plugin) {
        var configs = new ArrayList<>(plugin.children("configuration"));
        for (var executions : plugin.children("executions")) {
            for (var execution : executions.children("execution")) {
                configs.addAll(execution.children("configuration"));
            }
        }
        return configs;
    }

    /**
     * The text ranges to delete: the rejected parameters, or the whole configuration when nothing else is left and it
     * has no attributes.
     */
    private static List<Range> removals(String pom, Node config, Predicate<String> kept) {
        var rejected = config.children.stream().filter(parameter -> !kept.test(parameter.name)).toList();
        if (rejected.isEmpty()) {
            return List.of();
        }
        var remaining = new StringBuilder(pom.substring(config.contentStart, config.contentEnd));
        for (var parameter : rejected.reversed()) {
            remaining.delete(parameter.start - config.contentStart, parameter.end - config.contentStart);
        }
        if (remaining.toString().isBlank() && !config.hasAttributes(pom)) {
            return List.of(lines(pom, config.start, config.end));
        }
        return adjacent(pom, rejected).stream().map(range -> lines(pom, range.start(), range.end())).toList();
    }

    /**
     * Joins the parameters that only blanks separate into one range, so that the ranges widened to lines never
     * overlap.
     */
    private static List<Range> adjacent(String pom, List<Node> parameters) {
        var ranges = new ArrayList<Range>();
        for (var parameter : parameters) {
            var last = ranges.isEmpty() ? null : ranges.getLast();
            if (last != null && pom.substring(last.end(), parameter.start).isBlank()) {
                ranges.set(ranges.size() - 1, new Range(last.start(), parameter.end));
            } else {
                ranges.add(new Range(parameter.start, parameter.end));
            }
        }
        return ranges;
    }

    /**
     * Widens the range to whole lines when nothing but blanks shares these lines with it. Otherwise, it takes the
     * blanks between the range and the start or the end of the line, so no trailing or leading blanks are left.
     */
    private static Range lines(String text, int start, int end) {
        var lineStart = start;
        while (lineStart > 0 && isBlank(text.charAt(lineStart - 1))) {
            lineStart--;
        }
        var lineEnd = end;
        while (lineEnd < text.length() && isBlank(text.charAt(lineEnd))) {
            lineEnd++;
        }
        var ownsLineStart = lineStart == 0 || text.charAt(lineStart - 1) == '\n';
        var ownsLineEnd = lineEnd == text.length() || text.charAt(lineEnd) == '\r' || text.charAt(lineEnd) == '\n';
        if (ownsLineStart && ownsLineEnd) {
            return new Range(lineStart, lineEnd + lineBreakLength(text, lineEnd));
        } else if (ownsLineEnd) {
            return new Range(lineStart, end);
        } else if (ownsLineStart) {
            return new Range(start, lineEnd);
        }
        return new Range(start, end);
    }

    private static int lineBreakLength(String text, int index) {
        if (text.startsWith("\r\n", index)) {
            return 2;
        }
        return index < text.length() ? 1 : 0;
    }

    private static boolean isBlank(char ch) {
        return ch == ' ' || ch == '\t';
    }

    /** Builds the element tree of the text. Comments, CDATA sections and declarations are not elements. */
    private static Node parse(String pom) {
        var root = new Node("", 0, 0);
        var open = new ArrayDeque<Node>();
        open.push(root);
        var matcher = TOKEN.matcher(pom);
        while (matcher.find()) {
            var name = matcher.group(2);
            if (name == null) {
                continue;
            }
            if (!matcher.group(1).isEmpty()) {
                open.peek().close(matcher.start(), matcher.end());
                if (open.size() > 1) {
                    open.pop();
                }
            } else {
                var element = new Node(name, matcher.start(), matcher.end());
                open.peek().children.add(element);
                if (!matcher.group(3).endsWith("/")) {
                    open.push(element);
                } else {
                    element.close(matcher.end(), matcher.end());
                }
            }
        }
        return root;
    }

    private record Range(int start, int end) {
    }

    private static final class Node {
        private final String name;
        private final int start;
        private final int contentStart;
        private int contentEnd;
        private int end;
        private final List<Node> children = new ArrayList<>();

        private Node(String name, int start, int contentStart) {
            this.name = name;
            this.start = start;
            this.contentStart = contentStart;
        }

        private void close(int contentEnd, int end) {
            this.contentEnd = contentEnd;
            this.end = end;
        }

        private List<Node> children(String childName) {
            return children.stream().filter(child -> child.name.equals(childName)).toList();
        }

        private List<Node> descendants(String descendantName) {
            var found = new ArrayList<Node>();
            for (var child : children) {
                if (child.name.equals(descendantName)) {
                    found.add(child);
                }
                found.addAll(child.descendants(descendantName));
            }
            return found;
        }

        private boolean hasAttributes(String pom) {
            return pom.substring(start, contentStart).contains("=");
        }

        private @Nullable String childText(String pom, String childName) {
            return children(childName).stream()
                    .map(child -> pom.substring(child.contentStart, child.contentEnd).strip())
                    .findFirst()
                    .orElse(null);
        }
    }
}
