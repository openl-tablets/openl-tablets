/*
 * Created on Mar 8, 2004 Developed by OpenRules Inc. 2003-2004
 */

package org.openl.rules.datatype.binding;
import java.lang.reflect.Modifier;
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
import org.openl.rules.datatype.gen.ByteCodeGeneratorHelper;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.datatype.gen.SimpleBeanByteCodeGenerator;
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
public class DatatypeTableBoundNode implements IMemberBoundNode {

    private TableSyntaxNode tableSyntaxNode;
    private DatatypeOpenClass dataType;
    private String parentClassName;
    private ModuleOpenClass moduleOpenClass;

    private ILogicalTable table;
    private OpenL openl;

    public DatatypeTableBoundNode(TableSyntaxNode tableSyntaxNode, DatatypeOpenClass datatype,
    		ModuleOpenClass moduleOpenClass, ILogicalTable table, OpenL openl) {
        this(tableSyntaxNode,datatype, moduleOpenClass, table, openl, null);
    }
    
    public DatatypeTableBoundNode(TableSyntaxNode tableSyntaxNode, DatatypeOpenClass datatype,
    		ModuleOpenClass moduleOpenClass, ILogicalTable table, OpenL openl, String parentClassName) {
        this.tableSyntaxNode = tableSyntaxNode;
        this.dataType = datatype;
        this.table = table;
        this.openl = openl;
        this.parentClassName = parentClassName;
        this.moduleOpenClass = moduleOpenClass;
    }
    
    /**
     * Process datatype fields from source table.
     * 
     * @param cxt
     * @throws Exception
     */
    private void addFields(IBindingContext cxt) throws Exception {

        ILogicalTable dataTable = DatatypeHelper.getNormalizedDataPartTable(table, openl, cxt);

        int tableHeight = 0;
        
        if (dataTable != null) {
            tableHeight = dataTable.getHeight();
        }
        
        // map of fields that will be used for byte code generation.
        // key: name of the field, value: field type.
        //
        Map<String, FieldDescription> fields = new LinkedHashMap<String,  FieldDescription>();

        for (int i = 0; i < tableHeight; i++) {
            ILogicalTable row = dataTable.getRow(i);
            boolean firstField = false;
            if (i == 0) {
                firstField = true;
            }
            processRow(row, cxt, fields, firstField);            
        }
        checkInheritedFieldsDuplication(cxt);
        
        if (beanClassCanBeGenerated(cxt)) {
            Class<?> beanClass = createBeanForDatatype(fields);
            dataType.setInstanceClass(beanClass);
        }
    }
    
    private boolean beanClassCanBeGenerated(IBindingContext cxt) {
        if (tableSyntaxNode.hasErrors()) {
            return false;
        }
        if (parentClassName != null) {
            IOpenClass parentClass = cxt.findType(ISyntaxConstants.THIS_NAMESPACE, parentClassName);
            if (parentClass.getInstanceClass() == null) {
                return false;// parent bean was not generated
            }
        }
        return true;
    }
    
