package org.openl.rules.binding;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.MethodUtil;
import org.openl.binding.impl.BindingContextDelegator;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.component.ComponentBindingContext;
import org.openl.domain.IDomain;
import org.openl.exception.OpenLCompilationException;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.IMetaHolder;
import org.openl.meta.IMetaInfo;
import org.openl.meta.ValueMetaInfo;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.convertor.IObjectToDataConvertor;
import org.openl.rules.convertor.ObjectToDataConvertorFactory;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.dt.element.ArrayHolder;
import org.openl.rules.helpers.INumberRange;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.BaseMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.*;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.DomainUtils;
import org.openl.util.StringPool;
import org.openl.util.StringTool;
import org.openl.util.text.LocationUtils;

public final class RuleRowHelper {

    private RuleRowHelper() {
    }

    private static final String COMMENTARY = "//";
    public static final String ARRAY_ELEMENTS_SEPARATOR_ESCAPER = "\\";
    public static final String ARRAY_ELEMENTS_SEPARATOR = ",";
    public static final String CONSTRUCTOR = "constructor";

    public static int calculateHeight(ILogicalTable table) {

        int height = table.getHeight();

        int last = -1;

        for (int i = 0; i < height; i++) {
            String source = table.getRow(i).getSource().getCell(0, 0).getStringValue();

            if (source != null && source.trim().length() != 0) {
                last = i;
            }
        }

        return last + 1;
    }

    public static String[] extractElementsFromCommaSeparatedArray(ILogicalTable cell) {

        String[] tokens = null;
        String src = cell.getSource().getCell(0, 0).getStringValue();

        if (src != null) {
            tokens = StringTool.splitAndEscape(src, ARRAY_ELEMENTS_SEPARATOR, ARRAY_ELEMENTS_SEPARATOR_ESCAPER);
        }

        return tokens;
    }

    /**
     * Method to support loading Arrays through {@link #ARRAY_ELEMENTS_SEPARATOR} in one cell. Gets the cell string
     * value. Split it by {@link #ARRAY_ELEMENTS_SEPARATOR}, and process every token as single parameter. Returns array
     * of parameters.
     *
     * @return Array of parameters.
     */
    public static Object loadCommaSeparatedParam(IOpenClass aggregateType,
            IOpenClass paramType,
            String paramName,
            String ruleName,
            ILogicalTable cell,
            OpenlToolAdaptor openlAdaptor) throws SyntaxNodeException {

        Object arrayValues;
        String[] tokens = extractElementsFromCommaSeparatedArray(cell);

        if (tokens != null) {

            ArrayList<Object> values = new ArrayList<>(tokens.length);

            for (String token : tokens) {

                String str = StringPool.intern(token);

                Object res = loadSingleParam(paramType, paramName, ruleName, cell, openlAdaptor, str);

                if (res == null) {
                    res = paramType.nullObject();
                }

                values.add(res);
            }

            int valuesArraySize = values.size();
            IAggregateInfo aggregateInfo = aggregateType.getAggregateInfo();
            arrayValues = aggregateInfo.makeIndexedAggregate(paramType, valuesArraySize);
            IOpenIndex index = aggregateInfo.getIndex(aggregateType);

            for (int i = 0; i < valuesArraySize; i++) {
                index.setValue(arrayValues, i, values.get(i));
            }
        } else {
            arrayValues = aggregateType.getAggregateInfo().makeIndexedAggregate(paramType, 0);
        }

        return arrayValues;
    }

