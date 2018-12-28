package org.openl.rules.calc;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;
import org.openl.types.java.CustomJavaOpenClass;

/**
 * Serializable bean that handles result of spreadsheet calculation.
 */
@XmlRootElement
@CustomJavaOpenClass(type = SpreadsheetResultOpenClass.class, variableInContextFinder = SpreadsheetResultRootDictionaryContext.class)
public class SpreadsheetResult implements Serializable {

    private static final long serialVersionUID = 8704762477153429384L;
    private static final int MAX_WIDTH = 4;
    private static final int MAX_HEIGHT = 10;

    private Object[][] results;
    private String[] columnNames;
    private String[] rowNames;
    private transient Map<String, Point> fieldsCoordinates = null;

    /**
     * logical representation of calculated spreadsheet table it is needed for web studio to display results
     */
    private transient ILogicalTable logicalTable;

    public SpreadsheetResult() {
    }

    public SpreadsheetResult(Object[][] results, String[] rowNames, String[] columnNames) {
        this(results, rowNames, columnNames, null);
        initFieldsCoordinates();
    }

    public SpreadsheetResult(Object[][] results,
            String[] rowNames,
            String[] columnNames,
            Map<String, Point> fieldsCoordinates) {
        this.columnNames = columnNames.clone();
        this.rowNames = rowNames.clone();
        this.results = results;
        this.fieldsCoordinates = fieldsCoordinates;
    }

    static Map<String, Point> buildFieldsCoordinates(String[] columnNames, String[] rowNames) {
        Map<String, Point> fieldsCoordinates = new HashMap<String, Point>();
        for (int i = 0; i < rowNames.length; i++) {
            for (int j = 0; j < columnNames.length; j++) {
                StringBuilder sb = new StringBuilder();
                sb.append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                    .append(columnNames[j])
                    .append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                    .append(rowNames[i]);
                fieldsCoordinates.put(sb.toString(), new Point(j, i));
            }
        }
        return fieldsCoordinates;
    }

    private void initFieldsCoordinates() {
        this.fieldsCoordinates = buildFieldsCoordinates(columnNames, rowNames);
    }

    @XmlTransient
    public int getHeight() {
        return rowNames.length;
    }

    public Object[][] getResults() {
        return results.clone();
    }

    public void setResults(Object[][] results) {
        this.results = results.clone();
    }

    @XmlTransient
    public int getWidth() {
        return columnNames.length;
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

    public void setFieldValue(String name, Object value) {
        if (fieldsCoordinates == null) { // Required if default constructor is
                                         // used with setter methods.
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

    /**
     * 
     * @return logical representation of calculated spreadsheet table it is needed for web studio to display results
     */
    @XmlTransient
    public ILogicalTable getLogicalTable() {
        return logicalTable;
    }

    public void setLogicalTable(ILogicalTable logicalTable) {
        this.logicalTable = logicalTable;
    }

    public Object getFieldValue(String name) {
        if (fieldsCoordinates == null) { // Required if default constructor is
                                         // used with setter methods.
            initFieldsCoordinates();
        }
        Point fieldCoordinates = fieldsCoordinates.get(name);

        if (fieldCoordinates != null) {
            return getValue(fieldCoordinates.getRow(), fieldCoordinates.getColumn());
        }
        return null;
    }

    public boolean hasField(String name) {
        if (fieldsCoordinates == null) { // Required if default constructor is
                                         // used with setter methods.
            initFieldsCoordinates();
        }
        return fieldsCoordinates.get(name) != null;
    }

    @Override
    public String toString() {
        try {
            return printTable();
        } catch (Exception e) {
            // If it's impossible to print the table, fallback to default
            // toString() implementation
            return super.toString();
        }
    }

    private String printTable() {
        StringBuilder sb = new StringBuilder();
        int maxWidth = Math.min(MAX_WIDTH, getWidth());
        int maxHeight = Math.min(MAX_HEIGHT, getHeight());

        int[] width = new int[maxWidth + 1];

        for (int i1 = 0; i1 <= maxHeight; i1++) {
            for (int j1 = 0; j1 <= maxWidth; j1++) {
                width[j1] = Math.max(width[j1], getStringValue(j1, i1).length());
            }
        }

        for (int i = 0; i <= maxHeight; i++) {
            for (int j = 0; j <= maxWidth; j++) {
                if (j != 0) {
                    sb.append(" | ");
                }
                String cell = getStringValue(j, i);
                sb.append(cell);
                for (int k = 0; k < width[j] - cell.length(); k++) {
                    sb.append(' ');
                }
            }
            sb.append('\n');
        }
        if (getWidth() > MAX_WIDTH || getHeight() > MAX_HEIGHT) {
            sb.append("... TRUNCATED TABLE ...");
        }

        return sb.toString();
    }

    private String getStringValue(int col, int row) {
        if (col == 0 && row == 0)
            return "-X-";
        if (col == 0)
            return getRowName(row - 1);
        if (row == 0)
            return getColumnName(col - 1);

        Object value = getValue(row - 1, col - 1);
        return value == null ? "" : String.valueOf(value);
    }
}
