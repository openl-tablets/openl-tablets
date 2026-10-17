package org.openl.core;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.ref.Reference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.function.ToLongFunction;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sun.management.ThreadMXBean;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import org.openl.CompiledOpenClass;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.markdown.MarkdownProjectWriter;
import org.openl.rules.project.markdown.MarkdownProjectWriter.Converted;
import org.openl.rules.project.markdown.MarkdownProjectWriter.Kept;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.util.FileUtils;

/**
 * Measures how the projects of the functional tests compile and test once their workbooks are written as module
 * folders of Markdown files: each project is compiled and tested as it is, written as Markdown, and compiled and
 * tested again. The report, {@code target/markdown-corpus/report.md}, tells the share of projects whose Markdown copy
 * compiles and tests alike, every difference, the workbooks kept and why, and what each form costs: the size of its
 * files, the time and the memory a compilation takes, and the memory the compiled rules hold.
 *
 * <p>A copy is alike when it gives the same errors and warnings, word for word, and the same results of its tests.
 *
 * <p>It runs only on request, {@code -Dopenl.markdown.corpus=true}: it is a measurement, not a check of the build.
 */
@EnabledIfSystemProperty(named = "openl.markdown.corpus", matches = "true")
final class MarkdownCorpusTest {

    private static final Path CORPUS = Path.of("test-resources/functionality");
    private static final Path OUT = Path.of("target/markdown-corpus");
    private static final ThreadMXBean THREADS = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    /** A message that names a module by its workbook, which the Markdown copy names by its folder. */
    private static final Pattern WORKBOOK_MODULE = Pattern.compile("^(WARN: File '.*?)\\.xlsx?(' does not match )");

    private Locale defaultLocale;
    private TimeZone defaultTimeZone;

