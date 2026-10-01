package org.openl.rules.calc;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.UnaryOperator;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

import org.apache.commons.collections4.BidiMap;
import org.slf4j.LoggerFactory;

import org.openl.binding.impl.AllowOnlyStrictFieldMatchType;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.CustomJavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.CollectionUtils;

/**
 * Serializable bean that handles result of spreadsheet calculation.
 */
@XmlRootElement
@CustomJavaOpenClass(type = SpreadsheetResultOpenClass.class, variableInContextFinder = SpreadsheetResultRootDictionaryContext.class, normalize = true)
@AllowOnlyStrictFieldMatchType
public class SpreadsheetResult implements Serializable {

    private static final String TRUNCATED_TABLE = "... TRUNCATED TABLE ...";

    private static final int MAX_WIDTH = 4;
    private static final int MAX_HEIGHT = 10;
    private static final int MAX_DEPTH = 2;
    private static final int MAX_VALUE_LENGTH = 10 * 1024;

    // The cells hold whatever values the steps compute; the result serializes along whenever they do.
    @SuppressWarnings("java:S1948")
    Object[][] results;
    String[] columnNames;
    String[] rowNames;
    transient String[] rowNamesForResultModel;
    transient String[] columnNamesForResultModel;
    transient Map<String, Point> fieldsCoordinates;

    /**
     * logical representation of calculated spreadsheet table it is needed for web studio to display results
     */
    private transient ILogicalTable logicalTable;

    /**
     * Spreadsheet open class. This field is used for output bean generation.
     */
    private transient CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass;

    /**
     * Transient offset mappings for display purposes only.
     * Maps logical index to physical index (where physical includes description rows/columns).
     * These are not serialized and are only used for UI rendering.
     */
    private transient BidiMap<Integer, Integer> rowOffsets;
    private transient BidiMap<Integer, Integer> columnOffsets;

    public SpreadsheetResult() {
    }

    public SpreadsheetResult(Object[][] results,
                             String[] rowNames,
                             String[] columnNames,
                             String[] rowNamesForResultModel,
                             String[] columnNamesForResultModel,
                             Map<String, Point> fieldsCoordinates) {
        this.rowNames = Objects.requireNonNull(rowNames);
        this.columnNames = Objects.requireNonNull(columnNames);
        this.rowNamesForResultModel = Objects.requireNonNull(rowNamesForResultModel);
        this.columnNamesForResultModel = Objects.requireNonNull(columnNamesForResultModel);
        if (rowNames.length != rowNamesForResultModel.length) {
            throw new IllegalArgumentException(
                    "The length of rowNames is not equal to the length of rowNamesForResultModel.");
        }
        if (columnNames.length != columnNamesForResultModel.length) {
            throw new IllegalArgumentException(
                    "The length of columnNames is not equal to the length of columnNamesForResultModel.");
        }
        this.results = results;

        this.fieldsCoordinates = fieldsCoordinates;
    }

    public SpreadsheetResult(SpreadsheetResult spr) {
        this(spr.results,
                spr.rowNames,
                spr.columnNames,
                spr.rowNamesForResultModel,
                spr.columnNamesForResultModel,
                spr.fieldsCoordinates);
        this.logicalTable = spr.logicalTable;
        this.customSpreadsheetResultOpenClass = spr.customSpreadsheetResultOpenClass;
        this.rowOffsets = spr.rowOffsets;
        this.columnOffsets = spr.columnOffsets;
    }

    static Map<String, Point> buildFieldsCoordinates2(String[] columnNames,
                                                      String[] rowNames,
                                                      String[] modelColumnNames,
                                                      String[] modelRowNames) {
        var fieldsCoordinates = new HashMap<String, Point>();
        if (columnNames != null && rowNames != null) {
            putCellFieldsCoordinates(fieldsCoordinates, columnNames, rowNames);

            var index = getIndex(modelColumnNames == null ? columnNames : modelColumnNames);
            if (index >= 0) {
                putRowFieldsCoordinates(fieldsCoordinates, rowNames, index);
            }

            index = getIndex(modelRowNames == null ? rowNames : modelRowNames);
            if (index >= 0) {
                putColumnFieldsCoordinates(fieldsCoordinates, columnNames, index);
            }
        }
        return Map.copyOf(fieldsCoordinates);
    }

