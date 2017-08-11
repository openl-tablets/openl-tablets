/*
 * Created on Mar 8, 2004 Developed by OpenRules Inc. 2003-2004
 */

package org.openl.rules.datatype.binding;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.IMetaInfo;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.datatype.gen.DefaultFieldDescription;
import org.openl.rules.datatype.gen.FieldDescription;
import org.openl.rules.datatype.gen.RecursiveFieldDescription;
import org.openl.rules.datatype.gen.SimpleBeanByteCodeGenerator;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenClass.OpenFieldsConstructor;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.utils.ParserUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
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
import org.openl.util.StringTool;
import org.openl.util.text.LocationUtils;
import org.openl.util.text.TextInterval;

/**
 * Bound node for datatype table component.
 * 
 * @author snshor
 * 
 */
public class DatatypeTableBoundNode implements IMemberBoundNode {

    private TableSyntaxNode tableSyntaxNode;
    private DatatypeOpenClass dataType;
    private IdentifierNode parentClassIdentifier;
    private String parentClassName;
    private ModuleOpenClass moduleOpenClass;

    private ILogicalTable table;
    private OpenL openl;

    public DatatypeTableBoundNode(TableSyntaxNode tableSyntaxNode, DatatypeOpenClass datatype,
    		ModuleOpenClass moduleOpenClass, ILogicalTable table, OpenL openl) {
        this(tableSyntaxNode,datatype, moduleOpenClass, table, openl, null);
    }
    
    public DatatypeTableBoundNode(TableSyntaxNode tableSyntaxNode, DatatypeOpenClass datatype,
    		ModuleOpenClass moduleOpenClass, ILogicalTable table, OpenL openl, IdentifierNode parentClassIdentifier) {
        this.tableSyntaxNode = tableSyntaxNode;
        this.dataType = datatype;
        this.table = table;
        this.openl = openl;
        this.parentClassIdentifier = parentClassIdentifier;
        this.parentClassName = parentClassIdentifier != null ? parentClassIdentifier.getIdentifier() : parentClassName;
        this.moduleOpenClass = moduleOpenClass;
    }

    /**
     * Process datatype fields from source table.
     * 
     * @param cxt binding context
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
            boolean firstField = (i == 0);
            processRow(row, cxt, fields, firstField);
        }
        checkInheritedFieldsDuplication(cxt);
        
        if (beanClassCanBeGenerated(cxt)) {
            Class<?> beanClass = createBeanForDatatype(fields);
            dataType.setInstanceClass(beanClass);
            validateBeanForDatatype(beanClass, fields);
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
     * @return Class descriptor of generated bean class.
     * @throws SyntaxNodeException is can`t generate bean for datatype table.
     */
    private Class<?> createBeanForDatatype(Map<String, FieldDescription> fields) throws SyntaxNodeException {
        String datatypeName = dataType.getName();
        String beanName = getDatatypeBeanNameWithNamespace(datatypeName);
        IOpenClass superClass = dataType.getSuperClass();
        SimpleBeanByteCodeGenerator beanGenerator;
        if (superClass != null) {
            LinkedHashMap<String, FieldDescription> parentFields = new LinkedHashMap<String, FieldDescription>();
            for (Entry<String, IOpenField> field : superClass.getFields().entrySet()) {
                parentFields.put(field.getKey(), new DefaultFieldDescription(field.getValue()));
            }
            beanGenerator = new SimpleBeanByteCodeGenerator(beanName, fields, superClass.getInstanceClass(), parentFields);
        } else {
            beanGenerator = new SimpleBeanByteCodeGenerator(beanName, fields);
        }
        
        try {
            return beanGenerator.generateAndLoadBeanClass();
        } catch (RuntimeException e) {
            String errorMessage = String.format("Can't generate bean for datatype '%s': %s", datatypeName, e.getMessage());
            throw SyntaxNodeExceptionUtils.createError(errorMessage, e, tableSyntaxNode);
        }
    }

    private void validateBeanForDatatype(Class<?> beanClass,
            Map<String, FieldDescription> fields) throws SyntaxNodeException {
        String datatypeName = dataType.getName();
        String beanName = getDatatypeBeanNameWithNamespace(datatypeName);
        IOpenClass superClass = dataType.getSuperClass();
        if (superClass != null) {
            if (!beanClass.getSuperclass().equals(superClass.getInstanceClass())) {
                String errorMessage = String.format(
                    "Class '%s' is found in classloader. This class has invalid parent class. Please, regenerate your datatype classes.",
                    datatypeName);
                throw SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode);
            }
        }

