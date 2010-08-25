/*
 * Created on Mar 8, 2004 Developed by OpenRules Inc. 2003-2004
 */

package org.openl.rules.datatype.binding;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.DatatypeOpenField;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.InternalDatatypeClass;
import org.openl.rules.lang.xls.types.DatatypeOpenClass.OpenFieldsConstructor;


/**
 * Bound node for datatype table component.
 * 
 * @author snshor
 * 
 */
public class DatatypeTableMethodBoundNode implements IMemberBoundNode {

    private TableSyntaxNode tableSyntaxNode;
    private DatatypeOpenClass dataType;
    private String parentClassName;

    private ILogicalTable table;
    private OpenL openl;

    public DatatypeTableMethodBoundNode(TableSyntaxNode tableSyntaxNode, DatatypeOpenClass datatype,
            ILogicalTable table, OpenL openl) {
        this(tableSyntaxNode,datatype, table, openl, null);
    }
    
    public DatatypeTableMethodBoundNode(TableSyntaxNode tableSyntaxNode, DatatypeOpenClass datatype,
            ILogicalTable table, OpenL openl, String parentClassName) {
        this.tableSyntaxNode = tableSyntaxNode;
        this.dataType = datatype;
        this.table = table;
        this.openl = openl;
        this.parentClassName = parentClassName;
    }
    
    /**
     * Process datatype fields from source table.
     * 
     * @param cxt
     * @throws Exception
     */
    private void addFields(IBindingContext cxt) throws Exception {

        ILogicalTable dataTable = DatatypeHelper.getNormalizedDataPartTable(table, openl, cxt);

        int tableHeight = dataTable.getLogicalHeight();
        
        Map<String, FieldType> fields = new LinkedHashMap<String,  FieldType>();

        for (int i = 0; i < tableHeight; i++) {
            ILogicalTable row = dataTable.getLogicalRow(i);
            boolean firstField = false;
            if (i == 0) {
                firstField = true;
            }
            processRow(row, cxt, fields, firstField);            
        }
        checkInheritedFieldsDuplication(cxt);
        Class<?> beanClass = createBeanForDatatype(fields);
        dataType.setInstanceClass(beanClass);
    }
    
    /**
     * Generate a simple java bean for current datatype table.
     * 
     * @param fields fields for bean class
     * @return Class<?> descriptor of generated bean class.
     * @throws SyntaxNodeException is can`t generate bean for datatype table.
     */
    private Class<?> createBeanForDatatype(Map<String, FieldType> fields) throws SyntaxNodeException {
        String datatypeName = dataType.getName();
        String beanName = getDatatypeBeanNameWithNamespace(datatypeName);
        SimpleBeanByteCodeGenerator beanGenerator = new SimpleBeanByteCodeGenerator(beanName, fields, dataType.getSuperClass());
        
        Class<?> beanClass = beanGenerator.generateAndLoadBeanClass(); 
        
        if (beanClass == null) {
            String errorMessage = String.format("Cant generate bean for datatype:", datatypeName);
            throw SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode);
        }
        
