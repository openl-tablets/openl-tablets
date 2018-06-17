package org.openl.excel.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICellComment;

public class CommentsTest {
    private static final String FOLDER = FolderUtils.getResourcesFolder();
    private static final String XLS = FOLDER + "CommentWithShapes.xls";
    private static final String XLSX = FOLDER + "CommentsWithShapes.xlsx";

    @Test
    public void commentWithDomInXls() {
        try (ExcelReader reader = ExcelReaderFactory.fullReadFactory().create(XLS)){
            ICellComment comment = readComment(reader);

            assertNotNull(comment);
            assertEquals("First comment", comment.getText());
        }
    }

    @Test
    public void commentWithDomInXlsx() {
        try (ExcelReader reader = ExcelReaderFactory.fullReadFactory().create(XLSX)){
            ICellComment comment = readComment(reader);

            assertNotNull(comment);
            assertEquals("First comment", comment.getText());
        }
    }

    @Test
    public void commentWithSaxInXls() {
        try (ExcelReader reader = ExcelReaderFactory.sequentialFactory().create(XLS)) {
            ICellComment comment = readComment(reader);

            assertNotNull(comment);
            assertEquals("First comment", comment.getText());
        }
    }

    @Test
    public void commentWithSaxInXlsx() {
        try (ExcelReader reader = ExcelReaderFactory.sequentialFactory().create(XLSX)) {
            ICellComment comment = readComment(reader);

            assertNotNull(comment);
            assertEquals("First comment", comment.getText());
        }
    }

    private ICellComment readComment(ExcelReader reader) {
        TableStyles styles = reader.getTableStyles(reader.getSheets().get(0), new GridRegion(0, 0, 6, 3));
        return styles.getComment(5, 1);
    }
}
