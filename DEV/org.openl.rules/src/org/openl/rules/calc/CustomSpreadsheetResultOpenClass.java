package org.openl.rules.calc;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.IntStream;

import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.gen.FieldDescription;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.rules.lang.xls.binding.ModuleSpecificType;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.Point;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.vm.IRuntimeEnv;

public class CustomSpreadsheetResultOpenClass extends ADynamicClass implements ModuleSpecificType {

    private static final String[] EMPTY_STRING_ARRAY = new String[] {};
    private static final Comparator<String> FIELD_COMPARATOR = (o1, o2) -> {
        // We do not expect empty fields names, so the length of strings always be greater than zero.
        char c1 = Character.toUpperCase(o1.charAt(0));
        char c2 = Character.toUpperCase(o2.charAt(0));
        if (c1 != c2) {
            return c1 - c2;
        }

        int len1 = o1.length();
        int len2 = o2.length();
        int lim = Math.min(len1, len2);
        int k = 1;
        while (k < lim) {
            c1 = o1.charAt(k);
            c2 = o2.charAt(k);
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    };

    private String[] rowNames;
    private String[] columnNames;
    private String[] rowNamesForResultModel;
    private String[] columnNamesForResultModel;
    private final List<Pair<String[], String[]>> rowAndColumnNamesForResultModelHistory;
    private String[] rowTitles;
    private String[] columnTitles;
    private Map<String, Point> fieldsCoordinates;
    private final XlsModuleOpenClass module;
    private volatile Class<?> beanClass;
    private volatile SpreadsheetResultSetter[] spreadsheetResultSetters;
    private boolean simpleRefBeanByRow;
    private boolean simpleRefBeanByColumn;
    private long columnsForResultModelCount;
    private long rowsForResultModelCount;
    private boolean detailedPlainModel;
    private boolean ignoreCompilation = false;

    private ILogicalTable logicalTable;

    private volatile byte[] beanClassByteCode;
    private volatile String beanClassName;
    volatile Map<String, List<IOpenField>> beanFieldsMap;
    volatile Map<String, String> xmlNamesMap;
    private String[] sprStructureFieldNames;
    private volatile boolean initializing;

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
        this.module = module;
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

    private Collection<IOpenClass> superClasses = null;

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    public byte[] getBeanClassByteCode() {
        return beanClassByteCode.clone();
    }

    @Override
    public synchronized Collection<IOpenClass> superClasses() {
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
                addField(new CustomSpreadsheetResultField(this, field));
            } else {
                fieldMap().put(field.getName(),
                    new CastingCustomSpreadsheetResultField(this, field.getName(), thisField, field));
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

    public Map<String, Point> getFieldsCoordinates() {
        return fieldsCoordinates;
    }

    @Override
    public void updateWithType(IOpenClass openClass) {
        if (beanClassByteCode != null) {
            throw new IllegalStateException(
                "Java bean class for custom spreadsheet result is loaded to classloader. Custom spreadsheet result cannot be extended.");
        }
        CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) openClass;
        this.extendSpreadsheetResult(customSpreadsheetResultOpenClass.rowNames,
            customSpreadsheetResultOpenClass.columnNames,
            customSpreadsheetResultOpenClass.rowNamesForResultModel,
            customSpreadsheetResultOpenClass.columnNamesForResultModel,
            customSpreadsheetResultOpenClass.rowTitles,
            customSpreadsheetResultOpenClass.columnTitles,
            customSpreadsheetResultOpenClass.getFields(),
            customSpreadsheetResultOpenClass.simpleRefBeanByRow,
            customSpreadsheetResultOpenClass.simpleRefBeanByColumn,
            customSpreadsheetResultOpenClass.detailedPlainModel);
    }

    public void fixModuleFieldTypes() {
        if (beanClassByteCode != null) {
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
            if (type instanceof SpreadsheetResultOpenClass) {
                IOpenClass t = module.getSpreadsheetResultOpenClassWithResolvedFieldTypes();
                if (dim > 0) {
                    t = t.getArrayType(dim);
                }
                fieldMap().put(fieldName, new CustomSpreadsheetResultField(module, fieldName, t));
            } else if (type instanceof ModuleSpecificType) {
                IOpenClass openClass = module.findType(type.getName());
                if (openClass != null) {
                    IOpenClass t = openClass;
                    if (dim > 0) {
                        t = t.getArrayType(dim);
                    }
                    fieldMap().put(fieldName, new CustomSpreadsheetResultField(module, fieldName, t));
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

    @Override
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
        for (IOpenField field : getFields()) {
            if (isCustomSpreadsheetResultField(field)) {
                type.addField(field);
            }
        }
        type.setMetaInfo(getMetaInfo());
        type.logicalTable = this.logicalTable;
        return type;
    }

    public ILogicalTable getLogicalTable() {
        return logicalTable;
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

    public Object createBean(SpreadsheetResult spreadsheetResult) {
        if (!this.getName().equals(spreadsheetResult.getCustomSpreadsheetResultOpenClass().getName()) && !Objects
            .equals(
                module.getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                    .toCustomSpreadsheetResultOpenClass()
                    .getName(),
                getName())) {
            throw new IllegalArgumentException("Invalid spreadsheet result.");
        }
        Class<?> clazz = getBeanClass();
        Object target;
        try {
            target = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ignore) {
            return null;
        }

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
                    try {
                        generateBeanClass();
                        beanClass = getModule().getClassGenerationClassLoader().loadClass(getBeanClassName());
                        List<SpreadsheetResultSetter> sprSetters = new ArrayList<>();
                        for (Field field : beanClass.getDeclaredFields()) {
                            if (!field.isSynthetic()) {// SONAR adds synthetic fields
                                List<IOpenField> openFields = beanFieldsMap.get(field.getName());
                                if (openFields != null) {
                                    List<SpreadsheetResultFieldValueSetter> sprSettersForField = new ArrayList<>();
                                    for (IOpenField openField : openFields) {
                                        SpreadsheetResultFieldValueSetter spreadsheetResultValueSetter = new SpreadsheetResultFieldValueSetter(
                                            module,
                                            field,
                                            openField);
                                        sprSettersForField.add(spreadsheetResultValueSetter);
                                    }
                                    sprSetters.add(new SpreadsheetResultValueSetter(
                                        sprSettersForField.toArray(SpreadsheetResultFieldValueSetter.EMPTY_ARRAY)));
                                } else if (field.getName().equals(sprStructureFieldNames[0])) {
                                    sprSetters.add(new SpreadsheetResultRowNamesSetter(field));
                                } else if (field.getName().equals(sprStructureFieldNames[1])) {
                                    sprSetters.add(new SpreadsheetResultColumnNamesSetter(field));
                                } else if (field.getName().equals(sprStructureFieldNames[2])) {
                                    sprSetters
                                        .add(new SpreadsheetResultFieldNamesSetter(field, beanFieldsMap, xmlNamesMap));
                                }
                            }
                        }
                        spreadsheetResultSetters = sprSetters.toArray(SpreadsheetResultSetter.EMPTY_ARRAY);
                    } catch (Exception | LinkageError e) {
                        throw new IllegalStateException(
                            String.format("Failed to create bean class for '%s' spreadsheet result.", getName()),
                            e);
                    }
                }
            }
        }
        return beanClass;
    }

    public void generateBeanClass() {
        if (beanClassByteCode == null) {
            synchronized (this) {
                if (beanClassByteCode == null) {
                    if (!initializing) {
                        try {
                            initializing = true;
                            final String beanClassName = getBeanClassName();
                            JavaBeanClassBuilder beanClassBuilder = new JavaBeanClassBuilder(beanClassName)
                                .withAdditionalConstructor(false)
                                .withEqualsHashCodeToStringMethods(false);
                            TreeMap<String, String> xmlNames = new TreeMap<>(FIELD_COMPARATOR);
                            @SuppressWarnings("unchecked")
                            List<IOpenField>[][] used = new List[rowNames.length][columnNames.length];
                            Map<String, List<IOpenField>> fieldsMap = new HashMap<>();
                            List<Pair<Point, IOpenField>> fields = getSortedFields();
                            addFieldsToJavaClassBuilder(beanClassBuilder, fields, used, xmlNames, true, fieldsMap);
                            addFieldsToJavaClassBuilder(beanClassBuilder, fields, used, xmlNames, false, fieldsMap);
                            sprStructureFieldNames = addSprStructureFields(beanClassBuilder,
                                fieldsMap.keySet(),
                                xmlNames.values());
                            byte[] bc = beanClassBuilder.byteCode();
                            getModule().getClassGenerationClassLoader().addGeneratedClass(beanClassName, bc);
                            beanFieldsMap = Collections.unmodifiableMap(fieldsMap);
                            xmlNamesMap = Collections.unmodifiableMap(xmlNames);
                            beanClassByteCode = bc;
                        } finally {
                            initializing = false;
                        }
                    }
                }
            }
        }
    }

    private List<Pair<Point, IOpenField>> getSortedFields() {
        return getFields().stream()
            .map(e -> Pair.of(fieldsCoordinates.get(e.getName()), e))
            .sorted(COMP)
            .collect(toList());
    }

    public Map<String, List<IOpenField>> getBeanFieldsMap() {
        if (beanFieldsMap == null) {
            generateBeanClass();
        }
        return beanFieldsMap;
    }

    public Map<String, String> getXmlNamesMap() {
        if (xmlNamesMap == null) {
            generateBeanClass();
        }
        return xmlNamesMap;
    }

    public static String findNonConflictFieldName(Collection<String> beanFieldNames, String fName) {
        String fNewName = fName;
        int i = 1;
        while (beanFieldNames.contains(fNewName)) {
            fNewName = fName + i;
            i++;
        }
        return fNewName;
    }

    private String[] addSprStructureFields(JavaBeanClassBuilder beanClassBuilder,
            Set<String> beanFieldNames,
            Collection<String> xmlNames) {
        if (detailedPlainModel) {
            String[] sprStructureFieldNames = new String[3];
            sprStructureFieldNames[0] = findNonConflictFieldName(beanFieldNames, "rowNames");
            sprStructureFieldNames[1] = findNonConflictFieldName(beanFieldNames, "columnNames");
            sprStructureFieldNames[2] = findNonConflictFieldName(beanFieldNames, "tableDetails");
            beanClassBuilder.addField(sprStructureFieldNames[0],
                new FieldDescription(String[].class
                    .getName(), null, null, null, findNonConflictFieldName(xmlNames, "RowNames"), false));
            beanClassBuilder.addField(sprStructureFieldNames[1],
                new FieldDescription(String[].class
                    .getName(), null, null, null, findNonConflictFieldName(xmlNames, "ColumnNames"), false));
            beanClassBuilder.addField(sprStructureFieldNames[2],
                new FieldDescription(String[][].class
                    .getName(), null, null, null, findNonConflictFieldName(xmlNames, "TableDetails"), false));
            return sprStructureFieldNames;
        }
        return new String[3];
    }

    private static final Comparator<Pair<Point, IOpenField>> COMP = (a, b) -> {
        @SuppressWarnings("unchecked")
        Comparator<Point> c = ComparatorUtils.chainedComparator(
            Comparator.nullsLast(Comparator.comparingInt(Point::getRow)),
            Comparator.nullsLast(Comparator.comparingInt(Point::getColumn)));
        return c.compare(a.getLeft(), b.getLeft());
    };

    private void addFieldsToJavaClassBuilder(JavaBeanClassBuilder beanClassBuilder,
            List<Pair<Point, IOpenField>> fields,
            List<IOpenField>[][] used,
            Map<String, String> usedXmlNames,
            boolean addFieldNameWithCollisions,
            Map<String, List<IOpenField>> beanFieldsMap) {
        for (Pair<Point, IOpenField> pair : fields) {
            Point point = pair.getLeft();
            IOpenField field = pair.getRight();
            if (point == null) {
                continue;
            }
            int row = point.getRow();
            int column = point.getColumn();
            String rowName = rowNamesForResultModel[row];
            String columnName = columnNamesForResultModel[column];
            if (rowName != null && columnName != null) {
                if (used[row][column] == null) {
                    String fieldName;
                    String xmlName;
                    if (simpleRefBeanByRow) {
                        fieldName = ClassUtils.decapitalize(rowName);
                        xmlName = rowName;
                    } else if (simpleRefBeanByColumn) {
                        fieldName = ClassUtils.decapitalize(columnName);
                        xmlName = columnName;
                    } else if (absentInHistory(rowName, columnName)) {
                        continue;
                    } else if (StringUtils.isBlank(columnName)) { // * in the column
                        fieldName = ClassUtils.decapitalize(rowName);
                        xmlName = rowName;
                    } else if (StringUtils.isBlank(rowName)) { // * in the row
                        fieldName = ClassUtils.decapitalize(columnName);
                        xmlName = columnName;
                    } else {
                        fieldName = ClassUtils.decapitalize(columnName) + ClassUtils.capitalize(rowName);
                        xmlName = columnName + "_" + rowName;
                    }
                    if (StringUtils.isBlank(fieldName)) {
                        fieldName = "_";
                        xmlName = "_";
                    }
                    String typeName;
                    IOpenClass t = field.getType();
                    int dim = 0;
                    while (t.isArray()) {
                        dim++;
                        t = t.getComponentClass();
                    }
                    if (t instanceof CustomSpreadsheetResultOpenClass || t instanceof SpreadsheetResultOpenClass) {
                        CustomSpreadsheetResultOpenClass csroc;
                        String fieldClsName;
                        if (t instanceof CustomSpreadsheetResultOpenClass) {
                            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) t;
                            csroc = (CustomSpreadsheetResultOpenClass) getModule()
                                .findType(customSpreadsheetResultOpenClass.getName());
                            if (csroc != null) {
                                fieldClsName = csroc.getBeanClassName();
                                csroc.generateBeanClass();
                            } else {
                                fieldClsName = getModule().getGlobalTableProperties()
                                    .getSpreadsheetResultPackage() + ".AnySpreadsheetResult";
                                getModule().getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                                    .toCustomSpreadsheetResultOpenClass()
                                    .generateBeanClass();
                            }
                        } else {
                            fieldClsName = getModule().getGlobalTableProperties()
                                .getSpreadsheetResultPackage() + ".AnySpreadsheetResult";
                            getModule().getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                                .toCustomSpreadsheetResultOpenClass()
                                .generateBeanClass();
                        }
                        typeName = dim > 0 ? IntStream.range(0, dim)
                            .mapToObj(e -> "[")
                            .collect(joining()) + "L" + fieldClsName + ";" : fieldClsName;
                    } else if (JavaOpenClass.VOID.equals(t) || JavaOpenClass.CLS_VOID.equals(t) || NullOpenClass.the
                        .equals(t)) {
                        continue; // IGNORE VOID FIELDS
                    } else {
                        Class<?> instanceClass = field.getType().getInstanceClass();
                        if (instanceClass.isPrimitive()) {
                            typeName = ClassUtils.primitiveToWrapper(instanceClass).getName();
                        } else {
                            typeName = instanceClass.getName();
                        }
                    }
                    if (!usedXmlNames.containsKey(fieldName) && !usedXmlNames.containsValue(xmlName)) {
                        FieldDescription fieldDescription = new FieldDescription(typeName,
                            null,
                            null,
                            null,
                            xmlName,
                            false);
                        beanClassBuilder.addField(fieldName, fieldDescription);
                        beanFieldsMap.put(fieldName, fillUsed(used, point, field));
                        usedXmlNames.put(fieldName, xmlName);
                    } else if (addFieldNameWithCollisions) {
                        String newFieldName = fieldName;
                        int i = 1;
                        while (usedXmlNames.containsKey(newFieldName)) {
                            newFieldName = fieldName + i;
                            i++;
                        }
                        String newXmlName = xmlName;
                        i = 1;
                        while (usedXmlNames.containsValue(newXmlName)) {
                            newXmlName = xmlName + i;
                            i++;
                        }
                        FieldDescription fieldDescription = new FieldDescription(typeName,
                            null,
                            null,
                            null,
                            newXmlName,
                            false);
                        beanClassBuilder.addField(newFieldName, fieldDescription);
                        beanFieldsMap.put(newFieldName, fillUsed(used, point, field));
                        usedXmlNames.put(newFieldName, newXmlName);
                    }
                } else {
                    boolean f = false;
                    for (IOpenField openField : used[row][column]) { // Do not add the same twice
                        if (openField.getName().equals(field.getName())) {
                            f = true;
                            break;
                        }
                    }
                    if (!f) {
                        used[row][column].add(field);
                    }
                }
            }
        }
    }

    private boolean absentInHistory(String rowName, String columnName) {
        for (Pair<String[], String[]> p : rowAndColumnNamesForResultModelHistory) {
            for (String col : p.getLeft()) {
                if (Objects.equals(columnName, col)) {
                    for (String row : p.getRight()) {
                        if (Objects.equals(rowName, row)) {
                            return false; // column and row exist in the given Spreadsheet
                        }
                    }
                    break; // Skip checking of rest columns, because of the rowName does not exist
                }
            }
        }
        return true;
    }

    private List<IOpenField> fillUsed(List<IOpenField>[][] used, Point point, IOpenField field) {
        List<IOpenField> fields = new ArrayList<>();
        fields.add(field);
        if (simpleRefBeanByRow) {
            Arrays.fill(used[point.getRow()], fields);
        } else if (simpleRefBeanByColumn) {
            for (int w = 0; w < used.length; w++) {
                used[w][point.getColumn()] = fields;
            }
        } else {
            used[point.getRow()][point.getColumn()] = fields;
        }
        return fields;
    }

    private String getBeanClassName() {
        if (beanClassName == null) {
            synchronized (this) {
                if (beanClassName == null) {
                    String name = getName();
                    if (name.startsWith(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX)) {
                        if (name.length() > Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length()) {
                            name = name.substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length());
                        }
                        String firstLetterUppercaseName = Character
                            .toUpperCase(name.charAt(0)) + (name.length() > 1 ? name.substring(1) : StringUtils.EMPTY);
                        if (getModule()
                            .findType(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + firstLetterUppercaseName) == null) {
                            name = firstLetterUppercaseName;
                        }
                    }
                    beanClassName = getModule().getGlobalTableProperties().getSpreadsheetResultPackage() + "." + name;
                }
            }
        }
        return beanClassName;
    }

