package org.openl.rules.calc;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.IntStream;

import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.vm.IRuntimeEnv;

public class CustomSpreadsheetResultOpenClass extends ADynamicClass {

    private static final String[] EMPTY_STRING_ARRAY = new String[] {};

    private String[] rowNames;
    private String[] columnNames;
    private String[] rowNamesForResultModel;
    private String[] columnNamesForResultModel;
    private List<Pair<String[], String[]>> rowAndColumnNamesForResultModelHistory;
    private String[] rowTitles;
    private String[] columnTitles;
    private Map<String, Point> fieldsCoordinates;
    private XlsModuleOpenClass module;
    private volatile Class<?> beanClass;
    private volatile SpreadsheetResultSetter[] spreadsheetResultSetters;
    private boolean simpleRefBeanByRow;
    private boolean simpleRefBeanByColumn;
    private long columnsForResultModelCount;
    private long rowsForResultModelCount;
    private byte[] beanClassByteCode;
    private boolean detailedPlainModel;
    volatile Map<String, List<IOpenField>> beanFieldsMap;
    volatile boolean beanClassInitializing = false;
    private boolean ignoreCompilation = false;
    private ILogicalTable logicalTable;

    public CustomSpreadsheetResultOpenClass(String name,
            String[] rowNames,
            String[] columnNames,
            String[] rowNamesForResultModel,
            String[] columnNamesForResultModel,
            String[] rowTitles,
            String[] columnTitles,
            XlsModuleOpenClass module,
            boolean detailedPlainModel) {
        super(name, SpreadsheetResult.class);
        this.rowNames = Objects.requireNonNull(rowNames);
        this.columnNames = Objects.requireNonNull(columnNames);
        this.rowNamesForResultModel = Objects.requireNonNull(rowNamesForResultModel);
        this.columnNamesForResultModel = Objects.requireNonNull(columnNamesForResultModel);

        this.columnsForResultModelCount = Arrays.stream(columnNamesForResultModel).filter(Objects::nonNull).count();
        this.rowsForResultModelCount = Arrays.stream(rowNamesForResultModel).filter(Objects::nonNull).count();

        this.simpleRefBeanByRow = columnsForResultModelCount == 1;
        this.simpleRefBeanByColumn = rowsForResultModelCount == 1;

        this.rowAndColumnNamesForResultModelHistory = new ArrayList<>();
        this.rowAndColumnNamesForResultModelHistory
            .add(Pair.of(this.columnNamesForResultModel, this.rowNamesForResultModel));

        this.rowTitles = Objects.requireNonNull(rowTitles);
        this.columnTitles = Objects.requireNonNull(columnTitles);

        this.fieldsCoordinates = SpreadsheetResult.buildFieldsCoordinates(this.columnNames, this.rowNames);
        this.module = Objects.requireNonNull(module);
        this.detailedPlainModel = detailedPlainModel;
    }

    public CustomSpreadsheetResultOpenClass(String name, XlsModuleOpenClass module, ILogicalTable logicalTable) {
        this(name,
            EMPTY_STRING_ARRAY,
            EMPTY_STRING_ARRAY,
            EMPTY_STRING_ARRAY,
            EMPTY_STRING_ARRAY,
            EMPTY_STRING_ARRAY,
            EMPTY_STRING_ARRAY,
            module,
            false);
        this.simpleRefBeanByRow = true;
        this.simpleRefBeanByColumn = true;
        this.logicalTable = logicalTable;
    }

