package org.openl.rules.dt;

import static org.openl.rules.dt.DecisionTableHelper.isLookup;
import static org.openl.rules.dt.DecisionTableHelper.isSimple;
import static org.openl.rules.dt.DecisionTableHelper.isSmart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.dt.DTScale.RowScale;
import org.openl.rules.dt.element.Action;
import org.openl.rules.dt.element.ActionType;
import org.openl.rules.dt.element.Condition;
import org.openl.rules.dt.element.RuleRow;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.DecisionTableMetaInfoReader;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.ParserUtils;

/**
 * @author snshor
 *
 */
public class DecisionTableLoader {

    /**
     * protected modified is for tests access.
     */
    static final String EMPTY_BODY = "Decision table must contain body section.";

    private static final int MAX_COLUMNS_IN_DT = 100;

    private static class TableStructure {
        private final List<IBaseCondition> conditions = new ArrayList<>();
        private final List<IBaseAction> actions = new ArrayList<>();
        private boolean hasReturnAction = false;
        private boolean hasCollectReturnAction = false;
        private boolean hasCollectReturnKeyAction = false;
        private String firstUsedReturnActionHeader = null;
        private RuleRow ruleRow;
        private DTInfo info;
        private int columnsNumber;
    }

    private RowScale getConditionScale(TableStructure tableStructure, String name) {
        if (DecisionTableHelper.isValidHConditionHeader(name.toUpperCase())) {
            return tableStructure.info.getScale().getHScale();
        }
        return tableStructure.info.getScale().getVScale();
    }

    private void addRule(int row,
            ILogicalTable table,
            TableStructure tableStructure,
            IBindingContext bindingContext) throws SyntaxNodeException {
        if (tableStructure.ruleRow != null) {
            throw SyntaxNodeExceptionUtils.createError("Only one rule row/column allowed.",
                new GridCellSourceCodeModule(table.getRow(row).getSource(),
                    IDecisionTableConstants.INFO_COLUMN_INDEX,
                    0,
                    bindingContext));
        }
        tableStructure.ruleRow = new RuleRow(row, table);
    }

    public void loadAndBind(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            OpenL openl,
            ModuleOpenClass module,
            boolean transpose,
            IBindingContext bindingContext) throws Exception {
        TableStructure tableStructure = loadTableStructure(tableSyntaxNode, decisionTable, transpose, bindingContext);
        IBaseCondition[] conditionsArray = tableStructure.conditions.toArray(IBaseCondition.EMPTY);
        IBaseAction[] actionsArray = tableStructure.actions.toArray(IBaseAction.EMPTY);
        decisionTable.bindTable(conditionsArray,
            actionsArray,
            tableStructure.ruleRow,
            openl,
            module,
            bindingContext,
            tableStructure.columnsNumber);
    }

    private enum Direction {
        UNKNOWN,
        TRANSPOSED,
        NORMAL;
    }

    private boolean isLookupByHConditions(ILogicalTable tableBody) {
        int numberOfHCondition = DecisionTableHelper.getNumberOfHConditions(tableBody);
        int firstColumnHeight = tableBody.getSource().getCell(0, 0).getHeight();
        int firstColumnForHCondition = DecisionTableHelper
            .getFirstColumnForHCondition(tableBody, numberOfHCondition, firstColumnHeight);
        if (firstColumnForHCondition > 0 && firstColumnHeight != tableBody.getSource()
            .getCell(firstColumnForHCondition, 0)
            .getHeight()) {
            final DecisionTableHelper.NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter = new DecisionTableHelper.NumberOfColumnsUnderTitleCounter(
                tableBody,
                firstColumnHeight);
            int i = firstColumnForHCondition;
            while (i < tableBody.getSource().getWidth()) {
                int c = numberOfColumnsUnderTitleCounter.get(i);
                if (c > 1) {
                    return true;
                }
                i = i + tableBody.getSource().getCell(i, 0).getWidth();
            }
        }
        return false;
    }

    private Direction detectTableDirection(TableSyntaxNode tableSyntaxNode) {
        Direction direction = Direction.UNKNOWN;
        if (isSmart(tableSyntaxNode)) {
            ILogicalTable tableBody = tableSyntaxNode.getTableBody();
            if (tableBody != null && isLookup(tableSyntaxNode)) {
                if (isLookupByHConditions(tableBody)) {
                    direction = Direction.NORMAL;
                }
                if (isLookupByHConditions(tableBody.transpose())) {
                    if (Direction.UNKNOWN.equals(direction)) {
                        direction = Direction.TRANSPOSED;
                    } else {
                        direction = Direction.UNKNOWN;
                    }
                }
            }
        }
        return direction;
    }