    private interface SpreadsheetResultSetter {
        SpreadsheetResultSetter[] EMPTY_ARRAY = new SpreadsheetResultSetter[0];

        void set(SpreadsheetResult spreadsheetResult, Object target);
    }

    private static class SpreadsheetResultValueSetter implements SpreadsheetResultSetter {
        private final SpreadsheetResultFieldValueSetter[] spreadsheetResultFieldValueSetters;

        private SpreadsheetResultValueSetter(SpreadsheetResultFieldValueSetter[] spreadsheetResultFieldValueSetters) {
            this.spreadsheetResultFieldValueSetters = Objects.requireNonNull(spreadsheetResultFieldValueSetters);
        }

        @Override
        public void set(SpreadsheetResult spreadsheetResult, Object target) {
            for (SpreadsheetResultFieldValueSetter spreadsheetResultFieldValueSetter : spreadsheetResultFieldValueSetters) {
                if (spreadsheetResultFieldValueSetter.set(spreadsheetResult, target)) {
                    return;
                }
            }
        }
    }

    private static class SpreadsheetResultFieldValueSetter {
        static final SpreadsheetResultFieldValueSetter[] EMPTY_ARRAY = new SpreadsheetResultFieldValueSetter[0];
        private final Field field;
        private final IOpenField openField;
        private final XlsModuleOpenClass module;

