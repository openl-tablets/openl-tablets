package org.openl.rules.binding;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.binding.impl.component.ComponentBindingContext;
import org.openl.domain.IDomain;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.IMetaHolder;
import org.openl.meta.IMetaInfo;
import org.openl.meta.ValueMetaInfo;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.convertor.IObjectToDataConvertor;
import org.openl.rules.convertor.ObjectToDataConvertorFactory;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.dt.element.ArrayHolder;
import org.openl.rules.helpers.ArraySplitter;
import org.openl.rules.helpers.INumberRange;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.BaseMetaInfoReader;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.DomainUtils;
import org.openl.util.OpenClassUtils;
import org.openl.util.StringPool;

public final class RuleRowHelper {

    private RuleRowHelper() {
    }

    private static final String COMMENTARY = "//";
    public static final String CONSTRUCTOR = "constructor";

    private static final Object EMPTY_CELL = new Object();
    private static final Object[] EMPTY_ROW = new Object[0];

    public static int calculateHeight(ILogicalTable table) {
        var height = table.getHeight();
        var last = -1;
        for (var i = 0; i < height; i++) {
            var source = table.getRow(i).getSource().getCell(0, 0).getStringValue();
            if (source != null && source.trim().length() != 0) {
                last = i;
            }
        }
        return last + 1;
    }

    /**
     * Method to support loading Arrays through comma in one cell. Gets the cell string value. Split it by comma, and
     * process every token as single parameter. Returns array of parameters.
     *
     * @return Array of parameters.
     */
    public static Object loadCommaSeparatedParam(IOpenClass aggregateType,
                                                 IOpenClass paramType,
                                                 String paramName,
                                                 String ruleName,
                                                 ILogicalTable cell,
                                                 OpenlToolAdaptor openlAdaptor) {

        Object arrayValues;

        var src = cell.getSource().getCell(0, 0).getStringValue();

        if (src != null) {

            String[] tokens = ArraySplitter.split(src);
            var values = new ArrayList<Object>(tokens.length);

            for (String token : tokens) {

                String str = StringPool.intern(token);

                Object res = loadSingleParam(paramType, paramName, ruleName, cell, openlAdaptor, str);

                if (res == null) {
                    res = paramType.nullObject();
                }

                values.add(res);
            }

            var valuesArraySize = values.size();
            var aggregateInfo = aggregateType.getAggregateInfo();
            arrayValues = aggregateInfo.makeIndexedAggregate(paramType, valuesArraySize);
            var index = aggregateInfo.getIndex(aggregateType);

            if (index != null) {
                for (var i = 0; i < valuesArraySize; i++) {
                    index.setValue(arrayValues, i, values.get(i));
                }
            } else {
                if (arrayValues instanceof Collection collection) {
                    collection.addAll(values);
                }
            }
        } else {
            arrayValues = aggregateType.getAggregateInfo().makeIndexedAggregate(paramType, 0);
        }

        return arrayValues;
    }

    public static Object loadSingleParam(IOpenClass paramType,
                                         String paramName,
                                         String ruleName,
                                         ILogicalTable table,
                                         OpenlToolAdaptor openlAdapter) {

        validateSimpleParam(table, openlAdapter.getBindingContext());

        var theCell = table.getSource().getCell(0, 0);
        var theValueCell = theCell;

        if (theCell.getRegion() != null) {
            theValueCell = theCell.getTopLeftCellFromRegion();
        }

        if (String.class == paramType.getInstanceClass()) {
            // if param type is of type String, load as String
            var src = theValueCell.getStringValue();
            if (src != null) {
                src = src.length() <= 4 ? src.intern() : src;
            }
            return loadSingleParam(paramType, paramName, ruleName, table, openlAdapter, src);
        }

        // load value as native type
        if (theValueCell.hasNativeType()) {
            loadNativeValue(paramType, paramName, ruleName, table, openlAdapter, theValueCell);
        }

        // don`t move it up, as this call will convert native values such as
        // numbers and dates to strings, it
        // has negative performance implication
        var src = theValueCell.getStringValue();
        // TODO review our using of intern()
        // @see http://java-performance.info/string-intern-in-java-6-7-8/
        // if (src != null) src = src.intern();
        return loadSingleParam(paramType, paramName, ruleName, table, openlAdapter, src);
    }

