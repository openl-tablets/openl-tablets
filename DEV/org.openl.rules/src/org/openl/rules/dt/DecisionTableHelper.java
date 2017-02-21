package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.fuzzy.FuzzyUtils;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.INumberRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.load.SimpleSheetLoader;
import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
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
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;

public class DecisionTableHelper {

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

    public static boolean isValidRuleHeader(String s) {
        return s.equals(DecisionTableColumnHeaders.RULE.getHeaderKey());
    }

    public static boolean isActionHeader(String s) {
        return isValidActionHeader(s) || isValidRetHeader(s);
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
    public static ILogicalTable preprocessSimpleDecisionTable(DecisionTable decisionTable,
            ILogicalTable originalTable,
            int numberOfHcondition,
            boolean smartDecisionTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        IWritableGrid virtualGrid = createVirtualGrid();
        writeVirtualHeadersForSimpleDecisionTable(virtualGrid,
            originalTable,
            decisionTable,
            numberOfHcondition,
            smartDecisionTable,
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

    private static void writeVirtualHeadersForSimpleDecisionTable(IWritableGrid grid,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            int numberOfHcondition,
            boolean smartDecisionTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        Pair<Condition[], Integer> c = writeConditions(grid,
            originalTable,
            decisionTable,
            numberOfHcondition,
            smartDecisionTable,
            bindingContext);

        writeReturn(grid,
            originalTable,
            decisionTable,
            c.getLeft().length,
            c.getRight(),
            c.getLeft(),
            numberOfHcondition > 0,
            smartDecisionTable,
            bindingContext);
    }

    private final static String RET1_COLUMN_NAME = DecisionTableColumnHeaders.RETURN.getHeaderKey() + "1";

    private static boolean isCompoundReturnType(ILogicalTable originalTable,
            DecisionTable decisionTable,
            int numberOfConditions) {
        int compoundTypeParameters = originalTable.getWidth() - numberOfConditions;
        IOpenClass returnType = decisionTable.getType();
        return compoundTypeParameters > 1 && !returnType.isArray();
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

    private static void writeCompoundReturnColumns(IWritableGrid grid,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            int numberOfConditions,
            int columnsForConditions,
            Condition[] conditions,
            boolean smartDecisionTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        int compoundTypeParameters = originalTable.getWidth() - numberOfConditions;
        IOpenClass compoundType = decisionTable.getType();
        validateCompoundReturnType(compoundType);
        IOpenMethod[] openMethods = new IOpenMethod[compoundTypeParameters];
        //Set<IOpenMethod> usedMethods = new HashSet<IOpenMethod>();
        int column = columnsForConditions;
        for (int i = 0; i < compoundTypeParameters; i++) {
            String description = originalTable.getSource().getCell(column, 0).getStringValue();
            openMethods[i] = findBestMatchSetterMethodInCompoundType(description, compoundType);
            //usedMethods.add(openMethods[i]);
            int mergedColumnsCounts = originalTable.getSource().getCell(column, 2).getWidth();
            column += mergedColumnsCounts;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(compoundType.getName() + " ret = new " + compoundType.getName() + "();");

        if (smartDecisionTable) {
            for (int i = 0; i < numberOfConditions; i++) {
                String conditionDescription = conditions[i].getDescription();
                try{
                    IOpenMethod bestMatchMethodForCondition = findBestMatchSetterMethodInCompoundType(conditionDescription, compoundType);
                    //usedMethods.add(bestMatchMethodForCondition);
                    sb.append("ret.");
                    sb.append(bestMatchMethodForCondition.getName());
                    sb.append("(");
                    sb.append(
                    String.valueOf(decisionTable.getSignature().getParameterName(conditions[i].getParameterIndex())));
                    sb.append(");");
                }catch (Exception e) {
                }
            }
        }

        String[] paramNames = new String[compoundTypeParameters];
        for (int i = 0; i < compoundTypeParameters; i++) {
            paramNames[i] = RandomStringUtils.random(8, true, false);
        }
        int v = 0;
        for (IOpenMethod method : openMethods) {
            sb.append("ret.");
            sb.append(method.getName());
            sb.append("(");
            sb.append(String.valueOf(paramNames[v]));
            sb.append(");");
            v++;
        }

        sb.append("ret;");
        grid.setCellValue(columnsForConditions, 1, sb.toString());

        column = columnsForConditions;
        for (int i = 0; i < compoundTypeParameters; i++) {
            String stringValue = originalTable.getSource().getCell(column, 0).getStringValue();
            IOpenClass typeClass = openMethods[i].getSignature().getParameterType(0);
            grid.setCellValue(column, 2, typeClass.getName() + " " + paramNames[i]);

            int mergedColumnsCounts = originalTable.getSource().getCell(column, 2).getWidth();
            if (mergedColumnsCounts > 1) {
                grid.addMergedRegion(new GridRegion(2, column, 2, column + mergedColumnsCounts - 1));
            }

            if (!bindingContext.isExecutionMode()) {
                IOpenField openField = compoundType.getField(openMethods[i].getName().substring(3), false);
                String description = "Return: " + typeClass.getDisplayName(0) + " " + openField.getDisplayName(0);
                ICell cell = originalTable.getSource().getCell(column, 0);
                SimpleNodeUsage simpleNodeUsage = new SimpleNodeUsage(0,
                    stringValue.length() - 1,
                    description,
                    null,
                    NodeType.OTHER);
                CellMetaInfo meta = new CellMetaInfo(CellMetaInfo.Type.DT_CA_CODE,
                    null,
                    JavaOpenClass.STRING,
                    false,
                    Collections.singletonList(simpleNodeUsage));
                cell.setMetaInfo(meta);
            }

            column += mergedColumnsCounts;
        }

        for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
            grid.addMergedRegion(new GridRegion(row, columnsForConditions, row, column - 1));
        }

    }

    private static IOpenMethod findBestMatchSetterMethodInCompoundType(String description,
            IOpenClass compoundType) throws OpenLCompilationException {
        List<IOpenMethod> bestMatchMethods = new ArrayList<IOpenMethod>();
        double bestDistance = 0;
        for (IOpenMethod method : compoundType.getMethods()) {
            if (!method.isStatic() && method.getSignature().getNumberOfParameters() == 1 && method.getName()
                .startsWith("set")) {
                double distance = FuzzyUtils.getFuzzyDistance(method.getName().substring(3), description);
                if (Math.abs(distance - bestDistance) <= 1e-3 && Math.abs(bestDistance) > 1e-3) {
                    bestMatchMethods.add(method);
                } else {
                    if (distance - bestDistance > 1e-3) {
                        bestMatchMethods.clear();
                        bestDistance = distance;
                        bestMatchMethods.add(method);
                    }
                }
            }
        }
        if (bestMatchMethods.isEmpty()) {
            throw new OpenLCompilationException(String.format(
                "Change title: No field match in the return type for the title '%s'.", description));
        }
        if (bestMatchMethods.size() > 1) {
            throw new OpenLCompilationException(String.format(
                "Change title: More than one field match in the return type for the title '%s'.",
                description));
        }
        return bestMatchMethods.get(0);
    }

    private static void writeReturn(IWritableGrid grid,
            ILogicalTable originalTable,
            DecisionTable decisionTable,
            int numberOfConditions,
            int columnsForConditions,
            Condition[] conditions,
            boolean isLookupTable,
            boolean smartDecisionTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        // write return column
        //
        grid.setCellValue(columnsForConditions, 0, RET1_COLUMN_NAME);

        if (!isLookupTable) {
            if (originalTable.getWidth() > numberOfConditions) {
                boolean isCompoundReturnType = isCompoundReturnType(originalTable, decisionTable, numberOfConditions);
                if (isCompoundReturnType) {
                    writeCompoundReturnColumns(grid,
                        originalTable,
                        decisionTable,
                        numberOfConditions,
                        columnsForConditions,
                        conditions,
                        smartDecisionTable,
                        bindingContext);
                } else {
                    int mergedColumnsCounts = originalTable.getColumnWidth(getNumberOfConditions(decisionTable));
                    if (mergedColumnsCounts > 1) {
                        for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT; row++) {
                            grid.addMergedRegion(new GridRegion(row,
                                columnsForConditions,
                                row,
                                columnsForConditions + mergedColumnsCounts - 1));
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
            boolean smartDecisionTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        int numberOfConditions;
        Condition[] conditions;
        if (smartDecisionTable) {
            conditions = findConditionsForParameters(originalTable, decisionTable, numberOfHcondition);
            numberOfConditions = conditions.length;
        } else {
            numberOfConditions = getNumberOfConditions(decisionTable);
            conditions = new Condition[numberOfConditions];
            for (int i = 0; i < numberOfConditions; i++) {
                conditions[i] = new Condition(i, i >= numberOfConditions - numberOfHcondition);
            }
        }

        int column = 0;
        int vColumnCounter = 0;
        int hColumn = -1;

        for (int i = 0; i < numberOfConditions; i++) {
            if (column > originalTable.getWidth()) {
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
                if (hColumn < 0){
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
            Pair<String, String> typeOfValue = checkTypeOfValues(originalTable,
                i,
                decisionTable.getSignature().getParameterTypes()[conditions[i].getParameterIndex()],
                isThatVCondition,
                lastCondition,
                vColumnCounter);
            grid.setCellValue(column, 2, typeOfValue.getLeft());

            if (!bindingContext.isExecutionMode()) {
                writeMetaInfoForCondition(originalTable, column, decisionTable.getSignature().getParameterName(i), typeOfValue.getRight());
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
        
        if (!bindingContext.isExecutionMode()){
            writeMetaInfoForHConditions(originalTable, decisionTable, conditions, numberOfHcondition, numberOfConditions, hColumn);
        }

        return new ImmutablePair<Condition[], Integer>(conditions, column);
    }

    private static void writeMetaInfoForHConditions(ILogicalTable originalTable,
            DecisionTable decisionTable,
            Condition[] conditions,
            int numberOfHcondition,
            int numberOfConditions,
            int hColumn) {
        int j = 0;
        for (int i = numberOfConditions - numberOfHcondition; i < numberOfConditions; i++) {
            int c = hColumn; 
            while (c <= originalTable.getWidth()){
                String cellValue = originalTable.getSource().getCell(c, j).getFormattedValue();
                String text = String.format("Condition for %s", decisionTable.getSignature().getParameterName(conditions[i].getParameterIndex()));
                ICell cell = originalTable.getSource().getCell(c, j);
                SimpleNodeUsage simpleNodeUsage = new SimpleNodeUsage(0,
                    cellValue.length() - 1,
                    text,
                    null,
                    NodeType.OTHER);
                CellMetaInfo meta = new CellMetaInfo(CellMetaInfo.Type.DT_CA_CODE,
                    null,
                    JavaOpenClass.STRING,
                    false,
                    Collections.singletonList(simpleNodeUsage));
                cell.setMetaInfo(meta);
                c++;
            }
            j++;
        }
    }

    private static void writeMetaInfoForCondition(ILogicalTable originalTable,
            int column,
            String parameterName,
            String typeOfValue) {
        String cellValue = originalTable.getSource().getCell(column, 0).getFormattedValue();
        String text = String.format("Condition for %s: %s", parameterName, typeOfValue);
        ICell cell = originalTable.getSource().getCell(column, 0);
        SimpleNodeUsage simpleNodeUsage = new SimpleNodeUsage(0,
            cellValue.length() - 1,
            text,
            null,
            NodeType.OTHER);
        CellMetaInfo meta = new CellMetaInfo(CellMetaInfo.Type.DT_CA_CODE,
            null,
            JavaOpenClass.STRING,
            false,
            Collections.singletonList(simpleNodeUsage));
        cell.setMetaInfo(meta);
    }

    private final static class Condition {
        int parameterIndex;
        String description;
        boolean hCondition;

        public Condition(int parameterIndex, boolean hCondition) {
            this.parameterIndex = parameterIndex;
            this.hCondition = hCondition;
        }

        public Condition(int parameterIndex, String description, boolean hCondition) {
            this.parameterIndex = parameterIndex;
            this.description = description;
            this.hCondition = false;
        }

        public String getDescription() {
            return description;
        }

        public int getParameterIndex() {
            return parameterIndex;
        }

        public boolean isHCondition() {
            return hCondition;
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
            if (parameterIndex != other.parameterIndex)
                return false;
            return true;
        }
        
    }

    private static Condition[] findConditionsForParameters(ILogicalTable originalTable,
            DecisionTable decisionTable,
            int numberOfHcondition) throws OpenLCompilationException {
        int numberOfParameters = decisionTable.getSignature().getNumberOfParameters();
        int column = 0;
        List<List<Condition>> vConditions = new ArrayList<List<Condition>>();
        while (column + numberOfHcondition < originalTable.getWidth()) {
            String description = originalTable.getCell(column, 0).getStringValue();
            double bestFuzzyDistance = 0;
            List<Condition> bestMatchParameters = new ArrayList<Condition>();
            for (int i = 0; i < numberOfParameters - numberOfHcondition; i++) {
                String parameterName = decisionTable.getSignature().getParameterName(i);
                
                boolean g = false;
                for (List<Condition> vConditionDescriptionList : vConditions) {
                    if (vConditionDescriptionList.size() == 1){
                        if (vConditionDescriptionList.get(0).getParameterIndex() == i) {
                            g = true;
                            break;
                        }
                    }
                }
                if (g) { 
                    continue;
                }

                double fuzzyDistance = FuzzyUtils.getFuzzyDistance(parameterName, description);
                if (Math.abs(fuzzyDistance - bestFuzzyDistance) <= 1e-3 && Math.abs(bestFuzzyDistance) > 1e-3) {
                    bestMatchParameters.add(new Condition(i, description, false));
                } else {
                    if (fuzzyDistance - bestFuzzyDistance > 1e-3) {
                        bestFuzzyDistance = fuzzyDistance;
                        bestMatchParameters.clear();
                        bestMatchParameters.add(new Condition(i, description, false));
                    }
                }
            }

            if (bestMatchParameters.isEmpty()) {
                break;
            }

            column += originalTable.getColumnWidth(column);

            if (bestFuzzyDistance > 1e-3) {
                if (bestMatchParameters.size() > 1) {
                    vConditions.add(bestMatchParameters);
                } else {
                    Condition currentConditionDescrition = bestMatchParameters.get(0);
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
                        }
                    }
                    vConditions.add(Collections.singletonList(currentConditionDescrition));
                }
            }
        }

        Condition[] conditions = new Condition[vConditions.size() + numberOfHcondition]; 
        int v = 0;
        for (List<Condition> vConditionDescriptionList : vConditions) {
            if (vConditionDescriptionList.size() > 1){ 
                throw new OpenLCompilationException(
                    String.format("Change title: More than one input parameter match the title '%s'.",
                        vConditionDescriptionList.get(0).getDescription()));
            }else{
                conditions[v] = vConditionDescriptionList.get(0); 
            }
            v++;
        }
        
        for (int i = numberOfParameters - numberOfHcondition; i < numberOfParameters; i++) {
            conditions[vConditions.size() + i - (numberOfParameters - numberOfHcondition)] = new Condition(i, true);
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
    private static Pair<String, String> checkTypeOfValues(ILogicalTable originalTable,
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
                    return new ImmutablePair<String, String>(type.getName() + "[]", type.getDisplayName(0) + "[]");
                } else {
                    return new ImmutablePair<String, String>(type.getName(), type.getDisplayName(0));
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

            if (maybeIsRange(cellValue.getSource().getCell(0, 0).getStringValue())) {
                INumberRange range;

                String typeName = type instanceof DomainOpenClass ? type.getInstanceClass().getCanonicalName()
                                                                  : type.getName();

                /** try to create range by values **/
                if (intType.contains(typeName)) {
                    try {
                        range = new IntRange(cellValue.getSource().getCell(0, 0).getStringValue());

                        /** Return name of a class without a package prefix **/
                        return new ImmutablePair<String, String>(range.getClass().getSimpleName(), range.getClass().getSimpleName());
                    } catch (Exception e) {
                        continue;
                    }
                } else if (doubleType.contains(typeName)) {
                    try {
                        range = new DoubleRange(cellValue.getSource().getCell(0, 0).getStringValue());

                        /** Return name of a class without a package prefix **/
                        return new ImmutablePair<String, String>(range.getClass().getSimpleName(), range.getClass().getSimpleName());
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }
        if (!type.isArray()) {
            return new ImmutablePair<String, String>(type.getName() + "[]", type.getDisplayName(0) + "[]");
        } else {
            return new ImmutablePair<String, String>(type.getName(), type.getDisplayName(0));
        }
    }

    private static boolean maybeIsRange(String cellValue) {
        Pattern p = Pattern.compile(".*(more|less|[;<>\\[\\(+\\.]).*|.*\\d+.*-.*");
        Matcher m = p.matcher(cellValue);

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
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();

        return IXlsTableNames.SIMPLE_DECISION_TABLE.equals(dtType) || isSmartSimpleDecisionTable(tableSyntaxNode);
    }

    public static boolean isSmartSimpleDecisionTable(TableSyntaxNode tableSyntaxNode) {
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

                if (isValidConditionHeader(value)) {
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
