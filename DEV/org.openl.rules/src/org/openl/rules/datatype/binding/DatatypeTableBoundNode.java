
package org.openl.rules.datatype.binding;

import static org.openl.rules.datatype.binding.DatatypeHelper.COLUMN_TITLES;
import static org.openl.rules.datatype.binding.DatatypeHelper.DEFAULT_COLUMN_TITLE;
import static org.openl.rules.datatype.binding.DatatypeHelper.DESCRIPTION_COLUMN_TITLE;
import static org.openl.rules.datatype.binding.DatatypeHelper.EXAMPLE_COLUMN_TITLE;
import static org.openl.rules.datatype.binding.DatatypeHelper.MANDATORY_COLUMN_TITLE;
import static org.openl.rules.datatype.binding.DatatypeHelper.NAME_COLUMN_TITLE;
import static org.openl.rules.datatype.binding.DatatypeHelper.TYPE_COLUMN_TITLE;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jakarta.xml.bind.annotation.XmlTransient;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.module.ContextPropertyBinderUtils;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.classloader.OpenLClassLoader;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLCompilationException;
import org.openl.gen.ByteCodeGenerationException;
import org.openl.gen.FieldDescription;
import org.openl.gen.TypeDescription;
import org.openl.gen.writers.DefaultValue;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.datatype.gen.FieldDescriptionBuilder;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenField;
import org.openl.rules.lang.xls.types.meta.BaseMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.DatatypeTableMetaInfoReader;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.InternalDatatypeClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ArrayUtils;
import org.openl.util.ClassUtils;
import org.openl.util.OpenClassUtils;
import org.openl.util.ParserUtils;
import org.openl.util.StringUtils;
import org.openl.util.TableNameChecker;

/**
 * Bound node for datatype table component.
 *
 * @author snshor
 */
@Slf4j
public class DatatypeTableBoundNode implements IMemberBoundNode {

    private static final String COMMA_SEPARATED_COLUMN_TITLES = COLUMN_TITLES.stream()
            .map(e -> "'" + e + "'")
            .collect(Collectors.joining(", "));

    private static final Map<String, Integer> DEFAULT_COLUMN_TITLES_ORDER = Map.of(
            TYPE_COLUMN_TITLE, 0,
            NAME_COLUMN_TITLE, 1,
            DEFAULT_COLUMN_TITLE, 2
    );

    private static final Pattern CONTEXT_SPLITTER = Pattern.compile("\\s*:\\s*context\\s*");
    public static final String NON_TRANSIENT_FIELD_SUFFIX = "*";
    public static final String TRANSIENT_FIELD_SUFFIX = "~";

    @Getter
    private final TableSyntaxNode tableSyntaxNode;
    @Getter
    private final DatatypeOpenClass dataType;
    @Getter
    private final IdentifierNode parentClassIdentifier;
    @Getter
    private final String parentClassName;
    private final ModuleOpenClass moduleOpenClass;

    @Getter
    @Setter
    private DatatypeTableBoundNode parentDatatypeTableBoundNode;
    private boolean generated;
    private boolean generatingInProcess;
    private boolean byteCodeReadyToLoad;

    @Getter
    private ILogicalTable table;
    private final OpenL openl;

    @Getter(AccessLevel.PRIVATE)
    @Setter
    private Map<String, FieldDescription> fields;

    @Getter
    private Map<String, Integer> columnTitlesOrder;

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

