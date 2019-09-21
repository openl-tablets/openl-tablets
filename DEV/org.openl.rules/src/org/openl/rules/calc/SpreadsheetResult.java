package org.openl.rules.calc;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
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

    Object[][] results;
    String[] columnNames;
    String[] rowNames;
    transient String[] rowNamesMarkedWithAsterisk;
    transient String[] columnNamesMarkedWithAsterisk;
    transient Map<String, Point> fieldsCoordinates;

    boolean detailedPlainModel;

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
        this(results, rowNames, columnNames, new String[rowNames.length], new String[columnNames.length], null);
        initFieldsCoordinates();
    }

    public SpreadsheetResult(Object[][] results,
            String[] rowNames,
            String[] columnNames,
            String[] rowNamesMarkedWithAsterisk,
            String[] columnNamesMarkedWithAsterisk,
            Map<String, Point> fieldsCoordinates) {
        Objects.requireNonNull(rowNames);
        Objects.requireNonNull(columnNames);
        Objects.requireNonNull(rowNamesMarkedWithAsterisk);
        Objects.requireNonNull(columnNamesMarkedWithAsterisk);
        if (rowNames.length != rowNamesMarkedWithAsterisk.length) {
            throw new IllegalArgumentException(
                "The length of rowNames is not equal to the lenght of rowNamesMarkedWithAsterisk.");
        }
        if (columnNames.length != columnNamesMarkedWithAsterisk.length) {
            throw new IllegalArgumentException(
                "The length of columnNames is not equal to the lenght of columnNamesMarkedWithAsterisk.");
        }
        this.results = results;

        this.columnNames = columnNames;
        this.rowNames = rowNames;

        this.columnNamesMarkedWithAsterisk = columnNamesMarkedWithAsterisk;
        this.rowNamesMarkedWithAsterisk = rowNamesMarkedWithAsterisk;

        this.fieldsCoordinates = fieldsCoordinates;
    }

    public boolean isMarkedWithAsteriskField(String fieldName) {
        Point point = fieldsCoordinates.get(fieldName);
        if (point != null) {
            return columnNamesMarkedWithAsterisk[point.getColumn()] != null && rowNamesMarkedWithAsterisk[point
                .getRow()] != null;
        }
        return false;
    }

    static Map<String, Point> buildFieldsCoordinates(String[] columnNames, String[] rowNames) {
        Map<String, Point> fieldsCoordinates = new HashMap<>();
        if (columnNames != null && rowNames != null) {
            long nonNullsColumnsCount = Arrays.stream(columnNames).filter(Objects::nonNull).count();
            long nonNullsRowsCount = Arrays.stream(rowNames).filter(Objects::nonNull).count();
            for (int i = 0; i < rowNames.length; i++) {
                for (int j = 0; j < columnNames.length; j++) {
                    if (columnNames[j] != null && rowNames[i] != null) {
                        fieldsCoordinates.put(
                            SpreadsheetStructureBuilder.getSpreadsheetCellFieldName(columnNames[j], rowNames[i]),
                            new Point(j, i));
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
        return results;
    }

    public void setResults(Object[][] results) {
        this.results = results.clone();
    }

    @XmlTransient
    public int getWidth() {
        return columnNames.length;
    }

    public String[] getColumnNames() {
        return columnNames.clone();
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public String[] getRowNames() {
        return rowNames.clone();
    }

    public void setRowNames(String[] rowNames) {
        this.rowNames = rowNames;
    }

    @XmlTransient
    public String[] getRowNamesMarkedWithAsterisk() {
        return rowNamesMarkedWithAsterisk.clone();
    }

    public void setRowNamesMarkedWithAsterisk(String[] rowNamesMarkedWithAsterisk) {
        this.rowNamesMarkedWithAsterisk = rowNamesMarkedWithAsterisk;
    }

    @XmlTransient
    public String[] getColumnNamesMarkedWithAsterisk() {
        return columnNamesMarkedWithAsterisk.clone();
    }

    public void setColumnNamesMarkedWithAsterisk(String[] columnNamesMarkedWithAsterisk) {
        this.columnNamesMarkedWithAsterisk = columnNamesMarkedWithAsterisk;
    }

    @XmlTransient
    public boolean isDetailedPlainModel() {
        return detailedPlainModel;
    }
    
    public void setDetailedPlainModel(boolean detailedPlainModel) {
        this.detailedPlainModel = detailedPlainModel;
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
            return toMap();
        }
    }

    public Map<String, Object> toMap() throws InstantiationException, IllegalAccessException {
        return toMap(null);
    }

    public Object toPlain(XlsModuleOpenClass module) throws InstantiationException, IllegalAccessException {
        if (module == null) {
            return toMap(null);
        }
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

    public Map<String, Object> toMap(XlsModuleOpenClass module) throws InstantiationException, IllegalAccessException {
        Map<String, Object> values = new HashMap<>();
        if (columnNames != null && rowNames != null) {
            long nonNullsColumnsCount = Arrays.stream(columnNamesMarkedWithAsterisk).filter(Objects::nonNull).count();
            long nonNullsRowsCount = Arrays.stream(rowNamesMarkedWithAsterisk).filter(Objects::nonNull).count();
            String[][] fieldNames = detailedPlainModel ? new String[rowNames.length][columnNames.length] : null;
            if (customSpreadsheetResultOpenClass != null) {
                CustomSpreadsheetResultOpenClass csrt;
                if (module != null) {
                    csrt = (CustomSpreadsheetResultOpenClass) module
                        .findType(customSpreadsheetResultOpenClass.getName());
                } else {
                    csrt = customSpreadsheetResultOpenClass;
                }
                for (Map.Entry<String, List<IOpenField>> e : csrt.getBeanFieldsMap().entrySet()) {
                    List<IOpenField> openFields = e.getValue();
                    for (IOpenField openField : openFields) {
                        Point p = fieldsCoordinates.get(openField.getName());
                        if (p != null && columnNamesMarkedWithAsterisk[p
                            .getColumn()] != null && rowNamesMarkedWithAsterisk[p.getRow()] != null) {
                            values.put(e.getKey(),
                                convertSpreadsheetResults(module, getValue(p.getRow(), p.getColumn())));
                            if (detailedPlainModel) {
                                fieldNames[p.getRow()][p.getColumn()] = e.getKey();
                            }
                        }
                    }
                }
            } else {
                for (int i = 0; i < rowNamesMarkedWithAsterisk.length; i++) {
                    for (int j = 0; j < columnNamesMarkedWithAsterisk.length; j++) {
                        if (columnNamesMarkedWithAsterisk[j] != null && rowNamesMarkedWithAsterisk[i] != null) {
                            String fName = null;
                            if (nonNullsColumnsCount == 1) {
                                fName = rowNamesMarkedWithAsterisk[i];
                            } else if (nonNullsRowsCount == 1) {
                                fName = columnNamesMarkedWithAsterisk[j];
                            } else {
                                StringBuilder sb = new StringBuilder();
                                sb.append(columnNamesMarkedWithAsterisk[j])
                                    .append("_")
                                    .append(rowNamesMarkedWithAsterisk[i]);
                                fName = sb.toString();
                            }
                            values.put(fName, convertSpreadsheetResults(module, getValue(i, j)));
                            if (detailedPlainModel) {
                                fieldNames[i][j] = fName;
                            }
                        }
                    }
                }
            }
            if (detailedPlainModel) {
                values.put(values.containsKey("fieldNames") ? "$fieldNames" : "fieldNames", fieldNames);
                values.put(values.containsKey("rowNames") ? "$rowNames" : "rowNames", rowNames);
                values.put(values.containsKey("columnNames") ? "$columnNames" : "columnNames", columnNames);
            }
        }
        return values;
    }

    public static Object convertSpreadsheetResults(XlsModuleOpenClass module, Object v) throws InstantiationException,
                                                                                        IllegalAccessException {
        return convertSpreadsheetResults(module, v, null);
    }

    @SuppressWarnings("unchecked")
    public static Object convertSpreadsheetResults(XlsModuleOpenClass module,
            Object v,
            Class<?> type) throws InstantiationException, IllegalAccessException {
        if (v == null) {
            return null;
        }
        if (v instanceof Collection && !(v instanceof SortedSet)) {
            Collection<Object> collection = (Collection<Object>) v;
            Collection<Object> newCollection = (Collection<Object>) v.getClass().newInstance();
            for (Object o : collection) {
                newCollection.add(convertSpreadsheetResults(module, o));
            }
            return newCollection;
        }
        if (v instanceof Map && !(v instanceof SortedMap)) {
            Map<Object, Object> map = (Map<Object, Object>) v;
            Map<Object, Object> newMap = (Map<Object, Object>) v.getClass().newInstance();
            for (Entry<Object, Object> e : map.entrySet()) {
                newMap.put(convertSpreadsheetResults(module, e.getKey()),
                    convertSpreadsheetResults(module, e.getValue()));
            }
            return newMap;
        }
        if (v.getClass().isArray()) {
            Class<?> componentType = v.getClass().getComponentType();
            Class<?> t = v.getClass();
            while (t.isArray()) {
                t = t.getComponentType();
            }
            int len = Array.getLength(v);
            if (SpreadsheetResult.class.isAssignableFrom(t)) {
                Object tmpArray = Array
                    .newInstance(type != null && type.isArray() ? type.getComponentType() : Object.class, len);
                for (int i = 0; i < len; i++) {
                    Array.set(tmpArray,
                        i,
                        convertSpreadsheetResults(module,
                            Array.get(v, i),
                            type != null && type.isArray() ? type.getComponentType() : null));
                }
                if (type != null && type.isArray() && !Object.class.equals(type.getComponentType())) {
                    return tmpArray;
                }
                Class<?> c = null;
                boolean f = true;
                for (int i = 0; i < len; i++) {
                    Object v1 = Array.get(tmpArray, i);
                    if (v1 != null) {
                        if (c == null) {
                            c = v1.getClass();
                        } else {
                            if (!c.equals(v1.getClass())) {
                                f = false;
                            }
                        }
                    }
                }
                if (f && c != null) {
                    Object newArray = Array.newInstance(c, len);
                    for (int i = 0; i < len; i++) {
                        Array.set(newArray, i, Array.get(tmpArray, i));
                    }
                    return newArray;
                }
                return tmpArray;
            } else if (Object.class
                .equals(t) || Map.class.isAssignableFrom(t) && !SortedMap.class.isAssignableFrom(t) || Collection.class
                    .isAssignableFrom(t) && !SortedSet.class.isAssignableFrom(t)) {
                Object newArray = Array.newInstance(componentType, len);
                for (int i = 0; i < len; i++) {
                    Array.set(newArray, i, convertSpreadsheetResults(module, Array.get(v, i)));
                }
                return newArray;
            } else {
                return v;
            }
        }
        if (v instanceof SpreadsheetResult) {
            if (Map.class.equals(type)) {
                return ((SpreadsheetResult) v).toMap(module);
            } else {
                return ((SpreadsheetResult) v).toPlain(module);
            }
        }
        return v;
    }
}
