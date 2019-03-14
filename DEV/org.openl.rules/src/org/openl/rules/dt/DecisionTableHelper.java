package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.fuzzy.OpenLFuzzySearch;
import org.openl.rules.fuzzy.Token;
import org.openl.rules.helpers.CharRange;
import org.openl.rules.helpers.CharRangeParser;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.DoubleRangeParser;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.helpers.IntRangeParser;
import org.openl.rules.helpers.StringRange;
import org.openl.rules.helpers.StringRangeParser;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.DTColumnsDefinition;
import org.openl.rules.lang.xls.binding.XlsDefinitions;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.load.SimpleSheetLoader;
import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.DecisionTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.text.TextInfo;

public class DecisionTableHelper {

    private final static String RET1_COLUMN_NAME = DecisionTableColumnHeaders.RETURN.getHeaderKey() + "1";
    private final static String CRET1_COLUMN_NAME = DecisionTableColumnHeaders.COLLECT_RETURN.getHeaderKey() + "1";
    private final static String KEY1_COLUMN_NAME = DecisionTableColumnHeaders.KEY.getHeaderKey() + "1";
    private static final List<String> INT_TYPES = Arrays.asList("byte",
        "short",
        "int",
        "java.lang.Byte",
        "org.openl.meta.ByteValue",
        "org.openl.meta.ShortValue",
        "org.openl.meta.IntValue",
        "org.openl.meta.BigIntegerValue",
        "java.lang.Integer",
        "org.openl.meta.IntegerValue");
    private static final List<String> DOUBLE_TYPES = Arrays.asList("long",
        "float",
        "double",
        "java.lang.Long",
        "java.lang.Float",
        "java.lang.Double",
        "org.openl.meta.LongValue",
        "org.openl.meta.FloatValue",
        "org.openl.meta.DoubleValue",
        "org.openl.meta.BigDecimalValue");
    private static final List<String> CHAR_TYPES = Arrays.asList("char", "java.lang.Character");
    private static final List<String> STRINGS_TYPES = Arrays.asList("java.lang.String", "org.openl.meta.StringValue");
    private static final Pattern MAYBE_INT_ARRAY_PATTERN = Pattern.compile("\\s*(\\d+,)*\\d+\\s*");

    /**
     * Check if table is vertical.<br>
     * Vertical table is when conditions are represented from left to right, table is reading from top to bottom.</br>
     * Example of vertical table:
     *
     * <table cellspacing="2">
     * <tr>
     * <td align="center" bgcolor="#ccffff"><b>Rule</b></td>
     * <td align="center" bgcolor="#ccffff"><b>C1</b></td>
     * <td align="center" bgcolor="#ccffff"><b>C2</b></td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#ccffff"></td>
     * <td align="center" bgcolor="#ccffff">paramLocal1==paramInc</td>
     * <td align="center" bgcolor="#ccffff">paramLocal2==paramInc</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#ccffff"></td>
     * <td align="center" bgcolor="#ccffff">String paramLocal1</td>
     * <td align="center" bgcolor="#ccffff">String paramLocal2</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule</td>
     * <td align="center" bgcolor="#ffff99">Local Param 1</td>
     * <td align="center" bgcolor="#ffff99">Local Param 2</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule1</td>
     * <td align="center" bgcolor="#ffff99">value11</td>
     * <td align="center" bgcolor="#ffff99">value21</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule2</td>
     * <td align="center" bgcolor="#ffff99">value12</td>
     * <td align="center" bgcolor="#ffff99">value22</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule3</td>
     * <td align="center" bgcolor="#ffff99">value13</td>
     * <td align="center" bgcolor="#ffff99">value23</td>
     * </tr>
     * </table>
     *
     * @param table checked table
     * @return <code>TRUE</code> if table is vertical.
     */
    static boolean looksLikeVertical(ILogicalTable table) {

        if (table.getWidth() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            return true;
        }

        if (table.getHeight() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            return false;
        }

        int cnt1 = countConditionsAndActions(table);
        int cnt2 = countConditionsAndActions(table.transpose());

        if (cnt1 != cnt2) {
            return cnt1 > cnt2;
        }

        return table.getWidth() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER;
    }

    static boolean isValidConditionHeader(String s) {
        return s.length() >= 2 && s.charAt(0) == DecisionTableColumnHeaders.CONDITION.getHeaderKey()
            .charAt(0) && Character.isDigit(s.charAt(1));
    }

    static boolean isValidHConditionHeader(String headerStr) {
        return headerStr.startsWith(
            DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey()) && headerStr.length() > 2 && Character
                .isDigit(headerStr.charAt(2));
    }

    static boolean isValidMergedConditionHeader(String headerStr) {
        return headerStr.startsWith(
            DecisionTableColumnHeaders.MERGED_CONDITION.getHeaderKey()) && headerStr.length() > 2 && Character
                .isDigit(headerStr.charAt(2));
    }

    static boolean isValidActionHeader(String s) {
        return s.length() >= 2 && s.charAt(0) == DecisionTableColumnHeaders.ACTION.getHeaderKey().charAt(0) && Character
            .isDigit(s.charAt(1));
    }

    static boolean isValidRetHeader(String s) {
        return s.length() >= 3 && s.startsWith(
            DecisionTableColumnHeaders.RETURN.getHeaderKey()) && (s.length() == 3 || Character.isDigit(s.charAt(3)));
    }

    static boolean isValidKeyHeader(String s) {
        return s.length() >= 3 && s.startsWith(
            DecisionTableColumnHeaders.KEY.getHeaderKey()) && (s.length() == 3 || Character.isDigit(s.charAt(3)));
    }

    static boolean isValidCRetHeader(String s) {
        return s.length() >= 4 && s.startsWith(DecisionTableColumnHeaders.COLLECT_RETURN
            .getHeaderKey()) && (s.length() == 4 || Character.isDigit(s.charAt(4)));
    }

    static boolean isValidRuleHeader(String s) {
        return s.equals(DecisionTableColumnHeaders.RULE.getHeaderKey());
    }

    static boolean isConditionHeader(String s) {
        return isValidConditionHeader(s) || isValidHConditionHeader(s) || isValidMergedConditionHeader(s);
    }

    private static int countConditionsAndActions(ILogicalTable table) {

        int width = table.getWidth();
        int count = 0;

        for (int i = 0; i < width; i++) {

            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();

            if (value != null) {
                value = value.toUpperCase();
                count += isValidConditionHeader(value) || isValidActionHeader(value) || isValidRetHeader(
                    value) || isValidCRetHeader(value) || isValidKeyHeader(value) ? 1 : 0;
            }
        }

        return count;
    }

    /**
     * Checks if given table contain any horizontal condition header.
     *
     * @param table checked table
     * @return true if there is is any horizontal condition header in the table.
     */
    public static boolean hasHConditions(ILogicalTable table) {
        return countHConditions(table) > 0;
    }

    /**
     * Creates virtual headers for condition and return columns to load simple Decision Table as an usual Decision Table
     *
     * @param decisionTable method description for simple Decision Table.
     * @param originalTable The original body of simple Decision Table.
     * @param numberOfHcondition The number of horizontal conditions. In SimpleRules it == 0 in SimpleLookups > 0
     * @return prepared usual Decision Table.
     */
    static ILogicalTable preprocessSimpleDecisionTable(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            ILogicalTable originalTable,
            int numberOfHcondition,
            int numberOfMergedRows,
            boolean isSmartDecisionTable,
            boolean isCollectTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        IWritableGrid virtualGrid = createVirtualGrid();
        writeVirtualHeadersForSimpleDecisionTable(tableSyntaxNode,
            virtualGrid,
            originalTable,
            decisionTable,
            numberOfHcondition,
            numberOfMergedRows,
            isSmartDecisionTable,
            isCollectTable,
            bindingContext);

        // If the new table header size bigger than the size of the old table we
        // use the new table size
        int sizeOfVirtualGridTable = virtualGrid.getMaxColumnIndex(0) < originalTable.getSource()
            .getWidth() ? originalTable.getSource().getWidth() - 1 : virtualGrid.getMaxColumnIndex(0) - 1;
        GridTable virtualGridTable = new GridTable(0,
            0,
            IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1,
            sizeOfVirtualGridTable/* originalTable.getSource().getWidth() - 1 */,
            virtualGrid);

        IGrid grid = new CompositeGrid(new IGridTable[] { virtualGridTable, originalTable.getSource() }, true);

        // If the new table header size bigger than the size of the old table we
        // use the new table size
        int sizeofGrid = virtualGridTable.getWidth() < originalTable.getSource().getWidth() ? originalTable.getSource()
            .getWidth() - 1 : virtualGridTable.getWidth() - 1;

        return LogicalTableHelper.logicalTable(new GridTable(0,
            0,
            originalTable.getSource().getHeight() + IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1,
            sizeofGrid /* originalTable.getSource().getWidth() - 1 */,
            grid));
    }

    private static void writeVirtualHeadersForSimpleDecisionTable(TableSyntaxNode tableSyntaxNode,
            IWritableGrid grid,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            int numberOfHcondition,
            int numberOfMergedRows,
            boolean isSmartDecisionTable,
            boolean isCollectTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        List<DTHeader> dtHeaders = buildDTHeaders(tableSyntaxNode,
            originalTable,
            decisionTable,
            numberOfHcondition,
            isSmartDecisionTable,
            isCollectTable,
            bindingContext);

        List<DTHeader> conditions = dtHeaders.stream().filter(e -> e.isCondition()).collect(Collectors.toList());

        Integer column = writeConditions(tableSyntaxNode,
            grid,
            originalTable,
            decisionTable,
            conditions,
            numberOfHcondition,
            isSmartDecisionTable,
            isCollectTable,
            bindingContext);

        List<DTHeader> actions = dtHeaders.stream().filter(e -> e.isAction()).collect(Collectors.toList());

        column = writeActions(tableSyntaxNode,
            grid,
            originalTable,
            decisionTable,
            actions,
            numberOfHcondition,
            isSmartDecisionTable,
            isCollectTable,
            column,
            bindingContext);

        writeReturn(tableSyntaxNode,
            grid,
            originalTable,
            decisionTable,
            column,
            dtHeaders,
            numberOfMergedRows,
            numberOfHcondition > 0,
            isSmartDecisionTable,
            isCollectTable,
            bindingContext);
    }

