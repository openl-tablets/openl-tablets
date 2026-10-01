package org.openl.studio.docs;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toCollection;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.Code;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;
import org.jspecify.annotations.Nullable;

/**
 * Finds what breaks the user guides in the OpenL Studio viewer or on the documentation site.
 *
 * <p>The guides are a folder of Markdown pages and the files they use. A page links to another page, to a heading of
 * one, or to a file of the folder, and never outside of it: OpenL Studio serves the folder alone. An external link
 * starts with one of the allowed prefixes. Every image a page shows exists, and every image of the folder is shown.
 *
 * <p>Only the Markdown both renderers draw the same way is accepted: GFM, the {@code [!Note]} alert, a few HTML tags,
 * and code in the languages the viewer knows. A {@code csv} or an {@code openl} code block holds a table that must
 * read.
 *
 * <p>Names are compared case-sensitively, the way the jar and Linux compare them, even on a file system that does not.
 *
 * @author Yury Molchan
 */
@RequiredArgsConstructor
final class GuideValidator {

    /** The name of the table of contents OpenL Studio builds, which no file of the guides may take. */
    static final String TOC = "toc.json";

    /** The code languages the viewer draws or highlights; a block without a language is plain text. */
    static final Set<String> LANGUAGES = Set.of("", "bash", "csv", "groovy", "java", "json", "mermaid", "openl",
            "properties", "xml", "yaml");

    /** The HTML tags the viewer keeps: a line break, a sized image, and a video player in a paragraph. */
    static final Set<String> HTML_TAGS = Set.of("br", "iframe", "img", "p");

    private static final Set<String> IMAGE_TYPES = Set.of("gif", "jpeg", "jpg", "png", "svg", "webp");
    private static final Pattern EXTERNAL = Pattern.compile("^(?:[A-Za-z][A-Za-z0-9+.-]*:|//)");
    /** The addresses GFM links when they are written bare: the web ones and the e-mail ones. */
    private static final Pattern BARE_LINK = Pattern.compile("^(?:https?://|mailto:)");
    private static final Pattern TAG = Pattern.compile("<([A-Za-z][A-Za-z0-9-]*)([^>]*)>");
    private static final Pattern SRC = Pattern.compile("\\ssrc\\s*=\\s*[\"']([^\"']*)[\"']");
    private static final Pattern ALERT = Pattern.compile("^[\\s>]*>\\s*\\[!(\\w+)]");
    private static final Parser PARSER = Parser.builder()
            .extensions(List.of(AutolinkExtension.create(),
                    TablesExtension.create(),
                    YamlFrontMatterExtension.create()))
            .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
            .build();

    /** The folder of the guides. */
    private final Path root;

    /** The prefixes an external link may start with. */
    private final List<String> allowedLinks;

    /** What breaks the guides, ordered by file and line. */
    List<Problem> problems() throws IOException {
        var files = files();
        var pages = new HashMap<String, Page>();
        for (var file : files) {
            if (file.endsWith(".md")) {
                pages.put(file, Page.read(root, file));
            }
        }
        return new Run(files, pages).problems();
    }

