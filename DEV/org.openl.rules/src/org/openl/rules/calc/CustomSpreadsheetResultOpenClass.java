package org.openl.rules.calc;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import static org.openl.rules.calc.ASpreadsheetField.createFieldName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.binding.impl.cast.VOID;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.binding.impl.module.ModuleSpecificType;
import org.openl.rules.calc.SpreadsheetResultBeanByteCodeGenerator.FieldDescription;
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
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;

public class CustomSpreadsheetResultOpenClass extends ADynamicClass implements ModuleSpecificType {
    private final Logger log = LoggerFactory.getLogger(CustomSpreadsheetResultOpenClass.class);
    private static final String[] EMPTY_STRING_ARRAY = new String[]{};
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
    private Map<String, Point> fieldsCoordinates;
    private final XlsModuleOpenClass module;
    private volatile Class<?> beanClass;
    private boolean simpleRefByRow;
    private boolean simpleRefByColumn;
    private boolean ignoreCompilation;

    private ILogicalTable logicalTable;

    private volatile byte[] beanClassByteCode;
    protected volatile String beanClassName;
    volatile Map<String, List<IOpenField>> beanFieldsMap;
    volatile Map<String, String> xmlNamesMap;
    private volatile boolean initializing;

    private final boolean spreadsheet;

    public CustomSpreadsheetResultOpenClass(String name,
                                            String[] rowNames,
                                            String[] columnNames,
                                            String[] rowNamesForResultModel,
                                            String[] columnNamesForResultModel,
                                            XlsModuleOpenClass module,
                                            boolean spreadsheet) {
        super(name, SpreadsheetResult.class);
        this.rowNames = Objects.requireNonNull(rowNames);
        this.columnNames = Objects.requireNonNull(columnNames);
        this.rowNamesForResultModel = Objects.requireNonNull(rowNamesForResultModel);
        this.columnNamesForResultModel = Objects.requireNonNull(columnNamesForResultModel);

        var columnsForResultModelCount = Arrays.stream(columnNamesForResultModel).filter(Objects::nonNull).count();
        var rowsForResultModelCount = Arrays.stream(rowNamesForResultModel).filter(Objects::nonNull).count();

        this.simpleRefByRow = columnsForResultModelCount == 1;
        this.simpleRefByColumn = rowsForResultModelCount == 1;

        this.fieldsCoordinates = SpreadsheetResult
                .buildFieldsCoordinates(this.columnNames, this.rowNames, this.simpleRefByColumn, this.simpleRefByRow);
        this.module = module;
        this.spreadsheet = spreadsheet;
    }

    public CustomSpreadsheetResultOpenClass(String name,
                                            XlsModuleOpenClass module,
                                            ILogicalTable logicalTable,
                                            boolean spreadsheet) {
        this(name,
                EMPTY_STRING_ARRAY,
                EMPTY_STRING_ARRAY,
                EMPTY_STRING_ARRAY,
                EMPTY_STRING_ARRAY,
                module,
                spreadsheet);
        this.simpleRefByRow = true;
        this.simpleRefByColumn = true;
        this.logicalTable = logicalTable;
    }

    public boolean isSimpleRefByColumn() {
        return simpleRefByColumn;
    }

    public boolean isSimpleRefByRow() {
        return simpleRefByRow;
    }

    public boolean isSpreadsheet() {
        return spreadsheet;
    }

    @Override
    public IOpenClass getClosestClass(ModuleSpecificType openClass) {
        return getParentClass(openClass);
    }

