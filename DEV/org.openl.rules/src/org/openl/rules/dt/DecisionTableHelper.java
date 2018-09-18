package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.binding.IBindingContext;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.fuzzy.OpenLFuzzySearch;
import org.openl.rules.fuzzy.Token;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.INumberRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
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
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DomainOpenClass;

public class DecisionTableHelper {

    private static final Pattern RANGE_PATTERN = Pattern.compile(".*(more|less|[;<>\\[(+]).*|.*\\d+.*(-|\\.\\.).*");

    /**
     * Check if table is vertical.<br>
     * Vertical table is when conditions are represented from left to right,
     * table is reading from top to bottom.</br>
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
    public static boolean looksLikeVertical(ILogicalTable table) {

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

    public static boolean isValidConditionHeader(String s) {
        return s.length() >= 2 && s.charAt(0) == DecisionTableColumnHeaders.CONDITION.getHeaderKey()
            .charAt(0) && Character.isDigit(s.charAt(1));
    }

    public static boolean isValidHConditionHeader(String headerStr) {
        return headerStr.startsWith(
            DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey()) && headerStr.length() > 2 && Character
                .isDigit(headerStr.charAt(2));
    }

    public static boolean isValidMergedConditionHeader(String headerStr) {
        return headerStr.startsWith(
            DecisionTableColumnHeaders.MERGED_CONDITION.getHeaderKey()) && headerStr.length() > 2 && Character
                .isDigit(headerStr.charAt(2));
    }

    public static boolean isValidActionHeader(String s) {
        return s.length() >= 2 && s.charAt(0) == DecisionTableColumnHeaders.ACTION.getHeaderKey().charAt(0) && Character
            .isDigit(s.charAt(1));
    }

    public static boolean isValidRetHeader(String s) {
        return s.length() >= 3 && s.startsWith(
            DecisionTableColumnHeaders.RETURN.getHeaderKey()) && (s.length() == 3 || Character.isDigit(s.charAt(3)));
    }

    public static boolean isValidKeyHeader(String s) {
        return s.length() >= 3 && s.startsWith(
            DecisionTableColumnHeaders.KEY.getHeaderKey()) && (s.length() == 3 || Character.isDigit(s.charAt(3)));
    }

    public static boolean isValidCRetHeader(String s) {
        return s.length() >= 4 && s.startsWith(DecisionTableColumnHeaders.COLLECT_RETURN
            .getHeaderKey()) && (s.length() == 4 || Character.isDigit(s.charAt(4)));
    }

    public static boolean isValidRuleHeader(String s) {
        return s.equals(DecisionTableColumnHeaders.RULE.getHeaderKey());
    }

    public static boolean isActionHeader(String s) {
        return isValidActionHeader(s) || isValidRetHeader(s) || isValidCRetHeader(s) || isValidKeyHeader(s);
    }

    public static boolean isConditionHeader(String s) {
        return isValidConditionHeader(s) || isValidHConditionHeader(s) || isValidMergedConditionHeader(s);
    }

    public static int countConditionsAndActions(ILogicalTable table) {

        int width = table.getWidth();
        int count = 0;

        for (int i = 0; i < width; i++) {

            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();

            if (value != null) {
                value = value.toUpperCase();
                count += isValidConditionHeader(value) || isActionHeader(value) ? 1 : 0;
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
     * Creates virtual headers for condition and return columns to load simple
     * Decision Table as an usual Decision Table
     *
     * @param decisionTable method description for simple Decision Table.
     * @param originalTable The original body of simple Decision Table.
     * @param numberOfHcondition The number of horizontal conditions. In
     *            SimpleRules it == 0 in SimpleLookups > 0
     * @return prepared usual Decision Table.
     */
    public static ILogicalTable preprocessSimpleDecisionTable(TableSyntaxNode tableSyntaxNode,
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
        Pair<Condition[], Integer> c = writeConditions(grid,
            originalTable,
            decisionTable,
            numberOfHcondition,
            isSmartDecisionTable,
            isCollectTable,
            bindingContext);

        writeReturn(tableSyntaxNode,
            grid,
            originalTable,
            decisionTable,
            c.getRight(),
            c.getLeft(),
            numberOfMergedRows,
            numberOfHcondition > 0,
            isSmartDecisionTable,
            isCollectTable,
            bindingContext);
    }