        private SpreadsheetResultFieldValueSetter(XlsModuleOpenClass module, Field field, IOpenField openField) {
            this.field = Objects.requireNonNull(field);
            this.openField = Objects.requireNonNull(openField);
            this.module = Objects.requireNonNull(module);
            this.field.setAccessible(true);
        }

        public boolean set(SpreadsheetResult spreadsheetResult, Object target) {
            if (!spreadsheetResult.isFieldUsedInModel(openField.getName())) {
                return false;
            }
            Object v = openField.get(spreadsheetResult, null);
            try {
                if (v != null) {
                    Object cv = SpreadsheetResult.convertSpreadsheetResult(module, v, field.getType());
                    field.set(target, cv);
                    return true;
                }
            } catch (IllegalAccessException ignore) {
            }
            return false;
        }
    }

    private static class SpreadsheetResultColumnNamesSetter implements SpreadsheetResultSetter {
        private final Field field;

        public SpreadsheetResultColumnNamesSetter(Field field) {
            this.field = Objects.requireNonNull(field);
            this.field.setAccessible(true);
        }

        @Override
        public void set(SpreadsheetResult spreadsheetResult, Object target) {
            try {
                if (spreadsheetResult.isDetailedPlainModel()) {
                    field.set(target, spreadsheetResult.columnNames);
                }
            } catch (IllegalAccessException ignore) {
            }
        }
    }