    @Override
    public IOpenClass getParentClass(ModuleSpecificType openClass) {
        if (openClass instanceof CustomSpreadsheetResultOpenClass) {
            CustomSpreadsheetResultOpenClass csroc = (CustomSpreadsheetResultOpenClass) openClass;
            if (getModule().isDependencyModule(csroc.getModule(), new IdentityHashMap<>())) {
                return getModule().buildOrGetCombinedSpreadsheetResult(this, csroc);
            } else {
                return AnySpreadsheetResultOpenClass.INSTANCE;
            }
        }
        return null;
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
        if (ioc instanceof CustomSpreadsheetResultOpenClass && !(ioc instanceof CombinedSpreadsheetResultOpenClass)) {
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) ioc;
            return getModule().isDependencyModule(customSpreadsheetResultOpenClass.getModule(),
                    new IdentityHashMap<>()) && this.getName().equals(customSpreadsheetResultOpenClass.getName());
        }
        return false;
    }

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    public byte[] getBeanClassByteCode() {
        return beanClassByteCode.clone();
    }

    @Override
    public Collection<IOpenClass> superClasses() {
        return Collections.singleton(getModule().getSpreadsheetResultOpenClassWithResolvedFieldTypes());
    }

    protected IOpenField searchFieldFromSuperClass(String fname, boolean strictMatch) throws AmbiguousFieldException {
        return null;
    }

    public XlsModuleOpenClass getModule() {
        return module;
    }

    private void extendSpreadsheetResult(String[] rowNames,
                                         String[] columnNames,
                                         String[] rowNamesForResultModel,
                                         String[] columnNamesForResultModel,
                                         Collection<IOpenField> fields,
                                         boolean simpleRefByRow,
                                         boolean simpleRefByColumn) {
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

        boolean rowColumnsForResultModelNeedUpdate = false;

        for (int i = 0; i < rowNames.length; i++) {
            if (!existedRowNamesSet.contains(rowNames[i])) {
                nRowNames.add(rowNames[i]);
                nRowNamesForResultModel.add(rowNamesForResultModel[i]);
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
                rowColumnsForResultModelNeedUpdate = true;
            } else if (columnNamesForResultModel[i] != null) {
                int k = nColumnNames.indexOf(columnNames[i]);
                nColumnNamesForResultModel.set(k, columnNamesForResultModel[i]);
                rowColumnsForResultModelNeedUpdate = true;
            }
        }

        if (rowColumnsForResultModelNeedUpdate) {
            this.simpleRefByRow = simpleRefByRow && this.simpleRefByRow;
            this.simpleRefByColumn = simpleRefByColumn && this.simpleRefByColumn;

            this.rowNamesForResultModel = nRowNamesForResultModel.toArray(EMPTY_STRING_ARRAY);
            this.columnNamesForResultModel = nColumnNamesForResultModel.toArray(EMPTY_STRING_ARRAY);

            this.rowNames = nRowNames.toArray(EMPTY_STRING_ARRAY);
            this.columnNames = nColumnNames.toArray(EMPTY_STRING_ARRAY);

            this.fieldsCoordinates = Collections.unmodifiableMap(SpreadsheetResult
                    .buildFieldsCoordinates(this.columnNames, this.rowNames, this.simpleRefByColumn, this.simpleRefByRow));
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
    }

    public String[] getRowNames() {
        return rowNames.clone();
    }

    public String[] getColumnNames() {
        return columnNames.clone();
    }

    public Map<String, Point> getFieldsCoordinates() {
        return fieldsCoordinates;
    }

    @Override
    public void updateWithType(IOpenClass openClass) {
        if (beanClassByteCode != null) {
            throw new IllegalStateException(
                    "Java bean class for custom spreadsheet result is loaded to classloader. " + "Custom spreadsheet result cannot be extended.");
        }
        if (openClass instanceof SpreadsheetResultOpenClass) {
            this.updateWithType(((SpreadsheetResultOpenClass) openClass).toCustomSpreadsheetResultOpenClass());
            return;
        }
        CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) openClass;
        if (customSpreadsheetResultOpenClass.getModule() != getModule()) {
            customSpreadsheetResultOpenClass = customSpreadsheetResultOpenClass.convertToModuleType(getModule(), false);
        }
        this.extendSpreadsheetResult(customSpreadsheetResultOpenClass.rowNames,
                customSpreadsheetResultOpenClass.columnNames,
                customSpreadsheetResultOpenClass.rowNamesForResultModel,
                customSpreadsheetResultOpenClass.columnNamesForResultModel,
                customSpreadsheetResultOpenClass.getFields(),
                customSpreadsheetResultOpenClass.simpleRefByRow,
                customSpreadsheetResultOpenClass.simpleRefByColumn);

        eventsOnUpdateWithType.forEach(e -> e.accept(this));
    }

    private final Collection<Consumer<CustomSpreadsheetResultOpenClass>> eventsOnUpdateWithType = new ArrayList<>();

    public void addEventOnUpdateWithType(Consumer<CustomSpreadsheetResultOpenClass> c) {
        eventsOnUpdateWithType.add(c);
    }

    @Override
    public Collection<IOpenField> getFields() {
        return Collections.unmodifiableCollection(fieldMap().values());
    }

    private IOpenField fixModuleFieldType(IOpenField openField) {
        IOpenClass type = openField.getType();
        int dim = 0;
        while (type.isArray()) {
            type = type.getComponentClass();
            dim++;
        }
        IOpenClass t = getModule().toModuleType(type);
        if (t != type) {
            if (dim > 0) {
                t = t.getArrayType(dim);
            }
            return new CustomSpreadsheetResultField(this, openField.getName(), t);
        }
        return openField;
    }

    /**
     * Convert this type to a type belongs to another module and register it in the provided module.
     *
     * @param module
     * @return converted and registered type
     */
    @Override
    public CustomSpreadsheetResultOpenClass convertToModuleTypeAndRegister(ModuleOpenClass module) {
        return convertToModuleType(module, true);
    }

    protected CustomSpreadsheetResultOpenClass convertToModuleType(ModuleOpenClass module, boolean register) {
        if (getModule() != module) {
            if (register && module.findType(getName()) != null) {
                throw new IllegalStateException("Type has already exists in the module.");
            }
            CustomSpreadsheetResultOpenClass type = new CustomSpreadsheetResultOpenClass(getName(),
                    rowNames,
                    columnNames,
                    rowNamesForResultModel,
                    columnNamesForResultModel,
                    (XlsModuleOpenClass) module,
                    spreadsheet);
            type.simpleRefByRow = this.simpleRefByRow;
            type.simpleRefByColumn = this.simpleRefByColumn;
            if (register) {
                module.addType(type);
            }
            for (IOpenField field : getFields()) {
                if (field instanceof CustomSpreadsheetResultField) {
                    type.addField(type.fixModuleFieldType(field));
                } else {
                    type.addField(field);
                }
            }
            type.setMetaInfo(getMetaInfo());
            type.logicalTable = this.logicalTable;
            return type;
        }
        return this;
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
        spr.setCustomSpreadsheetResultOpenClass(this);
        spr.setLogicalTable(logicalTable);
        return spr;
    }

    public Object createBean(SpreadsheetResult spreadsheetResult) {
        return createBean(spreadsheetResult, null);
    }

    public Object createBean(SpreadsheetResult spreadsheetResult,
                             SpreadsheetResultBeanPropertyNamingStrategy spreadsheetResultBeanPropertyNamingStrategy) {
        Class<?> clazz = getBeanClass();
        Object target;
        try {
            target = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.debug("Ignored error: ", e);
            return null;
        }

        for (Map.Entry<String, List<IOpenField>> cell : beanFieldsMap.entrySet()) {
            var fieldName = cell.getKey();
            Object v;
            for (IOpenField openField : cell.getValue()) {
                if (spreadsheetResult.isFieldUsedInModel(openField.getName())) {
                    v = openField.get(spreadsheetResult, null);
                    if (v != null) {
                        Object cv = SpreadsheetResult.convertSpreadsheetResult(v,
                                ClassUtils.getType(target, fieldName),
                                openField.getType(),
                                spreadsheetResultBeanPropertyNamingStrategy);
                        try {
                            ClassUtils.set(target, fieldName, cv);
                        } catch (Exception e) {
                            log.debug("Ignored error: ", e);
                            continue;
                        }
                        break;
                    }
                }
            }
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
                        this.beanClass = getModule().getClassGenerationClassLoader().loadClass(getBeanClassName());
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

    protected void generateBeanClass() {
        if (beanClassByteCode == null) {
            synchronized (this) {
                if (beanClassByteCode == null && !initializing) {
                    try {
                        initializing = true;
                        TreeMap<String, String> xmlNames = new TreeMap<>(FIELD_COMPARATOR);
                        @SuppressWarnings("unchecked")
                        List<IOpenField>[][] used = new List[rowNames.length][columnNames.length];
                        Map<String, List<IOpenField>> fieldsMap = new HashMap<>();
                        List<Pair<Point, IOpenField>> fields = getListOfFields();
                        IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache = new IdentityHashMap<>();
                        var beanFields = new ArrayList<FieldDescription>();
                        addFieldsToJavaClassBuilder(beanFields, fields, used, xmlNames, true, fieldsMap, cache);
                        addFieldsToJavaClassBuilder(beanFields, fields, used, xmlNames, false, fieldsMap, cache);

                        final String beanClassName = getBeanClassName();
                        byte[] bc = SpreadsheetResultBeanByteCodeGenerator.byteCode(beanClassName, beanFields);
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

    private List<Pair<Point, IOpenField>> getListOfFields() {
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

    private static final Comparator<Pair<Point, IOpenField>> COMP = Comparator.comparing(Pair::getLeft,
            Comparator.nullsLast(Comparator.comparingInt(Point::getRow).thenComparingInt(Point::getColumn)));

    public boolean isExternalCustomSpreadsheetResultOpenClass(
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass,
            IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
        return !getModule().isDependencyModule(customSpreadsheetResultOpenClass.getModule(), cache);
    }

    public boolean isExternalSpreadsheetResultOpenClass(SpreadsheetResultOpenClass spreadsheetResultOpenClass,
                                                        IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
        return !getModule().isDependencyModule(spreadsheetResultOpenClass.getModule(), cache);
    }

    private void addFieldsToJavaClassBuilder(List<FieldDescription> beanFields,
                                             List<Pair<Point, IOpenField>> fields,
                                             List<IOpenField>[][] used,
                                             Map<String, String> usedXmlNames,
                                             boolean addFieldNameWithCollisions,
                                             Map<String, List<IOpenField>> beanFieldsMap,
                                             IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
        for (Pair<Point, IOpenField> pair : fields) {
            Point point = pair.getLeft();
            if (point == null) {
                continue;
            }
            int row = point.getRow();
            int column = point.getColumn();
            String rowName = rowNamesForResultModel[row];
            String columnName = columnNamesForResultModel[column];
            if (rowName != null && columnName != null) {
                IOpenField field;
                if (used[row][column] == null) {
                    String fieldName;
                    String xmlName;
                    if (simpleRefByRow || StringUtils.isBlank(columnName)) {
                        fieldName = rowName;
                        xmlName = rowName;
                        field = getField(createFieldName(null, rowName));
                    } else if (simpleRefByColumn || StringUtils.isBlank(rowName)) {
                        fieldName = columnName;
                        xmlName = columnName;
                        field = getField(createFieldName(columnName, null));
                    } else {
                        fieldName = columnName + StringUtils.capitalize(rowName);
                        xmlName = columnName + "_" + rowName;
                        field = getField(createFieldName(columnName, rowName));
                    }
                    if (field == null) {
                        field = pair.getRight();
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
                    if (t instanceof CustomSpreadsheetResultOpenClass || t instanceof SpreadsheetResultOpenClass || t instanceof AnySpreadsheetResultOpenClass) {
                        String fieldClsName;
                        XlsModuleOpenClass additionalClassGenerationClassloaderModule = null;
                        if (t instanceof CustomSpreadsheetResultOpenClass) {
                            CustomSpreadsheetResultOpenClass csroc = (CustomSpreadsheetResultOpenClass) t;
                            boolean externalCustomSpreadsheetResultOpenClass = isExternalCustomSpreadsheetResultOpenClass(
                                    csroc,
                                    cache);
                            if (externalCustomSpreadsheetResultOpenClass) {
                                additionalClassGenerationClassloaderModule = csroc.getModule();
                            }
                            fieldClsName = csroc.getBeanClassName();
                            csroc.generateBeanClass();
                        } else if (t instanceof SpreadsheetResultOpenClass) {
                            SpreadsheetResultOpenClass spreadsheetResultOpenClass = (SpreadsheetResultOpenClass) t;
                            final boolean externalSpreadsheetResultOpenClass = isExternalSpreadsheetResultOpenClass(
                                    spreadsheetResultOpenClass,
                                    cache);
                            XlsModuleOpenClass m = externalSpreadsheetResultOpenClass ? spreadsheetResultOpenClass
                                    .getModule() : getModule();
                            if (externalSpreadsheetResultOpenClass) {
                                additionalClassGenerationClassloaderModule = spreadsheetResultOpenClass.getModule();
                            }
                            fieldClsName = m.getGlobalTableProperties()
                                    .getSpreadsheetResultPackage() + ".AnySpreadsheetResult";
                            m.getSpreadsheetResultOpenClassWithResolvedFieldTypes()
                                    .toCustomSpreadsheetResultOpenClass()
                                    .generateBeanClass();
                        } else {
                            fieldClsName = Map.class.getCanonicalName();
                        }
                        if (additionalClassGenerationClassloaderModule != null) {
                            getModule().getClassGenerationClassLoader()
                                    .addClassLoader(
                                            additionalClassGenerationClassloaderModule.getClassGenerationClassLoader());
                        }
                        typeName = fieldClsName + "[]".repeat(dim);
                    } else if (JavaOpenClass.VOID.equals(t) || JavaOpenClass.CLS_VOID.equals(t) || NullOpenClass.the
                            .equals(t) || JavaOpenClass.getOpenClass(VOID.class).equals(t)) {
                        continue; // IGNORE VOID FIELDS
                    } else {
                        Class<?> instanceClass = field.getType().getInstanceClass();
                        if (instanceClass.isPrimitive()) {
                            typeName = ClassUtils.primitiveToWrapper(instanceClass).getName();
                        } else {
                            typeName = instanceClass.getCanonicalName();
                        }
                    }

                    fieldName = ClassUtils.decapitalize(fieldName); // FIXME: WSDL decapitalize field name without this
                    if (!usedXmlNames.containsKey(fieldName) && !usedXmlNames.containsValue(xmlName) || addFieldNameWithCollisions) {
                        if (usedXmlNames.containsKey(fieldName) || usedXmlNames.containsValue(xmlName)) {
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
                            fieldName = newFieldName;
                            xmlName = newXmlName;
                        }

                        FieldDescription fieldDescription = new FieldDescription(typeName,
                                simpleRefByRow || !simpleRefByColumn ? rowNames[row] : null,
                                !simpleRefByRow ? columnNames[column] : null,
                                simpleRefByColumn
                        );
                        beanFields.add(fieldDescription);
                        beanFieldsMap.put(fieldName, fillUsed(used, point, field));
                        usedXmlNames.put(fieldName, xmlName);
                    }
                } else {
                    boolean f = false;
                    for (IOpenField openField : used[row][column]) { // Do not add the same twice
                        if (openField.getName().equals(pair.getRight().getName())) {
                            f = true;
                            break;
                        }
                    }
                    if (!f) {
                        used[row][column].add(pair.getRight());
                    }
                }
            }
        }
    }

    private List<IOpenField> fillUsed(List<IOpenField>[][] used, Point point, IOpenField field) {
        List<IOpenField> fields = new ArrayList<>();
        fields.add(field);
        if (simpleRefByRow) {
            Arrays.fill(used[point.getRow()], fields);
        } else if (simpleRefByColumn) {
            for (int w = 0; w < used.length; w++) {
                used[w][point.getColumn()] = fields;
            }
        } else {
            used[point.getRow()][point.getColumn()] = fields;
        }
        return fields;
    }

    protected String spreadsheetResultNameToBeanName(String name) {
        if (name.startsWith(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX)) {
            if (name.length() > Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length()) {
                name = name.substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length());
            }
            String firstLetterUppercaseName = StringUtils.capitalize(name);
            if (getModule().findType(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + firstLetterUppercaseName) == null) {
                name = firstLetterUppercaseName;
            }
        }
        return name;
    }

    protected String getBeanClassName() {
        if (beanClassName == null) {
            synchronized (this) {
                if (beanClassName == null) {
                    String name = spreadsheetResultNameToBeanName(getName());
                    beanClassName = getModule().getGlobalTableProperties().getSpreadsheetResultPackage() + "." + name;
                }
            }
        }
        return beanClassName;
    }

    public SpreadsheetResult createSpreadsheetResult(Object bean,
                                                     Map<Class<?>, CustomSpreadsheetResultOpenClass> mapClassToSpr) {
        SpreadsheetResult spreadsheetResult = (SpreadsheetResult) newInstance(null);
        for (Map.Entry<String, List<IOpenField>> cell : beanFieldsMap.entrySet()) {
            var fieldName = cell.getKey();
            Object v;
            try {
                v = ClassUtils.get(bean, fieldName);
            } catch (Exception e) {
                log.debug("Ignored error: ", e);
                continue;
            }
            Object cv = SpreadsheetResult.convertBeansToSpreadsheetResults(v, mapClassToSpr);
            var openField = cell.getValue().get(0);
            openField.set(spreadsheetResult, cv, null);
        }
        return spreadsheetResult;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;

        CustomSpreadsheetResultOpenClass that = (CustomSpreadsheetResultOpenClass) o;

        return Objects.equals(module, that.module) && Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (module != null ? module.hashCode() : 0);
        return result;
    }
}
