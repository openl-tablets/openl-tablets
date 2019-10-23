package org.openl.rules.calc;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.openl.binding.IBindingContext;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.binding.impl.CastToWiderType;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
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

    private String[] rowNames;
    private String[] columnNames;
    private String[] rowNamesForModel;
    private String[] columnNamesForModel;
    private List<Pair<String[], String[]>> rowAndColumnNamesForModelHistory;
    private String[] rowTitles;
    private String[] columnTitles;
    private Map<String, Point> fieldsCoordinates;
    private XlsModuleOpenClass module;
    private volatile Class<?> beanClass;
    private volatile SpreadsheetResultSetter[] spreadsheetResultSetters;
    private boolean simpleRefBeanByRow;
    private boolean simpleRefBeanByColumn;
    private long columnsForModelCount;
    private long rowsForModelCount;
    private byte[] beanClassByteCode;
    private boolean detailedPlainModel;
    volatile Map<String, List<IOpenField>> beanFieldsMap;

    public CustomSpreadsheetResultOpenClass(String name,
            String[] rowNames,
            String[] columnNames,
            String[] rowNamesForModel,
            String[] columnNamesForModel,
            String[] rowTitles,
            String[] columnTitles,
            XlsModuleOpenClass module,
            boolean detailedPlainModel) {
        super(name, SpreadsheetResult.class);
        this.rowNames = Objects.requireNonNull(rowNames);
        this.columnNames = Objects.requireNonNull(columnNames);
        this.rowNamesForModel = Objects.requireNonNull(rowNamesForModel);
        this.columnNamesForModel = Objects.requireNonNull(columnNamesForModel);

        this.columnsForModelCount = Arrays.stream(columnNamesForModel).filter(Objects::nonNull).count();
        this.rowsForModelCount = Arrays.stream(rowNamesForModel).filter(Objects::nonNull).count();

        this.simpleRefBeanByRow = columnsForModelCount == 1;
        this.simpleRefBeanByColumn = rowsForModelCount == 1;

        this.rowAndColumnNamesForModelHistory = new ArrayList<>();
        this.rowAndColumnNamesForModelHistory
            .add(Pair.of(this.columnNamesForModel, this.rowNamesForModel));

        this.rowTitles = Objects.requireNonNull(rowTitles);
        this.columnTitles = Objects.requireNonNull(columnTitles);

        this.fieldsCoordinates = SpreadsheetResult.buildFieldsCoordinates(this.columnNames, this.rowNames);
        this.module = Objects.requireNonNull(module);
        this.detailedPlainModel = detailedPlainModel;
    }

    @Override
    public void addField(IOpenField field) throws DuplicatedFieldException {
        if (!(field instanceof CustomSpreadsheetResultField)) {
            throw new OpenlNotCheckedException(String.format("Expected '%s', but found '%s'.",
                CustomSpreadsheetResultField.class.getTypeName(),
                field.getClass().getTypeName()));
        }
        super.addField(field);
    }

    public CustomSpreadsheetResultOpenClass(String name) {
        super(name, SpreadsheetResult.class);
    }

    private Iterable<IOpenClass> superClasses = null;

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    public boolean isEmptyBeanClass() {
        return Arrays.stream(getBeanClass().getDeclaredFields()).filter(e -> !e.isSynthetic()).count() == 0; // SONAR
                                                                                                             // adds
                                                                                                             // synthetic
                                                                                                             // fields
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
            String[] rowNamesForModel,
            String[] columnNamesForModel,
            String[] rowTitles,
            String[] columnTitles,
            Collection<IOpenField> fields,
            boolean simpleRefBeanByRow,
            boolean simpleRefBeanByColumn,
            boolean detailedPlainModel,
            IBindingContext bindingContext) {
        if (beanClass != null) {
            throw new IllegalStateException(
                "Bean class for custom spreadsheet result has already been generated. Spreasheet result cannot be extended.");
        }

        List<String> nRowNames = new ArrayList<>(Arrays.asList(this.rowNames));
        List<String> nRowNamesForModel = new ArrayList<>(Arrays.asList(this.rowNamesForModel));
        Set<String> existedRowNamesSet = new HashSet<>(Arrays.asList(this.rowNames));
        List<String> nColumnNames = new ArrayList<>(Arrays.asList(this.columnNames));
        List<String> nColumnNamesForModel = new ArrayList<>(
            Arrays.asList(this.columnNamesForModel));
        Set<String> existedColumnNamesSet = new HashSet<>(Arrays.asList(this.columnNames));

        List<String> nRowTitles = new ArrayList<>(Arrays.asList(this.rowTitles));
        List<String> nColumnTitles = new ArrayList<>(Arrays.asList(this.columnTitles));

        boolean fieldCoordinatesNeedUpdate = false;
        boolean rowColumnsForModelNeedUpdate = false;

        for (int i = 0; i < rowNames.length; i++) {
            if (!existedRowNamesSet.contains(rowNames[i])) {
                nRowNames.add(rowNames[i]);
                nRowNamesForModel.add(rowNamesForModel[i]);
                nRowTitles.add(rowTitles[i]);
                fieldCoordinatesNeedUpdate = true;
                rowColumnsForModelNeedUpdate = true;
            } else if (rowNamesForModel[i] != null) {
                int k = nRowNames.indexOf(rowNames[i]);
                nRowNamesForModel.set(k, rowNamesForModel[i]);
                rowColumnsForModelNeedUpdate = true;
            }
        }

        for (int i = 0; i < columnNames.length; i++) {
            if (!existedColumnNamesSet.contains(columnNames[i])) {
                nColumnNames.add(columnNames[i]);
                nColumnNamesForModel.add(columnNamesForModel[i]);
                nColumnTitles.add(columnTitles[i]);
                fieldCoordinatesNeedUpdate = true;
                rowColumnsForModelNeedUpdate = true;
            } else if (columnNamesForModel[i] != null) {
                int k = nColumnNames.indexOf(columnNames[i]);
                nColumnNamesForModel.set(k, columnNamesForModel[i]);
                rowColumnsForModelNeedUpdate = true;
            }
        }

        if (fieldCoordinatesNeedUpdate) {
            this.rowNames = nRowNames.toArray(new String[] {});
            this.rowTitles = nRowTitles.toArray(new String[] {});

            this.columnNames = nColumnNames.toArray(new String[] {});
            this.columnTitles = nColumnTitles.toArray(new String[] {});

            this.fieldsCoordinates = Collections
                .unmodifiableMap(SpreadsheetResult.buildFieldsCoordinates(this.columnNames, this.rowNames));
        }

        if (rowColumnsForModelNeedUpdate) {
            this.simpleRefBeanByRow = simpleRefBeanByRow && this.simpleRefBeanByRow;
            this.simpleRefBeanByColumn = simpleRefBeanByColumn && this.simpleRefBeanByColumn;

            this.rowAndColumnNamesForModelHistory
                .add(Pair.of(columnNamesForModel, rowNamesForModel));

            this.rowNamesForModel = nRowNamesForModel.toArray(new String[] {});
            this.columnNamesForModel = nColumnNamesForModel.toArray(new String[] {});
            this.columnsForModelCount = Arrays.stream(columnNamesForModel)
                .filter(Objects::nonNull)
                .count();
            this.rowsForModelCount = Arrays.stream(rowNamesForModel).filter(Objects::nonNull).count();
        }

        for (IOpenField field : fields) {
            IOpenField thisField = getField(field.getName());

            if (thisField == null) {
                addField(field);
            } else {
                if (!thisField.getType().equals(field.getType())) {
                    CastToWiderType castToWiderType = CastToWiderType
                        .create(bindingContext, thisField.getType(), field.getType());
                    fieldMap().put(field.getName(),
                        new CastingCustomSpreadsheetResultField(getModule(),
                            field.getName(),
                            (CustomSpreadsheetResultField) thisField,
                            castToWiderType.getCast1(),
                            (CustomSpreadsheetResultField) field,
                            castToWiderType.getCast2(),
                            castToWiderType.getWiderType()));
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

    public void extendWith(CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass,
            IBindingContext bindingContext) {
        if (beanClass != null) {
            throw new IllegalStateException("Bean class is loaded. Custom spreadsheet result cannot be extended.");
        }
        this.extendSpreadsheetResult(customSpreadsheetResultOpenClass.rowNames,
            customSpreadsheetResultOpenClass.columnNames,
            customSpreadsheetResultOpenClass.rowNamesForModel,
            customSpreadsheetResultOpenClass.columnNamesForModel,
            customSpreadsheetResultOpenClass.rowTitles,
            customSpreadsheetResultOpenClass.columnTitles,
            customSpreadsheetResultOpenClass.getFields().values(),
            customSpreadsheetResultOpenClass.simpleRefBeanByRow,
            customSpreadsheetResultOpenClass.simpleRefBeanByColumn,
            customSpreadsheetResultOpenClass.detailedPlainModel,
            bindingContext);
    }

    public void fixCSRFields() {
        if (beanClass != null) {
            throw new IllegalStateException("Bean class is loaded. Custom spreadsheet result cannot be extended.");
        }
        for (String fieldName : fieldMap().keySet()) {
            IOpenField openField = fieldMap().get(fieldName);
            if (openField.getType() instanceof CustomSpreadsheetResultOpenClass) {
                IOpenClass openClass = module.findType(openField.getType().getName());
                if (openClass instanceof CustomSpreadsheetResultOpenClass) {
                    fieldMap().put(fieldName, new CustomSpreadsheetResultField(module, fieldName, openClass));
                } else if (openClass != null) {
                    throw new OpenlNotCheckedException("Expected custom spreadsheet result type.");
                }
            }
        }
    }

    public String[] getRowNamesForModel() {
        return rowNamesForModel.clone();
    }

    public String[] getColumnNamesForModel() {
        return columnNamesForModel.clone();
    }

    public CustomSpreadsheetResultOpenClass makeCopyForModule(XlsModuleOpenClass module) {
        CustomSpreadsheetResultOpenClass type = new CustomSpreadsheetResultOpenClass(getName(),
            rowNames,
            columnNames,
            rowNamesForModel,
            columnNamesForModel,
            rowTitles,
            columnTitles,
            module,
            detailedPlainModel);
        for (IOpenField field : getFields().values()) {
            if (field instanceof CustomSpreadsheetResultField) {
                type.addField(field);
            }
        }
        type.setMetaInfo(getMetaInfo());
        return type;
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        Object[][] result = new Object[rowNames.length][columnNames.length];
        return new SpreadsheetResult(result,
            rowNames,
            columnNames,
            rowNamesForModel,
            columnNamesForModel,
            fieldsCoordinates);
    }

    public Object createBean(SpreadsheetResult spreadsheetResult) throws IllegalAccessException,
                                                                  InstantiationException {
        if (!this.getName().equals(spreadsheetResult.getCustomSpreadsheetResultOpenClass().getName())) {
            throw new IllegalArgumentException("Invalid spreadsheet result structure.");
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
                    final String beanClassName = getBeanClassName(this);
                    JavaBeanClassBuilder beanClassBuilder = new JavaBeanClassBuilder(beanClassName);
                    Set<String> usedFields = new HashSet<>();
                    @SuppressWarnings("unchecked")
                    List<IOpenField>[][] used = new List[rowNames.length][columnNames.length];
                    Map<String, List<IOpenField>> beanFieldsMap = new HashMap<>();
                    List<Triple<String, Point, IOpenField>> fields = getSortedFields();
                    addFieldsToJavaClassBuilder(beanClassBuilder, fields, used, usedFields, true, beanFieldsMap);
                    addFieldsToJavaClassBuilder(beanClassBuilder, fields, used, usedFields, false, beanFieldsMap);
                    String[] sprStructureFieldNames = addSprStructureFields(beanClassBuilder, beanFieldsMap.keySet());

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
                }
            }
        }
        return beanClass;

    }

    private List<Triple<String, Point, IOpenField>> getSortedFields() {
        List<Triple<String, Point, IOpenField>> fields = new ArrayList<>();
        for (Entry<String, IOpenField> entry : getFields().entrySet()) {
            fields.add(Triple.of(entry.getKey(), fieldsCoordinates.get(entry.getKey()), entry.getValue()));
        }
        Collections.sort(fields, COMP);
        return fields;
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
            if (point != null && rowNamesForModel[point
                .getRow()] != null && columnNamesForModel[point.getColumn()] != null) {
                if (used[point.getRow()][point.getColumn()] == null) {
                    String fieldName;
                    if (simpleRefBeanByRow) {
                        fieldName = rowNamesForModel[point.getRow()];
                    } else if (simpleRefBeanByColumn) {
                        fieldName = columnNamesForModel[point.getColumn()];
                    } else {
                        boolean found = false;
                        for (Pair<String[], String[]> p : rowAndColumnNamesForModelHistory) {
                            for (String col : p.getLeft()) {
                                for (String row : p.getRight()) {
                                    if (!found && Objects.equals(columnNamesForModel[point.getColumn()],
                                        col) && Objects.equals(rowNamesForModel[point.getRow()], row)) {
                                        found = true;
                                    }
                                }
                            }
                        }
                        if (!found) {
                            continue;
                        }
                        fieldName = columnNamesForModel[point
                            .getColumn()] + "_" + rowNamesForModel[point.getRow()];
                    }
                    if (org.apache.commons.lang3.StringUtils.isBlank(fieldName)) {
                        fieldName = "_";
                    }
                    Class<?> type;
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
                        Class<?> fieldCls;
                        if (csroc != null) {
                            if (csroc.isEmptyBeanClass()) {
                                continue; // IGNORE EMPTY CSRS TYPES
                            }
                            fieldCls = csroc.getBeanClass();
                        } else {
                            fieldCls = Object.class;
                        }
                        if (dim > 0) {
                            type = Array.newInstance(fieldCls, new int[dim]).getClass();
                        } else {
                            type = fieldCls;
                        }
                    } else if (t instanceof SpreadsheetResultOpenClass) {
                        if (dim > 0) {
                            type = Array.newInstance(Map.class, new int[dim]).getClass();
                        } else {
                            type = Map.class;
                        }
                    } else if (JavaOpenClass.VOID.equals(t) || JavaOpenClass.CLS_VOID.equals(t) || NullOpenClass.the
                        .equals(t)) {
                        continue; // IGNORE VOID FIELDS
                    } else {
                        type = w.getRight().getType().getInstanceClass();
                    }
                    if (!isFieldConflictsWithOtherGetterSetters(usedGettersAndSetters, fieldName)) {
                        usedGettersAndSetters.add(ClassUtils.getter(fieldName));
                        usedGettersAndSetters.add(ClassUtils.setter(fieldName));
                        beanClassBuilder.addField(fieldName, type.getName());
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
                        beanClassBuilder.addField(newFieldName, type.getName());
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
        String firstLetterUppercasedName = Character
            .toUpperCase(name.charAt(0)) + (name.length() > 1 ? name.substring(1) : "");
        if (customSpreadsheetResultOpenClass.getModule()
            .findType(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + firstLetterUppercasedName) == null) {
            name = firstLetterUppercasedName;
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
                        if (p != null && spreadsheetResult.rowNamesForModel[p
                            .getRow()] != null && spreadsheetResult.columnNamesForModel[p
                                .getColumn()] != null) {
                            fieldNames[p.getRow()][p.getColumn()] = e.getKey();
                        }
                    }
                }
                field.set(target, fieldNames);
            }
        }

    }

}
