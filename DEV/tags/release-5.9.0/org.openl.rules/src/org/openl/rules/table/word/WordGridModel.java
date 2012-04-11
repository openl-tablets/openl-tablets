package org.openl.rules.table.word;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.openl.rules.indexer.WordTableElement;
import org.openl.rules.table.AGrid;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;

public class WordGridModel extends AGrid {

    private WordTableElement wordTable;

    private WordCell[][] grid;

    private IGridRegion[] regions;

    public WordGridModel(WordTableElement wte) {
        wordTable = wte;
        new WordGridBuilder().buildModel(this, wte);
    }

    public int getCellHeight(int column, int row) {

        IGridRegion reg = getRegionContaining(column, row);
        if (reg == null) {
            return 1;
        }

        if (reg.getLeft() == column && reg.getTop() == row) {
            return IGridRegion.Tool.height(reg);
        }
        return 1;
    }

    public ICell getCell(int column, int row) {
        return grid[column][row];
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.rules.table.IGrid#getCellType(int, int)
     */
    public int getCellType(int column, int row) {
        return IGrid.CELL_TYPE_STRING;
    }

    public String getCellUri(int column, int row) {
        return grid[column][row].getUri();
    }

    public int getCellWidth(int column, int row) {
        IGridRegion reg = getRegionContaining(column, row);
        if (reg == null) {
            return 1;
        }

        if (reg.getLeft() == column && reg.getTop() == row) {
            return IGridRegion.Tool.width(reg);
        }
        return 1;
    }

    public int getColumnWidth(int col) {
        return 0;
    }

    public String getFormattedCellValue(int column, int row) {
        return getStringCellValue(column, row);
    }

    public int getMaxColumnIndex(int row) {
        return grid.length - 1;
    }

    public int getMaxRowIndex() {
        return grid[0].length - 1;
    }

    public IGridRegion getMergedRegion(int i) {
        return regions[i];
    }

    public int getMinColumnIndex(int row) {
        return 0;
    }

    public int getMinRowIndex() {
        return 0;
    }

    public int getNumberOfMergedRegions() {
        return regions.length;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.rules.table.IGrid#getObjectCellValue(int, int)
     */
    public Object getObjectCellValue(int column, int row) {
        return getStringCellValue(column, row);
    }

    public String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd) {
        WordCell wcStart = grid[colStart][rowStart];
        WordCell wcEnd = grid[colEnd][rowEnd];

        return getUri() + "&wdParStart=" + wcStart.getParStart() + "&wdParEnd=" + wcEnd.getParEnd();
    }

    public IGridRegion getRegionContaining(int column, int row) {
        for (int i = 0; i < regions.length; i++) {
            if (IGridRegion.Tool.contains(regions[i], column, row)) {
                return regions[i];
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.rules.table.IGrid#getRegionStartingAt(int, int)
     */
    public IGridRegion getRegionStartingAt(int colFrom, int rowFrom) {
        IGridRegion reg = getRegionContaining(colFrom, rowFrom);
        return reg != null && reg.getLeft() == colFrom && reg.getTop() == rowFrom ? reg : null;

    }

    public String getStringCellValue(int column, int row) {
        return grid[column][row].getStringValue();
    }

    public String getUri() {
        return wordTable.getDocument().getUri() + "?_W";
    }

    public boolean isEmpty(int col, int row) {
        WordCell wc = grid[col][row];
        String text = wc.getStringValue();
        return text == null || text.length() == 0;
    }

    public boolean isPartOfTheMergedRegion(int col, int row) {
        IGridRegion gr = getRegionContaining(col, row);
        return gr != null;
    }

    public String getCellFormula(int column, int row) {
        //is formula possible in word?
        return null;
    }

    public static class WordGridBuilder {

        private WordCell[][] grid;

        private int nrows;

        private int ncolumns;

        private List<IGridRegion> regions = new ArrayList<IGridRegion>();

        private List<IGridRegion> vregions = new ArrayList<IGridRegion>();

        private TreeMap<Integer, Integer> wCells = new TreeMap<Integer, Integer>();

        public void buildModel(WordGridModel model, WordTableElement wte) {
            Table wt = wte.getTable();
            nrows = wt.numRows();

            ncolumns = calcColumns(wt);

            grid = new WordCell[ncolumns][nrows];

            normalizeWidths();

            fillCells(wt);

            model.grid = grid;
            IGridRegion[] rr = new IGridRegion[regions.size() + vregions.size()];
            for (int i = 0; i < regions.size(); i++) {
                rr[i] =  regions.get(i);
            }

            for (int i = 0; i < vregions.size(); i++) {
                rr[i + regions.size()] =  vregions.get(i);
            }

            model.regions = rr;

        }

        private int calcColumns(Table wt) {
            for (int i = 0; i < wt.numRows(); i++) {
                TableRow tr = wt.getRow(i);
                int ncells = tr.numCells();
                int w = 0;

                for (int j = 0; j < ncells; j++) {
                    TableCell tc = tr.getCell(j);
                    w += tc.getWidth();
                    wCells.put(w, w);

                }
            }
            return wCells.size();
        }

        private void fillCells(Table wt) {

            for (int i = 0; i < wt.numRows(); i++) {
                TableRow tr = wt.getRow(i);
                int ncells = tr.numCells();
                int w = 0;

                for (int j = 0; j < ncells; j++) {
                    TableCell tc = tr.getCell(j);

                    int colFrom = getColumn(w);

                    w += tc.getWidth();
                    int colTo = getColumn(w);

                    for (int k = colFrom; k < colTo; k++) {
                        grid[k][i] = new WordCell(tc, i, k );
                    }
                    if (tc.isVerticallyMerged()) {
                        if (tc.text().length() > 1) {
                            startNewVRegion(colFrom, i, colTo - 1);
                        } else {
                            mergeVRegion(colFrom, i, colTo - 1);
                        }
                    } else if (colTo - colFrom > 1) {
                        makeNewRegion(colFrom, i, colTo - 1);
                    }

                }
            }

        }

        private int getColumn(int width) {
            if (width == 0) {
                return 0;
            }
            Integer ww = (Integer) wCells.get(new Integer(width));
            if (ww == null) {
                throw new RuntimeException("Can not find width: " + width);
            }
            return ww.intValue();
        }

        private void makeNewRegion(int colFrom, int row, int colTo) {
            GridRegion gr = new GridRegion(row, colFrom, row, colTo);
            regions.add(gr);
        }

        private void mergeVRegion(int colFrom, int i, int colTo) {
            for (int j = 0; j < vregions.size(); j++) {
                GridRegion gr = (GridRegion) vregions.get(j);
                if (gr.getLeft() == colFrom && gr.getRight() == colTo && gr.getBottom() == i - 1) {
                    gr.setBottom(i);
                    return;
                }
            }
            startNewVRegion(colFrom, i, colTo);
        }

        private void normalizeWidths() {
            TreeMap<Integer, Integer> tm = new TreeMap<Integer, Integer>();
            int r = 0;
            for (Iterator<Integer> iter = wCells.keySet().iterator(); iter.hasNext();) {
                Integer w =  iter.next();

                tm.put(w, new Integer(++r));
            }

            wCells = tm;
        }

        private void startNewVRegion(int colFrom, int i, int colTo) {
            GridRegion gr = new GridRegion(i, colFrom, i, colTo);
            vregions.add(gr);
        }
    }

}
