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
import org.openl.rules.convertor.IObjectToDataConvertor;
import org.openl.rules.convertor.ObjectToDataConvertorFactory;
import org.openl.rules.helpers.ArraySplitter;
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
 */
public class ForeignKeyColumnDescriptor extends ColumnDescriptor {

    private final IdentifierNode foreignKeyTable;
    private final IdentifierNode[] foreignKeyTableAccessorChainTokens;
    private final IdentifierNode foreignKey;
    private String[] foreignKeyColumnChainTokens = {};

    private final CellKey foreignKeyCellCoordinate;

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

        var value = cellTable.getSource().getCell(0, 0).getStringValue();

        if (value != null) {
            value = value.trim();
        }

        return value;
    }

    /**
     * Goes through the values as foreign keys, finds all info about this objects in foreign table and puts it to array.
     * Can process array value presented as comma separated elements.
     *
     * @param valuesTable     Logical table representing array values for current table.
     * @param bindingContext  binding context
     * @param foreignTable    Foreign table with stored info about dependent values.
     * @param foreignKeyIndex index of the foreign key column
     * @return foreign key values
     */
    private ArrayList<Object> getArrayValuesByForeignKey(ILogicalTable valuesTable,
                                                         IBindingContext bindingContext,
                                                         ITable foreignTable,
                                                         int foreignKeyIndex,
                                                         IdentifierNode[] foreignKeyTableAccessorChainTokens) throws SyntaxNodeException {

        var valuesHeight = valuesTable.getHeight();

        var values = new ArrayList<Object>(valuesHeight);

        if (valuesHeight == 1) {
            // load array of values as comma separated parameters

            var src = valuesTable.getSource().getCell(0, 0).getStringValue();

            if (src != null) {
                String[] tokens = ArraySplitter.split(src);
                for (String token : tokens) {
                    var res = getValueByForeignKeyIndex(bindingContext,
                            foreignTable,
                            foreignKeyIndex,
                            foreignKeyTableAccessorChainTokens,
                            valuesTable,
                            token);

                    addResValues(values, res);
                }
            }
        } else {

            for (var i = 0; i < valuesHeight; i++) {
                // we take the appropriate cell for the current value.
                var valueTable = valuesTable.getRow(i);
                var value = getCellStringValue(valueTable);

                if (value == null || value.length() == 0) {
                    values.add(null);
                    continue;
                }

                var res = getValueByForeignKeyIndex(bindingContext,
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
            for (var i = 0; i < Array.getLength(res); i++) {
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
        IOpenClass resType;

        try {
            if (foreignKeyColumnChainTokens.length == 0) {
                foreignKeyColumnChainTokens = ArrayUtils.add(foreignKeyColumnChainTokens,
                        foreignTable.getColumnName(foreignKeyIndex));
                var foreignColumnDescriptor = foreignTable.getDataModel().getDescriptor(foreignKeyIndex);
                if (foreignColumnDescriptor
                        .isReference() && foreignColumnDescriptor instanceof ForeignKeyColumnDescriptor descriptor) {
                    // In the case when foreign key is like: ">policies.driver"
                    var endOfChain = descriptor.foreignKeyColumnChainTokens;
                    foreignKeyColumnChainTokens = ArrayUtils.addAll(foreignKeyColumnChainTokens, endOfChain);
                }
            }
            result = foreignTable.findObject(foreignKeyIndex, key, bindingContext);
            resType = foreignTable.getDataModel().getType();

            if (result == null) {
                throw createIndexNotFoundError(foreignTable, valueTable, key, null, bindingContext);
            }

            if (!ArrayUtils.isEmpty(foreignKeyTableAccessorChainTokens)) {
                var chainRes = getChainObject(bindingContext,
                        resType,
                        result,
                        foreignKeyTableAccessorChainTokens);
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
        return SyntaxNodeExceptionUtils
                .createError(message, ex, null, new GridCellSourceCodeModule(valuesTable.getSource(), bindingContext));
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

        var foreignKeyTableName = foreignKeyTable.getIdentifier();
        var foreignTable = db.getTable(foreignKeyTableName);
        Object result = null;

        var foreignKeyIndex = 0;
        String columnName;

        if (foreignKey != null) {
            columnName = foreignKey.getIdentifier();
            foreignKeyIndex = foreignTable.getColumnIndex(columnName);
        }

        var valuesAnArray = isValuesAnArray(fieldType);

        if (!valuesAnArray) {

            var value = getCellStringValue(valuesTable);

            if (value != null && value.length() > 0) {
                result = getValueByForeignKeyIndex(bindingContext,
                        foreignTable,
                        foreignKeyIndex,
                        foreignKeyTableAccessorChainTokens,
                        valuesTable,
                        value);
            }

        } else {

            var values = new ArrayList<Object>();
            var valuesHeight = valuesTable.getHeight();

            for (var i = 0; i < valuesHeight; i++) {

                var valueTable = valuesTable.getRow(i);
                var value = getCellStringValue(valueTable);

                if (value == null || value.length() == 0) {
                    break;
                }

                var res = getValueByForeignKeyIndex(bindingContext,
                        foreignTable,
                        foreignKeyIndex,
                        foreignKeyTableAccessorChainTokens,
                        valueTable,
                        value);
                values.add(res);
            }

            var componentType = fieldType.getAggregateInfo().getComponentType(fieldType);
            var ary = fieldType.getAggregateInfo().makeIndexedAggregate(componentType, values.size());

            for (var i = 0; i < values.size(); i++) {
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

                var foreignKeyTableName = foreignKeyTable.getIdentifier();
                var foreignTable = db.getTable(foreignKeyTableName);

                var foreignKeyIndex = getForeignKeyIndex(foreignTable);

                // table will have 1xN size
                //
                valuesTable = LogicalTableHelper.make1ColumnTable(valuesTable);

                var fieldType = getField().getType();

                var resType = foreignTable.getDataModel().getType();
                var s = getCellStringValue(valuesTable);
                if (!StringUtils.isEmpty(s)) {
                    Object result;
                    result = foreignTable.findObject(foreignKeyIndex, s, cxt);
                    if (result != null) {
                        var chainRes = getChainObject(cxt,
                                resType,
                                result,
                                foreignKeyTableAccessorChainTokens);
                        if (chainRes == null) {
                            throw createIndexNotFoundError(foreignTable, valuesTable, s, null, cxt);
                        }
                        resType = chainRes.getType();
                    }
                }

                var isCollection = ClassUtils.isAssignable(fieldType.getInstanceClass(), Collection.class);

                var f = true;
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
                        var cast = cxt.getCast(resType, fieldType);
                        if (cast == null || !cast.isImplicit()) {
                            String message = MessageUtils
                                    .getIncompatibleTypesErrorMessage(getField(), fieldType, resType);
                            throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
                        }
                        var res = getValueByForeignKeyIndex(cxt,
                                foreignTable,
                                foreignKeyIndex,
                                foreignKeyTableAccessorChainTokens,
                                valuesTable,
                                s);
                        getField().set(target, cast.convert(res), env);
                    }
                } else {
                    var componentType = getComponentType(fieldType);
                    IOpenCast cast = null;
                    if (fieldType.isArray()) {
                        cast = cxt.getCast(resType, componentType);
                        if (cast == null || !cast.isImplicit()) {
                            String message = MessageUtils
                                    .getIncompatibleTypesErrorMessage(getField(), fieldType, resType.getArrayType(1));
                            throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
                        }
                    }
                    // processing array or list values.
                    var cellValues = getArrayValuesByForeignKey(valuesTable,
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

                    var values = CollectionUtils.findAll(cellValues, Objects::nonNull);
                    if (!values.isEmpty()) {
                        var size = values.size();
                        var v = fieldType.getAggregateInfo().makeIndexedAggregate(componentType, size);

                        // Populate result array with values.
                        //
                        var isList = ClassUtils.isAssignable(fieldType.getInstanceClass(), List.class);
                        var isSet = ClassUtils.isAssignable(fieldType.getInstanceClass(), Set.class);
                        for (var i = 0; i < size; i++) {
                            var value = values.get(i);
                            if (cast != null) {
                                value = cast.convert(value);
                            }
                            if (isList) {
                                ((List<Object>) v).set(i, cast != null ? cast.convert(value) : value);
                            } else if (isSet) {
                                ((Set<Object>) v).add(cast != null ? cast.convert(value) : value);
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
        var context = (DatatypeArrayMultiRowElementContext) env.getLocalFrame()[0];
        var fieldType = getField().getType();
        for (var i = 0; i < valuesTable.getSource().getHeight(); i++) {
            context.setRow(i);
            var logicalTable = LogicalTableHelper
                    .logicalTable(valuesTable.getSource().getSubtable(0, i, 1, i + 1))
                    .getSubtable(0, 0, 1, 1);
            if (isCollection) {
                var cellValues = getArrayValuesByForeignKey(logicalTable,
                        cxt,
                        foreignTable,
                        foreignKeyIndex,
                        foreignKeyTableAccessorChainTokens);
                var values = CollectionUtils.findAll(cellValues, Objects::nonNull);
                var componentType = getComponentType(fieldType);
                var currentValue = getField().get(target, env);
                var isList = ClassUtils.isAssignable(fieldType.getInstanceClass(), List.class);
                var isSet = ClassUtils.isAssignable(fieldType.getInstanceClass(), Set.class);
                var isArray = !isList && !isSet;
                var shift = 0;
                Object v;
                if (currentValue == null) {
                    int size = isArray ? values.size() : 0;
                    v = fieldType.getAggregateInfo().makeIndexedAggregate(componentType, size);
                } else {
                    if (isArray) {
                        shift = Array.getLength(currentValue);
                        var size = values.size() + shift;
                        v = fieldType.getAggregateInfo().makeIndexedAggregate(componentType, size);
                        System.arraycopy(currentValue, 0, v, 0, shift);
                    } else {
                        v = currentValue;
                    }
                }
                for (var j = 0; j < values.size(); j++) {
                    var value = values.get(j);
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
                var s = getCellStringValue(logicalTable);
                if (StringUtils.isEmpty(s)) {
                    continue;
                }
                var res = getValueByForeignKeyIndex(cxt,
                        foreignTable,
                        foreignKeyIndex,
                        foreignKeyTableAccessorChainTokens,
                        logicalTable,
                        s);
                var cast = cxt.getCast(resType, fieldType);
                if (cast == null || !cast.isImplicit()) {
                    String message = MessageUtils.getIncompatibleTypesErrorMessage(getField(), fieldType, resType);
                    throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
                }
                getField().set(target, cast.convert(res), env);
            }
        }
    }

    private int getForeignKeyIndex(ITable foreignTable) {
        if (foreignTable != null) {
            if (foreignKey != null) {
                var columnName = foreignKey.getIdentifier();
                return foreignTable.getColumnIndex(columnName);
            } else {
                var descriptor = foreignTable.getDataModel().getDescriptors()[0];
                if (descriptor.isPrimaryKey()) {
                    return descriptor.getColumnIdx();
                }
                var firstColDescriptor = foreignTable.getDataModel().getDescriptor(0);
                if (firstColDescriptor.isPrimaryKey()) {
                    // first column is primary key for another level. So return column index for first descriptor
                    return descriptor.getColumnIdx();
                }
                // we don't have defined PK lets use first key as PK
                return 0;
            }
        } else {
            return -1;
        }
    }

    public DomainOpenClass getDomainClassForForeignTable(IDataBase db) throws SyntaxNodeException {
        if (foreignKeyTable != null) {
            var foreignKeyTableName = foreignKeyTable.getIdentifier();
            var foreignTable = db.getTable(foreignKeyTableName);

            var foreignKeyIndex = getForeignKeyIndex(foreignTable);
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
        final var foreignTableValues = foreignTable.getUniqueValues(foreignKeyIndex);

        var columnType = foreignTable.getColumnType(foreignKeyIndex);
        if (columnType == null || !columnType.isSimple()) {
            columnType = JavaOpenClass.OBJECT;
        }
        Object[] foreignArray = new Object[foreignTableValues.size()];
        var i = 0;
        for (Object foreignValue : foreignTableValues) {
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
            i++;
        }
        var domain = new EnumDomain<Object>(foreignArray);
        return new DomainOpenClass(getField().getName(), columnType, domain, null, null);
    }

    private ResultChainObject getChainObject(IBindingContext bindingContext,
                                             IOpenClass resType,
                                             Object parentObj,
                                             IdentifierNode[] fieldChainTokens) {
        var resObj = parentObj;
        if (fieldChainTokens.length > 1) {
            IOpenField openField = DataTableBindHelper.processFieldsChain(bindingContext,
                    null,
                    resType,
                    ArrayUtils.subarray(fieldChainTokens, 1, fieldChainTokens.length));
            if (openField == null) {
                return null;
            }
            resObj = openField.get(resObj, new SimpleRulesVM().getRuntimeEnv());
            resType = openField.getType();
        }
        return new ResultChainObject(resObj, resType);
    }

    public IdentifierNode getForeignKeyTable() {
        return foreignKeyTable;
    }

    public IdentifierNode getForeignKey() {
        return foreignKey;
    }

    public CellKey getForeignKeyCellCoordinate() {
        return foreignKeyCellCoordinate;
    }

    public IOpenField getForeignKeyField(IOpenClass type, IDataBase db) {
        if (foreignKeyColumnChainTokens.length > 0) {
            var fieldName = foreignKeyColumnChainTokens[foreignKeyColumnChainTokens.length - 1];

            if (isValuesAnArray(type)) {
                type = type.getComponentClass();
            }

            ITable table = db == null || foreignKeyTable == null ? null : db.getTable(foreignKeyTable.getIdentifier());
            return table == null ? type.getField(fieldName) : DataTableBindHelper.findField(fieldName, table, type);
        }
        return null;
    }

    static class ResultChainObject {
        private final Object value;
        private final IOpenClass type;

        ResultChainObject(Object value, IOpenClass type) {
            this.value = value;
            this.type = type;
        }

        public Object getValue() {
            return value;
        }

        public IOpenClass getType() {
            return type;
        }
    }
}
