package org.openl.rules.data;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.domain.EnumDomain;
import org.openl.meta.StringValue;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.util.StringTool;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

/**
 * Handles column descriptors that are represented as foreign keys to data from
 * other tables.
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

    private ICell foreignKeyCell;
    
    public static final String ARRAY_ACCESS_PATTERN = ".+\\[[0-9]+\\]$";

    public ForeignKeyColumnDescriptor(IOpenField field,
            IdentifierNode foreignKeyTable,
            IdentifierNode foreignKey,
            IdentifierNode[] foreignKeyTableAccessorChainTokens,
            ICell foreignKeyCell,
            StringValue displayValue,
            OpenL openl, boolean constructor, IdentifierNode[] fieldChainTokens) {

        super(field, displayValue, openl, constructor, fieldChainTokens);

        this.foreignKeyTable = foreignKeyTable;
        this.foreignKey = foreignKey;
        this.foreignKeyTableAccessorChainTokens = foreignKeyTableAccessorChainTokens;
        this.foreignKeyCell = foreignKeyCell;
    }

    /**
     * Gets the value as <code>String</code> from the cell. If there is no
     * value, returns <code>NULL</code>.
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
     * presented as {@link RuleRowHelper#ARRAY_ELEMENTS_SEPARATOR} array.
     * 
     * @param valuesTable Logical table representing array values for current
     *            table.
     * @param bindingContext binding context
     * @param foreignTable Foreign table with stored info about dependent
     *            values.
     * @param foreignKeyIndex index of the foreign key column
     * @param domainClass domain class for the column values
     * @return foreign key values
     */
    private ArrayList<Object> getArrayValuesByForeignKey(ILogicalTable valuesTable,
            IBindingContext bindingContext,
            ITable foreignTable,
            int foreignKeyIndex,
            IdentifierNode[] foreignKeyTableAccessorChainTokens,
            DomainOpenClass domainClass) throws SyntaxNodeException {

        int valuesHeight = valuesTable.getHeight();

        ArrayList<Object> values = new ArrayList<Object>(valuesHeight);

        if (valuesHeight == 1) {

            if(!bindingContext.isExecutionMode())
                RuleRowHelper.setCellMetaInfo(valuesTable, getField().getName(), domainClass, true);

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
                    // set meta info for empty cells.
                    if(!bindingContext.isExecutionMode())
                        RuleRowHelper.setCellMetaInfo(valueTable, getField().getName(), domainClass, false);
                    values.add(null);
                    continue;
                }

                if(!bindingContext.isExecutionMode())
                    RuleRowHelper.setCellMetaInfo(valueTable, getField().getName(), domainClass, false);
                Object res = getValueByForeignKeyIndex(bindingContext, foreignTable, foreignKeyIndex, foreignKeyTableAccessorChainTokens, valueTable, value);

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
     * Tries to find value by its key in foreign table. If no, throws an
     * exception.
     */
    private Object getValueByForeignKeyIndex(IBindingContext bindingContext,
            ITable foreignTable,
            int foreignKeyIndex,
            IdentifierNode[] foreignKeyTableAccessorChainTokens,
            ILogicalTable valueTable,
            String key) throws SyntaxNodeException {

        Object result = null;

        try {
            if (foreignKeyColumnChainTokens.length == 0) {
                foreignKeyColumnChainTokens = ArrayUtils.add(foreignKeyColumnChainTokens, foreignTable.getColumnName(foreignKeyIndex));
                ColumnDescriptor foreignColumnDescriptor = foreignTable.getDataModel().getDescriptor()[foreignKeyIndex];
                if (foreignColumnDescriptor.isReference() && foreignColumnDescriptor instanceof ForeignKeyColumnDescriptor) {
                    // In the case when foreign key is like: ">policies.driver"
                    String[] endOfChain = ((ForeignKeyColumnDescriptor) foreignColumnDescriptor).foreignKeyColumnChainTokens;
                    foreignKeyColumnChainTokens = ArrayUtils.addAll(foreignKeyColumnChainTokens, endOfChain);
                }
            }
            result = foreignTable.findObject(foreignKeyIndex, key, bindingContext);
            if (result == null) {
                throwIndexNotFound(foreignTable, valueTable, key, null, bindingContext);
            }

            if (!ArrayUtils.isEmpty(foreignKeyTableAccessorChainTokens)) {
                ResultChainObject chainRes = getChainObject(result, foreignKeyTableAccessorChainTokens);
                result = chainRes.getValue();
            }

        } catch (SyntaxNodeException ex) {
            throwIndexNotFound(foreignTable, valueTable, key, ex, bindingContext);
        }
       
        return result;
    }

    private void throwIndexNotFound(ITable foreignTable, ILogicalTable valuesTable, String src, Exception ex, IBindingContext bindingContext) throws SyntaxNodeException {

        String message = String.format("Index Key %s is not found in the foreign table %s", src, foreignTable.getName());

        throw SyntaxNodeExceptionUtils.createError(message,
            ex,
            null,
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

        if (foreignTable == null) {
            String message = String.format("Table '%s' is not found", foreignKeyTableName);
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
            String message = "Column '" + columnName + "' is not found";
            throw SyntaxNodeExceptionUtils.createError(message, null, foreignKey);
        }

        boolean valuesAnArray = isValuesAnArray(fieldType);

        if (!valuesAnArray) {

            String value = getCellStringValue(valuesTable);

            if (value != null && value.length() > 0) {
                result = getValueByForeignKeyIndex(bindingContext, foreignTable, foreignKeyIndex, foreignKeyTableAccessorChainTokens, valuesTable, value);
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

                Object res = getValueByForeignKeyIndex(bindingContext, foreignTable, foreignKeyIndex, foreignKeyTableAccessorChainTokens, valueTable, value);
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
    public boolean isReference() {
        return foreignKeyTable != null;
    }

    /**
     * Method is using to load data from foreign table, using foreign key (see
     * {@link DataTableBindHelper#getForeignKeyTokens(IBindingContext, ILogicalTable, int)}). Is used when data table is
     * represents as <b>NOT</b> a constructor (see {@link #isConstructor()}).
     */
    public void populateLiteralByForeignKey(Object target, ILogicalTable valuesTable, IDataBase db, IBindingContext cxt)
        throws Exception {
        if (getField() != null) {

            if (foreignKeyTable != null) {

                String foreignKeyTableName = foreignKeyTable.getIdentifier();
                ITable foreignTable = db.getTable(foreignKeyTableName);
                //foreignTable.findObject(columnIndex, key, bindingContext)

                if (foreignTable == null) {
                    String message = String.format("Table '%s' is not found", foreignKeyTableName);
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
                    String message = "Column '" + columnName + "' is not found";
                    throw SyntaxNodeExceptionUtils.createError(message, null, foreignKey);
                }

                final Map<String, Integer> index = foreignTable.getFormattedUniqueIndex(foreignKeyIndex);
                Set<String> strings = index.keySet();
                String[] domainStrings = strings.toArray(new String[strings.size()]);
                Arrays.sort(domainStrings, new Comparator<String>() {
                    @Override
                    public int compare(String ds1, String ds2) {
                        return index.get(ds1).compareTo(index.get(ds2));
                    }
                });

                IOpenClass columnType = foreignTable.getColumnType(foreignKeyIndex);
                EnumDomain<String> domain = new EnumDomain<String>(domainStrings);
                DomainOpenClass domainClass = new DomainOpenClass(getField().getName(),
                    columnType != null ? columnType : JavaOpenClass.STRING,
                    domain,
                    null);

                // table will have 1xN size
                //
                valuesTable = LogicalTableHelper.make1ColumnTable(valuesTable);

                IOpenClass fieldType = getField().getType();

                boolean valueAnArray = isValuesAnArray(fieldType);
                IOpenClass resType = foreignTable.getDataModel().getType();
                String s = getCellStringValue(valuesTable);
                if (!StringUtils.isEmpty(s)) {
                    try {
                        Object result = foreignTable.findObject(foreignKeyIndex, s, cxt);
                        if (result != null) {
                            ResultChainObject chainRes = getChainObject(result, foreignKeyTableAccessorChainTokens);
                            Class<?> instanceClass = chainRes.instanceClass;
                            int dim = 0;
                            while (instanceClass.isArray()) {
                                instanceClass = instanceClass.getComponentType();
                                dim++;
                            }
                            resType = cxt.findType(ISyntaxConstants.THIS_NAMESPACE, instanceClass.getSimpleName());
                            if (dim > 0) {
                                resType = resType.getArrayType(dim);
                            }
                        }
                    } catch (SyntaxNodeException ex) {
                        throwIndexNotFound(foreignTable, valuesTable, s, ex, cxt);
                    }
                }
                
                boolean isCollection = Collection.class.isAssignableFrom(fieldType.getInstanceClass());

                boolean f = true;
                if (fieldType.isArray()) {
                    f = !fieldType.getComponentClass().getInstanceClass().equals(resType.getInstanceClass());
                } else if (isCollection) {
                    f = fieldType.getInstanceClass().isAssignableFrom(resType.getInstanceClass());
                }
                

                if (f) {
                    if (StringUtils.isEmpty(s)) {
                        // Set meta info for empty cells
                        if(!cxt.isExecutionMode())
                            RuleRowHelper.setCellMetaInfo(valuesTable, getField().getName(), domainClass, false);
                    } else {
                        if (!cxt.isExecutionMode())
                            RuleRowHelper.setCellMetaInfo(valuesTable, getField().getName(), domainClass, false);
                        Object res = getValueByForeignKeyIndex(cxt,
                            foreignTable,
                            foreignKeyIndex,
                            foreignKeyTableAccessorChainTokens,
                            valuesTable,
                            s);
                        IOpenCast cast = cxt.getCast(resType, fieldType);
                        if (cast == null || !cast.isImplicit()) {
                            String message = String.format(
                                "Incompatible types: Field '%s' has type [%s] that differs from type of foreign table [%s]",
                                getField().getName(),
                                fieldType,
                                resType);
                            throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
                        }
                        getField().set(target, cast.convert(res), getRuntimeEnv());
                    }
                } else {
                    
                    // processing array or list values.
                    List<Object> cellValues = getArrayValuesByForeignKey(valuesTable, cxt, foreignTable,
                        foreignKeyIndex,
                        foreignKeyTableAccessorChainTokens,
                        domainClass);
                    // Cell can contain empty reference value. As a result we
                    // will
                    // receive collection with one null value element. The
                    // following code snippet
                    // searches null value elements and removes them.
                    //

                    List<Object> values = CollectionUtils.findAll(cellValues, new CollectionUtils.Predicate<Object>() {
                        public boolean evaluate(Object arg) {
                            return arg != null;
                        }
                    });

                    int size = values.size();
                    IOpenClass componentType;

                    if (valueAnArray) {
                        componentType = fieldType.getAggregateInfo().getComponentType(fieldType);
                    } else {
                        componentType = JavaOpenClass.OBJECT;
                    }

                    Object v = fieldType.getAggregateInfo().makeIndexedAggregate(componentType, size);

                    // Populate result array with values.
                    //
                    boolean isList = List.class.isAssignableFrom(fieldType.getInstanceClass());
                    boolean isSet = Set.class.isAssignableFrom(fieldType.getInstanceClass());

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
                    getField().set(target, v, getRuntimeEnv());
                }
            }
        } else {
            /**
             * field == null, in this case don`t do anything. The appropriate information why it is null would have been
             * processed during preparing column descriptor.
             * See {@link DataTableBindHelper#makeDescriptors(IBindingContext bindingContext, ITable table, IOpenClass type,
             * OpenL openl, ILogicalTable descriptorRows, ILogicalTable dataWithTitleRows, boolean hasForeignKeysRow,
             * boolean hasColumnTytleRow)}
             */
        }
    }
    
    private static int getArrayIndex(IdentifierNode fieldNameNode) {
        String fieldName = fieldNameNode.getIdentifier();
        String txtIndex = fieldName.substring(fieldName.indexOf("[") + 1, fieldName.indexOf("]"));

        return Integer.parseInt(txtIndex);
    }

    private static String getArrayName(IdentifierNode fieldNameNode) {
        String fieldName = fieldNameNode.getIdentifier();
        return fieldName.substring(0, fieldName.indexOf("["));
    }

    private ResultChainObject getChainObject(Object parentObj, IdentifierNode[] fieldChainTokens) throws SyntaxNodeException {
        Object resObj = parentObj;
        Class<?> resInctClass = parentObj.getClass();

        for (int i = 1; i < fieldChainTokens.length; i++) {
            IdentifierNode token = fieldChainTokens[i];
            if (resObj == null) {
                String message = String.format("Incorrect field '%s' in type [%s]",
                        token.getIdentifier(),
                        resInctClass);
                throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
            }
            
            boolean arrayAccess = token.getIdentifier().matches(ARRAY_ACCESS_PATTERN);

            Object prevResObj = resObj;
            try {
                Method method;
                if (arrayAccess){
                    method = resObj.getClass().getMethod(StringTool.getGetterName(getArrayName(token)));
                }else{
                    method = resObj.getClass().getMethod(StringTool.getGetterName(token.getIdentifier()));
                }
                resObj = method.invoke(resObj);

                /*Get null object information from method description*/
                if (resObj == null) {
                    if (arrayAccess){
                        String message = String.format("Incorrect array access in field '%s' of type [%s]",
                            token.getIdentifier(),
                            prevResObj.getClass());
                        throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
                    }else{
                        resInctClass = method.getReturnType();
                    }
                } else {
                    if (arrayAccess){
                        int arrayIndex = getArrayIndex(token);
                        resObj = Array.get(resObj, arrayIndex);
                    }
                    resInctClass = resObj.getClass();
                }
            } catch (SyntaxNodeException e) {
                throw e;
            } catch (Exception e) {
                String message = String.format("Incorrect field '%s' in type [%s]",
                        token.getIdentifier(),
                        resObj == null ? prevResObj.getClass() : resObj.getClass());
                throw SyntaxNodeExceptionUtils.createError(message, null, foreignKeyTable);
            }
        }

        return new ResultChainObject(resObj, resInctClass);
    }

    public void setForeignKeyCellMetaInfo(IDataBase db) {
        if (foreignKeyCell != null) {
            ITable foreignTable = db.getTable(foreignKeyTable.getIdentifier());
            if (foreignTable != null) {
                ILocation location = foreignKeyTable.getLocation();
                NodeUsage nodeUsage = new SimpleNodeUsage(
                    location.getStart().getAbsolutePosition(new TextInfo(foreignTable.getName())),
                    location.getEnd().getAbsolutePosition(new TextInfo(foreignTable.getName())) - 1,
                    foreignTable.getTableSyntaxNode().getHeaderLineValue().getValue(),
                    foreignTable.getTableSyntaxNode().getUri(),
                    NodeType.DATA);
                CellMetaInfo meta = new CellMetaInfo(CellMetaInfo.Type.DT_CA_CODE, null, JavaOpenClass.STRING, false, Collections.singletonList(nodeUsage));
                foreignKeyCell.setMetaInfo(meta);
            }
        }

        // Not needed anymore
        foreignKeyCell = null;
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