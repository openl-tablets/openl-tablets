package org.openl.rules.data;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.domain.EnumDomain;
import org.openl.meta.StringValue;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.dt.element.FunctionalRow;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Handles column descriptors that are represented as foreign keys to data from
 * other tables.
 * 
 * @author DLiauchuk
 * 
 */
public class ForeignKeyColumnDescriptor extends ColumnDescriptor {

    private static final String NOT_INITIALIZED = "<not_initialized>";
    private IdentifierNode foreignKeyTable;
    private IdentifierNode foreignKey;

    public ForeignKeyColumnDescriptor(IOpenField field,
            IdentifierNode foreignKeyTable,
            IdentifierNode foreignKey,
            StringValue displayValue,
            OpenL openl, boolean constructor) {

        super(field, displayValue, openl, constructor);

        this.foreignKeyTable = foreignKeyTable;
        this.foreignKey = foreignKey;
    }

    /**
     * Gets the value as <code>String</code> from the cell. If there is no
     * value, returns <code>NULL</code>.
     * 
     * @param cellTable
     * @return
     */
    private String getCellStringValue(ILogicalTable cellTable) {

        String value = cellTable.getSource().getCell(0, 0).getStringValue();

        if (value != null) {
            value = value.trim();
        }

        return value;
    }

    /**
     * Goes through the values as foreign keys, finds all info about this
     * objects in foreign table and puts it to array. Can process array value
     * presented as {@link FunctionalRow#ARRAY_ELEMENTS_SEPARATOR} array.
     * 
     * @param valuesTable Logical table representing array values for current
     *            table.
     * @param bindingContext
     * @param foreignTable Foreign table with stored info about dependent
     *            values.
     * @param foreignKeyIndex
     * @param domainClass
     * @return
     * @throws BoundError
     */
    private ArrayList<Object> getArrayValuesByForeignKey(ILogicalTable valuesTable,
            IBindingContext bindingContext,
            ITable foreignTable,
            int foreignKeyIndex,
            DomainOpenClass domainClass) throws SyntaxNodeException {

        int valuesHeight = valuesTable.getHeight();

        ArrayList<Object> values = new ArrayList<Object>(valuesHeight);

        boolean multiValue = false;

        if (valuesHeight == 1 && RuleRowHelper.isCommaSeparatedArray(valuesTable)) {

            multiValue = true;
            RuleRowHelper.setCellMetaInfo(valuesTable, getField().getName(), domainClass, multiValue);

            // load array of values as comma separated parameters
            String[] tokens = RuleRowHelper.extractElementsFromCommaSeparatedArray(valuesTable);

            if (tokens != null) {
                for (String token : tokens) {
                    Object res = getValueByForeignKeyIndex(bindingContext,
                        foreignTable,
                        foreignKeyIndex,
                        valuesTable,
                        token);
                    values.add(res);
                }
            }
        } else {

            for (int i = 0; i < valuesHeight; i++) {
                // we take the appropriate cell for the current value.
                ILogicalTable valueTable = valuesTable.getRow(i);
                String value = getCellStringValue(valueTable);

                if (value == null || value.length() == 0) {
                    // set meta info for empty cells.
                    RuleRowHelper.setCellMetaInfo(valueTable, getField().getName(), domainClass, multiValue);
                    values.add(null);
                    continue;
                }

                RuleRowHelper.setCellMetaInfo(valueTable, getField().getName(), domainClass, multiValue);
                Object res = getValueByForeignKeyIndex(bindingContext, foreignTable, foreignKeyIndex, valueTable, value);
                values.add(res);
            }
        }

        return values;
    }

    /**
     * Tries to find value by its key in foreign table. If no, throws an
     * exception.
     * 
     * @param bindingContext
     * @param foreignTable
     * @param foreignKeyIndex
     * @param valueTable
     * @param key
     * @return
     * @throws BoundError
     */
    private Object getValueByForeignKeyIndex(IBindingContext bindingContext,
            ITable foreignTable,
            int foreignKeyIndex,
            ILogicalTable valueTable,
            String key) throws SyntaxNodeException {

        Object result = null;

        try {
            result = foreignTable.findObject(foreignKeyIndex, key, bindingContext);
        } catch (SyntaxNodeException ex) {
            throwIndexNotFound(valueTable, key, ex, bindingContext);
        }

        if (result == null) {
            throwIndexNotFound(valueTable, key, null, bindingContext);
        }

        return result;
    }

    private void throwIndexNotFound(ILogicalTable valuesTable, String src, Exception ex, IBindingContext bindingContext) throws SyntaxNodeException {

        String message = String.format("Index Key %s not found", src);

        throw SyntaxNodeExceptionUtils.createError(message,
            ex,
            null,
            new GridCellSourceCodeModule(valuesTable.getSource(), bindingContext));
    }

