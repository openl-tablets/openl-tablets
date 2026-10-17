package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.studio.projects.model.tables.RawTableCellStyle;
import org.openl.studio.projects.service.tables.read.RawTableRead;
import org.openl.studio.projects.service.tables.read.RawTableReader;

/**
 * A table of a Markdown module, whose file keeps no formatting, is read in the OpenL table style: it reads as its copy
 * into a workbook formatted on save reads, and its file stays as written.
 */
class MarkdownTableLookTest {

    private static final String GREETING = """
            ---
            table: SmartRules String greeting(Integer hour)
            description: Greeting by the hour
            merges: [A3:A4]
            ---

            | Hour  | Greeting       |
            |-------|----------------|
            | 0-11  | Good Morning   |
            | 12-23 | Good Afternoon |
            |       | Good Evening   |
            """;

    private final TableFormatting formatting = mock(TableFormatting.class);
    private final TableCopyService copier = new TableCopyServiceImpl(mock(SystemPropertiesService.class),
            new TableVersionService(), formatting);

    @TempDir
    Path project;

    @BeforeEach
    void writeProject() throws IOException {
        Files.writeString(project.resolve("rules.xml"), """
                <project>
                    <name>Greetings</name>
                    <modules>
                        <module>
                            <name>Greetings</name>
                            <rules-root path="rules/Greetings"/>
                        </module>
                    </modules>
                </project>
                """);
        Files.createDirectories(project.resolve("rules/Greetings"));
        Files.writeString(project.resolve("rules/Greetings/greeting.md"), GREETING);
        when(formatting.onSave()).thenReturn(true);
    }

    @Test
    void tableIsReadInTheStyleItsCopyIntoAWorkbookIsFormattedIn(@TempDir Path workbookProject) throws IOException {
        TableTestProjects.projectModel(workbookProject, "Rates", sheet -> TableTestProjects.row(sheet, 1, 1,
                "Data String notes"));
        var markdown = TableTestProjects.table(TableTestProjects.projectModel(project), "greeting");
        copier.copyInto(markdown, "salutation", null, TableTestProjects.sheetGrid(workbookProject, "Copies"),
                new TableSyntaxNode[0]);
        var copy = TableTestProjects.table(TableTestProjects.projectModel(workbookProject), "salutation");

        var read = TableTestProjects.styledSource(markdown);

        assertEquals(looksOf(TableTestProjects.styledSource(copy)), looksOf(read));
        // The look is the style of the roles, not the default look of a cell
        var title = Objects.requireNonNull(read.get(2).getFirst().style());
        assertNotNull(title.background(), "a title is filled");
        assertEquals(Boolean.TRUE, title.bold());
        var runs = Objects.requireNonNull(read.getFirst().getFirst().runs());
        var name = runs.stream()
                .filter(run -> "greeting".equals(run.text().strip()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(runs.toString()));
        assertEquals(Boolean.TRUE, Objects.requireNonNull(name.style()).bold(), "the name of the table is bold");
        // Nothing is written into the file
        assertEquals(GREETING, Files.readString(project.resolve("rules/Greetings/greeting.md"),
                StandardCharsets.UTF_8));
    }

    @Test
    void windowOfTheRowsIsReadInTheStyleOfTheWholeTable() {
        var markdown = TableTestProjects.table(TableTestProjects.projectModel(project), "greeting");

        var window = new StyledRead(new RawTableReader().read(markdown, RawTableRead.builder()
                .withStyles(true)
                .startRow(4)
                .build())).source;

        var whole = TableTestProjects.styledSource(markdown);
        assertEquals(looksOf(whole.subList(4, whole.size())), looksOf(window));
    }

    @Test
    void readOfTheValuesAloneHasNoStyle() {
        var markdown = TableTestProjects.table(TableTestProjects.projectModel(project), "greeting");

        var read = new RawTableReader().read(markdown);

        assertTrue(read.source.stream().flatMap(List::stream).allMatch(cell -> cell.style() == null),
                read.source::toString);
        assertNull(read.styles);
    }

    /** The look of each cell of a read: its style and the styles of the pieces of its text. */
    private static List<List<Look>> looksOf(List<List<StyledRead.StyledCell>> source) {
        return source.stream()
                .map(row -> row.stream().map(cell -> new Look(cell.style(), cell.runs() == null ? null
                        : cell.runs().stream().map(StyledRead.StyledRun::style).toList())).toList())
                .toList();
    }

    /**
     * The look of a cell.
     *
     * @param style the style of the cell
     * @param runs  the styles of the pieces of its text, or {@code null} when it takes the font of the cell
     */
    private record Look(@Nullable RawTableCellStyle style, @Nullable List<@Nullable RawTableCellStyle> runs) {
    }
}
