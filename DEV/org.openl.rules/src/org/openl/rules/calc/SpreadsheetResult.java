package org.openl.rules.calc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.openl.rules.helpers.ITableAdaptor;
import org.openl.rules.helpers.TablePrinter;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;
import org.openl.types.java.CustomJavaOpenClass;

/**
 * Serializable bean that handles result of spreadsheet calculation.
 *
 */
@XmlRootElement
@CustomJavaOpenClass(type = SpreadsheetResultOpenClass.class, variableInContextFinder = SpreadsheetResultRootDictionaryContext.class)
public class SpreadsheetResult implements Serializable {

    private static final long serialVersionUID = 8704762477153429384L;

    private Object[][] results;
    private int height;
    private int width;
    private String[] columnNames;
    private String[] rowNames;
    private transient String[] columnTitles;
    private transient String[] rowTitles;
    private transient Map<String, Point> fieldsCoordinates = null;

    /**
     * logical representation of calculated spreadsheet table it is needed for
     * web studio to display results
     */
    private transient ILogicalTable logicalTable;

    public SpreadsheetResult() {
    }

    public SpreadsheetResult(int height, int width) {
        this.height = height;
        this.width = width;
        this.columnNames = new String[height];
        this.rowNames = new String[width];
        this.results = new Object[height][width];
        initFieldsCoordinates();
    }

    public SpreadsheetResult(Object[][] results,
            String[] rowNames,
            String[] columnNames,
            String[] rowTitles,
            String[] columnTitles) {
        this.columnNames = columnNames.clone();
        this.rowNames = rowNames.clone();
        this.columnTitles = columnTitles.clone();
        this.rowTitles = rowTitles.clone();
        this.height = rowNames.length;
        this.width = columnNames.length;
        this.results = results;
        initFieldsCoordinates();
    }

    private void initFieldsCoordinates() {
        this.fieldsCoordinates = new HashMap<String, Point>();
        for (int i = 0; i < this.rowNames.length; i++) {
            for (int j = 0; j < this.columnNames.length; j++) {
                StringBuilder sb = new StringBuilder();
                sb.append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                    .append(this.columnNames[j])
                    .append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                    .append(this.rowNames[i]);
                fieldsCoordinates.put(sb.toString(), new Point(j, i));
            }
        }
    }

    /**
     * @deprecated use {@link SpreadsheetResult#getHeight()} instead.
     * 
     */
    @Deprecated
    public int height() {
        return getHeight();
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Object[][] getResults() {
        return results.clone();
    }

    public void setResults(Object[][] results) {
        this.results = results.clone();
    }

    /**
     * @deprecated use {@link SpreadsheetResult#getWidth()} instead.
     * 
     */
    @Deprecated
    public int width() {
        return getWidth();
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames.clone();
    }

    public String[] getRowNames() {
        return rowNames;
    }

    public void setRowNames(String[] rowNames) {
        this.rowNames = rowNames.clone();
    }

    public Object getValue(int row, int column) {
        return results[row][column];
    }

    public String[] getRowTitles() {
        return rowTitles;
    }

    public void setRowTitles(String[] rowTitles) {
        this.rowTitles = rowTitles;
    }

    public String[] getColumnTitles() {
        return columnTitles;

    }

    public void setColumnTitles(String[] columnTitles) {
        this.columnTitles = columnTitles;
    }

    public void setFieldValue(String name, Object value) {
        if (fieldsCoordinates == null) { //Required if default constructor is used with setter methods.
            initFieldsCoordinates(); 
        }
        Point fieldCoordinates = fieldsCoordinates.get(name);

        if (fieldCoordinates != null) {
            setValue(fieldCoordinates.getRow(), fieldCoordinates.getColumn(), value);
        }
    }

    protected void setValue(int row, int column, Object value) {
        results[row][column] = value;
    }

    public String getColumnName(int column) {
        return columnNames[column];
    }

    public String getRowName(int row) {
        return rowNames[row];
    }

    public String getColumnTitle(int column) {
        return columnTitles[column];
    }

    public String getRowTitle(int row) {
        return rowTitles[row];
    }

    /**
     * 
     * @return logical representation of calculated spreadsheet table it is
     *         needed for web studio to display results
     */
    @XmlTransient
    public ILogicalTable getLogicalTable() {
        return logicalTable;
    }

    public void setLogicalTable(ILogicalTable logicalTable) {
        this.logicalTable = logicalTable;
    }

    public Object getFieldValue(String name) {
        if (fieldsCoordinates == null) { //Required if default constructor is used with setter methods.
            initFieldsCoordinates(); 
        }
        Point fieldCoordinates = fieldsCoordinates.get(name);

        if (fieldCoordinates != null) {
            return getValue(fieldCoordinates.getRow(), fieldCoordinates.getColumn());
        }
        return null;
    }

    public boolean hasField(String name) {
        if (fieldsCoordinates == null) { //Required if default constructor is used with setter methods.
            initFieldsCoordinates(); 
        }
        return fieldsCoordinates.get(name) != null;
    }

    public ITableAdaptor makeTableAdaptor() {
        return new ITableAdaptor() {
            // Huge SpreadSheetResult in text format is not human readable, can
            // crash browser,
            // slows down and consumes too many memory in IDE while debugging,
            // so we must truncate such tables.
            private static final int MAX_WIDTH = 10;
            private static final int MAX_HEIGHT = 10;

            public int width(int row) {
                return Math.min(MAX_WIDTH, getWidth() + 1);
            }

            public int maxWidth() {
                return Math.min(MAX_WIDTH, getWidth() + 1);
            }

            public int height() {
                return Math.min(MAX_HEIGHT, getHeight() + 1);
            }

            public Object get(int col, int row) {
                if (col == 0 && row == 0)
                    return "-X-";
                if (col == MAX_WIDTH - 1 && MAX_WIDTH <= getWidth() || row == MAX_HEIGHT - 1 && MAX_HEIGHT <= getHeight()) {
                    return "... TRUNCATED ...";
                }
                if (col == 0)
                    return getRowName(row - 1);
                if (row == 0)
                    return getColumnName(col - 1);

                return getValue(row - 1, col - 1);
            }
        };
    }

    public String printAsTable() {
        return new TablePrinter(makeTableAdaptor(), null, " | ").print();
    }

    @Override
    public String toString() {
        try {
            return printAsTable();
        } catch (Exception e) {
            // If it's impossible to print the table, fallback to default
            // toString() implementation
            return super.toString();
        }
    }
}
