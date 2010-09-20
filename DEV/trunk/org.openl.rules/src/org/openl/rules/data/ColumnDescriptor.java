/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;

import org.openl.OpenL;
import org.openl.meta.StringValue;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.OffSetGridTableHelper;
import org.openl.syntax.exception.SyntaxNodeException;
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
    boolean valuesAnArray = false;


    private Map<String, Integer> uniqueIndex = null;

    public ColumnDescriptor(IOpenField field, StringValue displayValue, OpenL openl) {
        this.field = field;
        this.displayValue = displayValue;
        this.openl = openl;
        if (field != null)
            this.valuesAnArray = isValuesAnArray(field.getType());
    }

    protected IRuntimeEnv getRuntimeEnv() {
        return openl.getVm().getRuntimeEnv();
    }

    /**
     * Checks if type values are represented as array of elements.
     * 
     * @param paramType Parameter type.
     * @return
     */
    protected static boolean isValuesAnArray(IOpenClass paramType) {
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
     * Method is using to load data. Is used when data table is represents
     * <b>AS</b> a constructor (see {@link #isConstructor()}).
     * @throws SyntaxNodeException 
     */
    public Object getLiteral(IOpenClass paramType, IGridTable valuesTable, OpenlToolAdaptor ota) throws SyntaxNodeException  {
        Object resultLiteral = null;
        boolean valuesAnArray = isValuesAnArray(paramType);

        if (valuesAnArray) {
            paramType = paramType.getAggregateInfo().getComponentType(paramType);
        }

        valuesTable = OffSetGridTableHelper.make1ColumnTable(valuesTable);

        if (!valuesAnArray) {
            resultLiteral = RuleRowHelper.loadSingleParam(paramType,
                field == null ? RuleRowHelper.CONSTRUCTOR : field.getName(),
                null,
                valuesTable,
                ota);
        }

        return resultLiteral;
    }

    public String getName() {
        return field == null ? "this" : field.getName();
    }

    public IOpenClass getType() {
        return field.getType();
    }

    public synchronized Map<String, Integer> getUniqueIndex(ITable table, int idx) throws SyntaxNodeException {
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
     * @throws SyntaxNodeException 
     */
    public void populateLiteral(Object literal, IGridTable valuesTable, OpenlToolAdaptor toolAdapter) throws SyntaxNodeException {

        IOpenClass paramType = field.getType();
        
        if (valuesAnArray) {
            paramType = paramType.getAggregateInfo().getComponentType(paramType);
        }

        valuesTable = OffSetGridTableHelper.make1ColumnTable(valuesTable);

        if (!valuesAnArray) {
            Object res = RuleRowHelper.loadSingleParam(paramType, field.getName(), null, valuesTable, toolAdapter);

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

    private Object getArrayValues(IGridTable valuesTable, OpenlToolAdaptor ota, IOpenClass paramType) throws SyntaxNodeException {

        if (valuesTable.getGridHeight() == 1 && valuesTable.getGridWidth() == 1) {
            return loadSingleRowArray(valuesTable, ota, paramType);
        }

        if (valuesTable.getGridHeight() != 1) {
            valuesTable.transpose();
            return loadMultiRowArray(valuesTable, ota, paramType);
        }

        return loadMultiRowArray(valuesTable, ota, paramType);
    }

    private Object loadSingleRowArray(IGridTable logicalTable, OpenlToolAdaptor openlAdaptor, IOpenClass paramType) throws SyntaxNodeException {
        return getValuesArrayCommaSeparated(logicalTable, openlAdaptor, paramType);
    }

    private Object loadMultiRowArray(IGridTable logicalTable, OpenlToolAdaptor openlAdaptor, IOpenClass paramType) throws SyntaxNodeException {

        int valuesTableHeight = logicalTable.getGridHeight();
        ArrayList<Object> values = new ArrayList<Object>(valuesTableHeight);

        for (int i = 0; i < valuesTableHeight; i++) {

            Object res = RuleRowHelper.loadSingleParam(paramType,
                field.getName(),
                null,
                logicalTable.getRow(i),
                openlAdaptor);

//            if (res == null) {
//                res = paramType.nullObject();
//            }
            if (res != null) {
                values.add(res);
            }
        }

        Object arrayValues = paramType.getAggregateInfo().makeIndexedAggregate(paramType, new int[] { values.size() });

        for (int i = 0; i < values.size(); i++) {
            Array.set(arrayValues, i, values.get(i));
        }

        return arrayValues;
    }

    private Object getValuesArrayCommaSeparated(IGridTable valuesTable, OpenlToolAdaptor ota, IOpenClass paramType) throws SyntaxNodeException {
        return RuleRowHelper.loadCommaSeparatedParam(paramType,
            field.getName(),
            null,
            valuesTable.getRow(0),
            ota);
    }

}
