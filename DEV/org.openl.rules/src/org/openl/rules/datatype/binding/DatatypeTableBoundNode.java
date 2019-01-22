/*
 * Created on Mar 8, 2004 Developed by OpenRules Inc. 2003-2004
 */

package org.openl.rules.datatype.binding;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.gen.FieldDescription;
import org.openl.rules.datatype.gen.FieldDescriptionBuilder;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.lang.xls.types.meta.BaseMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.DatatypeTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.utils.ParserUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.*;
import org.openl.syntax.exception.Runnable;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.DatatypeOpenField;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.InternalDatatypeClass;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.openl.util.text.LocationUtils;
import org.openl.util.text.TextInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bound node for datatype table component.
 * 
 * @author snshor
 * 
 */
public class DatatypeTableBoundNode implements IMemberBoundNode {

    private final Logger log = LoggerFactory.getLogger(DatatypeTableBoundNode.class);

    private TableSyntaxNode tableSyntaxNode;
    private DatatypeOpenClass dataType;
    private IdentifierNode parentClassIdentifier;
    private String parentClassName;
    private ModuleOpenClass moduleOpenClass;

    private ILogicalTable table;
    private OpenL openl;

    public DatatypeTableBoundNode(TableSyntaxNode tableSyntaxNode,
            DatatypeOpenClass datatype,
            ModuleOpenClass moduleOpenClass,
            ILogicalTable table,
            OpenL openl) {
        this(tableSyntaxNode, datatype, moduleOpenClass, table, openl, null);
    }

    public DatatypeTableBoundNode(TableSyntaxNode tableSyntaxNode,
            DatatypeOpenClass datatype,
            ModuleOpenClass moduleOpenClass,
            ILogicalTable table,
            OpenL openl,
            IdentifierNode parentClassIdentifier) {
        this.tableSyntaxNode = tableSyntaxNode;
        this.dataType = datatype;
        this.table = table;
        this.openl = openl;
        this.parentClassIdentifier = parentClassIdentifier;
        this.parentClassName = parentClassIdentifier != null ? parentClassIdentifier.getIdentifier() : parentClassName;
        this.moduleOpenClass = moduleOpenClass;
    }

    public static IOpenClass getRootComponentClass(IOpenClass fieldType) {
        if (!fieldType.isArray()) {
            return fieldType;
        }
        // Get the component type of the array
        //
        return getRootComponentClass(fieldType.getComponentClass());
    }

    public static GridCellSourceCodeModule getCellSource(ILogicalTable row, IBindingContext cxt, int columnIndex) {
        return new GridCellSourceCodeModule(row.getColumn(columnIndex).getSource(), cxt);
    }