    public void loadAndBind(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            OpenL openl,
            ModuleOpenClass module,
            IBindingContext bindingContext) throws Exception {
        ILogicalTable tableBody = tableSyntaxNode.getTableBody();
        int height = tableBody == null ? 0 : tableBody.getHeight();
        int width = tableBody == null ? 0 : tableBody.getWidth();
        Direction direction = detectTableDirection(tableSyntaxNode);
        boolean f = width > height && width >= MAX_COLUMNS_IN_DT;
        if (Direction.TRANSPOSED.equals(direction)) {
            f = true;
        } else if (Direction.NORMAL.equals(direction)) {
            f = false;
        }
        final boolean firstTransposedThenNormal = f;
        CompilationErrors loadAndBindErrors = compileAndRevertIfFails(tableSyntaxNode,
            () -> loadAndBind(tableSyntaxNode, decisionTable, openl, module, firstTransposedThenNormal, bindingContext),
            bindingContext);
        final DTInfo dtInfo = decisionTable.getDtInfo();
        if (loadAndBindErrors != null) {
            // If table have errors, try to compile transposed variant.
            // Note that compiling transposed table consumes memory twice and for big tables it does not make any sense
            // for smart tables
            if (Direction.UNKNOWN.equals(direction) && (tableBody == null || !isSmart(
                tableSyntaxNode) || (firstTransposedThenNormal ? width : height) <= MAX_COLUMNS_IN_DT)) {
                CompilationErrors altLoadAndBindErrors = compileAndRevertIfFails(tableSyntaxNode,
                    () -> loadAndBind(tableSyntaxNode,
                        decisionTable,
                        openl,
                        module,
                        !firstTransposedThenNormal,
                        bindingContext),
                    bindingContext);
                if (altLoadAndBindErrors == null) {
                    return;
                } else {
                    if (tableBody == null || isSmart(tableSyntaxNode) || isSimple(tableSyntaxNode)) {
                        // Select compilation with less errors count for smart tables
                        if (isNotUnmatchedTableError(
                            altLoadAndBindErrors) && loadAndBindErrors.getBindingSyntaxNodeException()
                                .size() > altLoadAndBindErrors.getBindingSyntaxNodeException().size()) {
                            putTableForBusinessView(tableSyntaxNode, !firstTransposedThenNormal);
                            altLoadAndBindErrors.apply(tableSyntaxNode, bindingContext);
                            if (altLoadAndBindErrors.getEx() != null) {
                                throw altLoadAndBindErrors.getEx();
                            }
                            return;
                        }
                    } else {
                        // Try to analyze what errors is better to use based on table headers
                        if (!firstTransposedThenNormal && looksLikeVertical(
                            tableBody) || firstTransposedThenNormal && looksLikeHorizontal(tableBody)) {
                            putTableForBusinessView(tableSyntaxNode, !firstTransposedThenNormal);
                            altLoadAndBindErrors.apply(tableSyntaxNode, bindingContext);
                            if (altLoadAndBindErrors.getEx() != null) {
                                throw altLoadAndBindErrors.getEx();
                            }
                            return;
                        }
                    }
                }
                decisionTable.setDtInfo(dtInfo);
            }
            putTableForBusinessView(tableSyntaxNode, firstTransposedThenNormal);
            loadAndBindErrors.apply(tableSyntaxNode, bindingContext);
            if (loadAndBindErrors.getEx() != null) {
                throw loadAndBindErrors.getEx();
            }
        }
    }

    private boolean isNotUnmatchedTableError(CompilationErrors altLoadAndBindErrors) {
        Throwable ex = altLoadAndBindErrors.getEx();
        if (ex != null) {
            if (ex instanceof SyntaxNodeException) {
                ex = ex.getCause();
            }
            return !(ex instanceof DTUnmatchedCompilationException);
        }
        return true;
    }

    private static boolean looksLikeHorizontal(ILogicalTable table) {
        if (table.getWidth() < IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            return true;
        }

        if (table.getHeight() < IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            return false;
        }

        int validCnt1 = countValidHeaders(table);
        int validCnt2 = countValidHeaders(table.transpose());

        int invalidCnt1 = table.getWidth() - validCnt1 - countEmptyHeaders(table);
        int invalidCnt2 = table.getHeight() - validCnt2 - countEmptyHeaders(table.transpose());

        if (invalidCnt1 == invalidCnt2) {
            if (validCnt1 != validCnt2) {
                return validCnt1 > validCnt2;
            }
            return table.getWidth() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER;
        } else {
            return invalidCnt1 < invalidCnt2;
        }
    }

