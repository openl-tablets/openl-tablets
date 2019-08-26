package org.openl.rules.calc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.engine.OpenLSystemProperties;
import org.openl.message.OpenLMessagesUtils;
import org.openl.meta.TableMetaInfo;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.SpreadsheetMetaInfoReader;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ILogicalTable;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.java.JavaOpenClass;

// TODO: refactor
// Extract all the binding and build code to the SpreadsheetBinder
public class SpreadsheetBoundNode extends AMethodBasedNode implements IMemberBoundNode {

    private SpreadsheetStructureBuilder structureBuilder;
    private SpreadsheetComponentsBuilder componentsBuilder;
    private SpreadsheetOpenClass spreadsheetOpenClass;
    private SpreadsheetCell[][] cells;

    IBindingContext bindingContext;

    public SpreadsheetBoundNode(TableSyntaxNode tableSyntaxNode,
            OpenL openl,
            IOpenMethodHeader header,
            XlsModuleOpenClass module) {

        super(tableSyntaxNode, openl, header, module);
    }

    @Override
    public XlsModuleOpenClass getModule() {
        return (XlsModuleOpenClass) super.getModule();
    }

    private CustomSpreadsheetResultOpenClass buildCustomSpreadsheetResultType(Spreadsheet spreadsheet) {
        if (spreadsheet.isCustomSpreadsheetType()) {
            Map<String, IOpenField> spreadsheetOpenClassFields = spreadsheet.getSpreadsheetType().getFields();
            spreadsheetOpenClassFields.remove("this");
            String typeName = Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + spreadsheet.getName();

            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = new CustomSpreadsheetResultOpenClass(
                typeName,
                spreadsheet.getRowNames(),
                spreadsheet.getColumnNames(),
                spreadsheet.getRowNamesMarkedWithAsterisk(),
                spreadsheet.getColumnNamesMarkedWithAsterisk(),
                spreadsheet.getRowTitles(),
                spreadsheet.getColumnTitles(),
                getModule());

            customSpreadsheetResultOpenClass
                .setMetaInfo(new TableMetaInfo("Spreadsheet", spreadsheet.getName(), spreadsheet.getSourceUrl()));

            for (IOpenField field : spreadsheetOpenClassFields.values()) {
                CustomSpreadsheetResultField customSpreadsheetResultField = new CustomSpreadsheetResultField(
                    customSpreadsheetResultOpenClass,
                    field.getName(),
                    field.getType());
                customSpreadsheetResultOpenClass.addField(customSpreadsheetResultField);
            }
            return customSpreadsheetResultOpenClass;
        }
        return null;
    }

