package org.openl.extension.xmlrules;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openl.exception.OpenLCompilationException;
import org.openl.extension.xmlrules.model.*;
import org.openl.extension.xmlrules.model.lazy.LazyCells;
import org.openl.extension.xmlrules.model.lazy.LazyWorkbook;
import org.openl.extension.xmlrules.model.single.*;
import org.openl.extension.xmlrules.model.single.node.Node;
import org.openl.extension.xmlrules.model.single.node.ValueHolder;
import org.openl.extension.xmlrules.project.XmlRulesModule;
import org.openl.extension.xmlrules.project.XmlRulesModuleSourceCodeModule;
import org.openl.extension.xmlrules.syntax.StringGridBuilder;
import org.openl.extension.xmlrules.utils.CellReference;
import org.openl.extension.xmlrules.utils.RulesTableReference;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.*;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorksheetSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.impl.ParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlRulesParser extends BaseParser {
    private final Logger log = LoggerFactory.getLogger(XmlRulesParser.class);

    public XmlRulesParser() {
    }

    protected ExtensionModule load(IOpenSourceCodeModule source) {
        String uri = source.getUri(0);
        return new ZipFileXmlDeserializer(uri).deserialize();
    }

    /**
     * Wrap source to XlsWorkbookSourceCodeModule
     */
    protected XlsWorkbookSourceCodeModule getWorkbookSourceCodeModule(ExtensionModule extensionModule,
            IOpenSourceCodeModule source) throws
                                          OpenLCompilationException {
        try {
            // TODO Check the cases when source can be UrlSourceCodeModule or another one.
            File projectFolder = new File(new File(new URI(source.getUri(0))).getParent());

            return new XlsWorkbookSourceCodeModule(source, new LazyXmlRulesWorkbookLoader(projectFolder,
                    extensionModule));
        } catch (URISyntaxException e) {
            throw new OpenLCompilationException(e.getMessage(), e);
        }
    }

    /**
     * Gets all grid tables from the sheet.
     */
    protected IGridTable[] getAllGridTables(XlsSheetSourceCodeModule sheetSource,
            ExtensionModule module,
            LazyWorkbook workbook,
            Sheet sheet, XmlRulesModuleSourceCodeModule sourceCodeModule) {
        String uri = sheetSource.getUri();
        LazyXmlRulesWorkbookLoader workbookLoader = (LazyXmlRulesWorkbookLoader) sheetSource.getWorkbookSource()
                .getWorkbookLoader();

        StringGridBuilder gridBuilder = new StringGridBuilder(uri,
                workbookLoader.getExtensionModule().getFileName());

        if (workbook.getXlsFileName().equals(ExtensionDescriptor.TYPES_WORKBOOK)) {
            createTypes(gridBuilder, sheet);
        } else {
            createDataInstances(gridBuilder, module, sheet);
            createTables(gridBuilder, sheet);
            createFunctions(gridBuilder, sheet);
            createCellExpressions(gridBuilder, sheet);
            createArrayCellExpressions(gridBuilder, sheet);

            if (sheet.getId() == 1) {
                createEnvironment(gridBuilder, sourceCodeModule);
            }
        }

        return gridBuilder.build().getTables();
    }

    private void createTypes(StringGridBuilder gridBuilder, Sheet sheet) {
        try {
            if (sheet.getTypes() == null) {
                return;
            }
            for (Type type : sheet.getTypes()) {
                ProjectData.getCurrentInstance().getTypes().add(type.getName());
                gridBuilder.addCell("Datatype " + type.getName(), 2).nextRow();

                for (Field field : type.getFields()) {
                    String typeName = field.getTypeName();
                    if (StringUtils.isBlank(typeName)) {
                        typeName = "String";
                    }

                    ProjectData.getCurrentInstance().getFields().add(field.getName());
                    gridBuilder.addCell(typeName).addCell(field.getName()).nextRow();
                }

                gridBuilder.nextRow();
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            OpenLMessagesUtils.addError(e);
            gridBuilder.nextRow();
        }
    }
    private void createEnvironment(StringGridBuilder gridBuilder, XmlRulesModuleSourceCodeModule sourceCodeModule) {
        try {
            gridBuilder.addCell("Environment", 2).nextRow();

            gridBuilder.addCell("dependency");
            String name = sourceCodeModule.getModuleName();
            String moduleName = name.substring(0, name.lastIndexOf(".")) + "." + ExtensionDescriptor.TYPES_WORKBOOK.substring(0,
                    ExtensionDescriptor.TYPES_WORKBOOK.lastIndexOf("."));
            gridBuilder.addCell(moduleName);
            gridBuilder.nextRow();

            List<String> dependencies = sourceCodeModule.getModule().getExtension().getDependencies();
            if (dependencies != null) {
                for (String dependency : dependencies) {
                    gridBuilder.addCell("dependency").addCell(dependency).nextRow();
                }
            }

            gridBuilder.nextRow();
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            OpenLMessagesUtils.addError(e);
            gridBuilder.nextRow();
        }
    }

    private void createDataInstances(StringGridBuilder gridBuilder, ExtensionModule module, Sheet sheet) {
        try {
            if (sheet.getDataInstances() == null) {
                return;
            }
            for (DataInstance dataInstance : sheet.getDataInstances()) {
                List<String> fields = dataInstance.getFields();
                Type t = getType(module, dataInstance);
                if (t == null) {
                    throw new IllegalArgumentException("Can't find type " + dataInstance.getType());
                }
                List<FieldImpl> actualFields = t.getFields();
                if (fields == null) {
                    fields = new ArrayList<String>();
                    for (FieldImpl field : actualFields) {
                        fields.add(field.getName());
                    }
                }

                gridBuilder.addCell("Data " + dataInstance.getType() + " " + dataInstance.getName(),
                        fields.size()).nextRow();
                // Fields
                boolean hasReferences = false;
                for (int fieldIndex = 0; fieldIndex < fields.size(); fieldIndex++) {
                    String field = fields.get(fieldIndex);

                    FieldImpl actualField = getField(actualFields, field);

                    if (actualField != null && actualField.getTypeName() != null && actualField.getTypeName().endsWith("[]")) {
                        int maximumArrayLength = getMaximumArrayLength(dataInstance, fieldIndex);
                        for (int i = 0; i < maximumArrayLength; i++) {
                            gridBuilder.addCell(field + "[" + i + "]");
                        }
                    } else {
                        gridBuilder.addCell(field);
                    }

                    if (getReference(dataInstance, field) != null) {
                        hasReferences = true;
                    }
                }
                gridBuilder.nextRow();

                // References
                if (hasReferences) {
                    for (int fieldIndex = 0; fieldIndex < fields.size(); fieldIndex++) {
                        String field = fields.get(fieldIndex);
                        Reference reference = getReference(dataInstance, field);

                        if (reference != null) {
                            int maximumArrayLength = getMaximumArrayLength(dataInstance, fieldIndex);
                            if (maximumArrayLength > 0) {
                                for (int i = 0; i < maximumArrayLength; i++) {
                                    gridBuilder.addCell(">" + reference.getDataInstance());
                                }
                            } else {
                                gridBuilder.addCell(">" + reference.getDataInstance());
                            }
                        } else {
                            gridBuilder.addCell(null);
                        }
                    }
                    gridBuilder.nextRow();
                }

                // Business names
                for (String field : fields) {
                    gridBuilder.addCell(field.toUpperCase());
                }
                gridBuilder.nextRow();

                for (ValuesRow row : dataInstance.getValues()) {
                    for (ArrayValue value : row.getList()) {
                        List<String> arrayValues = value.getValues();
                        for (String arrayValue : arrayValues) {
                            gridBuilder.addCell(arrayValue);
                        }
                    }
                    gridBuilder.nextRow();
                }

                gridBuilder.nextRow();
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            OpenLMessagesUtils.addError(e);
            gridBuilder.nextRow();
        }
    }

    private FieldImpl getField(List<FieldImpl> actualFields, String field) {
        FieldImpl actualField = null;
        for (FieldImpl f : actualFields) {
            if (f.getName().equals(field)) {
                actualField = f;
                break;
            }
        }
        return actualField;
    }

    private Type getType(ExtensionModule module, DataInstance dataInstance) {
        for (LazyWorkbook workbook : module.getInternalWorkbooks()) {
            for (Sheet s : workbook.getSheets()) {
                for (Type type : s.getTypes()) {
                    if (dataInstance.getType().equals(type.getName())) {
                        return type;
                    }
                }
            }
        }

        return null;
    }

    private Reference getReference(DataInstance dataInstance, String field) {
        List<Reference> references = dataInstance.getReferences();
        if (references != null) {
            for (Reference reference : references) {
                if (reference.getField().equals(field)) {
                    return reference;
                }
            }
        }

        return null;
    }

    private int getMaximumArrayLength(DataInstance dataInstance, int fieldIndex) {
        int maximumArrayLength = -1;
        for (ValuesRow row : dataInstance.getValues()) {
            List rowList = row.getList();
            if (fieldIndex >= rowList.size()) {
                continue;
            }
            Object value = rowList.get(fieldIndex);
            if (value instanceof ArrayValue) {
                int arraySize = ((ArrayValue) value).getValues().size();
                if (arraySize > maximumArrayLength) {
                    maximumArrayLength = arraySize;
                }
            }
        }
        return maximumArrayLength;
    }

    private void createTables(StringGridBuilder gridBuilder, Sheet sheet) {
        try {
            if (sheet instanceof SheetHolder && ((SheetHolder) sheet).getInternalSheet() != null) {
                sheet = ((SheetHolder) sheet).getInternalSheet();
            }
            if (sheet.getTables() == null) {
                return;
            }
            for (Table table : sheet.getTables()) {
                boolean isSimpleRules = table.getHorizontalConditions().isEmpty();

                int tableWidth = getTableWidth(table, isSimpleRules);

                int headerHeight = 0;
                int headerWidth = 0;

                Segment segment = table.getSegment();
                if (segment != null && segment.getTotalSegments() == 1) {
                    segment = null;
                }
                if (segment != null) {
                    String tablePartHeader = "TablePart " + table.getName() +
                            (segment.isColumnSegment() ? " column " : " row ")
                            + segment.getSegmentNumber() + " of " + segment.getTotalSegments();
                    gridBuilder.addCell(tablePartHeader, tableWidth).nextRow();
                }
                int tableRow = gridBuilder.getRow();

                String tableType = isSimpleRules ? "SimpleRules" : "SimpleLookup";
                StringBuilder header = new StringBuilder();
                String returnType = table.getReturnType();
                if (StringUtils.isBlank(returnType)) {
                    returnType = "String";
                }
                header.append(tableType).append(" ").append(returnType).append(" ").append(table.getName()).append("(");
                boolean needComma = false;
                for (Parameter parameter : table.getParameters()) {
                    if (needComma) {
                        header.append(", ");
                    }
                    String type = parameter.getType();
                    if (StringUtils.isBlank(type)) {
                        type = "String";
                    }
                    header.append(type).append(' ').append(parameter.getName());
                    needComma = true;
                }
                header.append(")");

                if (segment == null || segment.isColumnSegment() || segment.getSegmentNumber() == 1) {
                    gridBuilder.addCell(header.toString(), tableWidth);
                    gridBuilder.nextRow();
                    headerHeight++;
                }

                int startColumn = gridBuilder.getStartColumn();

                // HC expressions
                if (segment == null || !segment.isColumnSegment() || segment.getSegmentNumber() == 1) {
                    gridBuilder.setStartColumn(startColumn + table.getVerticalConditions().size());

                    for (Condition condition : table.getHorizontalConditions()) {
                        for (Expression expression : condition.getExpressions()) {
                            gridBuilder.addCell(expression.getValue(), expression.getWidth());
                        }
                        gridBuilder.nextRow();
                        headerHeight++;
                    }
                    gridBuilder.setStartColumn(startColumn);
                }

                // VC header
                if (segment == null || segment.isColumnSegment() || segment.getSegmentNumber() == 1) {
                    if (isSimpleRules) {
                        for (Parameter parameter : table.getParameters()) {
                            gridBuilder.addCell(parameter.getName().toUpperCase());
                        }
                        gridBuilder.addCell("Return");
                        gridBuilder.nextRow();
                        headerHeight++;
                    } else {
                        List<ParameterImpl> parameters = table.getParameters();
                        for (int i = 0; i < parameters.size(); i++) {
                            if (i >= table.getVerticalConditions().size()) {
                                break;
                            }
                            Parameter parameter = parameters.get(i);
                            gridBuilder.setCell(gridBuilder.getColumn(),
                                    tableRow + 1,
                                    1,
                                    table.getHorizontalConditions().size(),
                                    parameter.getName().toUpperCase());
                            headerWidth++;
                        }
                    }
                }

                // VC expressions
                int conditionRow = gridBuilder.getRow();
                int conditionColumn = gridBuilder.getColumn();
                for (Condition condition : table.getVerticalConditions()) {
                    int row = conditionRow;
                    for (Expression expression : condition.getExpressions()) {
                        gridBuilder.setCell(conditionColumn,
                                row,
                                expression.getWidth(),
                                expression.getHeight(),
                                expression.getValue());
                        row += expression.getHeight();
                    }
                    conditionColumn++;
                }

                // Return values
                String workbookName = sheet.getWorkbookName();
                String sheetName = sheet.getName();
                if (isSimpleRules) {
                    gridBuilder.setRow(tableRow + headerHeight);
                    gridBuilder.setStartColumn(conditionColumn);
                } else {
                    gridBuilder.setRow(tableRow + headerHeight);
                    gridBuilder.setStartColumn(startColumn + headerWidth);
                }
                for (ReturnRow returnValues : table.getReturnValues()) {
                    for (Expression returnValue : returnValues.getList()) {
                        if (returnValue.getReference()) {
                            String cell = CellReference.parse(workbookName, sheetName, returnValue.getValue())
                                    .getStringValue();
                            gridBuilder.addCell(String.format("= (%s) Cell(\"%s\")", returnType, cell));
                        } else {
                            gridBuilder.addCell(returnValue.getValue());
                        }
                        if (isSimpleRules && returnValues.getList().size() > 1) {
                            log.warn("SimpleRules can't contain two-dimensional return values");
                            break;
                        }
                    }
                    gridBuilder.nextRow();
                }
                gridBuilder.setStartColumn(startColumn);
                gridBuilder.nextRow();
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            OpenLMessagesUtils.addError(e);
            gridBuilder.nextRow();
        }
    }

    private int getTableWidth(Table table, boolean isSimpleRules) {
        int tableWidth = 0;
        for (Condition condition : table.getVerticalConditions()) {
            if (condition.getExpressions().size() > 0) {
                tableWidth += condition.getExpressions().get(0).getWidth();
            }
        }

        if (table.getHorizontalConditions().size() > 0) {
            Condition condition = table.getHorizontalConditions().get(0);
            for (Expression expression : condition.getExpressions()) {
                tableWidth += expression.getWidth();
            }
        }

        if (isSimpleRules) {
            tableWidth += 1;
        }

        if (tableWidth == 0) {
            tableWidth = 1;
        }
        return tableWidth;
    }

    private void createFunctions(StringGridBuilder gridBuilder, Sheet sheet) {
        if (sheet instanceof SheetHolder && ((SheetHolder) sheet).getInternalSheet() != null) {
            sheet = ((SheetHolder) sheet).getInternalSheet();
        }
        if (sheet.getFunctions() == null) {
            return;
        }
        for (Function function : sheet.getFunctions()) {
            try {
                StringBuilder headerBuilder = new StringBuilder();
                String returnType = function.getReturnType();
                if (StringUtils.isBlank(returnType)) {
                    returnType = "String";
                }
                headerBuilder.append("Method ")
                        .append(returnType)
                        .append(' ')
                        .append(function.getName())
                        .append('(');
                List<ParameterImpl> parameters = function.getParameters();
                String workbookName = sheet.getWorkbookName();
                String sheetName = sheet.getName();
                for (int i = 0; i < parameters.size(); i++) {
                    if (i > 0) {
                        headerBuilder.append(", ");
                    }
                    Parameter parameter = parameters.get(i);
                    String type = parameter.getType();
                    if (StringUtils.isBlank(type)) {
                        type = "String";
                    }
                    CellReference cellReference = CellReference.parse(workbookName, sheetName, parameter.getName());
                    headerBuilder.append(type)
                            .append(" ")
                            .append("R")
                            .append(cellReference.getRow())
                            .append("C")
                            .append(cellReference.getColumn());
                }
                headerBuilder.append(')');
                gridBuilder.addCell(headerBuilder.toString()).nextRow();

                for (ParameterImpl parameter : parameters) {
                    CellReference reference = CellReference.parse(workbookName, sheetName, parameter.getName());
                    String cell = String.format("Push(\"%s\", R%sC%s);",
                            reference.getStringValue(),
                            reference.getRow(),
                            reference.getColumn());
                    gridBuilder.addCell(cell).nextRow();
                }

                CellReference cellReference = CellReference.parse(workbookName, sheetName, function.getCellAddress());
                gridBuilder.addCell(String.format("%s result = (%s) Cell(\"%s\");",
                        returnType,
                        returnType,
                        cellReference.getStringValue()));
                gridBuilder.nextRow();

                for (ParameterImpl parameter : parameters) {
                    CellReference reference = CellReference.parse(workbookName, sheetName, parameter.getName());
                    String cell = String.format("Pop(\"%s\");", reference.getStringValue());
                    gridBuilder.addCell(cell).nextRow();
                }

                gridBuilder.addCell("return result;").nextRow();

                gridBuilder.nextRow();
            } catch (RuntimeException e) {
                log.error(e.getMessage(), e);
                OpenLMessagesUtils.addError(e);
                gridBuilder.nextRow();
            }
        }
    }

    private void createCellExpressions(StringGridBuilder gridBuilder, Sheet sheet) {
        try {
            if (sheet instanceof SheetHolder && ((SheetHolder) sheet).getInternalSheet() != null) {
                sheet = ((SheetHolder) sheet).getInternalSheet();
            }
            if (CollectionUtils.isEmpty(sheet.getCells())) {
                return;
            }
            final String workbookName = sheet.getWorkbookName();
            final String sheetName = sheet.getName();
            String cellsOnSheetName = new RulesTableReference(new CellReference(workbookName,
                    sheetName,
                    null,
                    null)).getTable();

            List<List<String>> conditions = new ArrayList<List<String>>();
            List<String> columnNumbers = new ArrayList<String>();
            columnNumbers.add("-");
            conditions.add(columnNumbers);
            for (LazyCells cells : sheet.getCells()) {
                for (Cell cell : cells.getCells()) {
                    if (cell.getEndAddress() != null) {
                        // Array cells are handled differently
                        continue;
                    }

                    try {
                        // Initialize rows and columns
                        // FIXME
                        CellReference reference = CellReference.parse(workbookName, sheetName, cell.getAddress());
                        getCurrentRow(conditions, reference);
                        getCurrentColumnNumber(columnNumbers, reference);
                    } catch (RuntimeException e) {
                        log.error(e.getMessage(), e);
                        OpenLMessagesUtils.addError(e);
                    }
                }
            }
            for (LazyCells cells : sheet.getCells()) {
                for (Cell cell : cells.getCells()) {
                    if (cell.getEndAddress() != null) {
                        // Array cells are handled differently
                        continue;
                    }

                    try {
                        CellReference reference = CellReference.parse(workbookName, sheetName, cell.getAddress());
                        List<String> currentRow = getCurrentRow(conditions, reference);
                        int currentColumnNumber = getCurrentColumnNumber(columnNumbers, reference);

                        while (currentRow.size() < currentColumnNumber + 1) {
                            currentRow.add(null);
                        }

                        Node node = cell.getNode();
                        String expression;
                        try {
                            if (node == null) {
                                throw new IllegalArgumentException("Cell [" + workbookName + "]" + sheetName + "!" + cell
                                        .getAddress()
                                        .toOpenLString() + " contains incorrect value. It will be skipped");
                            }
                            if (node instanceof ValueHolder) {
                                expression = ((ValueHolder) node).asString();
                            } else {
                                expression = "= " + node.toOpenLString();
                            }
                        } catch (RuntimeException e) {
                            expression = "";
                            String errorMessage = "Error in cell [" + workbookName + "]" + sheetName + "!" + cell.getAddress()
                                    .toOpenLString() + ": " + e.getMessage();
                            log.error(errorMessage, e);
                            OpenLMessagesUtils.addError(errorMessage);
                        }
                        currentRow.set(currentColumnNumber, expression);
                    } catch (Exception e) {
                        String errorMessage = "Error in cell [" + workbookName + "]" + sheetName + "!" + cell.getAddress()
                                .toOpenLString() + ": " + e.getMessage();
                        log.error(errorMessage, e);
                        OpenLMessagesUtils.addError(errorMessage);
                    }
                }
            }
            addCells(gridBuilder, cellsOnSheetName, conditions);
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            OpenLMessagesUtils.addError(e);
            gridBuilder.nextRow();
        }
    }

    private void createArrayCellExpressions(StringGridBuilder gridBuilder, Sheet sheet) {
        try {
            if (sheet instanceof SheetHolder && ((SheetHolder) sheet).getInternalSheet() != null) {
                sheet = ((SheetHolder) sheet).getInternalSheet();
            }
            if (CollectionUtils.isEmpty(sheet.getCells())) {
                return;
            }

            for (LazyCells cells : sheet.getCells()) {
                for (Cell cell : cells.getCells()) {
                    if (cell.getEndAddress() == null) {
                        // Non-array cells are handled differently
                        continue;
                    }

                    addArrayCells(gridBuilder, sheet, cell);
                }
            }

        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            OpenLMessagesUtils.addError(e);
            gridBuilder.nextRow();
        }
    }

    private void addArrayCells(StringGridBuilder gridBuilder, Sheet sheet, Cell cell) {
        final String workbookName = sheet.getWorkbookName();
        final String sheetName = sheet.getName();

        CellReference start = CellReference.parse(workbookName, sheetName, cell.getAddress());
        CellReference end = CellReference.parse(workbookName, sheetName, cell.getEndAddress());
        String tableName = new RulesTableReference(start, end).getTable();

        int startColumn = Integer.parseInt(start.getColumn());
        int startRow = Integer.parseInt(start.getRow());
        int endColumn = Integer.parseInt(end.getColumn());
        int endRow = Integer.parseInt(end.getRow());

        int columnsCount = endColumn - startColumn + 2;
        // TODO Add array operations parameters count

        gridBuilder.addCell("Spreadsheet SpreadsheetResult " + tableName + "()", columnsCount).nextRow();

        gridBuilder.addCell("#");
        for (int i = startColumn; i <= endColumn; i++) {
            gridBuilder.addCell("C" + i + " : Object");
        }
        gridBuilder.nextRow();

        // FIXME Here is simple case only
        for (int row = startRow; row <= endRow; row++) {
            gridBuilder.addCell("R" + row);

            for (int column = startColumn; column <= endColumn; column++) {
                Node node = cell.getNode();
                String expression;
                try {
                    if (node == null) {
                        throw new IllegalArgumentException("Cell [" + workbookName + "]" + sheetName + "!" + cell
                                .getAddress()
                                .toOpenLString() + " contains incorrect value. It will be skipped");
                    }
                    if (node instanceof ValueHolder) {
                        expression = ((ValueHolder) node).asString();
                    } else {
                        expression = "= " + node.toOpenLString();
                    }
                } catch (RuntimeException e) {
                    expression = "";
                    String errorMessage = "Error in cell [" + workbookName + "]" + sheetName + "!" + cell.getAddress()
                            .toOpenLString() + ": " + e.getMessage();
                    log.error(errorMessage, e);
                    OpenLMessagesUtils.addError(errorMessage);
                }
                gridBuilder.addCell(expression);
            }
            gridBuilder.nextRow();
        }

        gridBuilder.nextRow();
    }

    private List<String> getCurrentRow(List<List<String>> conditions, CellReference reference) {
        List<String> currentRow = null;
        for (int i = 1; i < conditions.size(); i++) {
            List<String> row = conditions.get(i);
            int comparison = Integer.valueOf(row.get(0)).compareTo(Integer.valueOf(reference.getRow()));
            if (comparison == 0) {
                currentRow = row;
                break;
            } else if (comparison > 0) {
                currentRow = new ArrayList<String>();
                currentRow.add(reference.getRow());
                conditions.add(i, currentRow);
                break;
            }
        }

        if (currentRow == null) {
            currentRow = new ArrayList<String>();
            currentRow.add(reference.getRow());
            conditions.add(currentRow);
        }
        return currentRow;
    }

    private int getCurrentColumnNumber(List<String> columnNumbers, CellReference reference) {
        int currentColumnNumber = 0;
        for (int i = 1; i < columnNumbers.size(); i++) {
            String columnNumber = columnNumbers.get(i);
            int comparison = Integer.valueOf(columnNumber.length()).compareTo(reference.getColumn().length());
            if (comparison == 0) {
                comparison = columnNumber.compareTo(reference.getColumn());
            }
            if (comparison == 0) {
                currentColumnNumber = i;
                break;
            } else if (comparison > 0) {
                columnNumbers.add(i, reference.getColumn());
                currentColumnNumber = i;
                break;
            }
        }

        if (currentColumnNumber == 0) {
            columnNumbers.add(reference.getColumn());
            currentColumnNumber = columnNumbers.size() - 1;
        }
        return currentColumnNumber;
    }

    private void addCells(StringGridBuilder gridBuilder, String cellsOnSheetName, List<List<String>> conditions) {
        int columnsCount = conditions.get(0).size();
        gridBuilder.addCell("SimpleLookup Object " + cellsOnSheetName + "(String row, String column)", columnsCount).nextRow();

        for (List<String> row : conditions) {
            for (String cell : row) {
                gridBuilder.addCell(cell);
            }
            gridBuilder.nextRow();
        }

        gridBuilder.nextRow();
    }

    @Override
    public IParsedCode parseAsModule(IOpenSourceCodeModule source) {
        XmlRulesModuleSourceCodeModule sourceCodeModule = (XmlRulesModuleSourceCodeModule) source;
        XmlRulesModule openlModule = sourceCodeModule.getModule();
        ExtensionModule module = load(source);

        ISyntaxNode syntaxNode = null;
        List<SyntaxNodeException> errors = new ArrayList<SyntaxNodeException>();

        try {
            XlsWorkbookSourceCodeModule workbookSourceCodeModule = getWorkbookSourceCodeModule(module, source);

            initTypes(module);

            WorkbookSyntaxNode[] workbooksArray = getWorkbooks(module, workbookSourceCodeModule,
                    sourceCodeModule);
            syntaxNode = new XlsModuleSyntaxNode(workbooksArray,
                    workbookSourceCodeModule,
                    null,
                    null,
                    getImports()
            );
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            String message = String.format("Failed to open extension module: %s. Reason: %s",
                    source.getUri(0),
                    e.getMessage());
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, e, null);
            errors.add(error);
        }

        SyntaxNodeException[] parsingErrors = errors.toArray(new SyntaxNodeException[errors.size()]);

        List<IDependency> dependencies = new ArrayList<IDependency>();
        if (!sourceCodeModule.getInternalModulePath().equals(ExtensionDescriptor.TYPES_WORKBOOK)) {
            String name = sourceCodeModule.getModuleName();
            String moduleName = name.substring(0,
                    name.lastIndexOf(".")) + "." + ExtensionDescriptor.TYPES_WORKBOOK.substring(0,
                    ExtensionDescriptor.TYPES_WORKBOOK.lastIndexOf("."));
            IdentifierNode node = new IdentifierNode(IXlsTableNames.DEPENDENCY, null, moduleName, null);
            dependencies.add(new Dependency(DependencyType.MODULE, node));
            List<String> dependenciesList = openlModule.getExtension().getDependencies();
            if (dependenciesList != null) {
                for (String dependency : dependenciesList) {
                    dependencies.add(new Dependency(DependencyType.MODULE,
                            new IdentifierNode(IXlsTableNames.DEPENDENCY, null, dependency, null)));
                }
            }
        }

        return new ParsedCode(syntaxNode,
                source,
                parsingErrors,
                dependencies.toArray(new IDependency[dependencies.size()]));
    }

    private void initTypes(ExtensionModule module) {
        for (LazyWorkbook workbook : module.getInternalWorkbooks()) {
            for (Sheet s : workbook.getSheets()) {
                for (Type type : s.getTypes()) {
                    ProjectData.getCurrentInstance().getTypes().add(type.getName());
                    for (Field field : type.getFields()) {
                        ProjectData.getCurrentInstance().getFields().add(field.getName());
                    }
                }
            }
        }
    }

    protected List<String> getImports() {
        return Collections.emptyList();
    }

    protected WorkbookSyntaxNode[] getWorkbooks(ExtensionModule module,
            XlsWorkbookSourceCodeModule workbookSourceCodeModule,
            XmlRulesModuleSourceCodeModule sourceCodeModule) {
        TablePartProcessor tablePartProcessor = new TablePartProcessor();

        List<WorkbookSyntaxNode> workbookSyntaxNodes = new ArrayList<WorkbookSyntaxNode>();
        List<WorksheetSyntaxNode> sheetNodeList = new ArrayList<WorksheetSyntaxNode>();

        for (LazyWorkbook workbook : module.getWorkbooks()) {
            if (!sourceCodeModule.getInternalModulePath().equals(workbook.getXlsFileName())) {
                continue;
            }
            if (workbook.getXlsFileName().equals(ExtensionDescriptor.TYPES_WORKBOOK)) {
                ArrayList<Sheet> sheets = new ArrayList<Sheet>();
                workbook.setSheets(sheets);
                SheetImpl sheet = new SheetImpl();
                sheet.setName(ExtensionDescriptor.TYPES_SHEET);
                ArrayList<Type> types = new ArrayList<Type>();
                sheet.setTypes(types);
                sheets.add(sheet);
                for (LazyWorkbook w : module.getInternalWorkbooks()) {
                    for (Sheet s : w.getSheets()) {
                        types.addAll(s.getTypes());
                    }
                }
            } else if (workbook.getXlsFileName().equals(ExtensionDescriptor.MAIN_WORKBOOK)) {
                ArrayList<Sheet> sheets = new ArrayList<Sheet>();
                workbook.setSheets(sheets);
                int id = 1;
                for (LazyWorkbook w : module.getInternalWorkbooks()) {
                    for (Sheet s : w.getSheets()) {
                        SheetImpl sheet = new SheetImpl();
                        sheet.setWorkbookName(s.getWorkbookName());
                        sheet.setId(id++);
                        sheet.setName(w.getXlsFileName() + "." + s.getName());
                        sheet.setTables(s.getTables());
                        sheet.setFunctions(s.getFunctions());
                        sheet.setDataInstances(s.getDataInstances());
                        sheet.setCells(s.getCells());
                        sheet.setInternalSheet(s);
                        sheets.add(sheet);
                    }
                }
            }
            List<Sheet> sheets = workbook.getSheets();
            for (int i = 0; i < sheets.size(); i++) {
                Sheet sheet = sheets.get(i);
                // Sheet name is used as category name in WebStudio
                XlsSheetSourceCodeModule sheetSource = new XmlSheetSourceCodeModule(i,
                        workbookSourceCodeModule,
                        workbook);
                sheetNodeList.add(getWorksheet(sheetSource, workbook, sheet, module, tablePartProcessor,
                        sourceCodeModule));
            }
        }

        WorksheetSyntaxNode[] sheetNodes = sheetNodeList.toArray(new WorksheetSyntaxNode[sheetNodeList.size()]);

        TableSyntaxNode[] mergedNodes = {};
        try {
            List<TablePart> tableParts = tablePartProcessor.mergeAllNodes();
            int n = tableParts.size();
            mergedNodes = new TableSyntaxNode[n];
            for (int i = 0; i < n; i++) {
                mergedNodes[i] = preprocessTable(tableParts.get(i).getTable(), tableParts.get(i).getSource(),
                        tablePartProcessor);
            }
        } catch (OpenLCompilationException e) {
            OpenLMessagesUtils.addError(e);
        }

        workbookSyntaxNodes.add(new WorkbookSyntaxNode(sheetNodes, mergedNodes, workbookSourceCodeModule));

        return workbookSyntaxNodes.toArray(new WorkbookSyntaxNode[workbookSyntaxNodes.size()]);
    }

    protected WorksheetSyntaxNode getWorksheet(XlsSheetSourceCodeModule sheetSource,
            LazyWorkbook workbook, Sheet sheet,
            ExtensionModule module,
            TablePartProcessor tablePartProcessor, XmlRulesModuleSourceCodeModule sourceCodeModule) {
        IGridTable[] tables = getAllGridTables(sheetSource, module, workbook, sheet, sourceCodeModule);
        List<TableSyntaxNode> tableNodes = new ArrayList<TableSyntaxNode>();

        for (IGridTable table : tables) {
            try {
                tableNodes.add(preprocessTable(table, sheetSource, tablePartProcessor));
            } catch (OpenLCompilationException e) {
                OpenLMessagesUtils.addError(e);
            }
        }

        return new WorksheetSyntaxNode(tableNodes.toArray(new TableSyntaxNode[tableNodes.size()]), sheetSource);
    }

    private TableSyntaxNode preprocessTable(IGridTable table,
            XlsSheetSourceCodeModule source,
            TablePartProcessor tablePartProcessor) throws OpenLCompilationException {
        TableSyntaxNode tsn = XlsHelper.createTableSyntaxNode(table, source);
        String type = tsn.getType();
        if (type.equals(XlsNodeTypes.XLS_TABLEPART.toString())) {
            try {
                tablePartProcessor.register(table, source);
            } catch (Throwable t) {
                SyntaxNodeException sne = SyntaxNodeExceptionUtils.createError(t, tsn);
                tsn.addError(sne);
                OpenLMessagesUtils.addError(sne.getMessage());
            }
        }
        return tsn;
    }
}
