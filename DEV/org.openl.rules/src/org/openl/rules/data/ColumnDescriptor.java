/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import org.openl.OpenL;
import org.openl.meta.StringValue;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenIndex;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class ColumnDescriptor {

    static final Object PREV_RES_EMPTY = new Object();

    private final IOpenField field;
    private final StringValue displayValue;
    private final OpenL openl;
    private final boolean valuesAnArray;
    private boolean supportMultirows = false;

    /**
     * Flag indicating that current column descriptor is a constructor.<br>
     * See {@link DataTableBindHelper#CONSTRUCTOR_FIELD}.
     */
    private final boolean constructor;

    private Map<String, Integer> uniqueIndex = null;
    private final IdentifierNode[] fieldChainTokens;
    private ColumnGroupKey groupKey;
    private final int columnIdx;
    private final boolean primaryKey; //true if current descriptor is PK

    public ColumnDescriptor(IOpenField field,
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
        if (field instanceof FieldChain) {
            FieldChain fieldChain = (FieldChain) field;
            IOpenField[] fields = fieldChain.getFields();
            for (IOpenField f : fields) {
                if (f instanceof CollectionElementWithMultiRowField) {
                    return true;
                }
            }
        }
        return false;
    }

    public ColumnGroupKey buildGroupKey() {
        if (field instanceof FieldChain) {
            int fields = ((FieldChain) field).getFields().length;
            if (isPrimaryKey() && ((FieldChain) field).getFields()[fields - 1] instanceof CollectionElementWithMultiRowField) {
                fields += 1;
            } else if (valuesAnArray) {
                return new ColumnGroupKey(fields, field.getName(), true);
            }
            return new ColumnGroupKey(fields - 1, fields > 1 ? field.getName() : "this", primaryKey);
        } else {
            return ColumnGroupKey.DEFAULT;
        }
    }

    protected IOpenField getField() {
        return field;
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
            OpenlToolAdaptor ota) throws SyntaxNodeException {

        boolean valuesAnArray = isValuesAnArray(paramType);
        valuesTable = LogicalTableHelper.make1ColumnTable(valuesTable);

        if (valuesAnArray) {
            IOpenClass aggregateType = paramType;
            paramType = aggregateType.getAggregateInfo().getComponentType(paramType);
            if (valuesTable.getHeight() == 1 && valuesTable.getWidth() == 1) {
                return RuleRowHelper.loadCommaSeparatedParam(aggregateType,
                        paramType,
                        field == null ? RuleRowHelper.CONSTRUCTOR : field.getName(),
                        null,
                        valuesTable,
                        ota);
            } else {
                return loadMultiRowArray(valuesTable, ota, paramType, aggregateType);
            }
        } else {
            return getSingleValue(valuesTable, ota, paramType);
        }
    }

    public String getName() {
        return field == null ? "this" : field.getName();
    }

    public IOpenClass getType() {
        return field == null ? null : field.getType();
    }

    public synchronized Map<String, Integer> getUniqueIndex(ITable table, int idx) throws SyntaxNodeException {
        if (uniqueIndex == null) {
            uniqueIndex = table.makeUniqueIndex(idx);
        }
        return uniqueIndex;
    }

    public boolean isConstructor() {
        return constructor;
    }

    public IdentifierNode[] getFieldChainTokens() {
        return fieldChainTokens;
    }

    /**
     * Method is using to load data. Is used when data table is represents as <b>NOT</b> a constructor (see
     * {@link #isConstructor()}). Support loading single value, array of values.
     */
    public Object populateLiteral(Object literal,
            ILogicalTable valuesTable,
            OpenlToolAdaptor toolAdapter,
            IRuntimeEnv env) throws SyntaxNodeException {

        if (field == null) {
            /*
             * field == null, in this case don`t do anything. The appropriate information why it is null would have been
             * processed during prepDaring column descriptor. See {@link
             * DataTableBindHelper#makeDescriptors(IBindingContext bindingContext, ITable table, IOpenClass type, OpenL
             * openl, ILogicalTable descriptorRows, ILogicalTable dataWithTitleRows, boolean hasForeignKeysRow, boolean
             * hasColumnTytleRow)}
             */
            return literal;
        }
        IOpenClass aggregateType = field.getType();
        IOpenClass paramType = aggregateType;

        if (valuesAnArray) {
            paramType = paramType.getAggregateInfo().getComponentType(paramType);
        }

        valuesTable = LogicalTableHelper.make1ColumnTable(valuesTable);

        env.pushThis(literal);
        if (supportMultirows) {
            processWithMultiRowsSupport(literal,
                valuesTable,
                toolAdapter,
                env,
                aggregateType,
                paramType);
        } else {
            Object res;
            if (valuesAnArray) {
                res = getArrayValues(valuesTable, toolAdapter, aggregateType, paramType);
            } else {
                IGridTable sourceGrid = valuesTable.getSource().getSubtable(0, 0, 1, 1);
                ILogicalTable logicalTable = LogicalTableHelper.logicalTable(sourceGrid).getSubtable(0, 0, 1, 1);
                res = getSingleValue(logicalTable, toolAdapter, paramType);
            }
            if (res != null) {
                field.set(literal, res, env);
            }
        }
        return env.popThis();
    }

    public void setFieldValue(Object literal, Object res, IRuntimeEnv env) {
        if (field == null) {
            return;
        }
        if (res != null) {
            field.set(literal, res, env);
        } else {
            field.get(literal, env); // Do not delete this line!!!
        }
    }

    public Object getFieldValue(Object literal, IRuntimeEnv env) {
        return field != null ? field.get(literal, env) : null;
    }

    private void processWithMultiRowsSupport(Object literal,
            ILogicalTable valuesTable,
            OpenlToolAdaptor toolAdapter,
            IRuntimeEnv env,
            IOpenClass aggregateType,
            IOpenClass paramType) throws SyntaxNodeException {

        DatatypeArrayMultiRowElementContext datatypeArrayMultiRowElementContext = (DatatypeArrayMultiRowElementContext) env
            .getLocalFrame()[0];
        Object prevRes = PREV_RES_EMPTY;
        for (int i = 0; i < valuesTable.getSource().getHeight(); i++) {
            datatypeArrayMultiRowElementContext.setRow(i);
            Object res;
            ILogicalTable logicalTable = LogicalTableHelper
                    .logicalTable(valuesTable.getSource().getSubtable(0, i, 1, i + 1))
                    .getSubtable(0, 0, 1, 1);
            boolean isSame = false;
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
                isSame = isSameSingleValue(res, prevRes);
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

    Object parseCellValue(ILogicalTable valuesTable,
                                 OpenlToolAdaptor toolAdapter) throws SyntaxNodeException {
        IOpenClass aggregateType = field.getType();
        IOpenClass paramType = aggregateType;

        if (valuesAnArray) {
            paramType = paramType.getAggregateInfo().getComponentType(paramType);
        }

        return valuesAnArray ? getArrayValues(valuesTable, toolAdapter, aggregateType, paramType)
                : getSingleValue(valuesTable, toolAdapter, paramType);
    }

    boolean isSameValue(Object res, Object prevRes) {
        return valuesAnArray ? isSameArrayValue(res, prevRes) : isSameSingleValue(res, prevRes);
    }

    private static boolean isSameArrayValue(Object res, Object prevRes) {
        boolean resIsEmpty = Array.getLength(res) == 0;

        return (resIsEmpty && Array.getLength(prevRes) == 0)
                || (Arrays.deepEquals((Object[]) prevRes, (Object[]) res))
                || (prevRes != PREV_RES_EMPTY && resIsEmpty);
    }

    private boolean isSameSingleValue(Object res, Object prevRes) {
        return (prevRes == null && res == null)
                || (prevRes != null && prevRes.equals(res))
                || (prevRes != PREV_RES_EMPTY && res == null);
    }

    public boolean isReference() {
        return false;
    }

    private Object getArrayValues(ILogicalTable valuesTable,
            OpenlToolAdaptor ota,
            IOpenClass aggregateType,
            IOpenClass paramType) throws SyntaxNodeException {

        if (valuesTable.getHeight() == 1 && valuesTable.getWidth() == 1) {
            String fieldName = field == null ? RuleRowHelper.CONSTRUCTOR : field.getName();
            return RuleRowHelper
                .loadCommaSeparatedParam(aggregateType, paramType, fieldName, null, valuesTable.getRow(0), ota);
        }

        return loadMultiRowArray(valuesTable, ota, paramType, aggregateType);
    }

    private Object getSingleValue(ILogicalTable logicalTable, OpenlToolAdaptor toolAdapter, IOpenClass paramType) throws SyntaxNodeException {
        String fieldName = field == null ? RuleRowHelper.CONSTRUCTOR : field.getName();
        return RuleRowHelper.loadSingleParam(paramType, fieldName, null, logicalTable, toolAdapter);
    }

    private Object loadMultiRowArray(ILogicalTable logicalTable,
            OpenlToolAdaptor openlAdaptor,
            IOpenClass paramType,
            IOpenClass aggregateType) throws SyntaxNodeException {

        // get height of table without empty cells at the end
        //
        int valuesTableHeight = RuleRowHelper.calculateHeight(logicalTable);/* logicalTable.getHeight(); */
        ArrayList<Object> values = new ArrayList<>(valuesTableHeight);

        for (int i = 0; i < valuesTableHeight; i++) {
            Object res = getSingleValue(logicalTable.getRow(i), openlAdaptor, paramType);

            // Change request: null value cells should be loaded into array as a
            // null value elements.
            //
            if (res == null) {
                res = paramType.nullObject();
            }

            values.add(res);
        }

        IAggregateInfo aggregateInfo = aggregateType.getAggregateInfo();
        Object arrayValues = aggregateInfo.makeIndexedAggregate(paramType, values.size());
        IOpenIndex index = aggregateInfo.getIndex(aggregateType);

        for (int i = 0; i < values.size(); i++) {
            index.setValue(arrayValues, i, values.get(i));
        }

        return arrayValues;
    }

    public boolean isSupportMultirows() {
        return supportMultirows;
    }

    public void setSupportMultirows(boolean supportMultirows) {
        this.supportMultirows = supportMultirows;
    }

    public boolean isValuesAnArray() {
        return valuesAnArray;
    }

    public int getColumnIdx() {
        return columnIdx;
    }

    public ColumnGroupKey getGroupKey() {
        return groupKey;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setGroupKey(ColumnGroupKey key) {
        this.groupKey = key;
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
                int sep = path.lastIndexOf('.');
                this.path = sep > 0 ? path.substring(0, sep) : path;
            }
        }

        public int getLevel() {
            return level;
        }

        @Override
        public int compareTo(ColumnGroupKey o) {
            int i = Integer.compare(level, o.level);
            if (i != 0) {
                return i;
            }
            return path.compareTo(o.path);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ColumnGroupKey key = (ColumnGroupKey) o;
            return level == key.level &&
                    Objects.equals(path, key.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(level, path);
        }
    }
}
