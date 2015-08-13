package org.openl.extension.xmlrules.syntax;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.openl.extension.ExtensionWrapperGrid;
import org.openl.extension.FileLauncher;
import org.openl.rules.table.AGrid;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;

public class SimpleGrid extends AGrid implements ExtensionWrapperGrid {
    private final NavigableMap<Integer, NavigableMap<Integer, ICell>> rows;
    private final String uri;
    private final String sourceFileName;

    private IGridRegion[] mergedRegions;

    public SimpleGrid(NavigableMap<Integer, NavigableMap<Integer, ICell>> rows, String uri, String sourceFileName) {
        this.rows = rows;
        this.uri = uri;
        this.sourceFileName = sourceFileName;
    }

    @Override
    public String getSourceFileName() {
        return sourceFileName;
    }

    @Override
    public boolean isLaunchSupported() {
        return XmlRulesFileLauncher.isLaunchSupported();
    }

    @Override
    public FileLauncher getFileLauncher() {
        return new XmlRulesFileLauncher(uri, sourceFileName);
    }

    @Override
    public ICell getCell(int column, int row) {
        Map<Integer, ICell> columns = rows.get(row);
        SimpleCell emptyCell = new SimpleCell(column, row, null);
        ICell cell = columns == null ? emptyCell : columns.get(column);
        return cell == null ? emptyCell : cell;
    }

    @Override
    public int getColumnWidth(int col) {
        // TODO Get column width from the file
        return 0;
    }

    @Override
    public int getMaxColumnIndex(int row) {
        if (rows.isEmpty()) {
            return 0;
        }

        NavigableMap<Integer, ICell> columns = rows.lastEntry().getValue();
        return columns.isEmpty() ? 0 : columns.lastKey();
    }

    @Override
    public int getMaxRowIndex() {
        return rows.isEmpty() ? 0 : rows.lastKey();
    }

    @Override
    public int getMinColumnIndex(int row) {
        if (rows.isEmpty()) {
            return 0;
        }

        NavigableMap<Integer, ICell> columns = rows.firstEntry().getValue();
        return columns.isEmpty() ? 0 : columns.firstKey();
    }

    @Override
    public int getMinRowIndex() {
        return rows.isEmpty() ? 0 : rows.firstKey();
    }

    @Override
    public IGridRegion getMergedRegion(int i) {
        return getMergedRegions()[i];
    }

    @Override
    public int getNumberOfMergedRegions() {
        return getMergedRegions().length;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public boolean isEmpty(int col, int row) {
        ICell cell = getCell(col, row);
        return cell == null || StringUtils.isBlank(cell.getStringValue());
    }

    private IGridRegion[] getMergedRegions() {
        if (mergedRegions == null) {
            List<IGridRegion> regions = new ArrayList<IGridRegion>();

            for (NavigableMap<Integer, ICell> columns : rows.values()) {
                for (ICell cell : columns.values()) {
                    if (cell.getRegion() != null) {
                        regions.add(cell.getRegion());
                    }
                }
            }

            mergedRegions = regions.toArray(new IGridRegion[regions.size()]);
        }
        return mergedRegions;
    }

    public static class SimpleGridBuilder {
        private String uri;
        private NavigableMap<Integer, NavigableMap<Integer, ICell>> rows = new TreeMap<Integer, NavigableMap<Integer, ICell>>();
        private String sourceFileName;

        public SimpleGridBuilder(String uri, String sourceFileName) {
            this.uri = uri;
            this.sourceFileName = sourceFileName;
        }

        public SimpleGridBuilder addCell(ICell cell) {
            int row = cell.getRow();

            NavigableMap<Integer, ICell> columns = rows.get(row);
            if (columns == null) {
                columns = new TreeMap<Integer, ICell>();
                rows.put(row, columns);
            }

            return this;
        }

        public SimpleGrid build() {
            return new SimpleGrid(rows, uri, sourceFileName);
        }
    }

}