    private static boolean looksLikeVertical(ILogicalTable table) {
        if (table.getHeight() < IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            return true;
        }

        if (table.getWidth() < IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            return false;
        }

        int validCnt1 = countValidHeaders(table);
        int validCnt2 = countValidHeaders(table.transpose());

        int invalidCnt1 = table.getWidth() - validCnt1 - countEmptyHeaders(table);
        int invalidCnt2 = table.getHeight() - validCnt2 - countEmptyHeaders(table.transpose());

        if (invalidCnt1 == invalidCnt2) {
            if (validCnt1 != validCnt2) {
                return validCnt1 < validCnt2;
            }
            return table.getHeight() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER;
        } else {
            return invalidCnt1 > invalidCnt2;
        }
    }

    private static int countValidHeaders(ILogicalTable table) {
        int width = table.getWidth();
        int count = 0;
        for (int i = 0; i < width; i++) {
            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();
            if (value != null) {
                value = value.toUpperCase();
                count += DecisionTableHelper.isValidConditionHeader(value) || DecisionTableHelper
                    .isValidActionHeader(value) || DecisionTableHelper.isValidRetHeader(value) || DecisionTableHelper
                        .isValidCRetHeader(value) || DecisionTableHelper.isValidKeyHeader(value) ? 1 : 0;
            }
        }
        return count;
    }

    private static int countEmptyHeaders(ILogicalTable table) {
        int width = table.getWidth();
        int count = 0;
        for (int i = 0; i < width; i++) {
            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();
            if (StringUtils.isBlank(value)) {
                count++;
            }
        }
        return count;
    }

