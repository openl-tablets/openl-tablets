
package org.openl.rules.datatype.binding;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlTransient;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.classloader.OpenLClassLoader;
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
import org.openl.rules.lang.xls.types.meta.BaseMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.DatatypeTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.types.NullOpenClass;
import org.openl.util.ParserUtils;
import org.openl.util.TableNameChecker;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.InternalDatatypeClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ArrayUtils;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bound node for datatype table component.
 *
 * @author snshor
 *
 */
public class DatatypeTableBoundNode implements IMemberBoundNode {

    private static final Pattern CONTEXT_SPLITTER = Pattern.compile("\\s*:\\s*context\\s*");
    private static final String NON_TRANSIENT_FIELD_SUFFIX = "*";
    private static final String TRANSIENT_FIELD_SUFFIX = "~";
    private static final Logger LOG = LoggerFactory.getLogger(DatatypeTableBoundNode.class);

    private final TableSyntaxNode tableSyntaxNode;
    private final DatatypeOpenClass dataType;
    private final IdentifierNode parentClassIdentifier;
    private final String parentClassName;
    private final ModuleOpenClass moduleOpenClass;

    private DatatypeTableBoundNode parentDatatypeTableBoundNode;
    private boolean generated;
    private boolean generatingInProcess;
    private boolean byteCodeReadyToLoad;

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

    public String getParentClassName() {
        return parentClassName;
    }

    /**
     * Process datatype fields from source table.
     *
     * @param bindingContext binding context
     */
    private void readFieldsAndGenerateByteCode(final IBindingContext bindingContext) {

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
        boolean useTransientSuffix = true;
        for (int i = 0; i < tableHeight; i++) {
            if (fieldNameEndsWithNonTransientSuffix(dataTable.getRow(i), bindingContext)) {
                useTransientSuffix = false;
                break;
            }
        }
        bindingContext.pushErrors();
        List<SyntaxNodeException> errors;
        try {
            for (int i = 0; i < tableHeight; i++) {
                processRow(dataTable.getRow(i), bindingContext, fields, i == 0, useTransientSuffix);
            }
            checkInheritedFieldsDuplication(bindingContext);
        } finally {
            errors = bindingContext.popErrors();
            errors.forEach(bindingContext::addError);
        }
        if (errors.isEmpty() && beanClassCanBeGenerated(bindingContext)) {
            String datatypeClassName = dataType.getJavaName();
            OpenLClassLoader classLoader = (OpenLClassLoader) Thread.currentThread()
                .getContextClassLoader();
            try {
                Class<?> beanClass = classLoader.loadClass(datatypeClassName);
                byteCodeReadyToLoad = true;
                validateDatatypeClass(beanClass, fields, bindingContext);
                LOG.debug("Class '{}' is loaded from classloader.", datatypeClassName);
            } catch (ClassNotFoundException e) {
                try {
                    final byte[] byteCode = buildByteCodeForDatatype(fields);
                    classLoader.addGeneratedClass(datatypeClassName, byteCode);
                    dataType.setBytecode(byteCode);
                    byteCodeReadyToLoad = true;
                    LOG.debug("Class '{}' is generated and loaded to classloader.", datatypeClassName);
                } catch (ByteCodeGenerationException e1) {
                    LOG.debug("Error occurred: ", e1);
                    String errorMessage = String
                        .format("Failed to generate a class for datatype '%s'. %s", datatypeClassName, e1.getMessage());
                    BindHelper.processError(errorMessage, e1, tableSyntaxNode, bindingContext);
                } catch (Exception e2) {
                    LOG.debug("Error occurred: ", e2);
                    String errorMessage = String.format("Failed to generate a class for datatype '%s'.",
                        datatypeClassName);
                    BindHelper.processError(errorMessage, e2, tableSyntaxNode, bindingContext);
                }
            }
        }
    }

    private boolean beanClassCanBeGenerated(IBindingContext cxt) {
        if (parentClassName != null) {
            IOpenClass parentClass = cxt.findType(ISyntaxConstants.THIS_NAMESPACE, parentClassName);
            return parentClass != null;
        }
        return true;
    }

