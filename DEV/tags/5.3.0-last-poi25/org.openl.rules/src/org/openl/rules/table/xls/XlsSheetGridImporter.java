package org.openl.rules.table.xls;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.IGridRegion;
import org.openl.util.export.IImportedSection;
import org.openl.util.export.IImporter;
import org.openl.util.export.ImportedSection;

public class XlsSheetGridImporter implements IImporter {
    private static class SectionId {
        private int column, row;

        private SectionId(int column, int row) {
            this.column = column;
            this.row = row;
        }
    }
    private IGridRegion region;

    private XlsSheetGridModel gridModel;

    public XlsSheetGridImporter(XlsSheetGridModel gridModel, TableSyntaxNode node) {
        this.gridModel = gridModel;
        region = node.getTable().getGridTable().getRegion();
        region = new GridRegion(region.getTop() + 1, region.getLeft(), region.getBottom(), region.getRight());
    }

    private String[] readRow(int row, int colStart, int colEnd) {
        List<String> rowData = new ArrayList<String>();
        for (int c = colStart; c <= colEnd; c += gridModel.getCellWidth(row, c)) {
            rowData.add(gridModel.getStringCellValue(c, row));
        }
        return rowData.toArray(new String[rowData.size()]);
    }

    /**
     * Reads rows of a section identified by <code>parentSectionId</code>.
     *
     * @param parentSectionId id of section to read rows for, or
     *            <code>null</code> for top level section.
     * @return array of <code>String[]</code>, where each element represent a
     *         row of data.
     */
    public String[][] readRows(Object parentSectionId) {
        if (parentSectionId == null) {
            return null;
        }

        SectionId id = (SectionId) parentSectionId;
        int column = id.column;
        int row = id.row;
        int height = gridModel.getCellHeight(column, row);
        column += gridModel.getCellWidth(column, row);

        List<String[]> rows = new ArrayList<String[]>();
        for (int r = row; r < row + height; ++r) {
            rows.add(readRow(r, column, region.getRight()));
        }
        return rows.toArray(new String[rows.size()][]);
    }

    /**
     * Reads all subsections of a section identified by
     * <code>parentSectionId</code>.
     *
     * @param parentSectionId id of section to read subsections of, or
     *            <code>null</code> for top level section
     * @return array of subsections of given section
     */
    public IImportedSection[] readSections(Object parentSectionId) {
        int column, row, height;
        if (parentSectionId == null) {
            column = region.getLeft();
            row = region.getTop();
            height = region.getBottom() - region.getTop() + 1;
        } else {
            SectionId id = (SectionId) parentSectionId;
            column = id.column;
            row = id.row;
            height = gridModel.getCellHeight(column, row);
            column += gridModel.getCellWidth(column, row);
        }

        List<IImportedSection> sections = new ArrayList<IImportedSection>();
        for (int r = row; r < row + height; r += gridModel.getCellHeight(column, r)) {
            sections.add(new ImportedSection(new SectionId(column, r), gridModel.getStringCellValue(column, r)));
        }
        return sections.toArray(new IImportedSection[sections.size()]);
    }
}
