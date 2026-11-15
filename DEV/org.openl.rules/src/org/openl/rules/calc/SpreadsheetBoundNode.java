package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.exception.AmbiguousTypeException;
import org.openl.binding.impl.BindHelper;
import org.openl.message.OpenLMessagesUtils;
import org.openl.meta.TableMetaInfo;
import org.openl.rules.binding.RulesModuleBindingContextHelper;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;

// Kept as an open refactoring task: it needs a design change beyond this cleanup.
@SuppressWarnings("java:S1135")
// TODO: refactor
// Extract all the binding and build code to the SpreadsheetBinder
public class SpreadsheetBoundNode extends AMethodBasedNode {

    private SpreadsheetStructureBuilder structureBuilder;
    private SpreadsheetOpenClass spreadsheetOpenClass;
    @Getter
    private SpreadsheetCell[][] cells;

    private IBindingContext bindingContext;

    public SpreadsheetBoundNode(TableSyntaxNode tableSyntaxNode,
                                OpenL openl,
                                IOpenMethodHeader header,
                                XlsModuleOpenClass module,
                                IBindingContext bindingContext) {

        super(tableSyntaxNode, openl, header, module);
        this.bindingContext = bindingContext;
    }

    @Override
    public XlsModuleOpenClass getModule() {
        return (XlsModuleOpenClass) super.getModule();
    }

    private String[][] getDescriptions(Spreadsheet spreadsheet) {
        BidiMap<Integer, Integer> rowOffsets = spreadsheet.getRowOffsets();
        BidiMap<Integer, Integer> columnOffsets = spreadsheet.getColumnOffsets();
        String[][] descriptions = new String[spreadsheet.getHeight()][spreadsheet.getWidth()];
        for (var i = 0; i < spreadsheet.getHeight(); i++) {
            for (var j = 0; j < spreadsheet.getWidth(); j++) {
                var col = columnOffsets.get(j);
                var row = rowOffsets.get(i);
                var f = false;
                if (spreadsheet.getColumnDescriptions().containsValue(col)) {
                    col = spreadsheet.getColumnDescriptions().inverseBidiMap().get(col);
                    f = true;
                } else if (spreadsheet.getRowDescriptions().containsValue(row)) {
                    row = spreadsheet.getRowDescriptions().inverseBidiMap().get(row);
                    f = true;
                }
                if (f) {
                    var cell = structureBuilder.getTableBody().getCell(col + 1, row + 1);
                    var value = cell.getStringValue();
                    if (org.openl.util.StringUtils.isNotBlank(value)) {
                        descriptions[i][j] = value;
                    }
                }
            }
        }
        return descriptions;
    }

    private CustomSpreadsheetResultOpenClass buildCustomSpreadsheetResultType(Spreadsheet spreadsheet) {
        Collection<IOpenField> spreadsheetOpenClassFields = spreadsheet.getSpreadsheetType()
                .getFields()
                .stream()
                .filter(e -> !"this".equals(e.getName()))
                .toList();

        var typeName = Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + spreadsheet.getName();

        var customSpreadsheetResultOpenClass = new CustomSpreadsheetResultOpenClass(
                typeName,
                spreadsheet.getRowNames(),
                spreadsheet.getColumnNames(),
                spreadsheet.getRowNamesForResultModel(),
                spreadsheet.getColumnNamesForResultModel(),
                getDescriptions(spreadsheet),
                getModule(),
                true);

        customSpreadsheetResultOpenClass
                .setMetaInfo(new TableMetaInfo("Spreadsheet", spreadsheet.getName(), spreadsheet.getSourceUrl()));

        for (IOpenField field : spreadsheetOpenClassFields) {
            var customSpreadsheetResultField = new CustomSpreadsheetResultField(
                    customSpreadsheetResultOpenClass,
                    field);
            customSpreadsheetResultOpenClass.addField(customSpreadsheetResultField);
        }
        return customSpreadsheetResultOpenClass;
    }