    public static IOpenClass getType(String typeCode,
            ISyntaxNode node,
            IBindingContext bindingContext) throws SyntaxNodeException {
        if (typeCode.endsWith("[]")) {
            String baseCode = typeCode.substring(0, typeCode.length() - 2);
            IOpenClass type = getType(baseCode, node, bindingContext);
            return type.getAggregateInfo().getIndexedAggregateType(type);
        }
        IOpenClass type = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeCode);
        if (type == null) {
            throw SyntaxNodeExceptionUtils.createError(String.format("Type '%s' is not found.", typeCode), node);
        }
        return type;
    }

    public static Object loadSingleParam(IOpenClass paramType,
            String paramName,
            String ruleName,
            ILogicalTable table,
            OpenlToolAdaptor openlAdapter) throws SyntaxNodeException {

        validateSimpleParam(table, openlAdapter.getBindingContext());

        ICell theCell = table.getSource().getCell(0, 0);
        ICell theValueCell = theCell;

        if (theCell.getRegion() != null) {
            theValueCell = theCell.getTopLeftCellFromRegion();
        }

        if (String.class.equals(paramType.getInstanceClass())) {
            // if param type is of type String, load as String
            String src = theValueCell.getStringValue();
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
        String src = theValueCell.getStringValue();
        // TODO review our using of intern()
        // @see http://java-performance.info/string-intern-in-java-6-7-8/
        // if (src != null) src = src.intern();
        return loadSingleParam(paramType, paramName, ruleName, table, openlAdapter, src);
    }

    private static Object loadNativeValue(IOpenClass paramType,
            String paramName,
            String ruleName,
            ILogicalTable table,
            OpenlToolAdaptor openlAdapter,
            ICell theValueCell) throws SyntaxNodeException {
        if (theValueCell.getNativeType() == IGrid.CELL_TYPE_NUMERIC) {
            try {
                Object res = loadNativeValue(theValueCell, paramType);

                if (res instanceof IMetaHolder) {
                    setMetaInfo((IMetaHolder) res, table, paramName, ruleName, openlAdapter.getBindingContext());
                }

                if (res != null) {
                    validateValue(res, paramType);
                    return res;
                }
            } catch (Exception | LinkageError t) {
                String message = t.getMessage();
                if (message == null) {
                    message = "Cannot load cell value";
                }
                IGridTable cellTable = getTopLeftCellFromMergedRegion(table.getSource());
                throw SyntaxNodeExceptionUtils.createError(message,
                    t,
                    LocationUtils.createTextInterval(theValueCell.getStringValue()),
                    new GridCellSourceCodeModule(cellTable, openlAdapter.getBindingContext()));
            }
        }
        return null;
    }

    private static void validateSimpleParam(ILogicalTable table,
            IBindingContext bindingContext) throws SyntaxNodeException {
        ICell theCell = table.getSource().getCell(0, 0);
        if (table.getWidth() > 1 || table.getHeight() > 1) {
            for (int i = 0; i < table.getHeight(); i++) {
                for (int j = 0; j < table.getWidth(); j++) {
                    if (!(i == 0 && j == 0)) {
                        ICell cell = table.getCell(j, i);
                        if ((theCell.getAbsoluteRegion().getTop() != cell.getAbsoluteRegion().getTop() || theCell
                            .getAbsoluteRegion()
                            .getLeft() != cell.getAbsoluteRegion().getLeft()) && cell.getStringValue() != null) {
                            if (!cell.getStringValue().startsWith(COMMENTARY)) {
                                IGridTable cellTable = getTopLeftCellFromMergedRegion(table.getSource());
                                throw SyntaxNodeExceptionUtils.createError(
                                    "Table structure is wrong. More than one cell with data found where only one cell is expected.",
                                    new GridCellSourceCodeModule(cellTable, bindingContext));
                            }
                        }
                    }
                }
            }
        }
    }

    public static Object loadNativeValue(ICell cell, IOpenClass paramType) {
        Object res = null;
        if (cell.getNativeType() == IGrid.CELL_TYPE_NUMERIC) {
            Class<?> expectedType = paramType.getInstanceClass();

            if (BigDecimal.class.isAssignableFrom(expectedType) || BigDecimalValue.class
                .isAssignableFrom(expectedType)) {
                // Convert String -> BigDecimal instead of double ->BigDecimal,
                // otherwise we lose in precision (part of EPBDS-5879)
                res = String2DataConvertorFactory.parse(expectedType, cell.getStringValue(), null);
            } else {
                double value = cell.getNativeNumber();
                IObjectToDataConvertor objectConvertor = ObjectToDataConvertorFactory.getConvertor(expectedType,
                    double.class);
                if (objectConvertor != ObjectToDataConvertorFactory.NO_Convertor) {
                    res = objectConvertor.convert(value);
                } else {
                    objectConvertor = ObjectToDataConvertorFactory.getConvertor(expectedType, Double.class);
                    if (objectConvertor != ObjectToDataConvertorFactory.NO_Convertor) {
                        res = objectConvertor.convert(value);
                    } else {
                        objectConvertor = ObjectToDataConvertorFactory.getConvertor(expectedType, Date.class);
                        if (objectConvertor != ObjectToDataConvertorFactory.NO_Convertor) {
                            Date dateValue = cell.getNativeDate();
                            res = objectConvertor.convert(dateValue);
                        } else if ((int) value == value) {
                            objectConvertor = ObjectToDataConvertorFactory.getConvertor(expectedType, Integer.class);
                            if (objectConvertor != ObjectToDataConvertorFactory.NO_Convertor) {
                                res = objectConvertor.convert((int) value);
                            }

                        }
                    }
                }
            }
        }
        return res;
    }

    public static SimpleNodeUsage createConstantNodeUsage(ConstantOpenField constantOpenField, int start, int end) {
        String description = MethodUtil.printType(constantOpenField.getType()) + " " + constantOpenField
            .getName() + " = " + constantOpenField.getValueAsString();
        return new SimpleNodeUsage(start, end, description, constantOpenField.getUri(), NodeType.OTHER);
    }

    private static XlsModuleOpenClass getComponentOpenClass(IBindingContext bindingContext) {
        if (bindingContext instanceof ComponentBindingContext) {
            IOpenClass openClass = ((ComponentBindingContext) bindingContext).getComponentOpenClass();
            if (openClass instanceof XlsModuleOpenClass) {
                return (XlsModuleOpenClass) openClass;
            }
        }
        if (bindingContext instanceof BindingContextDelegator) {
            BindingContextDelegator bindingContextDelegator = (BindingContextDelegator) bindingContext;
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
            return xlsModuleOpenClass.getConstantField(source.trim());
        }
        return null;
    }

    public static Object castConstantToExpectedType(IBindingContext bindingContext,
            ConstantOpenField constantOpenField,
            IOpenClass expectedType) {
        IOpenCast openCast = bindingContext.getCast(constantOpenField.getType(), expectedType);
        if (openCast != null && openCast.isImplicit()) {
            return openCast.convert(constantOpenField.getValue());
        } else {
            throw new ClassCastException(String.format("Expected value of type '%s'.", expectedType.getName()));
        }
    }

    private static Object loadSingleParam(IOpenClass paramType,
            String paramName,
            String ruleName,
            ILogicalTable cell,
            OpenlToolAdaptor openlAdaptor,
            String source) throws SyntaxNodeException {

        // TODO: parse values considering underlying excel format. Note: this
        // class does not know anything about Excel. Keep it storage format
        // agnostic (don't introduce excel dependencies). Also consider adding
        // meta info.
        if (source != null && (source = source.trim()).length() != 0) {
            if (openlAdaptor.getHeader() != null) {
                IOpenMethodHeader oldHeader = openlAdaptor.getHeader();
                OpenMethodHeader newHeader = new OpenMethodHeader(oldHeader.getName(),
                    paramType,
                    oldHeader.getSignature(),
                    oldHeader.getDeclaringClass());
                openlAdaptor.setHeader(newHeader);

                if (source.startsWith("{") && source.endsWith("}")) {
                    GridCellSourceCodeModule srcCode = new GridCellSourceCodeModule(cell.getSource(),
                        openlAdaptor.getBindingContext());

                    return openlAdaptor.makeMethod(srcCode);
                }

                if (source.startsWith("=") && (source.length() > 2 || source.length() == 2 && Character
                    .isLetterOrDigit(source.charAt(1)))) {

                    GridCellSourceCodeModule gridSource = new GridCellSourceCodeModule(cell.getSource(),
                        openlAdaptor.getBindingContext());
                    IOpenSourceCodeModule code = new SubTextSourceCodeModule(gridSource, 1);

                    return openlAdaptor.makeMethod(code);
                }
            }

            Class<?> expectedType = paramType.getInstanceClass();

            // Try to get cell object value with appropriate string parser.
            // A parser instance will be selected using expected type of cell
            // value.
            //
            Object result = null;

            try {
                IBindingContext bindingContext = openlAdaptor.getBindingContext();
                // Pasre as constant value
                ConstantOpenField constantOpenField = findConstantField(bindingContext, source);
                ICell theValueCell = cell.getSource().getCell(0, 0);
                if (constantOpenField != null) {
                    if (!bindingContext.isExecutionMode()) {
                        addContantMetaInfo(openlAdaptor, constantOpenField, theValueCell);
                    }
                    if (constantOpenField.getValue() != null) {
                        result = castConstantToExpectedType(bindingContext, constantOpenField, paramType);
                    }
                } else {
                    if (String.class.equals(paramType.getInstanceClass())) {
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
                String message = String.format("Cannot parse cell value '%s'. Expected value of type '%s'.",
                    source,
                    expectedType.getSimpleName());
                IGridTable cellTable = getTopLeftCellFromMergedRegion(cell.getSource());
                IOpenSourceCodeModule cellSourceCodeModule = new GridCellSourceCodeModule(cellTable,
                    openlAdaptor.getBindingContext());
                if (e instanceof CompositeSyntaxNodeException) {
                    throw SyntaxNodeExceptionUtils.createError(message, cellSourceCodeModule);
                } else {
                    throw SyntaxNodeExceptionUtils.createError(message, e, null, cellSourceCodeModule);
                }
            }

            if (result instanceof IMetaHolder) {
                setMetaInfo((IMetaHolder) result, cell, paramName, ruleName, openlAdaptor.getBindingContext());
            }

            try {
                validateValue(result, paramType);
            } catch (Exception e) {
                String message = String.format("Invalid cell value '%s'", source);
                IGridTable cellTable = getTopLeftCellFromMergedRegion(cell.getSource());
                IOpenSourceCodeModule cellSourceCodeModule = new GridCellSourceCodeModule(cellTable,
                    openlAdaptor.getBindingContext());

                throw SyntaxNodeExceptionUtils.createError(message, e, null, cellSourceCodeModule);
            }

            return result;
        }

        return null;
    }

    private static void addContantMetaInfo(OpenlToolAdaptor openlAdapter,
            ConstantOpenField constantOpenField,
            ICell theValueCell) {
        MetaInfoReader metaInfoReader = openlAdapter.getTableSyntaxNode().getMetaInfoReader();
        if (metaInfoReader instanceof BaseMetaInfoReader) {
            String[] tokens = StringTool.splitAndEscape(theValueCell.getStringValue(), ARRAY_ELEMENTS_SEPARATOR, null);
            String cellValue = theValueCell.getStringValue();
            int startFrom = 0;
            for (String token : tokens) {
                int start = cellValue.indexOf(token, startFrom);
                startFrom = start + token.length() - 1;
                if (token.equals(constantOpenField.getName())) {
                    int end = start + constantOpenField.getName().length() - 1;
                    SimpleNodeUsage nodeUsage = createConstantNodeUsage(constantOpenField, start, end);
                    ((BaseMetaInfoReader) metaInfoReader).addConstant(theValueCell, nodeUsage);
                }
            }
        }
    }

    /**
     * The cell in merged region must point to the top left cell of the region. If the cell is not in merged region,
     * return cellTable object unchanged. If cellTable is not cell return cellTable object unchanged.
     *
     * @param cellTable original cell
     * @return top left cell if region is merged or the cell itself otherwise
     */
    private static IGridTable getTopLeftCellFromMergedRegion(IGridTable cellTable) {
        if (cellTable instanceof SingleCellGridTable) {
            ICell cell = cellTable.getCell(0, 0);
            IGridRegion region = cell.getRegion();
            if (region == null) {
                return cellTable;
            }

            int left;
            int top;
            if (cellTable.isNormalOrientation()) {
                top = region.getTop();
                left = region.getLeft();
            } else {
                top = region.getLeft();
                left = region.getTop();
            }

            if (cell.getColumn() != left || cell.getRow() != top) {
                IGridTable table = ((SingleCellGridTable) cellTable).getOriginalGridTable();
                return new SingleCellGridTable(table, left, top);
            }
        }

        return cellTable;
    }

    public static boolean isFormula(String value) {
        if (value != null) {
            return value.trim().startsWith("=");
        }
        return false;
    }

    public static boolean isFormula(ILogicalTable valuesTable) {
        String stringValue = valuesTable.getSource().getCell(0, 0).getStringValue();
        return isFormula(stringValue);
    }

    public static CellMetaInfo createCellMetaInfo(IdentifierNode identifier, IMetaInfo metaInfo, NodeType nodeType) {
        SimpleNodeUsage nodeUsage = new SimpleNodeUsage(identifier,
            metaInfo.getDisplayName(INamedThing.SHORT),
            metaInfo.getSourceUrl(),
            nodeType);
        return new CellMetaInfo(JavaOpenClass.STRING, false, Collections.singletonList(nodeUsage));
    }

    private static void setMetaInfo(IMetaHolder holder,
            ILogicalTable cell,
            String paramName,
            String ruleName,
            IBindingContext bindingContext) {
        if (!bindingContext.isExecutionMode()) {
            ValueMetaInfo valueMetaInfo = new ValueMetaInfo();
            valueMetaInfo.setShortName(paramName);
            valueMetaInfo.setFullName(ruleName == null ? paramName : ruleName + "." + paramName);
            valueMetaInfo.setSource(new GridCellSourceCodeModule(cell.getSource(), bindingContext));

            holder.setMetaInfo(valueMetaInfo);
        }
    }

    @SuppressWarnings("unchecked")
    public static void validateValue(Object value, IOpenClass paramType) throws OpenLCompilationException {
        IDomain<Object> domain = (IDomain<Object>) paramType.getDomain();

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
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(value, i);
                validateDomain(element, domain, paramType);
            }
        } else if (value instanceof Iterable && !(value instanceof INumberRange)) {
            Iterable list = (Iterable) value;
            for (Object element : list) {
                validateDomain(element, domain, paramType);
            }
        } else {
            try {
                // block is surrounded by try block, as EnumDomain
                // implementation throws a
                // RuntimeException when value doesn`t belong to domain.
                //
                boolean contains = domain.selectObject(value);
                if (!contains) {
                    throw new OpenLCompilationException(
                        String.format("The value '%s' is outside of valid domain '%s'. Valid values: %s",
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
            boolean loadSingleParamOnly) throws SyntaxNodeException {

        if (!loadSingleParamOnly) {
            return loadSingleParam(paramType, paramName, ruleName, dataTable, openlAdaptor);
        }

        dataTable = LogicalTableHelper.make1ColumnTable(dataTable);

        int height = RuleRowHelper.calculateHeight(dataTable);

        boolean oneCellTable = height == 1;

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

        IOpenClass arrayType = paramType.getAggregateInfo().getComponentType(paramType);

        boolean isFormula = isFormula(dataTable);

        if (oneCellTable) {
            if (!isFormula) {
                // try to load as constant first
                String[] tokens = extractElementsFromCommaSeparatedArray(dataTable.getRow(0));
                if (tokens != null && tokens.length == 1) {
                    ConstantOpenField constantOpenField = findConstantField(openlAdaptor.getBindingContext(),
                        tokens[0]);
                    if (constantOpenField != null) {
                        IOpenCast openCast = openlAdaptor.getBindingContext()
                            .getCast(constantOpenField.getType(), paramType);
                        if (openCast != null && openCast.isImplicit()) {
                            if (!openlAdaptor.getBindingContext().isExecutionMode()) {
                                addContantMetaInfo(openlAdaptor,
                                    constantOpenField,
                                    dataTable.getRow(0).getSource().getCell(0, 0));
                            }
                            return openCast.convert(constantOpenField.getValue());
                        }
                    }
                }

                // load comma separated array
                return loadCommaSeparatedArrayParams(dataTable,
                    paramName,
                    ruleName,
                    openlAdaptor,
                    paramType,
                    arrayType);
            } else {
                return loadSingleParam(paramType, paramName, ruleName, dataTable, openlAdaptor);
            }
        } else {
            return loadSimpleArrayParams(dataTable, paramName, ruleName, openlAdaptor, paramType, arrayType);
        }
    }

    private static Object loadCommaSeparatedArrayParams(ILogicalTable dataTable,
            String paramName,
            String ruleName,
            OpenlToolAdaptor openlAdaptor,
            IOpenClass aggregateType,
            IOpenClass paramType) throws SyntaxNodeException {

        ILogicalTable paramSource = dataTable.getRow(0);
        Object params = RuleRowHelper
            .loadCommaSeparatedParam(aggregateType, paramType, paramName, ruleName, paramSource, openlAdaptor);
        Class<?> paramClass = params.getClass();
        if (paramClass.isArray() && !paramClass.getComponentType().isPrimitive()) {
            return processAsObjectParams(paramType, (Object[]) params);
        }
        return params;
    }

    /**
     * Checks if the elements of parameters array are the instances of {@link CompositeMethod}, if yes process it
     * through {@link ArrayHolder}. If no return Object[].
     *
     * @param paramType parameter type
     * @param paramsArray array of parameters
     * @return {@link ArrayHolder} if elements of parameters array are instances of {@link CompositeMethod}, in other
     *         case Object[].
     */
    private static Object processAsObjectParams(IOpenClass paramType, Object[] paramsArray) {
        List<CompositeMethod> methodsList = null;
        Object ary;
        int paramsLength = paramsArray.length;
        ary = paramType.getAggregateInfo().makeIndexedAggregate(paramType, paramsLength);
        for (int i = 0; i < paramsLength; i++) {
            if (paramsArray[i] instanceof CompositeMethod) {
                methodsList = new ArrayList<>(addMethod(methodsList, (CompositeMethod) paramsArray[i]));
            } else {
                Array.set(ary, i, paramsArray[i]);
            }
        }

        return methodsList == null ? ary : new ArrayHolder(paramType, methodsList.toArray(new CompositeMethod[0]));
    }

    private static List<CompositeMethod> addMethod(List<CompositeMethod> methods, CompositeMethod method) {
        if (methods == null) {
            methods = new ArrayList<>();
        }
        methods.add(method);

        return methods;
    }

    private static Object loadSimpleArrayParams(ILogicalTable dataTable,
            String paramName,
            String ruleName,
            OpenlToolAdaptor openlAdaptor,
            IOpenClass aggregateType,
            IOpenClass paramType) throws SyntaxNodeException {

        int height = RuleRowHelper.calculateHeight(dataTable);

        List<CompositeMethod> methodsList = null;
        List<Object> values = new ArrayList<>();

        for (int i = 0; i < height; i++) { // load array values represented as
            // number of cells
            ILogicalTable cell = dataTable.getRow(i);
            Object parameter = RuleRowHelper.loadSingleParam(paramType, paramName, ruleName, cell, openlAdaptor);

            if (parameter instanceof CompositeMethod) {
                methodsList = new ArrayList<>(addMethod(methodsList, (CompositeMethod) parameter));
            } else {
                if (parameter != null) {
                    values.add(parameter);
                }
            }
        }

        IAggregateInfo aggregateInfo = aggregateType.getAggregateInfo();
        Object ary = aggregateInfo.makeIndexedAggregate(paramType, values.size());
        IOpenIndex index = aggregateInfo.getIndex(aggregateType);

        for (int i = 0; i < values.size(); i++) {
            index.setValue(ary, i, values.get(i));
        }

        return methodsList == null ? ary : new ArrayHolder(paramType, methodsList.toArray(new CompositeMethod[0]));

    }
}
