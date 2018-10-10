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
import org.openl.binding.impl.NodeUsage;
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
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.SingleCellGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.DomainUtils;
import org.openl.util.Log;
import org.openl.util.StringPool;
import org.openl.util.StringTool;
import org.openl.util.text.LocationUtils;
import org.openl.vm.IRuntimeEnv;

public class RuleRowHelper {

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
    public static Object loadCommaSeparatedParam(IOpenClass paramType,
            String paramName,
            String ruleName,
            ILogicalTable cell,
            OpenlToolAdaptor openlAdaptor) throws SyntaxNodeException {

        Object arrayValues;
        String[] tokens = extractElementsFromCommaSeparatedArray(cell);

        if (tokens != null) {

            ArrayList<Object> values = new ArrayList<Object>(tokens.length);

            for (String token : tokens) {

                String str = StringPool.intern(token);
                Object res = loadSingleParam(paramType, paramName, ruleName, cell, openlAdaptor, str, true);

                if (res == null) {
                    res = paramType.nullObject();

                    // Set cell meta info manually.
                    //
                    //
                    if (!openlAdaptor.getBindingContext().isExecutionMode()) {
                        setCellMetaInfo(cell, paramName, paramType, true);
                    }
                }

                values.add(res);
            }

            int valuesArraySize = values.size();
            arrayValues = paramType.getAggregateInfo().makeIndexedAggregate(paramType, valuesArraySize);

            for (int i = 0; i < valuesArraySize; i++) {
                Array.set(arrayValues, i, values.get(i));
            }
        } else {
            arrayValues = paramType.getAggregateInfo().makeIndexedAggregate(paramType, 0);
            if (!openlAdaptor.getBindingContext().isExecutionMode()) {
                setCellMetaInfo(cell, paramName, paramType, true);
            }
        }

        return arrayValues;
    }

    public static IOpenClass getType(String typeCode, IBindingContext bindingContext) {

        if (typeCode.endsWith("[]")) {
            // FIXME: refactor, use
            // JavaClassGeneratorHelper#getDimension(String)
            //
            int dims = 0;
            String baseCode = typeCode;
            while (baseCode.endsWith("[]")) {
                baseCode = baseCode.substring(0, baseCode.length() - 2);
                dims++;
            }
            IOpenClass baseType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, baseCode);

            if (baseType == null) {
                return null;
            }

            return baseType.getAggregateInfo().getIndexedAggregateType(baseType, dims);
        }

