package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.table.IOpenLTable;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.projects.model.tables.RawCellStyleInput;
import org.openl.studio.projects.model.tables.RawTableSourceAction;
import org.openl.studio.projects.model.tables.RawTextMark;
import org.openl.studio.projects.model.tables.RawTextMarkColor;
import org.openl.studio.projects.model.tables.StyleTarget;
import org.openl.studio.projects.model.tables.UpdateTarget;

/**
 * A table of a Markdown module keeps no formatting but the indent, which carries the structure of a TBasic and a
 * ColumnMatch table.
 */
class MarkdownTablesTest {

    @Test
    void tableOfAMarkdownModuleTakesTheIndentAndNoOtherStyle() {
        var table = mock(IOpenLTable.class);
        when(table.getUri()).thenReturn("file:/projects/P/rules/Bonus/bonus.md?sheet=Bonus&range=A1:D6");
        var indent = styled(new RawCellStyleInput(null, null, null, null, null, null, 1));
        var bold = styled(new RawCellStyleInput(null, null, null, true, null, null, null));

        assertDoesNotThrow(() -> MarkdownTables.refuseStyle(table, indent));
        var refused = assertThrows(BadRequestException.class, () -> MarkdownTables.refuseStyle(table, bold));
        assertEquals("openl.error.400.table.markdown.style.message", refused.getErrorCode());

        // A table of a workbook takes any style
        when(table.getUri()).thenReturn("file:/projects/P/rules/Bonus.xlsx?sheet=Bonus&range=A1:D6");
        assertDoesNotThrow(() -> MarkdownTables.refuseStyle(table, bold));
    }

    @Test
    void textOfACellOfAMarkdownModuleIsNotMarkedInPieces() {
        var table = mock(IOpenLTable.class);
        when(table.getUri()).thenReturn("file:/projects/P/rules/Bonus/bonus.md?sheet=Bonus&range=A1:D6");
        var plain = List.<RawTableSourceAction>of(new RawTableSourceAction.Update(new UpdateTarget.Cell(1, 0, "Gold")));
        var marked = List.<RawTableSourceAction>of(new RawTableSourceAction.Update(new UpdateTarget.Cell(1, 0, "Gold",
                List.of(new RawTextMark(0, 2, RawTextMarkColor.INFO)))));

        assertDoesNotThrow(() -> MarkdownTables.refuseStyle(table, plain));
        // A file keeps the text of a cell and no look, so pieces of it are not marked
        var refused = assertThrows(BadRequestException.class, () -> MarkdownTables.refuseStyle(table, marked));
        assertEquals("openl.error.400.table.markdown.style.message", refused.getErrorCode());
    }

    private static List<RawTableSourceAction> styled(RawCellStyleInput style) {
        return List.of(new RawTableSourceAction.Style(new StyleTarget.Cells(0, 1, 1, 1, style)));
    }
}
