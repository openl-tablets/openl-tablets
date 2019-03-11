package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
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
import org.openl.types.impl.CompositeMethod;
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
                String descriptionOfCondition = condition.getDescription();
                try {
                    IOpenMethod bestMatchConditionMethod = findBestMatchOpenMethod(descriptionOfCondition,
                        compoundType,
                        true);
                    sb.append("ret.");
                    sb.append(bestMatchConditionMethod.getName());
                    sb.append("(");
                    sb.append(String
                        .valueOf(decisionTable.getSignature().getParameterName(condition.getMethodParameterIndex())));
                    sb.append(");");
                } catch (OpenLCompilationException e) {
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
                String description = originalTable.getSource().getCell(column, h).getStringValue();

                previoush = h;
                h = h + originalTable.getSource().getCell(column, h).getHeight();

                IOpenMethod[] m = null;

                if (h < numberOfMergedRows) {
                    IOpenMethod bestMatchMethod = findBestMatchOpenMethod(description, type, false);
                    if (bestMatchMethod != null) {
                        m = new IOpenMethod[] { bestMatchMethod };
                    }
                }
                if (m == null) {
                    m = findBestMatchOpenMethodRecursivelyForReturnType(description, type);
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

    private static IOpenMethod findBestMatchOpenMethod(String description,
            IOpenClass openClass,
            boolean validateEmptyResult) throws OpenLCompilationException {
        Map<Token, IOpenMethod[]> openClassFuzzyTokens = OpenLFuzzySearch.tokensMapToOpenClassSetterMethods(openClass);

        String tokenizedDescriptionString = OpenLFuzzySearch.toTokenString(description);
        Token[] fuzzyBestMatches = OpenLFuzzySearch.openlFuzzyExtract(tokenizedDescriptionString,
            openClassFuzzyTokens.keySet().toArray(new Token[] {}));

        if (fuzzyBestMatches.length == 0) {
            if (validateEmptyResult) {
                throw new OpenLCompilationException(
                    String.format("No field match in the return type for the description '%s'.", description));
            } else {
                return null;
            }
        }
        if (fuzzyBestMatches.length > 1) {
            throw new OpenLCompilationException(
                String.format("More than one field match in the return type for the description '%s'.", description));
        }
        if (openClassFuzzyTokens
            .get(fuzzyBestMatches[0]) == null || openClassFuzzyTokens.get(fuzzyBestMatches[0]).length == 0) {
            if (validateEmptyResult) {
                throw new OpenLCompilationException(
                    String.format("No field match in the return type for the description '%s'.", description));
            } else {
                return null;
            }
        }
        if (openClassFuzzyTokens.get(fuzzyBestMatches[0]).length > 1) {
            throw new OpenLCompilationException(
                String.format("More than one field match in the return type for the description '%s'.", description));
        }

        return openClassFuzzyTokens.get(fuzzyBestMatches[0])[0];
    }

    private static IOpenMethod[] findBestMatchOpenMethodRecursivelyForReturnType(String description,
            IOpenClass openClass) throws OpenLCompilationException {
        Map<Token, IOpenMethod[][]> openClassFuzzyTokens = OpenLFuzzySearch
            .tokensMapToOpenClassSetterMethodsRecursively(openClass);

        String tokenizedDescriptionString = OpenLFuzzySearch.toTokenString(description);
        Token[] fuzzyBestMatches = OpenLFuzzySearch.openlFuzzyExtract(tokenizedDescriptionString,
            openClassFuzzyTokens.keySet().toArray(new Token[] {}));

        if (fuzzyBestMatches.length == 0) {
            throw new OpenLCompilationException(
                String.format("No field match in the return type for the description '%s'.", description));
        }
        if (fuzzyBestMatches.length > 1) {
            throw new OpenLCompilationException(
                String.format("More than one field match in the return type for the description '%s'.", description));
        }
        if (openClassFuzzyTokens
            .get(fuzzyBestMatches[0]) == null || openClassFuzzyTokens.get(fuzzyBestMatches[0]).length == 0) {
            throw new OpenLCompilationException(
                String.format("No field match in the return type for the description '%s'.", description));
        }
        if (openClassFuzzyTokens.get(fuzzyBestMatches[0]).length > 1) {
            throw new OpenLCompilationException(
                String.format("More than one field match in the return type for the description '%s'.", description));
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

    private static boolean writeReturnWithReturnDefinition(TableSyntaxNode tableSyntaxNode,
            IWritableGrid grid,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            int firstReturnColumn,
            IBindingContext bindingContext) {

        XlsDefinitions xlsDefinitions = ((XlsModuleOpenClass) decisionTable.getDeclaringClass()).getXlsDefinitions();
        Set<String> titles = new HashSet<>();
        int c = firstReturnColumn;
        while (c < originalTable.getSource().getWidth()) {
            ICell cell = originalTable.getSource().getCell(c, 0);
            String d = cell.getStringValue();
            c = c + cell.getWidth();
            titles.add(d);
        }

        DTColumnsDefinition bestReturnDefinition = null;
        MatchDefinition bestMatchedDefinition = null;

        for (DTColumnsDefinition rd : xlsDefinitions.getReturnDefinitions()) {
            if (rd.getTitles().size() == titles.size() && rd.getTitles().containsAll(titles)) {
                IOpenClass decisionTableReturnType = decisionTable.getHeader().getType();
                IOpenClass definitionReturnType = rd.getCompositeMethod().getType();
                IOpenCast openCast = bindingContext.getCast(definitionReturnType, decisionTableReturnType);
                if (openCast != null && openCast.isImplicit()) {
                    MatchDefinition matchedDefinition = matchByDTColumnDefinition(rd,
                        decisionTable.getHeader(),
                        bindingContext);
                    if (matchedDefinition != null) {
                        if (bestReturnDefinition == null || bestMatchedDefinition.getMatchType()
                            .getPriority() > matchedDefinition.getMatchType().getPriority()) {
                            bestReturnDefinition = rd;
                            bestMatchedDefinition = matchedDefinition;
                        } else if (bestReturnDefinition != null) {
                            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                                "Ambiguous matching of column titles to DT return columns. Use more appropriate titles for return columns.",
                                tableSyntaxNode));
                        }
                    }
                }
            }
        }

        if (bestReturnDefinition == null) {
            return false;
        }

        grid.setCellValue(firstReturnColumn, 1, bestMatchedDefinition.getStatement());

        c = firstReturnColumn;
        while (c < originalTable.getSource().getWidth()) {
            ICell cell = originalTable.getSource().getCell(c, 0);
            String d = cell.getStringValue();
            for (String title : bestReturnDefinition.getTitles()) {
                if (Objects.equals(d, title)) {
                    List<IParameterDeclaration> localParameters = bestReturnDefinition.getLocalParameters(title);
                    List<String> localParameterNames = new ArrayList<>();
                    List<IOpenClass> typeOfColumns = new ArrayList<>();
                    int column = c;
                    for (IParameterDeclaration param : localParameters) {
                        if (param != null) {
                            String paramName = bestMatchedDefinition.getLocalParameterName(param.getName());
                            localParameterNames.add(paramName);
                            String value = param.getType().getName() + (paramName != null ? " " + paramName : "");
                            grid.setCellValue(column, 2, value);
                            typeOfColumns.add(param.getType());
                        } else {
                            typeOfColumns.add(bestReturnDefinition.getCompositeMethod().getType());
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
                            bestMatchedDefinition.getStatement(),
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
            if (originalTable.getWidth() > headers.size()) {
                if (!writeReturnWithReturnDefinition(tableSyntaxNode,
                    grid,
                    originalTable,
                    decisionTable,
                    firstReturnColumn,
                    bindingContext)) {
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
                            if (calculateReturnColumnsCount(originalTable,
                                firstReturnColumn,
                                numberOfMergedRows) == 1) {
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
                        int mergedColumnsCounts = originalTable.getColumnWidth(headers.size());
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
            } else {
                // if the physical number of columns for conditions is equals or
                // more than whole width of the table,
                // means there is no return column.
                //
                throw new OpenLCompilationException("Wrong table structure: There is no column for return values");
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
                dtHeaders.add(new DTHeader(i));
            }
            return dtHeaders;
        }
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
            grid.setCellValue(column, 0, conditionName);

            String conditionStatement;
            if (condition.isDeclared()) {
                conditionStatement = condition.getStatement();
            } else {
                conditionStatement = decisionTable.getSignature().getParameterName(condition.getMethodParameterIndex());
                if (condition.getMethodsChain() != null) {
                    Pair<String, IOpenClass> c = buildStatementByMethodsChain(
                        decisionTable.getSignature().getParameterType(condition.getMethodParameterIndex()),
                        conditions.get(i).getMethodsChain());
                    String chainStatement = c.getLeft();
                    conditionStatement = conditionStatement + "." + chainStatement;
                }
            }
            grid.setCellValue(column, 1, conditionStatement);

            if (!condition.isDeclared()) {
                IOpenClass typeOfCondition = decisionTable.getSignature().getParameterTypes()[condition
                    .getMethodParameterIndex()];
                if (condition.getMethodsChain() != null) {
                    typeOfCondition = condition.getMethodsChain()[condition.getMethodsChain().length - 1].getType();
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
            } else { // VCondition and declared by user
                int firstColumn = column;
                for (int j = 0; j < condition.getColumnParameters().length; j++) {
                    List<String> parameterNames = new ArrayList<>();
                    List<IOpenClass> typeOfColumns = new ArrayList<>();
                    int firstTitleColumn = column;
                    for (int k = 0; k < condition.getColumnParameters()[j].length; k++) {
                        IParameterDeclaration param = condition.getColumnParameters()[j][k];
                        if (param != null) {
                            String paramName = condition.getMatchedDefinition().getLocalParameterName(param.getName());
                            parameterNames.add(paramName);
                            grid.setCellValue(column,
                                2,
                                param.getType().getName() + (paramName != null ? " " + paramName : ""));
                            typeOfColumns.add(param.getType());
                        } else {
                            typeOfColumns.add(condition.getCompositeMethod().getType());
                        }
                        int h = originalTable.getSource().getCell(column, 0).getHeight();
                        int w1 = originalTable.getSource().getCell(column, h).getWidth();
                        if (w1 > 1) {
                            grid.addMergedRegion(new GridRegion(2, column, 2, column + w1 - 1));
                        }

                        column = column + w1;
                    }

                    if (!bindingContext.isExecutionMode()) {
                        writeMetaInfoForVCondition(originalTable,
                            decisionTable,
                            firstTitleColumn,
                            conditionName,
                            parameterNames.toArray(new String[] {}),
                            conditionStatement,
                            typeOfColumns.toArray(new IOpenClass[] {}));
                    }
                }

                // merge columns
                if (column - firstColumn > 1) {
                    for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
                        grid.addMergedRegion(new GridRegion(row, firstColumn, row, column - 1));
                    }
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

    private static void parseRec(ISyntaxNode node, List<IdentifierNode> identifierNodes) {
        if (node instanceof IdentifierNode) {
            identifierNodes.add((IdentifierNode) node);
        }
        for (int i = 0; i < node.getNumberOfChildren(); i++) {
            parseRec(node.getChild(i), identifierNodes);
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

    private static MatchDefinition matchByDTColumnDefinition(DTColumnsDefinition definition,
            IOpenMethodHeader header,
            IBindingContext bindingContext) {
        List<IdentifierNode> identifierNodes = new ArrayList<>();
        parseRec(definition.getCompositeMethod().getMethodBodyBoundNode().getSyntaxNode(), identifierNodes);
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

        int strictMatchOfParameters = 0;
        Map<String, String> methodParameters = new HashMap<>();
        Map<String, String> methodParametersWithCast = new HashMap<>();
        List<Integer> usedMethodParameterStrictIndexes = new ArrayList<>();
        List<Integer> usedMethodParameterIndexes = new ArrayList<>();
        List<Integer> usedMethodParameterWithCastIndexes = new ArrayList<>();
        for (String param : methodParametersUsedInExpression) {
            int j = -1;
            for (int i = 0; i < definition.getHeader().getSignature().getNumberOfParameters(); i++) {
                if (param.equals(definition.getHeader().getSignature().getParameterName(i))) {
                    j = i;
                    break;
                }
            }
            IOpenClass type = definition.getHeader().getSignature().getParameterType(j);
            int k = -1;
            int t = 0;
            int k1 = -1;
            int t1 = 0;
            for (int i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
                if (param.equals(header.getSignature().getParameterName(i)) && type
                    .equals(header.getSignature().getParameterType(i))) {
                    usedMethodParameterStrictIndexes.add(i);
                    strictMatchOfParameters = strictMatchOfParameters + 1;
                }
                IOpenCast openCast = bindingContext.getCast(type, header.getSignature().getParameterType(i));
                if (openCast != null && openCast.isImplicit()) {
                    k1 = i;
                    t1++;
                    usedMethodParameterWithCastIndexes.add(i);
                }
                if (type.equals(header.getSignature().getParameterType(i))) {
                    k = i;
                    t++;
                    usedMethodParameterIndexes.add(i);
                }
            }
            if (t == 1) {
                methodParameters.put(param, header.getSignature().getParameterName(k));
            }
            if (t1 == 1) {
                methodParametersWithCast.put(param, header.getSignature().getParameterName(k1));
            }
        }

        Set<String> methodParameterNames = new HashSet<>();
        for (int i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
            methodParameterNames.add(header.getSignature().getParameterName(i));
        }

        Map<String, String> renamedLocalParameters = new HashMap<>();
        for (String paramName : methodParameterNames) {
            if (localParameters.containsKey(paramName)) {
                int k = 1;
                String newParamName = "$" + paramName;
                while (localParameters.containsKey(newParamName) || renamedLocalParameters
                    .containsValue(newParamName) || methodParameterNames.contains(newParamName)) {
                    newParamName = "$" + paramName + k;
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

        if (strictMatchOfParameters == methodParametersUsedInExpression.size()) {
            return new MatchDefinition(
                renamedLocalParameters
                    .isEmpty() ? code : replaceIdentifierNodeNamesInCode(code, identifierNodes, renamedLocalParameters),
                ArrayUtils.toPrimitive(usedMethodParameterStrictIndexes.toArray(new Integer[] {})),
                renamedLocalParameters,
                renamedLocalParameters.isEmpty() ? MatchType.STRICT : MatchType.STRICT_LOCAL_PARAMS_RENAMED);
        }

        if (methodParametersUsedInExpression.size() == methodParameters.size()) {
            String newCode = replaceIdentifierNodeNamesInCode(code,
                identifierNodes,
                methodParameters,
                renamedLocalParameters);
            int[] usedMethodParameterIndexesArray = ArrayUtils
                .toPrimitive(usedMethodParameterIndexes.toArray(new Integer[] {}));
            return new MatchDefinition(newCode,
                usedMethodParameterIndexesArray,
                renamedLocalParameters,
                renamedLocalParameters.isEmpty() ? MatchType.METHOD_PARAMS_RENAMED
                                                 : MatchType.METHOD_LOCAL_PARAMS_RENAMED);

        }

        if (methodParametersUsedInExpression.size() == methodParametersWithCast.size()) {
            String newCode = replaceIdentifierNodeNamesInCode(code,
                identifierNodes,
                methodParametersWithCast,
                renamedLocalParameters);

            int[] usedMethodParameterIndexesArray = ArrayUtils
                .toPrimitive(usedMethodParameterWithCastIndexes.toArray(new Integer[] {}));
            for (Entry<String, String> entry : methodParametersWithCast.entrySet()) {
                if (!entry.getKey().equals(entry.getValue())) {
                    return new MatchDefinition(newCode,
                        usedMethodParameterIndexesArray,
                        renamedLocalParameters,
                        renamedLocalParameters.isEmpty() ? MatchType.METHOD_PARAMS_RENAMED_CASTED
                                                         : MatchType.METHOD_LOCAL_PARAMS_RENAMED_CASTED);
                }
            }
            return new MatchDefinition(newCode,
                usedMethodParameterIndexesArray,
                renamedLocalParameters,
                renamedLocalParameters.isEmpty() ? MatchType.CASTED : MatchType.LOCAL_PARAMS_RENAMED_CASTED);
        }

        return null;
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

    private static boolean matchWithFuzzySearch(ParameterTokens parameterTokens,
            Token[] returnTypeTokens,
            String title,
            int column,
            int numberOfHcondition,
            int numberOfColumnsUnderTitle,
            List<DTHeader> dtHeaders) {
        if (numberOfColumnsUnderTitle != 1) {
            return false;
        }

        String tokenizedDescriptionString = OpenLFuzzySearch.toTokenString(title);
        Token[] bestMatchedTokens = OpenLFuzzySearch.openlFuzzyExtract(tokenizedDescriptionString,
            parameterTokens.getTokens());
        if (bestMatchedTokens.length > 0) {
            if (numberOfHcondition == 0 && bestMatchedTokens.length > 1) {
                if (returnTypeTokens != null) {
                    Token[] bestMatchedTokensForReturnType = OpenLFuzzySearch.openlFuzzyExtract(title,
                        returnTypeTokens);
                    if (bestMatchedTokensForReturnType.length == 1) {
                        return false;
                    }
                }
            }
            for (int i = 0; i < bestMatchedTokens.length; i++) {
                dtHeaders.add(new DTHeader(parameterTokens.getParameterIndex(bestMatchedTokens[i].getValue()),
                    title,
                    parameterTokens.getMethodsChain(bestMatchedTokens[i].getValue()),
                    column - 1));
            }
        }
        return true;
    }

    private static boolean isCompatibleHeaders(DTHeader a, DTHeader b) {
        int c1 = a.getColumn();
        int c2 = a.getColumn() + a.getNumberOfUsedColumns() - 1;
        int d1 = b.getColumn();
        int d2 = b.getColumn() + b.getNumberOfUsedColumns() - 1;

        if (c1 <= d1 && d1 <= c2 || c1 <= d2 && d2 <= c2) {
            return false;
        }
        if (!a.isDeclared() && !b.isDeclared() && a.getMethodParameterIndex() == b.getMethodParameterIndex() && Arrays
            .deepEquals(a.getMethodsChain(), b.getMethodsChain())) {
            return false;
        }
        return true;
    }

    private static void bruteForceHeaders(int column,
            int numberOfParametersToUse,
            List<DTHeader> vConditions,
            boolean[][] matrix,
            Map<Integer, List<Integer>> columnToIndex,
            List<Integer> usedIndexes,
            Set<Integer> usedParameterIndexes,
            List<DTHeader[]> fits) {
        List<Integer> indexes = columnToIndex.get(column);
        if (indexes == null || usedParameterIndexes.size() >= numberOfParametersToUse) {
            List<DTHeader> headers = new ArrayList<>();
            for (Integer index : usedIndexes) {
                headers.add(vConditions.get(index));
            }
            fits.add(headers.toArray(new DTHeader[] {}));
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
                DTHeader condition = vConditions.get(index);
                int d = 1;
                if (condition.isDeclared() && condition.getColumnParameters() != null) {
                    d = condition.getColumnParameters().length;
                }
                Set<Integer> usedParameterIndexesTo = new HashSet<Integer>(usedParameterIndexes);
                for (int i : condition.getMethodParameterIndexes()) {
                    usedParameterIndexesTo.add(i);
                }
                if (usedParameterIndexesTo.size() <= numberOfParametersToUse) {
                    bruteForceHeaders(column + d,
                        numberOfParametersToUse,
                        vConditions,
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

    private static List<DTHeader[]> filterConditionsByMax(List<DTHeader[]> fits, Function<DTHeader[], Long> function) {
        long max = 0;
        List<DTHeader[]> newFits = new ArrayList<>();
        for (DTHeader[] conditions : fits) {
            long current = function.apply(conditions);
            if (current > max) {
                max = current;
                newFits.clear();
                newFits.add(conditions);
            } else if (current == max) {
                newFits.add(conditions);
            }
        }
        return newFits;
    }

    private static List<DTHeader[]> filterConditionsByMatchType(List<DTHeader[]> fits) {
        MatchType[] matchTypes = MatchType.values();
        Arrays.sort(matchTypes, Comparator.comparingInt(MatchType::getPriority));
        for (MatchType type : matchTypes) {
            fits = filterConditionsByMax(fits,
                e -> Arrays.stream(e)
                    .filter(x -> x.isDeclared())
                    .filter(x -> type.equals(x.getMatchedDefinition().getMatchType()))
                    .mapToLong(x -> x.getNumberOfUsedColumns())
                    .sum());
        }
        return fits;
    }

    private static DTHeader[] fitDtHeaders(TableSyntaxNode tableSyntaxNode,
            List<DTHeader> dtHeaders,
            int numberOfParametersToUse,
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

        // Declared covered columns filter
        fits = filterConditionsByMax(fits,
            e -> Arrays.stream(e).filter(x -> x.isDeclared()).mapToLong(x -> x.getNumberOfUsedColumns()).sum());
        fits = filterConditionsByMatchType(fits);
        fits = filterConditionsByMax(fits,
            e -> Arrays.stream(e).flatMapToInt(c -> Arrays.stream(c.getMethodParameterIndexes())).distinct().count());

        if (!fits.isEmpty()) {
            if (fits.size() > 1) {
                bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                    "Ambiguous matching of column titles to DT conditions. Use more appropriate titles for condition columns.",
                    tableSyntaxNode));
            }

            return fits.get(0);
        }

        return new DTHeader[] {};
    }

    private static int getNumberOfColumnsUnderTitle(ILogicalTable originalTable, int column) {
        int w = originalTable.getCell(column, 0).getWidth();
        int i = 0;
        int numberOfColumnsUnderTitle = 0;
        while (i < w) {
            i = i + originalTable.getCell(column, 1).getWidth();
            numberOfColumnsUnderTitle++;
        }
        return numberOfColumnsUnderTitle;
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

        while (true) {
            if (originalTable.getCell(column, 0).getHeight() != firstColumnHeight) {
                break;
            }
            String title = originalTable.getCell(column, 0).getStringValue();

            int numberOfColumnsUnderTitle = getNumberOfColumnsUnderTitle(originalTable, column);

            column += 1;
            if (column >= originalTable.getWidth()) {
                break;
            }

            if (isCollectTable && Map.class.isAssignableFrom(decisionTable.getType().getInstanceClass())) { // Collect
                                                                                                            // with
                                                                                                            // Map
                                                                                                            // uses
                                                                                                            // 2
                                                                                                            // last
                                                                                                            // columns
                if (column + originalTable.getColumnWidth(column) >= originalTable.getWidth()) {
                    break;
                }
            }

            if (matchReturnWithDefinitions(decisionTable,
                originalTable,
                column,
                xlsDefinitions,
                title,
                numberOfColumnsUnderTitle,
                bindingContext)) {
                break;
            }

            boolean f1 = matchConditionWithDefinitions(decisionTable,
                originalTable,
                column,
                xlsDefinitions,
                title,
                numberOfColumnsUnderTitle,
                dtHeaders,
                bindingContext);
            boolean f2 = matchWithFuzzySearch(parameterTokens,
                returnTypeTokens,
                title,
                column,
                numberOfHcondition,
                numberOfColumnsUnderTitle,
                dtHeaders);
            if (!f1 && !f2) {
                break;
            }
        }

        DTHeader[] fit = fitDtHeaders(tableSyntaxNode,
            dtHeaders,
            decisionTable.getSignature().getNumberOfParameters() - numberOfHcondition,
            bindingContext);

        boolean[] parameterIsUsed = new boolean[numberOfParameters];
        Arrays.fill(parameterIsUsed, false);
        for (DTHeader dtHeader : fit) {
            if (!dtHeader.isDeclared()) {
                parameterIsUsed[dtHeader.getMethodParameterIndex()] = true;
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
                headers[fit.length + j] = new DTHeader(w);
                j++;
            }
        }

        return Arrays.asList(headers);
    }

    private static boolean matchReturnWithDefinitions(DecisionTable decisionTable,
            ILogicalTable originalTable,
            int column,
            XlsDefinitions definitions,
            String title,
            int numberOfColumnsUnderTitle,
            IBindingContext bindingContext) {
        for (DTColumnsDefinition returnDefiniton : definitions.getReturnDefinitions()) {
            Set<String> titles = new HashSet<>(returnDefiniton.getTitles());
            String d = title;
            int x = column;
            while (titles.contains(d) && numberOfColumnsUnderTitle == returnDefiniton.getLocalParameters(title)
                .size() && x <= originalTable.getWidth()) {
                titles.remove(d);
                for (String s : returnDefiniton.getTitles()) {
                    if (s.equals(d)) {
                        break;
                    }
                }
                d = originalTable.getCell(x, 0).getStringValue();
                numberOfColumnsUnderTitle = getNumberOfColumnsUnderTitle(originalTable, x);
                x = x + 1;
            }
            if (titles.isEmpty() && x > originalTable.getWidth()) {
                IOpenClass decisionTableReturnType = decisionTable.getHeader().getType();
                IOpenClass definitionReturnType = returnDefiniton.getCompositeMethod().getType();
                IOpenCast openCast = bindingContext.getCast(definitionReturnType, decisionTableReturnType);
                if (openCast != null && openCast.isImplicit()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean matchConditionWithDefinitions(DecisionTable decisionTable,
            ILogicalTable originalTable,
            int column,
            XlsDefinitions definitions,
            String title,
            int numberOfColumnsUnderTitle,
            List<DTHeader> dtHeaders,
            IBindingContext bindingContext) {
        boolean matched = false;
        for (DTColumnsDefinition conditionDefinition : definitions.getConditionDefinitions()) {
            Set<String> titles = new HashSet<>(conditionDefinition.getTitles());
            String d = title;
            int x = column;
            IParameterDeclaration[][] columnParameters = new IParameterDeclaration[conditionDefinition
                .getNumberOfTitles()][];
            while (titles.contains(d) && numberOfColumnsUnderTitle == conditionDefinition.getLocalParameters(title)
                .size() && x <= originalTable.getWidth()) {
                titles.remove(d);
                for (String s : conditionDefinition.getTitles()) {
                    if (s.equals(d)) {
                        columnParameters[x - column] = conditionDefinition.getLocalParameters(d)
                            .toArray(new IParameterDeclaration[] {});
                        break;
                    }
                }
                d = originalTable.getCell(x, 0).getStringValue();
                numberOfColumnsUnderTitle = getNumberOfColumnsUnderTitle(originalTable, x);
                x = x + 1;
            }
            if (titles.isEmpty()) {
                MatchDefinition matchedDefinition = matchByDTColumnDefinition(conditionDefinition,
                    decisionTable.getHeader(),
                    bindingContext);
                if (matchedDefinition != null) {
                    dtHeaders.add(new DTHeader(matchedDefinition.getUsedMethodParameterIndexes(),
                        conditionDefinition.getCompositeMethod(),
                        matchedDefinition.getStatement(),
                        columnParameters,
                        column - 1,
                        matchedDefinition));
                    matched = true;
                }
            }
        }
        return matched;
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

    private static enum MatchType {
        STRICT(0),
        STRICT_LOCAL_PARAMS_RENAMED(1),
        METHOD_PARAMS_RENAMED(2),
        METHOD_LOCAL_PARAMS_RENAMED(3),
        CASTED(4),
        LOCAL_PARAMS_RENAMED_CASTED(5),
        METHOD_PARAMS_RENAMED_CASTED(6),
        METHOD_LOCAL_PARAMS_RENAMED_CASTED(7);

        int priority;

        private MatchType(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    private final static class MatchDefinition {
        String statement;
        int[] usedMethodParameterIndexes;
        MatchType matchType;
        Map<String, String> renamedLocalParameters;

        public MatchDefinition(String statement,
                int[] usedMethodParameterIndexes,
                Map<String, String> renamedLocalParameters,
                MatchType matchType) {
            super();
            this.statement = statement;
            this.usedMethodParameterIndexes = usedMethodParameterIndexes;
            this.matchType = matchType;
            this.renamedLocalParameters = renamedLocalParameters;
        }

        public String getLocalParameterName(String name) {
            if (renamedLocalParameters == null) {
                return name;
            }
            String newName = renamedLocalParameters.get(name);
            return newName != null ? newName : name;
        }

        public String getStatement() {
            return statement;
        }

        public int[] getUsedMethodParameterIndexes() {
            return usedMethodParameterIndexes;
        }

        public MatchType getMatchType() {
            return matchType;
        }
    }

    private enum HeaderType {
        CONDITION,
        ACTION,
        RETURN
    }

    private final static class DTHeader {
        int[] methodParameterIndexes;
        String description;
        IOpenMethod[] methodsChain;
        int column;
        String statement;
        IParameterDeclaration[][] columnParameters;
        boolean declared = false;
        CompositeMethod compositeMethod;
        MatchDefinition matchedDefinition;
        int numberOfUsedColumns;
        HeaderType type = HeaderType.CONDITION;

        public DTHeader(int methodParameterIndex) {
            this.methodParameterIndexes = new int[] { methodParameterIndex };
        }

        public DTHeader(int methodParameterIndex, String description, IOpenMethod[] methodsChain, int column) {
            this.methodParameterIndexes = new int[] { methodParameterIndex };
            this.description = description;
            this.methodsChain = methodsChain;
            this.column = column;
            this.numberOfUsedColumns = 1;
        }

        public DTHeader(int[] methodParameterIndexes,
                CompositeMethod compositeMethod,
                String statement,
                IParameterDeclaration[][] columnParameters,
                int column,
                MatchDefinition matchedDefinition) {
            this.methodParameterIndexes = methodParameterIndexes;
            this.columnParameters = columnParameters;
            this.statement = statement;
            this.declared = true;
            this.compositeMethod = compositeMethod;
            this.matchedDefinition = matchedDefinition;
            this.column = column;
            this.numberOfUsedColumns = 0;
            for (IParameterDeclaration[] pds : columnParameters) {
                this.numberOfUsedColumns = this.numberOfUsedColumns + pds.length;
            }
        }

        public HeaderType getType() {
            return type;
        }

        public boolean isCondition() {
            return HeaderType.CONDITION.equals(type);
        }

        public CompositeMethod getCompositeMethod() {
            return compositeMethod;
        }

        public boolean isDeclared() {
            return declared;
        }

        public String getStatement() {
            return statement;
        }

        public IParameterDeclaration[][] getColumnParameters() {
            return columnParameters;
        }

        public int getNumberOfUsedColumns() {
            if (columnParameters != null) {
                return columnParameters.length;
            } else {
                return 1;
            }
        }

        public String getDescription() {
            return description;
        }

        public int getMethodParameterIndex() {
            if (methodParameterIndexes != null) {
                return methodParameterIndexes[0];
            }
            return 0;
        }

        public int[] getMethodParameterIndexes() {
            return methodParameterIndexes;
        }

        public IOpenMethod[] getMethodsChain() {
            return methodsChain;
        }

        public int getColumn() {
            return column;
        }

        public MatchDefinition getMatchedDefinition() {
            return matchedDefinition;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + column;
            result = prime * result + Arrays.deepHashCode(columnParameters);
            result = prime * result + ((compositeMethod == null) ? 0 : compositeMethod.hashCode());
            result = prime * result + (declared ? 1231 : 1237);
            result = prime * result + ((description == null) ? 0 : description.hashCode());
            result = prime * result + ((matchedDefinition == null) ? 0 : matchedDefinition.hashCode());
            result = prime * result + Arrays.hashCode(methodParameterIndexes);
            result = prime * result + Arrays.hashCode(methodsChain);
            result = prime * result + numberOfUsedColumns;
            result = prime * result + ((statement == null) ? 0 : statement.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DTHeader other = (DTHeader) obj;
            if (column != other.column)
                return false;
            if (!Arrays.deepEquals(columnParameters, other.columnParameters))
                return false;
            if (compositeMethod == null) {
                if (other.compositeMethod != null)
                    return false;
            } else if (!compositeMethod.equals(other.compositeMethod))
                return false;
            if (declared != other.declared)
                return false;
            if (description == null) {
                if (other.description != null)
                    return false;
            } else if (!description.equals(other.description))
                return false;
            if (matchedDefinition == null) {
                if (other.matchedDefinition != null)
                    return false;
            } else if (!matchedDefinition.equals(other.matchedDefinition))
                return false;
            if (!Arrays.equals(methodParameterIndexes, other.methodParameterIndexes))
                return false;
            if (!Arrays.equals(methodsChain, other.methodsChain))
                return false;
            if (numberOfUsedColumns != other.numberOfUsedColumns)
                return false;
            if (statement == null) {
                if (other.statement != null)
                    return false;
            } else if (!statement.equals(other.statement))
                return false;
            return true;
        }

    }
}
