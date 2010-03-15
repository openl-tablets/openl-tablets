/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.impl.BoundError;
import org.openl.meta.StringValue;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.data.ITable;
import org.openl.rules.dt.FunctionalRow;
import org.openl.rules.table.ALogicalTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
public class ColumnDescriptor {

    private IOpenField field;
    private StringValue displayValue;
    private OpenL openl;

    private Map<String, Integer> uniqueIndex = null;

    protected IRuntimeEnv getRuntimeEnv() {
        return openl.getVm().getRuntimeEnv();
    }

    /**
     * Checks if type values are represented as array of elements.
     * 
     * @param paramType Parameter type.
     * @return
     */
    protected boolean isValuesAnArray(IOpenClass paramType) {
        return paramType.getAggregateInfo().isAggregate(paramType);
    }

    private Object getArrayValues(ILogicalTable valuesTable, OpenlToolAdaptor ota, IOpenClass paramType)
            throws BoundError {

        if (valuesTable.getLogicalHeight() == 1 && valuesTable.getLogicalWidth() == 1) {
            return loadSingleRowArray(valuesTable, ota, paramType);
        }

        if (valuesTable.getLogicalHeight() != 1) {
            valuesTable.transpose();
            return loadMultiRowArray(valuesTable, ota, paramType);
        }
        
        return loadMultiRowArray(valuesTable, ota, paramType);
    }

    private Object loadSingleRowArray(ILogicalTable logicalTable, OpenlToolAdaptor openlAdaptor, IOpenClass paramType)
            throws BoundError {

            return getValuesArrayCommaSeparated(logicalTable, openlAdaptor, paramType);
    }

    private Object loadMultiRowArray(ILogicalTable logicalTable, OpenlToolAdaptor openlAdaptor, IOpenClass paramType)
            throws BoundError {

        int valuesTableHeight = logicalTable.getLogicalHeight();
        ArrayList<Object> values = new ArrayList<Object>(valuesTableHeight);

        for (int i = 0; i < valuesTableHeight; i++) {

            Object res = FunctionalRow.loadSingleParam(paramType, field.getName(), null, logicalTable.getLogicalRow(i),
                    openlAdaptor);

            if (res == null) {
                res = paramType.nullObject();
            }

            values.add(res);
        }

        Object arrayValues = paramType.getAggregateInfo().makeIndexedAggregate(paramType, new int[] { values.size() });

        for (int i = 0; i < values.size(); i++) {
            Array.set(arrayValues, i, values.get(i));
        }

        return arrayValues;
    }

    private Object getValuesArrayCommaSeparated(ILogicalTable valuesTable, OpenlToolAdaptor ota, IOpenClass paramType)
            throws BoundError {
        Object res = FunctionalRow.loadCommaSeparatedParam(paramType, field.getName(), null, valuesTable
                .getLogicalRow(0), ota);
        return res;
    }

    public ColumnDescriptor(IOpenField field, StringValue displayValue, OpenL openl) {
        this.field = field;
        this.displayValue = displayValue;
        this.openl = openl;
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
     * Method is using to load data. Is used when data table is represents
     * <b>AS</b> a constructor (see {@link #isConstructor()}).
     */
    public Object getLiteral(IOpenClass paramType, ILogicalTable valuesTable, OpenlToolAdaptor ota) throws Exception {
        Object resultLiteral = null;
        boolean valuesAnArray = isValuesAnArray(paramType);

        if (valuesAnArray) {
            paramType = paramType.getAggregateInfo().getComponentType(paramType);
        }

        valuesTable = ALogicalTable.make1ColumnTable(valuesTable);

        if (!valuesAnArray) {
            resultLiteral = FunctionalRow.loadSingleParam(paramType, field == null ? FunctionalRow.CONSTRUCTOR : field
                    .getName(), null, valuesTable, ota);
        } else {

            // FIXME:SEEMS we don`t need it. And it was copy-pasted. Always
            // works previous branch. Can`t find use case for this branch.
            // we can`t load array of arrays.
            // List<Object> values = new ArrayList<Object>();
            // int valuesHeight = valuesTable.getLogicalHeight();
            // for (int i = 0; i < valuesHeight; i++) {
            // Object res = FunctionalRow.loadSingleParam(paramType,
            // field.getName(), null, valuesTable.getLogicalRow(i), null);
            // if (res == null) {
            // break;
            // }
            // values.add(res);
            // }
            // resultLiteral =
            // paramType.getAggregateInfo().makeIndexedAggregate(paramType, new
            // int[] { values.size() });
            //
            // for (int i = 0; i < values.size(); i++) {
            // Array.set(resultLiteral, i, values.get(i));
            // }
        }
        return resultLiteral;
    }

    public String getName() {
        return field == null ? "this" : field.getName();
    }

    public IOpenClass getType() {
        return field.getType();
    }

    public synchronized Map<String, Integer> getUniqueIndex(ITable table, int idx) throws BoundError {
        if (uniqueIndex == null) {
            uniqueIndex = table.makeUniqueIndex(idx);
        }
        return uniqueIndex;
    }

    public boolean isConstructor() {
        return field == null;
    }

    /**
     * Method is using to load data. Is used when data table is represents as
     * <b>NOT</b> a constructor (see {@link #isConstructor()}). Support loading
     * single value, array of values.
     */
    public void populateLiteral(Object literal, ILogicalTable valuesTable, OpenlToolAdaptor toolAdapter)
            throws Exception {
        IOpenClass paramType = field.getType();
        boolean valuesAnArray = isValuesAnArray(paramType);

        if (valuesAnArray) {
            paramType = paramType.getAggregateInfo().getComponentType(paramType);
        }

        valuesTable = ALogicalTable.make1ColumnTable(valuesTable);

        if (!valuesAnArray) {
            Object res = FunctionalRow.loadSingleParam(paramType, field.getName(), null, valuesTable, toolAdapter);
            if (res != null) {
                field.set(literal, res, getRuntimeEnv());
            }
        } else {
            Object arrayValues = getArrayValues(valuesTable, toolAdapter, paramType);

            field.set(literal, arrayValues, getRuntimeEnv());
        }
    }

    public boolean isReference() {
        return false;
    }

}
