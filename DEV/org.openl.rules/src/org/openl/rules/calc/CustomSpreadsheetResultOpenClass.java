package org.openl.rules.calc;

import static java.util.stream.Collectors.toSet;

import static org.openl.rules.calc.ASpreadsheetField.createFieldName;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.binding.impl.cast.IOpenCast;
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
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ArrayUtils;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;

@Slf4j
public class CustomSpreadsheetResultOpenClass extends ADynamicClass implements ModuleSpecificType {
    private static final String[] EMPTY_STRING_ARRAY = new String[]{};
    private static final String[][] EMPTY_DESCRIPTIONS_ARRAY = new String[][]{};
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
    @Getter
    private Map<String, Point> fieldsCoordinates;
    @Getter
    private final XlsModuleOpenClass module;
    private final AtomicReference<Class<?>> beanClass = new AtomicReference<>();
    @Getter
    private boolean simpleRefByRow;
    @Getter
    private boolean simpleRefByColumn;
    @Getter
    @Setter
    private boolean ignoreCompilation;

    @Getter
    private ILogicalTable logicalTable;

    private final AtomicReference<GeneratedBean> generatedBean = new AtomicReference<>();
    protected volatile String beanClassName;
    private volatile boolean initializing;

    /** The generated bean: its bytecode, the spreadsheet fields behind each of its fields and their XML names. */
    @Getter
    @RequiredArgsConstructor
    private static final class GeneratedBean {
        private final byte[] byteCode;
        private final Map<String, List<IOpenField>> fieldsMap;
        private final Map<String, String> xmlNames;
    }

    private String[][] descriptions;
    @Getter
    private final boolean spreadsheet;