    /**
     * Generate a simple java bean for current datatype table.
     * 
     * @param fields fields for bean class
     * @return Class<?> descriptor of generated bean class.
     * @throws SyntaxNodeException is can`t generate bean for datatype table.
     */
    private Class<?> createBeanForDatatype(Map<String, FieldDescription> fields) throws SyntaxNodeException {
        String datatypeName = dataType.getName();
        String beanName = getDatatypeBeanNameWithNamespace(datatypeName);
        IOpenClass superClass = dataType.getSuperClass();
        SimpleBeanByteCodeGenerator beanGenerator;
        if (superClass != null) {
            Map<String, FieldDescription> parentFields = ByteCodeGeneratorHelper.convertFields(superClass.getFields());
            beanGenerator = new SimpleBeanByteCodeGenerator(beanName, fields, superClass.getInstanceClass(), parentFields);
        } else {
            beanGenerator = new SimpleBeanByteCodeGenerator(beanName, fields);
        }
        
        Class<?> beanClass = beanGenerator.generateAndLoadBeanClass(); 
        
        if (beanClass == null) {
            String errorMessage = String.format("Cant generate bean for datatype '%s'", datatypeName);
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
        return String.format("%s.%s", tableSyntaxNode.getTableProperties().getPropertyValue("datatypePackage"), datatypeName);        
    }

    private void processRow(ILogicalTable row, IBindingContext cxt, Map<String, FieldDescription> fields, boolean firstField) 
        throws SyntaxNodeException, OpenLCompilationException {
        
        if (row.getWidth() > 2) {
            processWithAdditionalColumns(row, cxt, fields, firstField);
        } else {
            processSimple(row, cxt, fields, firstField);
        }
    }
    
    /**
     * Process datatype table row, that contains more than 2 columns.<br>
     * The 3rd column is a default value one.
     * 
     */
    private void processWithAdditionalColumns(ILogicalTable row, IBindingContext cxt, Map<String, FieldDescription> fields,
            boolean firstField) throws OpenLCompilationException {
        GridCellSourceCodeModule rowSrc = new GridCellSourceCodeModule(row.getSource(), cxt);

        if (canProcessRow(rowSrc)) {
            String fieldName = getName(row, cxt);

            IOpenClass fieldType = getFieldType(cxt, row, rowSrc);
            IOpenField field = new DatatypeOpenField(dataType, fieldName, fieldType);

            String defaultValue = getDefaultValue(row, cxt);
            try {
                dataType.addField(field);
                fields.put(fieldName, new FieldDescription(field, defaultValue));

                if (firstField) {
                    processFirstField(field);
                }
            } catch (Throwable t) {
                cannottAddField(row, cxt, fieldName, t);
            }
        }
    }
    
    /**
     * Process datatype table row, that contains only 2 columns.<br>
     * The 1st column is a type definition. The 2nd one is a field name.
     */
    private void processSimple(ILogicalTable row, IBindingContext cxt, Map<String, FieldDescription> fields, boolean firstField) 
        throws OpenLCompilationException {
        GridCellSourceCodeModule rowSrc = new GridCellSourceCodeModule(row.getSource(), cxt);
        if (canProcessRow(rowSrc)) {
            String fieldName = getName(row, cxt);
            
            IOpenClass fieldType = getFieldType(cxt, row, rowSrc);
            IOpenField field = new DatatypeOpenField(dataType, fieldName, fieldType);
            try {
                dataType.addField(field);
                fields.put(fieldName, new FieldDescription(field));
                
                if (firstField) {   
                    processFirstField(field);
                }
            } catch (Throwable t) {
                cannottAddField(row, cxt, fieldName, t);
            }
        }
    }

    private void processFirstField(IOpenField field) {
        // This is done for operations like people["john"] in OpenL
        // rules to access one instance of datatype from array by
        // user defined index.
        // If first field type of Datatype is int, for calling the instance, wrap it
        // with quotes, e.g. vehicle["23"].
        // Calling the instance like: drivers[7], you will get the 8 element of array.
        //
        // See DynamicArrayAggregateInfo#getIndex(IOpenClass aggregateType, IOpenClass indexType)
        // and DatatypeArrayTest
        dataType.setIndexField(field);
    }    

    private void cannottAddField(ILogicalTable row, IBindingContext cxt, String fieldName, Throwable t) throws SyntaxNodeException {
        String errorMessage = String.format("Can not add field %s: %s", fieldName, t.getMessage());
        throw SyntaxNodeExceptionUtils.createError(errorMessage, null, null, getCellSource(row, cxt, 1));
    }

    private String getName(ILogicalTable row, IBindingContext cxt) throws OpenLCompilationException {
        GridCellSourceCodeModule nameCellSource = getCellSource(row, cxt, 1);
        IdentifierNode[] idn = getIdentifierNode(nameCellSource);
        if (idn.length != 1) {
            String errorMessage = String.format("Bad field name: %s", nameCellSource.getCode());
            throw SyntaxNodeExceptionUtils.createError(errorMessage, null, null, nameCellSource);
        } else {
            return idn[0].getIdentifier();
        }
    }

    private GridCellSourceCodeModule getCellSource(ILogicalTable row, IBindingContext cxt, int columnIndex) {
        return new GridCellSourceCodeModule(row.getColumn(columnIndex).getSource(), cxt);        
    }

    private String getDefaultValue(ILogicalTable row, IBindingContext cxt) throws OpenLCompilationException {
        GridCellSourceCodeModule defaultValueSrc = getCellSource(row, cxt, 2);    
        IdentifierNode[] idn = getIdentifierNode(defaultValueSrc);
        if (idn.length > 1) {
            // only one token is supported as default value.
            //
            String errorMessage = String.format("Bad default field value: %s", defaultValueSrc.getCode());
            throw SyntaxNodeExceptionUtils.createError(errorMessage, null, null, defaultValueSrc);
        } else if (idn.length == 0) {
            // cell source is empty
            //
            return null;
        } else {
            String defaultValue = idn[0].getIdentifier(); 
            if (DatatypeHelper.isCommented(defaultValue)) {
                // value is commented 
                //
                return null; 
            }
            return defaultValue;
        }
    }

    private IdentifierNode[] getIdentifierNode(GridCellSourceCodeModule cellSrc) 
        throws OpenLCompilationException {
        
        IdentifierNode[] idn = Tokenizer.tokenize(cellSrc, " \r\n");

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

        if (srcCode.length() == 0 || DatatypeHelper.isCommented(srcCode)) {
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

        if (row.getWidth() < 2) {
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
            	// Remove invalid type from binding context.
            	//
            	cxt.removeType(ISyntaxConstants.THIS_NAMESPACE, dataType);
                throw new OpenLCompilationException(String.format("Parent class '%s' is not defined", parentClassName));
            }
            
            if (parentClass.getInstanceClass() != null) {//parent class has errors
                if (Modifier.isFinal(parentClass.getInstanceClass().getModifiers())) {
                    // Remove invalid type from binding context.
                    //
                    cxt.removeType(ISyntaxConstants.THIS_NAMESPACE, dataType);
                    throw new OpenLCompilationException(String.format("Cannot inherit from final class \"%s\"",
                            parentClassName));
                }

                if (Modifier.isAbstract(parentClass.getInstanceClass().getModifiers())) {
                    // Remove invalid type from binding context.
                    //
                    cxt.removeType(ISyntaxConstants.THIS_NAMESPACE, dataType);
                    throw new OpenLCompilationException(String.format("Cannot inherit from abstract class \"%s\"",
                            parentClassName));
                }
            }
            
            if (parentClass instanceof DomainOpenClass) {
            	// Remove invalid type from binding context.
            	//
            	cxt.removeType(ISyntaxConstants.THIS_NAMESPACE, dataType);
                throw new OpenLCompilationException(String.format("Parent class '%s' cannot be domain type", parentClassName));
            }
            
            dataType.setSuperClass(parentClass);
        }
        addFields(cxt);
        //adding constructor with all fields after all fields have been added
        if (dataType.getFields().size() > 0) {
            dataType.addMethod(new OpenFieldsConstructor(dataType));
        }
		// Add new type to internal types of module.
		//
        moduleOpenClass.addType(ISyntaxConstants.THIS_NAMESPACE, dataType);
    }
    
    private void checkInheritedFieldsDuplication(IBindingContext cxt) throws Exception {
        IOpenClass superClass = dataType.getSuperClass();
        if (superClass != null) {
            for (Entry<String, IOpenField> field : dataType.getDeclaredFields().entrySet()) {
                IOpenField fieldInParent = superClass.getField(field.getKey());
                if(fieldInParent != null){
                    if(fieldInParent.getType().getInstanceClass().equals(field.getValue().getType().getInstanceClass())){
                        BindHelper.processWarn(String.format("Field [%s] has been already defined in class \"%s\"",
                                field.getKey(), fieldInParent.getDeclaringClass().getDisplayName(0)), tableSyntaxNode, cxt);
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
