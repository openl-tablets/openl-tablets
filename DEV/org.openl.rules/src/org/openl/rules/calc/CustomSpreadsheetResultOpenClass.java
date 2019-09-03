package org.openl.rules.calc;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
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
    private String[] rowNamesMarkedWithAsterisk;
    private String[] columnNamesMarkedWithAsterisk;
    private List<Pair<String[], String[]>> rowAndColumnNamesMarkedWithAsteriskHistory;
    private String[] rowTitles;
    private String[] columnTitles;
    private Map<String, Point> fieldsCoordinates;
    private XlsModuleOpenClass module;
    private volatile Class<?> beanClass;
    private volatile SpreadsheetResultValueSetter[] spreadsheetResultValueSetters;
    private boolean simpleRefBeanByRow;
    private boolean simpleRefBeanByColumn;
    private long columnsWithAsteriskCount;
    private long rowsWithAsteriskCount;
    private byte[] beanClassByteCode;

    public CustomSpreadsheetResultOpenClass(String name,
            String[] rowNames,
            String[] columnNames,
            String[] rowNamesMarkedWithAsterisk,
            String[] columnNamesMarkedWithAsterisk,
            String[] rowTitles,
            String[] columnTitles,
            XlsModuleOpenClass module) {
        super(name, SpreadsheetResult.class);
        Objects.requireNonNull(rowNames);
        Objects.requireNonNull(columnNames);
        Objects.requireNonNull(rowNamesMarkedWithAsterisk);
        Objects.requireNonNull(columnNamesMarkedWithAsterisk);
        Objects.requireNonNull(rowTitles);
        Objects.requireNonNull(columnTitles);
        Objects.requireNonNull(module);
        this.rowNames = rowNames;
        this.columnNames = columnNames;
        this.rowNamesMarkedWithAsterisk = rowNamesMarkedWithAsterisk;
        this.columnNamesMarkedWithAsterisk = columnNamesMarkedWithAsterisk;

        this.columnsWithAsteriskCount = Arrays.stream(columnNamesMarkedWithAsterisk).filter(Objects::nonNull).count();
        this.rowsWithAsteriskCount = Arrays.stream(rowNamesMarkedWithAsterisk).filter(Objects::nonNull).count();

        this.simpleRefBeanByRow = columnsWithAsteriskCount == 1;
        this.simpleRefBeanByColumn = rowsWithAsteriskCount == 1;

        this.rowAndColumnNamesMarkedWithAsteriskHistory = new ArrayList<>();
        this.rowAndColumnNamesMarkedWithAsteriskHistory
            .add(Pair.of(this.columnNamesMarkedWithAsterisk, this.rowNamesMarkedWithAsterisk));

        this.rowTitles = rowTitles;
        this.columnTitles = columnTitles;
        this.fieldsCoordinates = SpreadsheetResult.buildFieldsCoordinates(this.columnNames, this.rowNames);
        this.module = module;
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
            String[] rowNamesMarkedWithAsterisk,
            String[] columnNamesMarkedWithAsterisk,
            String[] rowTitles,
            String[] columnTitles,
            Collection<IOpenField> fields,
            boolean simpleRefBeanByRow,
            boolean simpleRefBeanByColumn) {
        if (beanClass != null) {
            throw new IllegalStateException(
                "Bean class for custom spreadsheet result has already been generated. Spreasheet result can't be extended.");
        }

        List<String> nRowNames = new ArrayList<>(Arrays.asList(this.rowNames));
        List<String> nRowNamesMarkedWithAsterisk = new ArrayList<>(Arrays.asList(this.rowNamesMarkedWithAsterisk));
        Set<String> existedRowNamesSet = new HashSet<>(Arrays.asList(this.rowNames));
        List<String> nColumnNames = new ArrayList<>(Arrays.asList(this.columnNames));
        List<String> nColumnNamesMarkedWithAsterisk = new ArrayList<>(
            Arrays.asList(this.columnNamesMarkedWithAsterisk));
        Set<String> existedColumnNamesSet = new HashSet<>(Arrays.asList(this.columnNames));

        List<String> nRowTitles = new ArrayList<>(Arrays.asList(this.rowTitles));
        List<String> nColumnTitles = new ArrayList<>(Arrays.asList(this.columnTitles));

        boolean fieldCoordinatesRequresUpdate = false;
        boolean rowColumnsWithAsterisktRequiresUpdate = false;

        for (int i = 0; i < rowNames.length; i++) {
            if (!existedRowNamesSet.contains(rowNames[i])) {
                nRowNames.add(rowNames[i]);
                nRowNamesMarkedWithAsterisk.add(rowNamesMarkedWithAsterisk[i]);
                nRowTitles.add(rowTitles[i]);
                fieldCoordinatesRequresUpdate = true;
                rowColumnsWithAsterisktRequiresUpdate = true;
            } else if (rowNamesMarkedWithAsterisk[i] != null) {
                int k = nRowNames.indexOf(rowNames[i]);
                nRowNamesMarkedWithAsterisk.set(k, rowNamesMarkedWithAsterisk[i]);
                rowColumnsWithAsterisktRequiresUpdate = true;
            }
        }

        for (int i = 0; i < columnNames.length; i++) {
            if (!existedColumnNamesSet.contains(columnNames[i])) {
                nColumnNames.add(columnNames[i]);
                nColumnNamesMarkedWithAsterisk.add(columnNamesMarkedWithAsterisk[i]);
                nColumnTitles.add(columnTitles[i]);
                fieldCoordinatesRequresUpdate = true;
                rowColumnsWithAsterisktRequiresUpdate = true;
            } else if (columnNamesMarkedWithAsterisk[i] != null) {
                int k = nColumnNames.indexOf(columnNames[i]);
                nColumnNamesMarkedWithAsterisk.set(k, columnNamesMarkedWithAsterisk[i]);
                rowColumnsWithAsterisktRequiresUpdate = true;
            }
        }

        if (fieldCoordinatesRequresUpdate) {
            this.rowNames = nRowNames.toArray(new String[] {});
            this.rowTitles = nRowTitles.toArray(new String[] {});

            this.columnNames = nColumnNames.toArray(new String[] {});
            this.columnTitles = nColumnTitles.toArray(new String[] {});

            this.fieldsCoordinates = Collections
                .unmodifiableMap(SpreadsheetResult.buildFieldsCoordinates(this.columnNames, this.rowNames));
        }

        if (rowColumnsWithAsterisktRequiresUpdate) {
            this.simpleRefBeanByRow = simpleRefBeanByRow && this.simpleRefBeanByRow;
            this.simpleRefBeanByColumn = simpleRefBeanByColumn && this.simpleRefBeanByColumn;

            this.rowAndColumnNamesMarkedWithAsteriskHistory
                .add(Pair.of(columnNamesMarkedWithAsterisk, rowNamesMarkedWithAsterisk));

            this.rowNamesMarkedWithAsterisk = nRowNamesMarkedWithAsterisk.toArray(new String[] {});
            this.columnNamesMarkedWithAsterisk = nColumnNamesMarkedWithAsterisk.toArray(new String[] {});
            this.columnsWithAsteriskCount = Arrays.stream(columnNamesMarkedWithAsterisk)
                .filter(Objects::nonNull)
                .count();
            this.rowsWithAsteriskCount = Arrays.stream(rowNamesMarkedWithAsterisk).filter(Objects::nonNull).count();
        }

        for (IOpenField field : fields) {
            if (getField(field.getName()) == null) {
                super.addField(field);
            }
        }
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
            throw new IllegalStateException("Bean class is loaded. Custom spreadsheet result can't be extended.");
        }
        this.extendSpreadsheetResult(customSpreadsheetResultOpenClass.rowNames,
            customSpreadsheetResultOpenClass.columnNames,
            customSpreadsheetResultOpenClass.rowNamesMarkedWithAsterisk,
            customSpreadsheetResultOpenClass.columnNamesMarkedWithAsterisk,
            customSpreadsheetResultOpenClass.rowTitles,
            customSpreadsheetResultOpenClass.columnTitles,
            customSpreadsheetResultOpenClass.getFields().values(),
            customSpreadsheetResultOpenClass.simpleRefBeanByRow,
            customSpreadsheetResultOpenClass.simpleRefBeanByColumn);
        validate(this, customSpreadsheetResultOpenClass.getFields().values());
    }

    private void validate(CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass,
            Collection<IOpenField> fields) {
        List<String> errorMessages = new ArrayList<>();
        for (IOpenField field : fields) {
            IOpenField existedField = customSpreadsheetResultOpenClass.getField(field.getName());
            if (!existedField.getType().equals(field.getType())) {
                errorMessages.add(getName() + "." + field.getName() + "(expected: " + existedField.getType()
                    .getName() + ", found: " + field.getType().getName() + ")");
            }
        }
        if (!errorMessages.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String errorMessage : errorMessages) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(errorMessage);
            }
            throw new OpenlNotCheckedException("Incompatible type usage in spreadsheet fields: " + sb.toString());
        }
    }

    public String[] getRowNamesMarkedWithAsterisk() {
        return rowNamesMarkedWithAsterisk.clone();
    }

    public String[] getColumnNamesMarkedWithAsterisk() {
        return columnNamesMarkedWithAsterisk.clone();
    }

    public CustomSpreadsheetResultOpenClass makeCopyForModule(XlsModuleOpenClass module) {
        CustomSpreadsheetResultOpenClass type = new CustomSpreadsheetResultOpenClass(getName(),
            rowNames,
            columnNames,
            rowNamesMarkedWithAsterisk,
            columnNamesMarkedWithAsterisk,
            rowTitles,
            columnTitles,
            module);
        for (IOpenField field : getFields().values()) {
            type.addField(field);
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
            rowNamesMarkedWithAsterisk,
            columnNamesMarkedWithAsterisk,
            fieldsCoordinates);
    }

    public Object createBean(SpreadsheetResult spreadsheetResult) throws IllegalAccessException,
                                                                  InstantiationException {
        if (!this.getName().equals(spreadsheetResult.getCustomSpreadsheetResultOpenClass().getName())) {
            throw new IllegalArgumentException("Invalid spreadsheet result structure.");
        }
        Class<?> clazz = getBeanClass();
        Object target = clazz.newInstance();
        for (SpreadsheetResultValueSetter spreadsheetResultValueSetter : spreadsheetResultValueSetters) {
            spreadsheetResultValueSetter.set(spreadsheetResult, target);
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
                    boolean[][] used = new boolean[rowNames.length][columnNames.length];
                    for (int i = 0; i < used.length; i++) {
                        Arrays.fill(used[i], false);
                    }
                    Map<String, IOpenField> beanFieldsMap = new HashMap<>();
                    addFieldsToJavaClassBuilder(beanClassBuilder, used, usedFields, true, beanFieldsMap);
                    addFieldsToJavaClassBuilder(beanClassBuilder, used, usedFields, false, beanFieldsMap);
                    byte[] byteCode = beanClassBuilder.byteCode();
                    try {
                        this.beanClass = loadBeanClass(beanClassName, byteCode);
                        this.beanClassByteCode = byteCode;
                        List<SpreadsheetResultValueSetter> srValueSetters = new ArrayList<>();
                        for (Field field : beanClass.getDeclaredFields()) {
                            if (!field.isSynthetic()) {// SONAR adds synthetic fields
                                IOpenField openField = beanFieldsMap.get(field.getName());
                                SpreadsheetResultValueSetter spreadsheetResultValueSetter = new SpreadsheetResultValueSetter(
                                    module,
                                    field,
                                    openField);
                                srValueSetters.add(spreadsheetResultValueSetter);
                            }
                        }
                        this.spreadsheetResultValueSetters = srValueSetters
                            .toArray(new SpreadsheetResultValueSetter[] {});
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        return beanClass;
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
            boolean[][] used,
            Set<String> usedGettersAndSetters,
            boolean addFieldNameWithCollisions,
            Map<String, IOpenField> beanFieldsMap) {
        List<Triple<String, Point, IOpenField>> fields = new ArrayList<>();
        for (Entry<String, IOpenField> entry : getFields().entrySet()) {
            fields.add(Triple.of(entry.getKey(), fieldsCoordinates.get(entry.getKey()), entry.getValue()));
        }

        Collections.sort(fields, COMP);

        for (Triple<String, Point, IOpenField> w : fields) {
            Point point = w.getMiddle();
            if (point != null && rowNamesMarkedWithAsterisk[point
                .getRow()] != null && columnNamesMarkedWithAsterisk[point
                    .getColumn()] != null && !used[point.getRow()][point.getColumn()]) {

                String fieldName;
                if (simpleRefBeanByRow) {
                    fieldName = rowNamesMarkedWithAsterisk[point.getRow()];
                } else if (simpleRefBeanByColumn) {
                    fieldName = columnNamesMarkedWithAsterisk[point.getColumn()];
                } else {
                    boolean found = false;
                    for (Pair<String[], String[]> p : rowAndColumnNamesMarkedWithAsteriskHistory) {
                        for (String col : p.getLeft()) {
                            for (String row : p.getRight()) {
                                if (!found && Objects.equals(columnNamesMarkedWithAsterisk[point.getColumn()],
                                    col) && Objects.equals(rowNamesMarkedWithAsterisk[point.getRow()], row)) {
                                    found = true;
                                }
                            }
                        }
                    }
                    if (!found) {
                        continue;
                    }
                    fieldName = columnNamesMarkedWithAsterisk[point
                        .getColumn()] + "_" + rowNamesMarkedWithAsterisk[point.getRow()];
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
                    fillUsed(used, point);
                    beanClassBuilder.addField(fieldName, type.getName());
                    beanFieldsMap.put(fieldName, w.getRight());
                } else if (addFieldNameWithCollisions) {
                    String newFieldName = fieldName;
                    if (!fieldName.startsWith("_")) {
                        newFieldName = "_" + fieldName;
                    }
                    int i = 1;
                    while (isFieldConflictsWithOtherGetterSetters(usedGettersAndSetters, fieldName)) {
                        newFieldName = fieldName + "_" + i;
                        i++;
                    }
                    usedGettersAndSetters.add(ClassUtils.getter(newFieldName));
                    usedGettersAndSetters.add(ClassUtils.setter(newFieldName));

                    fillUsed(used, point);
                    beanClassBuilder.addField(newFieldName, type.getName());
                    beanFieldsMap.put(newFieldName, w.getRight());
                }
            }
        }
    }

    private boolean isFieldConflictsWithOtherGetterSetters(Set<String> usedGettersAndSetters, String fieldName) {
        return usedGettersAndSetters.contains(ClassUtils.getter(fieldName)) || usedGettersAndSetters
            .contains(ClassUtils.setter(fieldName));
    }

    public void fillUsed(boolean[][] used, Point point) {
        if (simpleRefBeanByRow) {
            for (int w = 0; w < used[point.getRow()].length; w++) {
                used[point.getRow()][w] = true;
            }
        } else if (simpleRefBeanByColumn) {
            for (int w = 0; w < used.length; w++) {
                used[w][point.getColumn()] = true;
            }
        } else {
            used[point.getRow()][point.getColumn()] = true;
        }
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

    private static class SpreadsheetResultValueSetter {
        private Field field;
        private IOpenField openField;
        private XlsModuleOpenClass module;

        private SpreadsheetResultValueSetter(XlsModuleOpenClass module, Field field, IOpenField openField) {
            Objects.requireNonNull(module);
            Objects.requireNonNull(field);
            Objects.requireNonNull(openField);
            this.field = field;
            this.openField = openField;
            this.module = module;
            this.field.setAccessible(true);
        }

        public void set(SpreadsheetResult spreadsheetResult, Object target) throws IllegalAccessException,
                                                                            InstantiationException {
            if (!spreadsheetResult.isMarkedWithAsteriskField(openField.getName())) {
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
}