    // The type is built from the row and column model of the spreadsheet result.
    @SuppressWarnings("java:S107")
    public CustomSpreadsheetResultOpenClass(String name,
                                            String[] rowNames,
                                            String[] columnNames,
                                            String[] rowNamesForResultModel,
                                            String[] columnNamesForResultModel,
                                            String[][] descriptions,
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
                .buildFieldsCoordinates2(this.columnNames, this.rowNames, this.columnNamesForResultModel, this.rowNamesForResultModel);
        this.module = module;
        this.spreadsheet = spreadsheet;
        this.descriptions = descriptions;
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
                EMPTY_DESCRIPTIONS_ARRAY,
                module,
                spreadsheet);
        this.simpleRefByRow = true;
        this.simpleRefByColumn = true;
        this.logicalTable = logicalTable;
    }

    @Override
    public IOpenClass getClosestClass(ModuleSpecificType openClass) {
        return getParentClass(openClass);
    }

    @Override
    public IOpenClass getParentClass(ModuleSpecificType openClass) {
        if (openClass instanceof CustomSpreadsheetResultOpenClass csroc) {
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
            throw new IllegalStateException("Expected type '%s', but found type '%s'.".formatted(
                    CustomSpreadsheetResultField.class.getTypeName(),
                    field.getClass().getTypeName()));
        }
        super.addField(field);
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        if (ioc instanceof CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass && !(ioc instanceof CombinedSpreadsheetResultOpenClass)) {
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
        return generatedBean.get().getByteCode().clone();
    }

    @Override
    public Collection<IOpenClass> superClasses() {
        return Set.of(getModule().getSpreadsheetResultOpenClassWithResolvedFieldTypes());
    }

    @Override
    protected IOpenField searchFieldFromSuperClass(String fname, boolean strictMatch) throws AmbiguousFieldException {
        return null;
    }

    private String chooseBestDescription(String description1, String description2) {
        // Choose the longest description, if length is equal choose the alphabetically first.
        if (description1 == null) {
            return description2;
        }
        if (description2 == null) {
            return description1;
        }
        if (description1.length() > description2.length()) {
            return description1;
        }
        if (description1.length() < description2.length()) {
            return description2;
        }
        return description1.compareTo(description2) < 0 ? description1 : description2;
    }

    // Extending takes the same row and column model the type is built from.
    @SuppressWarnings("java:S107")
    private void extendSpreadsheetResult(String[] rowNames,
                                         String[] columnNames,
                                         String[] rowNamesForResultModel,
                                         String[] columnNamesForResultModel,
                                         String[][] descriptions,
                                         Collection<IOpenField> fields,
                                         boolean simpleRefByRow,
                                         boolean simpleRefByColumn) {
        if (beanClass.get() != null) {
            throw new IllegalStateException(
                    "Bean class for custom spreadsheet result is already generated. This spreadsheet result type cannot be extended.");
        }

        var nRowNames = new ArrayList<>(Arrays.asList(this.rowNames));
        var nRowNamesForResultModel = new ArrayList<>(Arrays.asList(this.rowNamesForResultModel));
        var existedRowNamesSet = Arrays.stream(this.rowNames).collect(toSet());

        var nColumnNames = new ArrayList<>(Arrays.asList(this.columnNames));
        var nColumnNamesForResultModel = new ArrayList<>(Arrays.asList(this.columnNamesForResultModel));
        var existedColumnNamesSet = Arrays.stream(this.columnNames).collect(toSet());

        var rowNamesForResultModelNeedUpdate = mergeNames(rowNames,
                rowNamesForResultModel,
                existedRowNamesSet,
                nRowNames,
                nRowNamesForResultModel);
        var columnNamesForResultModelNeedUpdate = mergeNames(columnNames,
                columnNamesForResultModel,
                existedColumnNamesSet,
                nColumnNames,
                nColumnNamesForResultModel);

        this.descriptions = mergeDescriptions(nRowNames, nColumnNames, rowNames, columnNames, descriptions);

        if (rowNamesForResultModelNeedUpdate || columnNamesForResultModelNeedUpdate) {
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
            var thisField = getField(field.getName());
            if (thisField == null) {
                addField(new CustomSpreadsheetResultField(this, field));
            } else {
                fieldMap().put(field.getName(),
                        new CastingCustomSpreadsheetResultField(this, field.getName(), thisField, field));
            }
        }
    }

    /**
     * Appends the names that are not merged yet, and replaces the result model names of the merged ones when given.
     *
     * @return {@code true} when the merged result model names are changed
     */
    private static boolean mergeNames(String[] names,
                                      String[] namesForResultModel,
                                      Set<String> existedNamesSet,
                                      List<String> nNames,
                                      List<String> nNamesForResultModel) {
        var namesForResultModelNeedUpdate = false;
        for (var i = 0; i < names.length; i++) {
            if (!existedNamesSet.contains(names[i])) {
                nNames.add(names[i]);
                nNamesForResultModel.add(namesForResultModel[i]);
                namesForResultModelNeedUpdate = true;
            } else if (namesForResultModel[i] != null) {
                var k = nNames.indexOf(names[i]);
                nNamesForResultModel.set(k, namesForResultModel[i]);
                namesForResultModelNeedUpdate = true;
            }
        }
        return namesForResultModelNeedUpdate;
    }

    /**
     * Builds the descriptions of the merged rows and columns, choosing the best one when both spreadsheets describe
     * the same cell.
     */
    private String[][] mergeDescriptions(List<String> nRowNames,
                                         List<String> nColumnNames,
                                         String[] rowNames,
                                         String[] columnNames,
                                         String[][] descriptions) {
        String[][] newDescriptions = new String[nRowNames.size()][nColumnNames.size()];
        var rowNames1 = Arrays.stream(rowNames).toList();
        var colNames1 = Arrays.stream(columnNames).toList();
        for (var i = 0; i < nRowNames.size(); i++) {
            for (var j = 0; j < nColumnNames.size(); j++) {
                if (i < this.descriptions.length && j < this.descriptions[i].length) {
                    newDescriptions[i][j] = this.descriptions[i][j];
                }
                var i0 = rowNames1.indexOf(nRowNames.get(i));
                var j0 = colNames1.indexOf(nColumnNames.get(j));
                if (i0 >= 0 && j0 >= 0) {
                    newDescriptions[i][j] = chooseBestDescription(newDescriptions[i][j], descriptions[i0][j0]);
                }
            }
        }
        return newDescriptions;
    }

    public String[] getRowNames() {
        return rowNames.clone();
    }

    public String[] getColumnNames() {
        return columnNames.clone();
    }

    @Override
    public void updateWithType(IOpenClass openClass) {
        if (generatedBean.get() != null) {
            throw new IllegalStateException(
                    """
                    Java bean class for custom spreadsheet result is loaded to classloader. \
                    Custom spreadsheet result cannot be extended.""");
        }
        if (openClass instanceof SpreadsheetResultOpenClass class1) {
            this.updateWithType(class1.toCustomSpreadsheetResultOpenClass());
            return;
        }
        var customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) openClass;
        if (customSpreadsheetResultOpenClass.getModule() != getModule()) {
            customSpreadsheetResultOpenClass = customSpreadsheetResultOpenClass.convertToModuleType(getModule(), false);
        }
        this.extendSpreadsheetResult(customSpreadsheetResultOpenClass.rowNames,
                customSpreadsheetResultOpenClass.columnNames,
                customSpreadsheetResultOpenClass.rowNamesForResultModel,
                customSpreadsheetResultOpenClass.columnNamesForResultModel,
                customSpreadsheetResultOpenClass.descriptions,
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
        var type = openField.getType();
        var dim = 0;
        while (type.isArray()) {
            type = type.getComponentClass();
            dim++;
        }
        var t = getModule().toModuleType(type);
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
            var type = new CustomSpreadsheetResultOpenClass(getName(),
                    rowNames,
                    columnNames,
                    rowNamesForResultModel,
                    columnNamesForResultModel,
                    descriptions,
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

    @Override
    public Object newInstance(IRuntimeEnv env) {
        var spr = new SpreadsheetResult(new Object[rowNames.length][columnNames.length],
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
        return createBean(clazz, spreadsheetResult, spreadsheetResultBeanPropertyNamingStrategy);
    }

    public static Object createBean(Class<?> clazz, SpreadsheetResult spreadsheetResult, SpreadsheetResultBeanPropertyNamingStrategy namingStrategy) {
        try {
            var method = clazz.getMethod("valueOf", SpreadsheetResult.class, BiFunction.class);
            return method.invoke(null, spreadsheetResult, new BeanValueConvertor(spreadsheetResult, namingStrategy));
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Converts the values of a spreadsheet result to the types of the bean properties.
     */
    @RequiredArgsConstructor
    private static final class BeanValueConvertor implements BiFunction<Object, Class<?>, Object> {

        private final SpreadsheetResult spreadsheetResult;
        private final SpreadsheetResultBeanPropertyNamingStrategy namingStrategy;

        @Override
        public Object apply(Object v, Class<?> toClass) {
            if (v == null) {
                return JavaOpenClass.getOpenClass(toClass).nullObject();
            }
            var fromClass = v.getClass();
            if (toClass.equals(Object.class) && v instanceof SpreadsheetResult sr) {
                var customClass = sr.getCustomSpreadsheetResultOpenClass();
                if (customClass != null) {
                    return customClass.createBean(sr, namingStrategy);
                }
            }

            if (isSpreadsheetResultArrayToObject(fromClass, toClass)) {
                // Optimized case for SpreadsheetResult[][][]...
                return ArrayUtils.convert(v, x -> apply(x, Object.class));
            }
            if (v instanceof SpreadsheetResult result
                    && toClass.isAnnotationPresent(SpreadsheetResultBeanClass.class)) {
                return createBean(toClass, result, namingStrategy);
            }
            if (v instanceof Collection<?> collection) {
                return convertCollection(collection);
            }
            if (v instanceof Map<?, ?> map) {
                return convertMap(map);
            }
            if (ClassUtils.isAssignable(fromClass, toClass)) {
                // Optimization for the trivial case without type conversion
                return v;
            }

            if (toClass.isArray()) {
                return convertToArray(v, toClass.getComponentType());
            }

            var cast = findImplicitCast(fromClass, toClass);
            if (cast != null) {
                // Embedded conversion
                return cast.convert(v);
            }

            // Fallback to old implementation.
            return SpreadsheetResult.convertSpreadsheetResult(v, toClass, null, namingStrategy);
        }

        /**
         * Checks whether an array of spreadsheet results is converted to {@code Object} or to an array of it.
         */
        private static boolean isSpreadsheetResultArrayToObject(Class<?> fromClass, Class<?> toClass) {
            var toComponentType = toClass;
            while (toComponentType.isArray()) {
                toComponentType = toComponentType.getComponentType();
            }
            if (toComponentType.equals(Object.class) && fromClass.isArray()) {
                var fromComponentType = fromClass;
                while (fromComponentType.isArray()) {
                    fromComponentType = fromComponentType.getComponentType();
                }
                return SpreadsheetResult.class.isAssignableFrom(fromComponentType);
            }
            return false;
        }

        private Object convertCollection(Collection<?> collection) {
            try {
                Collection<Object> newCollection = collection.getClass().getDeclaredConstructor().newInstance();
                for (var o : collection) {
                    newCollection.add(apply(o, Object.class));
                }
                return newCollection;
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                    | InvocationTargetException e) {
                return collection;
            }
        }

        private Object convertMap(Map<?, ?> map) {
            try {
                Map<Object, Object> newCollection = map.getClass().getDeclaredConstructor().newInstance();
                for (var o : map.entrySet()) {
                    newCollection.put(apply(o.getKey(), Object.class), apply(o.getValue(), Object.class));
                }
                return newCollection;
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
                    | InvocationTargetException e) {
                return map;
            }
        }

        private Object convertToArray(Object v, Class<?> componentType) {
            if (v.getClass().isArray()) {
                var len = Array.getLength(v);
                var array = Array.newInstance(componentType, len);
                for (var i = 0; i < len; i++) {
                    Array.set(array, i, apply(Array.get(v, i), componentType));
                }
                return array;
            } else {
                var array = Array.newInstance(componentType, 1);
                Array.set(array, 0, apply(v, componentType));
                return array;
            }
        }

        /**
         * Finds an implicit cast in the module of the converted spreadsheet result.
         */
        private IOpenCast findImplicitCast(Class<?> fromClass, Class<?> toClass) {
            var openClass = spreadsheetResult.getCustomSpreadsheetResultOpenClass();
            if (openClass != null) {
                var resultModule = openClass.getModule();
                if (resultModule != null) {
                    var cast = resultModule.getObjectToDataOpenCastConvertor().getConvertor(fromClass, toClass);
                    if (cast != null && cast.isImplicit()) {
                        return cast;
                    }
                }
            }
            return null;
        }
    }

    public boolean isBeanClassInitialized() {
        return beanClass.get() != null;
    }

    public Class<?> getBeanClass() {
        var type = beanClass.get();
        if (type == null) {
            synchronized (this) {
                type = beanClass.get();
                if (type == null) {
                    try {
                        generateBeanClass();
                        type = getModule().getClassGenerationClassLoader().loadClass(getBeanClassName());
                        beanClass.set(type);
                    } catch (Exception | LinkageError e) {
                        throw new IllegalStateException(
                                "Failed to create bean class for '%s' spreadsheet result.".formatted(getName()),
                                e);
                    }
                }
            }
        }
        return type;
    }

    protected void generateBeanClass() {
        if (generatedBean.get() == null) {
            synchronized (this) {
                if (generatedBean.get() == null && !initializing) {
                    try {
                        initializing = true;
                        var xmlNames = new TreeMap<String, String>(FIELD_COMPARATOR);
                        @SuppressWarnings("unchecked")
                        List<IOpenField>[][] used = new List[rowNames.length][columnNames.length];
                        var fieldsMap = new HashMap<String, List<IOpenField>>();
                        List<Pair<Point, IOpenField>> fields = getListOfFields();
                        var cache = new IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>>();
                        var beanFields = new ArrayList<FieldDescription>();
                        addFieldsToJavaClassBuilder(beanFields, fields, used, xmlNames, true, fieldsMap, cache);
                        addFieldsToJavaClassBuilder(beanFields, fields, used, xmlNames, false, fieldsMap, cache);

                        final var className = getBeanClassName();
                        var bc = generateBytecode(className, beanFields);
                        getModule().getClassGenerationClassLoader().addGeneratedClass(className, bc);

                        generatedBean.set(new GeneratedBean(bc,
                                Collections.unmodifiableMap(fieldsMap),
                                Collections.unmodifiableMap(xmlNames)));
                    } finally {
                        initializing = false;
                    }
                }
            }
        }
    }

    protected byte[] generateBytecode(String beanClassName, List<FieldDescription> beanFields) {
        return SpreadsheetResultBeanByteCodeGenerator.byteCode(beanClassName, beanFields);
    }

    private List<Pair<Point, IOpenField>> getListOfFields() {
        return getFields().stream()
                .map(e -> Pair.of(fieldsCoordinates.get(e.getName()), e))
                .sorted(COMP)
                .toList();
    }

    public Map<String, List<IOpenField>> getBeanFieldsMap() {
        return generatedBean().getFieldsMap();
    }

    public Map<String, String> getXmlNamesMap() {
        return generatedBean().getXmlNames();
    }

    private GeneratedBean generatedBean() {
        var bean = generatedBean.get();
        if (bean == null) {
            generateBeanClass();
            bean = generatedBean.get();
        }
        if (bean == null) {
            throw new IllegalStateException(
                    "The bean class of '%s' spreadsheet result is not generated yet.".formatted(getName()));
        }
        return bean;
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
        for (Pair<Point, IOpenField> pair : fields.stream().filter(e -> e.getLeft() != null).toList()) {
            var point = pair.getLeft();
            var row = point.getRow();
            var column = point.getColumn();
            var rowName = rowNamesForResultModel[row];
            var columnName = columnNamesForResultModel[column];
            if (rowName != null && columnName != null) {
                if (used[row][column] == null) {
                    addBeanField(beanFields,
                            pair,
                            used,
                            usedXmlNames,
                            addFieldNameWithCollisions,
                            beanFieldsMap,
                            cache);
                } else {
                    addUsedField(used[row][column], pair.getRight());
                }
            }
        }
    }

    /**
     * Adds a bean property for the field of a cell. A field of a void type is skipped.
     *
     * <p>When the property name or the XML name is used already, the property is skipped, or it is added under unique
     * names if collisions are allowed.
     */
    private void addBeanField(List<FieldDescription> beanFields,
                              Pair<Point, IOpenField> pair,
                              List<IOpenField>[][] used,
                              Map<String, String> usedXmlNames,
                              boolean addFieldNameWithCollisions,
                              Map<String, List<IOpenField>> beanFieldsMap,
                              IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
        var point = pair.getLeft();
        var namedField = toNamedField(pair);
        var field = namedField.field();
        var fieldName = namedField.fieldName();
        var xmlName = namedField.xmlName();
        String typeName;
        var t = field.getType();
        var dim = 0;
        while (t.isArray()) {
            dim++;
            t = t.getComponentClass();
        }
        if (t instanceof CustomSpreadsheetResultOpenClass || t instanceof SpreadsheetResultOpenClass
                || t instanceof AnySpreadsheetResultOpenClass) {
            typeName = getSpreadsheetResultBeanClassName(t, cache) + "[]".repeat(dim);
        } else if (JavaOpenClass.VOID.equals(t) || JavaOpenClass.CLS_VOID.equals(t) || NullOpenClass.the
                .equals(t) || JavaOpenClass.getOpenClass(VOID.class).equals(t)) {
            return; // IGNORE VOID FIELDS
        } else {
            typeName = getJavaTypeName(field.getType().getInstanceClass());
        }

        fieldName = ClassUtils.decapitalize(fieldName);
        if (!usedXmlNames.containsKey(fieldName) && !usedXmlNames.containsValue(xmlName)
                || addFieldNameWithCollisions) {
            if (usedXmlNames.containsKey(fieldName) || usedXmlNames.containsValue(xmlName)) {
                fieldName = uniqueName(fieldName, usedXmlNames::containsKey);
                xmlName = uniqueName(xmlName, usedXmlNames::containsValue);
            }

            var fieldDescription = createFieldDescription(typeName, point, field);
            beanFields.add(fieldDescription);
            beanFieldsMap.put(fieldName, fillUsed(used, point, field));
            usedXmlNames.put(fieldName, xmlName);
        }
    }

    /**
     * The field of a cell with the property name and the XML name of the bean property for it.
     */
    private record NamedField(IOpenField field, String fieldName, String xmlName) {
    }

    /**
     * Names the bean property of a cell by its row, by its column, or by both, and finds the field referenced by
     * these names. The field of the cell is used when no such field exists.
     */
    private NamedField toNamedField(Pair<Point, IOpenField> pair) {
        var point = pair.getLeft();
        var rowName = rowNamesForResultModel[point.getRow()];
        var columnName = columnNamesForResultModel[point.getColumn()];
        String fieldName;
        String xmlName;
        IOpenField field;
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
        return new NamedField(field, fieldName, xmlName);
    }

    /**
     * Returns the bean class name of a spreadsheet result type and generates that bean class. The class loader of an
     * external module is added to the class loader of this module.
     */
    private String getSpreadsheetResultBeanClassName(
            IOpenClass t,
            IdentityHashMap<ModuleOpenClass, IdentityHashMap<ModuleOpenClass, Boolean>> cache) {
        String fieldClsName;
        XlsModuleOpenClass additionalClassGenerationClassloaderModule = null;
        switch (t) {
            case CustomSpreadsheetResultOpenClass csroc -> {
                var externalCustomSpreadsheetResultOpenClass = isExternalCustomSpreadsheetResultOpenClass(
                        csroc,
                        cache);
                if (externalCustomSpreadsheetResultOpenClass) {
                    additionalClassGenerationClassloaderModule = csroc.getModule();
                }
                fieldClsName = csroc.getBeanClassName();
                csroc.generateBeanClass();
            }
            case SpreadsheetResultOpenClass spreadsheetResultOpenClass -> {
                final var externalSpreadsheetResultOpenClass = isExternalSpreadsheetResultOpenClass(
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
            }
            case null, default -> fieldClsName = Map.class.getCanonicalName();
        }
        if (additionalClassGenerationClassloaderModule != null) {
            getModule().getClassGenerationClassLoader()
                    .addClassLoader(
                            additionalClassGenerationClassloaderModule.getClassGenerationClassLoader());
        }
        return fieldClsName;
    }

    private static String getJavaTypeName(Class<?> instanceClass) {
        if (instanceClass.isPrimitive()) {
            return ClassUtils.primitiveToWrapper(instanceClass).getName();
        } else {
            return instanceClass.getCanonicalName();
        }
    }

    /**
     * Returns the name, or the name followed by the smallest number starting from 1 that makes it unused.
     */
    private static String uniqueName(String name, Predicate<String> isUsed) {
        var newName = name;
        var i = 1;
        while (isUsed.test(newName)) {
            newName = name + i;
            i++;
        }
        return newName;
    }

    private FieldDescription createFieldDescription(String typeName, Point point, IOpenField field) {
        var row = point.getRow();
        var column = point.getColumn();
        return new FieldDescription(typeName,
                simpleRefByRow || !simpleRefByColumn ? rowNames[row] : null,
                !simpleRefByRow ? columnNames[column] : null,
                descriptions[row][column],
                DomainOpenClass.vocabularyValues(field.getType()));
    }

    private static void addUsedField(List<IOpenField> usedFields, IOpenField field) {
        for (IOpenField openField : usedFields) { // Do not add the same twice
            if (openField.getName().equals(field.getName())) {
                return;
            }
        }
        usedFields.add(field);
    }

    private List<IOpenField> fillUsed(List<IOpenField>[][] used, Point point, IOpenField field) {
        var fields = new ArrayList<IOpenField>();
        fields.add(field);
        if (simpleRefByRow) {
            Arrays.fill(used[point.getRow()], fields);
        } else if (simpleRefByColumn) {
            for (var w = 0; w < used.length; w++) {
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
                    var name = spreadsheetResultNameToBeanName(getName());
                    beanClassName = getModule().getGlobalTableProperties().getSpreadsheetResultPackage() + "." + name;
                }
            }
        }
        return beanClassName;
    }

    public SpreadsheetResult createSpreadsheetResult(Object bean,
                                                     Map<Class<?>, CustomSpreadsheetResultOpenClass> mapClassToSpr) {
        var spreadsheetResult = (SpreadsheetResult) newInstance(null);
        for (Map.Entry<String, List<IOpenField>> cell : getBeanFieldsMap().entrySet()) {
            var fieldName = cell.getKey();
            Object v;
            try {
                v = ClassUtils.get(bean, fieldName);
            } catch (Exception e) {
                log.debug("Ignored error: ", e);
                continue;
            }
            Object cv = SpreadsheetResult.convertBeansToSpreadsheetResults(v, mapClassToSpr);
            var openField = cell.getValue().getFirst();
            openField.set(spreadsheetResult, cv, null);
        }
        return spreadsheetResult;
    }

    @Override
    protected Map<MethodKey, IOpenMethod> initConstructorMap() {
        Map<MethodKey, IOpenMethod> constructorMap = super.initConstructorMap();
        var spreadsheetResultConstructorMap = new HashMap<MethodKey, IOpenMethod>();
        for (Map.Entry<MethodKey, IOpenMethod> entry : constructorMap.entrySet()) {
            var constructor = new CustomSpreadsheetResultConstructor(entry.getValue(), this);
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

        var that = (CustomSpreadsheetResultOpenClass) o;

        return Objects.equals(module, that.module) && Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        var result = super.hashCode();
        result = 31 * result + (module != null ? module.hashCode() : 0);
        return result;
    }
}