    private static void putRowFieldsCoordinates(Map<String, Point> fieldsCoordinates, String[] rowNames, int column) {
        for (var row = 0; row < rowNames.length; row++) {
            if (rowNames[row] != null) {
                fieldsCoordinates.put(ASpreadsheetField.createFieldName(null, rowNames[row]), Point.get(column, row));
            }
        }
    }

    private static void putColumnFieldsCoordinates(Map<String, Point> fieldsCoordinates,
                                                   String[] columnNames,
                                                   int row) {
        for (var column = 0; column < columnNames.length; column++) {
            if (columnNames[column] != null) {
                fieldsCoordinates.put(ASpreadsheetField.createFieldName(columnNames[column], null),
                        Point.get(column, row));
            }
        }
    }

    private static int getIndex(String[] names) {
        var index = -1;
        for (var i = 0; i < names.length; i++) {
            if (names[i] != null) {
                if (index >= 0) {
                    index = -1;
                    // The multiple references by name
                    break;
                }
                index = i;
            }
        }
        return index;
    }

    static Map<String, Point> buildFieldsCoordinates(String[] columnNames,
                                                     String[] rowNames,
                                                     boolean simpleRefByColumn,
                                                     boolean simpleRefByRow) {
        var fieldsCoordinates = new HashMap<String, Point>();
        if (columnNames != null && rowNames != null) {
            var nonNullsColumnsCount = Arrays.stream(columnNames).filter(Objects::nonNull).count();
            var nonNullsRowsCount = Arrays.stream(rowNames).filter(Objects::nonNull).count();
            var simpleRefByC = nonNullsColumnsCount == 1 || simpleRefByRow;
            var simpleRefByR = nonNullsRowsCount == 1 || simpleRefByColumn;
            putCellFieldsCoordinates(fieldsCoordinates, columnNames, rowNames);
            if (simpleRefByC) {
                putRowReferencesCoordinates(fieldsCoordinates, columnNames, rowNames);
            }
            if (simpleRefByR) {
                putColumnReferencesCoordinates(fieldsCoordinates, columnNames, rowNames);
            }
        }
        return fieldsCoordinates;
    }

    private static void putCellFieldsCoordinates(Map<String, Point> fieldsCoordinates,
                                                 String[] columnNames,
                                                 String[] rowNames) {
        for (var row = 0; row < rowNames.length; row++) {
            for (var column = 0; column < columnNames.length; column++) {
                if (columnNames[column] != null && rowNames[row] != null) {
                    fieldsCoordinates.put(ASpreadsheetField.createFieldName(columnNames[column], rowNames[row]),
                            Point.get(column, row));
                }
            }
        }
    }

    /**
     * Maps the references by a row name to the cells of the first column that has a name.
     */
    private static void putRowReferencesCoordinates(Map<String, Point> fieldsCoordinates,
                                                    String[] columnNames,
                                                    String[] rowNames) {
        for (var j = 0; j < columnNames.length; j++) {
            if (columnNames[j] != null) {
                for (var i = 0; i < rowNames.length; i++) {
                    if (rowNames[i] != null) {
                        fieldsCoordinates.put(SpreadsheetStructureBuilder.DOLLAR_SIGN + rowNames[i],
                                Point.get(j, i));
                    }
                }
                break;
            }
        }
    }

