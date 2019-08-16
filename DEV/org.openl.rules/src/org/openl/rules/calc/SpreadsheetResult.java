package org.openl.rules.calc;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;
import org.openl.types.IOpenClass;
import org.openl.types.java.CustomJavaOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

/**
 * Serializable bean that handles result of spreadsheet calculation.
 */
@XmlRootElement
@CustomJavaOpenClass(type = SpreadsheetResultOpenClass.class, variableInContextFinder = SpreadsheetResultRootDictionaryContext.class)
public class SpreadsheetResult implements Serializable {

    private static final long serialVersionUID = 8704762477153429384L;
    private static final int MAX_WIDTH = 4;
    private static final int MAX_HEIGHT = 10;
    private static final int MAX_DEPTH = 2;
    private static final int MAX_VALUE_LENGTH = 10 * 1024;

    private Object[][] results;
    private String[] columnNames;
    private String[] rowNames;
    private transient Map<String, Point> fieldsCoordinates = null;

    /**
     * logical representation of calculated spreadsheet table it is needed for web studio to display results
     */
    private transient ILogicalTable logicalTable;

    /**
     * Spreadsheet open class. This filed is used for output bean generation.
     */
    private transient CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass;

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
        Map<String, Point> fieldsCoordinates = new HashMap<>();
        if (columnNames != null && rowNames != null) {
            long nonNullsColumnsCount = Arrays.stream(columnNames).filter(Objects::nonNull).count();
            long nonNullsRowsCount = Arrays.stream(rowNames).filter(Objects::nonNull).count();
            for (int i = 0; i < rowNames.length; i++) {
                for (int j = 0; j < columnNames.length; j++) {
                    if (columnNames[j] != null && rowNames[i] != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                            .append(columnNames[j])
                            .append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                            .append(rowNames[i]);
                        fieldsCoordinates.put(sb.toString(), new Point(j, i));
                        if (nonNullsRowsCount == 1) {
                            StringBuilder sb1 = new StringBuilder();
                            sb1.append(SpreadsheetStructureBuilder.DOLLAR_SIGN).append(columnNames[j]);
                            fieldsCoordinates.put(sb1.toString(), new Point(j, i));
                        }
                        if (nonNullsColumnsCount == 1) {
                            StringBuilder sb1 = new StringBuilder();
                            sb1.append(SpreadsheetStructureBuilder.DOLLAR_SIGN).append(rowNames[i]);
                            fieldsCoordinates.put(sb1.toString(), new Point(j, i));
                        }
                    }
                }
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
            if (CollectionUtils.isEmpty(rowNames) || CollectionUtils.isEmpty(columnNames)) {
                return "[EMPTY]";
            } else {
                return printTable();
            }
        } catch (Exception e) {
            // If it's impossible to print the table, fallback to default
            // toString() implementation
            return super.toString();
        }
    }

    private static final ThreadLocal<Integer> DEPTH_LOCAL_THREAD = new ThreadLocal<>();

    private String truncateStringValue(String value) {
        if (value.length() > MAX_VALUE_LENGTH) {
            return value.substring(0, MAX_VALUE_LENGTH) + " ... TRUNCATED ...";
        } else {
            return value;
        }
    }

