/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.meta.StringValue;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.lang.xls.types.DatatypeOpenField;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public class ColumnDescriptor {

    static final Object PREV_RES_EMPTY = new Object();

    @Getter(AccessLevel.PROTECTED)
    private final IOpenField field;
    private final StringValue displayValue;
    private final OpenL openl;
    @Getter
    private final boolean valuesAnArray;
    @Getter
    @Setter
    private boolean supportMultirows;

    /**
     * Flag indicating that current column descriptor is a constructor.<br>
     * See {@link DataTableBindHelper#CONSTRUCTOR_FIELD}.
     */
    @Getter
    private final boolean constructor;

    private Map<String, Integer> uniqueIndex;
    @Getter
    private final IdentifierNode[] fieldChainTokens;
    @Getter
    @Setter
    private ColumnGroupKey groupKey;
    @Getter
    private final int columnIdx;
    @Getter
    private final boolean primaryKey; // true if current descriptor is PK

    public ColumnDescriptor(@Nullable IOpenField field,
                            StringValue displayValue,
                            OpenL openl,
                            boolean constructor,
                            IdentifierNode[] fieldChainTokens,
                            int columnIdx,
                            boolean primaryKey) {
        this.field = field;
        this.displayValue = displayValue;
        this.openl = openl;
        this.constructor = constructor;
        this.fieldChainTokens = fieldChainTokens;
        this.primaryKey = primaryKey;
        this.columnIdx = columnIdx;
        if (field == null) {
            this.valuesAnArray = false;
        } else {
            this.valuesAnArray = isValuesAnArray(field.getType());
            this.supportMultirows = isSupportMultirows(field);
        }
    }

    protected IRuntimeEnv getRuntimeEnv() {
        return openl.getVm().getRuntimeEnv();
    }

    /**
     * Checks if type values are represented as array of elements.
     *
     * @param paramType Parameter type.
     * @return true if paramType represents array
     */
    protected static boolean isValuesAnArray(IOpenClass paramType) {
        if (paramType.getAggregateInfo() == null) {
            return false;
        }
        return paramType.getAggregateInfo().isAggregate(paramType);
    }

    private static boolean isSupportMultirows(IOpenField field) {
        if (field instanceof FieldChain fieldChain) {
            var fields = fieldChain.getFields();
            for (IOpenField f : fields) {
                if (f instanceof CollectionElementWithMultiRowField) {
                    return true;
                }
            }
        }
        return false;
    }

    public ColumnGroupKey buildGroupKey() {
        if (field instanceof FieldChain chain) {
            var fields = chain.getFields().length;
            if (isPrimaryKey() && chain
                    .getFields()[fields - 1] instanceof CollectionElementWithMultiRowField) {
                fields += 1;
            } else if (valuesAnArray) {
                return new ColumnGroupKey(fields, field.getName(), true);
            }
            return new ColumnGroupKey(fields - 1, fields > 1 ? field.getName() : "this", primaryKey);
        }
        return ColumnGroupKey.DEFAULT;
    }

    public Object getColumnValue(Object target) {
        return field == null ? target : field.get(target, getRuntimeEnv());
    }

    public String getDisplayName() {
        return displayValue.getValue();
    }

    /**
     * Method is using to load data. Is used when data table is represents <b>AS</b> a constructor (see
     * {@link #isConstructor()}).
     */
    public Object getLiteral(IOpenClass paramType,
                             ILogicalTable valuesTable,
                             OpenlToolAdaptor ota) {

        var isValuesAnArray = isValuesAnArray(paramType);
        valuesTable = LogicalTableHelper.make1ColumnTable(valuesTable);

        if (isValuesAnArray) {
            var aggregateType = paramType;
            paramType = aggregateType.getAggregateInfo().getComponentType(paramType);
            if (valuesTable.getHeight() == 1 && valuesTable.getWidth() == 1) {
                return RuleRowHelper.loadCommaSeparatedParam(aggregateType,
                        paramType,
                        field == null ? RuleRowHelper.CONSTRUCTOR : field.getName(),
                        null,
                        valuesTable,
                        ota);
            }
            return loadMultiRowArray(valuesTable, ota, paramType, aggregateType);
        }
        return getSingleValue(valuesTable, ota, paramType);
    }

    public String getName() {
        return field == null ? "this" : field.getName();
    }

    public @Nullable IOpenClass getType() {
        return field == null ? null : field.getType();
    }

    public synchronized Map<String, Integer> getUniqueIndex(ITable table, int idx, IBindingContext cxt) {
        if (uniqueIndex == null) {
            uniqueIndex = table.makeUniqueIndex(idx, cxt);
        }
        return uniqueIndex;
    }

    /**
     * Method is using to load data. Is used when data table is represents as <b>NOT</b> a constructor (see
     * {@link #isConstructor()}). Support loading single value, array of values.
     */
    public Object populateLiteral(Object literal,
                                  ILogicalTable valuesTable,
                                  OpenlToolAdaptor toolAdapter,
                                  IRuntimeEnv env, boolean allowsNullObject) {

        if (field == null) {
            /*
             * field == null, in this case don`t do anything. The appropriate information why it is null would have been
             * processed during prepDaring column descriptor. See {@link DataTableBindHelper#makeDescriptors}.
             */
            return literal;
        }
        var aggregateType = field.getType();
        var paramType = aggregateType;

        if (valuesAnArray) {
            paramType = paramType.getAggregateInfo().getComponentType(paramType);
        }

        valuesTable = LogicalTableHelper.make1ColumnTable(valuesTable);

        env.pushThis(literal);
        if (supportMultirows) {
            processWithMultiRowsSupport(literal, valuesTable, toolAdapter, env, aggregateType, paramType);
        } else {
            Object res;
            if (valuesAnArray) {
                res = getArrayValues(valuesTable, toolAdapter, aggregateType, paramType);
            } else {
                var sourceGrid = valuesTable.getSource().getSubtable(0, 0, 1, 1);
                var logicalTable = LogicalTableHelper.logicalTable(sourceGrid).getSubtable(0, 0, 1, 1);
                res = getSingleValue(logicalTable, toolAdapter, paramType);
            }
            if (res != null) {
                field.set(literal, res, env);
            } else if (allowsNullObject) {
                field.set(literal, paramType.nullObject(), env);
            }
        }
        return env.popThis();
    }

    public void setFieldValue(Object literal, @Nullable Object res, IRuntimeEnv env) {
        if (field == null) {
            return;
        }
        if (res != null) {
            field.set(literal, res, env);
        } else {
            field.get(literal, env); // Do not delete this line!!!
        }
    }

    public @Nullable Object getFieldValue(Object literal, IRuntimeEnv env) {
        return field != null ? field.get(literal, env) : null;
    }

    private void processWithMultiRowsSupport(Object literal,
                                             ILogicalTable valuesTable,
                                             OpenlToolAdaptor toolAdapter,
                                             IRuntimeEnv env,
                                             IOpenClass aggregateType,
                                             IOpenClass paramType) {

        var datatypeArrayMultiRowElementContext =
                (DatatypeArrayMultiRowElementContext) env.getLocalFrame()[0];
        var prevRes = PREV_RES_EMPTY;
        for (var i = 0; i < valuesTable.getSource().getHeight(); i++) {
            datatypeArrayMultiRowElementContext.setRow(i);
            Object res;
            var logicalTable = LogicalTableHelper
                    .logicalTable(valuesTable.getSource().getSubtable(0, i, 1, i + 1))
                    .getSubtable(0, 0, 1, 1);
            var isSame = false;
            if (valuesAnArray) {
                res = getArrayValues(logicalTable, toolAdapter, aggregateType, paramType);
                if (prevRes != null && prevRes.getClass().isArray()) {
                    isSame = isSameArrayValue(res, prevRes);
                    datatypeArrayMultiRowElementContext.setRowValueIsTheSameAsPrevious(isSame);
                } else {
                    datatypeArrayMultiRowElementContext.setRowValueIsTheSameAsPrevious(false);
                }
            } else {
                res = getSingleValue(logicalTable, toolAdapter, paramType);
                isSame = isSameSingleValueAllowsNull(res, prevRes);
                datatypeArrayMultiRowElementContext.setRowValueIsTheSameAsPrevious(isSame);
            }
            if (isSame) {
                res = prevRes;
            }
            if (res != null || PREV_RES_EMPTY == prevRes) {
                field.set(literal, res, env);
            } else {
                field.get(literal, env); // Do not delete this line!!!
            }
            prevRes = res;
        }
    }

    Object parseCellValue(ILogicalTable valuesTable, OpenlToolAdaptor toolAdapter) {
        var aggregateType = field.getType();
        var paramType = aggregateType;

        if (valuesAnArray) {
            paramType = paramType.getAggregateInfo().getComponentType(paramType);
        }

        return valuesAnArray ? getArrayValues(valuesTable, toolAdapter, aggregateType, paramType)
                : getSingleValue(valuesTable, toolAdapter, paramType);
    }

    boolean isSameValue(Object res, Object prevRes) {
        return valuesAnArray ? isSameArrayValue(res, prevRes) : isSameSingleValue(res, prevRes);
    }

    private static boolean isSameArrayValue(@Nullable Object res, @Nullable Object prevRes) {
        if (prevRes == PREV_RES_EMPTY) {
            return false;
        }
        return sizeOf(res) == 0 || Objects.deepEquals(prevRes, res);
    }

    /**
     * Returns the number of the values of an array or a collection. A missing value has none.
     */
    static int sizeOf(@Nullable Object values) {
        if (values == null) {
            return 0;
        }
        if (values instanceof Collection<?> collection) {
            return collection.size();
        }
        return Array.getLength(values);
    }

    private boolean isSameSingleValue(Object res, Object prevRes) {
        return isSameSingleValueAllowsNull(res, prevRes) || (prevRes != PREV_RES_EMPTY && res == null);
    }

    private static boolean isSameSingleValueAllowsNull(Object res, Object prevRes) {
        return (prevRes == null && res == null) || (prevRes != null && prevRes.equals(res));
    }

    public boolean isReference() {
        return false;
    }

    private Object getArrayValues(ILogicalTable valuesTable,
                                  OpenlToolAdaptor ota,
                                  IOpenClass aggregateType,
                                  IOpenClass paramType) {

        if (valuesTable.getHeight() == 1 && valuesTable.getWidth() == 1) {
            String fieldName = field == null ? RuleRowHelper.CONSTRUCTOR : field.getName();
            return RuleRowHelper
                    .loadCommaSeparatedParam(aggregateType, paramType, fieldName, null, valuesTable.getRow(0), ota);
        }

        return loadMultiRowArray(valuesTable, ota, paramType, aggregateType);
    }

    private Object getSingleValue(ILogicalTable logicalTable,
                                  OpenlToolAdaptor toolAdapter,
                                  IOpenClass paramType) {
        String fieldName = field == null ? RuleRowHelper.CONSTRUCTOR : field.getName();
        return RuleRowHelper.loadSingleParam(paramType, fieldName, null, logicalTable, toolAdapter);
    }

    private Object loadMultiRowArray(ILogicalTable logicalTable,
                                     OpenlToolAdaptor openlAdaptor,
                                     IOpenClass paramType,
                                     IOpenClass aggregateType) {

        // get height of table without empty cells at the end
        //
        var valuesTableHeight = RuleRowHelper.calculateHeight(logicalTable);
        var values = new ArrayList<Object>(valuesTableHeight);

        for (var i = 0; i < valuesTableHeight; i++) {
            var res = getSingleValue(logicalTable.getRow(i), openlAdaptor, paramType);

            // Change request: null value cells should be loaded into array as a
            // null value elements.
            //
            if (res == null) {
                res = paramType.nullObject();
            }

            values.add(res);
        }

        var aggregateInfo = aggregateType.getAggregateInfo();
        var arrayValues = aggregateInfo.makeIndexedAggregate(paramType, values.size());
        var index = aggregateInfo.getIndex(aggregateType);

        for (var i = 0; i < values.size(); i++) {
            index.setValue(arrayValues, i, values.get(i));
        }

        return arrayValues;
    }

    boolean isDeclaredClassSupportMultirow() {
        var targetField = getTargetField();
        if (!(targetField instanceof DatatypeOpenField)) {
            return true;
        }
        var declaredClass = targetField.getDeclaringClass();

        for (IOpenField declaredField : declaredClass.getFields()) {
            var fieldType = declaredField.getType();
            var info = fieldType.getAggregateInfo();
            if (info != null && info.isAggregate(fieldType)) {
                return true;
            }
        }
        return false;
    }

    private IOpenField getTargetField() {
        if (!(field instanceof FieldChain)) {
            return null;
        }
        var fieldChain = (FieldChain) field;
        var fields = fieldChain.getFields();
        if (fields.length == 0) {
            return null;
        }
        return fields[fields.length - 1];
    }

    public static final class ColumnGroupKey implements Comparable<ColumnGroupKey> {

        private static final ColumnGroupKey DEFAULT = new ColumnGroupKey(0, "this", false);

        private final int level;
        private final String path;

        public ColumnGroupKey(int level, String path, boolean pk) {
            this.level = level;
            if (pk) {
                this.path = path;
            } else {
                var sep = path.lastIndexOf('.');
                this.path = sep > 0 ? path.substring(0, sep) : path;
            }
        }

        @Override
        public int compareTo(ColumnGroupKey o) {
            var i = Integer.compare(level, o.level);
            if (i != 0) {
                return i;
            }
            return path.compareTo(o.path);
        }

        /**
         * Checks whether the columns of the other group define the object of this group or an object nested in it.
         */
        boolean contains(ColumnGroupKey other) {
            var nested = other.level > level && other.path.startsWith(path + ".");
            return nested || equals(other);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            var key = (ColumnGroupKey) o;
            return level == key.level && Objects.equals(path, key.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(level, path);
        }
    }
}