    @Override
    protected ExecutableRulesMethod createMethodShell() {
        Spreadsheet spreadsheet;
        if (componentsBuilder.isExistsReturnHeader()) {
            spreadsheet = new Spreadsheet(getHeader(), this, false);
        } else {
            /*
             * We need to generate a customSpreadsheet class only if return type of the spreadsheet is SpreadsheetResult
             * and the customspreadsheet property is true
             */
            boolean isCustomSpreadsheetType = SpreadsheetResult.class.equals(getType()
                .getInstanceClass()) && (!(getType() instanceof CustomSpreadsheetResultOpenClass)) && OpenLSystemProperties
                    .isCustomSpreadsheetType(bindingContext.getExternalParams());

            spreadsheet = new Spreadsheet(getHeader(), this, isCustomSpreadsheetType);
        }
        spreadsheet.setSpreadsheetType(spreadsheetOpenClass);
        // As custom spreadsheet result is being generated at runtime,
        // call this method to ensure that CSR will be generated during the
        // compilation.
        // Add generated type to be accessible through binding context.
        //
        spreadsheet.setRowNames(componentsBuilder.getRowNames());
        spreadsheet.setColumnNames(componentsBuilder.getColumnNames());

        spreadsheet.setRowNamesMarkedWithAsterisk(componentsBuilder.getRowNamesMarkedWithAsterisk());
        spreadsheet.setColumnNamesMarkedWithAsterisk(componentsBuilder.getColumnNamesMarkedWithAsterisk());

        spreadsheet.setRowTitles(componentsBuilder.getCellsHeadersExtractor().getRowNames());
        spreadsheet.setColumnTitles(componentsBuilder.getCellsHeadersExtractor().getColumnNames());

        validateWarnsRowsColumnsWithAsterisks(spreadsheet);

        if (spreadsheet.isCustomSpreadsheetType()) {
            IOpenClass type = null;
            try {
                type = buildCustomSpreadsheetResultType(spreadsheet); // Can throw RuntimeException
                bindingContext.addType(ISyntaxConstants.THIS_NAMESPACE, type);
                IOpenClass bindingContextType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE,
                    type.getName());
                spreadsheet.setCustomSpreadsheetResultType((CustomSpreadsheetResultOpenClass) bindingContextType);
            } catch (Exception | LinkageError e) {
                String message = String.format("Can't define type %s", spreadsheet.getName());
                SyntaxNodeException exception = SyntaxNodeExceptionUtils.createError(message, e, getTableSyntaxNode());
                getTableSyntaxNode().addError(exception);
                bindingContext.addError(exception);
                spreadsheet.setCustomSpreadsheetResultType(
                    (CustomSpreadsheetResultOpenClass) bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE,
                        Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + spreadsheet.getName()));
            }
        }
        return spreadsheet;
    }

    public void validateWarnsRowsColumnsWithAsterisks(Spreadsheet spreadsheet) {
        long columnsWithAsteriskCount = Arrays.stream(spreadsheet.getColumnNamesMarkedWithAsterisk())
            .filter(Objects::nonNull)
            .count();
        long rowsWithAsteriskCount = Arrays.stream(spreadsheet.getRowNamesMarkedWithAsterisk())
            .filter(Objects::nonNull)
            .count();

        if (columnsWithAsteriskCount > 0 && rowsWithAsteriskCount == 0) {
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                "If columns are marked with asterisk symbols, then at least one row must be marked also, otherwise marked columns with asterisk symbols are ignored.",
                getTableSyntaxNode()));
        }
        if (rowsWithAsteriskCount > 0 && columnsWithAsteriskCount == 0) {
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                "If rows are marked with asterisk symbols, then at least one column must be marked also, otherwise marked rows with asterisk symbols are ignored.",
                getTableSyntaxNode()));
        }
        StringBuilder sb = new StringBuilder();
        Map<String, String> fNames = new HashMap<>();
        int warnCnt = 0;
        for (int i = 0; i < spreadsheet.getRowNamesMarkedWithAsterisk().length; i++) {
            for (int j = 0; j < spreadsheet.getColumnNamesMarkedWithAsterisk().length; j++) {
                if (spreadsheet.getColumnNamesMarkedWithAsterisk()[j] != null && spreadsheet
                    .getRowNamesMarkedWithAsterisk()[i] != null) {
                    String fieldName = SpreadsheetStructureBuilder.DOLLAR_SIGN + spreadsheet
                        .getColumnNames()[j] + SpreadsheetStructureBuilder.DOLLAR_SIGN + spreadsheet.getRowNames()[i];
                    IOpenField field = spreadsheet.getSpreadsheetType().getField(fieldName);
                    IOpenClass t = field.getType();
                    while (t.isArray()) {
                        t = t.getComponentClass();
                    }
                    boolean f = false;
                    if (t instanceof CustomSpreadsheetResultOpenClass) {
                        CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) t;
                        CustomSpreadsheetResultOpenClass csroc = (CustomSpreadsheetResultOpenClass) this.getModule()
                            .findType(customSpreadsheetResultOpenClass.getName());
                        if (csroc.isEmptyBeanClass()) {
                            f = true; // IGNORE EMPTY CSRS TYPES
                        }
                    } else if (JavaOpenClass.VOID.equals(t) || JavaOpenClass.CLS_VOID.equals(t)) {
                        f = true; // IGNORE VOID TYPES
                    }

                    String refName;
                    if (columnsWithAsteriskCount == 1) {
                        refName = SpreadsheetStructureBuilder.DOLLAR_SIGN + spreadsheet.getRowNames()[i];
                    } else if (rowsWithAsteriskCount == 1) {
                        refName = SpreadsheetStructureBuilder.DOLLAR_SIGN + spreadsheet.getColumnNames()[j];
                    } else {
                        refName = fieldName;
                    }

                    if (f) {
                        if (warnCnt > 0) {
                            sb.append(", ");
                        }
                        sb.append(refName);
                        warnCnt++;
                    } else {
                        StringBuilder sb1 = new StringBuilder();
                        if (columnsWithAsteriskCount == 1) {
                            sb1.append(spreadsheet.getRowNamesMarkedWithAsterisk()[i]);
                        } else if (rowsWithAsteriskCount == 1) {
                            sb1.append(spreadsheet.getColumnNamesMarkedWithAsterisk()[j]);
                        } else {
                            sb1.append(spreadsheet.getColumnNamesMarkedWithAsterisk()[j]);
                            sb1.append("_");
                            sb1.append(spreadsheet.getRowNamesMarkedWithAsterisk()[i]);
                        }
                        String fName = sb1.toString();
                        String key = fName.length() > 1 ? Character.toLowerCase(fName.charAt(0)) + fName.substring(1)
                                                        : fName.toLowerCase();
                        String v = fNames.put(key, refName);
                        if (v != null) {
                            getTableSyntaxNode().addError(SyntaxNodeExceptionUtils.createError(String.format(
                                "Cells '%s' and '%s' conflicts with each other in the output model for this spreadsheet result.",
                                v,
                                refName), getTableSyntaxNode()));
                        }
                    }
                }
            }
        }
        if (warnCnt == 1) {
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(String.format(
                "Spreadsheet cell '%s' is always empty. Using asterisk symbols on column/row for this cell makes output model excess.",
                sb.toString()), getTableSyntaxNode()));
        } else if (warnCnt > 1) {
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(String.format(
                "Spreadsheet cells [%s] are always empty. Using asterisk symbols on columns/rows for these cells makes output model excess.",
                sb.toString()), getTableSyntaxNode()));
        }
    }

    public void preBind(IBindingContext bindingContext) throws SyntaxNodeException {
        if (!bindingContext.isExecutionMode()) {
            getTableSyntaxNode().setMetaInfoReader(new SpreadsheetMetaInfoReader(this));
        }

        TableSyntaxNode tableSyntaxNode = getTableSyntaxNode();
        validateTableBody(tableSyntaxNode, bindingContext);
        IOpenMethodHeader header = getHeader();
        if (header.getType() == JavaOpenClass.VOID) {
            throw SyntaxNodeExceptionUtils.createError("Spreadsheet can not return 'void' type", tableSyntaxNode);
        }
        this.bindingContext = bindingContext;
        componentsBuilder = new SpreadsheetComponentsBuilder(tableSyntaxNode, bindingContext);
        componentsBuilder.buildHeaders(header.getType());
        structureBuilder = new SpreadsheetStructureBuilder(componentsBuilder, header);
        String headerType = header.getName() + "Type";
        OpenL openL = bindingContext.getOpenL();
        spreadsheetOpenClass = new SpreadsheetOpenClass(headerType, openL);

        Boolean autoType = tableSyntaxNode.getTableProperties().getAutoType();
        structureBuilder.addCellFields(spreadsheetOpenClass, autoType);
    }

    @Override
    public void finalizeBind(IBindingContext bindingContext) throws Exception {
        super.finalizeBind(bindingContext);

        ILogicalTable tableBody = getTableSyntaxNode().getTableBody();

        getTableSyntaxNode().getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tableBody);

        cells = structureBuilder.getCells();
        Spreadsheet spreadsheet = (Spreadsheet) getMethod();
        spreadsheet.setCells(cells);

        spreadsheet.setResultBuilder(componentsBuilder.buildResultBuilder(spreadsheet));
    }

    public SpreadsheetCell[][] getCells() {
        return cells;
    }

    private void validateTableBody(TableSyntaxNode tableSyntaxNode,
            IBindingContext bindingContext) throws SyntaxNodeException {
        ILogicalTable tableBody = tableSyntaxNode.getTableBody();
        if (tableBody == null) {
            throw SyntaxNodeExceptionUtils.createError(
                "Table has no body! Try to merge header cell horizontally to identify table.",
                getTableSyntaxNode());
        }

        int height = tableBody.getHeight();
        int width = tableBody.getWidth();

        if (height < 2 || width < 2) {
            String message = "Spreadsheet has empty body. Spreadsheet table should has at least 2x3 cells.";
            BindHelper.processWarn(message, tableSyntaxNode, bindingContext);
        }
    }

    public Spreadsheet getSpreadsheet() {
        return (Spreadsheet) getMethod();
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        if (cells != null) {
            for (SpreadsheetCell[] cellArray : cells) {
                if (cellArray != null) {
                    for (SpreadsheetCell cell : cellArray) {
                        if (cell != null) {
                            CompositeMethod method = (CompositeMethod) cell.getMethod();
                            if (method != null) {
                                method.updateDependency(dependencies);
                            }
                        }
                    }
                }
            }
        }
    }

    public SpreadsheetComponentsBuilder getComponentsBuilder() {
        return componentsBuilder;
    }

    @Override
    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        if (cxt.isExecutionMode()) {
            super.removeDebugInformation(cxt);
            // clean the builder, that was used for creating spreadsheet
            //
            this.structureBuilder.getSpreadsheetStructureBuilderHolder().clear();
            this.bindingContext = null;
        }
    }
}