    private SortedSet<String> files() throws IOException {
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile)
                    .map(path -> root.relativize(path).toString().replace(File.separatorChar, '/'))
                    .collect(toCollection(TreeSet::new));
        }
    }

    /**
     * The folder a relative link of a page leads to.
     *
     * @return the path relative to the guides folder, or {@code null} when the link leaves it
     */
    static @Nullable String resolve(String from, String target) {
        var segments = new ArrayList<String>();
        for (var segment : (from.substring(0, from.lastIndexOf('/') + 1) + target).split("/")) {
            if (segment.equals("..")) {
                if (segments.isEmpty()) {
                    return null;
                }
                segments.removeLast();
            } else if (!segment.isEmpty() && !segment.equals(".")) {
                segments.add(segment);
            }
        }
        return String.join("/", segments);
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value.replace("+", "%2B"), UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    private static int lineOf(Node node) {
        var spans = node.getSourceSpans();
        return spans.isEmpty() ? 0 : spans.getFirst().getLineIndex() + 1;
    }

    /** The text of a heading, which its id is made of. */
    private static String text(Node node) {
        var text = new StringBuilder();
        for (var child = node.getFirstChild(); child != null; child = child.getNext()) {
            text.append(switch (child) {
                case Text plain -> plain.getLiteral();
                case Code code -> code.getLiteral();
                case Image ignored -> "";
                case HtmlInline ignored -> "";
                default -> text(child);
            });
        }
        return text.toString();
    }

    /**
     * A Markdown page of the guides.
     *
     * @param path the page, relative to the guides folder
     * @param anchors the ids of its headings
     */
    private record Page(String path, List<String> lines, Node document, Set<String> anchors) {

        static Page read(Path root, String path) throws IOException {
            var text = Files.readString(root.resolve(path), UTF_8);
            var document = PARSER.parse(text);
            return new Page(path, text.lines().toList(), document, anchors(document));
        }

        private static Set<String> anchors(Node document) {
            var ids = new HeadingIds();
            var anchors = new HashSet<String>();
            document.accept(new AbstractVisitor() {
                @Override
                public void visit(Heading heading) {
                    anchors.add(ids.next(text(heading)));
                }
            });
            return anchors;
        }
    }

    /** One pass over the guides, collecting what breaks them. */
    @RequiredArgsConstructor
    private final class Run {

        private final SortedSet<String> files;
        private final Map<String, Page> pages;
        private final Set<String> used = new HashSet<>();
        private final List<Problem> problems = new ArrayList<>();

        List<Problem> problems() {
            files.stream()
                    .filter(pages::containsKey)
                    .map(pages::get)
                    .forEach(page -> page.document().accept(new PageCheck(page)));
            files.stream()
                    .filter(file -> IMAGE_TYPES.contains(extensionOf(file)) && !used.contains(file))
                    .forEach(file -> problems.add(new Problem(file, 0, "No page shows the image.")));
            if (files.contains(TOC)) {
                problems.add(new Problem(TOC, 0,
                        "The name is reserved for the table of contents OpenL Studio builds."));
            }
            return problems.stream()
                    .distinct()
                    .sorted(Comparator.comparing(Problem::file).thenComparingInt(Problem::line))
                    .toList();
        }

        private static String extensionOf(String file) {
            return file.substring(file.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }

        private String missing(String destination, String target) {
            return files.stream()
                    .filter(target::equalsIgnoreCase)
                    .findFirst()
                    .map(file -> destination + " differs in case from " + file + ".")
                    .orElse(destination + " does not exist.");
        }

        /** Checks what one page links to, shows and is written in. */
        @RequiredArgsConstructor
        private final class PageCheck extends AbstractVisitor {

            private final Page page;

            @Override
            public void visit(Link link) {
                if (!isBare(link) || BARE_LINK.matcher(link.getDestination()).find()) {
                    target(link.getDestination(), lineOf(link), false);
                }
                visitChildren(link);
            }

            /**
             * Whether the link is an address written as text, without brackets.
             *
             * <p>The parser links such an address with any scheme, a JDBC URL included, while GFM links only a web or an
             * e-mail address.
             */
            private boolean isBare(Link link) {
                var spans = link.getSourceSpans();
                if (spans.isEmpty()) {
                    return false;
                }
                var first = page.lines().get(spans.getFirst().getLineIndex()).charAt(spans.getFirst().getColumnIndex());
                return first != '[' && first != '<';
            }

            @Override
            public void visit(Image image) {
                target(image.getDestination(), lineOf(image), true);
            }

            @Override
            public void visit(HtmlInline html) {
                html(html.getLiteral(), lineOf(html));
            }

            @Override
            public void visit(HtmlBlock html) {
                html(html.getLiteral(), lineOf(html));
            }

            @Override
            public void visit(FencedCodeBlock code) {
                var info = code.getInfo() == null ? "" : code.getInfo().strip();
                var language = info.isEmpty() ? "" : info.split("\\s+", 2)[0];
                var line = lineOf(code);
                if (!LANGUAGES.contains(language)) {
                    report(line, "The code language `" + language + "` is not supported.");
                } else if (language.equals("csv") || language.equals("openl")) {
                    table(code.getLiteral(), language, line + 1);
                }
            }

            @Override
            public void visit(BlockQuote quote) {
                var line = lineOf(quote);
                var alert = ALERT.matcher(line > 0 ? page.lines().get(line - 1) : "");
                if (alert.find() && !alert.group(1).equalsIgnoreCase("note")) {
                    report(line, "The alert [!" + alert.group(1) + "] is not supported. Use [!Note].");
                }
                visitChildren(quote);
            }

            private void target(String destination, int line, boolean image) {
                if (EXTERNAL.matcher(destination).find()) {
                    external(destination, line);
                } else if (destination.startsWith("/")) {
                    report(line, leaves(destination));
                } else {
                    relative(destination, line, image);
                }
            }

            private static String leaves(String destination) {
                return "The link " + destination + " leaves the guides. Link to the documentation site instead.";
            }

            private void external(String url, int line) {
                if (allowedLinks.stream().noneMatch(url::startsWith)) {
                    report(line, "The external link " + url
                            + " is not allowed. Add its prefix to allowed-links.txt when it is right.");
                }
            }

            private void relative(String destination, int line, boolean image) {
                var hash = destination.indexOf('#');
                var path = decode(hash < 0 ? destination : destination.substring(0, hash));
                var target = path.isEmpty() ? page.path() : resolve(page.path(), path);
                if (target == null) {
                    report(line, leaves(destination));
                } else if (image) {
                    shown(target, destination, line);
                } else {
                    followed(target, hash < 0 ? "" : decode(destination.substring(hash + 1)), destination, line);
                }
            }

            private void shown(String target, String destination, int line) {
                if (files.contains(target)) {
                    used.add(target);
                } else {
                    report(line, "The image " + missing(destination, target));
                }
            }

            private void followed(String target, String fragment, String destination, int line) {
                var file = files.contains(target) ? target : indexOf(target);
                if (!files.contains(file)) {
                    report(line, "The link " + missing(destination, target));
                } else if (namesNoHeading(file, fragment)) {
                    report(line, "The link " + destination + " names no heading of " + file + ".");
                } else {
                    used.add(file);
                }
            }

            /** Whether a link to a page names a heading the page does not have. */
            private boolean namesNoHeading(String file, String fragment) {
                var target = pages.get(file);
                return target != null && !fragment.isEmpty() && !target.anchors().contains(fragment);
            }

            /** The page a link to a folder opens. */
            private static String indexOf(String folder) {
                return folder.isEmpty() ? "index.md" : folder + "/index.md";
            }

            private void html(String literal, int line) {
                var tags = TAG.matcher(literal);
                while (tags.find()) {
                    var name = tags.group(1).toLowerCase(Locale.ROOT);
                    var at = line + (int) literal.substring(0, tags.start()).chars().filter(c -> c == '\n').count();
                    var src = SRC.matcher(tags.group(2));
                    if (!HTML_TAGS.contains(name)) {
                        report(at, "The HTML tag <" + name + "> is not supported.");
                    } else if (src.find()) {
                        source(name, src.group(1), at);
                    }
                }
            }

            /** Checks what an image shows or a video player plays. */
            private void source(String tag, String src, int line) {
                if (tag.equals("img")) {
                    target(src, line, true);
                } else if (tag.equals("iframe")) {
                    external(src, line);
                }
            }

            private void table(String text, String language, int firstLine) {
                var issues = language.equals("openl") ? OpenLTable.issues(text) : csvIssues(text);
                issues.forEach(issue -> report(firstLine + issue.line(), issue.message()));
            }

            /** What breaks the table of a {@code csv} code block: a broken record, or no record at all. */
            private static List<Issue> csvIssues(String text) {
                var records = CsvRecords.parse(text);
                return records.rows().isEmpty() ? List.of(new Issue(0, "The table has no rows.")) : records.issues();
            }

            private void report(int line, String message) {
                problems.add(new Problem(page.path(), line, message));
            }
        }
    }
}