    /**
     * Maps the references by a column name to the cells of the first row that has a name.
     */
    private static void putColumnReferencesCoordinates(Map<String, Point> fieldsCoordinates,
                                                       String[] columnNames,
                                                       String[] rowNames) {
        for (var i = 0; i < rowNames.length; i++) {
            if (rowNames[i] != null) {
                for (var j = 0; j < columnNames.length; j++) {
                    if (columnNames[j] != null) {
                        fieldsCoordinates.put(SpreadsheetStructureBuilder.DOLLAR_SIGN + columnNames[j],
                                Point.get(j, i));
                    }
                }
                break;
            }
        }
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
        return columnNames != null ? columnNames.clone() : null;
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public String[] getRowNames() {
        return rowNames != null ? rowNames.clone() : null;
    }

    public void setRowNames(String[] rowNames) {
        this.rowNames = rowNames;
    }

    public Object getValue(int row, int column) {
        return results[row][column];
    }

    public Object getValue(String row, String column) {
        var p = getPoint(ASpreadsheetField.createFieldName(column, row));
        return p != null ? getValue(p.getRow(), p.getColumn()) : null;
    }

    public void setFieldValue(String name, Object value) {
        var fieldCoordinates = getPoint(name);

        if (fieldCoordinates != null) {
            setValue(fieldCoordinates.getRow(), fieldCoordinates.getColumn(), value);
        }
    }

    private Point getPoint(String name) {
        if (fieldsCoordinates == null) { // Required if default constructor is
            // used with setter methods.
            fieldsCoordinates = buildFieldsCoordinates2(columnNames, rowNames, columnNamesForResultModel, rowNamesForResultModel);
        }
        return fieldsCoordinates.get(name);
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
     * @return logical representation of calculated spreadsheet table it is needed for web studio to display results
     */
    @XmlTransient
    public ILogicalTable getLogicalTable() {
        return logicalTable;
    }

    public void setLogicalTable(ILogicalTable logicalTable) {
        this.logicalTable = logicalTable;
    }

    /**
     * Gets the row offset mapping for display purposes.
     * Maps logical row index to physical row index in the original spreadsheet.
     *
     * @return the row offset mapping, or null if not set
     */
    @XmlTransient
    public BidiMap<Integer, Integer> getRowOffsets() {
        return rowOffsets;
    }

    /**
     * Sets the row offset mapping for display purposes.
     * This is transient and only used for UI rendering.
     *
     * @param rowOffsets the row offset mapping
     */
    public void setRowOffsets(BidiMap<Integer, Integer> rowOffsets) {
        this.rowOffsets = rowOffsets;
    }

    /**
     * Gets the column offset mapping for display purposes.
     * Maps logical column index to physical column index in the original spreadsheet.
     *
     * @return the column offset mapping, or null if not set
     */
    @XmlTransient
    public BidiMap<Integer, Integer> getColumnOffsets() {
        return columnOffsets;
    }

    /**
     * Sets the column offset mapping for display purposes.
     * This is transient and only used for UI rendering.
     *
     * @param columnOffsets the column offset mapping
     */
    public void setColumnOffsets(BidiMap<Integer, Integer> columnOffsets) {
        this.columnOffsets = columnOffsets;
    }

    public Object getFieldValue(String name) {
        var fieldCoordinates = getPoint(name);

        if (fieldCoordinates != null) {
            return getValue(fieldCoordinates.getRow(), fieldCoordinates.getColumn());
        }
        return null;
    }

    /**
     * @see SpreadsheetResultBeanByteCodeGenerator
     */
    public Object getModelValue(String name) {
        var p = getPoint(name);

        if (p != null) {
            var column = columnNamesForResultModel[p.getColumn()];
            var row = rowNamesForResultModel[p.getRow()];
            if (column != null && row != null) {
                return getValue(p.getRow(), p.getColumn());
            }
        }
        return null;
    }

    public boolean hasField(String name) {
        return getPoint(name) != null;
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
            LoggerFactory.getLogger(getClass()).debug(e.getMessage(), e);
            return super.toString();
        }
    }

    private static final ThreadLocal<Integer> DEPTH_LOCAL_THREAD = new ThreadLocal<>();

    private String truncateStringValue(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() > MAX_VALUE_LENGTH) {
            return value.substring(0, MAX_VALUE_LENGTH) + " ... TRUNCATED ...";
        } else {
            return value;
        }
    }

