package org.openl.rules.webstudio.web.servlet;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNullElse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import jakarta.servlet.ServletContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * The user guides the war holds, and their table of contents.
 *
 * <p>The table of contents lays the pages out the way the documentation site lays out its sidebar. A folder lists its
 * pages, then its folders, each in the order of their names, and its own {@code index.md} stands for the folder. A
 * folder holding no page is left out.
 *
 * <p>A page is titled by the {@code title} of its front matter, else by the heading of level 1 to 3 its text starts
 * with, else by its file name. A folder takes the title of its {@code index.md}, else its name. A title made of a name
 * drops a number prefix such as {@code 01-} and capitalizes each word: {@code 05-sso-saml} is titled {@code Sso Saml}.
 *
 * @param files the files of the guides, relative to the guides folder and starting with {@code /}
 * @param contents the table of contents, whose root entry is the guides folder
 * @author Yury Molchan
 */
record UserGuides(Set<String> files, Entry contents) {

    private static final Pattern FRONT_MATTER_TITLE = Pattern.compile("title:\\s*(.*?)\\s*");
    private static final Pattern HEADING = Pattern.compile("#{1,3}\\s+(.+?)\\s*");

    /**
     * An entry of the table of contents: a page, or a folder with the entries inside it.
     *
     * @param file the page, relative to the guides folder; a folder without an {@code index.md} has none
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    record Entry(String title, @Nullable String file, List<Entry> children) {
    }

    /** Reads the guides kept in the given folder of the web application, such as {@code /docs/}. */
    static UserGuides read(ServletContext context, String root) {
        var files = new TreeSet<String>();
        var contents = new Reader(context, root, files).folder(root);
        return new UserGuides(Set.copyOf(files), contents);
    }

    /** The title a page gives itself, by its front matter or by the heading it starts with. */
    static @Nullable String titleOf(String page) {
        var lines = page.lines().toList();
        var opened = !lines.isEmpty() && lines.getFirst().strip().equals("---");
        // A front matter is closed by a line of its own; an unclosed one is the text of the page.
        var end = opened
                ? IntStream.range(1, lines.size()).filter(i -> lines.get(i).strip().equals("---")).findFirst().orElse(-1)
                : -1;
        var title = end > 0 ? firstMatch(lines.subList(1, end), FRONT_MATTER_TITLE) : null;
        if (title != null) {
            return unquoted(title);
        }
        var text = end + 1;
        var first = lines.stream().skip(text).filter(line -> !line.isBlank()).limit(1).toList();
        return firstMatch(first, HEADING);
    }

    /** The title a name makes: without a number prefix, every word capitalized. */
    static String titleOfName(String name) {
        var words = new ArrayList<>(List.of(name.split("-")));
        var prefix = words.getFirst();
        if (words.size() > 1 && !prefix.isEmpty() && prefix.length() <= 3 && Character.isDigit(prefix.charAt(0))) {
            words.removeFirst();
        }
        return words.stream().map(UserGuides::capitalized).collect(Collectors.joining(" ")).replace("Openl", "OpenL");
    }

    private static @Nullable String firstMatch(List<String> lines, Pattern pattern) {
        return lines.stream()
                .map(line -> pattern.matcher(line.strip()))
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                .findFirst()
                .orElse(null);
    }

    private static String unquoted(String value) {
        var quoted = value.length() > 1 && (value.startsWith("\"") && value.endsWith("\"")
                || value.startsWith("'") && value.endsWith("'"));
        return quoted ? value.substring(1, value.length() - 1) : value;
    }

    private static String capitalized(String word) {
        if (word.isEmpty()) {
            return word;
        }
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1).toLowerCase(Locale.ROOT);
    }

    /** One walk through the folders of the guides. */
    @Slf4j
    @RequiredArgsConstructor
    private static final class Reader {

        private final ServletContext context;
        private final String root;
        private final Set<String> files;

        /** The entry of a folder, collecting the files it holds. */
        Entry folder(String folder) {
            var paths = new TreeSet<>(requireNonNullElse(context.getResourcePaths(folder), Set.<String>of()));
            var pages = new ArrayList<Entry>();
            var folders = new ArrayList<Entry>();
            for (var path : paths) {
                if (path.endsWith("/")) {
                    folders.add(folder(path));
                } else {
                    files.add(path.substring(root.length() - 1));
                    if (isPage(path)) {
                        pages.add(page(path));
                    }
                }
            }
            folders.removeIf(entry -> entry.file() == null && entry.children().isEmpty());
            pages.addAll(folders);
            var index = folder + "index.md";
            var name = folder.substring(folder.lastIndexOf('/', folder.length() - 2) + 1, folder.length() - 1);
            return paths.contains(index)
                    ? new Entry(requireNonNullElse(titleIn(index), titleOfName(name)), relative(index), pages)
                    : new Entry(titleOfName(name), null, pages);
        }

        private static boolean isPage(String path) {
            return path.endsWith(".md") && !path.endsWith("/index.md");
        }

        private Entry page(String path) {
            var name = path.substring(path.lastIndexOf('/') + 1, path.length() - ".md".length());
            return new Entry(requireNonNullElse(titleIn(path), titleOfName(name)), relative(path), List.of());
        }

        private String relative(String path) {
            return path.substring(root.length());
        }

        private @Nullable String titleIn(String path) {
            try (var page = context.getResourceAsStream(path)) {
                return page == null ? null : titleOf(new String(page.readAllBytes(), UTF_8));
            } catch (IOException e) {
                log.warn("Failed to read the title of the guide '{}'.", path, e);
                return null;
            }
        }
    }
}