        return beanClass;
    }
    
    /**
     * Gets the name for datatype bean with path to it (e.g <code>org.openl.this.Driver</code>)
     * 
     * @param datatypeName name of the datatype (e.g. <code>Driver</code>)
     * @return the name for datatype bean with path to it (e.g <code>org.openl.this.Driver</code>)
     */
    private String getDatatypeBeanNameWithNamespace(String datatypeName) {
        return String.format("%s.%s", ISyntaxConstants.GENERATED_BEANS, datatypeName);        
    }

    private void processRow(ILogicalTable row, IBindingContext cxt, Map<String, FieldType> fields, boolean firstField) 
        throws SyntaxNodeException, OpenLCompilationException {
        
        GridCellSourceCodeModule rowSrc = new GridCellSourceCodeModule(row.getGridTable());
        
        if (canProcessRow(rowSrc)) {
            GridCellSourceCodeModule firstLogicalRowSrc = 
                        new GridCellSourceCodeModule(row.getLogicalColumn(1).getGridTable());

            IdentifierNode[] idn = getIdentifierNode(firstLogicalRowSrc);
            
            String fieldName = idn[0].getIdentifier();
            
            IOpenClass fieldType = getFieldType(cxt, row, rowSrc);
            IOpenField field = new DatatypeOpenField(dataType, fieldName, fieldType);
             
            try {
                dataType.addField(field);
                fields.put(fieldName, new FieldType(field));
                if (firstField) { // this is done for operations like people["john"]
                                   // to access one instance of datatype from array by 
                                    // user defined index. But it`s not implemented yet.
                                    // See DynamicArrayAggregateInfo#getIndex(IOpenClass aggregateType, IOpenClass indexType)
                    dataType.setIndexField(field);
                }
            } catch (Throwable t) {
                String errorMessage = String.format("Can not add field %s: %s", fieldName, t.getMessage());
                throw SyntaxNodeExceptionUtils.createError(errorMessage,
                    null,
                    null,
                    firstLogicalRowSrc);
            }
        }
    }
    
    private IdentifierNode[] getIdentifierNode(GridCellSourceCodeModule firstLogicalRowSrc) 
        throws OpenLCompilationException, SyntaxNodeException {
        
        IdentifierNode[] idn = Tokenizer.tokenize(firstLogicalRowSrc, " \r\n");
        
        if (idn.length != 1) {
            String errorMessage = String.format("Bad field name: %s", firstLogicalRowSrc.getCode());
            throw SyntaxNodeExceptionUtils.createError(errorMessage, null, null, firstLogicalRowSrc);
        }
        return idn;
    }
    
    /**
     * Checks if the given row can be processed. 
     * 
     * @param rowSrc
     * @return false if row content is empty, or was commented with special symbols.
     */
    private boolean canProcessRow(GridCellSourceCodeModule rowSrc) {
        String srcCode = rowSrc.getCode().trim();

        if (srcCode.length() == 0 || srcCode.startsWith("//")) {
            return false;
        }
        return true;
    }

    private IOpenClass getFieldType(IBindingContext cxt, ILogicalTable row, GridCellSourceCodeModule tableSrc) 
        throws SyntaxNodeException {
        
        IOpenClass fieldType = OpenLManager.makeType(openl, tableSrc, (IBindingContextDelegator) cxt);

        if (fieldType == null || fieldType instanceof NullOpenClass) {
            String errorMessage = String.format("Type %s not found", tableSrc.getCode());
            throw SyntaxNodeExceptionUtils.createError(errorMessage, null, null, 
                  tableSrc);
        }

        if (row.getLogicalWidth() < 2) {
            String errorMessage = "Bad table structure: must be {header} / {type | name}";
            throw SyntaxNodeExceptionUtils.createError(errorMessage,
                null,
                null,
                tableSrc);
        }
        return fieldType;
    }    

    public void addTo(ModuleOpenClass openClass) {
        InternalDatatypeClass internalClassMember = new InternalDatatypeClass(dataType, openClass);
        tableSyntaxNode.setMember(internalClassMember);
    }

    public void finalizeBind(IBindingContext cxt) throws Exception {
        if(parentClassName != null){
            IOpenClass parentClass = cxt.findType(ISyntaxConstants.THIS_NAMESPACE, parentClassName);
            if (parentClass == null) {
                throw new OpenLCompilationException(String.format("Parent class [%s] is not defined", parentClassName));
            }
            if (parentClass instanceof DomainOpenClass) {
                throw new OpenLCompilationException(String.format("Parent class [%s] cannot be domain type", parentClassName));
            }
            
            dataType.setSuperClass(parentClass);
        }
        addFields(cxt);
        //adding constructor with all fields after all fields have been added
        dataType.addMethod(new OpenFieldsConstructor(dataType));
    }
    
    private void checkInheritedFieldsDuplication(IBindingContext cxt) throws Exception {
        IOpenClass superClass = dataType.getSuperClass();
        if (superClass != null) {
            for (Entry<String, IOpenField> field : dataType.getDeclaredFields().entrySet()) {
                IOpenField fieldInParent = superClass.getField(field.getKey());
                if(fieldInParent != null){
                    if(fieldInParent.getType().getInstanceClass().equals(field.getValue().getType().getInstanceClass())){
                        BindHelper.processWarn(String.format("Field [%s] has been already defined in class \"%s\"",
                                field.getKey(), fieldInParent.getDeclaringClass().getDisplayName(0)), tableSyntaxNode);
                    }else{
                        throw new SyntaxNodeException(String.format(
                                "Field [%s] has been already defined in class \"%s\" with another type",
                                field.getKey(), fieldInParent.getDeclaringClass().getDisplayName(0)), null,
                                tableSyntaxNode);
                    }
                }
            }
        }
    }

}
