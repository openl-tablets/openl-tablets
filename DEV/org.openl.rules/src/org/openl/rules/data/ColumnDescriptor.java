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

    private IOpenField field;
    private StringValue displayValue;
    private OpenL openl;
    private boolean valuesAnArray = false;
    private boolean supportMultirows = false;

    /**
     * Flag indicating that current column descriptor is a constructor.<br>
     * See {@link DataTableBindHelper#CONSTRUCTOR_FIELD}.
     */
    private boolean constructor;

    private Map<String, Integer> uniqueIndex = null;
    private IdentifierNode[] fieldChainTokens;

    public ColumnDescriptor(IOpenField field,
            StringValue displayValue,
            OpenL openl,
            boolean constructor,
            IdentifierNode[] fieldChainTokens) {
        this.field = field;
        this.displayValue = displayValue;
        this.openl = openl;
        this.constructor = constructor;
        this.fieldChainTokens = fieldChainTokens;
        if (field != null) {
            this.valuesAnArray = isValuesAnArray(field.getType());
            if (field instanceof FieldChain) {
                FieldChain fieldChain = (FieldChain) field;
                for (IOpenField f : fieldChain.getFields()) {
                    if (f instanceof CollectionElementWithMultiRowField) {
                        this.supportMultirows = true;
                    }
                }
            }
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
        Object resultLiteral;
        boolean valuesAnArray = isValuesAnArray(paramType);

        valuesTable = LogicalTableHelper.make1ColumnTable(valuesTable);

        if (!valuesAnArray) {
            resultLiteral = RuleRowHelper.loadSingleParam(paramType,
                field == null ? RuleRowHelper.CONSTRUCTOR : field.getName(),
                null,
                valuesTable,
                ota);
        } else {
            IOpenClass aggregateType = paramType;
            paramType = aggregateType.getAggregateInfo().getComponentType(paramType);
            if (valuesTable.getHeight() == 1 && valuesTable.getWidth() == 1) {
                resultLiteral = RuleRowHelper.loadCommaSeparatedParam(aggregateType,
                    paramType,
                    field == null ? RuleRowHelper.CONSTRUCTOR : field.getName(),
                    null,
                    valuesTable,
                    ota);
            } else {
                resultLiteral = loadMultiRowArray(valuesTable, ota, paramType, aggregateType);
            }
        }

        return resultLiteral;
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
        if (field != null) {
            IOpenClass aggregateType = field.getType();
            IOpenClass paramType = aggregateType;

            if (valuesAnArray) {
                paramType = paramType.getAggregateInfo().getComponentType(paramType);
            }

            valuesTable = LogicalTableHelper.make1ColumnTable(valuesTable);

            if (!valuesAnArray) {
                env.pushThis(literal);
                if (supportMultirows) {
                    processWithMultiRowsSupport(literal, valuesTable, toolAdapter, env, aggregateType, paramType, valuesAnArray);
                } else {
                    IGridTable sourceGrid = valuesTable.getSource().getSubtable(0, 0, 1, 1);
                    ILogicalTable logicalTable = LogicalTableHelper.logicalTable(sourceGrid).getSubtable(0, 0, 1, 1);
                    String fieldName = field.getName();
                    Object res = RuleRowHelper.loadSingleParam(paramType, fieldName, null, logicalTable, toolAdapter);
                    if (res != null) {
                        field.set(literal, res, env);
                    }
                }
                return env.popThis();
            } else {
                env.pushThis(literal);
                Object arrayValues;
                if (supportMultirows) {
                    processWithMultiRowsSupport(literal, valuesTable, toolAdapter, env, aggregateType, paramType, valuesAnArray);
                } else {
                    arrayValues = getArrayValues(valuesTable, toolAdapter, aggregateType, paramType);
                    field.set(literal, arrayValues, getRuntimeEnv());
                }
                return env.popThis();
            }
        } else {
            /*
             * field == null, in this case don`t do anything. The appropriate information why it is null would have been
             * processed during prepDaring column descriptor. See
             * {@link DataTableBindHelper#makeDescriptors(IBindingContext bindingContext, ITable table, IOpenClass type, OpenL openl, ILogicalTable descriptorRows, ILogicalTable dataWithTitleRows, boolean hasForeignKeysRow, boolean hasColumnTytleRow)}
             */
        }
        return literal;
    }

    private static final Object PREV_RES_EMPTY = new Object();

    private void processWithMultiRowsSupport(Object literal,
            ILogicalTable valuesTable,
            OpenlToolAdaptor toolAdapter,
            IRuntimeEnv env,
            IOpenClass aggregateType,
            IOpenClass paramType,
            boolean valuesAnArray) throws SyntaxNodeException {
        DatatypeArrayMultiRowElementContext datatypeArrayMultiRowElementContext = (DatatypeArrayMultiRowElementContext) env
            .getLocalFrame()[0];
        Object prevRes = PREV_RES_EMPTY;
        for (int i = 0; i < valuesTable.getSource().getHeight(); i++) {
            datatypeArrayMultiRowElementContext.setRow(i);
            Object res;
            ILogicalTable logicalTable = LogicalTableHelper
                .logicalTable(valuesTable.getSource().getSubtable(0, i, 1, i + 1))
                .getSubtable(0, 0, 1, 1);
            if (valuesAnArray) {
                res = getArrayValues(logicalTable, toolAdapter, aggregateType, paramType);
                if (prevRes != null && prevRes.getClass().isArray()) {
                    boolean prevResIsEmpty = Array.getLength(prevRes) == 0;
                    boolean resIsEmpty = Array.getLength(res) == 0;
                    if ((prevResIsEmpty && resIsEmpty) || (Arrays.deepEquals((Object[]) prevRes,
                        (Object[]) res)) || (prevRes != PREV_RES_EMPTY && resIsEmpty)) {
                        res = prevRes;
                        datatypeArrayMultiRowElementContext.setRowValueIsTheSameAsPrevious(true);
                    } else {
                        datatypeArrayMultiRowElementContext.setRowValueIsTheSameAsPrevious(false);
                    }
                } else {
                    datatypeArrayMultiRowElementContext.setRowValueIsTheSameAsPrevious(false);
                }
            } else {
                res = RuleRowHelper.loadSingleParam(paramType,
                    field == null ? RuleRowHelper.CONSTRUCTOR : field.getName(),
                    null,
                    logicalTable,
                    toolAdapter);
                if ((prevRes == null && res == null) || (prevRes != null && prevRes
                    .equals(res)) || (prevRes != PREV_RES_EMPTY && res == null)) {
                    res = prevRes;
                    datatypeArrayMultiRowElementContext.setRowValueIsTheSameAsPrevious(true);
                } else {
                    datatypeArrayMultiRowElementContext.setRowValueIsTheSameAsPrevious(false);
                }
            }
            if (res != null || PREV_RES_EMPTY == prevRes) {
                field.set(literal, res, env);
            } else {
                field.get(literal, env); // Do not delete this line!!!
            }
            prevRes = res;
        }
    }

    public boolean isReference() {
        return false;
    }

    private Object getArrayValues(ILogicalTable valuesTable,
            OpenlToolAdaptor ota,
            IOpenClass aggregateType, IOpenClass paramType) throws SyntaxNodeException {

        if (valuesTable.getHeight() == 1 && valuesTable.getWidth() == 1) {
            return RuleRowHelper.loadCommaSeparatedParam(aggregateType, paramType, field.getName(), null, valuesTable.getRow(0), ota);
        }

        return loadMultiRowArray(valuesTable, ota, paramType, aggregateType);
    }

    private Object loadMultiRowArray(ILogicalTable logicalTable,
            OpenlToolAdaptor openlAdaptor,
            IOpenClass paramType, IOpenClass aggregateType) throws SyntaxNodeException {

        // get height of table without empty cells at the end
        //
        int valuesTableHeight = RuleRowHelper.calculateHeight(logicalTable);/* logicalTable.getHeight(); */
        ArrayList<Object> values = new ArrayList<>(valuesTableHeight);

        for (int i = 0; i < valuesTableHeight; i++) {

            Object res = RuleRowHelper.loadSingleParam(paramType,
                field == null ? RuleRowHelper.CONSTRUCTOR : field.getName(),
                null,
                logicalTable.getRow(i),
                openlAdaptor);

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
}