    @BeforeEach
    void setupLocale() {
        defaultLocale = Locale.getDefault();
        defaultTimeZone = TimeZone.getDefault();
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void restoreLocale() {
        Locale.setDefault(defaultLocale);
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    void corpus() throws IOException {
        if (Files.exists(OUT)) {
            FileUtils.delete(OUT);
        }
        var entries = new ArrayList<Entry>();
        try (var paths = Files.list(CORPUS)) {
            for (var path : paths.sorted().toList()) {
                var name = path.getFileName().toString();
                if (Files.isDirectory(path) || name.endsWith(".xlsx") || name.endsWith(".xls")) {
                    entries.add(measure(path));
                }
            }
        }
        assertFalse(entries.isEmpty(), "The corpus holds no project");
        var report = Report.of(entries);
        Files.writeString(OUT.resolve("report.md"), report, StandardCharsets.UTF_8);
        System.out.println(report.lines().limit(40).reduce((all, line) -> all + "\n" + line).orElse(""));
    }

    /** Compiles and tests an entry of the corpus as it is and as Markdown. */
    private static Entry measure(Path path) {
        var name = path.getFileName().toString();
        try {
            var source = asProject(path);
            if (isWorkspace(source)) {
                return Entry.of(name, Status.SKIPPED, "A workspace of several projects");
            }
            var excel = run(source);
            var markdown = OUT.resolve("md").resolve(name);
            var written = MarkdownProjectWriter.write(factory(source, false), markdown);
            if (written.converted().isEmpty()) {
                return new Entry(name, Status.KEPT, "No workbook written as Markdown", excel, null, Size.NONE,
                        written.kept());
            }
            var md = run(markdown);
            var size = Size.of(written.converted(), markdown);
            var difference = md.differenceFrom(excel);
            return new Entry(name, difference.isEmpty() ? Status.SAME : Status.DIFFERENT, difference, excel, md, size,
                    written.kept());
        } catch (Exception | LinkageError e) {
            return Entry.of(name, Status.FAILED, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /** The project of an entry: its folder, or a folder of its own for a workbook. */
    private static Path asProject(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            return path;
        }
        var project = Files.createDirectories(OUT.resolve("workbooks")
                .resolve(FileUtils.removeExtension(path.getFileName().toString())));
        Files.copy(path, project.resolve(path.getFileName()));
        return project;
    }

    private static boolean isWorkspace(Path folder) throws IOException {
        try (var paths = Files.list(folder)) {
            return paths.allMatch(Files::isDirectory);
        }
    }

    private static SimpleProjectEngineFactory<Object> factory(Path project, boolean executionMode) {
        return new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<>()
                .setProject(project.toString())
                .setExecutionMode(executionMode)
                .build();
    }

    /**
     * Compiles a project and runs its tests, then compiles it in the execution mode, as OpenL Rule Services does, and
     * measures what the compiled rules hold.
     */
    private static Run run(Path project) throws Exception {
        var messages = new ArrayList<String>();
        var tests = new ArrayList<String>();
        var compiled = new CompiledOpenClass[1];
        var compile = Cost.of(() -> compiled[0] = factory(project, false).getCompiledOpenClass());
        run(compiled[0], messages, tests);
        var execution = Cost.of(() -> factory(project, true).getCompiledOpenClass());
        messages.sort(null);
        tests.sort(null);
        return new Run(List.copyOf(messages), List.copyOf(tests), compile, execution);
    }

    /** Collects the errors and the warnings of a compilation, each by its severity and text, and runs its tests. */
    private static void run(CompiledOpenClass compiled, List<String> messages, List<String> tests) {
        compiled.getAllMessages()
                .stream()
                .filter(message -> message.getSeverity() == Severity.ERROR || message.getSeverity() == Severity.WARN)
                .map(message -> message.getSeverity() + ": " + message.getSummary())
                .forEach(messages::add);
        if (!compiled.hasErrors()) {
            var env = new SimpleRulesVM().getRuntimeEnv();
            var openClass = compiled.getOpenClass();
            var instance = openClass.newInstance(env);
            for (var test : ProjectHelper.allTesters(openClass)) {
                var result = (TestUnitsResults) test.invoke(instance, new Object[0], env);
                // A test table without a name of its own is named with a number it takes in the order of compilation
                tests.add("%s: %d units, %d failed".formatted(test.getName().replaceFirst("\\$Test\\$\\d+$", ""),
                        result.getNumberOfTestUnits(), result.getNumberOfFailures()));
            }
        }
    }

    private enum Status {
        SAME,
        DIFFERENT,
        KEPT,
        SKIPPED,
        FAILED
    }

    /**
     * An entry of the corpus, as it is and as Markdown.
     *
     * @param kept the workbooks the Markdown copy keeps
     */
    private record Entry(String name, Status status, String detail, Run excel, Run markdown, Size size,
                         List<Kept> kept) {

        static Entry of(String name, Status status, String detail) {
            return new Entry(name, status, detail, null, null, Size.NONE, List.of());
        }
    }

    /**
     * The size of the modules written as folders.
     *
     * @param workbook the size of the workbooks
     * @param markdown the size of the Markdown files
     * @param zipped   the size of the Markdown files in a ZIP archive, as a deployment carries them
     */
    private record Size(long workbook, long markdown, long zipped) {

        static final Size NONE = new Size(0, 0, 0);

        static Size of(List<Converted> modules, Path project) throws IOException {
            var workbook = modules.stream().mapToLong(Converted::workbookBytes).sum();
            var markdown = modules.stream().mapToLong(Converted::markdownBytes).sum();
            var zipped = new long[1];
            var counter = new OutputStream() {
                @Override
                public void write(int b) {
                    zipped[0]++;
                }

                @Override
                public void write(byte[] b, int off, int len) {
                    zipped[0] += len;
                }
            };
            try (var zip = new ZipOutputStream(counter)) {
                for (var module : modules) {
                    var folder = project.resolve(module.folder());
                    try (var files = Files.walk(folder)) {
                        for (var file : files.filter(Files::isRegularFile).sorted().toList()) {
                            zip.putNextEntry(new ZipEntry(project.relativize(file).toString()));
                            zip.write(Files.readAllBytes(file));
                            zip.closeEntry();
                        }
                    }
                }
            }
            return new Size(workbook, markdown, zipped[0]);
        }
    }

    /**
     * What a compilation costs.
     *
     * @param millis    how long it took
     * @param allocated the memory allocated to compile
     * @param retained  the memory the compiled rules hold, as the heap tells after a collection
     */
    private record Cost(long millis, long allocated, long retained) {

        static Cost of(Compilation compilation) throws Exception {
            var before = usedHeap();
            var allocatedBefore = THREADS.getCurrentThreadAllocatedBytes();
            var start = System.nanoTime();
            var compiled = compilation.compile();
            var millis = (System.nanoTime() - start) / 1_000_000;
            var allocated = THREADS.getCurrentThreadAllocatedBytes() - allocatedBefore;
            var retained = Math.max(0, usedHeap() - before);
            Reference.reachabilityFence(compiled);
            return new Cost(millis, allocated, retained);
        }

        private static long usedHeap() {
            System.gc();
            System.gc();
            var runtime = Runtime.getRuntime();
            return runtime.totalMemory() - runtime.freeMemory();
        }
    }

    @FunctionalInterface
    private interface Compilation {
        CompiledOpenClass compile() throws Exception;
    }

    /**
     * A compilation of a project, the results of its tests, and what it costs.
     *
     * @param messages  the errors and the warnings of the compilation, each by its severity and text
     * @param tests     each test with the number of its test units and of their failures
     * @param compile   the cost of the compilation that runs the tests, keeping the tables
     * @param execution the cost of the compilation in the execution mode, as OpenL Rule Services compiles
     */
    private record Run(List<String> messages, List<String> tests, Cost compile, Cost execution) {

        /**
         * What differs from the run of the project as it is, or nothing. A message differs by its text, but for the
         * name of a module: the module of a workbook is named by the workbook, the one of its copy by the folder.
         */
        String differenceFrom(Run excel) {
            var expected = excel.messages.stream()
                    .map(message -> WORKBOOK_MODULE.matcher(message).replaceFirst("$1$2"))
                    .sorted()
                    .toList();
            if (!messages.equals(expected)) {
                var added = messages.stream().filter(message -> !expected.contains(message)).limit(3).toList();
                var lost = expected.stream().filter(message -> !messages.contains(message)).limit(3).toList();
                return "%d messages, %d in Excel; new %s; gone %s".formatted(messages.size(), expected.size(),
                        added, lost);
            }
            if (tests.equals(excel.tests)) {
                return "";
            }
            var added = tests.stream().filter(test -> !excel.tests.contains(test)).limit(3).toList();
            var lost = excel.tests.stream().filter(test -> !tests.contains(test)).limit(3).toList();
            return "tests %s; in Excel %s".formatted(added, lost);
        }
    }

    /** The report of the corpus. */
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class Report {

        static String of(List<Entry> entries) {
            var byStatus = new TreeMap<Status, Long>();
            entries.forEach(entry -> byStatus.merge(entry.status(), 1L, Long::sum));
            var alike = byStatus.getOrDefault(Status.SAME, 0L);
            var compared = alike + byStatus.getOrDefault(Status.DIFFERENT, 0L);
            var same = entries.stream().filter(entry -> entry.status() == Status.SAME).toList();
            var text = new StringBuilder("# Markdown corpus\n\n| Projects | Count |\n|---|---|\n");
            byStatus.forEach((status, count) -> text.append("| %s | %d |%n".formatted(status, count)));
            text.append("%nAlike: %d of %d compared (%.1f%%).%n".formatted(alike, compared,
                    compared == 0 ? 0 : 100.0 * alike / compared));
            text.append("%n| Over the %d projects alike | Excel | Markdown | Excel / Markdown |%n|---|---|---|---|%n"
                    .formatted(same.size()));
            row(text, "Size of the module files, bytes", sum(same, entry -> entry.size().workbook()),
                    sum(same, entry -> entry.size().markdown()));
            row(text, "Size in a ZIP archive, bytes", sum(same, entry -> entry.size().workbook()),
                    sum(same, entry -> entry.size().zipped()));
            row(text, "Compile time, ms", sum(same, entry -> entry.excel().compile().millis()),
                    sum(same, entry -> entry.markdown().compile().millis()));
            row(text, "Memory allocated to compile, bytes", sum(same, entry -> entry.excel().compile().allocated()),
                    sum(same, entry -> entry.markdown().compile().allocated()));
            row(text, "Compile time in the execution mode, ms", sum(same, entry -> entry.excel().execution().millis()),
                    sum(same, entry -> entry.markdown().execution().millis()));
            row(text, "Memory allocated in the execution mode, bytes",
                    sum(same, entry -> entry.excel().execution().allocated()),
                    sum(same, entry -> entry.markdown().execution().allocated()));
            row(text, "Memory the rules hold in the execution mode, bytes",
                    sum(same, entry -> entry.excel().execution().retained()),
                    sum(same, entry -> entry.markdown().execution().retained()));
            text.append("\n## Projects compiled or tested differently\n\n");
            entries.stream()
                    .filter(entry -> entry.status() == Status.DIFFERENT || entry.status() == Status.FAILED)
                    .forEach(entry -> text.append("- **%s** — %s%n".formatted(entry.name(),
                            entry.detail().replace('\n', ' '))));
            text.append("\n## Workbooks kept, and why\n\n");
            var reasons = new TreeMap<String, Long>();
            entries.forEach(entry -> entry.kept().forEach(kept -> kept.reasons().forEach(reason -> reasons.merge(
                    reason.replaceFirst("^[^:]*: ", ""), 1L, Long::sum))));
            reasons.forEach((reason, count) -> text.append("- %d × %s%n".formatted(count, reason)));
            text.append('\n');
            entries.forEach(entry -> entry.kept().forEach(kept -> text.append("- %s/%s: %s%n".formatted(entry.name(),
                    kept.path(), String.join("; ", kept.reasons())))));
            return text.toString();
        }

        private static void row(StringBuilder text, String measure, long excel, long markdown) {
            text.append("| %s | %,d | %,d | %.1f |%n".formatted(measure, excel, markdown,
                    markdown == 0 ? 0 : (double) excel / markdown));
        }

        private static long sum(List<Entry> entries, ToLongFunction<Entry> measure) {
            return entries.stream().mapToLong(measure).sum();
        }
    }
}
