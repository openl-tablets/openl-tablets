package org.openl.studio.projects.service.tables.read;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.tables.RawTableCellUsage;
import org.openl.studio.projects.model.tables.RawTableUsageKind;
import org.openl.studio.projects.model.tables.RawTableView;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Verifies that a table is read with the keys of the localized messages its cells write: a key a call of
 * {@code msg} gives for sure, and a text that equals a key of the default messages as one it may be. A key leads to
 * the project its texts are written in, and nowhere when no file of the workspace writes it.
 */
class RawTableMessageKeysTest {

    private ProjectModel projectModel;
    private IOpenLTable table;

    @BeforeEach
    void writeProject(@TempDir Path projectDir) throws Exception {
        Files.createDirectories(projectDir.resolve("i18n"));
        Files.writeString(projectDir.resolve("i18n/message.properties"),
                "greeting = Hello, {0}!\norder.new = New order\n");
        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Calls");
            sheet.createRow(0).createCell(0).setCellValue("Spreadsheet SpreadsheetResult Calls()");
            String[][] steps = {
                    {"Step", "Value"},
                    {"Known", "= msg(\"greeting\", \"Ann\")"},
                    {"Absent", "= msg(\"absent\")"},
                    {"Chosen", "= \"order.new\""},
                    {"Plain", "= \"plain text\""},
            };
            for (var i = 0; i < steps.length; i++) {
                var row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(steps[i][0]);
                row.createCell(1).setCellValue(steps[i][1]);
            }
            try (OutputStream out = Files.newOutputStream(projectDir.resolve("Calls.xlsx"))) {
                workbook.write(out);
            }
        }
        var module = ProjectResolver.getInstance().resolve(projectDir).getModules().getFirst();
        projectModel = new ProjectModel(mock(WebStudio.class), null);
        projectModel.setModuleInfo(module);
        table = new TableSyntaxNodeAdapter(projectModel.getAllTableSyntaxNodes().iterator().next());
    }

    @AfterEach
    void destroyModel() {
        projectModel.destroy();
    }

    @Test
    void marksTheKeyACallOfMsgGivesWithItsDefaultText() {
        var usage = single(read(), "= msg(\"greeting\", \"Ann\")");

        assertEquals(RawTableUsageKind.MESSAGE, usage.kind());
        assertEquals("greeting", usage.messageKey());
        assertEquals("greeting = Hello, {0}!", usage.description());
        // A key a call gives is a key for sure.
        assertNull(usage.soft());
        // The project read writes it: its texts are read there.
        assertNull(usage.projectId());
    }

    @Test
    void leadsAKeyAProjectItDependsOnGivesToThatProject() {
        Function<String, MessageKeys.Origin> origins = key -> "order.new".equals(key)
                ? new MessageKeys.Origin.Dependency("BASE")
                : new MessageKeys.Origin.Here();

        var read = read(() -> origins);

        var possible = single(read, "= \"order.new\"");
        assertEquals("order.new", possible.messageKey());
        assertEquals("BASE", possible.projectId());
        assertNull(single(read, "= msg(\"greeting\", \"Ann\")").projectId());
    }

    @Test
    void tellsTheTextOfAKeyAnExternalResourceGivesAndLeadsNowhere() {
        var read = read(() -> key -> new MessageKeys.Origin.External());

        var usage = usageOf(read, "= msg(\"greeting\", \"Ann\")");
        assertEquals(RawTableUsageKind.MESSAGE, usage.kind());
        assertEquals("greeting = Hello, {0}! (from an external resource)", usage.description());
        assertNull(usage.messageKey());
        assertNull(usage.projectId());
        assertEquals("May be the localization key \"order.new\": New order (from an external resource)",
                usageOf(read, "= \"order.new\"").description());
        // A key with no default text is still to be added to the project.
        assertEquals("absent", single(read, "= msg(\"absent\")").messageKey());
    }

    @Test
    void looksForWhereAKeyIsWrittenOnceAndNotForAKeyNoMessagesGive() {
        var made = new AtomicInteger();
        var asked = new AtomicInteger();
        Supplier<Function<String, MessageKeys.Origin>> origins = () -> {
            made.incrementAndGet();
            return key -> {
                asked.incrementAndGet();
                return new MessageKeys.Origin.Here();
            };
        };

        var keys = MessageKeys.of(projectModel, origins);

        assertEquals(new MessageKeys.Origin.Here(), keys.originOf("absent"));
        assertEquals(0, made.get());
        keys.originOf("greeting");
        keys.originOf("order.new");
        assertEquals(1, made.get());
        assertEquals(2, asked.get());
    }

    @Test
    void marksAKeyNoDefaultFileHasSoTheReaderCanAddIt() {
        var usage = single(read(), "= msg(\"absent\")");

        assertEquals("absent", usage.messageKey());
        assertEquals("absent is not in the default localization file", usage.description());
    }

    @Test
    void marksATextThatEqualsAKeyAsOneItMayBe() {
        var usage = single(read(), "= \"order.new\"");

        assertEquals("order.new", usage.messageKey());
        assertEquals(Boolean.TRUE, usage.soft());
        assertEquals("May be the localization key \"order.new\": New order", usage.description());
    }

    @Test
    void leavesATextNoMessageHasUnmarked() {
        var metaInfo = TableTestProjects.cellOf(read().source, "= \"plain text\"").metaInfo();

        assertTrue(metaInfo == null || metaInfo.usages().stream().noneMatch(usage -> usage.messageKey() != null));
    }

    @Test
    void marksNoKeysOfATableReadWithoutTheMessages() {
        var read = new RawTableReader().read(table, RawTableRead.builder().withMetaInfo(true).build());

        var metaInfo = TableTestProjects.cellOf(read.source, "= msg(\"greeting\", \"Ann\")").metaInfo();
        assertTrue(metaInfo == null || metaInfo.usages().stream().noneMatch(usage -> usage.messageKey() != null));
    }

    private RawTableView read() {
        return read(() -> key -> new MessageKeys.Origin.Here());
    }

    private RawTableView read(Supplier<Function<String, MessageKeys.Origin>> origins) {
        return new RawTableReader().read(table, RawTableRead.builder()
                .withMetaInfo(true)
                .messages(MessageKeys.of(projectModel, origins))
                .build());
    }

    /** The one usage of the kind of messages of the cell of the given text. */
    private static RawTableCellUsage usageOf(RawTableView read, String text) {
        var usages = TableTestProjects.cellOf(read.source, text)
                .metaInfo()
                .usages()
                .stream()
                .filter(usage -> usage.kind() == RawTableUsageKind.MESSAGE)
                .toList();
        assertEquals(1, usages.size(), usages::toString);
        return usages.getFirst();
    }

    /** The one key the cell of the given text is marked by, its range checked over the text of the cell. */
    private static RawTableCellUsage single(RawTableView read, String text) {
        var cell = TableTestProjects.cellOf(read.source, text);
        var keys = cell.metaInfo()
                .usages()
                .stream()
                .filter(usage -> usage.messageKey() != null)
                .toList();
        assertEquals(1, keys.size(), keys::toString);
        var usage = keys.getFirst();
        assertEquals(usage.messageKey(), String.valueOf(cell.value()).substring(usage.start(), usage.end()));
        return usage;
    }
}