    private static void validateCollectSyntaxNode(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            IBindingContext bindingContext) throws SyntaxNodeException {
        int parametersCount = tableSyntaxNode.getHeader().getCollectParameters().length;
        IOpenClass type = decisionTable.getType();
        if ((type.isArray() || ClassUtils.isAssignable(type.getInstanceClass(),
            Collection.class)) && parametersCount > 1) {
            throw SyntaxNodeExceptionUtils
                .createError(String.format("Expected exactly one parameter for return type '%s'.",
                    type.getComponentClass().getDisplayName(0)), tableSyntaxNode.getHeader().getCellSource());
        }
        if (ClassUtils.isAssignable(type.getInstanceClass(), Map.class)) {
            if (parametersCount != 2) {
                throw SyntaxNodeExceptionUtils
                    .createError(String.format("Expected two parameters for return type '%s'.",
                        type.getComponentClass().getDisplayName(0)), tableSyntaxNode.getHeader().getCellSource());
            }
        }
        for (String parameterType : tableSyntaxNode.getHeader().getCollectParameters()) {
            IOpenClass t = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, parameterType);
            if (t == null) {
                throw SyntaxNodeExceptionUtils.createError(String.format("Type '%s' is not found.", parameterType),
                    tableSyntaxNode.getHeader().getCellSource());
            } else {
                if (type.isArray() && bindingContext.getCast(t, type.getComponentClass()) == null) {
                    throw SyntaxNodeExceptionUtils.createError(String.format("Incompatible types: '%s' and '%s'.",
                        type.getComponentClass().getDisplayName(0),
                        t.getDisplayName(0)), tableSyntaxNode.getHeader().getCellSource());
                }
            }
        }
    }

    private TableStructure loadTableStructure(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            boolean transpose,
            IBindingContext bindingContext) throws SyntaxNodeException {

        if (DecisionTableHelper.isCollect(tableSyntaxNode)) {
            validateCollectSyntaxNode(tableSyntaxNode, decisionTable, bindingContext);
        }

        ILogicalTable tableBody = tableSyntaxNode.getTableBody();

        if (tableBody == null) {
            throw SyntaxNodeExceptionUtils.createError(EMPTY_BODY, tableSyntaxNode);
        }
        if (transpose) {
            tableBody = tableBody.transpose();
        }
        // preprocess decision tables (without conditions and return headers)
        // add virtual headers to the table body.
        //
        if (DecisionTableHelper.isSmartDecisionTable(tableSyntaxNode) || DecisionTableHelper
            .isSimpleDecisionTable(tableSyntaxNode) || DecisionTableHelper
                .isSimpleLookupTable(tableSyntaxNode) || DecisionTableHelper.isSmartLookupTable(tableSyntaxNode)) {
            try {
                tableBody = DecisionTableHelper
                    .preprocessDecisionTableWithoutHeaders(tableSyntaxNode, decisionTable, tableBody, bindingContext);
            } catch (OpenLCompilationException e) {
                throw SyntaxNodeExceptionUtils.createError(
                    "Cannot create a header for a Simple Rules, Lookup Table or Smart Table.",
                    e,
                    tableSyntaxNode);
            }
        }
        int height = tableBody.getHeight();
        if (height < IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            throw SyntaxNodeExceptionUtils.createError("Invalid structure of decision table.", tableSyntaxNode);
        }
        if (height == IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            bindingContext.addMessage(OpenLMessagesUtils
                .newWarnMessage("There are no rule rows in the table.", tableSyntaxNode));
        }
        ILogicalTable toParse = tableBody;

        // process lookup decision table.
        //
        int nHConditions = DecisionTableHelper.countHConditionsByHeaders(toParse);
        int nVConditions = DecisionTableHelper.countVConditionsByHeaders(toParse);
        TableStructure tableStructure = new TableStructure();
        if (nHConditions > 0 && height > IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            try {
                DecisionTableLookupConvertor dtlc = new DecisionTableLookupConvertor();
                IGridTable convertedTable = dtlc.convertTable(toParse);
                toParse = LogicalTableHelper.logicalTable(convertedTable);
                tableStructure.info = new DTInfo(nHConditions, nVConditions, dtlc.getScale());
            } catch (OpenLCompilationException e) {
                throw SyntaxNodeExceptionUtils.createError(e, tableSyntaxNode);
            } catch (Exception e) {
                throw SyntaxNodeExceptionUtils.createError("Cannot convert table.", e, tableSyntaxNode);
            }
        }
        toParse = toParse.transpose();

        if (needToUnmergeFirstRow(toParse)) {
            toParse = unmergeFirstRow(toParse);
        }

        if (tableStructure.info == null) {
            tableStructure.info = new DTInfo(nHConditions, nVConditions);
        }
        decisionTable.setDtInfo(tableStructure.info);
        tableStructure.columnsNumber = toParse.getWidth() - IDecisionTableConstants.SERVICE_COLUMNS_NUMBER;

        // NOTE! this method call depends on upper stacks calls, don`t move it upper.
        //
        putTableForBusinessView(tableSyntaxNode, transpose);
        for (int i = 0; i < toParse.getHeight(); i++) {
            loadRow(decisionTable, tableStructure, toParse, i, bindingContext);
        }

        validateMapReturnType(tableSyntaxNode, decisionTable, tableStructure);
        return tableStructure;
    }

    private static class CompilationErrors {
        private final List<SyntaxNodeException> bindingSyntaxNodeException;
        private final Collection<OpenLMessage> openLMessages;
        private final Exception ex;
        private final DecisionTableMetaInfoReader.MetaInfoHolder metaInfos;

        private CompilationErrors(List<SyntaxNodeException> bindingSyntaxNodeException,
                Collection<OpenLMessage> openLMessages,
                DecisionTableMetaInfoReader.MetaInfoHolder metaInfos,
                Exception ex) {
            this.bindingSyntaxNodeException = Objects.requireNonNull(bindingSyntaxNodeException,
                "bindingSyntaxNodeException cannot be null");
            this.openLMessages = Objects.requireNonNull(openLMessages, "openLMessages cannot be null");
            this.metaInfos = metaInfos;
            this.ex = ex;
        }

        private void apply(TableSyntaxNode tableSyntaxNode, IBindingContext bindingContext) {
            bindingSyntaxNodeException.forEach(bindingContext::addError);
            openLMessages.forEach(bindingContext::addMessage);
            if (!bindingContext.isExecutionMode()) {
                DecisionTableMetaInfoReader decisionTableMetaInfoReader = (DecisionTableMetaInfoReader) tableSyntaxNode
                    .getMetaInfoReader();
                decisionTableMetaInfoReader.getMetaInfos().merge(metaInfos);
            }
        }

        public Exception getEx() {
            return ex;
        }

        public List<SyntaxNodeException> getBindingSyntaxNodeException() {
            return bindingSyntaxNodeException;
        }
    }

    @FunctionalInterface
    private interface Supplier {
        void get() throws Exception;
    }

    private CompilationErrors compileAndRevertIfFails(TableSyntaxNode tableSyntaxNode,
            Supplier supplier,
            IBindingContext bindingContext) {
        DecisionTableMetaInfoReader decisionTableMetaInfoReader = null;
        if (!bindingContext.isExecutionMode()) {
            decisionTableMetaInfoReader = (DecisionTableMetaInfoReader) tableSyntaxNode.getMetaInfoReader();
            decisionTableMetaInfoReader.pushMetaInfos();
        }
        bindingContext.pushErrors();
        bindingContext.pushMessages();
        Exception ex = null;
        try {
            supplier.get();
        } catch (Exception e) {
            ex = e;
        }
        List<SyntaxNodeException> errors = bindingContext.popErrors();
        Collection<OpenLMessage> messages = bindingContext.popMessages();
        DecisionTableMetaInfoReader.MetaInfoHolder metaInfos = null;
        if (decisionTableMetaInfoReader != null) {
            metaInfos = decisionTableMetaInfoReader.popMetaInfos();
        }
        if (errors.isEmpty() && ex == null) {
            messages.forEach(bindingContext::addMessage);
            if (decisionTableMetaInfoReader != null) {
                decisionTableMetaInfoReader.getMetaInfos().merge(metaInfos);
            }
            return null;
        }
        return new CompilationErrors(errors, messages, metaInfos, ex);
    }

    private void validateMapReturnType(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            TableStructure tableStructure) throws SyntaxNodeException {
        if (ClassUtils.isAssignable(decisionTable.getType().getInstanceClass(), Map.class)) {
            if (tableStructure.hasCollectReturnAction && !tableStructure.hasCollectReturnKeyAction) {
                throw SyntaxNodeExceptionUtils.createError(
                    "Invalid Decision Table headers: At least one KEY header is required.",
                    tableSyntaxNode);
            }
            if (tableStructure.hasCollectReturnKeyAction && !tableStructure.hasCollectReturnAction) {
                throw SyntaxNodeExceptionUtils.createError(
                    "Invalid Decision Table headers: At least one CRET header is required.",
                    tableSyntaxNode);
            }
        }
    }

    private ILogicalTable unmergeFirstRow(ILogicalTable toParse) {

        return LogicalTableHelper
            .unmergeColumns(toParse, IDecisionTableConstants.SERVICE_COLUMNS_NUMBER, toParse.getWidth());
    }

    private boolean needToUnmergeFirstRow(ILogicalTable toParse) {
        String header = getHeaderStr(0, toParse);

        return DecisionTableHelper
            .isConditionHeader(header) && !DecisionTableHelper.isValidMergedConditionHeader(header);
    }

    /**
     * Put subtable, that will be displayed at the business view.<br>
     * It must be without method header, properties section, conditions and return headers.
     *
     * @param tableSyntaxNode table syntax node
     */
    @SuppressWarnings("StatementWithEmptyBody")
    private void putTableForBusinessView(TableSyntaxNode tableSyntaxNode, boolean transpose) {
        ILogicalTable tableBody = tableSyntaxNode.getTableBody();
        if (tableBody == null) {
            return;
        }

        if (DecisionTableHelper.isSmartDecisionTable(tableSyntaxNode) || DecisionTableHelper
            .isSimpleDecisionTable(tableSyntaxNode) || DecisionTableHelper
                .isSimpleLookupTable(tableSyntaxNode) || DecisionTableHelper.isSmartLookupTable(tableSyntaxNode)) {
            // if DT is simple, its body does not contain conditions and return headers.
            // so put the body as it is.
        } else if (transpose) {
            // table is horizontal, so remove service columns.
            tableBody = tableBody.getColumns(IDecisionTableConstants.SERVICE_COLUMNS_NUMBER - 1);
        } else {
            // if table is vertical, remove service rows.
            tableBody = tableBody.getRows(IDecisionTableConstants.SERVICE_COLUMNS_NUMBER - 1);
        }

        tableSyntaxNode.getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tableBody);
    }

    private String getHeaderStr(int row, ILogicalTable table) {
        String headerStr = table.getRow(row)
            .getSource()
            .getCell(IDecisionTableConstants.INFO_COLUMN_INDEX, 0)
            .getStringValue();

        if (headerStr == null) {
            return StringUtils.EMPTY;
        }

        return headerStr.toUpperCase();
    }

    private void loadRow(DecisionTable decisionTable,
            TableStructure tableStructure,
            ILogicalTable table,
            int row,
            IBindingContext bindingContext) throws SyntaxNodeException {

        String header = getHeaderStr(row, table);

        if (DecisionTableHelper.isConditionHeader(header)) {
            tableStructure.conditions.add(new Condition(header, row, table, getConditionScale(tableStructure, header)));
        } else if (DecisionTableHelper.isValidActionHeader(header)) {
            tableStructure.actions
                .add(new Action(header, row, table, ActionType.ACTION, DTScale.getStandardScale(), decisionTable));
        } else if (DecisionTableHelper.isValidRuleHeader(header)) {
            addRule(row, table, tableStructure, bindingContext);
        } else if (DecisionTableHelper.isValidKeyHeader(header)) {
            tableStructure.actions.add(new Action(header,
                row,
                table,
                ActionType.COLLECT_RETURN_KEY,
                DTScale.getStandardScale(),
                decisionTable));
            tableStructure.hasCollectReturnKeyAction = true;
        } else if (DecisionTableHelper.isValidRetHeader(header)) {
            if (tableStructure.hasCollectReturnAction) {
                throw SyntaxNodeExceptionUtils.createError(
                    String.format("Invalid Decision Table header '%s'. Headers '%s' and '%s' cannot be used together.",
                        header,
                        tableStructure.firstUsedReturnActionHeader,
                        header),
                    new GridCellSourceCodeModule(table.getRow(row).getSource(),
                        IDecisionTableConstants.INFO_COLUMN_INDEX,
                        0,
                        bindingContext));
            }
            tableStructure.actions
                .add(new Action(header, row, table, ActionType.RETURN, DTScale.getStandardScale(), decisionTable));
            if (tableStructure.firstUsedReturnActionHeader == null) {
                tableStructure.firstUsedReturnActionHeader = header;
            }
            tableStructure.hasReturnAction = true;
        } else if (DecisionTableHelper.isValidCRetHeader(header)) {
            if (tableStructure.hasReturnAction) {
                throw SyntaxNodeExceptionUtils.createError(
                    String.format("Invalid Decision Table header '%s'. Headers '%s' and '%s' cannot be used together.",
                        header,
                        tableStructure.firstUsedReturnActionHeader,
                        header),
                    new GridCellSourceCodeModule(table.getRow(row).getSource(),
                        IDecisionTableConstants.INFO_COLUMN_INDEX,
                        0,
                        bindingContext));
            }
            tableStructure.hasCollectReturnAction = true;
            if (tableStructure.firstUsedReturnActionHeader == null) {
                tableStructure.firstUsedReturnActionHeader = header;
            }
            if (validateCollectReturnType(decisionTable)) {
                tableStructure.actions.add(new Action(header,
                    row,
                    table,
                    ActionType.COLLECT_RETURN,
                    DTScale.getStandardScale(),
                    decisionTable));
            } else {
                if (isSmart(decisionTable.getSyntaxNode()) || isSimple(decisionTable.getSyntaxNode())) {
                    boolean isMap = decisionTable.getSyntaxNode().getHeader().getCollectParameters().length > 0;
                    final String errorMsg = String.format(
                        "Decision table return type '%s' is incompatible with keyword 'Collect' in the table header, expected %s.",
                        decisionTable.getType().getName(),
                        isMap ? "a map" : "an array or a collection");
                    throw SyntaxNodeExceptionUtils.createError(errorMsg, decisionTable.getSyntaxNode());
                } else {
                    throw SyntaxNodeExceptionUtils.createError(
                        String.format("Decision table return type '%s' is incompatible with column header '%s'.",
                            decisionTable.getType().getName(),
                            header),
                        new GridCellSourceCodeModule(table.getRow(row).getSource(),
                            IDecisionTableConstants.INFO_COLUMN_INDEX,
                            0,
                            bindingContext));
                }
            }
        } else if (!ParserUtils.isBlankOrCommented(header)) {
            throw SyntaxNodeExceptionUtils.createError(String.format("Invalid Decision Table header '%s'.", header),
                new GridCellSourceCodeModule(table.getRow(row).getSource(),
                    IDecisionTableConstants.INFO_COLUMN_INDEX,
                    0,
                    bindingContext));
        }
    }

    private boolean validateCollectReturnType(DecisionTable decisionTable) {
        IOpenClass type = decisionTable.getType();

        if (type.isArray()) {
            return true;
        }
        if (ClassUtils.isAssignable(type.getInstanceClass(), Collection.class)) {
            return true;
        }
        return ClassUtils.isAssignable(type.getInstanceClass(), Map.class);

    }

}