package org.openl.rules.calc;

import java.util.*;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.binding.CustomDynamicOpenClass;
import org.openl.rules.table.Point;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class CustomSpreadsheetResultOpenClass extends ADynamicClass implements CustomDynamicOpenClass {

    private String[] rowNames;
    private String[] columnNames;
    private String[] rowTitles;
    private String[] columnTitles;
    private Map<String, Point> fieldsCoordinates;

    public CustomSpreadsheetResultOpenClass(String name,
            String[] rowNames,
            String[] columnNames,
            String[] rowTitles,
            String[] columnTitles) {
        super(name, SpreadsheetResult.class);
        if (rowNames == null) {
            throw new IllegalArgumentException();
        }
        if (columnNames == null) {
            throw new IllegalArgumentException();
        }
        if (rowTitles == null) {
            throw new IllegalArgumentException();
        }
        if (columnTitles == null) {
            throw new IllegalArgumentException();
        }
        this.rowNames = rowNames.clone();
        this.columnNames = columnNames.clone();
        this.columnTitles = columnTitles.clone();
        this.rowTitles = rowTitles.clone();
        this.fieldsCoordinates = SpreadsheetResult.buildFieldsCoordinates(this.columnNames, this.rowNames);
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
    
    public void extendSpreadsheetResult(String[] rowNames,
            String[] columnNames,
            String[] rowTitles,
            String[] columnTitles,
            Collection<IOpenField> fields) {
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

        // Add simplified fields if they are not existed
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

    @Override
    public IOpenClass copy() {
        return copyCustomSpreadsheetResult();
    }

    @Override
    public void updateOpenClass(IOpenClass openClass) {
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

    private CustomSpreadsheetResultOpenClass copyCustomSpreadsheetResult() {
        CustomSpreadsheetResultOpenClass type = new CustomSpreadsheetResultOpenClass(getName(),
            getRowNames(),
            getColumnNames(),
            getRowTitles(),
            getColumnTitles());
        for (IOpenField field : getFields().values()) {
            type.addField(field);
        }
        type.setMetaInfo(getMetaInfo());
        return type;
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        Object[][] result = new Object[rowNames.length][columnNames.length];
        return new SpreadsheetResult(result, rowNames.clone(), columnNames.clone(), fieldsCoordinates);
    }

}