    public static IdentifierNode[] getIdentifierNode(
            GridCellSourceCodeModule cellSrc) throws OpenLCompilationException {
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

    /**
     * Process datatype fields from source table.
     *
     * @param cxt binding context
     */
    private void addFields(final IBindingContext cxt) throws Exception {

        final ILogicalTable dataTable = DatatypeHelper.getNormalizedDataPartTable(table, openl, cxt);
        // Save normalized table to work with it later
        this.table = dataTable;

        int tableHeight = 0;

        if (dataTable != null) {
            tableHeight = dataTable.getHeight();
        }

        // map of fields that will be used for byte code generation.
        // key: name of the field, value: field type.
        //
        final Map<String, FieldDescription> fields = new LinkedHashMap<>();
        SyntaxNodeExceptionCollector syntaxNodeExceptionCollector = new SyntaxNodeExceptionCollector();
        for (int i = 0; i < tableHeight; i++) {
            final int index = i;
            syntaxNodeExceptionCollector.run(new Runnable() {
                @Override
                public void run() throws Exception {
                    ILogicalTable row = dataTable.getRow(index);
                    boolean firstField = (index == 0);
                    processRow(row, cxt, fields, firstField);
                }
            });
        }
        syntaxNodeExceptionCollector.run(new Runnable() {
            @Override
            public void run() throws Exception {
                checkInheritedFieldsDuplication(cxt);
            }
        });

        syntaxNodeExceptionCollector.throwIfAny();

        if (beanClassCanBeGenerated(cxt)) {
            Class<?> beanClass;
            String beanName = dataType.getJavaName();
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            try {
                beanClass = classLoader.loadClass(beanName);
                log.debug("Bean {} is using previously loaded", beanName);
            } catch (ClassNotFoundException e) {
                try {
                    byte[] byteCode = createBeanForDatatype(fields);
                    beanClass = ClassUtils.defineClass(beanName, byteCode, classLoader);
                    log.debug("bean {} is using generated at runtime", beanName);
                } catch (Exception e2) {
                    throw SyntaxNodeExceptionUtils.createError("Can't generate a Java bean for datatype " + beanName,
                        tableSyntaxNode);
                }
            }

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
            return parentClass.getInstanceClass() != null;
        }
        return true;
    }

    /**
     * Generate a simple java bean for current datatype table.
     *
     * @param fields fields for bean class
     * @return Class descriptor of generated bean class.
     */
    private byte[] createBeanForDatatype(Map<String, FieldDescription> fields) {
        String beanName = dataType.getJavaName();
        IOpenClass superOpenClass = dataType.getSuperClass();
        JavaBeanClassBuilder beanBuilder = new JavaBeanClassBuilder(beanName);
        if (superOpenClass != null) {
            Class<?> superClass = superOpenClass.getInstanceClass();
            beanBuilder.setParentClass(superClass);
            for (Entry<String, IOpenField> field : superOpenClass.getFields().entrySet()) {
                beanBuilder.addParentField(field.getKey(), field.getValue().getType().getJavaName());
            }
        }
        beanBuilder.addFields(fields);
        return beanBuilder.byteCode();
    }

    private void validateBeanForDatatype(Class<?> beanClass,
            Map<String, FieldDescription> fields) throws SyntaxNodeException {
        String beanName = dataType.getJavaName();
        IOpenClass superClass = dataType.getSuperClass();
        if (superClass != null) {
            if (!beanClass.getSuperclass().equals(superClass.getInstanceClass())) {
                String errorMessage = String.format(
                    "Datatype '%s' validation is failed on missed parent class. Please, regenerate datatype classes.",
                    beanName);
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
                    "Datatype '%s' validation is failed on missed field '%s'. Please, regenerate your datatype classes.",
                    beanName,
                    fieldName);
                throw SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode);
            }

            String name = ClassUtils.capitalize(fieldName); // According to JavaBeans v1.01
            Method getterMethod;
            try {
                getterMethod = beanClass.getMethod("get"+name);
            } catch (NoSuchMethodException e) {
                String errorMessage = String.format(
                    "Datatype '%s' validation is failed on missed method 'get%s'. Please, regenerate your datatype classes.",
                    beanName,
                    name);
                name = StringUtils.capitalize(fieldName); // Try old solution (before 5.21.7)
                try {
                    getterMethod = beanClass.getMethod("get"+name);
                } catch (NoSuchMethodException e1) {
                    throw SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode);
                }

            }
            if (!getterMethod.getReturnType().getName().equals(fieldDescription.getTypeName())) {
                String errorMessage = String.format(
                        "Datatype '%s' validation is failed on method 'get%s' with unexpected return type. Please, regenerate your datatype classes.",
                        beanName,
                        name);
                throw SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode);
            }

            String setterMethodName = "set" + name;
            Method[] methods = beanClass.getMethods();
            boolean found = false;
            for (Method method : methods) {
                if (method.getName().equals(setterMethodName)) {
                    if ((method.getParameterTypes().length == 1) && method.getParameterTypes()[0].getName()
                        .equals(fieldDescription.getTypeName())) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                String errorMessage = String.format(
                    "Datatype '%s' validation is failed on missed method '%s(%s)'. Please, regenerate your datatype classes.",
                    beanName,
                    setterMethodName,
                    fieldDescription.getTypeName());
                throw SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode);
            }
        }

        try {
            beanClass.getConstructor();
        } catch (NoSuchMethodException e) {
            String errorMessage = String.format(
                "Datatype '%s' validation is failed on missed default constructor. Please, regenerate datatype classes.",
                beanName);
            throw SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode);
        }
    }

    private void processRow(ILogicalTable row,
            IBindingContext cxt,
            Map<String, FieldDescription> fields,
            boolean firstField) throws OpenLCompilationException {

        GridCellSourceCodeModule rowSrc = new GridCellSourceCodeModule(row.getSource(), cxt);

        if (canProcessRow(rowSrc)) {
            String fieldName = getName(row, cxt);

            IOpenClass fieldType = getFieldType(cxt, row, rowSrc);
            IOpenField field = new DatatypeOpenField(dataType, fieldName, fieldType);

            if (!isRecursiveField(field) && getRootComponentClass(field.getType()).getInstanceClass() == null) {
                // For example type A depends on B and B depends on A. At this
                // point B is not generated yet.
                // TODO Implement circular datatype dependencies support like in
                // Java.
                GridCellSourceCodeModule cellSource = getCellSource(row, cxt, 0);
                TextInterval location = LocationUtils.createTextInterval(cellSource.getCode());

                String message = "Type " + getRootComponentClass(field.getType()).getName() + " isn't generated yet";
                throw SyntaxNodeExceptionUtils.createError(message, null, location, cellSource);
            }

            FieldDescriptionBuilder fieldDescriptionBuilder;
            try {
                if (fields.containsKey(fieldName)) {
                    throw SyntaxNodeExceptionUtils.createError(String.format("Field '%s' has already been defined!",
                        fieldName), null, null, getCellSource(row, cxt, 1));
                }
                if (fields.containsKey(ClassUtils.decapitalize(fieldName)) || fields
                    .containsKey(ClassUtils.capitalize(fieldName))) {
                    String f = null;
                    if (fields.containsKey(ClassUtils.decapitalize(fieldName))) {
                        f = ClassUtils.decapitalize(fieldName);
                    }
                    if (fields.containsKey(ClassUtils.capitalize(fieldName))) {
                        f = ClassUtils.capitalize(fieldName);
                    }
                    throw SyntaxNodeExceptionUtils.createError(
                        String.format("Field '%s' conflicts with '%s' field!", fieldName, f),
                        null,
                        null,
                        getCellSource(row, cxt, 1));
                }

                dataType.addField(field);
                if (firstField) {
                    // This is done for operations like people["john"] in OpenL
                    // rules to access one instance of datatype from array by
                    // user defined index.
                    // If first field type of Datatype is int, for calling the
                    // instance, wrap it
                    // with quotes, e.g. vehicle["23"].
                    // Calling the instance like: drivers[7], you will get the 8
                    // element of array.
                    //
                    // See DynamicArrayAggregateInfo#getIndex(IOpenClass
                    // aggregateType, IOpenClass indexType)
                    // and DatatypeArrayTest
                    dataType.setIndexField(field);
                }

                fieldDescriptionBuilder = FieldDescriptionBuilder.create(field.getType().getJavaName());
            } catch (SyntaxNodeException e) {
                throw e;
            } catch (Exception t) {
                throw SyntaxNodeExceptionUtils.createError(t.getMessage(), t, null, getCellSource(row, cxt, 1));
            }

            FieldDescription fieldDescription = null;
            if (row.getWidth() > 2) {
                String defaultValue = getDefaultValue(row, cxt);

                ConstantOpenField constantOpenField = RuleRowHelper.findConstantField(cxt, defaultValue);
                if (constantOpenField != null) {
                    fieldDescriptionBuilder.setDefaultValue(constantOpenField.getValue());
                    fieldDescriptionBuilder.setDefaultValueAsString(constantOpenField.getValueAsString());
                    if (!cxt.isExecutionMode()) {
                        ICell cell = getCellSource(row, cxt, 2).getCell();
                        MetaInfoReader metaInfoReader = tableSyntaxNode.getMetaInfoReader();
                        if (metaInfoReader instanceof BaseMetaInfoReader) {
                            SimpleNodeUsage nodeUsage = RuleRowHelper.createConstantNodeUsage(defaultValue, constantOpenField);
                            ((BaseMetaInfoReader) metaInfoReader).addConstant(cell, nodeUsage);
                        }
                    }
                } else {
                    fieldDescriptionBuilder.setDefaultValueAsString(defaultValue);

                    if (!String.class.equals(fieldType.getInstanceClass())) {
                        ICell theCellValue = row.getColumn(2).getCell(0, 0);
                        if (theCellValue.hasNativeType()) {
                            Object value = RuleRowHelper.loadNativeValue(theCellValue, fieldType);
                            if (value != null) {
                                fieldDescriptionBuilder.setDefaultValue(value);
                            }
                        }
                    }

                    try {
                        fieldDescription = fieldDescriptionBuilder.build();
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
                            TextInterval location = defaultValue == null ? null
                                                                         : LocationUtils
                                                                             .createTextInterval(defaultValue);
                            throw SyntaxNodeExceptionUtils.createError(message, e, location, cellSourceCodeModule);
                        }
                    }
                    Object value = fieldDescription.getDefaultValue();
                    if (value != null && !(fieldDescription.hasDefaultKeyWord() && fieldDescription.isArray())) {
                        // Validate not null default value
                        // The null value is allowed for alias types
                        try {
                            RuleRowHelper.validateValue(value, fieldType);
                        } catch (Exception e) {
                            throw SyntaxNodeExceptionUtils
                                .createError(e.getMessage(), e, null, getCellSource(row, cxt, 2));
                        }
                    }
                }
            }
            if (fieldDescription == null) {
                fieldDescription = fieldDescriptionBuilder.build();
            }
            fields.put(fieldName, fieldDescription);
        }
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

    public static String getDefaultValue(ILogicalTable row, IBindingContext cxt) throws OpenLCompilationException {
        String defaultValue = null;
        GridCellSourceCodeModule defaultValueSrc = getCellSource(row, cxt, 2);
        if (!ParserUtils.isCommented(defaultValueSrc.getCode())) {
            IdentifierNode[] idn = getIdentifierNode(defaultValueSrc);
            if (idn.length > 0) {
                // if there is any valid identifier, consider it is a default
                // value
                //
                defaultValue = defaultValueSrc.getCode();
            }
        }
        return defaultValue;
    }

    private IOpenClass getFieldType(IBindingContext bindingContext,
            ILogicalTable row,
            GridCellSourceCodeModule tableSrc) throws SyntaxNodeException {

        IOpenClass fieldType = OpenLManager.makeType(openl, tableSrc, bindingContext);

        if (fieldType == null || fieldType instanceof NullOpenClass) {
            String errorMessage = String.format("Type %s is not found", tableSrc.getCode());
            throw SyntaxNodeExceptionUtils.createError(errorMessage, null, null, tableSrc);
        }

        if (row.getWidth() < 2) {
            String errorMessage = "Bad table structure: must be {header} / {type | name}";
            throw SyntaxNodeExceptionUtils.createError(errorMessage, null, null, tableSrc);
        }
        return fieldType;
    }

    public void addTo(ModuleOpenClass openClass) {
        InternalDatatypeClass internalClassMember = new InternalDatatypeClass(dataType, openClass);
        tableSyntaxNode.setMember(internalClassMember);
    }

    public void finalizeBind(IBindingContext cxt) throws Exception {
        if (!cxt.isExecutionMode()) {
            tableSyntaxNode.setMetaInfoReader(new DatatypeTableMetaInfoReader(this));
        }
        if (parentClassName != null) {
            IOpenClass parentClass = cxt.findType(ISyntaxConstants.THIS_NAMESPACE, parentClassName);
            if (parentClass == null) {
                throw new OpenLCompilationException(String.format("Parent class '%s' is not defined", parentClassName));
            }

            if (parentClass.getInstanceClass() != null) {// parent class has
                                                         // errors
                if (Modifier.isFinal(parentClass.getInstanceClass().getModifiers())) {
                    throw new OpenLCompilationException(
                        String.format("Cannot inherit from final class \"%s\"", parentClassName));
                }

                if (Modifier.isAbstract(parentClass.getInstanceClass().getModifiers())) {
                    throw new OpenLCompilationException(
                        String.format("Cannot inherit from abstract class \"%s\"", parentClassName));
                }
            }

            if (parentClass instanceof DomainOpenClass) {
                throw new OpenLCompilationException(
                    String.format("Parent class '%s' cannot be domain type", parentClassName));
            }

            dataType.setSuperClass(parentClass);
        }
        addFields(cxt);
        // Add new type to internal types of module.
        //
        moduleOpenClass.addType(dataType);
    }

    private void checkInheritedFieldsDuplication(final IBindingContext cxt) throws Exception {
        final IOpenClass superClass = dataType.getSuperClass();
        if (superClass != null) {
            SyntaxNodeExceptionCollector syntaxNodeExceptionCollector = new SyntaxNodeExceptionCollector();
            for (final Entry<String, IOpenField> field : dataType.getDeclaredFields().entrySet()) {
                syntaxNodeExceptionCollector.run(new Runnable() {
                    @Override
                    public void run() throws Exception {
                        IOpenField fieldInParent = superClass.getField(field.getKey());
                        if (fieldInParent != null) {
                            if (fieldInParent.getType().getInstanceClass().equals(
                                field.getValue().getType().getInstanceClass())) {
                                BindHelper
                                    .processWarn(String.format("Field [%s] has been already defined in class \"%s\"",
                                        field.getKey(),
                                        fieldInParent.getDeclaringClass().getDisplayName(0)), tableSyntaxNode, cxt);
                            } else {
                                throw SyntaxNodeExceptionUtils.createError(String.format(
                                    "Field [%s] has been already defined in class \"%s\" with another type",
                                    field.getKey(),
                                    fieldInParent.getDeclaringClass().getDisplayName(0)), tableSyntaxNode);
                            }
                        }
                    }
                });
            }
            syntaxNodeExceptionCollector.throwIfAny();
        }
    }

    public void removeDebugInformation(IBindingContext cxt) {
        // nothing to remove
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return tableSyntaxNode;
    }

    public DatatypeOpenClass getDataType() {
        return dataType;
    }

    public ILogicalTable getTable() {
        return table;
    }

    public IdentifierNode getParentClassIdentifier() {
        return parentClassIdentifier;
    }
}
