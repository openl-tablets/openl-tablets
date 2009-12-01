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

public class ForeignKeyColumnDescriptor extends OpenlBasedColumnDescriptor {
    
    private static final String NOT_INITIALIZED = "<not_initialized>";
    private IdentifierNode foreignKeyTable;
    private IdentifierNode foreignKey;    
    
    
    public ForeignKeyColumnDescriptor(IOpenField field, IdentifierNode foreignKeyTable, IdentifierNode foreignKey,
            StringValue displayValue, OpenL openl) {
        super(field, displayValue, openl);        
        this.foreignKeyTable = foreignKeyTable;
        this.foreignKey = foreignKey;        
    }
    
    public Object getLiteralByForeignKey(IOpenClass fieldType, ILogicalTable values, IDataBase db, IBindingContext cxt)
            throws Exception {
        String tablename = foreignKeyTable.getIdentifier();
        ITable t = db.getTable(tablename);
        if (t == null) {
            BoundError err = new BoundError(foreignKeyTable, "Table " + tablename + " not found", null);
            throw err;
        }

        int foreignKeyIndex = 0;
        String columnName = NOT_INITIALIZED;
        if (foreignKey != null) {
            columnName = foreignKey.getIdentifier();

            foreignKeyIndex = t.getColumnIndex(columnName);
        }

        if (foreignKeyIndex == -1) {
            BoundError err = new BoundError(foreignKey, "Column " + columnName + " not found", null);
            throw err;
        }

        boolean valuesAnArray = isValuesAnArray(fieldType);

        Object res = null;

        if (!valuesAnArray) {
            String s = values.getGridTable().getCell(0, 0).getStringValue();
            if (s != null) {

                s = s.trim();
                if (s.length() > 0) {
                    res = t.findObject(foreignKeyIndex, s, cxt);
                    if (res == null) {
                        throw new BoundError(null, "Index Key " + s + " not found", null, new GridCellSourceCodeModule(
                                values.getGridTable()));
                    }
                    return res;
                }
                return null;
            }
        } else {
            List<Object> v = new ArrayList<Object>();
            for (int i = 0; i < values.getLogicalHeight(); i++) {
                String s = values.getGridTable().getCell(0, i).getStringValue();

                if (s != null) {
                    s = s.trim();
                } else {
                    break;
                }

                if (s.length() == 0) {
                    break;
                }

                Object res1 = t.findObject(foreignKeyIndex, s, cxt);
                if (res1 == null) {
                    throw new BoundError(null, "Index Key " + s + " not found", null, new GridCellSourceCodeModule(
                            values.getLogicalRow(i).getGridTable()));
                }
                v.add(res1);
            }
            
            IOpenClass componentType = fieldType.getAggregateInfo().getComponentType(fieldType);
            Object ary = fieldType.getAggregateInfo().makeIndexedAggregate(componentType, new int[] { v.size() });

            for (int i = 0; i < v.size(); i++) {
                Array.set(ary, i, v.get(i));
            }

            res = ary;
        }

        return res;
    }
    
    public boolean isReference() {        
        return foreignKeyTable != null;
    }
    
    /**
     * Method is using to load data from foreign table, using foreign key (see {@link DataNodeBinder#getForeignKeyTokens()}).
     * Is used when data table is represents as not a constructor (see {@link #isConstructor()}).
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

            valuesTable = ALogicalTable.make1ColumnTable(valuesTable);

            IOpenClass fieldType = getField().getType();

            boolean valueAnArray = isValuesAnArray(fieldType);
            boolean isList = List.class.isAssignableFrom(fieldType
                    .getInstanceClass());

            if (!valueAnArray && !isList) {
                String s = valuesTable.getGridTable().getCell(0, 0).getStringValue();
                if (s != null) {
                    s = s.trim();
                }                
                if (s == null || s.length() == 0) {
                    //Set meta info for empty cells
                    FunctionalRow.setCellMetaInfo(valuesTable, getField().getName(), domainClass);
                } else {
                    if (s.length() > 0) {
                        Object res = null;
                        FunctionalRow.setCellMetaInfo(valuesTable, getField().getName(), domainClass);
                        try {
                            res = foreignTable.findObject(foreignKeyIndex, s, cxt);
                        } catch (Exception ex) {
                            throw new BoundError(null, "Index Key " + s
                                    + " not found", ex,
                                    new GridCellSourceCodeModule(valuesTable
                                            .getGridTable()));
                        }
                        getField().set(target, res, getRuntimeEnv());
                    }
                }
            } else {
                // processing array or list values.
                ArrayList<Object> values = getArrayValuesByForeignKey(
                        valuesTable, cxt, foreignTable, foreignKeyIndex,
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
    
    /**
     * Goes through the values as foreign keys, finds all info about this objects in foreign table and puts it
     * to array. 
     * @param valuesTable Logical table representing array values for current table.
     * @param cxt
     * @param foreignTable Foreign table with stored info about dependent values.
     * @param foreignKeyIndex 
     * @param domainClass
     * @return
     * @throws BoundError
     * TODO: Add support of comma-separated arrays.
     */
    private ArrayList<Object> getArrayValuesByForeignKey(ILogicalTable valuesTable, IBindingContext cxt,
            ITable foreignTable, int foreignKeyIndex, DomainOpenClass domainClass) throws BoundError {
        int valuesHeight = valuesTable.getLogicalHeight();
        ArrayList<Object> values = new ArrayList<Object>(valuesHeight);                
        for (int i = 0; i < valuesHeight; i++) {
            String s = valuesTable.getGridTable().getCell(0, i).getStringValue();

            if (s != null) {
                s = s.trim();
            }

            if (s == null || s.length() == 0) {
                // set meta info for empty cells.
                FunctionalRow.setCellMetaInfo(valuesTable, getField().getName(), domainClass);
                values.add(null);                        
                continue;
            }
            Object res = null;
            FunctionalRow.setCellMetaInfo(valuesTable, getField().getName(), domainClass);
            try {                        
                res = foreignTable.findObject(foreignKeyIndex, s, cxt);
            } catch (Exception ex) {
                throw new BoundError(null, "Index Key " + s
                        + " not found", ex,
                        new GridCellSourceCodeModule(valuesTable
                                .getLogicalRow(i).getGridTable()));
            }                                       
            values.add(res);
        }
        return values;
    }

}