    private final static String RET1_COLUMN_NAME = DecisionTableColumnHeaders.RETURN.getHeaderKey() + "1";
    private final static String CRET1_COLUMN_NAME = DecisionTableColumnHeaders.COLLECT_RETURN.getHeaderKey() + "1";
    private final static String KEY1_COLUMN_NAME = DecisionTableColumnHeaders.KEY.getHeaderKey() + "1";

    private static boolean isCompoundReturnType(ILogicalTable originalTable,
            DecisionTable decisionTable,
            int firstReturnColumn,
            boolean isCollectTable) {
        int c = firstReturnColumn;
        int compoundTypeParameterCount = 0;
        while (c < originalTable.getSource().getWidth()) {
            if (originalTable.getSource().getCell(c, 0).getStringValue() == null || originalTable.getSource()
                .getCell(c, 0)
                .getStringValue()
                .trim()
                .isEmpty()) {
                break;
            }
            c = c + originalTable.getSource().getCell(c, 0).getWidth();
            compoundTypeParameterCount++;
        }

        IOpenClass returnType = decisionTable.getType();
        if (isCollectTable) {
            return compoundTypeParameterCount > 1;
        } else {
            return compoundTypeParameterCount > 1 && !returnType.isArray();
        }
    }

    private static void validateCompoundReturnType(IOpenClass compoundType) throws OpenLCompilationException {
        try {
            compoundType.getInstanceClass().getConstructor();
        } catch (Exception e) {
            throw new OpenLCompilationException(
                String.format("Invalid compound return type: There is no default constructor in return type '%s'",
                    compoundType.getDisplayName(0)));
        }
    }

