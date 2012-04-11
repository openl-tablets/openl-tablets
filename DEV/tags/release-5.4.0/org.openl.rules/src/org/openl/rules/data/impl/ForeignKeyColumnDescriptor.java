package org.openl.rules.data.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BoundError;
import org.openl.domain.EnumDomain;
import org.openl.meta.StringValue;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.ITable;
import org.openl.rules.data.binding.DataNodeBinder;
import org.openl.rules.dt.FunctionalRow;
import org.openl.rules.table.ALogicalTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Handles column descriptors that are represented as foreign keys to data from other tables.  
 * @author DLiauchuk
 *
 */
public class ForeignKeyColumnDescriptor extends ColumnDescriptor {
    
    private static final String NOT_INITIALIZED = "<not_initialized>";
    private IdentifierNode foreignKeyTable;
    private IdentifierNode foreignKey;    
    
    /**
     * Gets the value as <code>String</code> from the cell. If there is no value, returns <code>NULL</code>.
     * @param cellTable
     * @return
     */
    private String getCellStringValue(ILogicalTable cellTable) {
        String s = null;
        s = cellTable.getGridTable().getCell(0, 0).getStringValue();
        if (s != null) {
            s = s.trim();
        }
        return s;
    }
    
    /**
     * Goes through the values as foreign keys, finds all info about this objects in foreign table and puts it
     * to array. Can process array value presented as {@link FunctionalRow#ARRAY_ELEMENTS_SEPARATOR} array.
     * @param valuesTable Logical table representing array values for current table.
     * @param cxt
     * @param foreignTable Foreign table with stored info about dependent values.
     * @param foreignKeyIndex 
     * @param domainClass
     * @return
     * @throws BoundError     
     */
    private ArrayList<Object> getArrayValuesByForeignKey(ILogicalTable valuesTable, IBindingContext cxt,
            ITable foreignTable, int foreignKeyIndex, DomainOpenClass domainClass) throws BoundError {        
        int valuesHeight = valuesTable.getLogicalHeight();        
        
        ArrayList<Object> values = new ArrayList<Object>(valuesHeight);        
        
        boolean multiValue = false;
        if (valuesHeight == 1 && FunctionalRow.isCommaSeparatedArray(valuesTable)) {
            multiValue = true;
            FunctionalRow.setCellMetaInfo(valuesTable, getField().getName(), domainClass, multiValue);
            // load array of values as comma separated parameters
            String[] tokens = FunctionalRow.extractElementsFromCommaSeparatedArray(valuesTable);
            if (tokens != null) {
                for (String token : tokens) {
                    Object res = getValueByForeignKeyIndex(cxt, foreignTable, foreignKeyIndex, valuesTable, token);                                                           
                    values.add(res);
                }
            }            
        } else {
            for (int i = 0; i < valuesHeight; i++) {
                //we take the appropriate cell for the current value. 
                ILogicalTable valueTable = valuesTable.getLogicalRow(i);
                String s = getCellStringValue(valueTable);                
                if (s == null || s.length() == 0) {
                    // set meta info for empty cells.
                    FunctionalRow.setCellMetaInfo(valueTable, getField().getName(), domainClass, multiValue);
                    values.add(null);                        
                    continue;
                }    
                FunctionalRow.setCellMetaInfo(valueTable, getField().getName(), domainClass, multiValue);
                Object res = getValueByForeignKeyIndex(cxt, foreignTable, foreignKeyIndex, valueTable, s);                                       
                values.add(res);
            }
        }        
        return values;
    }
    
    /**
     * Tries to find value by its key in foreign table. If no, throws an exception.
     * @param cxt
     * @param foreignTable
     * @param foreignKeyIndex
     * @param valueTable
     * @param s
     * @return
     * @throws BoundError
     */
    private Object getValueByForeignKeyIndex(IBindingContext cxt, ITable foreignTable, int foreignKeyIndex, ILogicalTable valueTable,
            String s) throws BoundError {        
        Object res = null;
        try {                        
            res = foreignTable.findObject(foreignKeyIndex, s, cxt);            
        } catch (BoundError ex) {
            throwIndexNotFound(valueTable, s, ex);                    
        }
        if (res == null) {
            throwIndexNotFound(valueTable, s, null);
        }
        return res;
    }

    private void throwIndexNotFound(ILogicalTable valuesTable, String src, Exception ex) throws BoundError {
        throw new BoundError(null, String.format("Index Key %s not found", src), ex,
                new GridCellSourceCodeModule(valuesTable.getGridTable()));
    }
    
    public ForeignKeyColumnDescriptor(IOpenField field, IdentifierNode foreignKeyTable, IdentifierNode foreignKey,
            StringValue displayValue, OpenL openl) {
        super(field, displayValue, openl);        
        this.foreignKeyTable = foreignKeyTable;
        this.foreignKey = foreignKey;        
    }
    