    private static class SpreadsheetResultRowNamesSetter implements SpreadsheetResultSetter {
        private final Field field;

        public SpreadsheetResultRowNamesSetter(Field field) {
            this.field = Objects.requireNonNull(field);
            this.field.setAccessible(true);
        }

        @Override
        public void set(SpreadsheetResult spreadsheetResult, Object target) {
            try {
                if (spreadsheetResult.isDetailedPlainModel()) {
                    field.set(target, spreadsheetResult.rowNames);
                }
            } catch (IllegalAccessException ignore) {
            }
        }
    }

    private static class SpreadsheetResultFieldNamesSetter implements SpreadsheetResultSetter {
        private final Field field;
        private final Map<String, List<IOpenField>> beanFieldsMap;
        private final Map<String, String> xmlNamesMap;

        public SpreadsheetResultFieldNamesSetter(Field field,
                Map<String, List<IOpenField>> beanFieldsMap,
                Map<String, String> xmlNamesMap) {
            this.field = Objects.requireNonNull(field);
            this.beanFieldsMap = Objects.requireNonNull(beanFieldsMap);
            this.field.setAccessible(true);
            this.xmlNamesMap = Objects.requireNonNull(xmlNamesMap);
        }

        @Override
        public void set(SpreadsheetResult spreadsheetResult, Object target) {
            if (spreadsheetResult.isDetailedPlainModel()) {
                String[][] plainModelDetails = new String[spreadsheetResult.getRowNames().length][spreadsheetResult
                    .getColumnNames().length];
                for (Map.Entry<String, List<IOpenField>> e : beanFieldsMap.entrySet()) {
                    List<IOpenField> openFields = e.getValue();
                    for (IOpenField openField : openFields) {
                        Point p = spreadsheetResult.fieldsCoordinates.get(openField.getName());
                        if (p != null && spreadsheetResult.rowNamesForResultModel[p
                            .getRow()] != null && spreadsheetResult.columnNamesForResultModel[p.getColumn()] != null) {
                            plainModelDetails[p.getRow()][p.getColumn()] = xmlNamesMap.get(e.getKey());
                        }
                    }
                }
                try {
                    field.set(target, plainModelDetails);
                } catch (IllegalAccessException ignore) {
                }
            }
        }
    }

    public boolean isIgnoreCompilation() {
        return ignoreCompilation;
    }

    public void setIgnoreCompilation(boolean ignoreCompilation) {
        this.ignoreCompilation = ignoreCompilation;
    }

    @Override
    protected Map<MethodKey, IOpenMethod> initConstructorMap() {
        Map<MethodKey, IOpenMethod> constructorMap = super.initConstructorMap();
        Map<MethodKey, IOpenMethod> spreadsheetResultConstructorMap = new HashMap<>();
        for (Map.Entry<MethodKey, IOpenMethod> entry : constructorMap.entrySet()) {
            IOpenMethod constructor = new CustomSpreadsheetResultConstructor(entry.getValue(), this);
            spreadsheetResultConstructorMap.put(new MethodKey(constructor), constructor);
        }
        return spreadsheetResultConstructorMap;
    }
}