    private static void writeCompoundReturnColumns(TableSyntaxNode tableSyntaxNode,
            IWritableGrid grid,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            int firstReturnColumn,
            int numberOfMergedRows,
            Condition[] conditions,
            boolean isSmartDecisionTable,
            boolean isCollectTable,
            int retParameterIndex,
            IBindingContext bindingContext) throws OpenLCompilationException {
        int numberOfConditions = conditions.length;
        int compoundReturnColumnsCount = calculateCompoundReturnColumnsCount(originalTable,
            numberOfConditions,
            numberOfMergedRows);
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
        validateCompoundReturnType(compoundType);

        StringBuilder sb = new StringBuilder();
        sb.append(compoundType.getName()).append(" ret = new ").append(compoundType.getName()).append("();");

        if (isSmartDecisionTable) {
            // Set conditions parameters to compound type. Recursively search is
            // not supported.
            for (Condition condition : conditions) {
                String descriptionOfCondition = condition.getDescription();
                try {
                    IOpenMethod bestMatchConditionMethod = findBestMatchOpenMethod(descriptionOfCondition,
                        compoundType,
                        true);
                    sb.append("ret.");
                    sb.append(bestMatchConditionMethod.getName());
                    sb.append("(");
                    sb.append(
                        String.valueOf(decisionTable.getSignature().getParameterName(condition.getParameterIndex())));
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
                    m = findBestMatchOpenMethodRecursively(description, type);
                }

                if (!bindingContext.isExecutionMode()) {
                    if (fieldChainSb == null) {
                        fieldChainSb = new StringBuilder();
                    } else {
                        fieldChainSb.append(".");
                    }
                    IOpenClass t = type;
                    for (int j = 0; j < m.length; j++) {
                        IOpenField openField = t.getField(m[j].getName().substring(3), false);
                        fieldChainSb.append(openField.getDisplayName(0));
                        if (j < m.length - 1) {
                            fieldChainSb.append(".");
                        }
                        t = m[j].getSignature().getParameterType(0);
                    }
                }

                /*
                 * if (type.isArray()){ throw new
                 * OpenLCompilationException(String.
                 * format("Found array type for field in the return type for the title '%s'. Current version doesn't support arrays in the return type."
                 * , description)); }
                 */
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
                String description = "Return: " + type.getDisplayName(0) + " " + fieldChainSb.toString();

                MetaInfoReader metaReader = tableSyntaxNode.getMetaInfoReader();
                if (metaReader instanceof DecisionTableMetaInfoReader) {
                    DecisionTableMetaInfoReader metaInfoReader = (DecisionTableMetaInfoReader) metaReader;
                    metaInfoReader.addSimpleRulesReturn(cell.getAbsoluteRow(), cell.getAbsoluteColumn(), description);
                }
            }

            column += mergedColumnsCounts;
        }

        sb.append("ret;");
        grid.setCellValue(firstReturnColumn, 1, sb.toString());

        for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
            grid.addMergedRegion(new GridRegion(row, firstReturnColumn, row, column - 1));
        }
    }

    private static int calculateCompoundReturnColumnsCount(ILogicalTable originalTable,
            int numberOfConditions,
            int numberOfMergedRows) {
        return originalTable.getRow(numberOfMergedRows).getWidth() - numberOfConditions;
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
                    String.format("Change title: No field match in the return type for the title '%s'.", description));
            } else {
                return null;
            }
        }
        if (fuzzyBestMatches.length > 1) {
            throw new OpenLCompilationException(String
                .format("Change title: More than one field match in the return type for the title '%s'.", description));
        }
        if (openClassFuzzyTokens
            .get(fuzzyBestMatches[0]) == null || openClassFuzzyTokens.get(fuzzyBestMatches[0]).length == 0) {
            if (validateEmptyResult) {
                throw new OpenLCompilationException(
                    String.format("Change title: No field match in the return type for the title '%s'.", description));
            } else {
                return null;
            }
        }
        if (openClassFuzzyTokens.get(fuzzyBestMatches[0]).length > 1) {
            throw new OpenLCompilationException(String
                .format("Change title: More than one field match in the return type for the title '%s'.", description));
        }

        return openClassFuzzyTokens.get(fuzzyBestMatches[0])[0];
    }

    private static IOpenMethod[] findBestMatchOpenMethodRecursively(String description,
            IOpenClass openClass) throws OpenLCompilationException {
        Map<Token, IOpenMethod[][]> openClassFuzzyTokens = OpenLFuzzySearch
            .tokensMapToOpenClassSetterMethodsRecursively(openClass);

        String tokenizedDescriptionString = OpenLFuzzySearch.toTokenString(description);
        Token[] fuzzyBestMatches = OpenLFuzzySearch.openlFuzzyExtract(tokenizedDescriptionString,
            openClassFuzzyTokens.keySet().toArray(new Token[] {}));

        if (fuzzyBestMatches.length == 0) {
            throw new OpenLCompilationException(
                String.format("Change title: No field match in the return type for the title '%s'.", description));
        }
        if (fuzzyBestMatches.length > 1) {
            throw new OpenLCompilationException(String
                .format("Change title: More than one field match in the return type for the title '%s'.", description));
        }
        if (openClassFuzzyTokens
            .get(fuzzyBestMatches[0]) == null || openClassFuzzyTokens.get(fuzzyBestMatches[0]).length == 0) {
            throw new OpenLCompilationException(
                String.format("Change title: No field match in the return type for the title '%s'.", description));
        }
        if (openClassFuzzyTokens.get(fuzzyBestMatches[0]).length > 1) {
            throw new OpenLCompilationException(String
                .format("Change title: More than one field match in the return type for the title '%s'.", description));
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

    private static void writeReturn(TableSyntaxNode tableSyntaxNode,
            IWritableGrid grid,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            int firstColumnAfterConditionColumns,
            Condition[] conditions,
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
                        IOpenClass componentOpenClass = decisionTable.getType().getAggregateInfo().getComponentType(
                            decisionTable.getType());
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
            if (originalTable.getWidth() > conditions.length) {
                boolean isCompoundReturnType = isCompoundReturnType(originalTable,
                    decisionTable,
                    firstReturnColumn,
                    isCollectTable);
                if (isCompoundReturnType) {
                    writeCompoundReturnColumns(tableSyntaxNode,
                        grid,
                        originalTable,
                        decisionTable,
                        firstReturnColumn,
                        numberOfMergedRows,
                        conditions,
                        isSmartDecisionTable,
                        isCollectTable,
                        retParameterIndex,
                        bindingContext);
                } else {
                    int mergedColumnsCounts = originalTable.getColumnWidth(conditions.length);
                    if (mergedColumnsCounts > 1) {
                        for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT; row++) {
                            grid.addMergedRegion(new GridRegion(row,
                                firstReturnColumn,
                                row,
                                firstReturnColumn + mergedColumnsCounts - 1));
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

    private static Pair<Condition[], Integer> writeConditions(IWritableGrid grid,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            int numberOfHcondition,
            boolean isSmartDecisionTable,
            boolean isCollectTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        int numberOfConditions;
        Condition[] conditions;
        if (isSmartDecisionTable) {
            conditions = findConditionsForParameters(originalTable, decisionTable, numberOfHcondition, isCollectTable);
            numberOfConditions = conditions.length;
        } else {
            numberOfConditions = getNumberOfConditions(decisionTable);

            conditions = new Condition[numberOfConditions];
            for (int i = 0; i < numberOfConditions; i++) {
                conditions[i] = new Condition(i);
            }
        }

        int column = 0;
        int vColumnCounter = 0;
        int hColumn = -1;

        for (int i = 0; i < numberOfConditions; i++) {
            if (column >= originalTable.getSource().getWidth()) {
                String message = "Wrong table structure: Columns count is less than parameters count";
                throw new OpenLCompilationException(message);
            }
            // write headers
            //
            boolean isThatVCondition = i < numberOfConditions - numberOfHcondition;
            boolean lastCondition = i + 1 == numberOfConditions;

            if (isThatVCondition) {
                vColumnCounter++;
                // write simple condition
                //
                if (i == 0 && numberOfHcondition == 0 && numberOfConditions < 2) {
                    grid.setCellValue(column,
                        0,
                        (DecisionTableColumnHeaders.MERGED_CONDITION.getHeaderKey() + (i + 1)).intern());
                } else {
                    grid.setCellValue(column,
                        0,
                        (DecisionTableColumnHeaders.CONDITION.getHeaderKey() + (i + 1)).intern());
                }
            } else {
                if (hColumn < 0) {
                    hColumn = column;
                }
                // write horizontal condition
                //
                grid.setCellValue(column,
                    0,
                    (DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey() + (i + 1)).intern());
            }

            grid.setCellValue(column,
                1,
                decisionTable.getSignature().getParameterName(conditions[i].getParameterIndex()));

            // Set type of condition values(for Ranges and Array)
            Pair<String, String> typeOfValue = checkTypeOfValues(bindingContext,
                originalTable,
                i,
                decisionTable.getSignature().getParameterTypes()[conditions[i].getParameterIndex()],
                isThatVCondition,
                lastCondition,
                vColumnCounter);
            grid.setCellValue(column, 2, typeOfValue.getLeft());

            if (!bindingContext.isExecutionMode()) {
                MetaInfoReader metaReader = decisionTable.getSyntaxNode().getMetaInfoReader();
                if (metaReader instanceof DecisionTableMetaInfoReader) {
                    DecisionTableMetaInfoReader metaInfoReader = (DecisionTableMetaInfoReader) metaReader;
                    ICell cell = originalTable.getSource().getCell(column, 0);
                    metaInfoReader.addSimpleRulesCondition(cell.getAbsoluteRow(),
                        cell.getAbsoluteColumn(),
                        conditions[i].getParameterIndex());
                }
            }

            // merge columns
            if (isThatVCondition || lastCondition) {
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

        if (!bindingContext.isExecutionMode()) {
            writeMetaInfoForHConditions(originalTable,
                decisionTable,
                conditions,
                numberOfHcondition,
                numberOfConditions,
                hColumn);
        }

        return new ImmutablePair<>(conditions, column);
    }

    private static void writeMetaInfoForHConditions(ILogicalTable originalTable,
            DecisionTable decisionTable,
            Condition[] conditions,
            int numberOfHcondition,
            int numberOfConditions,
            int hColumn) {
        MetaInfoReader metaInfoReader = decisionTable.getSyntaxNode().getMetaInfoReader();
        int j = 0;
        for (int i = numberOfConditions - numberOfHcondition; i < numberOfConditions; i++) {
            int c = hColumn;
            while (c <= originalTable.getSource().getWidth()) {
                ICell cell = originalTable.getSource().getCell(c, j);

                String cellValue = cell.getStringValue();
                if (cellValue != null) {
                    if (metaInfoReader instanceof DecisionTableMetaInfoReader) {
                        ((DecisionTableMetaInfoReader) metaInfoReader).addSimpleRulesCondition(cell.getAbsoluteRow(),
                            cell.getAbsoluteColumn(),
                            conditions[i].getParameterIndex());
                    }
                }
                c = c + cell.getWidth();
            }
            j++;
        }
    }

    private final static class Condition {
        int parameterIndex;
        String description;

        public Condition(int parameterIndex) {
            this.parameterIndex = parameterIndex;
        }

        public Condition(int parameterIndex, String description) {
            this.parameterIndex = parameterIndex;
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public int getParameterIndex() {
            return parameterIndex;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + parameterIndex;
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
            Condition other = (Condition) obj;
            return parameterIndex == other.parameterIndex;
        }

    }

    private static Condition[] findConditionsForParameters(ILogicalTable originalTable,
            DecisionTable decisionTable,
            int numberOfHcondition,
            boolean isCollectTable) throws OpenLCompilationException {
        int numberOfParameters = decisionTable.getSignature().getNumberOfParameters();
        int column = 0;
        List<List<Condition>> vConditions = new ArrayList<>();

        BidiMap<String, Integer> parameterTokensMap = new DualHashBidiMap<>();
        Token[] parameterTokens = new Token[numberOfParameters];
        for (int i = 0; i < numberOfParameters; i++) {
            String tokenString = OpenLFuzzySearch.toTokenString(decisionTable.getSignature().getParameterName(i));
            parameterTokensMap.put(tokenString, i);
            parameterTokens[i] = new Token(tokenString, 0);
        }
        int j = 0;
        int firstColumnHeight = originalTable.getCell(0, 0).getHeight();
        while (j < numberOfParameters) {
            if (originalTable.getCell(column, 0).getHeight() != firstColumnHeight) {
                break;
            }
            String description = originalTable.getCell(column, 0).getStringValue();

            column += originalTable.getColumnWidth(column);

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

            String tokenizedDescriptionString = OpenLFuzzySearch.toTokenString(description);
            Token[] bestMatchedTokens = OpenLFuzzySearch.openlFuzzyExtract(tokenizedDescriptionString, parameterTokens);
            if (bestMatchedTokens.length == 0) {
                break;
            }

            if (bestMatchedTokens.length > 1) {
                List<Condition> conditions = new ArrayList<>();
                for (Token token : bestMatchedTokens) {
                    conditions.add(new Condition(parameterTokensMap.get(token.getValue()), description));
                }

                vConditions.add(conditions);
            } else {
                Condition currentConditionDescrition = new Condition(
                    parameterTokensMap.get(bestMatchedTokens[0].getValue()),
                    description);
                boolean alreadyExists = false;
                for (List<Condition> vConditionDescriptionList : vConditions) {
                    if (vConditionDescriptionList.size() == 1) {
                        if (vConditionDescriptionList.get(0).getParameterIndex() == currentConditionDescrition
                            .getParameterIndex()) {
                            alreadyExists = true;
                            break;
                        }
                    }
                }
                if (alreadyExists) {
                    break;
                }
                for (List<Condition> vConditionDescriptionList : vConditions) {
                    if (vConditionDescriptionList.size() > 1) {
                        vConditionDescriptionList.remove(currentConditionDescrition);
                        if (vConditionDescriptionList.size() == 1) {
                            Integer index = vConditionDescriptionList.get(0).getParameterIndex();
                            parameterTokensMap.removeValue(index);
                        }
                    }
                }
                vConditions.add(Collections.singletonList(currentConditionDescrition));
            }
            j++;
        }

        Condition[] conditions = new Condition[vConditions.size() + numberOfHcondition];
        boolean[] parameterIsUsed = new boolean[numberOfParameters];
        Arrays.fill(parameterIsUsed, false);
        int v = 0;
        for (List<Condition> vConditionDescriptionList : vConditions) {
            if (vConditionDescriptionList.size() > 1) {
                throw new OpenLCompilationException(
                    String.format("Change title: More than one input parameter match the title '%s'.",
                        vConditionDescriptionList.get(0).getDescription()));
            } else {
                conditions[v] = vConditionDescriptionList.get(0);
                parameterIsUsed[conditions[v].getParameterIndex()] = true;
            }
            v++;
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

        j = 0;
        for (int w = i + 1; w < numberOfParameters; w++) {
            if (!parameterIsUsed[w] && j < numberOfHcondition) {
                conditions[vConditions.size() + j] = new Condition(w);
                j++;
            }
        }

        return conditions;
    }

    /**
     * Check type of condition values. If condition values are complex(Range,
     * Array) then types of complex values will be returned
     * 
     * @param originalTable The original body of simple Decision Table.
     * @param column The number of a condition
     * @param type The type of an input parameter
     * @param isThatVCondition If condition is vertical value = true
     * @param vColumnCounter Counter of vertical conditions. Needed for
     *            calculating position of horizontal condition
     * @return type of condition values
     */
    private static Pair<String, String> checkTypeOfValues(IBindingContext bindingContext,
            ILogicalTable originalTable,
            int column,
            IOpenClass type,
            boolean isThatVCondition,
            boolean lastCondition,
            int vColumnCounter) {
        final List<String> intType = Arrays.asList("byte",
            "short",
            "int",
            "java.lang.Byte",
            "org.openl.meta.ByteValue",
            "org.openl.meta.ShortValue",
            "org.openl.meta.IntValue",
            "org.openl.meta.BigIntegerValue",
            "java.lang.Integer",
            "org.openl.meta.IntegerValue");
        final List<String> doubleType = Arrays.asList("long",
            "float",
            "double",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "org.openl.meta.LongValue",
            "org.openl.meta.FloatValue",
            "org.openl.meta.DoubleValue",
            "org.openl.meta.BigDecimalValue");
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
                    return new ImmutablePair<>(type.getName() + "[]", type.getDisplayName(0) + "[]");
                } else {
                    return new ImmutablePair<>(type.getName(), type.getDisplayName(0));
                }
            }
        }

        for (int valueNum = 1; valueNum < width; valueNum++) {
            ILogicalTable cellValue;

            if (isThatVCondition) {
                cellValue = decisionValues.getRow(valueNum);
            } else {
                cellValue = decisionValues.getColumn(valueNum);
            }

            if (cellValue.getSource().getCell(0, 0).getStringValue() == null) {
                continue;
            }

            ConstantOpenField constantOpenField = RuleRowHelper.findConstantField(bindingContext,
                cellValue.getSource().getCell(0, 0).getStringValue());
            if (constantOpenField != null && (IntRange.class.equals(constantOpenField.getType()
                .getInstanceClass()) || DoubleRange.class.equals(constantOpenField.getType().getInstanceClass()))) {
                return new ImmutablePair<>(constantOpenField.getType().getInstanceClass().getSimpleName(),
                    constantOpenField.getType().getInstanceClass().getSimpleName());
            }

            if (maybeIsRange(cellValue.getSource().getCell(0, 0).getStringValue())) {
                INumberRange range;

                String typeName = type instanceof DomainOpenClass ? type.getInstanceClass().getCanonicalName()
                                                                  : type.getName();

                /* try to create range by values **/
                if (intType.contains(typeName)) {
                    try {
                        range = new IntRange(cellValue.getSource().getCell(0, 0).getStringValue());

                        /* Return name of a class without a package prefix **/
                        return new ImmutablePair<>(range.getClass().getSimpleName(), range.getClass().getSimpleName());
                    } catch (Exception e) {
                        continue;
                    }
                } else if (doubleType.contains(typeName)) {
                    try {
                        range = new DoubleRange(cellValue.getSource().getCell(0, 0).getStringValue());

                        /* Return name of a class without a package prefix **/
                        return new ImmutablePair<>(range.getClass().getSimpleName(), range.getClass().getSimpleName());
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }
        if (!type.isArray()) {
            return new ImmutablePair<>(type.getName() + "[]", type.getDisplayName(0) + "[]");
        } else {
            return new ImmutablePair<>(type.getName(), type.getDisplayName(0));
        }
    }

    private static boolean maybeIsRange(String cellValue) {
        Matcher m = RANGE_PATTERN.matcher(cellValue);
        return m.matches();
    }

    private static int getNumberOfConditions(DecisionTable decisionTable) {
        // number of conditions is counted by the number of income parameters
        //
        return decisionTable.getSignature().getNumberOfParameters();
    }

    public static IWritableGrid createVirtualGrid(String poiSheetName, int numberOfColumns) {
        // Pre-2007 excel sheets had a limitation of 256 columns.
        Workbook workbook = (numberOfColumns > 256) ? new XSSFWorkbook() : new HSSFWorkbook();
        final Sheet sheet = workbook.createSheet(poiSheetName);
        return createVirtualGrid(sheet);
    }

    public static boolean isSimpleDecisionTableOrSmartDecisionTable(TableSyntaxNode tableSyntaxNode) {
        return isSimpleDecisionTable(tableSyntaxNode) || isSmartDecisionTable(tableSyntaxNode);
    }

    public static boolean isCollectDecisionTable(TableSyntaxNode tableSyntaxNode) {
        return tableSyntaxNode.getHeader().isCollect();
    }

    public static boolean isSimpleDecisionTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();

        return IXlsTableNames.SIMPLE_DECISION_TABLE.equals(dtType);
    }

    public static boolean isSmartDecisionTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();

        return IXlsTableNames.SMART_DECISION_TABLE.equals(dtType);
    }

    public static boolean isSmartSimpleLookupTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();

        return IXlsTableNames.SMART_DECISION_LOOKUP.equals(dtType);
    }

    public static boolean isSimpleLookupTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();

        return IXlsTableNames.SIMPLE_DECISION_LOOKUP.equals(dtType) || isSmartSimpleLookupTable(tableSyntaxNode);
    }

    public static int countHConditions(ILogicalTable table) {
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

    public static int countVConditions(ILogicalTable table) {
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

}