        for (Entry<String, FieldDescription> fieldEntry : fields.entrySet()) {
            String fieldName = fieldEntry.getKey();
            FieldDescription fieldDescription = fieldEntry.getValue();
            try {
                beanClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                String errorMessage = String.format(
                    "Class '%s' is found in classloader. Field '%s' is missed. Please, regenerate your datatype classes.", beanName, fieldName);
                throw SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode);
            }

            String getterMethodName = StringTool.getGetterName(fieldName);
            try {
                Method getterMethod = beanClass.getMethod(getterMethodName);
                if (!getterMethod.getReturnType().getCanonicalName().equals(fieldDescription.getCanonicalTypeName())) {
                    String errorMessage = String.format(
                        "Class '%s' is found in classloader. Method '%s' returns invalid type. Please, regenerate your datatype classes.", beanName, getterMethodName);
                    throw SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode);
                }
            } catch (NoSuchMethodException e) {
                String errorMessage = String.format(
                    "Class '%s' is found in classloader. Method '%s' is missed. Please, regenerate your datatype classes.", beanName, getterMethodName);
                throw SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode);
            }

            String setterMethodName = StringTool.getSetterName(fieldName);
            Method[] methods = beanClass.getMethods();
            boolean found = false;
            for (Method method : methods) {
                if (method.getName().equals(setterMethodName)) {
                    if ((method.getParameterTypes().length == 1) && method.getParameterTypes()[0].getCanonicalName()
                        .equals(fieldDescription.getCanonicalTypeName())) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                String errorMessage = String.format(
                    "Class '%s' is found in classloader. Method '%s(%s)' is missed. Please, regenerate your datatype classes.",
                    beanName,
                    setterMethodName,
                    fieldDescription.getCanonicalTypeName());
                throw SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode);
            }
        }
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
            throws OpenLCompilationException {

        GridCellSourceCodeModule rowSrc = new GridCellSourceCodeModule(row.getSource(), cxt);

        if (canProcessRow(rowSrc)) {
            String fieldName = getName(row, cxt);

            IOpenClass fieldType = getFieldType(cxt, row, rowSrc);
            IOpenField field = new DatatypeOpenField(dataType, fieldName, fieldType);

            if (!cxt.isExecutionMode()) {
                IdentifierNode[] parsedHeader = Tokenizer.tokenize(rowSrc, "[]\n\r");
                IMetaInfo metaInfo = fieldType.getMetaInfo();
                if (metaInfo == null) {
                    metaInfo = getRootComponentClass(fieldType).getMetaInfo();
                }

                // Link to field type table
                RuleRowHelper.setCellMetaInfoWithNodeUsage(row, parsedHeader[0], metaInfo, NodeType.DATATYPE);
            }

            if (!isRecursiveField(field) && getRootComponentClass(field.getType()).getInstanceClass() == null) {
                // For example type A depends on B and B depends on A. At this point B is not generated yet.
                // TODO Implement circular datatype dependencies support like in Java.
                GridCellSourceCodeModule cellSource = getCellSource(row, cxt, 0);
                TextInterval location = LocationUtils.createTextInterval(cellSource.getCode());

                String message = "Type " + getRootComponentClass(field.getType()).getName() + " isn't generated yet";
                throw SyntaxNodeExceptionUtils.createError(message, null, location, cellSource);
            }

            FieldDescription fieldDescription;
            try {
                dataType.addField(field);
                if (firstField) {
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

                fieldDescription = fieldDescriptionFactory(field);
                fields.put(fieldName, fieldDescription);
            } catch (Throwable t) {
                throw SyntaxNodeExceptionUtils.createError(t.getMessage(), t, null, getCellSource(row, cxt, 1));
            }

            if (row.getWidth() > 2) {
                String defaultValue = getDefaultValue(row, cxt);
                fieldDescription.setDefaultValueAsString(defaultValue);
                if (fieldDescription.getType().equals(Date.class)){
                    //EPBDS-6068 add metainfo for XlsDataFormatterFactory.getFormatter can define correct formater for cell.
                    Object value = row.getColumn(2).getCell(0, 0).getObjectValue();
                    if (value != null && fieldDescription.getType().equals(value.getClass())){
                        RuleRowHelper.setCellMetaInfo(row.getColumn(2), null, fieldType, false);
                        fieldDescription.setDefaultValue(value);
                    }
                }
                Object value;
                try {
                    value = fieldDescription.getDefaultValue();
                } catch (RuntimeException e) {
                    String message = String.format("Can't parse cell value '%s'", defaultValue);
                    IOpenSourceCodeModule cellSourceCodeModule = getCellSource(row, cxt, 2);

                    if (e instanceof CompositeSyntaxNodeException) {
                        CompositeSyntaxNodeException exception = (CompositeSyntaxNodeException) e;
                        if (exception.getErrors() != null && exception.getErrors().length == 1) {
                            SyntaxNodeException syntaxNodeException = exception.getErrors()[0];
                            throw SyntaxNodeExceptionUtils.createError(message,
                                    null,
                                    syntaxNodeException.getLocation(),
                                    cellSourceCodeModule);
                        }
                        throw SyntaxNodeExceptionUtils.createError(message, cellSourceCodeModule);
                    } else {
                        TextInterval location = defaultValue == null ?
                                                null :
                                                LocationUtils.createTextInterval(defaultValue);
                        throw SyntaxNodeExceptionUtils.createError(message, e, location, cellSourceCodeModule);
                    }
                }
                if (value != null && !(fieldDescription.hasDefaultKeyWord() && fieldDescription.getType().isArray())) {
                    // Validate not null default value
                    // The null value is allowed for alias types
                    try {
                        RuleRowHelper.validateValue(value, fieldType);
                    } catch (Exception e) {
                        throw SyntaxNodeExceptionUtils.createError(e.getMessage(), e, null, getCellSource(row, cxt, 2));
                    }
                }
            }
        }
    }

    private FieldDescription fieldDescriptionFactory(IOpenField field) {
        if (isRecursiveField(field)) {
            return new RecursiveFieldDescription(field);
        }
        return new DefaultFieldDescription(field);
    }

    /**
     * Checks if the type of the field is equal to the current datatype.
     *
     * @param field checking field
     * @return true if the type of the field is equal to the given datatype
     */
    private boolean isRecursiveField(IOpenField field) {
        IOpenClass fieldType = getRootComponentClass(field.getType());
        return fieldType.getName().equals(dataType.getName());
    }

    public static IOpenClass getRootComponentClass(IOpenClass fieldType) {
        if (!fieldType.isArray()) {
            return fieldType;
        }
        // Get the component type of the array
        //
        return getRootComponentClass(fieldType.getComponentClass());
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

    public static GridCellSourceCodeModule getCellSource(ILogicalTable row, IBindingContext cxt, int columnIndex) {
        return new GridCellSourceCodeModule(row.getColumn(columnIndex).getSource(), cxt);        
    }

    private String getDefaultValue(ILogicalTable row, IBindingContext cxt) throws OpenLCompilationException {
        String defaultValue = null;
        GridCellSourceCodeModule defaultValueSrc = getCellSource(row, cxt, 2);
        if (!ParserUtils.isCommented(defaultValueSrc.getCode())) {
            IdentifierNode[] idn = getIdentifierNode(defaultValueSrc);
            if (idn.length > 0) {
                // if there is any valid identifier, consider it is a default value
                //
                defaultValue = defaultValueSrc.getCode();
            }          
        }
        return defaultValue;
    }

    public static IdentifierNode[] getIdentifierNode(GridCellSourceCodeModule cellSrc)
        throws OpenLCompilationException {
        return Tokenizer.tokenize(cellSrc, " \r\n");
    }

    /**
     * Encapsulates the wrapping the row and bindingContext with the GridCellSourceCodeModule
     */
    public static boolean canProcessRow(ILogicalTable row, IBindingContext cxt) {
        GridCellSourceCodeModule rowSrc = new GridCellSourceCodeModule(row.getSource(), cxt);
        return canProcessRow(rowSrc);
    }
    
    /**
     * Checks if the given row can be processed. 
     * 
     * @param rowSrc checked row
     * @return false if row content is empty, or was commented with special symbols.
     */
    public static boolean canProcessRow(GridCellSourceCodeModule rowSrc) {
        return !ParserUtils.isBlankOrCommented(rowSrc.getCode());
    }

    private IOpenClass getFieldType(IBindingContext cxt, ILogicalTable row, GridCellSourceCodeModule tableSrc) 
        throws SyntaxNodeException {
        
        IOpenClass fieldType = OpenLManager.makeType(openl, tableSrc, (IBindingContextDelegator) cxt);

        if (fieldType == null || fieldType instanceof NullOpenClass) {
            String errorMessage = String.format("Type %s is not found", tableSrc.getCode());
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

            if (!cxt.isExecutionMode()) {
                // Link to parent class table
                RuleRowHelper.setCellMetaInfoWithNodeUsage(table, parentClassIdentifier, parentClass.getMetaInfo(),
                        NodeType.DATATYPE);
            }

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
                        throw SyntaxNodeExceptionUtils.createError(String.format(
                                "Field [%s] has been already defined in class \"%s\" with another type",
                                field.getKey(), fieldInParent.getDeclaringClass().getDisplayName(0)),
                                tableSyntaxNode);
                    }
                }
            }
        }
    }

    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        //nothing to remove
    }

}