    /**
     * Process datatype fields from source table.
     *
     * @param bindingContext binding context
     */
    private void readFieldsAndGenerateByteCode(final IBindingContext bindingContext) {

        final ILogicalTable dataTable = DatatypeHelper.getNormalizedDataPartTable(table, openl, bindingContext);
        // Save normalized table to work with it later
        this.table = dataTable;

        var tableHeight = 0;

        if (dataTable != null) {
            tableHeight = dataTable.getHeight();
        }

        // map of fields that will be used for byte code generation.
        // key: name of the field, value: field type.
        //
        fields = new LinkedHashMap<>();
        bindingContext.pushErrors();
        List<SyntaxNodeException> errors;
        try {
            this.columnTitlesOrder = getDatatypeColumnOrder(dataTable, tableHeight, bindingContext);
            if (tableHeight > 0 && dataTable.getWidth() < 2) {
                BindHelper.processError("Bad table structure: expected {header} / {type | name}.", tableSyntaxNode, bindingContext);
                return;
            }
            // columnTitlesOrder always contains TYPE_COLUMN_TITLE and NAME_COLUMN_TITLE:
            // - New design: both headers are present (checked in getDatatypeColumnOrder)
            // - Legacy design: DEFAULT_COLUMN_TITLES_ORDER is returned which has them at positions 0 and 1
            int firstRow = columnTitlesOrder == DEFAULT_COLUMN_TITLES_ORDER ? 0 : 1;
            int nameColumn = columnTitlesOrder.getOrDefault(NAME_COLUMN_TITLE, 1);
            var useTransientSuffix = !anyFieldMarkedNonTransient(dataTable, firstRow, tableHeight, nameColumn,
                    bindingContext);
            var indexFieldDeclared = false;
            for (var i = firstRow; i < tableHeight; i++) {
                var field = processRow(dataTable, dataTable.getRow(i), bindingContext, fields, columnTitlesOrder, useTransientSuffix);
                if (field != null && !indexFieldDeclared) {
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
                    indexFieldDeclared = true;
                }
            }
            validateInheritedFieldsDuplication(bindingContext);
            validateContextPropertyFields(bindingContext);
        } finally {
            errors = bindingContext.popErrors();
            errors.forEach(bindingContext::addError);
        }
        if (errors.isEmpty() && beanClassCanBeGenerated(bindingContext)) {
            var datatypeClassName = dataType.getJavaName();
            var classLoader = (OpenLClassLoader) Thread.currentThread().getContextClassLoader();
            try {
                var beanClass = classLoader.loadClass(datatypeClassName);
                byteCodeReadyToLoad = true;
                validateDatatypeClass(beanClass, fields, bindingContext);
                log.debug("Class '{}' is loaded from classloader.", datatypeClassName);
            } catch (ClassNotFoundException e) {
                try {
                    final var byteCode = buildByteCodeForDatatype(fields);
                    classLoader.addGeneratedClass(datatypeClassName, byteCode);
                    dataType.setBytecode(byteCode);
                    byteCodeReadyToLoad = true;
                    log.debug("Class '{}' is generated and loaded to classloader.", datatypeClassName);
                } catch (ByteCodeGenerationException e1) {
                    log.debug("Error occurred: ", e1);
                    var errorMessage = "Failed to generate a class for datatype '%s'. %s"
                            .formatted(datatypeClassName, e1.getMessage());
                    BindHelper.processError(errorMessage, e1, tableSyntaxNode, bindingContext);
                } catch (Exception e2) {
                    log.debug("Error occurred: ", e2);
                    var errorMessage = "Failed to generate a class for datatype '%s'.".formatted(
                            datatypeClassName);
                    BindHelper.processError(errorMessage, e2, tableSyntaxNode, bindingContext);
                }
            }
        }
    }

    private Map<String, Integer> getDatatypeColumnOrder(ILogicalTable dataTable, int tableHeight, IBindingContext cxt) {
        if (tableHeight == 0) {
            return DEFAULT_COLUMN_TITLES_ORDER;
        }

        // Whether the first row titles the columns is the one rule every reader of a datatype shares; only the
        // way a cell's text is read is the binder's own.
        var firstRow = dataTable.getRow(0);
        if (DatatypeHelper.hasColumnTitles(dataTable.getWidth(),
                i -> getCellSource(firstRow, cxt, i).getCode())) {
            var columnTitlesOrder = new HashMap<String, Integer>();
            for (var i = 0; i < dataTable.getWidth(); i++) {
                var cellSource = getCellSource(dataTable.getRow(0), cxt, i);
                var title = cellSource.getCode();
                if (StringUtils.isNotBlank(title)) {
                    if (columnTitlesOrder.containsKey(title)) {
                        BindHelper.processError("Column title '%s' is duplicated.".formatted(title), cellSource, cxt);
                    } else if (!COLUMN_TITLES.contains(title)) {
                        BindHelper.processError("Column title '%s' is not allowed. The title must be one of: %s".formatted(title, COMMA_SEPARATED_COLUMN_TITLES), cellSource, cxt);
                    } else {
                        columnTitlesOrder.put(title, i);
                    }
                }
            }
            return columnTitlesOrder;
        }

        // Legacy design - use positional columns
        // Show warning if more than 3 columns
        if (dataTable.getWidth() > 3) {
            BindHelper.processWarn(
                    """
                            Datatype %s uses legacy column layout without headers and has more than 3 columns. \
                            For backward compatibility only the first three positional columns (Type, Name, Default) are used. \
                            To enable additional column types, add explicit column headers: %s""".formatted(dataType.getName(), COMMA_SEPARATED_COLUMN_TITLES),
                    tableSyntaxNode,
                    cxt);
        }
        return DEFAULT_COLUMN_TITLES_ORDER;
    }