    /**
     * Method is using to load data from foreign table, using foreign key (see
     * {@link DataNodeBinder#getForeignKeyTokens()}). Is used when data table is
     * represents <b>AS</b> a constructor (see {@link #isConstructor()}).
     */
    public Object getLiteralByForeignKey(IOpenClass fieldType,
            ILogicalTable valuesTable,
            IDataBase db,
            IBindingContext bindingContext) throws Exception {

        String foreignKeyTableName = foreignKeyTable.getIdentifier();
        ITable foreignTable = db.getTable(foreignKeyTableName);
        Object result = null;

        if (foreignTable == null) {
            String message = String.format("Table '%s' not found", foreignKeyTableName);
            throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
        } else if (foreignTable.getTableSyntaxNode().hasErrors()) {
            String message = String.format("Foreign table '%s' has errors", foreignKeyTableName);
            throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
        }

        int foreignKeyIndex = 0;
        String columnName = NOT_INITIALIZED;

        if (foreignKey != null) {
            columnName = foreignKey.getIdentifier();
            foreignKeyIndex = foreignTable.getColumnIndex(columnName);
        }

        if (foreignKeyIndex == -1) {
            String message = "Column " + columnName + " not found";
            throw SyntaxNodeExceptionUtils.createError(message, null, foreignKey);
        }

        boolean valuesAnArray = isValuesAnArray(fieldType);

        if (!valuesAnArray) {

            String value = getCellStringValue(valuesTable);

            if (value != null && value.length() > 0) {
                result = getValueByForeignKeyIndex(bindingContext, foreignTable, foreignKeyIndex, valuesTable, value);
            }

        } else {

            List<Object> values = new ArrayList<Object>();
            int valuesHeight = valuesTable.getHeight();

            for (int i = 0; i < valuesHeight; i++) {

                ILogicalTable valueTable = valuesTable.getRow(i);
                String value = getCellStringValue(valueTable);

                if (value == null || value.length() == 0) {
                    break;
                }

                Object res = getValueByForeignKeyIndex(bindingContext, foreignTable, foreignKeyIndex, valueTable, value);
                values.add(res);
            }

            IOpenClass componentType = fieldType.getAggregateInfo().getComponentType(fieldType);
            Object ary = fieldType.getAggregateInfo().makeIndexedAggregate(componentType, new int[] { values.size() });

            for (int i = 0; i < values.size(); i++) {
                Array.set(ary, i, values.get(i));
            }

            result = ary;
        }

        return result;
    }

    /**
     * Returns <code>TRUE</code> if instance has foreign key table.
     */
    public boolean isReference() {
        return foreignKeyTable != null;
    }

    /**
     * Method is using to load data from foreign table, using foreign key (see
     * {@link DataNodeBinder#getForeignKeyTokens()}). Is used when data table is
     * represents as <b>NOT</b> a constructor (see {@link #isConstructor()}).
     */
    public void populateLiteralByForeignKey(Object target, ILogicalTable valuesTable, IDataBase db, IBindingContext cxt) throws Exception {

        if (foreignKeyTable != null) {

            String foreignKeyTableName = foreignKeyTable.getIdentifier();
            ITable foreignTable = db.getTable(foreignKeyTableName);

            if (foreignTable == null) {
                String message = String.format("Table '%s' not found", foreignKeyTableName);
                throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);

            } else if (foreignTable.getTableSyntaxNode().hasErrors()) {
                String message = String.format("Foreign table '%s' has errors", foreignKeyTableName);
                throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
            }

            int foreignKeyIndex = 0;
            String columnName = NOT_INITIALIZED;

            if (foreignKey != null) {
                columnName = foreignKey.getIdentifier();
                foreignKeyIndex = foreignTable.getColumnIndex(columnName);
            }

            if (foreignKeyIndex == -1) {
                String message = "Column " + columnName + " not found";
                throw SyntaxNodeExceptionUtils.createError(message, null, foreignKey);
            }

            Map<String, Integer> index = foreignTable.getUniqueIndex(foreignKeyIndex);
            String[] domainStrings = index.keySet().toArray(new String[0]);

            EnumDomain<String> domain = new EnumDomain<String>(domainStrings);
            DomainOpenClass domainClass = new DomainOpenClass(getField().getName(), JavaOpenClass.STRING, domain, null);

            // table will have 1xN size
            //
            valuesTable = LogicalTableHelper.make1ColumnTable(valuesTable);

            IOpenClass fieldType = getField().getType();

            boolean valueAnArray = isValuesAnArray(fieldType);
            boolean isList = List.class.isAssignableFrom(fieldType.getInstanceClass());

            if (!valueAnArray && !isList) {

                String s = getCellStringValue(valuesTable);

                if (s == null || s.length() == 0) {
                    // Set meta info for empty cells
                    RuleRowHelper.setCellMetaInfo(valuesTable, getField().getName(), domainClass, false);
                } else {
                    if (s.length() > 0) {
                        RuleRowHelper.setCellMetaInfo(valuesTable, getField().getName(), domainClass, false);
                        Object res = getValueByForeignKeyIndex(cxt, foreignTable, foreignKeyIndex, valuesTable, s);
                        getField().set(target, res, getRuntimeEnv());
                    }
                }
            } else {
                // processing array or list values.
                List<Object> cellValues = getArrayValuesByForeignKey(valuesTable, cxt, foreignTable, foreignKeyIndex, domainClass);
                List<Object> values = new ArrayList<Object>();

                // Cell can contain empty reference value. As a result we will
                // receive collection with one null value element. The following code snippet 
                // searches null value elements and removes them.
                //
                Predicate predicate = new Predicate() {
                    public boolean evaluate(Object arg) {
                        return arg != null;
                    }
                };
                
                CollectionUtils.select(cellValues, predicate, values);
                
                int size = values.size();
                IOpenClass componentType = null;

                if (valueAnArray) {
                    componentType = fieldType.getAggregateInfo().getComponentType(fieldType);
                } else {
                    componentType = JavaOpenClass.OBJECT;
                }

                Object array = fieldType.getAggregateInfo().makeIndexedAggregate(componentType, new int[] { size });

                // Populate result array with values.
                //
                for (int i = 0; i < size; i++) {
                    Array.set(array, i, values.get(i));
                }

                if (isList) {
                    int len = Array.getLength(array);

                    List<Object> list = new ArrayList<Object>(len);

                    for (int i = 0; i < len; i++) {
                        list.add(Array.get(array, i));
                    }

                    getField().set(target, list, getRuntimeEnv());
                } else {
                    getField().set(target, array, getRuntimeEnv());
                }
            }
        }
    }
}
