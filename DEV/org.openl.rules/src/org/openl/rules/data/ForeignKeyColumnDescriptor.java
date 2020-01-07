package org.openl.rules.data;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.domain.EnumDomain;
import org.openl.meta.StringValue;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.convertor.IObjectToDataConvertor;
import org.openl.rules.convertor.ObjectToDataConvertorFactory;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.CollectionUtils;
import org.openl.util.MessageUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * Handles column descriptors that are represented as foreign keys to data from other tables.
 *
 * @author DLiauchuk
 *
 */
public class ForeignKeyColumnDescriptor extends ColumnDescriptor {

    private static final String NOT_INITIALIZED = "<not_initialized>";
    private final IdentifierNode foreignKeyTable;
    private final IdentifierNode[] foreignKeyTableAccessorChainTokens;
    private final IdentifierNode foreignKey;
    private String[] foreignKeyColumnChainTokens = {};

    private CellKey foreignKeyCellCoordinate;

    public ForeignKeyColumnDescriptor(IOpenField field,
            IdentifierNode foreignKeyTable,
            IdentifierNode foreignKey,
            IdentifierNode[] foreignKeyTableAccessorChainTokens,
            ICell foreignKeyCell,
            StringValue displayValue,
            OpenL openl,
            boolean constructor,
            IdentifierNode[] fieldChainTokens,
            int columnNum) {

        super(field, displayValue, openl, constructor, fieldChainTokens, columnNum, false);

        this.foreignKeyTable = foreignKeyTable;
        this.foreignKey = foreignKey;
        this.foreignKeyTableAccessorChainTokens = foreignKeyTableAccessorChainTokens;
        this.foreignKeyCellCoordinate = CellKey.CellKeyFactory.getCellKey(foreignKeyCell.getAbsoluteColumn(),
            foreignKeyCell.getAbsoluteRow());
    }

    /**
     * Gets the value as <code>String</code> from the cell. If there is no value, returns <code>NULL</code>.
     */
    private String getCellStringValue(ILogicalTable cellTable) {

        String value = cellTable.getSource().getCell(0, 0).getStringValue();

        if (value != null) {
            value = value.trim();
        }

        return value;
    }

    /**
     * Goes through the values as foreign keys, finds all info about this objects in foreign table and puts it to array.
     * Can process array value presented as {@link RuleRowHelper#ARRAY_ELEMENTS_SEPARATOR} array.
     *
     * @param valuesTable Logical table representing array values for current table.
     * @param bindingContext binding context
     * @param foreignTable Foreign table with stored info about dependent values.
     * @param foreignKeyIndex index of the foreign key column
     * @return foreign key values
     */
    private ArrayList<Object> getArrayValuesByForeignKey(ILogicalTable valuesTable,
            IBindingContext bindingContext,
            ITable foreignTable,
            int foreignKeyIndex,
            IdentifierNode[] foreignKeyTableAccessorChainTokens) throws SyntaxNodeException {

        int valuesHeight = valuesTable.getHeight();

        ArrayList<Object> values = new ArrayList<>(valuesHeight);

        if (valuesHeight == 1) {
            // load array of values as comma separated parameters
            String[] tokens = RuleRowHelper.extractElementsFromCommaSeparatedArray(valuesTable);

            if (tokens != null) {
                for (String token : tokens) {
                    Object res = getValueByForeignKeyIndex(bindingContext,
                        foreignTable,
                        foreignKeyIndex,
                        foreignKeyTableAccessorChainTokens,
                        valuesTable,
                        token);

                    addResValues(values, res);
                }
            }
        } else {

            for (int i = 0; i < valuesHeight; i++) {
                // we take the appropriate cell for the current value.
                ILogicalTable valueTable = valuesTable.getRow(i);
                String value = getCellStringValue(valueTable);

                if (value == null || value.length() == 0) {
                    values.add(null);
                    continue;
                }

                Object res = getValueByForeignKeyIndex(bindingContext,
                    foreignTable,
                    foreignKeyIndex,
                    foreignKeyTableAccessorChainTokens,
                    valueTable,
                    value);

                addResValues(values, res);
            }
        }

        return values;
    }