    private void validateContextPropertyFields(IBindingContext bindingContext) {
        var contextPropertiesCounter = new HashMap<String, Integer>();
        dataType.getFields()
                .stream()
                .filter(f -> Objects.nonNull(f.getContextProperty()))
                .forEach(e -> contextPropertiesCounter.merge(e.getContextProperty(), 1, Integer::sum));
        for (Entry<String, Integer> entry : contextPropertiesCounter.entrySet()) {
            if (entry.getValue() > 1) {
                var errorMessage = "Multiple fields refer to the same context property '%s'.".formatted(
                        entry.getKey());
                BindHelper.processError(errorMessage, tableSyntaxNode, bindingContext);
            }
        }
    }

    private boolean beanClassCanBeGenerated(IBindingContext cxt) {
        if (parentClassName != null) {
            var parentClass = cxt.findType(parentClassName);
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
        var datatypeClassName = dataType.getJavaName();
        var superOpenClass = dataType.getSuperClass();
        var beanBuilder = new JavaBeanClassBuilder(datatypeClassName);
        if (superOpenClass != null) {
            beanBuilder.setParentType(new TypeDescription(superOpenClass.getJavaName()));
            if (superOpenClass instanceof DatatypeOpenClass) {
                var parentFields = new LinkedHashMap<String, FieldDescription>();
                extractParentFields(this, parentFields, new HashSet<>());
                for (Entry<String, FieldDescription> field : parentFields.entrySet()) {
                    beanBuilder.addParentField(field.getKey(), field.getValue());
                }
            }
        }
        beanBuilder.addFields(fields);
        return beanBuilder.byteCode();
    }

    private void validateDatatypeClass(Class<?> datatypeClass,
                                       Map<String, FieldDescription> fields,
                                       IBindingContext cxt) {
        var datatypeClassName = dataType.getJavaName();
        var superClass = dataType.getSuperClass();
        if (superClass != null && !datatypeClass.getSuperclass().getName().equals(superClass.getJavaName())) {
            var errorMessage = "Invalid parent class in the '%s' class. Update the class so that it is compatible with the datatype.\n".formatted(
                    datatypeClassName);
            BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
        }

        try {
            datatypeClass.getConstructor();
        } catch (NoSuchMethodException e) {
            var errorMessage = "Default constructor is not found in the '%s' class. \" + \" Update the class so that it is compatible with the datatype.".formatted(
                    datatypeClassName);
            BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
        }

        Object instance = null;
        try {
            instance = datatypeClass.getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
                 InvocationTargetException e) {
            log.debug("Error occurred: ", e);
            String errorMessage = """
                    Default constructor is not found in class '%s' or the class is not instantiatable. \
                    Please, update the class to be compatible with the datatype.""".formatted(
                    datatypeClassName);
            BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
        }

        for (Entry<String, FieldDescription> fieldEntry : fields.entrySet()) {
            var fieldName = fieldEntry.getKey();
            var fieldDescription = fieldEntry.getValue();
            try {
                var field = datatypeClass.getDeclaredField(fieldName);
                if (fieldDescription.isTransient() != Modifier.isTransient(field.getModifiers()) || (fieldDescription
                        .isTransient() && !field.isAnnotationPresent(XmlTransient.class)) || (!fieldDescription
                        .isTransient() && field.isAnnotationPresent(XmlTransient.class))) {
                    String errorMessage = ("The '%s' field is " + (fieldDescription
                            .isTransient() ? "not "
                            : "") + "transient in the '%s' class. "
                            + "Update the class so that it is compatible with the datatype.").formatted(
                            fieldName,
                            datatypeClassName);
                    BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
                }
            } catch (NoSuchFieldException e) {
                log.debug("Error occurred: ", e);
                String errorMessage = """
                        The '%s' %s is not found in the '%s' class. \
                        Update the class so that it is compatible with the datatype.""".formatted(
                        fieldName,
                        dataType.isStatic() ? "static field" : "field",
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
                    String errorMessage = ("The '%s' field is " + (fieldDescription
                            .isTransient() ? "not "
                            : "") + "transient in the '%s' class. " + "Update the class so that it is compatible with the datatype.").formatted(
                            fieldName,
                            datatypeClassName);
                    BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
                }
            } catch (NoSuchMethodException e) {
                var errorMessage = "The 'get%s' method is not found in the '%s' class. Update the class so that it is compatible with the datatype.".formatted(
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
                    String errorMessage = """
                            Unexpected return type for method '%s' in class '%s'. \
                            Please, update the class to be compatible with the datatype.""".formatted(
                            getterMethod.getName(),
                            datatypeClassName);
                    BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
                } else if (!Modifier.isPublic(getterMethod.getModifiers())) {
                    String errorMessage = """
                            Unexpected access modifier on method '%s' in class '%s'. \
                            Please, update the class to be compatible with the datatype.""".formatted(
                            getterMethod.getName(),
                            datatypeClassName);
                    BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
                } else if (instance != null && fieldEntry.getValue().getDefaultValue() != null) {
                    var f = false;
                    try {
                        if (fieldEntry.getValue().hasDefaultKeyWord()) {
                            var defaultValue = getterMethod.invoke(instance);
                            if (defaultValue == null) {
                                f = true;
                            }
                        } else if (fieldEntry.getValue().hasDefaultValue()) {
                            var defaultValue = getterMethod.invoke(instance);
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
                        log.debug("Ignored error: ", e);
                    }
                    if (f) {
                        String errorMessage = """
                                The default value for the '%s' field in the '%s' class \
                                mismatches the default value used in the '%s' datatype. \
                                Update the class so that it is compatible with the datatype.""".formatted(
                                fieldEntry.getKey(),
                                datatypeClassName,
                                dataType.getName());
                        BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
                    }
                }
            }

            var setterMethodName = "set" + name;
            var methods = datatypeClass.getMethods();
            var found = false;
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
                String errorMessage = """
                        The '%s(%s)' method is not found in the '%s' class. \
                        Update the class so that it is compatible with the datatype.""".formatted(
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
                String errorMessage = """
                        Invalid parent class '%s' is found in class '%s'. \
                        Please, update the class to be compatible with the datatype.""".formatted(
                        datatypeClass.getSuperclass() != null ? (" " + datatypeClass.getSuperclass().getTypeName()) : "",
                        datatypeClassName);
                BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
            }
            for (Entry<String, FieldDescription> fieldEntry : parentDatatypeTableBoundNode.getFields().entrySet()) {
                try {
                    var f = datatypeClass.getSuperclass().getDeclaredField(fieldEntry.getKey());
                    if (!Modifier.isPublic(f.getModifiers()) && !Modifier.isProtected(f.getModifiers())) {
                        String errorMessage = """
                                An invalid access modifier is found for the '%s' field in the '%s' class. \
                                Update the class so that it is compatible with the datatype.""".formatted(
                                fieldEntry.getKey(),
                                datatypeClass.getSuperclass().getTypeName());
                        BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
                    }
                } catch (NoSuchFieldException e) {
                    log.debug("Ignored error: ", e);
                }
            }
            var g = false;
            var parentFields = new LinkedList<FieldDescription>();
            var p = parentDatatypeTableBoundNode;
            while (p != null) {
                var x = new LinkedList<FieldDescription>();
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
                    var i = 0;
                    var f = true;
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
                String errorMessage = """
                        A mandatory constructor with parameters is not found in the '%s' class. \
                        Update the class so that it is compatible with the datatype.""".formatted(
                        datatypeClass.getSuperclass().getTypeName());
                BindHelper.processError(errorMessage, tableSyntaxNode, cxt);
            }
        }
    }

    /**
     * Whether any field of the datatype is marked with the non-transient suffix ({@code name*}).
     *
     * <p>A single marked field inverts the marking of the whole datatype: the marked fields are the ones the bean
     * keeps, and every unmarked field becomes transient.
     *
     * <p>The name is read from the column the table gives to {@code Name}, which a titled table may put anywhere.
     * Only the rows that declare a field are looked at, so neither a title row nor a commented row can invert a
     * datatype.
     */
    private static boolean anyFieldMarkedNonTransient(ILogicalTable dataTable,
                                                      int firstRow,
                                                      int tableHeight,
                                                      int nameColumn,
                                                      IBindingContext cxt) {
        for (var i = firstRow; i < tableHeight; i++) {
            var row = dataTable.getRow(i);
            if (!declaresNoField(dataTable, row, cxt) && isMarkedNonTransient(getCellSource(row, cxt, nameColumn)
                    .getCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a row of the body declares no field at all.
     *
     * <p>A commented row never declares one. A table without the columns beyond {@code Default} may also leave a
     * row blank.
     */
    private static boolean declaresNoField(ILogicalTable dataTable, ILogicalTable row, IBindingContext cxt) {
        var firstCellCode = getCellSource(row, cxt, 0).getCode();
        return dataTable.getWidth() > 3 ? ParserUtils.isCommented(firstCellCode)
                : ParserUtils.isBlankOrCommented(firstCellCode);
    }

    /**
     * Whether a name cell marks its field with the non-transient suffix ({@code name*}).
     *
     * <p>The name may carry a runtime context property, which the suffix precedes.
     */
    private static boolean isMarkedNonTransient(String nameCellCode) {
        return CONTEXT_SPLITTER.split(nameCellCode, 2)[0].endsWith(NON_TRANSIENT_FIELD_SUFFIX);
    }

    private void handleExampleValueError(String fieldName, IOpenClass fieldType, GridCellSourceCodeModule exampleValueCellSource, IBindingContext bindingContext) {
        var errorMessage = "The provided example value '%s' is not supported for the field '%s' of type '%s'. Please provide an example value that matches the field type.".formatted(exampleValueCellSource.getCode().trim(), fieldName, fieldType.getName());
        BindHelper.processError(errorMessage, exampleValueCellSource, bindingContext);
    }

    private void handleDefaultValueError(String fieldName, IOpenClass fieldType, GridCellSourceCodeModule defaultValueCellSource, IBindingContext bindingContext) {
        var errorMessage = "The provided default value '%s' is not supported for the field '%s' of type '%s'. Please provide an default value that matches the field type.".formatted(defaultValueCellSource.getCode().trim(), fieldName, fieldType.getName());
        BindHelper.processError(errorMessage, defaultValueCellSource, bindingContext);
    }

    /**
     * Declares the field a row of the body carries.
     *
     * @return the declared field, or {@code null} when the row carries none
     */
    private DatatypeOpenField processRow(ILogicalTable dataTable,
                                         ILogicalTable row,
                                         IBindingContext bindingContext,
                                         Map<String, FieldDescription> fields,
                                         Map<String, Integer> columnTitlesOrder,
                                         boolean useTransientSuffix) {
        if (declaresNoField(dataTable, row, bindingContext)) {
            return null;
        }
        GridCellSourceCodeModule typeCellSource = getCellSource(row, bindingContext, columnTitlesOrder.getOrDefault(TYPE_COLUMN_TITLE, 0));
        IOpenClass fieldType = OpenLManager
                .makeType(bindingContext.getOpenL(), typeCellSource.getCode(), typeCellSource, bindingContext);
        if (fieldType == NullOpenClass.the) {
            fieldType = JavaOpenClass.OBJECT;
        }
        GridCellSourceCodeModule nameCellSource = getCellSource(row, bindingContext, columnTitlesOrder.getOrDefault(NAME_COLUMN_TITLE, 1));
        final var code = nameCellSource.getCode();
        String contextProperty;
        var parts = CONTEXT_SPLITTER.split(code, 2);
        var rawFieldName = parts[0];
        final boolean isTransient = useTransientSuffix ? rawFieldName.endsWith(TRANSIENT_FIELD_SUFFIX)
                : !rawFieldName.endsWith(NON_TRANSIENT_FIELD_SUFFIX);
        String fieldName = extractFieldName(rawFieldName);
        if (TableNameChecker.isInvalidJavaIdentifier(fieldName)) {
            var errorMessage = "Bad field name: '%s'.".formatted(fieldName);
            BindHelper.processError(errorMessage, nameCellSource, bindingContext);
            return null;
        }
        if (parts.length > 1) {
            contextProperty = parts[1];
            if (contextProperty.isEmpty()) {
                contextProperty = fieldName;
            } else if (contextProperty.startsWith(".")) {
                contextProperty = StringUtils.trim(contextProperty.substring(1));
                if (TableNameChecker.isInvalidJavaIdentifier(contextProperty)) {
                    var errorMessage = "Bad context property name: '%s'.".formatted(contextProperty);
                    BindHelper.processError(errorMessage, nameCellSource, bindingContext);
                    return null;
                }
            }
            String errorMessage = ContextPropertyBinderUtils
                    .validateContextProperty(contextProperty, fieldType, bindingContext);
            if (errorMessage != null) {
                contextProperty = null;
                GridCellSourceCodeModule cellSource = getCellSource(row, bindingContext, columnTitlesOrder.getOrDefault(NAME_COLUMN_TITLE, 1));
                BindHelper.processError(errorMessage, cellSource, bindingContext);
            }
        } else {
            contextProperty = null;
        }

        FieldDescriptionBuilder fieldDescriptionBuilder;
        if (fields.containsKey(fieldName)) {
            var errorMessage = "Field '%s' is already declared.".formatted(fieldName);
            BindHelper.processError(errorMessage, nameCellSource, bindingContext);
            return null;
        } else if (fields.containsKey(ClassUtils.decapitalize(fieldName)) || fields
                .containsKey(ClassUtils.capitalize(fieldName))) {
            String f = null;
            if (fields.containsKey(ClassUtils.decapitalize(fieldName))) {
                f = ClassUtils.decapitalize(fieldName);
            }
            if (fields.containsKey(ClassUtils.capitalize(fieldName))) {
                f = ClassUtils.capitalize(fieldName);
            }
            var errorMessage = "Field '%s' conflicts with field '%s'.".formatted(fieldName, f);
            BindHelper.processError(errorMessage, nameCellSource, bindingContext);
        }
        fieldDescriptionBuilder = FieldDescriptionBuilder.create(fieldType.getJavaName())
                .setTransient(isTransient)
                .setContextPropertyName(contextProperty);

        fieldDescriptionBuilder.setContextPropertyName(contextProperty);

        if (fieldType.getDomain() != null) {
            Iterator<?> itr = fieldType.getDomain().iterator();
            var allowableValues = new ArrayList<String>();
            while (itr.hasNext()) {
                allowableValues.add(itr.next().toString());
            }
            fieldDescriptionBuilder.setAllowableValues(allowableValues.toArray(new String[0]));
        }

        FieldDescription fieldDescription;
        Object defaultValue = null;
        GridCellSourceCodeModule defaultValueCellSource = null;
        String defaultValueCode;
        if (columnTitlesOrder.containsKey(DEFAULT_COLUMN_TITLE) && row.getWidth() > 2) {
            var defaultColumnIndex = columnTitlesOrder.get(DEFAULT_COLUMN_TITLE);
            defaultValueCellSource = getCellSource(row, bindingContext, defaultColumnIndex);
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
                    var cell = defaultValueCellSource.getCell();
                    var metaInfoReader = tableSyntaxNode.getMetaInfoReader();
                    if (metaInfoReader instanceof BaseMetaInfoReader<?> reader) {
                        reader.addConstant(cell, constantOpenField);
                    }
                }
            } else {
                fieldDescriptionBuilder.setDefaultValueAsString(defaultValueCode);
                if (String.class != fieldType.getInstanceClass()) {
                    var theCellValue = row.getColumn(defaultColumnIndex).getCell(0, 0);
                    if (theCellValue.hasNativeType()) {
                        defaultValue = RuleRowHelper.loadNativeValue(theCellValue, fieldType);
                        if (defaultValue == null && !DefaultValue.DEFAULT.equals(defaultValueCode)) {
                            if (fieldType.getInstanceClass() != null) {
                                try {
                                    defaultValue = String2DataConvertorFactory.parse(fieldType.getInstanceClass(), defaultValueCode, bindingContext);
                                } catch (Exception e) {
                                    handleDefaultValueError(fieldName, fieldType, defaultValueCellSource, bindingContext);
                                }
                            } else if (StringUtils.isNotBlank(defaultValueCode)) {
                                handleDefaultValueError(fieldName, fieldType, defaultValueCellSource, bindingContext);
                            }
                        }
                        if (defaultValue != null) {
                            fieldDescriptionBuilder.setDefaultValue(defaultValue);
                        }
                    }
                }
            }
        }

        if (columnTitlesOrder.containsKey(DESCRIPTION_COLUMN_TITLE)) {
            var descriptionColumnIndex = columnTitlesOrder.get(DESCRIPTION_COLUMN_TITLE);
            GridCellSourceCodeModule descriptionValueCellSource = getCellSource(row, bindingContext, descriptionColumnIndex);
            if (StringUtils.isNotBlank(descriptionValueCellSource.getCode())) {
                fieldDescriptionBuilder.setDescriptionValue(descriptionValueCellSource.getCode().trim());
            }
        }
        if (columnTitlesOrder.containsKey(EXAMPLE_COLUMN_TITLE)) {
            var examplesColumnIndex = columnTitlesOrder.get(EXAMPLE_COLUMN_TITLE);
            GridCellSourceCodeModule examplesValueCellSource = getCellSource(row, bindingContext, examplesColumnIndex);
            var examplesValueCellSourceValue = examplesValueCellSource.getCode();
            if (StringUtils.isNotBlank(examplesValueCellSourceValue)) {
                if (fieldType.getInstanceClass() != null) {
                    try {
                        Object exampleValue = String2DataConvertorFactory.parse(fieldType.getInstanceClass(), examplesValueCellSourceValue.trim(), bindingContext);
                        try {
                            RuleRowHelper.validateValue(exampleValue, fieldType);
                            fieldDescriptionBuilder.setExampleValue(examplesValueCellSourceValue.trim());
                        } catch (Exception e) {
                            BindHelper.processError(e, defaultValueCellSource, bindingContext);
                        }
                    } catch (Exception e) {
                        handleExampleValueError(fieldName, fieldType, examplesValueCellSource, bindingContext);
                    }
                } else {
                    handleExampleValueError(fieldName, fieldType, examplesValueCellSource, bindingContext);
                }
            }
        }
        if (columnTitlesOrder.containsKey(MANDATORY_COLUMN_TITLE)) {
            var mandatoryColumnIndex = columnTitlesOrder.get(MANDATORY_COLUMN_TITLE);
            GridCellSourceCodeModule mandatoryValueCellSource = getCellSource(row, bindingContext, mandatoryColumnIndex);
            if (StringUtils.isNotBlank(mandatoryValueCellSource.getCode())) {
                try {
                    Boolean mandatoryValue = String2DataConvertorFactory.parse(Boolean.class, mandatoryValueCellSource.getCode(), bindingContext);
                    if (mandatoryValue != null) {
                        fieldDescriptionBuilder.setMandatoryValue(mandatoryValue);
                    }
                } catch (Exception e) {
                    var errorMessage = "The provided value '%s' is not valid for the mandatory column. Please provide a valid value.".formatted(mandatoryValueCellSource.getCode().trim());
                    BindHelper.processError(errorMessage, mandatoryValueCellSource, bindingContext);
                }
            }
        }


        try {
            fieldDescription = fieldDescriptionBuilder.build();
            if (defaultValue != null && !fieldDescription.hasDefaultKeyWord()) {
                // Validate not null default value
                // The null value is allowed for alias types
                var validationMessage = OpenClassUtils.isValidValue(defaultValue, fieldType);
                if (validationMessage != null) {
                    BindHelper.processError(validationMessage, defaultValueCellSource, bindingContext);
                }
            }
            fields.put(fieldName, fieldDescription);
        } catch (Exception e) {
            // If we have an exception here, it means that default value is wrong, we have already processed it.
        }

        var field = new DatatypeOpenField(dataType, fieldName, fieldType, contextProperty, isTransient);
        dataType.addField(field);
        return field;
    }

    private static String extractFieldName(String fieldName) {
        return fieldName.endsWith(NON_TRANSIENT_FIELD_SUFFIX) || fieldName.endsWith(TRANSIENT_FIELD_SUFFIX) ? fieldName
                .substring(0, fieldName.length() - 1) : fieldName;
    }

    @Override
    public void addTo(ModuleOpenClass openClass) {
        var internalClassMember = new InternalDatatypeClass(dataType, openClass);
        tableSyntaxNode.setMember(internalClassMember);
    }

    @Override
    public void finalizeBind(IBindingContext bindingContext) throws Exception {
        try {
            if (!byteCodeReadyToLoad) {
                return;
            }
            var classLoader = (OpenLClassLoader) Thread.currentThread().getContextClassLoader();
            var datatypeClass = classLoader.loadClass(dataType.getJavaName());
            dataType.setInstanceClass(datatypeClass);
            moduleOpenClass.addType(dataType);
        } catch (ClassNotFoundException | LinkageError e) {
            log.debug("Error occurred: ", e);
            var errorMessage = "Failed to load a class for datatype '%s'.".formatted(dataType.getJavaName());
            BindHelper.processError(errorMessage, e, tableSyntaxNode, bindingContext);
        } finally {
            fields = null;
        }
    }

    public void generateByteCode(IBindingContext bindingContext) throws Exception {
        if (!bindingContext.isExecutionMode()) {
            tableSyntaxNode.setMetaInfoReader(new DatatypeTableMetaInfoReader(this));
        }
        if (!generated) {
            if (generatingInProcess) {
                throw new OpenLCompilationException("Circular dependency with respect to inheritance '%s' is detected."
                        .formatted(parentClassName));
            }
            generatingInProcess = true;
            try {
                if (parentClassName != null) {
                    IOpenClass parentOpenClass;
                    var parentBoundNode = getParentDatatypeTableBoundNode();
                    if (parentBoundNode != null) {
                        parentBoundNode.generateByteCode(bindingContext);
                        parentOpenClass = parentBoundNode.getDataType();
                    } else {
                        parentOpenClass = bindingContext.findType(parentClassName);
                    }
                    if (parentOpenClass == null) {
                        byteCodeReadyToLoad = true;
                        throw new OpenLCompilationException(
                                "Parent class '%s' is not found.".formatted(parentClassName));
                    }

                    if (parentOpenClass.getInstanceClass() != null) {// parent class has
                        // errors
                        if (Modifier.isFinal(parentOpenClass.getInstanceClass().getModifiers())) {
                            throw new OpenLCompilationException(
                                    "Cannot inherit from final class '%s'.".formatted(parentClassName));
                        }
                        try {
                            parentOpenClass.getInstanceClass().getConstructor();
                        } catch (NoSuchMethodException e) {
                            throw new OpenLCompilationException(
                                    "Cannot inherit from class '%s'. Default constructor is not found.".formatted(
                                            parentClassName));
                        }
                    }

                    if (parentOpenClass instanceof DomainOpenClass) {
                        throw new OpenLCompilationException(
                                "Parent class '%s' cannot be a domain type.".formatted(parentClassName));
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

    private void validateInheritedFieldsDuplication(final IBindingContext cxt) {
        final var superClass = dataType.getSuperClass();
        if (superClass != null) {
            for (final IOpenField field : dataType.getDeclaredFields()) {
                var fieldInParent = superClass.getField(field.getName());
                if (fieldInParent != null) {
                    if (Objects.equals(fieldInParent.getType(), field.getType())) {
                        BindHelper.processWarn("Field '%s' is already declared in parent class '%s'.".formatted(
                                field.getName(),
                                fieldInParent.getDeclaringClass().getDisplayName(0)), tableSyntaxNode, cxt);
                    } else {
                        var errorMessage = "Field '%s' is already declared in class '%s' with another type.".formatted(
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
}