    @Override
    protected ExecutableRulesMethod createMethodShell() {
        /*
         * We need to generate a customSpreadsheet class only if return type of the spreadsheet is SpreadsheetResult
         * and the customspreadsheet property is true
         */
        var isTypeCustomSpreadsheetResult = SpreadsheetResult.class == getType().getInstanceClass()
                && !(getType() instanceof CustomSpreadsheetResultOpenClass)
                && !structureBuilder.isExistsReturnHeader();

        //*TODO Refactor this to build Spreadsheet in the SpreadsheetBinder
        var spreadsheet = new Spreadsheet(getHeader(), this);
        spreadsheet.setSpreadsheetType(spreadsheetOpenClass);
        // As custom spreadsheet result is being generated at runtime,
        // call this method to ensure that CSR will be generated during the
        // compilation.
        // Add generated type to be accessible through binding context.
        //
        spreadsheet.setRowNames(structureBuilder.getRowNames());
        spreadsheet.setRowOffsets(structureBuilder.getRowOffsets());
        spreadsheet.setRowDescriptions(structureBuilder.getRowDescriptions());

        spreadsheet.setColumnNames(structureBuilder.getColumnNames());
        spreadsheet.setColumnOffsets(structureBuilder.getColumnOffsets());
        spreadsheet.setColumnDescriptions(structureBuilder.getColumnDescriptions());

        spreadsheet.setRowNamesForResultModel(structureBuilder.getRowNamesForResultModel());
        spreadsheet.setColumnNamesForResultModel(structureBuilder.getColumnNamesForResultModel());

        if (isTypeCustomSpreadsheetResult) {
            CustomSpreadsheetResultOpenClass type;
            try {
                type = buildCustomSpreadsheetResultType(spreadsheet); // Can throw RuntimeException
                type.setReplacedVersion(isReplacedVersion());
                var bindingContextType = bindingContext.addType(type);
                spreadsheet.setCustomSpreadsheetResultType((CustomSpreadsheetResultOpenClass) bindingContextType);
            } catch (Exception | LinkageError e) {
                var message = "Cannot define type '%s'.".formatted(
                        Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + spreadsheet.getName());
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, e, getTableSyntaxNode());
                bindingContext.addError(error);
                spreadsheet.setCustomSpreadsheetResultType(
                        (CustomSpreadsheetResultOpenClass) bindingContext.findType(
                                Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + spreadsheet.getName()));
            }
        }
        return spreadsheet;
    }

    /**
     * Whether this table is a version that an active version replaces: it is not active, and another Spreadsheet of the
     * module has the same name, the same types of parameters and the same dimension properties and is active. Such a
     * version is never called.
     */
    private boolean isReplacedVersion() {
        var node = getTableSyntaxNode();
        var properties = node.getTableProperties();
        if (properties == null || !Boolean.FALSE.equals(properties.getActive())) {
            return false;
        }
        var signature = signatureOf(node);
        return signature != null && Arrays.stream(getModule().getXlsMetaInfo().getXlsModuleNode().getXlsTableSyntaxNodes())
                .filter(other -> other != node && XlsNodeTypes.XLS_SPREADSHEET == other.getNodeType())
                .filter(other -> other.getTableProperties() == null
                        || !Boolean.FALSE.equals(other.getTableProperties().getActive()))
                .anyMatch(other -> signature.equals(signatureOf(other))
                        && sameDimensions(properties, other.getTableProperties()));
    }

    /**
     * The name and the types of the parameters the header of a table declares, or {@code null} when unreadable. A type
     * is named as OpenL resolves it, so {@code String} and {@code java.lang.String} are the same type.
     */
    private @Nullable String signatureOf(TableSyntaxNode node) {
        var header = node.getHeaderLineValue();
        var text = header == null ? "" : header.getValue().strip();
        var open = text.indexOf('(');
        if (open < 0 || !text.endsWith(")")) {
            return null;
        }
        var beforeParameters = text.substring(0, open).strip();
        var name = beforeParameters.substring(lastWhitespace(beforeParameters) + 1);
        var types = parametersOf(text.substring(open + 1, text.length() - 1)).stream()
                .map(String::strip)
                .filter(parameter -> !parameter.isEmpty())
                .map(parameter -> resolvedType(typeOf(parameter)))
                .toList();
        return name + types;
    }

    /** The Java name of a written type, such as {@code java.lang.String[]} for {@code String []}, or the text itself. */
    private String resolvedType(String written) {
        var base = StringUtils.deleteWhitespace(written);
        var dimensions = 0;
        while (base.endsWith("[]")) {
            base = base.substring(0, base.length() - 2);
            dimensions++;
        }
        IOpenClass type;
        bindingContext.pushErrors();
        bindingContext.pushMessages();
        try {
            type = bindingContext.findType(base);
        } catch (AmbiguousTypeException e) {
            type = null;
        } finally {
            bindingContext.popErrors();
            bindingContext.popMessages();
        }
        return (type == null ? base : type.getJavaName()) + "[]".repeat(dimensions);
    }

    /** The parameters of a header, split at the commas outside the angle brackets of a generic type. */
    private static List<String> parametersOf(String text) {
        var parameters = new ArrayList<String>();
        var depth = 0;
        var start = 0;
        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parameters.add(text.substring(start, i));
                start = i + 1;
            }
        }
        parameters.add(text.substring(start));
        return parameters;
    }

    /**
     * The type of a parameter written as {@code Type name}, without the context it takes its value from, such as
     * {@code : context.currentDate}, or the text as it is when it has no name.
     */
    private static String typeOf(String parameter) {
        var colon = parameter.indexOf(':');
        var declaration = (colon < 0 ? parameter : parameter.substring(0, colon)).strip();
        var end = lastWhitespace(declaration);
        return end < 0 ? declaration : declaration.substring(0, end).strip();
    }

    private static int lastWhitespace(String text) {
        var i = text.length() - 1;
        while (i >= 0 && !Character.isWhitespace(text.charAt(i))) {
            i--;
        }
        return i;
    }

    private static boolean sameDimensions(ITableProperties properties, @Nullable ITableProperties other) {
        return DimensionPropertiesMethodKey.compareMethodDimensionProperties(properties.getAllDimensionalProperties(),
                other == null ? Map.of() : other.getAllDimensionalProperties());
    }

    public void validateRowsColumnsForResultModel(Spreadsheet spreadsheet) {
        var columnsForResultModelCount = Arrays.stream(spreadsheet.getColumnNamesForResultModel())
                .filter(Objects::nonNull)
                .count();
        var rowsForResultModelCount = Arrays.stream(spreadsheet.getRowNamesForResultModel())
                .filter(Objects::nonNull)
                .count();

        var fNames = new HashMap<String, String>();
        var warnCnt = 0;
        for (var i = 0; i < spreadsheet.getRowNamesForResultModel().length; i++) {
            for (var j = 0; j < spreadsheet.getColumnNamesForResultModel().length; j++) {
                if (spreadsheet.getColumnNamesForResultModel()[j] != null && spreadsheet
                        .getRowNamesForResultModel()[i] != null && warnCnt < 10) { // Don't show more than 10 conflict
                    // messages
                    var conflict = putResultModelFieldName(spreadsheet,
                            i,
                            j,
                            columnsForResultModelCount,
                            rowsForResultModelCount,
                            fNames);
                    if (conflict != null) {
                        bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(conflict, getTableSyntaxNode()));
                        warnCnt++;
                    }
                }
            }
        }
    }

    /**
     * Registers the output model field name of a cell that is not of a void type.
     *
     * @return the warning about a cell registered under the same name before, or {@code null} if there is none
     */
    private static String putResultModelFieldName(Spreadsheet spreadsheet,
                                                  int i,
                                                  int j,
                                                  long columnsForResultModelCount,
                                                  long rowsForResultModelCount,
                                                  Map<String, String> fNames) {
        String fieldName = SpreadsheetStructureBuilder
                .getSpreadsheetCellFieldName(spreadsheet.getColumnNames()[j], spreadsheet.getRowNames()[i]);

        var field = spreadsheet.getSpreadsheetType().getField(fieldName);
        var t = field.getType();
        while (t.isArray()) {
            t = t.getComponentClass();
        }
        var f = !JavaOpenClass.VOID.equals(t) && !JavaOpenClass.CLS_VOID.equals(t) && !NullOpenClass.the
                .equals(t);
        // IGNORE VOID TYPES

        if (f) {
            String refName;
            if (columnsForResultModelCount == 1) {
                refName = SpreadsheetStructureBuilder.DOLLAR_SIGN + spreadsheet.getRowNames()[i];
            } else if (rowsForResultModelCount == 1) {
                refName = SpreadsheetStructureBuilder.DOLLAR_SIGN + spreadsheet.getColumnNames()[j];
            } else {
                refName = fieldName;
            }

            var fName = getResultModelFieldName(spreadsheet, i, j, columnsForResultModelCount, rowsForResultModelCount);
            String key = fName.length() > 1 ? (Character.toLowerCase(fName.charAt(0)) + fName.substring(1))
                    : fName.toLowerCase();
            var v = fNames.put(key, refName);
            if (v != null) {
                return "Cells '%s' and '%s' conflict with each other in the spreadsheet output model.".formatted(v,
                        refName);
            }
        }
        return null;
    }

    private static String getResultModelFieldName(Spreadsheet spreadsheet,
                                                  int i,
                                                  int j,
                                                  long columnsForResultModelCount,
                                                  long rowsForResultModelCount) {
        var sb = new StringBuilder();
        if (columnsForResultModelCount == 1) {
            sb.append(ClassUtils.decapitalize(spreadsheet.getRowNamesForResultModel()[i]));
        } else if (rowsForResultModelCount == 1) {
            sb.append(ClassUtils.decapitalize(spreadsheet.getColumnNamesForResultModel()[j]));
        } else {
            sb.append(ClassUtils.decapitalize(spreadsheet.getColumnNamesForResultModel()[j]));
            sb.append(ClassUtils.capitalize(spreadsheet.getRowNamesForResultModel()[i]));
        }
        var fName = sb.toString();
        if (StringUtils.isBlank(fName)) {
            fName = "_";
        }
        return fName;
    }

    public void preBind() throws SyntaxNodeException {
        var tableSyntaxNode = getTableSyntaxNode();
        validateTableBody(tableSyntaxNode, bindingContext);
        var header = getHeader();
        RulesModuleBindingContextHelper.compileAllTypesInSignature(header.getSignature(), bindingContext);
        structureBuilder = new SpreadsheetStructureBuilder(tableSyntaxNode, bindingContext, header, getModule());
        var headerType = header.getName() + "Type";
        var openL = bindingContext.getOpenL();
        spreadsheetOpenClass = new SpreadsheetOpenClass(headerType, openL);

        var autoType = tableSyntaxNode.getTableProperties().getAutoType();
        structureBuilder.addCellFields(spreadsheetOpenClass, autoType);
    }

    @Override
    public void finalizeBind(IBindingContext bindingContext) throws Exception {
        super.finalizeBind(bindingContext);

        var tableBody = getTableSyntaxNode().getTableBody();

        getTableSyntaxNode().getSubTables().put(IXlsTableNames.VIEW_BUSINESS, tableBody);

        cells = structureBuilder.getCells();
        var spreadsheet = (Spreadsheet) getMethod();
        if (spreadsheet != null) {
            validateRowsColumnsForResultModel(spreadsheet);

            spreadsheet.setCells(cells);

            spreadsheet.setResultBuilder(structureBuilder.buildResultBuilder(spreadsheet, bindingContext));
        }
    }

    private void validateTableBody(TableSyntaxNode tableSyntaxNode,
                                   IBindingContext bindingContext) throws SyntaxNodeException {
        var tableBody = tableSyntaxNode.getTableBody();
        if (tableBody == null) {
            throw SyntaxNodeExceptionUtils.createError(
                    "Table has no body. Try to merge header cell horizontally to identify table.",
                    getTableSyntaxNode());
        }

        var height = tableBody.getHeight();
        var width = tableBody.getWidth();

        if (height < 2 || width < 2) {
            var message = "Spreadsheet has empty body. Spreadsheet table should has at least 2x3 cells.";
            BindHelper.processWarn(message, tableSyntaxNode, bindingContext);
        }
    }

    public Spreadsheet getSpreadsheet() {
        return (Spreadsheet) getMethod();
    }

    /**
     * How the body of the Spreadsheet was read: its steps, its columns and the header whose value it returns.
     *
     * @return the builder of the structure, or {@code null} before the Spreadsheet is bound
     */
    public SpreadsheetStructureBuilder getStructureBuilder() {
        return structureBuilder;
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        if (cells != null) {
            for (SpreadsheetCell[] cellArray : cells) {
                if (cellArray != null) {
                    updateCellsDependency(cellArray, dependencies);
                }
            }
        }
    }

    private static void updateCellsDependency(SpreadsheetCell[] cellArray, BindingDependencies dependencies) {
        for (SpreadsheetCell cell : cellArray) {
            if (cell != null) {
                var method = (CompositeMethod) cell.getMethod();
                if (method != null) {
                    method.updateDependency(dependencies);
                }
            }
        }
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