    private String printTable() {
        var sb = new StringBuilder();
        var d = DEPTH_LOCAL_THREAD.get();
        d = d != null ? d : 0;
        try {
            DEPTH_LOCAL_THREAD.set(d + 1);
            var maxWidth = Math.min(MAX_WIDTH, getWidth());
            var maxHeight = Math.min(MAX_HEIGHT, getHeight());

            int[] width = getColumnWidths(maxWidth, maxHeight, d);

            for (var i = 0; i <= maxHeight; i++) {
                for (var j = 0; j <= maxWidth; j++) {
                    if (j != 0) {
                        sb.append(" | ");
                    }
                    String cell = getCellText(i, j, d);

                    sb.append(cell);
                    for (var k = 0; k < width[j] - cell.length(); k++) {
                        sb.append(' ');
                    }
                }
                sb.append('\n');
            }
            if (getWidth() > MAX_WIDTH || getHeight() > MAX_HEIGHT) {
                sb.append(TRUNCATED_TABLE);
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

    private int[] getColumnWidths(int maxWidth, int maxHeight, int depth) {
        int[] width = new int[maxWidth + 1];

        for (var i1 = 0; i1 <= maxHeight; i1++) {
            for (var j1 = 0; j1 <= maxWidth; j1++) {
                width[j1] = Math.max(width[j1], getCellText(i1, j1, depth).length());
            }
        }
        return width;
    }

    /**
     * Returns the truncated text of a table cell. A spreadsheet result nested deeper than the maximum depth is shown as
     * a truncated table.
     */
    private String getCellText(int row, int col, int depth) {
        if (row > 0 && col > 0 && getValue(row - 1, col - 1) instanceof SpreadsheetResult && depth > MAX_DEPTH) {
            return TRUNCATED_TABLE;
        }
        return truncateStringValue(getStringValue(col, row));
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

        var value = getValue(row - 1, col - 1);

        if (value == null) {
            return "";
        } else {
            String s = Arrays.deepToString(new Object[]{value});
            return s.substring(1, s.length() - 1);
        }
    }

    @XmlTransient
    public CustomSpreadsheetResultOpenClass getCustomSpreadsheetResultOpenClass() {
        return customSpreadsheetResultOpenClass;
    }

    public void setCustomSpreadsheetResultOpenClass(CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
        this.customSpreadsheetResultOpenClass = customSpreadsheetResultOpenClass;
    }

    public Map<String, Object> toMap() {
        return toMap(true, null);
    }

    public Map<String, Object> toMap(boolean spreadsheetResultsToMap,
                                     SpreadsheetResultBeanPropertyNamingStrategy spreadsheetResultBeanPropertyNamingStrategy) {
        var values = new HashMap<String, Object>();
        if (columnNames != null && rowNames != null) {
            var nonNullsColumnsCount = Arrays.stream(columnNamesForResultModel).filter(Objects::nonNull).count();
            var nonNullsRowsCount = Arrays.stream(rowNamesForResultModel).filter(Objects::nonNull).count();
            final var isSingleRow = nonNullsRowsCount == 1;
            final var isSingleColumn = nonNullsColumnsCount == 1;
            if (customSpreadsheetResultOpenClass != null) {
                Map<String, String> xmlNamesMap = customSpreadsheetResultOpenClass.getXmlNamesMap();
                for (Map.Entry<String, List<IOpenField>> e : customSpreadsheetResultOpenClass.getBeanFieldsMap()
                        .entrySet()) {
                    putBeanFieldValues(values,
                            e,
                            xmlNamesMap,
                            spreadsheetResultsToMap,
                            spreadsheetResultBeanPropertyNamingStrategy);
                }
            } else {
                putCellValues(values,
                        isSingleRow,
                        isSingleColumn,
                        spreadsheetResultsToMap,
                        spreadsheetResultBeanPropertyNamingStrategy);
            }
        }
        return values;
    }

    /**
     * Puts the values of the cells of a bean property. A value is put under its key when no other cell of the
     * property has the same key, and under the XML name of the property otherwise.
     */
    private void putBeanFieldValues(Map<String, Object> values,
                                    Entry<String, List<IOpenField>> e,
                                    Map<String, String> xmlNamesMap,
                                    boolean spreadsheetResultsToMap,
                                    SpreadsheetResultBeanPropertyNamingStrategy namingStrategy) {
        List<IOpenField> openFields = e.getValue();
        var p1 = new HashMap<String, Integer>();
        var points = new HashSet<Point>();
        for (IOpenField openField : openFields) {
            var p = getPoint(openField.getName());
            if (p != null && !points.contains(p) && columnNamesForResultModel[p
                    .getColumn()] != null && rowNamesForResultModel[p.getRow()] != null) {
                var key = getKey(namingStrategy, xmlNamesMap, e, p);
                p1.merge(key, 1, Integer::sum);
                points.add(p);
            }
        }
        for (IOpenField openField : openFields) {
            var p = getPoint(openField.getName());
            if (p != null && columnNamesForResultModel[p.getColumn()] != null && rowNamesForResultModel[p
                    .getRow()] != null) {
                var key = getKey(namingStrategy, xmlNamesMap, e, p);
                String fName;
                if (p1.get(key) == 1) {
                    fName = key;
                } else {
                    fName = xmlNamesMap.get(e.getKey());
                }
                values.put(fName,
                        convertSpreadsheetResult(getValue(p.getRow(), p.getColumn()),
                                spreadsheetResultsToMap,
                                namingStrategy));
            }
        }
    }

    /**
     * Puts the values of the cells that have both a row and a column name for the result model. A value is put under
     * a numbered name when its name is used already.
     */
    private void putCellValues(Map<String, Object> values,
                               boolean isSingleRow,
                               boolean isSingleColumn,
                               boolean spreadsheetResultsToMap,
                               SpreadsheetResultBeanPropertyNamingStrategy namingStrategy) {
        for (var i = 0; i < rowNamesForResultModel.length; i++) {
            for (var j = 0; j < columnNamesForResultModel.length; j++) {
                if (columnNamesForResultModel[j] != null && rowNamesForResultModel[i] != null) {
                    String fName = getCellValueName(i, j, isSingleRow, isSingleColumn, namingStrategy);
                    values.put(freeName(values, fName),
                            convertSpreadsheetResult(getValue(i, j),
                                    spreadsheetResultsToMap,
                                    namingStrategy));
                }
            }
        }
    }

    /**
     * Returns the name when the map has no such key yet. Otherwise returns the name followed by the first number that
     * makes it a new key.
     */
    private static String freeName(Map<String, ?> values, String name) {
        var candidate = name;
        var k = 1;
        while (values.containsKey(candidate)) {
            candidate = name + k;
            k++;
        }
        return candidate;
    }

    private String getCellValueName(int i,
                                    int j,
                                    boolean isSingleRow,
                                    boolean isSingleColumn,
                                    SpreadsheetResultBeanPropertyNamingStrategy namingStrategy) {
        if (isSingleColumn) {
            return namingStrategy == null ? rowNamesForResultModel[i]
                    : namingStrategy.transform(rowNamesForResultModel[i]);
        } else if (isSingleRow) {
            return namingStrategy == null ? columnNamesForResultModel[j]
                    : namingStrategy.transform(columnNamesForResultModel[j]);
        }
        return namingStrategy == null ? columnNamesForResultModel[j] + "_" + rowNamesForResultModel[i]
                : namingStrategy.transform(columnNamesForResultModel[j], rowNamesForResultModel[i]);
    }

    private String getKey(SpreadsheetResultBeanPropertyNamingStrategy spreadsheetResultBeanPropertyNamingStrategy,
                          Map<String, String> xmlNamesMap,
                          Entry<String, List<IOpenField>> e,
                          Point p) {
        String key;
        if (spreadsheetResultBeanPropertyNamingStrategy == null) {
            key = xmlNamesMap.get(e.getKey());
        } else {
            if (customSpreadsheetResultOpenClass.isSimpleRefByRow()) {
                key = spreadsheetResultBeanPropertyNamingStrategy.transform(rowNamesForResultModel[p.getRow()]);
            } else if (customSpreadsheetResultOpenClass.isSimpleRefByColumn()) {
                key = spreadsheetResultBeanPropertyNamingStrategy.transform(columnNamesForResultModel[p.getColumn()]);
            } else {
                key = spreadsheetResultBeanPropertyNamingStrategy.transform(columnNamesForResultModel[p.getColumn()],
                        rowNamesForResultModel[p.getRow()]);
            }
        }
        return key;
    }

    private static Object convertSpreadsheetResult(Object v,
                                                   boolean spreadsheetResultsToMap,
                                                   SpreadsheetResultBeanPropertyNamingStrategy spreadsheetResultBeanPropertyNamingStrategy) {
        return convertSpreadsheetResult(v,
                null,
                null,
                spreadsheetResultsToMap,
                spreadsheetResultBeanPropertyNamingStrategy);
    }

    public static Object convertSpreadsheetResult(Object v,
                                                  SpreadsheetResultBeanPropertyNamingStrategy spreadsheetResultBeanPropertyNamingStrategy) {
        return convertSpreadsheetResult(v, null, null, false, spreadsheetResultBeanPropertyNamingStrategy);
    }

    public static Object convertSpreadsheetResult(Object v,
                                                  Class<?> toType,
                                                  IOpenClass toTypeOpenClass,
                                                  SpreadsheetResultBeanPropertyNamingStrategy spreadsheetResultBeanPropertyNamingStrategy) {
        return convertSpreadsheetResult(v, toType, toTypeOpenClass, false, spreadsheetResultBeanPropertyNamingStrategy);
    }

    public static Object convertBeansToSpreadsheetResults(Object v,
                                                          Map<Class<?>, CustomSpreadsheetResultOpenClass> mapClassToSprOpenClass) {
        if (v == null) {
            return null;
        }
        if (v instanceof Collection<?> collection) {
            return convertCollection(collection,
                    e -> convertBeansToSpreadsheetResults(e, mapClassToSprOpenClass));
        }
        if (v instanceof Map<?, ?> map) {
            return convertMap(map, e -> convertBeansToSpreadsheetResults(e, mapClassToSprOpenClass));
        }
        if (v.getClass().isArray()) {
            return convertBeanArray(v, mapClassToSprOpenClass);
        }
        if (mapClassToSprOpenClass.containsKey(v.getClass())) {
            var customSpreadsheetResultOpenClass1 = mapClassToSprOpenClass
                    .get(v.getClass());
            return customSpreadsheetResultOpenClass1.createSpreadsheetResult(v, mapClassToSprOpenClass);
        }
        return v;
    }

    private static Object convertBeanArray(Object v,
                                           Map<Class<?>, CustomSpreadsheetResultOpenClass> mapClassToSprOpenClass) {
        Class<?> componentType = v.getClass().getComponentType();
        Class<?> t = v.getClass();
        while (t.isArray()) {
            t = t.getComponentType();
        }
        var len = Array.getLength(v);
        Object newArray = null;
        if (mapClassToSprOpenClass.containsKey(t)) {
            newArray = Array.newInstance(SpreadsheetResult.class, len);
        } else if (ClassUtils.isAssignable(t, Map.class) || ClassUtils.isAssignable(t, Collection.class)) {
            newArray = Array.newInstance(componentType, len);
        }
        if (newArray != null) {
            for (var i = 0; i < len; i++) {
                Array.set(newArray, i, convertBeansToSpreadsheetResults(Array.get(v, i), mapClassToSprOpenClass));
            }
            return newArray;
        }
        return v;
    }

    private static Object convertSpreadsheetResult(Object v,
                                                   Class<?> toType,
                                                   IOpenClass toTypeOpenClass,
                                                   boolean spreadsheetResultsToMap,
                                                   SpreadsheetResultBeanPropertyNamingStrategy spreadsheetResultBeanPropertyNamingStrategy) {
        if (v == null) {
            return null;
        }
        if (v instanceof Collection<?> collection) {
            return convertCollection(collection,
                    e -> convertSpreadsheetResult(e, spreadsheetResultsToMap, spreadsheetResultBeanPropertyNamingStrategy));
        }
        if (v instanceof Map<?, ?> map) {
            return convertMap(map,
                    e -> convertSpreadsheetResult(e, spreadsheetResultsToMap, spreadsheetResultBeanPropertyNamingStrategy));
        }
        if (v.getClass().isArray()) {
            return convertArray(v,
                    toType,
                    toTypeOpenClass,
                    spreadsheetResultsToMap,
                    spreadsheetResultBeanPropertyNamingStrategy);
        }
        if (v instanceof SpreadsheetResult spreadsheetResult) {
            return convertSpreadsheetResultToType(spreadsheetResult,
                    toType,
                    toTypeOpenClass,
                    spreadsheetResultsToMap,
                    spreadsheetResultBeanPropertyNamingStrategy);
        }
        return v;
    }

    private static Object convertArray(Object v,
                                       Class<?> toType,
                                       IOpenClass toTypeOpenClass,
                                       boolean spreadsheetResultsToMap,
                                       SpreadsheetResultBeanPropertyNamingStrategy namingStrategy) {
        Class<?> componentType = v.getClass().getComponentType();
        Class<?> t = v.getClass();
        while (t.isArray()) {
            t = t.getComponentType();
        }
        var len = Array.getLength(v);
        if (ClassUtils.isAssignable(t, SpreadsheetResult.class)) {
            return convertSpreadsheetResultArray(v,
                    len,
                    toType,
                    toTypeOpenClass,
                    spreadsheetResultsToMap,
                    namingStrategy);
        } else if (ClassUtils.isAssignable(SpreadsheetResult.class, t) || ClassUtils.isAssignable(t,
                Map.class) || ClassUtils.isAssignable(t, Collection.class)) {
            Object newArray = Array.newInstance(componentType, len);
            for (var i = 0; i < len; i++) {
                Array.set(newArray,
                        i,
                        convertSpreadsheetResult(Array.get(v, i),
                                componentType,
                                null,
                                spreadsheetResultsToMap,
                                namingStrategy));
            }
            return newArray;
        } else {
            return v;
        }
    }

    /**
     * Converts the elements of an array of spreadsheet results. The converted array has the component type of the
     * requested array type. When that type is {@code Object} or no array type is requested, the converted array has
     * the class shared by all its elements that are not null, if there is such a class.
     */
    private static Object convertSpreadsheetResultArray(Object v,
                                                        int len,
                                                        Class<?> toType,
                                                        IOpenClass toTypeOpenClass,
                                                        boolean spreadsheetResultsToMap,
                                                        SpreadsheetResultBeanPropertyNamingStrategy namingStrategy) {
        Object tmpArray = Array
                .newInstance(toType != null && toType.isArray() ? toType.getComponentType() : Object.class, len);
        for (var i = 0; i < len; i++) {
            Array.set(tmpArray,
                    i,
                    convertSpreadsheetResult(Array.get(v, i),
                            toType != null && toType.isArray() ? toType.getComponentType() : null,
                            toTypeOpenClass != null && toTypeOpenClass.isArray() ? toTypeOpenClass.getComponentClass()
                                    : null,
                            spreadsheetResultsToMap,
                            namingStrategy));
        }
        if (toType != null && toType.isArray() && Object.class != toType.getComponentType()) {
            return tmpArray;
        }
        var c = getCommonElementClass(tmpArray, len);
        if (c != null) {
            Object newArray = Array.newInstance(c, len);
            for (var i = 0; i < len; i++) {
                Array.set(newArray, i, Array.get(tmpArray, i));
            }
            return newArray;
        }
        return tmpArray;
    }

    /**
     * Returns the class of the array elements that are not null when all of them have the same class, or
     * {@code null} otherwise.
     */
    private static Class<?> getCommonElementClass(Object array, int len) {
        Class<?> c = null;
        var f = true;
        for (var i = 0; i < len; i++) {
            Object v1 = Array.get(array, i);
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
        return f ? c : null;
    }

    private static Object convertSpreadsheetResultToType(SpreadsheetResult spreadsheetResult,
                                                         Class<?> toType,
                                                         IOpenClass toTypeOpenClass,
                                                         boolean spreadsheetResultsToMap,
                                                         SpreadsheetResultBeanPropertyNamingStrategy namingStrategy) {
        if (toType != null && toType.isAnnotationPresent(SpreadsheetResultBeanClass.class)) {
            return CustomSpreadsheetResultOpenClass.createBean(toType, spreadsheetResult, namingStrategy);
        }
        if (Map.class == toType || spreadsheetResultsToMap) {
            return spreadsheetResult.toMap(spreadsheetResultsToMap, namingStrategy);
        } else if (toTypeOpenClass instanceof CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass
                && customSpreadsheetResultOpenClass.getBeanClass() == toType) {
            return customSpreadsheetResultOpenClass.createBean(spreadsheetResult,
                    namingStrategy);
        } else if (toTypeOpenClass instanceof SpreadsheetResultOpenClass class1 && class1
                .toCustomSpreadsheetResultOpenClass()
                .getBeanClass() == toType) {
            var customSpreadsheetResultOpenClass = class1
                    .toCustomSpreadsheetResultOpenClass();
            return customSpreadsheetResultOpenClass.createBean(spreadsheetResult,
                    namingStrategy);
        } else if (spreadsheetResult.getCustomSpreadsheetResultOpenClass() != null && toType == spreadsheetResult
                .getCustomSpreadsheetResultOpenClass()
                .getModule()
                .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                .toCustomSpreadsheetResultOpenClass()
                .getBeanClass()) {
            return spreadsheetResult.getCustomSpreadsheetResultOpenClass()
                    .getModule()
                    .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                    .toCustomSpreadsheetResultOpenClass()
                    .createBean(spreadsheetResult, namingStrategy);
        } else {
            if (spreadsheetResult.getCustomSpreadsheetResultOpenClass() != null) {
                return spreadsheetResult.getCustomSpreadsheetResultOpenClass().createBean(spreadsheetResult, null);
            } else {
                return spreadsheetResult.toMap(false, null);
            }
        }
    }

    private static Object convertMap(Map<?, ?> v, UnaryOperator<Object> function) {
        Map<Object, Object> newMap;
        try {
            newMap = v.getClass().getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            return v;
        }
        for (Entry<?, ?> e : v.entrySet()) {
            newMap.put(function.apply(e.getKey()), function.apply(e.getValue()));
        }
        return newMap;
    }

    private static Object convertCollection(Collection<?> v, UnaryOperator<Object> function) {
        Collection<Object> newCollection;
        try {
            newCollection = v.getClass().getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            return v;
        }
        for (Object o : v) {
            newCollection.add(function.apply(o));
        }
        return newCollection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        var that = (SpreadsheetResult) o;

        if (rowNames.length != that.rowNames.length) {
            return false;
        }
        if (columnNames.length != that.columnNames.length) {
            return false;
        }
        for (var row = 0; row < rowNames.length; row++) {
            for (var column = 0; column < columnNames.length; column++) {
                var v = getValue(row, column);
                var thatV = that.getValue(getRowName(row), getColumnName(column));
                if (!Objects.deepEquals(v, thatV)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        var hashCode = 0;
        for (var row : rowNames) {
            hashCode += Objects.hashCode(row);
        }
        for (var column : columnNames) {
            hashCode += Objects.hashCode(column);
        }
        return hashCode;
    }
}