        return bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeCode);
    }

    public static void loadParams(Object[] array,
            int from,
            Object[] paramValues,
            Object target,
            Object[] params,
            IRuntimeEnv env) {

        for (int i = 0; i < paramValues.length; i++) {

            Object value = paramValues[i];

            if (value instanceof IOpenMethod) {
                value = ((IOpenMethod) value).invoke(target, params, env);
            } else if (value instanceof ArrayHolder) {
                value = ((ArrayHolder) value).invoke(target, params, env);
            }

            array[i + from] = value;
        }
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
            if (src != null)
                src = src.length() <= 4 ? src.intern() : src;
            return loadSingleParam(paramType, paramName, ruleName, table, openlAdapter, src, false);
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
        return loadSingleParam(paramType, paramName, ruleName, table, openlAdapter, src, false);
    }

    private static Object loadNativeValue(IOpenClass paramType,
            String paramName,
            String ruleName,
            ILogicalTable table,
            OpenlToolAdaptor openlAdapter,
            ICell theValueCell) throws SyntaxNodeException {
        if (theValueCell.getNativeType() == IGrid.CELL_TYPE_NUMERIC) {
            try {
                Object res = loadNativeValue(theValueCell, paramType, openlAdapter.getBindingContext());

                if (res != null && res instanceof IMetaHolder) {
                    setMetaInfo((IMetaHolder) res, table, paramName, ruleName, openlAdapter.getBindingContext());
                }

                if (res != null) {
                    validateValue(res, paramType);
                    if (!openlAdapter.getBindingContext().isExecutionMode()) {
                        setCellMetaInfo(table, paramName, paramType, false);
                    }
                    return res;
                }
            } catch (Exception t) {
                String message = t.getMessage();
                if (message == null) {
                    message = "Can't load cell value";
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
                    if ((!(i == 0 && j == 0))) {
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

    public static Object loadNativeValue(ICell cell, IOpenClass paramType, IBindingContext bindingContext) {
        Object res = null;
        if (cell.getNativeType() == IGrid.CELL_TYPE_NUMERIC) {
            Class<?> expectedType = paramType.getInstanceClass();

            if (BigDecimal.class.isAssignableFrom(expectedType) || BigDecimalValue.class
                .isAssignableFrom(expectedType)) {
                // Convert String -> BigDecimal instead of double ->BigDecimal,
                // otherwise we lose in precision (part of EPBDS-5879)
                IObjectToDataConvertor objectConvertor = ObjectToDataConvertorFactory.getConvertor(expectedType,
                    String.class);
                res = objectConvertor.convert(cell.getStringValue(), bindingContext);
            } else {
                double value = cell.getNativeNumber();
                IObjectToDataConvertor objectConvertor = ObjectToDataConvertorFactory.getConvertor(expectedType,
                    double.class);
                if (objectConvertor != ObjectToDataConvertorFactory.NO_Convertor) {
                    res = objectConvertor.convert(value, bindingContext);
                } else {
                    objectConvertor = ObjectToDataConvertorFactory.getConvertor(expectedType, Double.class);
                    if (objectConvertor != ObjectToDataConvertorFactory.NO_Convertor) {
                        res = objectConvertor.convert(value, bindingContext);
                    } else {
                        objectConvertor = ObjectToDataConvertorFactory.getConvertor(expectedType, Date.class);
                        if (objectConvertor != ObjectToDataConvertorFactory.NO_Convertor) {
                            Date dateValue = cell.getNativeDate();
                            res = objectConvertor.convert(dateValue, bindingContext);
                        } else if (((int) value) == value) {
                            objectConvertor = ObjectToDataConvertorFactory.getConvertor(expectedType, Integer.class);
                            if (objectConvertor != ObjectToDataConvertorFactory.NO_Convertor)
                                res = objectConvertor.convert((int) value, bindingContext);

                        }
                    }
                }
            }
        }
        return res;
    }

    public static void setMetaInfoWithNodeUsageForConstantCell(ICell sourceCell,
            String cellCode,
            IOpenField openField,
            IBindingContext bindingContext) {
        if (!bindingContext.isExecutionMode()) {
            ConstantOpenField constantOpenField = (ConstantOpenField) openField;
            CellMetaInfo metaInfo = sourceCell.getMetaInfo();
            if (metaInfo == null) {
                metaInfo = new CellMetaInfo(CellMetaInfo.Type.DT_CA_CODE,
                    null,
                    JavaOpenClass.STRING,
                    false,
                    Collections.<NodeUsage> emptyList());
            }

            List<NodeUsage> nodeUsages = new ArrayList<NodeUsage>();
            String description = MethodUtil.printType(constantOpenField.getType()) + " " + constantOpenField
                .getName() + " = " + constantOpenField.getValueAsString();
            if (metaInfo.getUsedNodes() != null && !metaInfo.getUsedNodes().isEmpty()) {
                nodeUsages.addAll(metaInfo.getUsedNodes());
            } else {
                nodeUsages.add(new SimpleNodeUsage(0,
                    cellCode.length() - 1,
                    description,
                    constantOpenField.getUri(),
                    NodeType.OTHER));
            }
            metaInfo.setUsedNodes(nodeUsages);
            sourceCell.setMetaInfo(metaInfo);
        }
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
            OpenlToolAdaptor openlAdapter,
            String source,
            boolean isPartOfArray) throws SyntaxNodeException {

        // TODO: parse values considering underlying excel format. Note: this
        // class doesn't know anything about Excel. Keep it storage format
        // agnostic (don't introduce excel dependencies). Also consider adding
        // meta info.
        if (source != null && (source = source.trim()).length() != 0) {
            if (openlAdapter.getHeader() != null) {
                IOpenMethodHeader old_header = openlAdapter.getHeader();
                OpenMethodHeader newHeader = new OpenMethodHeader(old_header.getName(),
                    paramType,
                    old_header.getSignature(),
                    old_header.getDeclaringClass());
                openlAdapter.setHeader(newHeader);

                if (source.startsWith("{") && source.endsWith("}")) {
                    GridCellSourceCodeModule srcCode = new GridCellSourceCodeModule(cell.getSource(),
                        openlAdapter.getBindingContext());

                    return openlAdapter.makeMethod(srcCode);
                }

                if (source.startsWith("=") && (source.length() > 2 || source.length() == 2 && Character
                    .isLetterOrDigit(source.charAt(1)))) {

                    GridCellSourceCodeModule gridSource = new GridCellSourceCodeModule(cell.getSource(),
                        openlAdapter.getBindingContext());
                    IOpenSourceCodeModule code = new SubTextSourceCodeModule(gridSource, 1);

                    return openlAdapter.makeMethod(code);
                }
            }

            Class<?> expectedType = paramType.getInstanceClass();

            // Set cell meta information at first.
            //
            if (!openlAdapter.getBindingContext().isExecutionMode()) {
                setCellMetaInfo(cell, paramName, paramType, isPartOfArray);
            }

            // Try to get cell object value with appropriate string parser.
            // A parser instance will be selected using expected type of cell
            // value.
            //
            Object result = null;

            try {
                IBindingContext bindingContext = openlAdapter.getBindingContext();
                // Pasre as constant value
                ConstantOpenField constantOpenField = findConstantField(bindingContext, source);
                ICell theValueCell = cell.getSource().getCell(0, 0);
                if (constantOpenField != null) {
                    setMetaInfoWithNodeUsageForConstantCell(theValueCell,
                        theValueCell.getStringValue(),
                        constantOpenField,
                        bindingContext);
                    if (constantOpenField.getValue() != null) {
                        result = castConstantToExpectedType(bindingContext, constantOpenField, paramType);
                    }
                } else {
                    if (String.class.equals(paramType.getInstanceClass())) {
                        result = String2DataConvertorFactory.parse(expectedType, source, bindingContext);
                    } else {
                        if (theValueCell.hasNativeType()) {
                            result = loadNativeValue(paramType, paramName, ruleName, cell, openlAdapter, theValueCell);
                        }
                        if (result == null) {
                            result = String2DataConvertorFactory.parse(expectedType, source, bindingContext);
                        }
                    }
                }

            } catch (Exception e) {
                // Parsing of loaded string value can be sophisticated process.
                // As a result various exception types can be thrown (e.g.
                // CompositeSyntaxNodeException) with not user-friendly message.
                //
                String message = String.format("Cannot parse cell value '%s'. Expected value of type '%s'.",
                    source,
                    expectedType.getSimpleName());
                IGridTable cellTable = getTopLeftCellFromMergedRegion(cell.getSource());
                IOpenSourceCodeModule cellSourceCodeModule = new GridCellSourceCodeModule(cellTable,
                    openlAdapter.getBindingContext());
                if (e instanceof CompositeSyntaxNodeException) {
                    throw SyntaxNodeExceptionUtils.createError(message, cellSourceCodeModule);
                } else {
                    throw SyntaxNodeExceptionUtils.createError(message, e, null, cellSourceCodeModule);
                }
            }

            if (result instanceof IMetaHolder) {
                setMetaInfo((IMetaHolder) result, cell, paramName, ruleName, openlAdapter.getBindingContext());
            }

            try {
                validateValue(result, paramType);
            } catch (Exception e) {
                String message = String.format("Invalid cell value '%s'", source);
                IGridTable cellTable = getTopLeftCellFromMergedRegion(cell.getSource());
                IOpenSourceCodeModule cellSourceCodeModule = new GridCellSourceCodeModule(cellTable,
                    openlAdapter.getBindingContext());

                throw SyntaxNodeExceptionUtils.createError(message, e, null, cellSourceCodeModule);
            }

            return result;
        } else {
            // Set meta info for empty cells. To suggest an appropriate editor
            // according to cell type.
            if (!openlAdapter.getBindingContext().isExecutionMode())
                setCellMetaInfo(cell, paramName, paramType, false);
        }

        return null;
    }

    /**
     * The cell in merged region must point to the top left cell of the region. If the cell is not in merged region,
     * return cellTable object unchanged. If cellTable is not cell return cellTable object unchanged.
     *
     * @param cellTable original cell
     * @return top left cell if region is merged or the cell itself otherwise
     */
    public static IGridTable getTopLeftCellFromMergedRegion(IGridTable cellTable) {
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

    public static boolean isFormula(ILogicalTable valuesTable) {

        String stringValue = valuesTable.getSource().getCell(0, 0).getStringValue();

        if (stringValue != null) {
            stringValue = stringValue.trim();
            return stringValue.startsWith("=");
        }

        return false;
    }

    public static void setCellMetaInfo(ILogicalTable logicalCell,
            String paramName,
            IOpenClass paramType,
            boolean isMultiValue) {
        CellMetaInfo meta = new CellMetaInfo(CellMetaInfo.Type.DT_DATA_CELL, paramName, paramType, isMultiValue);
        ICell cell = logicalCell.getSource().getCell(0, 0);

        if (cell.getMetaInfo() != null && cell.getMetaInfo().getUsedNodes() != null) {
            meta.setUsedNodes(cell.getMetaInfo().getUsedNodes());
        }

        cell.setMetaInfo(meta);
    }

    public static void setCellMetaInfoWithNodeUsage(ILogicalTable logicalCell,
            IdentifierNode identifier,
            IMetaInfo metaInfo,
            NodeType nodeType) {
        if (metaInfo != null) {
            SimpleNodeUsage nodeUsage = new SimpleNodeUsage(identifier,
                metaInfo.getDisplayName(INamedThing.SHORT),
                metaInfo.getSourceUrl(),
                nodeType);
            CellMetaInfo meta = new CellMetaInfo(CellMetaInfo.Type.DT_CA_CODE,
                null,
                JavaOpenClass.STRING,
                false,
                Collections.singletonList(nodeUsage));
            ICell cell = logicalCell.getSource().getCell(0, 0);
            cell.setMetaInfo(meta);
        }
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
            // return;
        } else if (value.getClass().isArray()) {
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

    private static Object loadEmptyCellParams(ILogicalTable dataTable,
            String paramName,
            String ruleName,
            OpenlToolAdaptor openlAdaptor,
            IOpenClass paramType) {
        if (!openlAdaptor.getBindingContext().isExecutionMode()) {
            if (paramType.isArray()) {
                IOpenClass arrayType = paramType.getAggregateInfo().getComponentType(paramType);
                setCellMetaInfo(dataTable, paramName, arrayType, true);
            } else {
                setCellMetaInfo(dataTable, paramName, paramType, false);
            }
        }
        return null;
    }

    public static Object loadParam(ILogicalTable dataTable,
            IOpenClass paramType,
            String paramName,
            String ruleName,
            OpenlToolAdaptor openlAdaptor,
            boolean indexed) throws SyntaxNodeException {

        if (!indexed) {
            return loadSingleParam(paramType, paramName, ruleName, dataTable, openlAdaptor);
        }

        dataTable = LogicalTableHelper.make1ColumnTable(dataTable);

        int height = RuleRowHelper.calculateHeight(dataTable);

        boolean oneCellTable = height == 1;

        if (height == 0) {
            return loadEmptyCellParams(dataTable, paramName, ruleName, openlAdaptor, paramType);
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
            try {
                return loadSingleParam(paramType, paramName, ruleName, dataTable, openlAdaptor);
            } catch (Exception e) {

                Log.debug(e);
                // do nothing, assume the type was wrong or this was not an
                // expression
                // let the regular flow of events take it's course
            }
        }

        // Load parameter value as an array of values.
        //

        IOpenClass arrayType = paramType.getAggregateInfo().getComponentType(paramType);

        boolean isFormula = isFormula(dataTable);

        if (oneCellTable) {
            if (!isFormula) {
                // load comma separated array
                return loadCommaSeparatedArrayParams(dataTable, paramName, ruleName, openlAdaptor, arrayType);
            } else {
                return loadSingleParam(paramType, paramName, ruleName, dataTable, openlAdaptor);
            }
        } else {
            return loadSimpleArrayParams(dataTable, paramName, ruleName, openlAdaptor, arrayType);
        }
    }

    private static Object loadCommaSeparatedArrayParams(ILogicalTable dataTable,
            String paramName,
            String ruleName,
            OpenlToolAdaptor openlAdaptor,
            IOpenClass paramType) throws SyntaxNodeException {

        ILogicalTable paramSource = dataTable.getRow(0);
        Object params = RuleRowHelper
            .loadCommaSeparatedParam(paramType, paramName, ruleName, paramSource, openlAdaptor);
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
                methodsList = new ArrayList<CompositeMethod>(addMethod(methodsList, (CompositeMethod) paramsArray[i]));
            } else {
                Array.set(ary, i, paramsArray[i]);
            }
        }

        return methodsList == null ? ary
                                   : new ArrayHolder(paramType,
                                       methodsList.toArray(new CompositeMethod[methodsList.size()]));
    }

    private static List<CompositeMethod> addMethod(List<CompositeMethod> methods, CompositeMethod method) {
        if (methods == null) {
            methods = new ArrayList<CompositeMethod>();
        }
        methods.add(method);

        return methods;
    }

    private static Object loadSimpleArrayParams(ILogicalTable dataTable,
            String paramName,
            String ruleName,
            OpenlToolAdaptor openlAdaptor,
            IOpenClass paramType) throws SyntaxNodeException {

        int height = RuleRowHelper.calculateHeight(dataTable);

        List<CompositeMethod> methodsList = null;
        List<Object> values = new ArrayList<Object>();

        for (int i = 0; i < height; i++) { // load array values represented as
            // number of cells
            ILogicalTable cell = dataTable.getRow(i);
            Object parameter = RuleRowHelper.loadSingleParam(paramType, paramName, ruleName, cell, openlAdaptor);

            if (parameter instanceof CompositeMethod) {
                methodsList = new ArrayList<CompositeMethod>(addMethod(methodsList, (CompositeMethod) parameter));
            } else {
                if (parameter != null) {
                    values.add(parameter);
                }
            }
        }

        Object ary = paramType.getAggregateInfo().makeIndexedAggregate(paramType, values.size());

        for (int i = 0; i < values.size(); i++) {
            Array.set(ary, i, values.get(i));
        }

        return methodsList == null ? ary
                                   : new ArrayHolder(paramType,
                                       methodsList.toArray(new CompositeMethod[methodsList.size()]));

    }
}
