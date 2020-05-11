/*
 * Created on Mar 8, 2004 Developed by OpenRules Inc. 2003-2004
 */

package org.openl.rules.datatype.binding;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.gen.ByteCodeGenerationException;
import org.openl.gen.FieldDescription;
import org.openl.gen.TypeDescription;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.datatype.gen.FieldDescriptionBuilder;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenField;
import org.openl.rules.lang.xls.types.TransientOpenField;
import org.openl.rules.lang.xls.types.meta.BaseMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.DatatypeTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.utils.ParserUtils;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionCollector;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.InternalDatatypeClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ArrayUtils;
import org.openl.util.ClassUtils;
import org.openl.util.MessageUtils;
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

    public static final String NON_TRANSIENT_FIELD_SUFFIX = "*";
    public static final String TRANSIENT_FIELD_SUFFIX = "~";
    private final Logger log = LoggerFactory.getLogger(DatatypeTableBoundNode.class);

    private final TableSyntaxNode tableSyntaxNode;
    private final DatatypeOpenClass dataType;
    private final IdentifierNode parentClassIdentifier;
    private final String parentClassName;
    private final ModuleOpenClass moduleOpenClass;

    private DatatypeTableBoundNode parentDatatypeTableBoundNode;
    private boolean generated = false;
    private boolean generatingInProcess = false;
    private boolean byteCodeReadyToLoad = false;

    private ILogicalTable table;
    private final OpenL openl;

    private Map<String, FieldDescription> fields;

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
        this.parentClassName = parentClassIdentifier != null ? parentClassIdentifier.getIdentifier() : null;
        this.moduleOpenClass = moduleOpenClass;
    }

    public static GridCellSourceCodeModule getCellSource(ILogicalTable row, IBindingContext cxt, int columnIndex) {
        return new GridCellSourceCodeModule(row.getColumn(columnIndex).getSource(), cxt);
    }

    public static IdentifierNode[] getIdentifierNode(
            GridCellSourceCodeModule cellSrc) throws OpenLCompilationException {
        return Tokenizer.tokenize(cellSrc, " \r\n");
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

    public String getParentClassName() {
        return parentClassName;
    }

    /**
     * Process datatype fields from source table.
     *
     * @param bindingContext binding context
     */
    private void readFieldsAndGenerateByteCode(final IBindingContext bindingContext) throws Exception {

        final ILogicalTable dataTable = DatatypeHelper.getNormalizedDataPartTable(table, openl, bindingContext);
        // Save normalized table to work with it later
        this.table = dataTable;

        int tableHeight = 0;

        if (dataTable != null) {
            tableHeight = dataTable.getHeight();
        }

        // map of fields that will be used for byte code generation.
        // key: name of the field, value: field type.
        //
        fields = new LinkedHashMap<>();
        SyntaxNodeExceptionCollector syntaxNodeExceptionCollector = new SyntaxNodeExceptionCollector();
        boolean useTransientSuffix = true;
        for (int i = 0; i < tableHeight; i++) {
            if (fieldNameEndsWithNonTransientSuffix(dataTable.getRow(i), bindingContext)) {
                useTransientSuffix = false;
                break;
            }
        }
        MutableBoolean firstNonTransientField = new MutableBoolean(true);
        for (int i = 0; i < tableHeight; i++) {
            final int index = i;
            final boolean withTransientSuffix = useTransientSuffix;
            syntaxNodeExceptionCollector.run(() -> processRow(dataTable
                .getRow(index), bindingContext, fields, firstNonTransientField, withTransientSuffix));
        }

        syntaxNodeExceptionCollector.run(() -> checkInheritedFieldsDuplication(bindingContext));
        syntaxNodeExceptionCollector.throwIfAny();

        if (beanClassCanBeGenerated(bindingContext)) {
            String datatypeClassName = dataType.getJavaName();
            OpenLBundleClassLoader classLoader = (OpenLBundleClassLoader) Thread.currentThread()
                .getContextClassLoader();
            try {
                Class<?> beanClass = classLoader.loadClass(datatypeClassName);
                byteCodeReadyToLoad = true;
                validateDatatypeClass(beanClass, fields);
                log.debug("Loaded from classloader class '{}' is used.", datatypeClassName);
            } catch (ClassNotFoundException e) {
                try {
                    final byte[] byteCode = buildByteCodeForDatatype(fields);
                    classLoader.addGeneratedClass(datatypeClassName, byteCode);
                    dataType.setBytecode(byteCode);
                    byteCodeReadyToLoad = true;
                    log.debug("Generated at runtime class '{}' is used.", datatypeClassName);
                } catch (ByteCodeGenerationException e1) {
                    throw SyntaxNodeExceptionUtils.createError(String
                        .format("Failed to generate a class for datatype '%s'. %s", datatypeClassName, e1.getMessage()),
                        e1,
                        tableSyntaxNode);
                } catch (Exception e2) {
                    throw SyntaxNodeExceptionUtils.createError(
                        String.format("Failed to generate a class for datatype '%s'.", datatypeClassName),
                        tableSyntaxNode);
                }
            }
        }
    }

    private boolean beanClassCanBeGenerated(IBindingContext cxt) {
        if (tableSyntaxNode.hasErrors()) {
            return false;
        }
        if (parentClassName != null) {
            IOpenClass parentClass = cxt.findType(ISyntaxConstants.THIS_NAMESPACE, parentClassName);
            return parentClass != null;
        }
        return true;
    }

    /**
     * Generate a simple java bean for current datatype table.
     *
     * @param fields fields for bean class
     * @return Class descriptor of generated bean class.
     */
    private byte[] buildByteCodeForDatatype(Map<String, FieldDescription> fields) {
        String datatypeClassName = dataType.getJavaName();
        IOpenClass superOpenClass = dataType.getSuperClass();
        JavaBeanClassBuilder beanBuilder = new JavaBeanClassBuilder(datatypeClassName);
        if (superOpenClass != null) {
            beanBuilder.setParentType(new TypeDescription(superOpenClass.getJavaName()));
            if (superOpenClass instanceof DatatypeOpenClass) {
                Map<String, FieldDescription> parentFields;
                if (parentDatatypeTableBoundNode != null) {
                    parentFields = parentDatatypeTableBoundNode.getFields();
                } else {
                    parentFields = new LinkedHashMap<>();
                    for (IOpenField field : superOpenClass.getFields()) {
                        parentFields.put(field.getName(), new FieldDescription(field.getType().getJavaName()));
                    }
                }
                for (Entry<String, FieldDescription> field : parentFields.entrySet()) {
                    beanBuilder.addParentField(field.getKey(), field.getValue());
                }
            }
        }
        beanBuilder.addFields(fields);
        return beanBuilder.byteCode();
    }

    private Map<String, FieldDescription> getFields() {
        return fields;
    }

    public void setFields(Map<String, FieldDescription> fields) {
        this.fields = fields;
    }

    private void validateDatatypeClass(Class<?> datatypeClass,
            Map<String, FieldDescription> fields) throws SyntaxNodeException {
        SyntaxNodeExceptionCollector syntaxNodeExceptionCollector = new SyntaxNodeExceptionCollector();
        String datatypeClassName = dataType.getJavaName();
        IOpenClass superClass = dataType.getSuperClass();
        if (superClass != null && !datatypeClass.getSuperclass().getName().equals(superClass.getJavaName())) {
            String errorMessage = String
                .format("Invalid parent class in class '%s'. Please, regenerate datatype classes.", datatypeClassName);
            syntaxNodeExceptionCollector
                .addSyntaxNodeException(SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode));
        }

        try {
            datatypeClass.getConstructor();
        } catch (NoSuchMethodException e) {
            String errorMessage = String.format(
                "Default constructor is not found in class '%s'. Please, regenerate datatype classes.",
                datatypeClassName);
            syntaxNodeExceptionCollector
                .addSyntaxNodeException(SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode));
        }

        Object instance = null;
        try {
            instance = datatypeClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            String errorMessage = String.format(
                "Default constructor is not found in class '%s' or class is not instantiatable. Please, regenerate datatype classes.",
                datatypeClassName);
            syntaxNodeExceptionCollector
                .addSyntaxNodeException(SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode));
        }

        for (Entry<String, FieldDescription> fieldEntry : fields.entrySet()) {
            String fieldName = fieldEntry.getKey();
            FieldDescription fieldDescription = fieldEntry.getValue();
            try {
                datatypeClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                String errorMessage = String.format(
                    "Field '%s' is not found in class '%s'. Please, regenerate datatype classes.",
                    fieldName,
                    datatypeClassName);
                syntaxNodeExceptionCollector
                    .addSyntaxNodeException(SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode));

            }

            String name = ClassUtils.capitalize(fieldName); // According to JavaBeans v1.01
            Method getterMethod = null;
            try {
                getterMethod = datatypeClass.getMethod("get" + name);
            } catch (NoSuchMethodException e) {
                String errorMessage = String.format(
                    "Method 'get%s' is not found in class '%s'. Please, regenerate datatype classes.",
                    name,
                    datatypeClassName);
                name = StringUtils.capitalize(fieldName); // Try old solution (before 5.21.7)
                try {
                    getterMethod = datatypeClass.getMethod("get" + name);
                } catch (NoSuchMethodException e1) {
                    syntaxNodeExceptionCollector
                        .addSyntaxNodeException(SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode));
                }
            }
            if (getterMethod != null) {
                if (!getterMethod.getReturnType().getName().equals(fieldDescription.getTypeName())) {
                    String errorMessage = String.format(
                        "Unexpected return type for method '%s' in class '%s'. Please, regenerate datatype classes.",
                        getterMethod.getName(),
                        datatypeClassName);
                    throw SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode);
                }
                if (!Modifier.isPublic(getterMethod.getModifiers())) {
                    String errorMessage = String.format(
                        "Unexpected access modifier on method '%s' in class '%s'. Please, regenerate datatype classes.",
                        getterMethod.getName(),
                        datatypeClassName);
                    throw SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode);
                }
                if (instance != null && fieldEntry.getValue().getDefaultValue() != null) {
                    boolean f = false;
                    try {
                        if (fieldEntry.getValue().hasDefaultKeyWord()) {
                            Object defaultValue = getterMethod.invoke(instance);
                            if (defaultValue == null) {
                                f = true;
                            }
                        } else if (fieldEntry.getValue().hasDefaultValue()) {
                            Object defaultValue = getterMethod.invoke(instance);
                            if (getterMethod.getReturnType().isArray() && defaultValue.getClass().isArray()) {
                                if (!ArrayUtils.deepEquals(fieldEntry.getValue().getDefaultValue(), defaultValue)) {
                                    f = true;
                                }
                            } else {
                                if (!Objects.equals(fieldEntry.getValue().getDefaultValue(), defaultValue)) {
                                    f = true;
                                }
                            }
                        }
                    } catch (ReflectiveOperationException | LinkageError ignored) {
                    }
                    if (f) {
                        String errorMessage = String.format(
                            "Default value for field '%s' in class '%s' mismatches default value is used in datatype '%s'. Please, regenerate datatype classes.",
                            fieldEntry.getKey(),
                            datatypeClassName,
                            dataType.getName());
                        syntaxNodeExceptionCollector.addSyntaxNodeException(
                            SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode));
                    }
                }
            }

            String setterMethodName = "set" + name;
            Method[] methods = datatypeClass.getMethods();
            boolean found = false;
            for (Method method : methods) {
                if (method.getName().equals(setterMethodName)) {
                    if (method.getParameterTypes().length == 1 && method.getParameterTypes()[0].getName()
                        .equals(fieldDescription.getTypeName())) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                String errorMessage = String.format(
                    "Method '%s(%s)' is not found in class '%s'. Please, regenerate datatype classes.",
                    setterMethodName,
                    fieldDescription.getTypeName(),
                    datatypeClassName);
                syntaxNodeExceptionCollector
                    .addSyntaxNodeException(SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode));

            }
        }
        if (parentDatatypeTableBoundNode != null) {
            if (datatypeClass.getSuperclass() == null || !Objects.equals(
                parentDatatypeTableBoundNode.getDataType().getJavaName(),
                datatypeClass.getSuperclass().getName())) {
                String errorMessage = String.format(
                    "Invalid parent class'%s' is found in class '%s'. Please, regenerate datatype classes.",
                    datatypeClass.getSuperclass() != null ? " " + datatypeClass.getSuperclass().getTypeName() : "",
                    datatypeClassName);
                syntaxNodeExceptionCollector
                    .addSyntaxNodeException(SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode));
            }
            for (Entry<String, FieldDescription> fieldEntry : parentDatatypeTableBoundNode.getFields().entrySet()) {
                try {
                    Field f = datatypeClass.getSuperclass().getDeclaredField(fieldEntry.getKey());
                    if (!Modifier.isPublic(f.getModifiers()) && !Modifier.isProtected(f.getModifiers())) {
                        String errorMessage = String.format(
                            "Invalid access modifier is found on field '%s' in class '%s'. Please, regenerate datatype classes.",
                            fieldEntry.getKey(),
                            datatypeClass.getSuperclass().getTypeName());
                        syntaxNodeExceptionCollector.addSyntaxNodeException(
                            SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode));
                    }
                } catch (NoSuchFieldException ignored) {
                }
            }
            boolean g = false;
            for (Constructor<?> constructor : datatypeClass.getSuperclass().getConstructors()) {
                if (constructor.getParameterCount() == parentDatatypeTableBoundNode.getFields().size()) {
                    int i = 0;
                    boolean f = true;
                    for (FieldDescription fieldDescription : parentDatatypeTableBoundNode.getFields().values()) {
                        if (!constructor.getParameterTypes()[i].getName().equals(fieldDescription.getTypeName())) {
                            f = false;
                            break;
                        }
                        i++;
                    }
                    if (f) {
                        g = true;
                        break;
                    }
                }
            }
            if (!g) {
                String errorMessage = String.format(
                    "Required constructor with parameters is not found in class '%s'. Please, regenerate datatype classes.",
                    datatypeClass.getSuperclass().getTypeName());
                syntaxNodeExceptionCollector
                    .addSyntaxNodeException(SyntaxNodeExceptionUtils.createError(errorMessage, tableSyntaxNode));
            }
        }
        syntaxNodeExceptionCollector.throwIfAny();
    }

    private boolean fieldNameEndsWithNonTransientSuffix(ILogicalTable row,
            IBindingContext bindingContext) throws OpenLCompilationException {
        GridCellSourceCodeModule nameCellSource = getCellSource(row, bindingContext, 1);
        IdentifierNode[] idn = getIdentifierNode(nameCellSource);
        final String code = Arrays.stream(idn).map(IdentifierNode::getIdentifier).collect(Collectors.joining());
        if (code.contains(":context")) {
            int c = code.indexOf(":context");
            return code.substring(0, c).endsWith(NON_TRANSIENT_FIELD_SUFFIX);
        } else {
            if (idn.length != 1) {
                return false;
            } else {
                return idn[0].getIdentifier().endsWith(NON_TRANSIENT_FIELD_SUFFIX);
            }
        }
    }

    private Pair<String, String> parseFieldNameCell(ILogicalTable row,
            IBindingContext bindingContext) throws OpenLCompilationException {
        GridCellSourceCodeModule nameCellSource = getCellSource(row, bindingContext, 1);
        IdentifierNode[] idn = getIdentifierNode(nameCellSource);
        final String code = Arrays.stream(idn).map(IdentifierNode::getIdentifier).collect(Collectors.joining());
        if (code.contains(":context")) {
            int c = code.indexOf(":context");
            String contextPropertyName = code.substring(c + ":context".length());
            if (contextPropertyName.startsWith(".")) {
                contextPropertyName = contextPropertyName.substring(1);
            }
            return Pair.of(code.substring(0, c),
                contextPropertyName.isEmpty() ? code.substring(0, c) : contextPropertyName);
        } else {
            if (idn.length != 1) {
                String errorMessage = String.format("Bad field name: '%s'.", nameCellSource.getCode());
                throw SyntaxNodeExceptionUtils.createError(errorMessage, null, null, nameCellSource);
            } else {
                return Pair.of(idn[0].getIdentifier(), null);
            }
        }
    }

    private void processRow(ILogicalTable row,
            IBindingContext bindingContext,
            Map<String, FieldDescription> fields,
            MutableBoolean firstNonTransientField,
            boolean useTransientSuffix) throws OpenLCompilationException {
        GridCellSourceCodeModule rowSrc = new GridCellSourceCodeModule(row.getSource(), bindingContext);
        if (canProcessRow(rowSrc)) {
            Pair<String, String> fieldNameCellParsed = parseFieldNameCell(row, bindingContext);
            final boolean isTransient = useTransientSuffix ? fieldNameCellParsed.getLeft()
                .endsWith(TRANSIENT_FIELD_SUFFIX) : !fieldNameCellParsed.getLeft().endsWith(NON_TRANSIENT_FIELD_SUFFIX);
            final String fieldName = extractFieldName(fieldNameCellParsed.getLeft());
            final IOpenClass fieldType = getFieldType(bindingContext, row, rowSrc);
            FieldDescriptionBuilder fieldDescriptionBuilder = null;
            final String contextPropertyName = fieldNameCellParsed.getValue();
            validateContextPropertyName(row, contextPropertyName, fieldType, bindingContext);
            IOpenField field;
            if (isTransient) {
                field = new TransientOpenField(dataType, fieldName, fieldType, contextPropertyName);
            } else {
                field = new DatatypeOpenField(dataType, fieldName, fieldType, contextPropertyName);
            }
            try {
                if (!isTransient) {
                    if (fields.containsKey(fieldName)) {
                        throw SyntaxNodeExceptionUtils.createError(String.format("Field '%s' has already been defined.",
                            fieldName), null, null, getCellSource(row, bindingContext, 1));
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
                            String.format("Field '%s' conflicts with '%s' field.", fieldName, f),
                            null,
                            null,
                            getCellSource(row, bindingContext, 1));
                    }
                    fieldDescriptionBuilder = FieldDescriptionBuilder.create(field.getType().getJavaName());
                    fieldDescriptionBuilder.setContextPropertyName(contextPropertyName);
                }
                dataType.addField(field);
                dataType.getFields();
                if (firstNonTransientField.booleanValue()) {
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
                    firstNonTransientField.setValue(false);
                }
            } catch (SyntaxNodeException e) {
                throw e;
            } catch (Exception t) {
                throw SyntaxNodeExceptionUtils
                    .createError(t.getMessage(), t, null, getCellSource(row, bindingContext, 1));
            }

            if (fieldDescriptionBuilder != null) {
                fieldDescriptionBuilder.setContextPropertyName(contextPropertyName);
            }

            FieldDescription fieldDescription = null;
            if (row.getWidth() > 2) {
                String defaultValueCode = getDefaultValueCode(row, bindingContext);
                Object defaultValue = defaultValueCode;
                ConstantOpenField constantOpenField = RuleRowHelper.findConstantField(bindingContext, defaultValueCode);
                if (constantOpenField != null) {
                    defaultValue = constantOpenField.getValue();
                    if (fieldDescriptionBuilder != null) {
                        fieldDescriptionBuilder.setDefaultValue(defaultValue);
                        fieldDescriptionBuilder.setDefaultValueAsString(constantOpenField.getValueAsString());
                    }
                    if (!bindingContext.isExecutionMode()) {
                        ICell cell = getCellSource(row, bindingContext, 2).getCell();
                        MetaInfoReader metaInfoReader = tableSyntaxNode.getMetaInfoReader();
                        if (metaInfoReader instanceof BaseMetaInfoReader) {
                            SimpleNodeUsage nodeUsage = RuleRowHelper
                                .createConstantNodeUsage(constantOpenField, 0, defaultValueCode.length() - 1);
                            ((BaseMetaInfoReader<?>) metaInfoReader).addConstant(cell, nodeUsage);
                        }
                    }
                } else {
                    if (fieldDescriptionBuilder != null) {
                        fieldDescriptionBuilder.setDefaultValueAsString(defaultValueCode);
                    }
                    if (String.class != fieldType.getInstanceClass()) {
                        ICell theCellValue = row.getColumn(2).getCell(0, 0);
                        if (theCellValue.hasNativeType()) {
                            defaultValue = RuleRowHelper.loadNativeValue(theCellValue, fieldType);
                            if (defaultValue != null && fieldDescriptionBuilder != null) {
                                fieldDescriptionBuilder.setDefaultValue(defaultValue);
                            }
                        }
                    }
                }
                if (fieldDescriptionBuilder != null) {
                    try {
                        fieldDescription = fieldDescriptionBuilder.build();
                    } catch (RuntimeException e) {
                        String message = String.format("Cannot parse cell value '%s'", defaultValueCode);
                        IOpenSourceCodeModule cellSourceCodeModule = getCellSource(row, bindingContext, 2);
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
                            TextInterval location = defaultValueCode == null ? null
                                                                             : LocationUtils
                                                                                 .createTextInterval(defaultValueCode);
                            throw SyntaxNodeExceptionUtils.createError(message, e, location, cellSourceCodeModule);
                        }
                    }
                    if (defaultValue != null && !(fieldDescription.hasDefaultKeyWord() && fieldDescription.isArray())) {
                        // Validate not null default value
                        // The null value is allowed for alias types
                        try {
                            RuleRowHelper.validateValue(defaultValue, fieldType);
                        } catch (Exception e) {
                            throw SyntaxNodeExceptionUtils
                                .createError(e.getMessage(), e, null, getCellSource(row, bindingContext, 2));
                        }
                    }
                }
            }
            if (fieldDescription == null && fieldDescriptionBuilder != null) {
                fieldDescription = fieldDescriptionBuilder.build();
            }
            if (fieldDescription != null) {
                fields.put(fieldName, fieldDescription);
            }
        }

    }

    private String extractFieldName(String fieldName) {
        return fieldName.endsWith(NON_TRANSIENT_FIELD_SUFFIX) || fieldName.endsWith(TRANSIENT_FIELD_SUFFIX) ? fieldName
            .substring(0, fieldName.length() - 1) : fieldName;
    }

    private void validateContextPropertyName(final ILogicalTable row,
            final String contextPropertyName,
            final IOpenClass fieldType,
            final IBindingContext bindingContext) throws SyntaxNodeException {
        if (contextPropertyName != null) {
            if (DefaultRulesRuntimeContext.CONTEXT_PROPERTIES.get(contextPropertyName) == null) {
                throw SyntaxNodeExceptionUtils.createError(
                    String.format("Property '%s' is not found in context. Supported properties: [%s].",
                        contextPropertyName,
                        String.join(", ", DefaultRulesRuntimeContext.CONTEXT_PROPERTIES.keySet())),
                    getCellSource(row, bindingContext, 1));
            }
            if (dataType.getFields()
                .stream()
                .filter(f -> Objects.nonNull(f.getContextProperty()))
                .anyMatch(f -> f.getContextProperty().equals(contextPropertyName))) {
                throw SyntaxNodeExceptionUtils.createError(
                    String.format("Multiple fields refer to the same context property '%s'.", contextPropertyName),
                    getCellSource(row, bindingContext, 1));
            }
            IOpenClass contextPropertyType = JavaOpenClass
                .getOpenClass(DefaultRulesRuntimeContext.CONTEXT_PROPERTIES.get(contextPropertyName));
            IOpenCast openCast = bindingContext.getCast(fieldType, contextPropertyType);
            if (openCast == null || !openCast.isImplicit() && !contextPropertyType.getInstanceClass().isEnum()) {
                throw SyntaxNodeExceptionUtils.createError(
                    String.format("Type mismatch for context property '%s'. Cannot convert from '%s' to '%s'.",
                        contextPropertyName,
                        fieldType.getName(),
                        contextPropertyType.getName()),
                    getCellSource(row, bindingContext, 1));
            }
        }
    }

    public static String getDefaultValueCode(ILogicalTable row, IBindingContext cxt) throws OpenLCompilationException {
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
            String errorMessage = MessageUtils.getTypeNotFoundMessage(tableSrc.getCode());
            throw SyntaxNodeExceptionUtils.createError(errorMessage, null, null, tableSrc);
        }

        if (row.getWidth() < 2) {
            String errorMessage = "Bad table structure: expected {header} / {type | name}.";
            throw SyntaxNodeExceptionUtils.createError(errorMessage, null, null, tableSrc);
        }
        return fieldType;
    }

    @Override
    public void addTo(ModuleOpenClass openClass) {
        InternalDatatypeClass internalClassMember = new InternalDatatypeClass(dataType, openClass);
        tableSyntaxNode.setMember(internalClassMember);
    }

    @Override
    public void finalizeBind(IBindingContext bindingContext) throws Exception {
        try {
            if (!bindingContext.isExecutionMode()) {
                tableSyntaxNode.setMetaInfoReader(new DatatypeTableMetaInfoReader(this));
            }
            if (!byteCodeReadyToLoad) {
                return;
            }
            OpenLBundleClassLoader classLoader = (OpenLBundleClassLoader) Thread.currentThread()
                .getContextClassLoader();
            Class<?> datatypeClass = classLoader.loadClass(dataType.getJavaName());
            dataType.setInstanceClass(datatypeClass);
            moduleOpenClass.addType(dataType);
        } catch (ClassNotFoundException | LinkageError e) {
            throw SyntaxNodeExceptionUtils.createError(
                String.format("Failed to load a class for datatype '%s'.", dataType.getJavaName()),
                tableSyntaxNode);
        } finally {
            fields = null;
        }
    }

    public void generateByteCode(IBindingContext bindingContext) throws Exception {
        if (!generated) {
            if (generatingInProcess) {
                throw new OpenLCompilationException(
                    String.format("Cyclic inheritance involving '%s'", parentClassName));
            }
            generatingInProcess = true;
            try {
                if (parentClassName != null) {
                    IOpenClass parentOpenClass;
                    DatatypeTableBoundNode parentDatatypeTableBoundNode = getParentDatatypeTableBoundNode();
                    if (parentDatatypeTableBoundNode != null) {
                        parentDatatypeTableBoundNode.generateByteCode(bindingContext);
                        parentOpenClass = parentDatatypeTableBoundNode.getDataType();
                    } else {
                        parentOpenClass = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, parentClassName);
                    }
                    if (parentOpenClass == null) {
                        byteCodeReadyToLoad = true;
                        throw new OpenLCompilationException(
                            String.format("Parent class '%s' is not found.", parentClassName));
                    }

                    if (parentOpenClass.getInstanceClass() != null) {// parent class has
                        // errors
                        if (Modifier.isFinal(parentOpenClass.getInstanceClass().getModifiers())) {
                            throw new OpenLCompilationException(
                                String.format("Cannot inherit from final class '%s'.", parentClassName));
                        }
                        try {
                            parentOpenClass.getInstanceClass().getConstructor();
                        } catch (NoSuchMethodException e) {
                            throw new OpenLCompilationException(
                                String.format("Cannot inherit from class '%s'. Default constructor is not found.",
                                    parentClassName));
                        }
                    }

                    if (parentOpenClass instanceof DomainOpenClass) {
                        throw new OpenLCompilationException(
                            String.format("Parent class '%s' cannot be domain type.", parentClassName));
                    }
                    dataType.setSuperClass(parentOpenClass);
                }

                readFieldsAndGenerateByteCode(bindingContext);
            } finally {
                generated = true;
                generatingInProcess = false;
            }
        }
    }

    private void checkInheritedFieldsDuplication(final IBindingContext cxt) throws Exception {
        final IOpenClass superClass = dataType.getSuperClass();
        if (superClass != null) {
            SyntaxNodeExceptionCollector syntaxNodeExceptionCollector = new SyntaxNodeExceptionCollector();
            for (final IOpenField field : dataType.getDeclaredFields()) {
                syntaxNodeExceptionCollector.run(() -> {
                    IOpenField fieldInParent = superClass.getField(field.getName());
                    if (fieldInParent != null) {
                        if (fieldInParent.getType().getInstanceClass().equals(field.getType().getInstanceClass())) {
                            BindHelper.processWarn(String.format("Field [%s] has been already defined in class '%s'",
                                field.getName(),
                                fieldInParent.getDeclaringClass().getDisplayName(0)), tableSyntaxNode, cxt);
                        } else {
                            throw SyntaxNodeExceptionUtils.createError(
                                String.format("Field [%s] has been already defined in class '%s' with another type",
                                    field.getName(),
                                    fieldInParent.getDeclaringClass().getDisplayName(0)),
                                tableSyntaxNode);
                        }
                    }
                });
            }
            syntaxNodeExceptionCollector.throwIfAny();
        }
    }

    @Override
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

    public DatatypeTableBoundNode getParentDatatypeTableBoundNode() {
        return parentDatatypeTableBoundNode;
    }

    public void setParentDatatypeTableBoundNode(DatatypeTableBoundNode parentDatatypeTableBoundNode) {
        this.parentDatatypeTableBoundNode = parentDatatypeTableBoundNode;
    }
}