    @Override
    public void addField(IOpenField field) throws DuplicatedFieldException {
        if (!(field instanceof CustomSpreadsheetResultField)) {
            throw new IllegalStateException(String.format("Expected type '%s', but found type '%s'.",
                CustomSpreadsheetResultField.class.getTypeName(),
                field.getClass().getTypeName()));
        }
        super.addField(field);
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        if (ioc instanceof CustomSpreadsheetResultOpenClass) {
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) ioc;
            return this.getName().equals(customSpreadsheetResultOpenClass.getName());
        }
        return false;
    }

    public CustomSpreadsheetResultOpenClass(String name) {
        super(name, SpreadsheetResult.class);
    }

    private Iterable<IOpenClass> superClasses = null;

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    public byte[] getBeanClassByteCode() {
        return beanClassByteCode.clone();
    }

    @Override
    public synchronized Iterable<IOpenClass> superClasses() {
        if (superClasses == null) {
            Class<?>[] interfaces = SpreadsheetResult.class.getInterfaces();
            List<IOpenClass> superClasses = new ArrayList<>(interfaces.length + 1);
            for (Class<?> interf : interfaces) {
                superClasses.add(JavaOpenClass.getOpenClass(interf));
            }
            this.superClasses = superClasses;
        }
        return superClasses;
    }

    public XlsModuleOpenClass getModule() {
        return module;
    }

    private void extendSpreadsheetResult(String[] rowNames,
            String[] columnNames,
            String[] rowNamesForResultModel,
            String[] columnNamesForResultModel,
            String[] rowTitles,
            String[] columnTitles,
            Collection<IOpenField> fields,
            boolean simpleRefBeanByRow,
            boolean simpleRefBeanByColumn,
            boolean detailedPlainModel) {
        if (beanClass != null) {
            throw new IllegalStateException(
                "Bean class for custom spreadsheet result is already generated. This spreadsheet result type cannot be extended.");
        }

        List<String> nRowNames = Arrays.stream(this.rowNames).collect(toList());
        List<String> nRowNamesForResultModel = Arrays.stream(this.rowNamesForResultModel).collect(toList());
        Set<String> existedRowNamesSet = Arrays.stream(this.rowNames).collect(toSet());

        List<String> nColumnNames = Arrays.stream(this.columnNames).collect(toList());
        List<String> nColumnNamesForResultModel = Arrays.stream(this.columnNamesForResultModel).collect(toList());
        Set<String> existedColumnNamesSet = Arrays.stream(this.columnNames).collect(toSet());

        List<String> nRowTitles = Arrays.stream(this.rowTitles).collect(toList());
        List<String> nColumnTitles = Arrays.stream(this.columnTitles).collect(toList());

        boolean fieldCoordinatesNeedUpdate = false;
        boolean rowColumnsForResultModelNeedUpdate = false;

        for (int i = 0; i < rowNames.length; i++) {
            if (!existedRowNamesSet.contains(rowNames[i])) {
                nRowNames.add(rowNames[i]);
                nRowNamesForResultModel.add(rowNamesForResultModel[i]);
                nRowTitles.add(rowTitles[i]);
                fieldCoordinatesNeedUpdate = true;
                rowColumnsForResultModelNeedUpdate = true;
            } else if (rowNamesForResultModel[i] != null) {
                int k = nRowNames.indexOf(rowNames[i]);
                nRowNamesForResultModel.set(k, rowNamesForResultModel[i]);
                rowColumnsForResultModelNeedUpdate = true;
            }
        }

        for (int i = 0; i < columnNames.length; i++) {
            if (!existedColumnNamesSet.contains(columnNames[i])) {
                nColumnNames.add(columnNames[i]);
                nColumnNamesForResultModel.add(columnNamesForResultModel[i]);
                nColumnTitles.add(columnTitles[i]);
                fieldCoordinatesNeedUpdate = true;
                rowColumnsForResultModelNeedUpdate = true;
            } else if (columnNamesForResultModel[i] != null) {
                int k = nColumnNames.indexOf(columnNames[i]);
                nColumnNamesForResultModel.set(k, columnNamesForResultModel[i]);
                rowColumnsForResultModelNeedUpdate = true;
            }
        }

        if (fieldCoordinatesNeedUpdate) {
            this.rowNames = nRowNames.toArray(EMPTY_STRING_ARRAY);
            this.rowTitles = nRowTitles.toArray(EMPTY_STRING_ARRAY);

            this.columnNames = nColumnNames.toArray(EMPTY_STRING_ARRAY);
            this.columnTitles = nColumnTitles.toArray(EMPTY_STRING_ARRAY);

            this.fieldsCoordinates = Collections
                .unmodifiableMap(SpreadsheetResult.buildFieldsCoordinates(this.columnNames, this.rowNames));
        }

        if (rowColumnsForResultModelNeedUpdate) {
            this.simpleRefBeanByRow = simpleRefBeanByRow && this.simpleRefBeanByRow;
            this.simpleRefBeanByColumn = simpleRefBeanByColumn && this.simpleRefBeanByColumn;

            this.rowAndColumnNamesForResultModelHistory.add(Pair.of(columnNamesForResultModel, rowNamesForResultModel));

            this.rowNamesForResultModel = nRowNamesForResultModel.toArray(EMPTY_STRING_ARRAY);
            this.columnNamesForResultModel = nColumnNamesForResultModel.toArray(EMPTY_STRING_ARRAY);
            this.columnsForResultModelCount = Arrays.stream(columnNamesForResultModel).filter(Objects::nonNull).count();
            this.rowsForResultModelCount = Arrays.stream(rowNamesForResultModel).filter(Objects::nonNull).count();
        }

        for (IOpenField field : fields) {
            IOpenField thisField = getField(field.getName());

            if (thisField == null) {
                addField(field);
            } else {
                if (thisField instanceof CustomSpreadsheetResultField && field instanceof CustomSpreadsheetResultField) {
                    fieldMap().put(field.getName(),
                        new CastingCustomSpreadsheetResultField(this,
                            field.getName(),
                            (CustomSpreadsheetResultField) thisField,
                            (CustomSpreadsheetResultField) field));
                }
            }
        }

        this.detailedPlainModel = this.detailedPlainModel || detailedPlainModel;

    }

    public String[] getRowNames() {
        return rowNames.clone();
    }

    public String[] getColumnNames() {
        return columnNames.clone();
    }

    public String[] getRowTitles() {
        return rowTitles.clone();
    }

    public String[] getColumnTitles() {
        return columnTitles.clone();
    }

    public void extendWith(CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
        if (beanClass != null) {
            throw new IllegalStateException(
                "Java bean class for custom spreadsheet result is loaded to classloader. Custom spreadsheet result cannot be extended.");
        }
        this.extendSpreadsheetResult(customSpreadsheetResultOpenClass.rowNames,
            customSpreadsheetResultOpenClass.columnNames,
            customSpreadsheetResultOpenClass.rowNamesForResultModel,
            customSpreadsheetResultOpenClass.columnNamesForResultModel,
            customSpreadsheetResultOpenClass.rowTitles,
            customSpreadsheetResultOpenClass.columnTitles,
            customSpreadsheetResultOpenClass.getFields().values(),
            customSpreadsheetResultOpenClass.simpleRefBeanByRow,
            customSpreadsheetResultOpenClass.simpleRefBeanByColumn,
            customSpreadsheetResultOpenClass.detailedPlainModel);
    }

    public void fixCSRFields() {
        if (beanClass != null) {
            throw new IllegalStateException(
                "Java bean class for custom spreadsheet result is loaded to classloader. Custom spreadsheet result cannot be extended.");
        }
        for (String fieldName : fieldMap().keySet()) {
            IOpenField openField = fieldMap().get(fieldName);
            IOpenClass type = openField.getType();
            int dim = 0;
            while (type.isArray()) {
                type = type.getComponentClass();
                dim++;
            }
            if (type instanceof CustomSpreadsheetResultOpenClass) {
                IOpenClass openClass = module.findType(type.getName());
                if (openClass instanceof CustomSpreadsheetResultOpenClass) {
                    IOpenClass t = openClass;
                    if (dim > 0) {
                        t = t.getArrayType(dim);
                    }
                    fieldMap().put(fieldName, new CustomSpreadsheetResultField(module, fieldName, t));
                } else if (openClass != null) {
                    throw new IllegalStateException(String.format("Expected type '%s', but found type '%s'.",
                        CustomSpreadsheetResultOpenClass.class.getTypeName(),
                        openClass.getName()));
                }
            }
        }
    }

    public String[] getRowNamesForResultModel() {
        return rowNamesForResultModel.clone();
    }

    public String[] getColumnNamesForResultModel() {
        return columnNamesForResultModel.clone();
    }

    private boolean isCustomSpreadsheetResultField(IOpenField field) {
        return field instanceof CustomSpreadsheetResultField;
    }

    public CustomSpreadsheetResultOpenClass makeCopyForModule(XlsModuleOpenClass module) {
        CustomSpreadsheetResultOpenClass type = new CustomSpreadsheetResultOpenClass(getName(),
            rowNames,
            columnNames,
            rowNamesForResultModel,
            columnNamesForResultModel,
            rowTitles,
            columnTitles,
            module,
            detailedPlainModel);
        for (IOpenField field : getFields().values()) {
            if (isCustomSpreadsheetResultField(field)) {
                type.addField(field);
            }
        }
        type.setMetaInfo(getMetaInfo());
        type.logicalTable = this.logicalTable;
        return type;
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        SpreadsheetResult spr = new SpreadsheetResult(new Object[rowNames.length][columnNames.length],
            rowNames,
            columnNames,
            rowNamesForResultModel,
            columnNamesForResultModel,
            fieldsCoordinates);
        spr.setLogicalTable(logicalTable);
        return spr;
    }

    public Object createBean(SpreadsheetResult spreadsheetResult) throws IllegalAccessException,
                                                                  InstantiationException {
        if (!this.getName().equals(spreadsheetResult.getCustomSpreadsheetResultOpenClass().getName())) {
            throw new IllegalArgumentException("Invalid spreadsheet result.");
        }
        Class<?> clazz = getBeanClass();
        Object target = clazz.newInstance();
        for (SpreadsheetResultSetter spreadsheetResultSetter : spreadsheetResultSetters) {
            spreadsheetResultSetter.set(spreadsheetResult, target);
        }
        return target;
    }

    public boolean isBeanClassInitialized() {
        return beanClass != null;
    }

    public Class<?> getBeanClass() {
        if (beanClass == null) {
            synchronized (this) {
                if (beanClass == null) {
                    if (beanClassInitializing) {
                        // The same thread compiles this bean class. SOF occurs in method
                        // 'addFieldsToJavaClassBuilder'.
                        return null;
                    }
                    try {
                        beanClassInitializing = true;
                        final String beanClassName = getBeanClassName(this);
                        JavaBeanClassBuilder beanClassBuilder = new JavaBeanClassBuilder(beanClassName);
                        Set<String> usedFields = new HashSet<>();
                        @SuppressWarnings("unchecked")
                        List<IOpenField>[][] used = new List[rowNames.length][columnNames.length];
                        Map<String, List<IOpenField>> beanFieldsMap = new HashMap<>();
                        List<Triple<String, Point, IOpenField>> fields = getSortedFields();
                        addFieldsToJavaClassBuilder(beanClassBuilder, fields, used, usedFields, true, beanFieldsMap);
                        addFieldsToJavaClassBuilder(beanClassBuilder, fields, used, usedFields, false, beanFieldsMap);
                        String[] sprStructureFieldNames = addSprStructureFields(beanClassBuilder,
                            beanFieldsMap.keySet());

                        byte[] byteCode = beanClassBuilder.byteCode();
                        try {
                            this.beanClass = loadBeanClass(beanClassName, byteCode);
                            this.beanClassByteCode = byteCode;
                            List<SpreadsheetResultSetter> sprSetters = new ArrayList<>();
                            for (Field field : beanClass.getDeclaredFields()) {
                                if (!field.isSynthetic()) {// SONAR adds synthetic fields
                                    List<IOpenField> openFields = beanFieldsMap.get(field.getName());
                                    if (openFields != null) {
                                        for (IOpenField openField : openFields) {
                                            SpreadsheetResultValueSetter spreadsheetResultValueSetter = new SpreadsheetResultValueSetter(
                                                module,
                                                field,
                                                openField);
                                            sprSetters.add(spreadsheetResultValueSetter);
                                        }
                                    } else if (field.getName().equals(sprStructureFieldNames[0])) {
                                        sprSetters.add(new SpreadsheetResultRowNamesSetter(field));
                                    } else if (field.getName().equals(sprStructureFieldNames[1])) {
                                        sprSetters.add(new SpreadsheetResultColumnNamesSetter(field));
                                    } else if (field.getName().equals(sprStructureFieldNames[2])) {
                                        sprSetters.add(new SpreadsheetResultFieldNamesSetter(field, beanFieldsMap));
                                    }
                                }
                            }
                            this.beanFieldsMap = beanFieldsMap;
                            this.spreadsheetResultSetters = sprSetters.toArray(new SpreadsheetResultSetter[] {});
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                    } finally {
                        beanClassInitializing = false;
                    }
                }
            }
        }
        return beanClass;

    }

    private List<Triple<String, Point, IOpenField>> getSortedFields() {
        return getFields().entrySet()
            .stream()
            .map(entry -> Triple.of(entry.getKey(), fieldsCoordinates.get(entry.getKey()), entry.getValue()))
            .sorted(COMP)
            .collect(toList());
    }

    public Map<String, List<IOpenField>> getBeanFieldsMap() {
        if (beanFieldsMap == null) {
            getBeanClass();
        }
        return beanFieldsMap;
    }

    private String[] addSprStructureFields(JavaBeanClassBuilder beanClassBuilder, Set<String> beanFieldNames) {
        if (detailedPlainModel) {
            String[] sprStructureFieldNames = new String[3];
            sprStructureFieldNames[0] = beanFieldNames.contains("rowNames") ? "$rowNames" : "rowNames";
            sprStructureFieldNames[1] = beanFieldNames.contains("columnNames") ? "$columnNames" : "columnNames";
            sprStructureFieldNames[2] = beanFieldNames.contains("fieldNames") ? "$fieldNames" : "fieldNames";
            beanClassBuilder.addField(sprStructureFieldNames[0], String[].class.getName());
            beanClassBuilder.addField(sprStructureFieldNames[1], String[].class.getName());
            beanClassBuilder.addField(sprStructureFieldNames[2], String[][].class.getName());
            return sprStructureFieldNames;
        }
        return new String[3];
    }

    private Class<?> loadBeanClass(final String beanClassName, byte[] byteCode) throws Exception {
        try {
            return getModule().getClassGenerationClassLoader().loadClass(beanClassName);
        } catch (ClassNotFoundException e) {
            return ClassUtils.defineClass(beanClassName, byteCode, module.getClassGenerationClassLoader());
        }
    }

    private static final Comparator<Triple<String, Point, IOpenField>> COMP = (a, b) -> {
        @SuppressWarnings("unchecked")
        Comparator<Point> c = ComparatorUtils.chainedComparator(
            Comparator.nullsLast(Comparator.comparingInt(Point::getRow)),
            Comparator.nullsLast(Comparator.comparingInt(Point::getColumn)));
        return c.compare(a.getMiddle(), b.getMiddle());
    };

    private void addFieldsToJavaClassBuilder(JavaBeanClassBuilder beanClassBuilder,
            List<Triple<String, Point, IOpenField>> fields,
            List<IOpenField>[][] used,
            Set<String> usedGettersAndSetters,
            boolean addFieldNameWithCollisions,
            Map<String, List<IOpenField>> beanFieldsMap) {
        for (Triple<String, Point, IOpenField> w : fields) {
            Point point = w.getMiddle();
            if (point != null && rowNamesForResultModel[point.getRow()] != null && columnNamesForResultModel[point
                .getColumn()] != null) {
                if (used[point.getRow()][point.getColumn()] == null) {
                    String fieldName;
                    if (simpleRefBeanByRow) {
                        fieldName = rowNamesForResultModel[point.getRow()];
                    } else if (simpleRefBeanByColumn) {
                        fieldName = columnNamesForResultModel[point.getColumn()];
                    } else {
                        boolean found = false;
                        for (Pair<String[], String[]> p : rowAndColumnNamesForResultModelHistory) {
                            for (String col : p.getLeft()) {
                                for (String row : p.getRight()) {
                                    if (!found && Objects.equals(columnNamesForResultModel[point.getColumn()],
                                        col) && Objects.equals(rowNamesForResultModel[point.getRow()], row)) {
                                        found = true;
                                    }
                                }
                            }
                        }
                        if (!found) {
                            continue;
                        }
                        fieldName = columnNamesForResultModel[point.getColumn()] + "_" + rowNamesForResultModel[point
                            .getRow()];
                    }
                    if (org.apache.commons.lang3.StringUtils.isBlank(fieldName)) {
                        fieldName = "_";
                    }
                    String typeName;
                    IOpenClass t = w.getRight().getType();
                    int dim = 0;
                    while (t.isArray()) {
                        dim++;
                        t = t.getComponentClass();
                    }
                    if (t instanceof CustomSpreadsheetResultOpenClass) {
                        CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) t;
                        CustomSpreadsheetResultOpenClass csroc = (CustomSpreadsheetResultOpenClass) this.getModule()
                            .findType(customSpreadsheetResultOpenClass.getName());
                        String fieldClsName;
                        if (csroc != null) {
                            fieldClsName = getBeanClassName(csroc);
                            // Loads field bean class to the same class loader
                            csroc.getBeanClass();
                        } else {
                            fieldClsName = Object.class.getName();
                        }
                        typeName = dim > 0 ? IntStream.range(0, dim)
                            .mapToObj(e -> "[")
                            .collect(joining()) + "L" + fieldClsName + ";" : fieldClsName;
                    } else if (t instanceof SpreadsheetResultOpenClass) {
                        if (dim > 0) {
                            typeName = Array.newInstance(Map.class, new int[dim]).getClass().getName();
                        } else {
                            typeName = Map.class.getName();
                        }
                    } else if (JavaOpenClass.VOID.equals(t) || JavaOpenClass.CLS_VOID.equals(t) || NullOpenClass.the
                        .equals(t)) {
                        continue; // IGNORE VOID FIELDS
                    } else {
                        typeName = w.getRight().getType().getInstanceClass().getName();
                    }
                    if (!isFieldConflictsWithOtherGetterSetters(usedGettersAndSetters, fieldName)) {
                        usedGettersAndSetters.add(ClassUtils.getter(fieldName));
                        usedGettersAndSetters.add(ClassUtils.setter(fieldName));
                        beanClassBuilder.addField(fieldName, typeName);
                        beanFieldsMap.put(fieldName, fillUsed(used, point, w.getRight()));
                    } else if (addFieldNameWithCollisions) {
                        String newFieldName = fieldName;
                        if (!fieldName.startsWith("_")) {
                            newFieldName = "_" + fieldName;
                        }
                        int i = 1;
                        while (isFieldConflictsWithOtherGetterSetters(usedGettersAndSetters, newFieldName)) {
                            newFieldName = fieldName + "_" + i;
                            i++;
                        }
                        usedGettersAndSetters.add(ClassUtils.getter(newFieldName));
                        usedGettersAndSetters.add(ClassUtils.setter(newFieldName));
                        beanClassBuilder.addField(newFieldName, typeName);
                        beanFieldsMap.put(newFieldName, fillUsed(used, point, w.getRight()));
                    }
                } else {
                    boolean f = false;
                    for (IOpenField openField : used[point.getRow()][point.getColumn()]) { // Do not add the same twice
                        if (openField.getName().equals(w.getRight().getName())) {
                            f = true;
                            break;
                        }
                    }
                    if (!f) {
                        used[point.getRow()][point.getColumn()].add(w.getRight());
                    }
                }
            }
        }
    }

    private boolean isFieldConflictsWithOtherGetterSetters(Set<String> usedGettersAndSetters, String fieldName) {
        return usedGettersAndSetters.contains(ClassUtils.getter(fieldName)) || usedGettersAndSetters
            .contains(ClassUtils.setter(fieldName));
    }

    private List<IOpenField> fillUsed(List<IOpenField>[][] used, Point point, IOpenField field) {
        List<IOpenField> fields = new ArrayList<>();
        fields.add(field);
        if (simpleRefBeanByRow) {
            for (int w = 0; w < used[point.getRow()].length; w++) {
                used[point.getRow()][w] = fields;
            }
        } else if (simpleRefBeanByColumn) {
            for (int w = 0; w < used.length; w++) {
                used[w][point.getColumn()] = fields;
            }
        } else {
            used[point.getRow()][point.getColumn()] = fields;
        }
        return fields;
    }

    private static synchronized String getBeanClassName(
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
        String name = customSpreadsheetResultOpenClass.getName()
            .substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length());
        String firstLetterUppercaseName = Character
            .toUpperCase(name.charAt(0)) + (name.length() > 1 ? name.substring(1) : StringUtils.EMPTY);
        if (customSpreadsheetResultOpenClass.getModule()
            .findType(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + firstLetterUppercaseName) == null) {
            name = firstLetterUppercaseName;
        }

        return customSpreadsheetResultOpenClass.getModule().getCsrBeansPackage() + "." + name;
    }

    private static interface SpreadsheetResultSetter {
        void set(SpreadsheetResult spreadsheetResult, Object target) throws IllegalAccessException,
                                                                     InstantiationException;
    }

    private static class SpreadsheetResultValueSetter implements SpreadsheetResultSetter {
        private Field field;
        private IOpenField openField;
        private XlsModuleOpenClass module;

        private SpreadsheetResultValueSetter(XlsModuleOpenClass module, Field field, IOpenField openField) {
            this.field = Objects.requireNonNull(field);

            this.openField = Objects.requireNonNull(openField);

            this.module = Objects.requireNonNull(module);

            this.field.setAccessible(true);
        }

        @Override
        public void set(SpreadsheetResult spreadsheetResult, Object target) throws IllegalAccessException,
                                                                            InstantiationException {
            if (!spreadsheetResult.isFieldUsedInModel(openField.getName())) {
                return;
            }

            Object v = openField.get(spreadsheetResult, null);

            if (v == null) {
                field.set(target, null);
                return;
            }
            Object cv = SpreadsheetResult.convertSpreadsheetResults(module, v, field.getType());
            field.set(target, cv);
        }
    }

    private static class SpreadsheetResultColumnNamesSetter implements SpreadsheetResultSetter {
        private Field field;

        public SpreadsheetResultColumnNamesSetter(Field field) {
            this.field = Objects.requireNonNull(field);
            this.field.setAccessible(true);
        }

        @Override
        public void set(SpreadsheetResult spreadsheetResult, Object target) throws IllegalAccessException,
                                                                            InstantiationException {
            if (spreadsheetResult.isDetailedPlainModel()) {
                field.set(target, spreadsheetResult.columnNames);
            }
        }

    }

    private static class SpreadsheetResultRowNamesSetter implements SpreadsheetResultSetter {
        private Field field;

        public SpreadsheetResultRowNamesSetter(Field field) {
            this.field = Objects.requireNonNull(field);
            this.field.setAccessible(true);
        }

        @Override
        public void set(SpreadsheetResult spreadsheetResult, Object target) throws IllegalAccessException,
                                                                            InstantiationException {
            if (spreadsheetResult.isDetailedPlainModel()) {
                field.set(target, spreadsheetResult.rowNames);
            }
        }

    }

    private static class SpreadsheetResultFieldNamesSetter implements SpreadsheetResultSetter {
        private Field field;
        private Map<String, List<IOpenField>> beanFieldsMap;

        public SpreadsheetResultFieldNamesSetter(Field field, Map<String, List<IOpenField>> beanFieldsMap) {
            this.field = Objects.requireNonNull(field);

            this.beanFieldsMap = Objects.requireNonNull(beanFieldsMap);
            this.field.setAccessible(true);
        }

        @Override
        public void set(SpreadsheetResult spreadsheetResult, Object target) throws IllegalAccessException,
                                                                            InstantiationException {
            if (spreadsheetResult.isDetailedPlainModel()) {
                String[][] fieldNames = new String[spreadsheetResult.getRowNames().length][spreadsheetResult
                    .getColumnNames().length];
                for (Map.Entry<String, List<IOpenField>> e : beanFieldsMap.entrySet()) {
                    List<IOpenField> openFields = e.getValue();
                    for (IOpenField openField : openFields) {
                        Point p = spreadsheetResult.fieldsCoordinates.get(openField.getName());
                        if (p != null && spreadsheetResult.rowNamesForResultModel[p
                            .getRow()] != null && spreadsheetResult.columnNamesForResultModel[p.getColumn()] != null) {
                            fieldNames[p.getRow()][p.getColumn()] = e.getKey();
                        }
                    }
                }
                field.set(target, fieldNames);
            }
        }
    }

    public boolean isIgnoreCompilation() {
        return ignoreCompilation;
    }

    public void setIgnoreCompilation(boolean ignoreCompilation) {
        this.ignoreCompilation = ignoreCompilation;
    }
}
