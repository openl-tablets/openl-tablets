package org.openl.excel.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICellComment;

class CommentsTest {
    private static final String FOLDER = FolderUtils.getResourcesFolder();
    private static final String XLS = FOLDER + "CommentWithShapes.xls";
    private static final String XLSX = FOLDER + "CommentsWithShapes.xlsx";

    @Test
    void commentWithDomInXls() {
        try (var reader = ExcelReaderFactory.fullReadFactory().create(XLS)) {
            var comment = readComment(reader);

            assertNotNull(comment);
            assertEquals("First comment", comment.getText());
        }
    }

    @Test
    void commentWithDomInXlsx() {
        try (var reader = ExcelReaderFactory.fullReadFactory().create(XLSX)) {
            var comment = readComment(reader);

            assertNotNull(comment);
            assertEquals("First comment", comment.getText());
        }
    }

    @Test
    void commentWithSaxInXls() {
        try (var reader = ExcelReaderFactory.sequentialFactory().create(XLS)) {
            var comment = readComment(reader);

            assertNotNull(comment);
            assertEquals("First comment", comment.getText());
        }
    }

    @Test
    void commentWithSaxInXlsx() {
        try (var reader = ExcelReaderFactory.sequentialFactory().create(XLSX)) {
            var comment = readComment(reader);

            assertNotNull(comment);
            assertEquals("First comment", comment.getText());
        }
    }

    private ICellComment readComment(ExcelReader reader) {
        var styles = reader.getTableStyles(reader.getSheets().getFirst(), new GridRegion(0, 0, 6, 3));
        return styles.getComment(5, 1);
    }
}
