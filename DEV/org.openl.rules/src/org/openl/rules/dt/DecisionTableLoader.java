package org.openl.rules.dt;

import static org.openl.rules.dt.DecisionTableHelper.isSimple;
import static org.openl.rules.dt.DecisionTableHelper.isSmart;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessage;
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
import org.openl.rules.utils.ParserUtils;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.util.ClassUtils;

/**
 * @author snshor
 *
 */
public class DecisionTableLoader {

    /**
     * protected modified is for tests access.
     */
    static final String EMPTY_BODY = "Decision table must contain body section.";

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
            boolean transponse,
            IBindingContext bindingContext) throws Exception {
        TableStructure tableStructure = loadTableStructure(tableSyntaxNode, decisionTable, transponse, bindingContext);
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

    public void loadAndBind(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            OpenL openl,
            ModuleOpenClass module,
            IBindingContext bindingContext) throws Exception {
        CompilationErrors loadAndBindErrors = compileAndRevertIfFails(tableSyntaxNode, () -> {
            loadAndBind(tableSyntaxNode, decisionTable, openl, module, false, bindingContext);
            return null;
        }, bindingContext);
        final DTInfo dtInfo = decisionTable.getDtInfo();
        if (loadAndBindErrors != null) {
            if (DecisionTableHelper.isSimple(tableSyntaxNode) || DecisionTableHelper.isSmart(tableSyntaxNode)) {
                CompilationErrors transponsedLoadAndBindErrors = compileAndRevertIfFails(tableSyntaxNode, () -> {
                    loadAndBind(tableSyntaxNode, decisionTable, openl, module, true, bindingContext);
                    return null;
                }, bindingContext);
                if (transponsedLoadAndBindErrors == null) {
                    return;
                } else {
                    // Select compilation with less errors count
                    if (loadAndBindErrors.getBindingSyntaxNodeException().size() > transponsedLoadAndBindErrors
                        .getBindingSyntaxNodeException()
                        .size() || loadAndBindErrors.getSyntaxNodeExceptions().length > transponsedLoadAndBindErrors
                            .getSyntaxNodeExceptions().length && loadAndBindErrors.getBindingSyntaxNodeException()
                                .size() == transponsedLoadAndBindErrors.getBindingSyntaxNodeException().size()) {
                        transponsedLoadAndBindErrors.apply(tableSyntaxNode, bindingContext);
                        if (transponsedLoadAndBindErrors.getEx() != null) {
                            throw transponsedLoadAndBindErrors.getEx();
                        }
                        return;
                    }
                }
                decisionTable.setDtInfo(dtInfo);
            }
            loadAndBindErrors.apply(tableSyntaxNode, bindingContext);
            if (loadAndBindErrors.getEx() != null) {
                throw loadAndBindErrors.getEx();
            }
        }
    }

    private TableStructure loadTableStructure(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            boolean transponse,
            IBindingContext bindingContext) throws SyntaxNodeException {

        ILogicalTable tableBody = tableSyntaxNode.getTableBody();

        if (tableBody == null) {
            throw SyntaxNodeExceptionUtils.createError(EMPTY_BODY, tableSyntaxNode);
        }
        if (transponse) {
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
        ILogicalTable toParse = tableBody;

        // process lookup decision table.
        //

        int nHConditions = DecisionTableHelper.countHConditionsByHeaders(toParse);
        int nVConditions = DecisionTableHelper.countVConditionsByHeaders(toParse);
        TableStructure tableStructure = new TableStructure();
        if (nHConditions > 0) {
            try {
                DecisionTableLookupConvertor dtlc = new DecisionTableLookupConvertor();

                IGridTable convertedTable = dtlc.convertTable(toParse);
                ILogicalTable offsetConvertedTable = LogicalTableHelper.logicalTable(convertedTable);
                toParse = offsetConvertedTable.transpose();
                tableStructure.info = new DTInfo(nHConditions, nVConditions, dtlc.getScale());
            } catch (OpenLCompilationException e) {
                throw SyntaxNodeExceptionUtils.createError(e, tableSyntaxNode);
            } catch (Exception e) {
                throw SyntaxNodeExceptionUtils.createError("Cannot convert table.", e, tableSyntaxNode);
            }
        } else if (DecisionTableHelper.looksLikeVertical(toParse)) {
            // parsing is based on horizontal representation of decision table.
            //
            toParse = toParse.transpose();
        }

        if (needToUnmergeFirstRow(toParse)) {
            toParse = unmergeFirstRow(toParse);
        }

        if (tableStructure.info == null) {
            tableStructure.info = new DTInfo(nHConditions, nVConditions);
        }
        decisionTable.setDtInfo(tableStructure.info);

        if (toParse.getWidth() < IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            throw SyntaxNodeExceptionUtils.createError("Invalid structure of decision table.", tableSyntaxNode);
        }
        tableStructure.columnsNumber = toParse.getWidth() - IDecisionTableConstants.SERVICE_COLUMNS_NUMBER;

        // NOTE! this method call depends on upper stacks calls, don`t move it upper.
        //
        putTableForBusinessView(tableSyntaxNode);
        for (int i = 0; i < toParse.getHeight(); i++) {
            loadRow(decisionTable, tableStructure, toParse, i, bindingContext);
        }

        validateMapReturnType(tableSyntaxNode, decisionTable, tableStructure);
        return tableStructure;
    }

    private static class CompilationErrors {
        private SyntaxNodeException[] syntaxNodeExceptions;
        private List<SyntaxNodeException> bindingSyntaxNodeException;
        private Collection<OpenLMessage> openLMessages;
        private Exception ex;
        private DecisionTableMetaInfoReader.MetaInfoHolder metaInfos;

        private CompilationErrors(SyntaxNodeException[] syntaxNodeExceptions,
                List<SyntaxNodeException> bindingSyntaxNodeException,
                Collection<OpenLMessage> openLMessages,
                DecisionTableMetaInfoReader.MetaInfoHolder metaInfos,
                Exception ex) {
            this.syntaxNodeExceptions = Objects.requireNonNull(syntaxNodeExceptions,
                "syntaxNodeExceptions cannot be null");
            this.bindingSyntaxNodeException = Objects.requireNonNull(bindingSyntaxNodeException,
                "bindingSyntaxNodeException cannot be null");
            this.openLMessages = Objects.requireNonNull(openLMessages, "openLMessages cannot be null");
            this.metaInfos = metaInfos;
            this.ex = ex;
        }

        private void apply(TableSyntaxNode tableSyntaxNode, IBindingContext bindingContext) {
            bindingSyntaxNodeException.forEach(bindingContext::addError);
            openLMessages.forEach(bindingContext::addMessage);
            Arrays.stream(syntaxNodeExceptions).forEach(tableSyntaxNode::addError);
            if (!bindingContext.isExecutionMode()) {
                DecisionTableMetaInfoReader decisionTableMetaInfoReader = (DecisionTableMetaInfoReader) tableSyntaxNode
                    .getMetaInfoReader();
                decisionTableMetaInfoReader.getMetaInfos().merge(metaInfos);
            }
        }

        public Exception getEx() {
            return ex;
        }

        public SyntaxNodeException[] getSyntaxNodeExceptions() {
            return syntaxNodeExceptions;
        }

        public List<SyntaxNodeException> getBindingSyntaxNodeException() {
            return bindingSyntaxNodeException;
        }
    }

    @FunctionalInterface
    private interface Supplier<T> {
        T get() throws Exception;
    }

    private CompilationErrors compileAndRevertIfFails(TableSyntaxNode tableSyntaxNode,
            Supplier<T> supplier,
            IBindingContext bindingContext) {
        SyntaxNodeException[] syntaxNodeExceptions = tableSyntaxNode.getErrors();
        DecisionTableMetaInfoReader decisionTableMetaInfoReader = null;
        if (!bindingContext.isExecutionMode()) {
            decisionTableMetaInfoReader = (DecisionTableMetaInfoReader) tableSyntaxNode.getMetaInfoReader();
            decisionTableMetaInfoReader.pushMetaInfos();
        }
        tableSyntaxNode.clearErrors();
        bindingContext.pushErrors();
        bindingContext.pushMessages();
        try {
            supplier.get();
            SyntaxNodeException[] newSyntaxNodeExceptions = tableSyntaxNode.getErrors();
            tableSyntaxNode.clearErrors();
            Arrays.stream(syntaxNodeExceptions).forEach(tableSyntaxNode::addError);
            if (bindingContext.getErrors().length == 0 && !tableSyntaxNode.hasErrors()) {
                bindingContext.popErrors().forEach(bindingContext::addError);
                bindingContext.popMessages().forEach(bindingContext::addMessage);
                Arrays.stream(newSyntaxNodeExceptions).forEach(tableSyntaxNode::addError);
                if (decisionTableMetaInfoReader != null) {
                    DecisionTableMetaInfoReader.MetaInfoHolder metaInfos = decisionTableMetaInfoReader.popMetaInfos();
                    decisionTableMetaInfoReader.getMetaInfos().merge(metaInfos);
                }
                return null;
            } else {
                return new CompilationErrors(newSyntaxNodeExceptions,
                    bindingContext.popErrors(),
                    bindingContext.popMessages(),
                    decisionTableMetaInfoReader != null ? decisionTableMetaInfoReader.popMetaInfos() : null,
                    null);
            }
        } catch (Exception e) {
            SyntaxNodeException[] newSyntaxNodeExceptions = tableSyntaxNode.getErrors();
            tableSyntaxNode.clearErrors();
            Arrays.stream(syntaxNodeExceptions).forEach(tableSyntaxNode::addError);
            return new CompilationErrors(newSyntaxNodeExceptions,
                bindingContext.popErrors(),
                bindingContext.popMessages(),
                decisionTableMetaInfoReader != null ? decisionTableMetaInfoReader.popMetaInfos() : null,
                e);
        }
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
    private void putTableForBusinessView(TableSyntaxNode tableSyntaxNode) {
        ILogicalTable tableBody = tableSyntaxNode.getTableBody();

        if (DecisionTableHelper.isSmartDecisionTable(tableSyntaxNode) || DecisionTableHelper
            .isSimpleDecisionTable(tableSyntaxNode) || DecisionTableHelper
                .isSimpleLookupTable(tableSyntaxNode) || DecisionTableHelper.isSmartLookupTable(tableSyntaxNode)) {
            // if DT is simple, its body does not contain conditions and return headers.
            // so put the body as it is.
            tableSyntaxNode.getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tableBody);
        } else {
            // need to get the subtable without conditions and return headers.
            ILogicalTable businessView;
            if (DecisionTableHelper.looksLikeVertical(tableBody)) {
                // if table is vertical, remove service rows.
                businessView = tableBody.getRows(IDecisionTableConstants.SERVICE_COLUMNS_NUMBER - 1);
            } else {
                // table is horizontal, so remove service columns.
                businessView = tableBody.getColumns(IDecisionTableConstants.SERVICE_COLUMNS_NUMBER - 1);
            }

            tableSyntaxNode.getSubTables().put(IXlsTableNames.VIEW_BUSINESS, businessView);
        }

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
            tableStructure.actions.add(new Action(header, row, table, ActionType.ACTION, DTScale.getStandardScale()));
        } else if (DecisionTableHelper.isValidRuleHeader(header)) {
            addRule(row, table, tableStructure, bindingContext);
        } else if (DecisionTableHelper.isValidKeyHeader(header)) {
            tableStructure.actions
                .add(new Action(header, row, table, ActionType.COLLECT_RETURN_KEY, DTScale.getStandardScale()));
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
            tableStructure.actions.add(new Action(header, row, table, ActionType.RETURN, DTScale.getStandardScale()));
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
                tableStructure.actions
                    .add(new Action(header, row, table, ActionType.COLLECT_RETURN, DTScale.getStandardScale()));
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