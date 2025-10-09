package org.openl.rules.calc;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;

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

    private static final int MAX_WIDTH = 4;
    private static final int MAX_HEIGHT = 10;
    private static final int MAX_DEPTH = 2;
    private static final int MAX_VALUE_LENGTH = 10 * 1024;

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
    }

    static Map<String, Point> buildFieldsCoordinates2(String[] columnNames,
                                                      String[] rowNames,
                                                      String[] modelColumnNames,
                                                      String[] modelRowNames) {
        Map<String, Point> fieldsCoordinates = new HashMap<>();
        if (columnNames != null && rowNames != null) {
            for (int row = 0; row < rowNames.length; row++) {
                for (int column = 0; column < columnNames.length; column++) {
                    if (columnNames[column] != null && rowNames[row] != null) {
                        fieldsCoordinates.put(ASpreadsheetField.createFieldName(columnNames[column], rowNames[row]), Point.get(column, row));
                    }
                }
            }

            int index = getIndex(modelColumnNames == null ? columnNames : modelColumnNames);
            if (index >= 0) {
                for (int row = 0; row < rowNames.length; row++) {
                    if (rowNames[row] != null) {
                        fieldsCoordinates.put(ASpreadsheetField.createFieldName(null, rowNames[row]), Point.get(index, row));
                    }
                }
            }

            index = getIndex(modelRowNames == null ? rowNames : modelRowNames);
            if (index >= 0) {
                for (int column = 0; column < columnNames.length; column++) {
                    if (columnNames[column] != null) {
                        fieldsCoordinates.put(ASpreadsheetField.createFieldName(columnNames[column], null), Point.get(column, index));
                    }

                }
            }
        }
        return Map.copyOf(fieldsCoordinates);
    }

    private static int getIndex(String[] names) {
        int index = -1;
        for (int i = 0; i < names.length; i++) {
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
        Map<String, Point> fieldsCoordinates = new HashMap<>();
        if (columnNames != null && rowNames != null) {
            long nonNullsColumnsCount = Arrays.stream(columnNames).filter(Objects::nonNull).count();
            long nonNullsRowsCount = Arrays.stream(rowNames).filter(Objects::nonNull).count();
            boolean simpleRefByC = nonNullsColumnsCount == 1 || simpleRefByRow;
            boolean simpleRefByR = nonNullsRowsCount == 1 || simpleRefByColumn;
            for (int i = 0; i < rowNames.length; i++) {
                for (int j = 0; j < columnNames.length; j++) {
                    if (columnNames[j] != null && rowNames[i] != null) {
                        fieldsCoordinates.put(
                                ASpreadsheetField.createFieldName(columnNames[j], rowNames[i]),
                                Point.get(j, i));
                    }
                }
            }
            if (simpleRefByC) {
                for (int j = 0; j < columnNames.length; j++) {
                    if (columnNames[j] != null) {
                        for (int i = 0; i < rowNames.length; i++) {
                            if (rowNames[i] != null) {
                                fieldsCoordinates.put(SpreadsheetStructureBuilder.DOLLAR_SIGN + rowNames[i],
                                        Point.get(j, i));
                            }
                        }
                        break;
                    }
                }
            }
            if (simpleRefByR) {
                for (int i = 0; i < rowNames.length; i++) {
                    if (rowNames[i] != null) {
                        for (int j = 0; j < columnNames.length; j++) {
                            if (columnNames[j] != null) {
                                fieldsCoordinates.put(SpreadsheetStructureBuilder.DOLLAR_SIGN + columnNames[j],
                                        Point.get(j, i));
                            }
                        }
                        break;
                    }
                }
            }
        }
        return fieldsCoordinates;
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
        Point fieldCoordinates = getPoint(name);

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

    public Object getFieldValue(String name) {
        Point fieldCoordinates = getPoint(name);

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
                Object result = getValue(p.getRow(), p.getColumn());
                return result;
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
                            i1 > 0 && j1 > 0 && getValue(i1 - 1,
                                    j1 - 1) instanceof SpreadsheetResult && d > MAX_DEPTH ? "... TRUNCATED TABLE ..."
                                    .length() : truncateStringValue(getStringValue(j1, i1)).length());
                }
            }

            for (int i = 0; i <= maxHeight; i++) {
                for (int j = 0; j <= maxWidth; j++) {
                    if (j != 0) {
                        sb.append(" | ");
                    }
                    String cell;
                    if (i > 0 && j > 0 && getValue(i - 1, j - 1) instanceof SpreadsheetResult && d > MAX_DEPTH) {
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
        Map<String, Object> values = new HashMap<>();
        if (columnNames != null && rowNames != null) {
            long nonNullsColumnsCount = Arrays.stream(columnNamesForResultModel).filter(Objects::nonNull).count();
            long nonNullsRowsCount = Arrays.stream(rowNamesForResultModel).filter(Objects::nonNull).count();
            final boolean isSingleRow = nonNullsRowsCount == 1;
            final boolean isSingleColumn = nonNullsColumnsCount == 1;
            if (customSpreadsheetResultOpenClass != null) {
                Map<String, String> xmlNamesMap = customSpreadsheetResultOpenClass.getXmlNamesMap();
                for (Map.Entry<String, List<IOpenField>> e : customSpreadsheetResultOpenClass.getBeanFieldsMap()
                        .entrySet()) {
                    List<IOpenField> openFields = e.getValue();
                    Map<String, Integer> p1 = new HashMap<>();
                    Set<Point> points = new HashSet<>();
                    for (IOpenField openField : openFields) {
                        Point p = getPoint(openField.getName());
                        if (p != null && !points.contains(p) && columnNamesForResultModel[p
                                .getColumn()] != null && rowNamesForResultModel[p.getRow()] != null) {
                            String key = getKey(spreadsheetResultBeanPropertyNamingStrategy, xmlNamesMap, e, p);
                            p1.merge(key, 1, Integer::sum);
                            points.add(p);
                        }
                    }
                    for (IOpenField openField : openFields) {
                        Point p = getPoint(openField.getName());
                        if (p != null && columnNamesForResultModel[p.getColumn()] != null && rowNamesForResultModel[p
                                .getRow()] != null) {
                            String key = getKey(spreadsheetResultBeanPropertyNamingStrategy, xmlNamesMap, e, p);
                            String fName;
                            if (p1.get(key) == 1) {
                                fName = key;
                            } else {
                                fName = xmlNamesMap.get(e.getKey());
                            }
                            values.put(fName,
                                    convertSpreadsheetResult(getValue(p.getRow(), p.getColumn()),
                                            spreadsheetResultsToMap,
                                            spreadsheetResultBeanPropertyNamingStrategy));
                        }
                    }
                }
            } else {
                for (int i = 0; i < rowNamesForResultModel.length; i++) {
                    for (int j = 0; j < columnNamesForResultModel.length; j++) {
                        if (columnNamesForResultModel[j] != null && rowNamesForResultModel[i] != null) {
                            String fName;
                            if (isSingleColumn) {
                                fName = spreadsheetResultBeanPropertyNamingStrategy == null ? rowNamesForResultModel[i]
                                        : spreadsheetResultBeanPropertyNamingStrategy
                                        .transform(
                                                rowNamesForResultModel[i]);
                            } else {
                                if (isSingleRow) {
                                    fName = spreadsheetResultBeanPropertyNamingStrategy == null ? columnNamesForResultModel[j]
                                            : spreadsheetResultBeanPropertyNamingStrategy
                                            .transform(
                                                    columnNamesForResultModel[j]);
                                } else {
                                    fName = spreadsheetResultBeanPropertyNamingStrategy == null ? columnNamesForResultModel[j] + "_" + rowNamesForResultModel[i]
                                            : spreadsheetResultBeanPropertyNamingStrategy
                                            .transform(
                                                    columnNamesForResultModel[j],
                                                    rowNamesForResultModel[i]);
                                }
                            }
                            String fNewName = fName;
                            int k = 1;
                            while (values.containsKey(fNewName)) {
                                fNewName = fName + k;
                                k++;
                            }
                            values.put(fNewName,
                                    convertSpreadsheetResult(getValue(i, j),
                                            spreadsheetResultsToMap,
                                            spreadsheetResultBeanPropertyNamingStrategy));
                        }
                    }
                }
            }
        }
        return values;
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
        if (v instanceof SpreadsheetResult) {
            SpreadsheetResult spreadsheetResult = (SpreadsheetResult) v;
            if (spreadsheetResult.getCustomSpreadsheetResultOpenClass() == null) {
                CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = spreadsheetResult
                        .getCustomSpreadsheetResultOpenClass()
                        .getModule()
                        .getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                        .toCustomSpreadsheetResultOpenClass();
                return convertSpreadsheetResult(v,
                        customSpreadsheetResultOpenClass.getBeanClass(),
                        customSpreadsheetResultOpenClass,
                        spreadsheetResultsToMap,
                        spreadsheetResultBeanPropertyNamingStrategy);
            } else {
                return convertSpreadsheetResult(v,
                        null,
                        null,
                        spreadsheetResultsToMap,
                        spreadsheetResultBeanPropertyNamingStrategy);
            }
        }
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
        if (v instanceof Collection) {
            return convertCollection((Collection<?>) v,
                    e -> convertBeansToSpreadsheetResults(e, mapClassToSprOpenClass));
        }
        if (v instanceof Map) {
            return convertMap((Map<?, ?>) v, e -> convertBeansToSpreadsheetResults(e, mapClassToSprOpenClass));
        }
        if (v.getClass().isArray()) {
            Class<?> componentType = v.getClass().getComponentType();
            Class<?> t = v.getClass();
            while (t.isArray()) {
                t = t.getComponentType();
            }
            int len = Array.getLength(v);
            Object newArray = null;
            if (mapClassToSprOpenClass.containsKey(t)) {
                newArray = Array.newInstance(SpreadsheetResult.class, len);
            } else if (ClassUtils.isAssignable(t, Map.class) || ClassUtils.isAssignable(t, Collection.class)) {
                newArray = Array.newInstance(componentType, len);
            }
            if (newArray != null) {
                for (int i = 0; i < len; i++) {
                    Array.set(newArray, i, convertBeansToSpreadsheetResults(Array.get(v, i), mapClassToSprOpenClass));
                }
                return newArray;
            }
            return v;
        }
        if (mapClassToSprOpenClass.containsKey(v.getClass())) {
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass1 = mapClassToSprOpenClass
                    .get(v.getClass());
            return customSpreadsheetResultOpenClass1.createSpreadsheetResult(v, mapClassToSprOpenClass);
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
        if (v instanceof Collection) {
            return convertCollection((Collection<?>) v,
                    e -> convertSpreadsheetResult(e, spreadsheetResultsToMap, spreadsheetResultBeanPropertyNamingStrategy));
        }
        if (v instanceof Map) {
            return convertMap((Map<?, ?>) v,
                    e -> convertSpreadsheetResult(e, spreadsheetResultsToMap, spreadsheetResultBeanPropertyNamingStrategy));
        }
        if (v.getClass().isArray()) {
            Class<?> componentType = v.getClass().getComponentType();
            Class<?> t = v.getClass();
            while (t.isArray()) {
                t = t.getComponentType();
            }
            int len = Array.getLength(v);
            if (ClassUtils.isAssignable(t, SpreadsheetResult.class)) {
                Object tmpArray = Array
                        .newInstance(toType != null && toType.isArray() ? toType.getComponentType() : Object.class, len);
                for (int i = 0; i < len; i++) {
                    Array.set(tmpArray,
                            i,
                            convertSpreadsheetResult(Array.get(v, i),
                                    toType != null && toType.isArray() ? toType.getComponentType() : null,
                                    toTypeOpenClass != null && toTypeOpenClass.isArray() ? toTypeOpenClass.getComponentClass()
                                            : null,
                                    spreadsheetResultsToMap,
                                    spreadsheetResultBeanPropertyNamingStrategy));
                }
                if (toType != null && toType.isArray() && Object.class != toType.getComponentType()) {
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
            } else if (ClassUtils.isAssignable(SpreadsheetResult.class, t) || ClassUtils.isAssignable(t,
                    Map.class) || ClassUtils.isAssignable(t, Collection.class)) {
                Object newArray = Array.newInstance(componentType, len);
                for (int i = 0; i < len; i++) {
                    Array.set(newArray,
                            i,
                            convertSpreadsheetResult(Array.get(v, i),
                                    componentType,
                                    null,
                                    spreadsheetResultsToMap,
                                    spreadsheetResultBeanPropertyNamingStrategy));
                }
                return newArray;
            } else {
                return v;
            }
        }
        if (v instanceof SpreadsheetResult) {
            SpreadsheetResult spreadsheetResult = (SpreadsheetResult) v;
            if (toType != null && toType.isAnnotationPresent(SpreadsheetResultBeanClass.class)) {
                return CustomSpreadsheetResultOpenClass.createBean(toType, spreadsheetResult, spreadsheetResultBeanPropertyNamingStrategy);
            }
            if (Map.class == toType || spreadsheetResultsToMap) {
                return spreadsheetResult.toMap(spreadsheetResultsToMap, spreadsheetResultBeanPropertyNamingStrategy);
            } else if (toTypeOpenClass instanceof CustomSpreadsheetResultOpenClass && ((CustomSpreadsheetResultOpenClass) toTypeOpenClass)
                    .getBeanClass() == toType) {
                CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) toTypeOpenClass;
                return customSpreadsheetResultOpenClass.createBean(spreadsheetResult,
                        spreadsheetResultBeanPropertyNamingStrategy);
            } else if (toTypeOpenClass instanceof SpreadsheetResultOpenClass && ((SpreadsheetResultOpenClass) toTypeOpenClass)
                    .toCustomSpreadsheetResultOpenClass()
                    .getBeanClass() == toType) {
                CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = ((SpreadsheetResultOpenClass) toTypeOpenClass)
                        .toCustomSpreadsheetResultOpenClass();
                return customSpreadsheetResultOpenClass.createBean(spreadsheetResult,
                        spreadsheetResultBeanPropertyNamingStrategy);
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
                        .createBean(spreadsheetResult, spreadsheetResultBeanPropertyNamingStrategy);
            } else {
                if (spreadsheetResult.getCustomSpreadsheetResultOpenClass() != null) {
                    return spreadsheetResult.getCustomSpreadsheetResultOpenClass().createBean(spreadsheetResult, null);
                } else {
                    return spreadsheetResult.toMap(false, null);
                }
            }
        }
        return v;
    }

    private static Object convertMap(Map<?, ?> v, Function<Object, Object> function) {
        Map<Object, Object> newMap;
        try {
            newMap = (Map<Object, Object>) v.getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            return v;
        }
        for (Entry<?, ?> e : v.entrySet()) {
            newMap.put(function.apply(e.getKey()), function.apply(e.getValue()));
        }
        return newMap;
    }

    private static Object convertCollection(Collection<?> v, Function<Object, Object> function) {
        Collection<Object> newCollection;
        try {
            newCollection = (Collection<Object>) v.getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
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

        SpreadsheetResult that = (SpreadsheetResult) o;

        if (rowNames.length != that.rowNames.length) {
            return false;
        }
        if (columnNames.length != that.columnNames.length) {
            return false;
        }
        for (int row = 0; row < rowNames.length; row++) {
            for (int column = 0; column < columnNames.length; column++) {
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
        int hashCode = 0;
        for (var row : rowNames) {
            hashCode += Objects.hashCode(row);
        }
        for (var column : columnNames) {
            hashCode += Objects.hashCode(column);
        }
        return hashCode;
    }
}