    private String printTable() {
        StringBuilder sb = new StringBuilder();
        Integer d = DEPTH_LOCAL_THREAD.get();
        d = d != null ? d : 0;
        try {
            DEPTH_LOCAL_THREAD.set(d + 1);
            int maxWidth = Math.min(MAX_WIDTH, getWidth());
            int maxHeight = Math.min(MAX_HEIGHT, getHeight());

            int[] width = new int[maxWidth + 1];

            for (int i1 = 0; i1 <= maxHeight; i1++) {
                for (int j1 = 0; j1 <= maxWidth; j1++) {
                    width[j1] = Math.max(width[j1],
                        i1 > 0 && j1 > 0 && (getValue(i1 - 1,
                            j1 - 1) instanceof SpreadsheetResult) && d > MAX_DEPTH ? "... TRUNCATED TABLE ..."
                                .length() : truncateStringValue(getStringValue(j1, i1)).length());
                }
            }

            for (int i = 0; i <= maxHeight; i++) {
                for (int j = 0; j <= maxWidth; j++) {
                    if (j != 0) {
                        sb.append(" | ");
                    }
                    String cell;
                    if (i > 0 && j > 0 && (getValue(i - 1, j - 1) instanceof SpreadsheetResult) && d > MAX_DEPTH) {
                        cell = "... TRUNCATED TABLE ...";
                    } else {
                        cell = truncateStringValue(getStringValue(j, i));
                    }

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
        } finally {
            if (d == 0) {
                DEPTH_LOCAL_THREAD.remove();
            } else {
                DEPTH_LOCAL_THREAD.set(d);
            }
        }

        return sb.toString();
    }

    private String getStringValue(int col, int row) {
        if (col == 0 && row == 0) {
            return "-X-";
        }
        if (col == 0) {
            return getRowName(row - 1);
        }
        if (row == 0) {
            return getColumnName(col - 1);
        }

        Object value = getValue(row - 1, col - 1);
        StringBuilder builder = new StringBuilder(10);

        StringUtils.print(value, builder);
        return builder.toString();
    }

    @XmlTransient
    public CustomSpreadsheetResultOpenClass getCustomSpreadsheetResultOpenClass() {
        return customSpreadsheetResultOpenClass;
    }

    public void setCustomSpreadsheetResultOpenClass(CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
        this.customSpreadsheetResultOpenClass = customSpreadsheetResultOpenClass;
    }

    public Object toPlain() throws InstantiationException, IllegalAccessException {
        if (getCustomSpreadsheetResultOpenClass() != null) {
            return toPlain(getCustomSpreadsheetResultOpenClass().getModule());
        } else {
            return toMap(null);
        }
    }

    public Object toPlain(XlsModuleOpenClass module) throws InstantiationException, IllegalAccessException {
        if (getCustomSpreadsheetResultOpenClass() != null) {
            IOpenClass openClass = module.findType(getCustomSpreadsheetResultOpenClass().getName());
            if (openClass instanceof CustomSpreadsheetResultOpenClass) {
                return ((CustomSpreadsheetResultOpenClass) openClass).createBean(this);
            } else {
                throw new IllegalStateException(
                    String.format("Custom spreadsheet type with name '%s' is not found in the module.",
                        getCustomSpreadsheetResultOpenClass().getName()));
            }
        } else {
            return toMap(module);
        }
    }

    private Map<String, Object> toMap(XlsModuleOpenClass module) throws InstantiationException, IllegalAccessException {
        Map<String, Object> values = new HashMap<>();
        if (columnNames != null && rowNames != null) {
            long nonNullsColumnsCount = Arrays.stream(columnNames).filter(Objects::nonNull).count();
            long nonNullsRowsCount = Arrays.stream(rowNames).filter(Objects::nonNull).count();
            for (int i = 0; i < rowNames.length; i++) {
                for (int j = 0; j < columnNames.length; j++) {
                    if (columnNames[j] != null && rowNames[i] != null) {
                        if (nonNullsColumnsCount == 1 || nonNullsRowsCount == 1) {
                            if (nonNullsRowsCount == 1) {
                                values.put(columnNames[j], ifSpreadsheerResultThenConvert(module, getValue(i, j)));
                            }
                            if (nonNullsColumnsCount == 1) {
                                values.put(rowNames[i], ifSpreadsheerResultThenConvert(module, getValue(i, j)));
                            }
                        } else {
                            StringBuilder sb = new StringBuilder();
                            sb.append(columnNames[j]).append("_").append(rowNames[i]);
                            values.put(sb.toString(), ifSpreadsheerResultThenConvert(module, getValue(i, j)));
                        }
                    }
                }
            }
        }
        return values;
    }

    private Object ifSpreadsheerResultThenConvert(XlsModuleOpenClass module, Object v) throws InstantiationException,
                                                                                       IllegalAccessException {
        if (v instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<Object> collection = (Collection<Object>) v;
            for (Object o : collection) {
                if (o instanceof SpreadsheetResult) {
                    @SuppressWarnings("unchecked")
                    Collection<Object> newCollection = (Collection<Object>) v.getClass().newInstance();
                    for (Object o1 : collection) {
                        newCollection.add(ifSpreadsheerResultThenConvert(module, o1));
                    }
                    return newCollection;
                }
            }
        }
        if (v instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) v;
            for (Entry<Object, Object> e : map.entrySet()) {
                if (e.getKey() instanceof SpreadsheetResult || e.getValue() instanceof SpreadsheetResult) {
                    @SuppressWarnings("unchecked")
                    Map<Object, Object> newMap = (Map<Object, Object>) v.getClass().newInstance();
                    for (Entry<Object, Object> e1 : map.entrySet()) {
                        newMap.put(ifSpreadsheerResultThenConvert(module, e1.getKey()),
                            ifSpreadsheerResultThenConvert(module, e1.getValue()));
                    }
                    return newMap;
                }
            }
        }
        if (v instanceof SpreadsheetResult) {
            return ((SpreadsheetResult) v).toPlain(module);
        }
        return v;
    }
}