    private static boolean isCompoundReturnType(IOpenClass compoundType) {
        if (compoundType.getConstructor(IOpenClass.EMPTY) == null) {
            return false;
        }

        int count = 0;
        for (IOpenMethod method : compoundType.getMethods()) {
            if (OpenLFuzzySearch.isSetterMethod(method)) {
                count++;
            }
        }
        return count > 0;
    }

    private static void validateCompoundReturnType(IOpenClass compoundType) throws OpenLCompilationException {
        try {
            compoundType.getInstanceClass().getConstructor();
        } catch (Exception e) {
            throw new OpenLCompilationException(
                String.format("Invalid compound return type: There is no default constructor found in return type '%s'",
                    compoundType.getDisplayName(0)));
        }
    }

    private static void writeCompoundReturnColumns(TableSyntaxNode tableSyntaxNode,
            IWritableGrid grid,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            int firstReturnColumn,
            int numberOfMergedRows,
            List<DTHeader> headers,
            boolean isSmartDecisionTable,
            boolean isCollectTable,
            int retParameterIndex,
            IOpenClass compoundType,
            IBindingContext bindingContext) throws OpenLCompilationException {

        validateCompoundReturnType(compoundType);
        int compoundReturnColumnsCount = calculateReturnColumnsCount(originalTable,
            firstReturnColumn,
            numberOfMergedRows);

        StringBuilder sb = new StringBuilder();
        sb.append(compoundType.getName()).append(" ret = new ").append(compoundType.getName()).append("();");

        if (isSmartDecisionTable) {
            // Set conditions parameters to compound type. Recursively search is
            // not supported.
            for (DTHeader condition : headers) {
                if (condition
                    .isCondition() && (condition instanceof SimpleDTHeader || condition instanceof FuzzyDTHeader)) {
                    String condtionTitle;
                    if (condition instanceof SimpleDTHeader) {
                        condtionTitle = ((SimpleDTHeader) condition).getTitle();
                    } else {
                        condtionTitle = ((FuzzyDTHeader) condition).getTitle();
                    }
                    try {
                        IOpenMethod bestMatchedConditionMethod = findBestMatchOpenMethod(condtionTitle,
                            compoundType,
                            decisionTable.getSignature().getParameterType(condition.getMethodParameterIndex()),
                            bindingContext,
                            true);
                        sb.append("ret.");
                        sb.append(bestMatchedConditionMethod.getName());
                        sb.append("(");
                        sb.append(String.valueOf(
                            decisionTable.getSignature().getParameterName(condition.getMethodParameterIndex())));
                        sb.append(");");
                    } catch (OpenLCompilationException e) {
                    }
                }
            }
        }

        Set<String> generatedNames = new HashSet<>();
        while (generatedNames.size() < compoundReturnColumnsCount) {
            generatedNames.add(RandomStringUtils.random(8, true, false));
        }
        String[] compoundColumnParamNames = generatedNames.toArray(new String[] {});
        int column = firstReturnColumn;
        Map<String, Map<IOpenMethod, String>> variables = new HashMap<>();
        for (int i = 0; i < compoundReturnColumnsCount; i++) {
            StringBuilder fieldChainSb = null;
            IOpenClass type = compoundType;
            int h = 0;
            String currentVariable = "ret";
            int previoush = h;
            while (h < numberOfMergedRows) {
                String title = originalTable.getSource().getCell(column, h).getStringValue();

                previoush = h;
                h = h + originalTable.getSource().getCell(column, h).getHeight();

                IOpenMethod[] m = null;

                if (h < numberOfMergedRows) {
                    IOpenMethod bestMatchMethod = findBestMatchOpenMethod(title, type, null, bindingContext, false);
                    if (bestMatchMethod != null) {
                        m = new IOpenMethod[] { bestMatchMethod };
                    }
                }
                if (m == null) {
                    m = findBestMatchOpenMethodRecursivelyForReturnType(title, type);
                }

                if (!bindingContext.isExecutionMode()) {
                    if (fieldChainSb == null) {
                        fieldChainSb = new StringBuilder();
                    } else {
                        fieldChainSb.append(".");
                    }
                    fieldChainSb.append(buildStatementByMethodsChain(type, m).getLeft());
                }

                for (int j = 0; j < m.length; j++) {
                    String var = null;
                    type = m[j].getSignature().getParameterType(0);
                    if (h < numberOfMergedRows || j < m.length - 1) {
                        Map<IOpenMethod, String> vm = variables.get(currentVariable);
                        if (vm == null || vm.get(m[j]) == null) {
                            var = RandomStringUtils.random(8, true, false);
                            while (generatedNames.contains(var)) { // Prevent
                                                                   // variable
                                                                   // duplication
                                var = RandomStringUtils.random(8, true, false);
                            }
                            generatedNames.add(var);
                            sb.append(type.getName())
                                .append(" ")
                                .append(var)
                                .append(" = new ")
                                .append(type.getName())
                                .append("();");
                            vm = new HashMap<>();
                            vm.put(m[j], var);
                            variables.put(currentVariable, vm);
                        } else {
                            var = vm.get(m[j]);
                        }
                    }
                    sb.append(currentVariable).append(".");
                    sb.append(m[j].getName());
                    sb.append("(");
                    if (h < numberOfMergedRows || j < m.length - 1) {
                        sb.append(var);
                        currentVariable = var;
                    } else {
                        sb.append(compoundColumnParamNames[i]);
                    }
                    sb.append(");");
                }
            }

            grid.setCellValue(column, 2, type.getName() + " " + compoundColumnParamNames[i]);

            int mergedColumnsCounts = originalTable.getSource().getCell(column, numberOfMergedRows).getWidth();
            if (mergedColumnsCounts > 1) {
                grid.addMergedRegion(new GridRegion(2, column, 2, column + mergedColumnsCounts - 1));
            }

            if (!bindingContext.isExecutionMode()) {
                ICell cell = originalTable.getSource().getCell(column, previoush);
                String description = "Return for " + fieldChainSb.toString() + ": " + type
                    .getDisplayName(INamedThing.SHORT);

                writeReturnMetaInfo(tableSyntaxNode, cell, description);
            }

            column += mergedColumnsCounts;
        }

        sb.append("ret;");
        grid.setCellValue(firstReturnColumn, 1, sb.toString());

        if (firstReturnColumn < column - 1) {
            for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
                grid.addMergedRegion(new GridRegion(row, firstReturnColumn, row, column - 1));
            }
        }
    }

    private static void writeReturnMetaInfo(TableSyntaxNode tableSyntaxNode, ICell cell, String description) {
        MetaInfoReader metaReader = tableSyntaxNode.getMetaInfoReader();
        if (metaReader instanceof DecisionTableMetaInfoReader) {
            DecisionTableMetaInfoReader metaInfoReader = (DecisionTableMetaInfoReader) metaReader;
            metaInfoReader.addSimpleRulesReturn(cell.getAbsoluteRow(), cell.getAbsoluteColumn(), description);
        }
    }

    private static IOpenClass getCompoundReturnType(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            boolean isCollectTable,
            int retParameterIndex,
            IBindingContext bindingContext) throws OpenLCompilationException {
        IOpenClass compoundType;
        if (isCollectTable) {
            if (tableSyntaxNode.getHeader().getCollectParameters().length > 0) {
                compoundType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE,
                    tableSyntaxNode.getHeader().getCollectParameters()[retParameterIndex]);
            } else {
                if (decisionTable.getType().isArray()) {
                    compoundType = decisionTable.getType().getComponentClass();
                } else {
                    compoundType = decisionTable.getType();
                }
            }
        } else {
            compoundType = decisionTable.getType();
        }
        return compoundType;
    }

    private static Pair<String, IOpenClass> buildStatementByMethodsChain(IOpenClass type, IOpenMethod[] methodsChain) {
        StringBuilder fieldChainSb = new StringBuilder();
        for (int i = 0; i < methodsChain.length; i++) {
            IOpenField openField = type.getField(methodsChain[i].getName().substring(3), false);
            fieldChainSb.append(openField.getDisplayName(0));
            if (i < methodsChain.length - 1) {
                fieldChainSb.append(".");
            }
            if (methodsChain[i].getSignature().getNumberOfParameters() == 0) {
                type = methodsChain[i].getType();
            } else {
                type = methodsChain[i].getSignature().getParameterType(0);
            }
        }
        return Pair.of(fieldChainSb.toString(), type);
    }

    private static int calculateReturnColumnsCount(ILogicalTable originalTable,
            int firstReturnColumn,
            int numberOfMergedRows) {
        IGridTable gt = originalTable.getSource().getRow(numberOfMergedRows);
        int w = gt.getWidth();
        int w0 = 0;
        int i = 0;
        while (i < w) {
            if (i >= firstReturnColumn) {
                w0++;
            }
            i = i + gt.getCell(i, 0).getWidth();
        }
        return w0;
    }

    private static IOpenMethod findBestMatchOpenMethod(String title,
            IOpenClass openClass,
            IOpenClass paramType,
            IBindingContext bindingContext,
            boolean thowExceptionIfNotFound) throws OpenLCompilationException {
        Map<Token, IOpenMethod[]> openClassFuzzyTokens = OpenLFuzzySearch.tokensMapToOpenClassSetterMethods(openClass);

        String tokenizedTitleString = OpenLFuzzySearch.toTokenString(title);
        Token[] fuzzyBestMatches = OpenLFuzzySearch.openlFuzzyExtract(tokenizedTitleString,
            openClassFuzzyTokens.keySet().toArray(new Token[] {}));

        if (fuzzyBestMatches.length == 0) {
            if (thowExceptionIfNotFound) {
                throw new OpenLCompilationException(
                    String.format("No field match in the return type for the title '%s'.", title));
            } else {
                return null;
            }
        }
        if (fuzzyBestMatches.length > 1) {
            throw new OpenLCompilationException(
                String.format("More than one field match in the return type for the title '%s'.", title));
        }
        if (openClassFuzzyTokens
            .get(fuzzyBestMatches[0]) == null || openClassFuzzyTokens.get(fuzzyBestMatches[0]).length == 0) {
            if (thowExceptionIfNotFound) {
                throw new OpenLCompilationException(
                    String.format("No field match in the return type for the title '%s'.", title));
            } else {
                return null;
            }
        }
        if (openClassFuzzyTokens.get(fuzzyBestMatches[0]).length > 1) {
            throw new OpenLCompilationException(
                String.format("More than one field match in the return type for the title '%s'.", title));
        }

        IOpenMethod openMethod = openClassFuzzyTokens.get(fuzzyBestMatches[0])[0];
        if (paramType != null) {
            IOpenCast openCast = bindingContext.getCast(paramType, openMethod.getSignature().getParameterType(0));
            if (openCast == null || !openCast.isImplicit()) {
                if (thowExceptionIfNotFound) {
                    throw new OpenLCompilationException(
                        String.format("No field match in the return type for the title '%s'.", title));
                } else {
                    return null;
                }
            }
        }
        return openMethod;
    }

    private static IOpenMethod[] findBestMatchOpenMethodRecursivelyForReturnType(String title,
            IOpenClass openClass) throws OpenLCompilationException {
        Map<Token, IOpenMethod[][]> openClassFuzzyTokens = OpenLFuzzySearch
            .tokensMapToOpenClassSetterMethodsRecursively(openClass);

        String tokenizedTitleString = OpenLFuzzySearch.toTokenString(title);
        Token[] fuzzyBestMatches = OpenLFuzzySearch.openlFuzzyExtract(tokenizedTitleString,
            openClassFuzzyTokens.keySet().toArray(new Token[] {}));

        if (fuzzyBestMatches.length == 0) {
            throw new OpenLCompilationException(
                String.format("No field match in the return type for the title '%s'.", title));
        }
        if (fuzzyBestMatches.length > 1) {
            throw new OpenLCompilationException(
                String.format("More than one field match in the return type for the title '%s'.", title));
        }
        if (openClassFuzzyTokens
            .get(fuzzyBestMatches[0]) == null || openClassFuzzyTokens.get(fuzzyBestMatches[0]).length == 0) {
            throw new OpenLCompilationException(
                String.format("No field match in the return type for the title '%s'.", title));
        }
        if (openClassFuzzyTokens.get(fuzzyBestMatches[0]).length > 1) {
            throw new OpenLCompilationException(
                String.format("More than one field match in the return type for the title '%s'.", title));
        }
        return openClassFuzzyTokens.get(fuzzyBestMatches[0])[0];
    }

    private static void validateCollectSyntaxNode(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            ILogicalTable originalTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        int parametersCount = tableSyntaxNode.getHeader().getCollectParameters().length;
        IOpenClass type = decisionTable.getType();
        if ((type.isArray() || Collection.class.isAssignableFrom(type.getInstanceClass())) && parametersCount > 1) {
            throw new OpenLCompilationException(
                String.format("Error: Cannot bind node: '%s'. Found more than one parameter for '%s'.",
                    Tokenizer.firstToken(tableSyntaxNode.getHeader().getModule(), "").getIdentifier(),
                    type.getComponentClass().getDisplayName(0)));
        }
        if (Map.class.isAssignableFrom(type.getInstanceClass())) {
            if (parametersCount > 2) {
                throw new OpenLCompilationException(
                    String.format("Error: Cannot bind node: '%s'. Found more than two parameter for '%s'.",
                        Tokenizer.firstToken(tableSyntaxNode.getHeader().getModule(), "").getIdentifier(),
                        type.getDisplayName(0)));
            }
            if (parametersCount == 1) {
                throw new OpenLCompilationException(
                    String.format("Error: Cannot bind node: '%s'. Found only one parameter for '%s'.",
                        Tokenizer.firstToken(tableSyntaxNode.getHeader().getModule(), "").getIdentifier(),
                        type.getDisplayName(0)));
            }
        }
        for (String parameterType : tableSyntaxNode.getHeader().getCollectParameters()) {
            IOpenClass t = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, parameterType);
            if (t == null) {
                throw new OpenLCompilationException(
                    String.format("Error: Cannot bind node: '%s'. Cannot find type: '%s'.",
                        Tokenizer.firstToken(tableSyntaxNode.getHeader().getModule(), "").getIdentifier(),
                        parameterType));
            } else {
                if (type.isArray() && bindingContext.getCast(t, type.getComponentClass()) == null) {
                    throw new OpenLCompilationException(
                        String.format("Error: Cannot bind node: '%s'. Incompatible types: '%s' and '%s'.",
                            Tokenizer.firstToken(tableSyntaxNode.getHeader().getModule(), "").getIdentifier(),
                            type.getComponentClass().getDisplayName(0),
                            t.getDisplayName(0)));
                }
            }
        }
    }

    private static boolean writeReturnWithReturnDtHeader(TableSyntaxNode tableSyntaxNode,
            IWritableGrid grid,
            ILogicalTable originalTable,
            int firstReturnColumn,
            DeclaredDTHeader dtHeader,
            IBindingContext bindingContext) {
        grid.setCellValue(firstReturnColumn, 1, dtHeader.getStatement());
        DTColumnsDefinition dtColumnsDefinition = dtHeader.getMatchedDefinition().getDtColumnsDefinition();
        int c = firstReturnColumn;
        while (c < originalTable.getSource().getWidth()) {
            ICell cell = originalTable.getSource().getCell(c, 0);
            String d = cell.getStringValue();
            d = OpenLFuzzySearch.toTokenString(d);
            for (String title : dtColumnsDefinition.getTitles()) {
                if (Objects.equals(d, title)) {
                    List<IParameterDeclaration> localParameters = dtColumnsDefinition.getLocalParameters(title);
                    List<String> localParameterNames = new ArrayList<>();
                    List<IOpenClass> typeOfColumns = new ArrayList<>();
                    int column = c;
                    for (IParameterDeclaration param : localParameters) {
                        if (param != null) {
                            String paramName = dtHeader.getMatchedDefinition().getLocalParameterName(param.getName());
                            localParameterNames.add(paramName);
                            String value = param.getType().getName() + (paramName != null ? " " + paramName : "");
                            grid.setCellValue(column, 2, value);
                            typeOfColumns.add(param.getType());
                        } else {
                            typeOfColumns.add(dtHeader.getCompositeMethod().getType());
                        }

                        int h = originalTable.getSource().getCell(column, 0).getHeight();
                        int w1 = originalTable.getSource().getCell(column, h).getWidth();
                        if (w1 > 1) {
                            grid.addMergedRegion(new GridRegion(2, column, 2, column + w1 - 1));
                        }

                        column = column + w1;
                    }
                    if (!bindingContext.isExecutionMode()) {
                        String text = String.format("Parameter %s of return RET1 with expression %s: %s",
                            DecisionTableMetaInfoReader.toString(localParameterNames.toArray(new String[] {}), e -> e),
                            dtHeader.getStatement(),
                            DecisionTableMetaInfoReader.toString(typeOfColumns.toArray(new IOpenClass[] {}),
                                e -> e.getDisplayName(INamedThing.SHORT)));
                        writeReturnMetaInfo(tableSyntaxNode, cell, text);
                    }
                    break;
                }
            }
            c = c + cell.getWidth();
        }

        if (originalTable.getSource().getWidth() - firstReturnColumn > 1) {
            for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
                grid.addMergedRegion(
                    new GridRegion(row, firstReturnColumn, row, originalTable.getSource().getWidth() - 1));
            }
        }

        return true;
    }

    private static void writeReturn(TableSyntaxNode tableSyntaxNode,
            IWritableGrid grid,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            int firstColumnAfterConditionColumns,
            List<DTHeader> headers,
            int numberOfMergedRows,
            boolean isLookupTable,
            boolean isSmartDecisionTable,
            boolean isCollectTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        // write return column
        //
        int firstReturnColumn = firstColumnAfterConditionColumns;
        int retParameterIndex = 0;
        if (isCollectTable) {
            validateCollectSyntaxNode(tableSyntaxNode, decisionTable, originalTable, bindingContext);

            if (Map.class.isAssignableFrom(decisionTable.getType().getInstanceClass())) {
                grid.setCellValue(firstReturnColumn, 0, KEY1_COLUMN_NAME);
                if (tableSyntaxNode.getHeader().getCollectParameters().length > 0) {
                    grid.setCellValue(firstReturnColumn, 1, "keyRet");
                    grid.setCellValue(firstReturnColumn,
                        2,
                        tableSyntaxNode.getHeader().getCollectParameters()[retParameterIndex] + " " + "keyRet");
                    retParameterIndex++;
                }
                int mergedColumnsCounts = originalTable.getSource()
                    .getCell(firstReturnColumn, numberOfMergedRows)
                    .getWidth();
                firstReturnColumn = firstReturnColumn + mergedColumnsCounts;
            }

            grid.setCellValue(firstReturnColumn, 0, CRET1_COLUMN_NAME);
            if (tableSyntaxNode.getHeader().getCollectParameters().length > 0) {
                grid.setCellValue(firstReturnColumn, 1, "extraRet");
                grid.setCellValue(firstReturnColumn,
                    2,
                    tableSyntaxNode.getHeader().getCollectParameters()[retParameterIndex] + " " + "extraRet");
            } else {
                if (decisionTable.getType().isArray()) {
                    grid.setCellValue(firstReturnColumn, 1, "extraRet");
                    String componentClassName = decisionTable.getType().getComponentClass().getName();
                    if (decisionTable.getType().getAggregateInfo() != null) {
                        IOpenClass componentOpenClass = decisionTable.getType()
                            .getAggregateInfo()
                            .getComponentType(decisionTable.getType());
                        if (componentOpenClass != null) {
                            componentClassName = componentOpenClass.getName();
                        }
                    }
                    grid.setCellValue(firstReturnColumn, 2, componentClassName + " " + "extraRet");
                }
            }
        } else {
            grid.setCellValue(firstReturnColumn, 0, RET1_COLUMN_NAME);
        }

        if (!isLookupTable) {
            if (!isSmartDecisionTable && originalTable.getWidth() < headers.size()) {
                // if the physical number of columns for conditions is equals or
                // more than whole width of the table,
                // means there is no return column.
                //
                throw new OpenLCompilationException("Wrong table structure: There is no column for return values");
            }

            DeclaredDTHeader returnDtHeader = headers.stream()
                .filter(e -> (e instanceof DeclaredDTHeader) && e.isReturn())
                .map(e -> (DeclaredDTHeader) e)
                .findAny()
                .orElse(null);

            if (returnDtHeader != null) {
                writeReturnWithReturnDtHeader(tableSyntaxNode,
                    grid,
                    originalTable,
                    firstReturnColumn,
                    returnDtHeader,
                    bindingContext);
            } else {
                IOpenClass compoundType = getCompoundReturnType(tableSyntaxNode,
                    decisionTable,
                    isCollectTable,
                    retParameterIndex,
                    bindingContext);
                boolean mergeRetCells = true;
                if (isCompoundReturnType(compoundType)) {
                    try {
                        mergeRetCells = false;
                        writeCompoundReturnColumns(tableSyntaxNode,
                            grid,
                            originalTable,
                            decisionTable,
                            firstReturnColumn,
                            numberOfMergedRows,
                            headers,
                            isSmartDecisionTable,
                            isCollectTable,
                            retParameterIndex,
                            compoundType,
                            bindingContext);
                    } catch (OpenLCompilationException e) {
                        if (calculateReturnColumnsCount(originalTable, firstReturnColumn, numberOfMergedRows) == 1) {
                            mergeRetCells = true;
                        } else {
                            throw e;
                        }
                    }
                } else {
                    if (!bindingContext.isExecutionMode()) {
                        ICell cell = originalTable.getSource().getCell(firstReturnColumn, 0);
                        String description = "Return: " + decisionTable.getHeader()
                            .getType()
                            .getDisplayName(INamedThing.SHORT);

                        writeReturnMetaInfo(tableSyntaxNode, cell, description);
                    }
                }

                if (mergeRetCells) {
                    int mergedColumnsCounts = originalTable.getColumnWidth(originalTable.getWidth() - 1);
                    if (mergedColumnsCounts > 1) {
                        for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT; row++) {
                            grid.addMergedRegion(new GridRegion(row,
                                firstReturnColumn,
                                row,
                                firstReturnColumn + mergedColumnsCounts - 1));
                        }
                    }
                }
            }
        }
    }

    private static List<DTHeader> buildDTHeaders(TableSyntaxNode tableSyntaxNode,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            int numberOfHcondition,
            boolean isSmartDecisionTable,
            boolean isCollectTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        if (isSmartDecisionTable) {
            return findDTHeadersForSmartDecisionTable(tableSyntaxNode,
                originalTable,
                decisionTable,
                numberOfHcondition,
                isCollectTable,
                bindingContext);
        } else {
            int numberOfConditions = getNumberOfConditions(decisionTable);
            List<DTHeader> dtHeaders = new ArrayList<>();
            for (int i = 0; i < numberOfConditions; i++) {
                dtHeaders.add(new SimpleDTHeader(i,
                    decisionTable.getSignature().getParameterName(i),
                    null,
                    i,
                    i >= numberOfConditions - numberOfHcondition));
            }
            if (numberOfHcondition == 0) {
                dtHeaders.add(new SimpleReturnDTHeader(null, null, numberOfConditions));
            }
            return dtHeaders;
        }
    }

    private static int writeDeclaredDtHeader(IWritableGrid grid,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            DeclaredDTHeader dtHeader,
            String headerName,
            int column,
            IBindingContext bindingContext) {
        grid.setCellValue(column, 0, headerName);

        String statement = dtHeader.getStatement();
        grid.setCellValue(column, 1, statement);

        int firstColumn = column;

        for (int j = 0; j < dtHeader.getColumnParameters().length; j++) {
            int firstTitleColumn = column;
            List<String> parameterNames = new ArrayList<>();
            List<IOpenClass> typeOfColumns = new ArrayList<>();
            for (int k = 0; k < dtHeader.getColumnParameters()[j].length; k++) {
                IParameterDeclaration param = dtHeader.getColumnParameters()[j][k];
                if (param != null) {
                    String paramName = dtHeader.getMatchedDefinition().getLocalParameterName(param.getName());
                    parameterNames.add(paramName);
                    grid.setCellValue(column,
                        2,
                        param.getType().getName() + (paramName != null ? " " + paramName : ""));
                    typeOfColumns.add(param.getType());
                } else {
                    typeOfColumns.add(dtHeader.getCompositeMethod().getType());
                }
                int h = originalTable.getSource().getCell(column, 0).getHeight();
                int w1 = originalTable.getSource().getCell(column, h).getWidth();
                if (w1 > 1) {
                    grid.addMergedRegion(new GridRegion(2, column, 2, column + w1 - 1));
                }

                column = column + w1;
            }

            if (!bindingContext.isExecutionMode()) {
                if (dtHeader.isAction()) {
                    writeMetaInfoForAction(originalTable,
                        decisionTable,
                        firstTitleColumn,
                        headerName,
                        parameterNames.toArray(new String[] {}),
                        statement,
                        typeOfColumns.toArray(new IOpenClass[] {}));
                } else if (dtHeader.isCondition()) {
                    writeMetaInfoForVCondition(originalTable,
                        decisionTable,
                        firstTitleColumn,
                        headerName,
                        parameterNames.toArray(new String[] {}),
                        statement,
                        typeOfColumns.toArray(new IOpenClass[] {}));
                }
            }

        }
        // merge columns
        if (column - firstColumn > 1) {
            for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
                grid.addMergedRegion(new GridRegion(row, firstColumn, row, column - 1));
            }
        }
        return column;
    }

    private static int writeActions(TableSyntaxNode tableSyntaxNode,
            IWritableGrid grid,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            List<DTHeader> actions,
            int numberOfHcondition,
            boolean isSmartDecisionTable,
            boolean isCollectTable,
            int column,
            IBindingContext bindingContext) throws OpenLCompilationException {
        int i = 0;
        for (DTHeader action : actions) {
            if (column >= originalTable.getSource().getWidth()) {
                String message = "Wrong table structure: Wrong number of action columns!";
                throw new OpenLCompilationException(message);
            }

            DeclaredDTHeader declaredAction = (DeclaredDTHeader) action;
            String actionName = (DecisionTableColumnHeaders.ACTION.getHeaderKey() + (i + 1)).intern();
            column = writeDeclaredDtHeader(grid,
                originalTable,
                decisionTable,
                declaredAction,
                actionName,
                column,
                bindingContext);
            i++;
        }
        return column;
    }

    private static int writeConditions(TableSyntaxNode tableSyntaxNode,
            IWritableGrid grid,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            List<DTHeader> conditions,
            int numberOfHcondition,
            boolean isSmartDecisionTable,
            boolean isCollectTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        int column = 0;
        int vColumnCounter = 0;
        int hColumn = -1;
        int numberOfConditions = conditions.size();
        int i = 0;
        for (DTHeader condition : conditions) {
            if (column >= originalTable.getSource().getWidth()) {
                String message = "Wrong table structure: Columns count is less than parameters count";
                throw new OpenLCompilationException(message);
            }
            // write headers
            //
            boolean isThatVCondition = i < numberOfConditions - numberOfHcondition;
            boolean lastCondition = i + 1 == numberOfConditions;
            String conditionName;
            if (isThatVCondition) {
                vColumnCounter++;
                // write simple condition
                //
                if (i == 0 && numberOfHcondition == 0 && numberOfConditions < 2) {
                    conditionName = (DecisionTableColumnHeaders.MERGED_CONDITION.getHeaderKey() + (i + 1)).intern();
                } else {
                    conditionName = (DecisionTableColumnHeaders.CONDITION.getHeaderKey() + (i + 1)).intern();
                }
            } else {
                if (hColumn < 0) {
                    hColumn = column;
                }
                // write horizontal condition
                //
                conditionName = (DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey() + (i + 1)).intern();
            }

            if (condition instanceof DeclaredDTHeader) {
                column = writeDeclaredDtHeader(grid,
                    originalTable,
                    decisionTable,
                    (DeclaredDTHeader) condition,
                    conditionName,
                    column,
                    bindingContext);
            } else {
                grid.setCellValue(column, 0, conditionName);
                String conditionStatement = condition.getStatement();
                grid.setCellValue(column, 1, conditionStatement);

                IOpenClass typeOfCondition = decisionTable.getSignature().getParameterTypes()[condition
                    .getMethodParameterIndex()];
                if (condition instanceof FuzzyDTHeader) {
                    FuzzyDTHeader fuzzyCondition = (FuzzyDTHeader) condition;
                    if (fuzzyCondition.getMethodsChain() != null) {
                        typeOfCondition = fuzzyCondition.getMethodsChain()[fuzzyCondition.getMethodsChain().length - 1]
                            .getType();
                    }
                }

                // Set type of condition values(for Ranges and Array)
                Pair<String, IOpenClass> typeOfValue = checkTypeOfValues(bindingContext,
                    originalTable,
                    i,
                    typeOfCondition,
                    isThatVCondition,
                    lastCondition,
                    vColumnCounter);
                grid.setCellValue(column, 2, typeOfValue.getLeft());

                if (!bindingContext.isExecutionMode() && isThatVCondition) {
                    writeMetaInfoForVCondition(originalTable,
                        decisionTable,
                        column,
                        null,
                        null,
                        conditionStatement,
                        new IOpenClass[] { typeOfValue.getRight() });
                }

                // merge columns
                if (isThatVCondition) {
                    int mergedColumnsCounts = isThatVCondition ? originalTable.getColumnWidth(
                        i) : originalTable.getSource().getCell(vColumnCounter, i - vColumnCounter).getWidth();

                    if (mergedColumnsCounts > 1) {
                        for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT; row++) {
                            grid.addMergedRegion(new GridRegion(row, column, row, column + mergedColumnsCounts - 1));
                        }
                    }

                    column += mergedColumnsCounts;
                } else {
                    column++;
                }
            }
            i++;
        }

        if (!bindingContext.isExecutionMode()) {
            writeMetaInfoForHConditions(originalTable,
                decisionTable,
                conditions,
                numberOfHcondition,
                numberOfConditions,
                hColumn);
        }

        return column;
    }

    private static void writeMetaInfoForVCondition(ILogicalTable originalTable,
            DecisionTable decisionTable,
            int column,
            String conditionName,
            String[] parameterNames,
            String conditionStatement,
            IOpenClass[] typeOfColumns) {
        MetaInfoReader metaReader = decisionTable.getSyntaxNode().getMetaInfoReader();
        if (metaReader instanceof DecisionTableMetaInfoReader) {
            DecisionTableMetaInfoReader metaInfoReader = (DecisionTableMetaInfoReader) metaReader;
            ICell cell = originalTable.getSource().getCell(column, 0);
            metaInfoReader.addSimpleRulesCondition(cell.getAbsoluteRow(),
                cell.getAbsoluteColumn(),
                conditionName,
                parameterNames,
                conditionStatement,
                typeOfColumns);
        }
    }

    private static void writeMetaInfoForAction(ILogicalTable originalTable,
            DecisionTable decisionTable,
            int column,
            String conditionName,
            String[] parameterNames,
            String conditionStatement,
            IOpenClass[] typeOfColumns) {
        MetaInfoReader metaReader = decisionTable.getSyntaxNode().getMetaInfoReader();
        if (metaReader instanceof DecisionTableMetaInfoReader) {
            DecisionTableMetaInfoReader metaInfoReader = (DecisionTableMetaInfoReader) metaReader;
            ICell cell = originalTable.getSource().getCell(column, 0);
            metaInfoReader.addSimpleRulesAction(cell.getAbsoluteRow(),
                cell.getAbsoluteColumn(),
                conditionName,
                parameterNames,
                conditionStatement,
                typeOfColumns);
        }
    }

    private static void writeMetaInfoForHConditions(ILogicalTable originalTable,
            DecisionTable decisionTable,
            List<DTHeader> conditions,
            int numberOfHcondition,
            int numberOfConditions,
            int hColumn) {
        MetaInfoReader metaInfoReader = decisionTable.getSyntaxNode().getMetaInfoReader();
        int j = 0;
        for (int i = numberOfConditions - numberOfHcondition; i < numberOfConditions; i++) {
            int c = hColumn;
            while (c < originalTable.getSource().getWidth()) {
                ICell cell = originalTable.getSource().getCell(c, j);
                String cellValue = cell.getStringValue();
                if (cellValue != null) {
                    if (metaInfoReader instanceof DecisionTableMetaInfoReader) {
                        ((DecisionTableMetaInfoReader) metaInfoReader).addSimpleRulesCondition(cell.getAbsoluteRow(),
                            cell.getAbsoluteColumn(),
                            (DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey() + (i + 1)).intern(),
                            null,
                            decisionTable.getSignature().getParameterName(conditions.get(i).getMethodParameterIndex()),
                            new IOpenClass[] { decisionTable.getSignature()
                                .getParameterType(conditions.get(i).getMethodParameterIndex()) });
                    }
                }
                c = c + cell.getWidth();
            }
            j++;
        }
    }

    private static void parseRec(ISyntaxNode node,
            MutableBoolean chain,
            boolean inChain,
            List<IdentifierNode> identifierNodes) {
        for (int i = 0; i < node.getNumberOfChildren(); i++) {
            if (node.getChild(i) instanceof IdentifierNode) {
                if ("identifier".equals(node.getChild(i).getType())) {
                    if (!chain.booleanValue()) {
                        identifierNodes.add((IdentifierNode) node.getChild(i));
                        if (inChain) {
                            chain.setTrue();
                        }
                    }
                }
            } else {
                if ("chain".equals(node.getType())) {
                    boolean f = chain.booleanValue();
                    parseRec(node.getChild(i), chain, true, identifierNodes);
                    chain.setValue(f);
                } else {
                    parseRec(node.getChild(i), chain, inChain, identifierNodes);
                }
            }
        }
    }

    @SafeVarargs
    private static String replaceIdentifierNodeNamesInCode(String code,
            List<IdentifierNode> identifierNodes,
            Map<String, String>... namesMaps) {
        final TextInfo textInfo = new TextInfo(code);
        Collections.sort(identifierNodes,
            Comparator.<IdentifierNode> comparingInt(e -> e.getLocation().getStart().getAbsolutePosition(textInfo))
                .reversed());

        StringBuilder sb = new StringBuilder(code);
        for (IdentifierNode identifierNode : identifierNodes) {
            int start = identifierNode.getLocation().getStart().getAbsolutePosition(textInfo);
            int end = identifierNode.getLocation().getEnd().getAbsolutePosition(textInfo);
            for (Map<String, String> m : namesMaps) {
                if (m.containsKey(identifierNode.getIdentifier())) {
                    sb.replace(start, end + 1, m.get(identifierNode.getIdentifier()));
                }
            }
        }
        return sb.toString();
    }

    private static MatchedDefinition matchByDTColumnDefinition(DecisionTable decisionTable,
            DTColumnsDefinition definition,
            IBindingContext bindingContext) {
        IOpenMethodHeader header = decisionTable.getHeader();
        if (definition.isReturn()) {
            IOpenClass methodReturnType = header.getType();
            IOpenClass definitionType = definition.getCompositeMethod().getType();
            IOpenCast openCast = bindingContext.getCast(definitionType, methodReturnType);
            if (openCast == null || !openCast.isImplicit()) {
                return null;
            }
        }

        List<IdentifierNode> identifierNodes = new ArrayList<>();
        parseRec(definition.getCompositeMethod().getMethodBodyBoundNode().getSyntaxNode(),
            new MutableBoolean(false),
            false,
            identifierNodes);
        Set<String> methodParametersUsedInExpression = new HashSet<>();

        Map<String, IParameterDeclaration> localParameters = new HashMap<>();
        for (IParameterDeclaration localParameter : definition.getLocalParameters()) {
            localParameters.put(localParameter.getName(), localParameter);
        }

        for (IdentifierNode identifierNode : identifierNodes) {
            if (!localParameters.containsKey(identifierNode.getIdentifier())) {
                methodParametersUsedInExpression.add(identifierNode.getIdentifier());
            }
        }

        Map<String, String> methodParametersToRename = new HashMap<>();
        Set<Integer> usedMethodParameterIndexes = new HashSet<>();
        Iterator<String> itr = methodParametersUsedInExpression.iterator();
        MatchType matchType = MatchType.STRICT;
        Map<String, Integer> paramToIndex = new HashMap<>();
        while (itr.hasNext()) {
            String param = itr.next();
            int j = -1;
            for (int i = 0; i < definition.getHeader().getSignature().getNumberOfParameters(); i++) {
                if (param.equals(definition.getHeader().getSignature().getParameterName(i))) {
                    j = i;
                    break;
                }
            }
            if (j < 0) { // Constants, etc
                itr.remove();
                continue;
            }
            paramToIndex.put(param, j);
            IOpenClass type = definition.getHeader().getSignature().getParameterType(j);
            for (int i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
                if (param.equals(header.getSignature().getParameterName(i)) && type
                    .equals(header.getSignature().getParameterType(i))) {
                    usedMethodParameterIndexes.add(i);
                    methodParametersToRename.put(param, param);
                    break;
                }
            }
        }

        MatchType[] matchTypes = { MatchType.STRICT_CASTED,
                MatchType.METHOD_PARAMS_RENAMED,
                MatchType.METHOD_PARAMS_RENAMED_CASTED };

        for (MatchType mt : matchTypes) {
            itr = methodParametersUsedInExpression.iterator();
            while (itr.hasNext()) {
                String param = itr.next();
                int j = paramToIndex.get(param);
                IOpenClass type = definition.getHeader().getSignature().getParameterType(j);
                boolean duplicatedMatch = false;
                for (int i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
                    boolean predicate = true;
                    IOpenCast openCast = bindingContext.getCast(header.getSignature().getParameterType(i), type);
                    switch (mt) {
                        case METHOD_PARAMS_RENAMED_CASTED:
                            predicate = openCast != null && openCast.isImplicit();
                            break;
                        case STRICT_CASTED:
                            predicate = openCast != null && openCast.isImplicit() && param
                                .equals(header.getSignature().getParameterName(i));
                            break;
                        case METHOD_PARAMS_RENAMED:
                            predicate = type.equals(header.getSignature().getParameterType(i));
                            break;
                        default:
                            throw new IllegalStateException();
                    }

                    if (!usedMethodParameterIndexes.contains(i) && predicate) {
                        if (duplicatedMatch) {
                            return null;
                        }
                        duplicatedMatch = true;
                        matchType = mt;
                        usedMethodParameterIndexes.add(i);
                        String newParam = null;
                        switch (mt) {
                            case STRICT_CASTED:
                            case METHOD_PARAMS_RENAMED_CASTED:
                                String typeName = type.getInstanceClass().getSimpleName();
                                if (bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName) == null) {
                                    typeName = type.getJavaName();
                                }
                                newParam = "((" + typeName + ")" + header.getSignature().getParameterName(i) + ")";
                                break;
                            case METHOD_PARAMS_RENAMED:
                                newParam = header.getSignature().getParameterName(i);
                                break;
                            default:
                                throw new IllegalStateException();
                        }
                        methodParametersToRename.put(param, newParam);
                    }
                }
            }
        }

        if (usedMethodParameterIndexes.size() != methodParametersUsedInExpression.size()) {
            return null;
        }

        Set<String> methodParameterNames = new HashSet<>();
        for (int i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
            methodParameterNames.add(header.getSignature().getParameterName(i));
        }

        Map<String, String> renamedLocalParameters = new HashMap<>();
        for (String paramName : methodParameterNames) {
            if (localParameters.containsKey(paramName)) {
                int k = 1;
                String newParamName = "_" + paramName;
                while (localParameters.containsKey(newParamName) || renamedLocalParameters
                    .containsValue(newParamName) || methodParameterNames.contains(newParamName)) {
                    newParamName = "_" + paramName + "_" + k;
                    k++;
                }
                renamedLocalParameters.put(paramName, newParamName);
            }
        }

        final String code = definition.getCompositeMethod()
            .getMethodBodyBoundNode()
            .getSyntaxNode()
            .getModule()
            .getCode();

        String newCode = replaceIdentifierNodeNamesInCode(code,
            identifierNodes,
            methodParametersToRename,
            renamedLocalParameters);

        int[] usedMethodParameterIndexesArray = ArrayUtils
            .toPrimitive(usedMethodParameterIndexes.toArray(new Integer[] {}));

        switch (matchType) {
            case STRICT:
                return new MatchedDefinition(definition,
                    newCode,
                    usedMethodParameterIndexesArray,
                    renamedLocalParameters,
                    renamedLocalParameters.isEmpty() ? MatchType.STRICT : MatchType.STRICT_LOCAL_PARAMS_RENAMED);
            case STRICT_CASTED:
                return new MatchedDefinition(definition,
                    newCode,
                    usedMethodParameterIndexesArray,
                    renamedLocalParameters,
                    renamedLocalParameters.isEmpty() ? MatchType.STRICT_CASTED
                                                     : MatchType.STRICT_CASTED_LOCAL_PARAMS_RENAMED);
            case METHOD_PARAMS_RENAMED:
                return new MatchedDefinition(definition,
                    newCode,
                    usedMethodParameterIndexesArray,
                    renamedLocalParameters,
                    renamedLocalParameters.isEmpty() ? MatchType.METHOD_PARAMS_RENAMED
                                                     : MatchType.METHOD_LOCAL_PARAMS_RENAMED);
            case METHOD_PARAMS_RENAMED_CASTED:
                return new MatchedDefinition(definition,
                    newCode,
                    usedMethodParameterIndexesArray,
                    renamedLocalParameters,
                    renamedLocalParameters.isEmpty() ? MatchType.METHOD_PARAMS_RENAMED_CASTED
                                                     : MatchType.METHOD_LOCAL_PARAMS_RENAMED_CASTED);
            default:
                return null;
        }
    }

    private static ParameterTokens buildParameterTokens(DecisionTable decisionTable) {
        int numberOfParameters = decisionTable.getSignature().getNumberOfParameters();
        Map<String, Integer> tokenToParameterIndex = new HashMap<>();
        Map<String, IOpenMethod[]> tokenToMethodsChain = new HashMap<>();
        List<Token> tokens = new ArrayList<>();
        for (int i = 0; i < numberOfParameters; i++) {
            IOpenClass parameterType = decisionTable.getSignature().getParameterType(i);
            if (!parameterType.isSimple() && !parameterType.isArray()) {
                Map<Token, IOpenMethod[][]> openClassFuzzyTokens = OpenLFuzzySearch
                    .tokensMapToOpenClassGetterMethodsRecursively(parameterType,
                        decisionTable.getSignature().getParameterName(i));
                for (Map.Entry<Token, IOpenMethod[][]> entry : openClassFuzzyTokens.entrySet()) {
                    tokens.add(entry.getKey());
                    tokenToParameterIndex.put(entry.getKey().getValue(), i);
                    tokenToMethodsChain.put(entry.getKey().getValue(), entry.getValue()[0]);
                }
            }

            String tokenString = OpenLFuzzySearch.toTokenString(decisionTable.getSignature().getParameterName(i));
            tokenToParameterIndex.put(tokenString, i);
            tokens.add(new Token(tokenString, 0));
        }

        return new ParameterTokens(tokens.toArray(new Token[] {}), tokenToParameterIndex, tokenToMethodsChain);
    }

    private static void matchWithFuzzySearch(DecisionTable decisionTable,
            ILogicalTable originalTable,
            ParameterTokens parameterTokens,
            Token[] returnTypeTokens,
            int column,
            int numberOfHcondition,
            NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
            List<DTHeader> dtHeaders) {
        int numberOfColumnsUnderTitle = numberOfColumnsUnderTitleCounter.get(column);
        if (numberOfColumnsUnderTitle > 2) {
            return;
        }
        String title = originalTable.getCell(column, 0).getStringValue();

        String tokenizedDescriptionString = OpenLFuzzySearch.toTokenString(title);
        Token[] bestMatchedTokens = OpenLFuzzySearch.openlFuzzyExtract(tokenizedDescriptionString,
            parameterTokens.getTokens());
        if (numberOfHcondition == 0) {
            if (returnTypeTokens != null) {
                Token[] bestMatchedTokensForReturnType = OpenLFuzzySearch.openlFuzzyExtract(title, returnTypeTokens);
                if (bestMatchedTokensForReturnType.length == 1) {
                    for (Token token : bestMatchedTokensForReturnType) {
                        dtHeaders.add(new FuzzyDTHeader(-1,
                            null,
                            title,
                            parameterTokens.getMethodsChain(token.getValue()),
                            column,
                            true));
                    }
                }
            }
        }
        for (Token token : bestMatchedTokens) {
            int paramIndex = parameterTokens.getParameterIndex(token.getValue());
            IOpenMethod[] methodsChain = parameterTokens.getMethodsChain(token.getValue());
            StringBuilder conditionStatement = new StringBuilder(
                decisionTable.getSignature().getParameterName(paramIndex));
            if (methodsChain != null) {
                Pair<String, IOpenClass> c = buildStatementByMethodsChain(
                    decisionTable.getSignature().getParameterType(paramIndex),
                    methodsChain);
                String chainStatement = c.getLeft();
                conditionStatement.append(".");
                conditionStatement.append(chainStatement);
            }

            dtHeaders.add(new FuzzyDTHeader(paramIndex,
                conditionStatement.toString(),
                title,
                parameterTokens.getMethodsChain(token.getValue()),
                column,
                false));
        }
    }

    private static boolean isCompatibleHeaders(DTHeader a, DTHeader b) {
        int c1 = a.getColumn();
        int c2 = a.getColumn() + a.getNumberOfUsedColumns() - 1;
        int d1 = b.getColumn();
        int d2 = b.getColumn() + b.getNumberOfUsedColumns() - 1;

        if (c1 <= d1 && d1 <= c2 || c1 <= d2 && d2 <= c2 || d1 <= c2 && c2 <= d2 || d1 <= c1 && c1 <= d2) {
            return false;
        }

        if ((a.isCondition() && b.isAction() || a.isAction() && b.isReturn() || a.isCondition() && b
            .isReturn()) && c1 >= d1) {
            return false;
        }
        if ((b.isCondition() && a.isAction() || b.isAction() && a.isReturn() || b.isCondition() && a
            .isReturn()) && d1 >= c1) {
            return false;
        }

        if ((a instanceof FuzzyDTHeader) && b instanceof FuzzyDTHeader) {
            FuzzyDTHeader a1 = (FuzzyDTHeader) a;
            FuzzyDTHeader b1 = (FuzzyDTHeader) b;
            if (a1.isCondition() && b1
                .isCondition() && a1.getMethodParameterIndex() == b1.getMethodParameterIndex() && Arrays
                    .deepEquals(a1.getMethodsChain(), b1.getMethodsChain())) {
                return false;
            }
        }
        return true;
    }

    private static void bruteForceHeaders(int column,
            int maxNumberOfParameters,
            List<DTHeader> dtHeaders,
            boolean[][] matrix,
            Map<Integer, List<Integer>> columnToIndex,
            List<Integer> usedIndexes,
            Set<Integer> usedParameterIndexes,
            List<DTHeader[]> fits) {
        List<Integer> indexes = columnToIndex.get(column);
        if (indexes == null || usedParameterIndexes.size() >= maxNumberOfParameters) {
            List<DTHeader> fit = new ArrayList<>();
            for (Integer index : usedIndexes) {
                fit.add(dtHeaders.get(index));
            }
            fits.add(fit.toArray(new DTHeader[] {}));
            if (indexes == null) {
                return;
            }
        }
        for (Integer index : indexes) {
            boolean f = true;
            for (Integer usedIndex : usedIndexes) {
                if (!matrix[index][usedIndex]) {
                    f = false;
                    break;
                }
            }
            if (f) {
                usedIndexes.add(index);
                DTHeader dtHeader = dtHeaders.get(index);
                Set<Integer> usedParameterIndexesTo = new HashSet<Integer>(usedParameterIndexes);
                for (int i : dtHeader.getMethodParameterIndexes()) {
                    usedParameterIndexesTo.add(i);
                }
                if (usedParameterIndexesTo.size() <= maxNumberOfParameters) {
                    bruteForceHeaders(column + dtHeader.getNumberOfUsedColumns(),
                        maxNumberOfParameters,
                        dtHeaders,
                        matrix,
                        columnToIndex,
                        usedIndexes,
                        usedParameterIndexesTo,
                        fits);
                }
                usedIndexes.remove(usedIndexes.size() - 1);
            }
        }
    }

    private static List<DTHeader[]> filterHeadersByMax(List<DTHeader[]> fits, Function<DTHeader[], Long> function) {
        long max = Long.MIN_VALUE;
        List<DTHeader[]> newFits = new ArrayList<>();
        for (DTHeader[] fit : fits) {
            long current = function.apply(fit);
            if (current > max) {
                max = current;
                newFits.clear();
                newFits.add(fit);
            } else if (current == max) {
                newFits.add(fit);
            }
        }
        return newFits;
    }

    private static List<DTHeader[]> filterHeadersByMin(List<DTHeader[]> fits, Function<DTHeader[], Long> function) {
        long min = Long.MAX_VALUE;
        List<DTHeader[]> newFits = new ArrayList<>();
        for (DTHeader[] fit : fits) {
            long current = function.apply(fit);
            if (current < min) {
                min = current;
                newFits.clear();
                newFits.add(fit);
            } else if (current == min) {
                newFits.add(fit);
            }
        }
        return newFits;
    }

    private static List<DTHeader[]> filterHeadersByMatchType(List<DTHeader[]> fits) {
        MatchType[] matchTypes = MatchType.values();
        Arrays.sort(matchTypes, Comparator.comparingInt(MatchType::getPriority));
        for (MatchType type : matchTypes) {
            fits = filterHeadersByMax(fits,
                e -> Arrays.stream(e)
                    .filter(x -> x instanceof DeclaredDTHeader)
                    .map(x -> (DeclaredDTHeader) x)
                    .filter(x -> type.equals(x.getMatchedDefinition().getMatchType()))
                    .mapToLong(x -> x.getNumberOfUsedColumns())
                    .sum());
        }
        return fits;
    }

    private static boolean isLastDtColumnValid(DTHeader dtHeader, int maxColumnCount, int columnsInReturn) {
        if (dtHeader.isReturn()) {
            return dtHeader.getColumn() + dtHeader.getNumberOfUsedColumns() == maxColumnCount;
        }
        if (dtHeader.isCondition() || dtHeader.isAction()) {
            return dtHeader.getColumn() + dtHeader.getNumberOfUsedColumns() < maxColumnCount - columnsInReturn;
        }
        return true;
    }

    private static List<DTHeader[]> filterBadOnes(ILogicalTable originalTable,
            List<DTHeader[]> fits,
            boolean twoColumnsInReturn) {
        int maxColumnCount = originalTable.getWidth();
        fits = fits.stream()
            .filter(
                e -> e.length == 0 || isLastDtColumnValid(e[e.length - 1], maxColumnCount, twoColumnsInReturn ? 1 : 0))
            .collect(Collectors.toList());
        return fits;
    }

    private static boolean isAmbiguousFits(List<DTHeader[]> fits, Predicate<DTHeader> predicate) {
        if (fits.size() <= 1) {
            return false;
        }
        DTHeader[] dtHeaders0 = Arrays.stream(fits.get(0)).filter(e -> predicate.test(e)).toArray(DTHeader[]::new);
        for (int i = 1; i < fits.size(); i++) {
            DTHeader[] dtHeaders1 = Arrays.stream(fits.get(i)).filter(e -> predicate.test(e)).toArray(DTHeader[]::new);
            if (!Arrays.equals(dtHeaders0, dtHeaders1)) {
                return true;
            }
        }
        return false;
    }

    private static DTHeader[] fitDtHeaders(ILogicalTable originalTable,
            TableSyntaxNode tableSyntaxNode,
            List<DTHeader> dtHeaders,
            List<DTHeader> simpleDtHeaders,
            int numberOfParametersToUse,
            boolean twoColumnsInReturn,
            IBindingContext bindingContext) throws SyntaxNodeException {
        boolean[][] matrix = new boolean[dtHeaders.size()][dtHeaders.size()];
        for (int i = 0; i < dtHeaders.size(); i++) {
            for (int j = 0; j < dtHeaders.size(); j++) {
                matrix[i][j] = true;
            }
        }
        Map<Integer, List<Integer>> columnToIndex = new HashMap<>();
        for (int i = 0; i < dtHeaders.size(); i++) {
            List<Integer> indexes = columnToIndex.get(dtHeaders.get(i).getColumn());
            if (indexes == null) {
                indexes = new ArrayList<>();
                columnToIndex.put(dtHeaders.get(i).getColumn(), indexes);
            }
            indexes.add(i);
            for (int j = i; j < dtHeaders.size(); j++) {
                if (i == j || !isCompatibleHeaders(dtHeaders.get(i), dtHeaders.get(j))) {
                    matrix[i][j] = false;
                    matrix[j][i] = false;
                }
            }
        }
        List<DTHeader[]> fits = new ArrayList<>();
        bruteForceHeaders(0,
            numberOfParametersToUse,
            dtHeaders,
            matrix,
            columnToIndex,
            new ArrayList<>(),
            new HashSet<>(),
            fits);
        fits = fits.stream()
            .filter(e -> Arrays.stream(e).filter(x -> x.isReturn()).count() <= 1 || Arrays.stream(e)
                .filter(x -> x.isReturn())
                .allMatch(x -> x instanceof FuzzyDTHeader))
            .collect(Collectors.toList()); // Only one column for return if not compound return

        fits = filterBadOnes(originalTable, fits, twoColumnsInReturn);

        fits.add(simpleDtHeaders.toArray(new DTHeader[] {}));
        
        fits = filterHeadersByMin(fits, e -> Arrays.stream(e).filter(x -> x instanceof SimpleDTHeader).count());
        fits = filterHeadersByMin(fits, e -> Arrays.stream(e).filter(x -> x instanceof SimpleReturnDTHeader).count());

        // Declared covered columns filter
        fits = filterHeadersByMax(fits,
            e -> Arrays.stream(e)
                .filter(x -> x instanceof DeclaredDTHeader)
                .mapToLong(x -> x.getNumberOfUsedColumns())
                .sum());
        fits = filterHeadersByMatchType(fits);
        fits = filterHeadersByMax(fits, e -> Arrays.stream(e).anyMatch(x -> x.isCondition()) ? 1l : 0l); // Prefer full
                                                                                                         // matches with
                                                                                                         // first
                                                                                                         // condition
                                                                                                         // headers
        fits = filterHeadersByMax(fits,
            e -> Arrays.stream(e).flatMapToInt(c -> Arrays.stream(c.getMethodParameterIndexes())).distinct().count());
        fits = filterHeadersByMax(fits, e -> Arrays.stream(e).anyMatch(x -> x.isReturn()) ? 1l : 0l); // Prefer full
                                                                                                      // matches with
                                                                                                      // last
                                                                                                      // return headers
        if (!fits.isEmpty()) {
            if (fits.size() > 1) {
                if (isAmbiguousFits(fits, e -> e.isCondition())) {
                    bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                        "Ambiguous matching of column titles to DT conditions. Use more appropriate titles for condition columns.",
                        tableSyntaxNode));
                }
                if (isAmbiguousFits(fits, e -> e.isAction())) {
                    bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                        "Ambiguous matching of column titles to DT action columns. Use more appropriate titles for action columns.",
                        tableSyntaxNode));
                }
                if (isAmbiguousFits(fits, e -> e.isReturn())) {
                    bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                        "Ambiguous matching of column titles to DT return columns. Use more appropriate titles for return columns.",
                        tableSyntaxNode));
                }
            }

            return fits.get(0);
        }

        return new DTHeader[] {};
    }

    private static List<DTHeader> findDTHeadersForSmartDecisionTable(TableSyntaxNode tableSyntaxNode,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            int numberOfHcondition,
            boolean isCollectTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        int numberOfParameters = decisionTable.getSignature().getNumberOfParameters();
        int column = 0;
        int firstColumnHeight = originalTable.getCell(0, 0).getHeight();

        boolean twoColumnsInReturn = isTwoColumnsInReturn(decisionTable, isCollectTable);

        ParameterTokens parameterTokens = buildParameterTokens(decisionTable);

        IOpenClass returnCompoudType = null;
        Token[] returnTypeTokens = null;
        if (numberOfHcondition == 0) {
            try {
                returnCompoudType = getCompoundReturnType(tableSyntaxNode,
                    decisionTable,
                    isCollectTable,
                    0,
                    bindingContext);
                if (isCompoundReturnType(returnCompoudType)) {
                    Map<Token, IOpenMethod[][]> returnTypeFuzzyTokens = OpenLFuzzySearch
                        .tokensMapToOpenClassSetterMethodsRecursively(returnCompoudType);
                    returnTypeTokens = returnTypeFuzzyTokens.keySet().toArray(new Token[] {});
                }
            } catch (OpenLCompilationException e) {
            }
        }

        XlsDefinitions xlsDefinitions = ((XlsModuleOpenClass) decisionTable.getDeclaringClass()).getXlsDefinitions();
        List<DTHeader> dtHeaders = new ArrayList<>();

        NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter = new NumberOfColumnsUnderTitleCounter(
            originalTable,
            false);

        List<DTHeader> simpleDtHeaders = new ArrayList<>();

        while (true) {
            if (column >= originalTable.getWidth()) {
                break;
            }

            if (originalTable.getCell(column, 0).getHeight() != firstColumnHeight) {
                break;
            }

            matchDtColumnsDefinitions(decisionTable,
                originalTable,
                column,
                xlsDefinitions,
                numberOfColumnsUnderTitleCounter,
                dtHeaders,
                bindingContext);

            matchWithFuzzySearch(decisionTable,
                originalTable,
                parameterTokens,
                returnTypeTokens,
                column,
                numberOfHcondition,
                numberOfColumnsUnderTitleCounter,
                dtHeaders);

            String title = originalTable.getCell(column, 0).getStringValue();
            if (column == originalTable.getWidth() - 1 && numberOfHcondition == 0) {
                dtHeaders.add(new SimpleReturnDTHeader(null, title, column));
                simpleDtHeaders.add(new SimpleReturnDTHeader(null, title, column));
            } else {
                if (column < numberOfParameters) {
                    simpleDtHeaders.add(new SimpleDTHeader(column,
                        decisionTable.getSignature().getParameterName(column),
                        title,
                        column,
                        false));
                }
            }
            column += 1;
        }

        DTHeader[] fit = fitDtHeaders(originalTable,
            tableSyntaxNode,
            dtHeaders,
            simpleDtHeaders,
            decisionTable.getSignature().getNumberOfParameters() - numberOfHcondition,
            twoColumnsInReturn,
            bindingContext);

        boolean[] parameterIsUsed = new boolean[numberOfParameters];
        Arrays.fill(parameterIsUsed, false);
        for (DTHeader dtHeader : fit) {
            for (int paramIndex : dtHeader.getMethodParameterIndexes()) {
                parameterIsUsed[paramIndex] = true;
            }
        }

        int k = 0;
        int i = numberOfParameters - 1;
        while (k < numberOfHcondition && i >= 0) {
            if (!parameterIsUsed[i]) {
                k++;
            }
            i--;
        }

        if (k < numberOfHcondition) {
            throw new OpenLCompilationException("No input parameter found for horizontal condition!");
        }

        DTHeader[] headers = new DTHeader[fit.length + numberOfHcondition];
        int j = 0;
        for (DTHeader dtHeader : fit) {
            headers[j] = dtHeader;
            j++;
        }

        j = 0;
        for (int w = i + 1; w < numberOfParameters; w++) {
            if (!parameterIsUsed[w] && j < numberOfHcondition) {
                headers[fit.length + j] = new SimpleDTHeader(w,
                    decisionTable.getSignature().getParameterName(w),
                    null,
                    column,
                    true);
                j++;
            }
        }

        return Arrays.asList(headers);
    }

    private static boolean isTwoColumnsInReturn(DecisionTable decisionTable, boolean isCollectTable) {
        boolean twoColumnsInReturn = false;
        if (isCollectTable && Map.class.isAssignableFrom(decisionTable.getType().getInstanceClass())) {
            twoColumnsInReturn = true;
        }
        return twoColumnsInReturn;
    }

    private static void matchDtColumnsDefinitions(DecisionTable decisionTable,
            ILogicalTable originalTable,
            int column,
            XlsDefinitions definitions,
            NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
            List<DTHeader> dtHeaders,
            IBindingContext bindingContext) {
        for (DTColumnsDefinition definition : definitions.getDtColumnsDefinitions()) {
            Set<String> titles = new HashSet<>(definition.getTitles());
            String title = originalTable.getCell(column, 0).getStringValue();
            title = OpenLFuzzySearch.toTokenString(title);
            int numberOfColumnsUnderTitle = numberOfColumnsUnderTitleCounter.get(column);
            int x = column;
            IParameterDeclaration[][] columnParameters = new IParameterDeclaration[definition.getNumberOfTitles()][];
            while (titles.contains(
                title) && numberOfColumnsUnderTitle == definition.getLocalParameters(title).size() && x < originalTable
                    .getWidth()) {
                titles.remove(title);
                for (String s : definition.getTitles()) {
                    if (s.equals(title)) {
                        columnParameters[x - column] = definition.getLocalParameters(title)
                            .toArray(new IParameterDeclaration[] {});
                        break;
                    }
                }
                x = x + 1;
                title = originalTable.getCell(x, 0).getStringValue();
                title = OpenLFuzzySearch.toTokenString(title);
                numberOfColumnsUnderTitle = numberOfColumnsUnderTitleCounter.get(x);
            }
            if (titles.isEmpty()) {
                MatchedDefinition matchedDefinition = matchByDTColumnDefinition(decisionTable,
                    definition,
                    bindingContext);
                if (matchedDefinition != null) {
                    DeclaredDTHeader dtHeader = new DeclaredDTHeader(matchedDefinition.getUsedMethodParameterIndexes(),
                        definition.getCompositeMethod(),
                        columnParameters,
                        column,
                        matchedDefinition);
                    dtHeaders.add(dtHeader);
                }
            }
        }
    }

    /**
     * Check type of condition values. If condition values are complex(Range, Array) then types of complex values will
     * be returned
     *
     * @param originalTable The original body of simple Decision Table.
     * @param column The number of a condition
     * @param type The type of an input parameter
     * @param isThatVCondition If condition is vertical value = true
     * @param vColumnCounter Counter of vertical conditions. Needed for calculating position of horizontal condition
     * @return type of condition values
     */
    private static Pair<String, IOpenClass> checkTypeOfValues(IBindingContext bindingContext,
            ILogicalTable originalTable,
            int column,
            IOpenClass type,
            boolean isThatVCondition,
            boolean lastCondition,
            int vColumnCounter) {
        ILogicalTable decisionValues;
        int width;

        if (isThatVCondition) {
            decisionValues = originalTable.getColumn(column);
            width = decisionValues.getHeight();
        } else {
            int numOfHRow = column - vColumnCounter;

            decisionValues = LogicalTableHelper.logicalTable(originalTable.getSource().getRow(numOfHRow));
            width = decisionValues.getWidth();
        }

        if (isThatVCondition || lastCondition) {
            int mergedColumnsCounts = isThatVCondition ? originalTable.getColumnWidth(
                column) : originalTable.getSource().getCell(vColumnCounter, column - vColumnCounter).getWidth();
            boolean isMerged = mergedColumnsCounts > 1;

            // if the name row is merged then we have Array
            if (isMerged) {
                if (!type.isArray()) {
                    return Pair.of(type.getName() + "[]", JavaOpenClass.getArrayType(type, 1));
                } else {
                    return Pair.of(type.getName(), type);
                }
            }
        }

        String typeName = type instanceof DomainOpenClass ? type.getInstanceClass().getCanonicalName() : type.getName();

        for (int valueNum = 1; valueNum < width; valueNum++) {
            ILogicalTable cellValue;

            if (isThatVCondition) {
                cellValue = decisionValues.getRow(valueNum);
            } else {
                cellValue = decisionValues.getColumn(valueNum);
            }

            String value = cellValue.getSource().getCell(0, 0).getStringValue();

            if (value == null) {
                continue;
            }

            ConstantOpenField constantOpenField = RuleRowHelper.findConstantField(bindingContext, value);
            if (constantOpenField != null && (IntRange.class
                .equals(constantOpenField.getType().getInstanceClass()) || DoubleRange.class
                    .equals(constantOpenField.getType().getInstanceClass()) || CharRange.class
                        .equals(constantOpenField.getType().getInstanceClass()) || StringRange.class
                            .equals(constantOpenField.getType().getInstanceClass()))) {
                return Pair.of(constantOpenField.getType().getInstanceClass().getSimpleName(),
                    constantOpenField.getType());
            }

            /* try to create range by values **/
            if (INT_TYPES.contains(typeName)) {
                try {
                    boolean f = false;
                    if (MAYBE_INT_ARRAY_PATTERN.matcher(value).matches()) {
                        f = true;
                    }
                    if (IntRangeParser.getInstance().parse(value) != null && !f) {
                        return Pair.of(IntRange.class.getSimpleName(), JavaOpenClass.getOpenClass(IntRange.class));
                    }
                } catch (Exception e) {
                    continue;
                }
            } else if (DOUBLE_TYPES.contains(typeName)) {
                try {
                    boolean f = true;
                    try {
                        Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        f = false;
                    }
                    if (DoubleRangeParser.getInstance().parse(value) != null && !f) {
                        return Pair.of(DoubleRange.class.getSimpleName(),
                            JavaOpenClass.getOpenClass(DoubleRange.class));
                    }
                } catch (Exception e) {
                    continue;
                }
            } else if (CHAR_TYPES.contains(typeName)) {
                try {
                    boolean f = true;
                    try {
                        CharRangeParser.getInstance().parse(value);
                    } catch (Exception e) {
                        f = false;
                    }
                    if (f && value.length() != 1) {
                        return Pair.of(CharRange.class.getSimpleName(), JavaOpenClass.getOpenClass(CharRange.class));
                    }
                } catch (Exception e) {
                    continue;
                }
            } else if (STRINGS_TYPES.contains(typeName)) {
                try {
                    if (StringRangeParser.getInstance().isStringRange(value)) {
                        return Pair.of(StringRange.class.getSimpleName(),
                            JavaOpenClass.getOpenClass(StringRange.class));
                    }
                } catch (Exception ignored) {
                    // OK
                }
            }
        }
        if (!type.isArray()) {
            return Pair.of(type.getName() + "[]", JavaOpenClass.getArrayType(type, 1));
        } else {
            return Pair.of(type.getName(), type);
        }
    }

    private static int getNumberOfConditions(DecisionTable decisionTable) {
        // number of conditions is counted by the number of income parameters
        //
        return decisionTable.getSignature().getNumberOfParameters();
    }

    public static XlsSheetGridModel createVirtualGrid(String poiSheetName, int numberOfColumns) {
        // Pre-2007 excel sheets had a limitation of 256 columns.
        Workbook workbook = (numberOfColumns > 256) ? new XSSFWorkbook() : new HSSFWorkbook();
        final Sheet sheet = workbook.createSheet(poiSheetName);
        return createVirtualGrid(sheet);
    }

    static boolean isSimpleDecisionTableOrSmartDecisionTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();

        return IXlsTableNames.SIMPLE_DECISION_TABLE.equals(dtType) || isSmartDecisionTable(tableSyntaxNode);
    }

    static boolean isCollectDecisionTable(TableSyntaxNode tableSyntaxNode) {
        return tableSyntaxNode.getHeader().isCollect();
    }

    static boolean isSmartDecisionTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();

        return IXlsTableNames.SMART_DECISION_TABLE.equals(dtType);
    }

    static boolean isSmartSimpleLookupTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();

        return IXlsTableNames.SMART_DECISION_LOOKUP.equals(dtType);
    }

    static boolean isSimpleLookupTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();

        return IXlsTableNames.SIMPLE_DECISION_LOOKUP.equals(dtType) || isSmartSimpleLookupTable(tableSyntaxNode);
    }

    static int countHConditions(ILogicalTable table) {
        int width = table.getWidth();
        int cnt = 0;

        for (int i = 0; i < width; i++) {

            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();

            if (value != null) {
                value = value.toUpperCase();

                if (isValidHConditionHeader(value)) {
                    ++cnt;
                }
            }
        }

        return cnt;
    }

    static int countVConditions(ILogicalTable table) {
        int width = table.getWidth();
        int cnt = 0;

        for (int i = 0; i < width; i++) {

            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();

            if (value != null) {
                value = value.toUpperCase();

                if (isValidConditionHeader(value) || isValidMergedConditionHeader(value)) {
                    ++cnt;
                }
            }
        }

        return cnt;
    }

    /**
     * Creates virtual {@link XlsSheetGridModel} with poi source sheet.
     */
    public static XlsSheetGridModel createVirtualGrid() {
        Sheet sheet = new HSSFWorkbook().createSheet();
        return createVirtualGrid(sheet);
    }

    /**
     * Creates virtual {@link XlsSheetGridModel} from poi source sheet.
     *
     * @param sheet poi sheet source
     * @return virtual grid that wraps sheet
     */
    private static XlsSheetGridModel createVirtualGrid(Sheet sheet) {
        final StringSourceCodeModule sourceCodeModule = new StringSourceCodeModule("", null);
        final SimpleWorkbookLoader workbookLoader = new SimpleWorkbookLoader(sheet.getWorkbook());
        XlsWorkbookSourceCodeModule mockWorkbookSource = new XlsWorkbookSourceCodeModule(sourceCodeModule,
            workbookLoader);
        XlsSheetSourceCodeModule mockSheetSource = new XlsSheetSourceCodeModule(new SimpleSheetLoader(sheet),
            mockWorkbookSource);

        return new XlsSheetGridModel(mockSheetSource);
    }

    private final static class ParameterTokens {
        Token[] tokens;
        Map<String, Integer> tokensToParameterIndex;
        Map<String, IOpenMethod[]> tokenToMethodsChain;

        public ParameterTokens(Token[] tokens,
                Map<String, Integer> tokensToParameterIndex,
                Map<String, IOpenMethod[]> tokenToMethodsChain) {
            this.tokens = tokens;
            this.tokensToParameterIndex = tokensToParameterIndex;
            this.tokenToMethodsChain = tokenToMethodsChain;
        }

        public IOpenMethod[] getMethodsChain(String value) {
            return tokenToMethodsChain.get(value);
        }

        public int getParameterIndex(String value) {
            return tokensToParameterIndex.get(value);
        }

        public Token[] getTokens() {
            return tokens;
        }
    }

    private static class NumberOfColumnsUnderTitleCounter {
        ILogicalTable logicalTable;
        boolean source;
        Map<Integer, Integer> numberOfColumnsMap = new HashMap<>();

        private int get(int column) {
            Integer numberOfColumns = numberOfColumnsMap.get(column);
            if (numberOfColumns == null) {
                int w;
                int h;
                if (source) {
                    w = logicalTable.getSource().getCell(column, 0).getWidth();
                    h = logicalTable.getSource().getCell(column, 0).getHeight();
                } else {
                    w = logicalTable.getCell(column, 0).getWidth();
                    h = logicalTable.getCell(column, 0).getHeight();
                }
                int i = 0;
                int count = 0;
                while (i < w) {
                    if (source) {
                        i = i + logicalTable.getSource().getCell(column, h).getWidth();
                    } else {
                        i = i + logicalTable.getCell(column, h).getWidth();
                    }
                    count++;
                }
                numberOfColumns = count;
                numberOfColumnsMap.put(column, count);
            }
            return numberOfColumns;
        }

        private NumberOfColumnsUnderTitleCounter(ILogicalTable logicalTable, boolean source) {
            this.logicalTable = logicalTable;
            this.source = source;
        }
    }
}