    private void addResValues(ArrayList<Object> values, Object res) {
        if (res != null && res.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(res); i++) {
                values.add(Array.get(res, i));
            }
        } else {
            values.add(res);
        }
    }

    /**
     * Tries to find value by its key in foreign table. If no, throws an exception.
     */
    private Object getValueByForeignKeyIndex(IBindingContext bindingContext,
            ITable foreignTable,
            int foreignKeyIndex,
            IdentifierNode[] foreignKeyTableAccessorChainTokens,
            ILogicalTable valueTable,
            String key) throws SyntaxNodeException {

        Object result;

        try {
            if (foreignKeyColumnChainTokens.length == 0) {
                foreignKeyColumnChainTokens = ArrayUtils.add(foreignKeyColumnChainTokens,
                    foreignTable.getColumnName(foreignKeyIndex));
                ColumnDescriptor foreignColumnDescriptor = foreignTable.getDataModel().getDescriptor(foreignKeyIndex);
                if (foreignColumnDescriptor
                    .isReference() && foreignColumnDescriptor instanceof ForeignKeyColumnDescriptor) {
                    // In the case when foreign key is like: ">policies.driver"
                    String[] endOfChain = ((ForeignKeyColumnDescriptor) foreignColumnDescriptor).foreignKeyColumnChainTokens;
                    foreignKeyColumnChainTokens = ArrayUtils.addAll(foreignKeyColumnChainTokens, endOfChain);
                }
            }
            result = foreignTable.findObject(foreignKeyIndex, key, bindingContext);
            if (result == null) {
                throw createIndexNotFoundError(foreignTable, valueTable, key, null, bindingContext);
            }

            if (!ArrayUtils.isEmpty(foreignKeyTableAccessorChainTokens)) {
                ResultChainObject chainRes = getChainObject(bindingContext, result, foreignKeyTableAccessorChainTokens);
                if (chainRes == null) {
                    throw createIndexNotFoundError(foreignTable, valueTable, key, null, bindingContext);
                }
                result = chainRes.getValue();
            }

        } catch (SyntaxNodeException ex) {
            throw createIndexNotFoundError(foreignTable, valueTable, key, ex, bindingContext);
        }

        return result;
    }

    private SyntaxNodeException createIndexNotFoundError(ITable foreignTable,
            ILogicalTable valuesTable,
            String src,
            Exception ex,
            IBindingContext bindingContext) {

        String message = MessageUtils.getUnknownForeignKeyIndexErrorMessage(src, foreignTable.getName());
        return SyntaxNodeExceptionUtils.createError(message, ex, null,
                new GridCellSourceCodeModule(valuesTable.getSource(), bindingContext));
    }

    /**
     * Method is using to load data from foreign table, using foreign key (see
     * {@link DataTableBindHelper#getForeignKeyTokens(IBindingContext, ILogicalTable, int)}). Is used when data table is
     * represents <b>AS</b> a constructor (see {@link #isConstructor()}).
     */
    public Object getLiteralByForeignKey(IOpenClass fieldType,
            ILogicalTable valuesTable,
            IDataBase db,
            IBindingContext bindingContext) throws Exception {

        String foreignKeyTableName = foreignKeyTable.getIdentifier();
        ITable foreignTable = db.getTable(foreignKeyTableName);
        Object result = null;

        validateForeignTable(foreignTable, foreignKeyTableName);

        int foreignKeyIndex = 0;
        String columnName = NOT_INITIALIZED;

        if (foreignKey != null) {
            columnName = foreignKey.getIdentifier();
            foreignKeyIndex = foreignTable.getColumnIndex(columnName);
        }

        if (foreignKeyIndex == -1) {
            String message = MessageUtils.getConstructorNotFoundMessage(columnName);
            throw SyntaxNodeExceptionUtils.createError(message, null, foreignKey);
        }

        boolean valuesAnArray = isValuesAnArray(fieldType);

        if (!valuesAnArray) {

            String value = getCellStringValue(valuesTable);

            if (value != null && value.length() > 0) {
                result = getValueByForeignKeyIndex(bindingContext,
                    foreignTable,
                    foreignKeyIndex,
                    foreignKeyTableAccessorChainTokens,
                    valuesTable,
                    value);
            }

        } else {

            List<Object> values = new ArrayList<>();
            int valuesHeight = valuesTable.getHeight();

            for (int i = 0; i < valuesHeight; i++) {

                ILogicalTable valueTable = valuesTable.getRow(i);
                String value = getCellStringValue(valueTable);

                if (value == null || value.length() == 0) {
                    break;
                }

                Object res = getValueByForeignKeyIndex(bindingContext,
                    foreignTable,
                    foreignKeyIndex,
                    foreignKeyTableAccessorChainTokens,
                    valueTable,
                    value);
                values.add(res);
            }

            IOpenClass componentType = fieldType.getAggregateInfo().getComponentType(fieldType);
            Object ary = fieldType.getAggregateInfo().makeIndexedAggregate(componentType, values.size());

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
    @Override
    public boolean isReference() {
        return foreignKeyTable != null;
    }

    /**
     * Method is using to load data from foreign table, using foreign key (see
     * {@link DataTableBindHelper#getForeignKeyTokens(IBindingContext, ILogicalTable, int)}). Is used when data table is
     * represents as <b>NOT</b> a constructor (see {@link #isConstructor()}).
     */
    @SuppressWarnings("unchecked")
    public void populateLiteralByForeignKey(Object target,
            ILogicalTable valuesTable,
            IDataBase db,
            IBindingContext cxt,
            IRuntimeEnv env) throws Exception {
        if (getField() != null) {

            if (foreignKeyTable != null) {

                String foreignKeyTableName = foreignKeyTable.getIdentifier();
                ITable foreignTable = db.getTable(foreignKeyTableName);

                validateForeignTable(foreignTable, foreignKeyTableName);

                int foreignKeyIndex = getForeignKeyIndex(foreignTable);

                if (foreignKeyIndex == -1) {
                    String message = MessageUtils.getColumnNotFoundErrorMessage(foreignKey.getIdentifier());
                    throw SyntaxNodeExceptionUtils.createError(message, null, foreignKey);
                }

                // table will have 1xN size
                //
                valuesTable = LogicalTableHelper.make1ColumnTable(valuesTable);

                IOpenClass fieldType = getField().getType();

                IOpenClass resType = foreignTable.getDataModel().getType();
                String s = getCellStringValue(valuesTable);
                if (!StringUtils.isEmpty(s)) {
                    Object result;
                    try {
                        result = foreignTable.findObject(foreignKeyIndex, s, cxt);
                    } catch (SyntaxNodeException ex) {
                        foreignTable.getTableSyntaxNode().addError(ex);
                        cxt.addError(ex);
                        return;
                    }
                    if (result != null) {
                        ResultChainObject chainRes = getChainObject(cxt, result, foreignKeyTableAccessorChainTokens);
                        if (chainRes == null) {
                            throw createIndexNotFoundError(foreignTable, valuesTable, s, null, cxt);
                        }
                        Class<?> instanceClass = chainRes.instanceClass;
                        int dim = 0;
                        while (instanceClass.isArray()) {
                            instanceClass = instanceClass.getComponentType();
                            dim++;
                        }
                        resType = JavaOpenClass.getOpenClass(instanceClass);
                        if (dim > 0) {
                            resType = resType.getArrayType(dim);
                        }
                    }
                }

                boolean isCollection = ClassUtils.isAssignable(fieldType.getInstanceClass(), Collection.class);

                boolean f = true;
                if (fieldType.isArray()) {
                    f = !fieldType.getComponentClass().getInstanceClass().equals(resType.getInstanceClass());
                } else if (isCollection) {
                    f = fieldType.isAssignableFrom(resType);
                }

                if (isSupportMultirows()) {
                    populateLiteralByForeignKeyWithMultiRowSupport(target,
                        valuesTable,
                        cxt,
                        foreignTable,
                        foreignKeyIndex,
                        !f,
                        resType,
                        env);
                    return;
                }

                if (f) {
                    if (!StringUtils.isEmpty(s)) {
                        Object res = getValueByForeignKeyIndex(cxt,
                            foreignTable,
                            foreignKeyIndex,
                            foreignKeyTableAccessorChainTokens,
                            valuesTable,
                            s);
                        IOpenCast cast = cxt.getCast(resType, fieldType);
                        if (cast == null || !cast.isImplicit()) {
                            String message = MessageUtils.getIncompatibleTypesErrorMessage(getField(), fieldType, resType);
                            throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
                        }
                        getField().set(target, cast.convert(res), env);
                    }
                } else {
                    // processing array or list values.
                    List<Object> cellValues = getArrayValuesByForeignKey(valuesTable,
                        cxt,
                        foreignTable,
                        foreignKeyIndex,
                        foreignKeyTableAccessorChainTokens);
                    // Cell can contain empty reference value. As a result we
                    // will
                    // receive collection with one null value element. The
                    // following code snippet
                    // searches null value elements and removes them.
                    //

                    List<Object> values = CollectionUtils.findAll(cellValues, Objects::nonNull);
                    if (!values.isEmpty()) {
                        int size = values.size();
                        IOpenClass componentType = getComponentType(fieldType);

                        Object v = fieldType.getAggregateInfo().makeIndexedAggregate(componentType, size);

                        // Populate result array with values.
                        //
                        boolean isList = ClassUtils.isAssignable(fieldType.getInstanceClass(), List.class);
                        boolean isSet = ClassUtils.isAssignable(fieldType.getInstanceClass(), Set.class);
                        for (int i = 0; i < size; i++) {
                            Object value = values.get(i);
                            if (isList) {
                                ((List<Object>) v).set(i, value);
                            } else if (isSet) {
                                ((Set<Object>) v).add(value);
                            } else {
                                Array.set(v, i, value);
                            }
                        }
                        getField().set(target, v, env);
                    }
                }
            }
        }
    }

    private IOpenClass getComponentType(IOpenClass fieldType) {
        return isValuesAnArray() ? fieldType.getAggregateInfo().getComponentType(fieldType) : JavaOpenClass.OBJECT;
    }

    private void populateLiteralByForeignKeyWithMultiRowSupport(Object target,
            ILogicalTable valuesTable,
            IBindingContext cxt,
            ITable foreignTable,
            int foreignKeyIndex,
            boolean isCollection,
            IOpenClass resType,
            IRuntimeEnv env) throws Exception {
        DatatypeArrayMultiRowElementContext context = (DatatypeArrayMultiRowElementContext) env.getLocalFrame()[0];
        IOpenClass fieldType = getField().getType();
        for (int i = 0; i < valuesTable.getSource().getHeight(); i++) {
            context.setRow(i);
            ILogicalTable logicalTable = LogicalTableHelper
                .logicalTable(valuesTable.getSource().getSubtable(0, i, 1, i + 1))
                .getSubtable(0, 0, 1, 1);
            if (isCollection) {
                List<Object> cellValues = getArrayValuesByForeignKey(logicalTable,
                    cxt,
                    foreignTable,
                    foreignKeyIndex,
                    foreignKeyTableAccessorChainTokens);
                List<Object> values = CollectionUtils.findAll(cellValues, Objects::nonNull);
                IOpenClass componentType = getComponentType(fieldType);
                Object currentValue = getField().get(target, env);
                boolean isList = ClassUtils.isAssignable(fieldType.getInstanceClass(), List.class);
                boolean isSet = ClassUtils.isAssignable(fieldType.getInstanceClass(), Set.class);
                boolean isArray = !isList && !isSet;
                int shift = 0;
                Object v;
                if (currentValue == null) {
                    int size = isArray ? values.size() : 0;
                    v = fieldType.getAggregateInfo().makeIndexedAggregate(componentType, size);
                } else {
                    if (isArray) {
                        shift = Array.getLength(currentValue);
                        int size = values.size() + shift;
                        v = fieldType.getAggregateInfo().makeIndexedAggregate(componentType, size);
                        System.arraycopy(currentValue, 0, v, 0, shift);
                    } else {
                        v = currentValue;
                    }
                }
                for (int j = 0; j < values.size(); j++) {
                    Object value = values.get(j);
                    if (isList) {
                        ((List<Object>) v).add(value);
                    } else if (isSet) {
                        ((Set<Object>) v).add(value);
                    } else {
                        Array.set(v, j + shift, value);
                    }
                }
                getField().set(target, v, env);
            } else {
                String s = getCellStringValue(logicalTable);
                if (StringUtils.isEmpty(s)) {
                    continue;
                }
                Object res = getValueByForeignKeyIndex(cxt,
                    foreignTable,
                    foreignKeyIndex,
                    foreignKeyTableAccessorChainTokens,
                    logicalTable,
                    s);
                IOpenCast cast = cxt.getCast(resType, fieldType);
                if (cast == null || !cast.isImplicit()) {
                    String message = MessageUtils.getIncompatibleTypesErrorMessage(getField(), fieldType, resType);
                    throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
                }
                getField().set(target, cast.convert(res), env);
            }
        }
    }

    private void validateForeignTable(ITable foreignTable, String foreignKeyTableName) throws SyntaxNodeException {
        if (foreignTable == null) {
            String message = MessageUtils.getTableNotFoundErrorMessage(foreignKeyTableName);
            throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
        } else if (foreignTable.getTableSyntaxNode().hasErrors()) {
            String message = MessageUtils.getForeignTableCompilationErrorsMessage(foreignKeyTableName);
            throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
        }
    }

    private int getForeignKeyIndex(ITable foreignTable) {
        if (foreignKey != null) {
            String columnName = foreignKey.getIdentifier();
            return foreignTable.getColumnIndex(columnName);
        } else {
            ColumnDescriptor descriptor = foreignTable.getDataModel().getDescriptors()[0];
            if (descriptor.isPrimaryKey()) {
                return descriptor.getColumnIdx();
            }
            ColumnDescriptor firstColDescriptor = foreignTable.getDataModel().getDescriptor(0);
            if (firstColDescriptor.isPrimaryKey()) {
                // first column is primary key for another level. So return column index for first descriptor
                return descriptor.getColumnIdx();
            }
            // we don't have defined PK lets use first key as PK
            return 0;
        }
    }

    public DomainOpenClass getDomainClassForForeignTable(IDataBase db) throws SyntaxNodeException {
        if (foreignKeyTable != null) {
            String foreignKeyTableName = foreignKeyTable.getIdentifier();
            ITable foreignTable = db.getTable(foreignKeyTableName);

            int foreignKeyIndex = getForeignKeyIndex(foreignTable);
            if (foreignKeyIndex == -1) {
                return null;
            }

            return getDomainClass(foreignTable, foreignKeyIndex);
        }

        return null;
    }

    private DomainOpenClass getDomainClass(ITable foreignTable, int foreignKeyIndex) throws SyntaxNodeException {
        if (getField() == null) {
            return null;
        }
        final List<Object> foreignTableValues = foreignTable.getUniqueValues(foreignKeyIndex);

        IOpenClass columnType = foreignTable.getColumnType(foreignKeyIndex);
        if (columnType == null || !columnType.isSimple()) {
            columnType = JavaOpenClass.OBJECT;
        }
        Object[] foreignArray = new Object[foreignTableValues.size()];
        for (int i = 0; i < foreignTableValues.size(); i++) {
            Object foreignValue = foreignTableValues.get(i);
            foreignArray[i] = foreignValue;

            // If String - no need to convert to Object and later format back.
            // Otherwise will be formatted later.
            if (foreignValue != null && !(foreignValue instanceof String)) {
                IObjectToDataConvertor convertor = ObjectToDataConvertorFactory
                    .getConvertor(columnType.getInstanceClass(), foreignValue.getClass());
                if (convertor != ObjectToDataConvertorFactory.NO_Convertor) {
                    foreignArray[i] = convertor.convert(foreignValue);
                }
            }

        }
        EnumDomain<Object> domain = new EnumDomain<>(foreignArray);
        return new DomainOpenClass(getField().getName(), columnType, domain, null);
    }

    private ResultChainObject getChainObject(IBindingContext bindingContext,
            Object parentObj,
            IdentifierNode[] fieldChainTokens) {
        Object resObj = parentObj;
        Class<?> resInctClass = parentObj.getClass();
        if (fieldChainTokens.length > 1) {
            IOpenField openField = DataTableBindHelper.processFieldsChain(bindingContext,
                null,
                JavaOpenClass.getOpenClass(resInctClass),
                ArrayUtils.subarray(fieldChainTokens, 1, fieldChainTokens.length));
            if (openField == null) {
                return null;
            }
            resObj = openField.get(resObj, new SimpleRulesVM().getRuntimeEnv());
            resInctClass = openField.getType().getInstanceClass();
        }
        return new ResultChainObject(resObj, resInctClass);
    }

    public IdentifierNode getForeignKeyTable() {
        return foreignKeyTable;
    }

    public CellKey getForeignKeyCellCoordinate() {
        return foreignKeyCellCoordinate;
    }

    public IOpenField getForeignKeyField(IOpenClass type, IDataBase db) {
        if (foreignKeyColumnChainTokens.length > 0) {
            String fieldName = foreignKeyColumnChainTokens[foreignKeyColumnChainTokens.length - 1];

            if (isValuesAnArray(type)) {
                type = type.getComponentClass();
            }

            ITable table = db == null || foreignKeyTable == null ? null : db.getTable(foreignKeyTable.getIdentifier());
            return table == null ? type.getField(fieldName) : DataTableBindHelper.findField(fieldName, table, type);
        }
        return null;
    }

    static class ResultChainObject {
        private Object value;
        private Class<?> instanceClass;

        ResultChainObject(Object value, Class<?> instanceClass) {
            this.value = value;
            this.instanceClass = instanceClass;
        }

        public Object getValue() {
            return value;
        }

        public Class<?> getInstanceClass() {
            return instanceClass;
        }
    }
}