    private static boolean isCellNumericStringDate(ICell theValueCell, IOpenClass paramType) {
        Class<?> instanceClass = paramType.getInstanceClass();
        var nativeType = theValueCell.getNativeType();
        return ClassUtils.isAssignable(instanceClass, Date.class) && nativeType == IGrid.CELL_TYPE_STRING && isNumeric(
                theValueCell.getStringValue());
    }

    public static boolean isNumeric(final CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return false;
        }
        final var sz = cs.length();
        var dots = 0;
        for (var i = 0; i < sz; i++) {
            if (!Character.isDigit(cs.charAt(i))) {
                return false;
            }
            if (cs.charAt(i) == '.') {
                if (++dots > 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Object loadNativeValue(IOpenClass paramType,
                                          String paramName,
                                          String ruleName,
                                          ILogicalTable table,
                                          OpenlToolAdaptor openlAdapter,
                                          ICell theValueCell) {
        if (theValueCell.getNativeType() == IGrid.CELL_TYPE_NUMERIC || isCellNumericStringDate(theValueCell,
                paramType)) {
            try {
                Object res = loadNativeValue(theValueCell, paramType);

                if (res instanceof IMetaHolder holder) {
                    setMetaInfo(holder, table, paramName, ruleName, openlAdapter.getBindingContext());
                }

                if (res != null) {
                    var validationMessage = OpenClassUtils.isValidValue(res, paramType);
                    if (validationMessage != null) {
                        BindHelper.processError(validationMessage,
                                new GridCellSourceCodeModule(table.getSource(), openlAdapter.getBindingContext()),
                                openlAdapter.getBindingContext());
                    }
                    return res;
                }
            } catch (Exception | LinkageError t) {
                var message = t.getMessage();
                if (message == null) {
                    message = "Cannot load cell value";
                }

                BindHelper.processError(message,
                        t,
                        new GridCellSourceCodeModule(table.getSource(), openlAdapter.getBindingContext()),
                        openlAdapter.getBindingContext());
            }
        }
        return null;
    }

    private static void validateSimpleParam(ILogicalTable table, IBindingContext bindingContext) {
        var theCell = table.getSource().getCell(0, 0);
        if (table.getWidth() > 1 || table.getHeight() > 1) {
            for (var i = 0; i < table.getHeight(); i++) {
                for (var j = 0; j < table.getWidth(); j++) {
                    if (!(i == 0 && j == 0)) {
                        var cell = table.getCell(j, i);
                        if ((theCell.getAbsoluteRegion().getTop() != cell.getAbsoluteRegion().getTop() || theCell
                                .getAbsoluteRegion()
                                .getLeft() != cell.getAbsoluteRegion().getLeft()) && cell.getStringValue() != null) {
                            if (!cell.getStringValue().startsWith(COMMENTARY)) {
                                BindHelper.processError(
                                        "Table structure is wrong. More than one cell with data found where only one cell is expected.",
                                        new GridCellSourceCodeModule(table.getSource(), bindingContext),
                                        bindingContext);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public static Object loadNativeValue(ICell cell, IOpenClass paramType) {
        Object res = null;
        Class<?> expectedType = paramType.getInstanceClass();
        if (cell.getNativeType() == IGrid.CELL_TYPE_NUMERIC || isCellNumericStringDate(cell, paramType)) {
            if (expectedType == null) {
                return null;
            }
            if (cell.getObjectValue() instanceof Date) {
                IObjectToDataConvertor objectConverter = ObjectToDataConvertorFactory.getConvertor(expectedType,
                        Date.class);
                return objectConverter != ObjectToDataConvertorFactory.NO_Convertor ? objectConverter
                        .convert(cell.getNativeDate()) : null;
            }

            if (ClassUtils.isAssignable(expectedType, BigDecimal.class)) {
                // Convert String -> BigDecimal instead of double ->BigDecimal,
                // otherwise we lose in precision (part of EPBDS-5879)
                res = String2DataConvertorFactory.parse(expectedType, cell.getStringValue(), null);
            } else {
                var value = cell.getNativeNumber();
                IObjectToDataConvertor objectConverter = ObjectToDataConvertorFactory.getConvertor(expectedType,
                        double.class);
                if (objectConverter != ObjectToDataConvertorFactory.NO_Convertor) {
                    res = objectConverter.convert(value);
                } else {
                    objectConverter = ObjectToDataConvertorFactory.getConvertor(expectedType, Double.class);
                    if (objectConverter != ObjectToDataConvertorFactory.NO_Convertor) {
                        res = objectConverter.convert(value);
                    } else {
                        objectConverter = ObjectToDataConvertorFactory.getConvertor(expectedType, Date.class);
                        if (objectConverter != ObjectToDataConvertorFactory.NO_Convertor) {
                            var dateValue = cell.getNativeDate();
                            res = objectConverter.convert(dateValue);
                        } else if ((int) value == value) {
                            objectConverter = ObjectToDataConvertorFactory.getConvertor(expectedType, Integer.class);
                            if (objectConverter != ObjectToDataConvertorFactory.NO_Convertor) {
                                res = objectConverter.convert((int) value);
                            }

                        }
                    }
                }
            }
        }
        return res;
    }

    private static XlsModuleOpenClass getComponentOpenClass(IBindingContext bindingContext) {
        if (bindingContext instanceof ComponentBindingContext context) {
            var openClass = context.getComponentOpenClass();
            if (openClass instanceof XlsModuleOpenClass class1) {
                return class1;
            }
        }
        if (bindingContext instanceof BindingContextDelegator bindingContextDelegator) {
            return getComponentOpenClass(bindingContextDelegator.getDelegate());
        }
        return null;
    }

    public static ConstantOpenField findConstantField(IBindingContext bindingContext, String source) {
        if (source == null) {
            return null;
        }

        XlsModuleOpenClass xlsModuleOpenClass = getComponentOpenClass(bindingContext);
        if (xlsModuleOpenClass != null) {
            var openField = xlsModuleOpenClass.getField(source.trim());
            if (openField instanceof ConstantOpenField field) {
                return field;
            }
        }
        return null;
    }

    public static Object castConstantToExpectedType(IBindingContext bindingContext,
                                                    ConstantOpenField constantOpenField,
                                                    IOpenClass expectedType) {
        var openCast = bindingContext.getCast(constantOpenField.getType(), expectedType);
        if (openCast != null && openCast.isImplicit()) {
            return openCast.convert(constantOpenField.getValue());
        } else {
            throw new ClassCastException("Expected value of type '%s'.".formatted(expectedType.getName()));
        }
    }

    private static Object loadSingleParam(IOpenClass paramType,
                                          String paramName,
                                          String ruleName,
                                          ILogicalTable cell,
                                          OpenlToolAdaptor openlAdaptor,
                                          String source) {

        // TODO: parse values considering underlying excel format. Note: this
        // class does not know anything about Excel. Keep it storage format
        // agnostic (don't introduce excel dependencies). Also consider adding
        // meta info.
        if (source != null && (source = source.trim()).length() != 0) {
            var bindingContext = openlAdaptor.getBindingContext();
            if (openlAdaptor.getHeader() != null) {
                var oldHeader = openlAdaptor.getHeader();
                var newHeader = new OpenMethodHeader(oldHeader.getName(),
                        paramType,
                        oldHeader.getSignature(),
                        oldHeader.getDeclaringClass());
                openlAdaptor.setHeader(newHeader);

                if (source.startsWith("{") && source.endsWith("}")) {
                    var srcCode = new GridCellSourceCodeModule(cell.getSource(), bindingContext);

                    return openlAdaptor.makeMethod(srcCode);
                }

                if (source.startsWith("=") && (source.length() > 2 || source.length() == 2 && Character
                        .isLetterOrDigit(source.charAt(1)))) {

                    var gridSource = new GridCellSourceCodeModule(cell.getSource(),
                            bindingContext);
                    var code = new SubTextSourceCodeModule(gridSource, 1);

                    return openlAdaptor.makeMethod(code);
                }
            }

            Class<?> expectedType = paramType.getInstanceClass();
            if (expectedType == null) {
                var cellSourceCodeModule = new GridCellSourceCodeModule(cell.getSource(),
                        bindingContext);
                BindHelper.processError("Cannot parse cell value '%s'. Undefined cell type.".formatted(source),
                        cellSourceCodeModule,
                        bindingContext);
                return null;
            }

            // Try to get cell object value with appropriate string parser.
            // A parser instance will be selected using expected type of cell
            // value.
            //
            Object result = null;

            try {
                // Parse as constant value
                ConstantOpenField constantOpenField = findConstantField(bindingContext, source);
                var theValueCell = cell.getSource().getCell(0, 0);
                if (constantOpenField != null) {
                    if (!bindingContext.isExecutionMode()) {
                        addConstantMetaInfo(openlAdaptor, constantOpenField, theValueCell);
                    }
                    if (constantOpenField.getValue() != null) {
                        result = castConstantToExpectedType(bindingContext, constantOpenField, paramType);
                    }
                } else {
                    if (String.class == paramType.getInstanceClass()) {
                        result = String2DataConvertorFactory.parse(expectedType, source, bindingContext);
                    } else {
                        if (theValueCell.hasNativeType()) {
                            result = loadNativeValue(paramType, paramName, ruleName, cell, openlAdaptor, theValueCell);
                        }
                        if (result == null) {
                            result = String2DataConvertorFactory.parse(expectedType, source, bindingContext);
                        }
                    }
                }
            } catch (Exception | LinkageError e) {
                // Parsing of loaded string value can be sophisticated process.
                // As a result various exception types can be thrown (e.g.
                // CompositeSyntaxNodeException) with not user-friendly message.
                //
                var message = "Cannot parse cell value '%s'. Expected value of type '%s'.".formatted(
                        source,
                        paramType.getDisplayName(INamedThing.SHORT));
                var cellSourceCodeModule = new GridCellSourceCodeModule(cell.getSource(),
                        bindingContext);
                BindHelper.processError(message, e, cellSourceCodeModule, bindingContext);
            }

            if (result instanceof IMetaHolder holder) {
                setMetaInfo(holder, cell, paramName, ruleName, bindingContext);
            }

            var validationMessage = OpenClassUtils.isValidValue(result, paramType);
            if (validationMessage != null) {
                var cellSourceCodeModule = new GridCellSourceCodeModule(cell.getSource(),
                        bindingContext);
                BindHelper.processError(validationMessage, cellSourceCodeModule, bindingContext);
            }

            return result;
        }

        return null;
    }

    private static void addConstantMetaInfo(OpenlToolAdaptor openlAdapter,
                                            ConstantOpenField constantOpenField,
                                            ICell theValueCell) {
        var metaInfoReader = openlAdapter.getTableSyntaxNode().getMetaInfoReader();
        if (metaInfoReader instanceof BaseMetaInfoReader reader) {
            reader.addConstant(theValueCell, constantOpenField);
        }
    }

    public static boolean isFormula(String value) {
        if (value != null) {
            return value.trim().startsWith("=");
        }
        return false;
    }

    public static boolean isFormula(ILogicalTable valuesTable) {
        var stringValue = valuesTable.getSource().getCell(0, 0).getStringValue();
        return isFormula(stringValue);
    }

    public static CellMetaInfo createCellMetaInfo(IdentifierNode identifier, IMetaInfo metaInfo, NodeType nodeType) {
        var nodeUsage = new SimpleNodeUsage(identifier,
                metaInfo.getDisplayName(INamedThing.SHORT),
                metaInfo.getSourceUrl(),
                nodeType);
        return new CellMetaInfo(JavaOpenClass.STRING, false, List.of(nodeUsage));
    }

    private static void setMetaInfo(IMetaHolder holder,
                                    ILogicalTable cell,
                                    String paramName,
                                    String ruleName,
                                    IBindingContext bindingContext) {
        if (!bindingContext.isExecutionMode()) {
            var valueMetaInfo = new ValueMetaInfo();
            valueMetaInfo.setShortName(paramName);
            valueMetaInfo.setFullName(ruleName == null ? paramName : ruleName + "." + paramName);
            valueMetaInfo.setSource(new GridCellSourceCodeModule(cell.getSource(), bindingContext));

            holder.setMetaInfo(valueMetaInfo);
        }
    }

    @SuppressWarnings("unchecked")
    public static void validateValue(Object value, IOpenClass paramType) throws OpenLCompilationException {
        var domain = (IDomain<Object>) paramType.getDomain();

        if (domain != null) {
            validateDomain(value, domain, paramType);
        }
    }

    private static void validateDomain(Object value,
                                       IDomain<Object> domain,
                                       IOpenClass paramType) throws OpenLCompilationException {
        if (value == null) {
            return;
        }
        if (value.getClass().isArray()) {
            var length = Array.getLength(value);
            for (var i = 0; i < length; i++) {
                Object element = Array.get(value, i);
                validateDomain(element, domain, paramType);
            }
        } else if (value instanceof Iterable list && !(value instanceof INumberRange)) {
            for (Object element : list) {
                validateDomain(element, domain, paramType);
            }
        } else {
            try {
                // block is surrounded by try block, as EnumDomain
                // implementation throws a
                // RuntimeException when value doesn`t belong to domain.
                //
                var contains = domain.selectObject(value);
                if (!contains) {
                    throw new OpenLCompilationException(
                            "The value '%s' is outside of valid domain '%s'. Valid values: %s".formatted(
                                    value,
                                    paramType.getName(),
                                    DomainUtils.toString(domain)));
                }
            } catch (RuntimeException e) {
                throw new OpenLCompilationException(e.getMessage(), e.getCause());
            }
        }
    }

    public static Object loadParam(ILogicalTable dataTable,
                                   IOpenClass paramType,
                                   String paramName,
                                   String ruleName,
                                   OpenlToolAdaptor openlAdaptor,
                                   boolean loadSingleParamOnly) {

        if (!loadSingleParamOnly) {
            return loadSingleParam(paramType, paramName, ruleName, dataTable, openlAdaptor);
        }

        dataTable = LogicalTableHelper.make1ColumnTable(dataTable);

        var height = RuleRowHelper.calculateHeight(dataTable);

        var oneCellTable = height == 1;

        if (height == 0) {
            return null;
        }

        // If data table contains one cell and parameter type is not array type
        // then load parameter value from single cell of table
        //
        // TODO: Is 'RuleRowHelper.isCommaSeparatedArray(dataTable)' check
        // required here? Can we make decision how to load data table using
        // value
        // of 'paramType' variable?
        //
        if (oneCellTable && !paramType.isArray()) {
            // attempt to load as a single paramType(will work in case of
            // expressions)
            return loadSingleParam(paramType, paramName, ruleName, dataTable, openlAdaptor);
        }

        // Load parameter value as an array of values.
        //

        var arrayType = paramType.getAggregateInfo().getComponentType(paramType);

        if (oneCellTable) {
            if (!isFormula(dataTable)) {
                // try to load as constant first
                var paramSource = dataTable.getRow(0);

                var src = paramSource.getSource().getCell(0, 0).getStringValue();

                if (src != null && !ArraySplitter.isArray(src)) {
                    ConstantOpenField constantOpenField = findConstantField(openlAdaptor.getBindingContext(), src);
                    if (constantOpenField != null) {
                        var openCast = openlAdaptor.getBindingContext()
                                .getCast(constantOpenField.getType(), paramType);
                        if (openCast != null && openCast.isImplicit()) {
                            if (!openlAdaptor.getBindingContext().isExecutionMode()) {
                                addConstantMetaInfo(openlAdaptor,
                                        constantOpenField,
                                        dataTable.getRow(0).getSource().getCell(0, 0));
                            }
                            return openCast.convert(constantOpenField.getValue());
                        }
                    }
                }

                // load comma separated array

                Object params = loadCommaSeparatedParam(paramType,
                        arrayType,
                        paramName,
                        ruleName,
                        paramSource,
                        openlAdaptor);
                Class<?> paramClass = params.getClass();
                if (paramClass.isArray() && !paramClass.getComponentType().isPrimitive()) {
                    for (Object o : (Object[]) params) {
                        if (o instanceof CompositeMethod) {
                            return new ArrayHolder(arrayType, (Object[]) params);
                        }
                    }
                }
                return params;
            } else {
                return loadSingleParam(paramType, paramName, ruleName, dataTable, openlAdaptor);
            }
        } else {
            return loadSimpleArrayParams(dataTable, paramName, ruleName, openlAdaptor, paramType, arrayType);
        }
    }

    private static Object loadSimpleArrayParams(ILogicalTable dataTable,
                                                String paramName,
                                                String ruleName,
                                                OpenlToolAdaptor openlAdaptor,
                                                IOpenClass aggregateType,
                                                IOpenClass paramType) {
        var hasFormulas = false;
        final var height = dataTable.getHeight();
        final var width = dataTable.getWidth();
        if (!paramType.isArray() || height == 1 || width == 1) {
            var values = new ArrayList<Object>();
            // 1 dim array
            var byHeight = height > 1 || width == 1;
            for (var i = 0; i < (byHeight ? height : width); i++) { // load array values represented as
                // number of cells
                ILogicalTable cell = byHeight ? dataTable.getRow(i) : dataTable.getColumn(i).transpose();
                var cellValue = cell.getCell(0, 0).getStringValue();
                if (!StringUtils.isEmpty(cellValue)) {
                    Object parameter = loadSingleParam(paramType, paramName, ruleName, cell, openlAdaptor);
                    if (parameter instanceof CompositeMethod) {
                        hasFormulas = true;
                    }
                    values.add(parameter);
                } else {
                    values.add(EMPTY_CELL);
                }
            }
            // For backward compatibility
            while (values.size() > 0 && values.getLast() == EMPTY_CELL) {
                values.removeLast();
            }
            for (var i = 0; i < values.size(); i++) {
                if (values.get(i) == EMPTY_CELL) {
                    values.set(i, paramType.nullObject());
                }
            }
            if (hasFormulas) {
                return new ArrayHolder(paramType, values.toArray(new Object[0]));
            } else {
                var aggregateInfo = aggregateType.getAggregateInfo();
                var array = aggregateInfo.makeIndexedAggregate(paramType, values.size());
                var index = aggregateInfo.getIndex(aggregateType);
                for (var i = 0; i < values.size(); i++) {
                    index.setValue(array, i, values.get(i));
                }
                return array;
            }
        } else {
            var values = new ArrayList<Object[]>();
            // 2 dim array
            for (var i = 0; i < width; i++) {
                Object[] values1 = new Object[height];
                var emptyRow = true;
                for (var j = 0; j < height; j++) {
                    // load array values represented as number of cells
                    var cell = dataTable.getSubtable(i, j, 1, 1);
                    var cellValue = cell.getCell(0, 0).getStringValue();
                    if (!StringUtils.isEmpty(cellValue)) {
                        emptyRow = false;
                        Object parameter = loadSingleParam(paramType
                                .getComponentClass(), paramName, ruleName, cell, openlAdaptor);
                        if (parameter instanceof CompositeMethod) {
                            hasFormulas = true;
                        }
                        values1[j] = parameter;
                    } else {
                        values1[j] = null;
                    }
                }
                if (emptyRow) {
                    values.add(EMPTY_ROW);
                } else {
                    values.add(values1);
                }
            }
            while (values.size() > 0 && values.getLast() == EMPTY_ROW) {
                values.removeLast();
            }
            for (var i = 0; i < values.size(); i++) {
                if (values.get(i) == EMPTY_ROW) {
                    values.set(i, new Object[dataTable.getHeight()]);
                }
            }
            if (hasFormulas) {
                return new ArrayHolder(paramType, values.toArray(new Object[0][0]));
            } else {
                var aggregateInfo = aggregateType.getAggregateInfo();
                var array = aggregateInfo.makeIndexedAggregate(paramType, values.size());
                var index = aggregateInfo.getIndex(aggregateType);
                for (var i = 0; i < values.size(); i++) {
                    var aggregateInfo1 = paramType.getAggregateInfo();
                    var array1 = aggregateInfo1.makeIndexedAggregate(paramType.getComponentClass(),
                            dataTable.getHeight());
                    var index1 = aggregateInfo1.getIndex(paramType);
                    for (var j = 0; j < values.get(i).length; j++) {
                        var v = values.get(i)[j];
                        index1.setValue(array1, j, v != null ? v : paramType.getComponentClass().nullObject());
                    }
                    index.setValue(array, i, array1);
                }
                return array;
            }
        }
    }
}
