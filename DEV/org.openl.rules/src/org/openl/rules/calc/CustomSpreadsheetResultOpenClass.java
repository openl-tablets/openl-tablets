package org.openl.rules.calc;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.table.Point;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.vm.IRuntimeEnv;

public class CustomSpreadsheetResultOpenClass extends ADynamicClass {

    private String[] rowNames;
    private String[] columnNames;
    private String[] rowNamesMarkedWithStar;
    private String[] columnNamesMarkedWithStar;
    private String[] rowTitles;
    private String[] columnTitles;
    private Map<String, Point> fieldsCoordinates;
    private XlsModuleOpenClass module;
    private volatile Class<?> beanClass;
    private volatile SpreadsheetResultValueSetter[] spreadsheetResultValueSetters;

    public CustomSpreadsheetResultOpenClass(String name,
            String[] rowNames,
            String[] columnNames,
            String[] rowNamesMarkedWithStar,
            String[] columnNamesMarkedWithStar,
            String[] rowTitles,
            String[] columnTitles,
            XlsModuleOpenClass module) {
        super(name, SpreadsheetResult.class);
        Objects.requireNonNull(rowNames);
        Objects.requireNonNull(columnNames);
        Objects.requireNonNull(rowNamesMarkedWithStar);
        Objects.requireNonNull(columnNamesMarkedWithStar);
        Objects.requireNonNull(rowTitles);
        Objects.requireNonNull(columnTitles);
        Objects.requireNonNull(module);
        this.rowNames = rowNames.clone();
        this.columnNames = columnNames.clone();
        this.rowNamesMarkedWithStar = rowNamesMarkedWithStar.clone();
        this.columnNamesMarkedWithStar = columnNamesMarkedWithStar.clone();
        this.columnTitles = columnTitles.clone();
        this.rowTitles = rowTitles.clone();
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

    public void extendSpreadsheetResult(String[] rowNames,
            String[] columnNames,
            String[] rowTitles,
            String[] columnTitles,
            Collection<IOpenField> fields) {
        if (beanClass != null) {
            throw new IllegalStateException(
                "Bean class for custom spreadsheet result has already been generated. Spreasheet result can't be extended.");
        }

        List<String> nRowNames = new ArrayList<>(Arrays.asList(this.rowNames));
        Set<String> existedRowNamesSet = new HashSet<>(Arrays.asList(this.rowNames));
        List<String> nColumnNames = new ArrayList<>(Arrays.asList(this.columnNames));
        Set<String> existedColumnNamesSet = new HashSet<>(Arrays.asList(this.columnNames));

        List<String> nRowTitles = new ArrayList<>(Arrays.asList(this.rowTitles));
        List<String> nColumnTitles = new ArrayList<>(Arrays.asList(this.columnTitles));

        boolean fieldCoordinatesRequresUpdate = false;

        for (int i = 0; i < rowNames.length; i++) {
            if (!existedRowNamesSet.contains(rowNames[i])) {
                nRowNames.add(rowNames[i]);
                nRowTitles.add(rowTitles[i]);
                fieldCoordinatesRequresUpdate = true;
            }
        }

        for (int i = 0; i < columnNames.length; i++) {
            if (!existedColumnNamesSet.contains(columnNames[i])) {
                nColumnNames.add(columnNames[i]);
                nColumnTitles.add(columnTitles[i]);
                fieldCoordinatesRequresUpdate = true;
            }
        }

        if (fieldCoordinatesRequresUpdate) {
            Set<String> newFieldNames = new HashSet<>();
            for (int i = 0; i < nRowNames.size(); i++) {
                for (int j = this.columnNames.length; j < nColumnNames.size(); j++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                        .append(nColumnNames.get(j))
                        .append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                        .append(nRowNames.get(i));
                    newFieldNames.add(sb.toString());
                }
            }

            for (int i = this.rowNames.length; i < nRowNames.size(); i++) {
                for (int j = 0; j < nColumnNames.size(); j++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                        .append(nColumnNames.get(j))
                        .append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                        .append(nRowNames.get(i));
                    newFieldNames.add(sb.toString());
                }
            }

            for (int i = this.rowNames.length; i < nRowNames.size(); i++) {
                for (int j = this.columnNames.length; j < nColumnNames.size(); j++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                        .append(nColumnNames.get(j))
                        .append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                        .append(nRowNames.get(i));
                    newFieldNames.add(sb.toString());
                }
            }
            this.rowNames = nRowNames.toArray(new String[] {});
            this.columnNames = nColumnNames.toArray(new String[] {});
            this.rowTitles = nRowTitles.toArray(new String[] {});
            this.columnTitles = nColumnTitles.toArray(new String[] {});
            for (IOpenField field : fields) {
                if (newFieldNames.contains(field.getName())) {
                    addField(field);
                }
            }

            this.fieldsCoordinates = Collections
                .unmodifiableMap(SpreadsheetResult.buildFieldsCoordinates(this.columnNames, this.rowNames));
        }

        for (IOpenField field : fields) {
            if (getField(field.getName()) == null) {
                addField(field);
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
        return rowTitles;
    }

    public String[] getColumnTitles() {
        return columnTitles;
    }

    public void extendWith(IOpenClass openClass) {
        CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) openClass;
        customSpreadsheetResultOpenClass.extendSpreadsheetResult(getRowNames(),
            getColumnNames(),
            getRowTitles(),
            getColumnTitles(),
            getFields().values());
        validate(customSpreadsheetResultOpenClass, getFields().values());
    }

    private void validate(CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass,
            Collection<IOpenField> fields) {
        List<String> errorMessages = new ArrayList<>();
        for (IOpenField field : fields) {
            IOpenField existedField = customSpreadsheetResultOpenClass.getField(field.getName());
            if (!existedField.getType().isAssignableFrom(field.getType())) {
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

    public Map<String, Point> getFieldsCoordinates() {
        return fieldsCoordinates;
    }

    public String[] getRowNamesMarkedWithStar() {
        return rowNamesMarkedWithStar;
    }

    public String[] getColumnNamesMarkedWithStar() {
        return columnNamesMarkedWithStar;
    }

    public CustomSpreadsheetResultOpenClass makeCopyForModule(XlsModuleOpenClass xlsModuleOpenClass) {
        CustomSpreadsheetResultOpenClass type = new CustomSpreadsheetResultOpenClass(getName(),
            getRowNames(),
            getColumnNames(),
            getRowNamesMarkedWithStar(),
            getColumnNamesMarkedWithStar(),
            getRowTitles(),
            getColumnTitles(),
            xlsModuleOpenClass);
        for (IOpenField field : getFields().values()) {
            type.addField(field);
        }
        type.setMetaInfo(getMetaInfo());
        return type;
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        // Only used for tests
        return new StubSpreadSheetResult();
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

    private Class<?> getBeanClass() {
        if (beanClass == null) {
            synchronized (this) {
                if (beanClass == null) {
                    final String beanClassName = getBeanClassName(this);
                    JavaBeanClassBuilder beanClassBuilder = new JavaBeanClassBuilder(beanClassName);
                    long columnsWithStarCount = Arrays.stream(columnNamesMarkedWithStar)
                        .filter(Objects::nonNull)
                        .count();
                    long rowsWithStarCount = Arrays.stream(rowNamesMarkedWithStar).filter(Objects::nonNull).count();
                    Set<String> usedFields = new HashSet<>();
                    boolean[][] used = new boolean[rowNames.length][columnNames.length];
                    for (int i = 0; i < used.length; i++) {
                        Arrays.fill(used[i], false);
                    }
                    Map<String, Point> beanFieldsCoords = new HashMap<>();
                    Map<String, IOpenClass> types = new HashMap<>();
                    addFieldsToJavaClassBuilder(beanClassBuilder,
                        columnsWithStarCount,
                        rowsWithStarCount,
                        used,
                        usedFields,
                        true,
                        beanFieldsCoords,
                        types);
                    addFieldsToJavaClassBuilder(beanClassBuilder,
                        columnsWithStarCount,
                        rowsWithStarCount,
                        used,
                        usedFields,
                        false,
                        beanFieldsCoords,
                        types);
                    byte[] byteCode = beanClassBuilder.byteCode();
                    try {
                        beanClass = ClassUtils
                            .defineClass(beanClassName, byteCode, module.getCustomSpreadsheetResultsClassLoader());
                        List<SpreadsheetResultValueSetter> srValueSetters = new ArrayList<>();
                        for (Field field : beanClass.getDeclaredFields()) {
                            Point point = beanFieldsCoords.get(field.getName());
                            if (point != null) {
                                SpreadsheetResultValueSetter spreadsheetResultValueSetter = new SpreadsheetResultValueSetter(
                                    module,
                                    field,
                                    point.getRow(),
                                    point.getColumn(),
                                    types.get(field.getName()));
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

    private void addFieldsToJavaClassBuilder(JavaBeanClassBuilder beanClassBuilder,
            long columnsWithStarCount,
            long rowsWithStarCount,
            boolean[][] used,
            Set<String> usedFields,
            boolean addFieldNameWithCollisions,
            Map<String, Point> beanFieldsCoords,
            Map<String, IOpenClass> types) {
        for (Entry<String, IOpenField> entry : getFields().entrySet()) {
            Point point = getFieldsCoordinates().get(entry.getKey());
            if (point != null && rowNamesMarkedWithStar[point.getRow()] != null && columnNamesMarkedWithStar[point
                .getColumn()] != null && !used[point.getRow()][point.getColumn()]) {
                String fieldName;
                if (columnsWithStarCount == 1) {
                    fieldName = rowNamesMarkedWithStar[point.getRow()];
                } else if (rowsWithStarCount == 1) {
                    fieldName = columnNamesMarkedWithStar[point.getColumn()];
                } else {
                    fieldName = columnNamesMarkedWithStar[point.getColumn()] + "_" + rowNamesMarkedWithStar[point
                        .getRow()];
                }
                Class<?> type;
                IOpenClass t = entry.getValue().getType();
                int dim = 0;
                while (t.isArray()) {
                    dim++;
                    t = t.getComponentClass();
                }
                if (t instanceof CustomSpreadsheetResultOpenClass) {
                    CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) t;
                    Class<?> beanType = customSpreadsheetResultOpenClass.getBeanClass();
                    if (dim > 0) {
                        type = Array.newInstance(beanType, new int[dim]).getClass();
                    } else {
                        type = beanType;
                    }
                } else if (t instanceof SpreadsheetResult) {
                    if (dim > 0) {
                        type = Array.newInstance(Map.class, new int[dim]).getClass();
                    } else {
                        type = Map.class;
                    }
                } else {
                    type = entry.getValue().getType().getInstanceClass();
                }
                if (!usedFields.contains(fieldName)) {
                    usedFields.add(fieldName);
                    used[point.getRow()][point.getColumn()] = true;
                    beanClassBuilder.addField(fieldName, type.getName());
                    beanFieldsCoords.put(fieldName, point);
                    types.put(fieldName, t);
                } else {
                    if (addFieldNameWithCollisions) {
                        String newFieldName = fieldName;
                        int i = 1;
                        while (usedFields.contains(newFieldName)) {
                            newFieldName = fieldName + i;
                            i++;
                        }
                        usedFields.add(newFieldName);
                        used[point.getRow()][point.getColumn()] = true;
                        beanClassBuilder.addField(newFieldName, type.getName());
                        beanFieldsCoords.put(newFieldName, point);
                        types.put(fieldName, t);
                    }
                }
            }
        }
    }

    private static synchronized String getBeanClassName(
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
        final String beanName = CustomSpreadsheetResultOpenClass.class.getPackage()
            .getName() + ".customspreasheets." + customSpreadsheetResultOpenClass.getName()
                .substring("SpreadsheetResult".length());
        try {
            customSpreadsheetResultOpenClass.getModule().getCustomSpreadsheetResultsClassLoader().loadClass(beanName);
            throw new IllegalStateException("This shouldn't happen.");
        } catch (ClassNotFoundException e) {
            return beanName;
        }
    }

    private static class SpreadsheetResultValueSetter {
        private Field field;
        private int row;
        private int column;
        private IOpenClass type;
        private XlsModuleOpenClass module;

        private SpreadsheetResultValueSetter(XlsModuleOpenClass module,
                Field field,
                int row,
                int column,
                IOpenClass type) {
            this.row = row;
            this.column = column;
            this.field = field;
            this.type = type;
            this.module = module;
            this.field.setAccessible(true);
        }

        private Object convert(Object value, Class<?> t) throws IllegalAccessException, InstantiationException {
            if (value == null) {
                return null;
            }
            if (value.getClass().isArray()) {
                int len = Array.getLength(value);
                Object target = Array.newInstance(t.getComponentType(), len);
                for (int i = 0; i < len; i++) {
                    Object v = convert(Array.get(value, i), t.getComponentType());
                    Array.set(target, i, v);
                }
                return target;
            } else {
                SpreadsheetResult spr = ((SpreadsheetResult) value);
                if (spr.getCustomSpreadsheetResultOpenClass() == null) {
                    return spr.toPlain(module);
                } else {
                    return ((CustomSpreadsheetResultOpenClass) type).createBean(spr);
                }
            }
        }

        public void set(SpreadsheetResult spreadsheetResult, Object target) throws IllegalAccessException,
                                                                            InstantiationException {
            Object v = spreadsheetResult.getValue(row, column);
            if (v == null) {
                field.set(target, null);
                return;
            }
            Class<?> t = v.getClass();
            while (t.isArray()) {
                t = t.getComponentType();
            }
            if (SpreadsheetResult.class.isAssignableFrom(t)) {
                field.set(target, convert(v, field.getType()));
            } else {
                field.set(target, v);
            }
        }
    }
}