    /**
     * Method is using to load data from foreign table, using foreign key (see {@link DataNodeBinder#getForeignKeyTokens()}).
     * Is used when data table is represents <b>AS</b> a constructor (see {@link #isConstructor()}).
     */
    public Object getLiteralByForeignKey(IOpenClass fieldType, ILogicalTable valuesTable, IDataBase db, IBindingContext cxt)
            throws Exception {
        Object result = null;
        String foreignKeyTableName = foreignKeyTable.getIdentifier();
        ITable foreignTable = db.getTable(foreignKeyTableName);
        if (foreignTable == null) {
            BoundError err = new BoundError(foreignKeyTable, "Table " + foreignKeyTableName + " not found", null);
            throw err;
        }

        int foreignKeyIndex = 0;
        String columnName = NOT_INITIALIZED;
        if (foreignKey != null) {
            columnName = foreignKey.getIdentifier();

            foreignKeyIndex = foreignTable.getColumnIndex(columnName);
        }

        if (foreignKeyIndex == -1) {
            BoundError err = new BoundError(foreignKey, "Column " + columnName + " not found", null);
            throw err;
        }

        boolean valuesAnArray = isValuesAnArray(fieldType);
        
        if (!valuesAnArray) {
            String s = getCellStringValue(valuesTable);            
            if (s != null && s.length() > 0) {
                result = getValueByForeignKeyIndex(cxt, foreignTable, foreignKeyIndex, valuesTable, s);               
            }            
        } else {
            List<Object> values = new ArrayList<Object>();
            int valuesHeight = valuesTable.getLogicalHeight();
            for (int i = 0; i < valuesHeight; i++) {
                ILogicalTable valueTable = valuesTable.getLogicalRow(i);
                String s = getCellStringValue(valueTable);
                if (s == null || s.length() == 0) {
                    break;
                }
                Object res = getValueByForeignKeyIndex(cxt, foreignTable, foreignKeyIndex, valueTable, s);                
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
     * Method is using to load data from foreign table, using foreign key (see {@link DataNodeBinder#getForeignKeyTokens()}).
     * Is used when data table is represents as <b>NOT</b> a constructor (see {@link #isConstructor()}).
     */
    public void populateLiteralByForeignKey(Object target,
            ILogicalTable valuesTable, IDataBase db, IBindingContext cxt)
            throws Exception {
        if (foreignKeyTable != null) {
            String foreignKeyTableName = foreignKeyTable.getIdentifier();
            ITable foreignTable = db.getTable(foreignKeyTableName);
            if (foreignTable == null) {
                BoundError err = new BoundError(foreignKeyTable, "Table "
                        + foreignKeyTableName + " not found", null);
                throw err;
            }

            int foreignKeyIndex = 0;
            String columnName = NOT_INITIALIZED;
            if (foreignKey != null) {
                columnName = foreignKey.getIdentifier();

                foreignKeyIndex = foreignTable.getColumnIndex(columnName);
            }

            if (foreignKeyIndex == -1) {
                BoundError err = new BoundError(foreignKey, "Column "
                        + columnName + " not found", null);
                throw err;
            }

            Map<String, Integer> index = foreignTable.getUniqueIndex(foreignKeyIndex);

            String[] domainStrings = index.keySet().toArray(new String[0]);

            EnumDomain<String> domain = new EnumDomain<String>(domainStrings);

            DomainOpenClass domainClass = new DomainOpenClass(getField().getName(),
                    JavaOpenClass.STRING, domain, null);
            /**
             * table will have 1xN size
             */
            valuesTable = ALogicalTable.make1ColumnTable(valuesTable);

            IOpenClass fieldType = getField().getType();

            boolean valueAnArray = isValuesAnArray(fieldType);
            boolean isList = List.class.isAssignableFrom(fieldType
                    .getInstanceClass());

            if (!valueAnArray && !isList) {
                String s = getCellStringValue(valuesTable);                
                if (s == null || s.length() == 0) {
                    //Set meta info for empty cells
                    FunctionalRow.setCellMetaInfo(valuesTable, getField().getName(), domainClass, false);
                } else {
                    if (s.length() > 0) {   
                        FunctionalRow.setCellMetaInfo(valuesTable, getField().getName(), domainClass, false);
                        Object res = getValueByForeignKeyIndex(cxt, foreignTable, foreignKeyIndex, valuesTable, s);                        
                        getField().set(target, res, getRuntimeEnv());
                    }
                }
            } else {
                // processing array or list values.
                ArrayList<Object> values = getArrayValuesByForeignKey(valuesTable, cxt, foreignTable, foreignKeyIndex,
                        domainClass);
                int size = values.size();
                IOpenClass componentType = valueAnArray 
                        ? fieldType.getAggregateInfo().getComponentType(fieldType)
                        : JavaOpenClass.OBJECT;
                Object ary = fieldType.getAggregateInfo().makeIndexedAggregate(
                        componentType, new int[] { size });

                for (int i = 0; i < size; i++) {
                    Array.set(ary, i, values.get(i));
                }

                if (isList) {
                    int len = Array.getLength(ary);
                    List<Object> list = new ArrayList<Object>(len);
                    for (int i = 0; i < len; i++) {
                        list.add(Array.get(ary, i));
                    }
                    getField().set(target, list, getRuntimeEnv());
                } else {
                    getField().set(target, ary, getRuntimeEnv());
                }
            }
        }        
    }   
}