    private void extractParentFields(DatatypeTableBoundNode datatypeTableBoundNode,
            LinkedHashMap<String, FieldDescription> parentFields,
            Set<DatatypeTableBoundNode> used) {
        if (datatypeTableBoundNode.parentDatatypeTableBoundNode != null) {
            if (used.contains(datatypeTableBoundNode.parentDatatypeTableBoundNode)) {
                return;
            }
            used.add(datatypeTableBoundNode.parentDatatypeTableBoundNode);
            extractParentFields(datatypeTableBoundNode.parentDatatypeTableBoundNode, parentFields, used);
            parentFields.putAll(datatypeTableBoundNode.parentDatatypeTableBoundNode.getFields());
        } else {
            if (datatypeTableBoundNode.dataType.getSuperClass() != null) {
                for (IOpenField field : datatypeTableBoundNode.dataType.getSuperClass().getFields()) {
                    parentFields.put(field.getName(), new FieldDescription(field.getType().getJavaName()));
                }
            }
        }
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
                LinkedHashMap<String, FieldDescription> parentFields = new LinkedHashMap<>();
                extractParentFields(this, parentFields, new HashSet<>());
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
            Map<String, FieldDescription> fields,
            IBindingContext cxt) {
        String datatypeClassName = dataType.getJavaName();
        IOpenClass superClass = dataType.getSuperClass();
        if (superClass != null && !datatypeClass.getSuperclass().getName().equals(superClass.getJavaName())) {
            String errorMessage = String.format(
                "Invalid parent class in class '%s'. Please, update the class to be compatible with the datatype.",
                datatypeClassName);
            BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
        }

        try {
            datatypeClass.getConstructor();
        } catch (NoSuchMethodException e) {
            String errorMessage = String.format(
                "Default constructor is not found in class '%s'. " + "Please, update the class to be compatible with the datatype.",
                datatypeClassName);
            BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
        }

        Object instance = null;
        try {
            instance = datatypeClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            LOG.debug("Error occurred: ", e);
            String errorMessage = String.format(
                "Default constructor is not found in class '%s' or the class is not instantiatable. " + "Please, update the class to be compatible with the datatype.",
                datatypeClassName);
            BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
        }

        for (Entry<String, FieldDescription> fieldEntry : fields.entrySet()) {
            String fieldName = fieldEntry.getKey();
            FieldDescription fieldDescription = fieldEntry.getValue();
            try {
                Field field = datatypeClass.getDeclaredField(fieldName);
                if (fieldDescription.isTransient() != Modifier.isTransient(field.getModifiers()) || (fieldDescription
                    .isTransient() && !field.isAnnotationPresent(XmlTransient.class)) || (!fieldDescription
                        .isTransient() && field.isAnnotationPresent(XmlTransient.class))) {
                    String errorMessage = String.format("Field '%s' is " + (fieldDescription
                        .isTransient() ? "not "
                                       : "") + "transient in class '%s'. " + "Please, update the class to be compatible with the datatype.",
                        fieldName,
                        datatypeClassName);
                    BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
                }
            } catch (NoSuchFieldException e) {
                LOG.debug("Error occurred: ", e);
                String errorMessage = String.format(
                    "%s '%s' is not found in class '%s'. " + "Please, update the class to be compatible with the datatype.",
                    dataType.isStatic() ? "Static field" : "Field",
                    fieldName,
                    datatypeClassName);
                BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
            }

            String name = ClassUtils.capitalize(fieldName); // According to JavaBeans v1.01
            Method getterMethod = null;
            try {
                getterMethod = datatypeClass.getMethod("get" + name);
                if ((fieldDescription.isTransient() && !getterMethod
                    .isAnnotationPresent(XmlTransient.class)) || (!fieldDescription.isTransient() && getterMethod
                        .isAnnotationPresent(XmlTransient.class))) {
                    String errorMessage = String.format("Field '%s' is " + (fieldDescription
                        .isTransient() ? "not "
                                       : "") + "transient in class '%s'. " + "Please, update the class to be compatible with the datatype.",
                        fieldName,
                        datatypeClassName);
                    BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
                }
            } catch (NoSuchMethodException e) {
                String errorMessage = String.format(
                    "Method 'get%s' is not found in class '%s'. " + "Please, update the class to be compatible with the datatype.",
                    name,
                    datatypeClassName);
                name = StringUtils.capitalize(fieldName); // Try old solution (before 5.21.7)
                try {
                    getterMethod = datatypeClass.getMethod("get" + name);
                } catch (NoSuchMethodException e1) {
                    BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
                }
            }
            if (getterMethod != null) {
                if (!getterMethod.getReturnType().getName().equals(fieldDescription.getTypeName())) {
                    String errorMessage = String.format(
                        "Unexpected return type for method '%s' in class '%s'. " + "Please, update the class to be compatible with the datatype.",
                        getterMethod.getName(),
                        datatypeClassName);
                    BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
                } else if (!Modifier.isPublic(getterMethod.getModifiers())) {
                    String errorMessage = String.format(
                        "Unexpected access modifier on method '%s' in class '%s'. " + "Please, update the class to be compatible with the datatype.",
                        getterMethod.getName(),
                        datatypeClassName);
                    BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
                } else if (instance != null && fieldEntry.getValue().getDefaultValue() != null) {
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
                    } catch (ReflectiveOperationException | LinkageError e) {
                        LOG.debug("Ignored error: ", e);
                    }
                    if (f) {
                        String errorMessage = String.format(
                            "Default value for field '%s' in class '%s' mismatches " + "default value is used in datatype '%s'. " + "Please, update the class to be compatible with the datatype.",
                            fieldEntry.getKey(),
                            datatypeClassName,
                            dataType.getName());
                        BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
                    }
                }
            }

            String setterMethodName = "set" + name;
            Method[] methods = datatypeClass.getMethods();
            boolean found = false;
            for (Method method : methods) {
                if (method.getName()
                    .equals(setterMethodName) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0]
                        .getName()
                        .equals(fieldDescription.getTypeName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                String errorMessage = String.format(
                    "Method '%s(%s)' is not found in class '%s'. " + "Please, update the class to be compatible with the datatype.",
                    setterMethodName,
                    fieldDescription.getTypeName(),
                    datatypeClassName);
                BindHelper.processError(errorMessage, tableSyntaxNode, cxt);

            }
        }
        if (parentDatatypeTableBoundNode != null) {
            if (datatypeClass.getSuperclass() == null || !Objects.equals(
                parentDatatypeTableBoundNode.getDataType().getJavaName(),
                datatypeClass.getSuperclass().getName())) {
                String errorMessage = String.format(
                    "Invalid parent class '%s' is found in class '%s'. " + "Please, update the class to be compatible with the datatype.",
                    datatypeClass.getSuperclass() != null ? (" " + datatypeClass.getSuperclass().getTypeName()) : "",
                    datatypeClassName);
                BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
            }
            for (Entry<String, FieldDescription> fieldEntry : parentDatatypeTableBoundNode.getFields().entrySet()) {
                try {
                    Field f = datatypeClass.getSuperclass().getDeclaredField(fieldEntry.getKey());
                    if (!Modifier.isPublic(f.getModifiers()) && !Modifier.isProtected(f.getModifiers())) {
                        String errorMessage = String.format(
                            "Invalid access modifier is found on field '%s' in class '%s'. " + "Please, update the class to be compatible with the datatype.",
                            fieldEntry.getKey(),
                            datatypeClass.getSuperclass().getTypeName());
                        BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
                    }
                } catch (NoSuchFieldException e) {
                    LOG.debug("Ignored error: ", e);
                }
            }
            boolean g = false;
            LinkedList<FieldDescription> parentFields = new LinkedList<>();
            DatatypeTableBoundNode p = parentDatatypeTableBoundNode;
            while (p != null) {
                LinkedList<FieldDescription> x = new LinkedList<>();
                for (FieldDescription fieldDescription : p.getFields().values()) {
                    x.addFirst(fieldDescription);
                }
                for (FieldDescription fieldDescription : x) {
                    parentFields.addFirst(fieldDescription);
                }
                p = p.parentDatatypeTableBoundNode;
            }
            for (Constructor<?> constructor : datatypeClass.getSuperclass().getConstructors()) {
                if (constructor.getParameterCount() == parentFields.size()) {
                    int i = 0;
                    boolean f = true;
                    for (FieldDescription fieldDescription : parentFields) {
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
                    "Mandatory constructor with parameters is not found in class '%s'. " + "Please, update the class to be compatible with the datatype.",
                    datatypeClass.getSuperclass().getTypeName());
                BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
            }
        }
    }

    private static boolean fieldNameEndsWithNonTransientSuffix(ILogicalTable row, IBindingContext bindingContext) {
        GridCellSourceCodeModule nameCellSource = getCellSource(row, bindingContext, 1);
        String rawFieldName = nameCellSource.getCode();
        return rawFieldName.matches("^[^\\s]+\\*(\\s*:\\s*context.*)?$");
    }

    private void processRow(ILogicalTable row,
            IBindingContext bindingContext,
            Map<String, FieldDescription> fields,
            boolean firstRow,
            boolean useTransientSuffix) {
        GridCellSourceCodeModule typeCellSource = getCellSource(row, bindingContext, 0);
        if (ParserUtils.isBlankOrCommented(typeCellSource.getCode())) {
            return;
        }
        if (row.getWidth() < 2) {
            String errorMessage = "Bad table structure: expected {header} / {type | name}.";
            BindHelper.processError(errorMessage, typeCellSource, bindingContext);
            return;
        }
        IOpenClass fieldType = RuleRowHelper.getType(typeCellSource.getCode(), typeCellSource, bindingContext);
        if (fieldType == NullOpenClass.the) {
            fieldType = JavaOpenClass.OBJECT;
        }
        GridCellSourceCodeModule nameCellSource = getCellSource(row, bindingContext, 1);
        final String code = nameCellSource.getCode();
        String contextPropertyName;
        String[] parts = CONTEXT_SPLITTER.split(code, 2);
        String rawFieldName = parts[0];
        final boolean isTransient = useTransientSuffix ? rawFieldName.endsWith(TRANSIENT_FIELD_SUFFIX)
                                                       : !rawFieldName.endsWith(NON_TRANSIENT_FIELD_SUFFIX);
        String fieldName = extractFieldName(rawFieldName);
        if (TableNameChecker.isInvalidJavaIdentifier(fieldName)) {
            String errorMessage = String.format("Bad field name: '%s'.", fieldName);
            BindHelper.processError(errorMessage, nameCellSource, bindingContext);
            return;
        }
        if (parts.length > 1) {
            contextPropertyName = parts[1];
            if (contextPropertyName.isEmpty()) {
                contextPropertyName = fieldName;
            } else if (contextPropertyName.startsWith(".")) {
                contextPropertyName = StringUtils.trim(contextPropertyName.substring(1));
                if (TableNameChecker.isInvalidJavaIdentifier(contextPropertyName)) {
                    String errorMessage = String.format("Bad context property name: '%s'.", contextPropertyName);
                    BindHelper.processError(errorMessage, nameCellSource, bindingContext);
                    return;
                }
            }
        } else {
            contextPropertyName = null;
        }

        FieldDescriptionBuilder fieldDescriptionBuilder;
        validateContextPropertyName(row, contextPropertyName, fieldType, bindingContext);
        if (fields.containsKey(fieldName)) {
            String errorMessage = String.format("Field '%s' is already declared.", fieldName);
            BindHelper.processError(errorMessage, nameCellSource, bindingContext);
            return;
        } else if (fields.containsKey(ClassUtils.decapitalize(fieldName)) || fields
            .containsKey(ClassUtils.capitalize(fieldName))) {
            String f = null;
            if (fields.containsKey(ClassUtils.decapitalize(fieldName))) {
                f = ClassUtils.decapitalize(fieldName);
            }
            if (fields.containsKey(ClassUtils.capitalize(fieldName))) {
                f = ClassUtils.capitalize(fieldName);
            }
            String errorMessage = String.format("Field '%s' conflicts with field '%s'.", fieldName, f);
            BindHelper.processError(errorMessage, nameCellSource, bindingContext);
        }
        fieldDescriptionBuilder = FieldDescriptionBuilder.create(fieldType.getJavaName())
            .setTransient(isTransient)
            .setContextPropertyName(contextPropertyName);

        fieldDescriptionBuilder.setContextPropertyName(contextPropertyName);

        FieldDescription fieldDescription;
        Object defaultValue = null;
        GridCellSourceCodeModule defaultValueCellSource = null;
        String defaultValueCode = null;
        if (row.getWidth() > 2) {
            defaultValueCellSource = getCellSource(row, bindingContext, 2);
            defaultValueCode = defaultValueCellSource.getCode();
            if (ParserUtils.isBlankOrCommented(defaultValueCode)) {
                defaultValueCode = null;
            }
            defaultValue = defaultValueCode;
            ConstantOpenField constantOpenField = RuleRowHelper.findConstantField(bindingContext, defaultValueCode);
            if (constantOpenField != null) {
                defaultValue = constantOpenField.getValue();
                fieldDescriptionBuilder.setDefaultValue(defaultValue);
                fieldDescriptionBuilder.setDefaultValueAsString(constantOpenField.getValueAsString());
                if (!bindingContext.isExecutionMode()) {
                    ICell cell = defaultValueCellSource.getCell();
                    MetaInfoReader metaInfoReader = tableSyntaxNode.getMetaInfoReader();
                    if (metaInfoReader instanceof BaseMetaInfoReader) {
                        SimpleNodeUsage nodeUsage = RuleRowHelper
                            .createConstantNodeUsage(constantOpenField, 0, defaultValueCode.length() - 1);
                        ((BaseMetaInfoReader<?>) metaInfoReader).addConstant(cell, nodeUsage);
                    }
                }
            } else {
                fieldDescriptionBuilder.setDefaultValueAsString(defaultValueCode);
                if (String.class != fieldType.getInstanceClass()) {
                    ICell theCellValue = row.getColumn(2).getCell(0, 0);
                    if (theCellValue.hasNativeType()) {
                        defaultValue = RuleRowHelper.loadNativeValue(theCellValue, fieldType);
                        if (defaultValue != null) {
                            fieldDescriptionBuilder.setDefaultValue(defaultValue);
                        }
                    }
                }
            }

        }

        try {
            fieldDescription = fieldDescriptionBuilder.build();
            if (defaultValue != null && !(fieldDescription.hasDefaultKeyWord() && fieldDescription.isArray())) {
                // Validate not null default value
                // The null value is allowed for alias types
                try {
                    RuleRowHelper.validateValue(defaultValue, fieldType);
                } catch (Exception e) {
                    BindHelper.processError(e, defaultValueCellSource, bindingContext);
                }
            }
            fields.put(fieldName, fieldDescription);
        } catch (RuntimeException e) {
            String errorMessage = String.format("Cannot parse cell value '%s'.", defaultValueCode);
            BindHelper.processError(errorMessage, e, defaultValueCellSource, bindingContext);
        }

        IOpenField field = new DatatypeOpenField(dataType, fieldName, fieldType, contextPropertyName, isTransient);
        dataType.addField(field);
        if (firstRow) {
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
    }

    private static String extractFieldName(String fieldName) {
        return fieldName.endsWith(NON_TRANSIENT_FIELD_SUFFIX) || fieldName.endsWith(TRANSIENT_FIELD_SUFFIX) ? fieldName
            .substring(0, fieldName.length() - 1) : fieldName;
    }

    private void validateContextPropertyName(final ILogicalTable row,
            final String contextPropertyName,
            final IOpenClass fieldType,
            final IBindingContext bindingContext) {
        if (contextPropertyName != null) {
            String errorMessage = null;
            Class<?> contextPropertyClass = DefaultRulesRuntimeContext.CONTEXT_PROPERTIES.get(contextPropertyName);
            if (contextPropertyClass == null) {
                errorMessage = String.format("Property '%s' is not found in the context. Available properties: [%s].",
                    contextPropertyName,
                    String.join(", ", DefaultRulesRuntimeContext.CONTEXT_PROPERTIES.keySet()));
            } else if (dataType.getFields()
                .stream()
                .filter(f -> Objects.nonNull(f.getContextProperty()))
                .anyMatch(f -> f.getContextProperty().equals(contextPropertyName))) {
                errorMessage = String.format("Multiple fields refer to the same context property '%s'.",
                    contextPropertyName);
            } else {
                IOpenClass contextPropertyType = JavaOpenClass.getOpenClass(contextPropertyClass);
                IOpenCast openCast;
                try {
                    openCast = bindingContext.getCast(fieldType, contextPropertyType);
                } catch (NullPointerException e) {
                    // FIXME: fieldType instance class have to be defined to prevent multiple NPEs in CastFactory
                    openCast = null;
                }
                if (openCast == null || (!openCast.isImplicit() && !contextPropertyType.getInstanceClass().isEnum())) {
                    errorMessage = String.format(
                        "Type mismatch for context property '%s'. Cannot convert from '%s' to '%s'.",
                        contextPropertyName,
                        fieldType.getName(),
                        contextPropertyType.getName());
                }
            }
            if (errorMessage != null) {
                GridCellSourceCodeModule cellSource = getCellSource(row, bindingContext, 1);
                BindHelper.processError(errorMessage, cellSource, bindingContext);
            }
        }
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
            OpenLClassLoader classLoader = (OpenLClassLoader) Thread.currentThread()
                .getContextClassLoader();
            Class<?> datatypeClass = classLoader.loadClass(dataType.getJavaName());
            dataType.setInstanceClass(datatypeClass);
            moduleOpenClass.addType(dataType);
        } catch (ClassNotFoundException | LinkageError e) {
            LOG.debug("Error occurred: ", e);
            String errorMessage = String.format("Failed to load a class for datatype '%s'.", dataType.getJavaName());
            BindHelper.processError(errorMessage, e, tableSyntaxNode, bindingContext);
        } finally {
            fields = null;
        }
    }

    public void generateByteCode(IBindingContext bindingContext) throws Exception {
        if (!generated) {
            if (generatingInProcess) {
                throw new OpenLCompilationException(String
                    .format("Circular dependency with respect to inheritance '%s' is detected.", parentClassName));
            }
            generatingInProcess = true;
            try {
                if (parentClassName != null) {
                    IOpenClass parentOpenClass;
                    DatatypeTableBoundNode parentBoundNode = getParentDatatypeTableBoundNode();
                    if (parentBoundNode != null) {
                        parentBoundNode.generateByteCode(bindingContext);
                        parentOpenClass = parentBoundNode.getDataType();
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
                            String.format("Parent class '%s' cannot be a domain type.", parentClassName));
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

    private void checkInheritedFieldsDuplication(final IBindingContext cxt) {
        final IOpenClass superClass = dataType.getSuperClass();
        if (superClass != null) {
            for (final IOpenField field : dataType.getDeclaredFields()) {
                IOpenField fieldInParent = superClass.getField(field.getName());
                if (fieldInParent != null) {
                    if (Objects.equals(fieldInParent.getType(), field.getType())) {
                        BindHelper.processWarn(String.format("Field '%s' is already declared in parent class '%s'.",
                            field.getName(),
                            fieldInParent.getDeclaringClass().getDisplayName(0)), tableSyntaxNode, cxt);
                    } else {
                        String errorMessage = String.format(
                            "Field '%s' is already declared in class '%s' with another type.",
                            field.getName(),
                            fieldInParent.getDeclaringClass().getDisplayName(0));
                        BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
                    }
                }
            }
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
