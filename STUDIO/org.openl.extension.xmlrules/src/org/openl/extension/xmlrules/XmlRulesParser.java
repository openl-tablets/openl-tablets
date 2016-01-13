package org.openl.extension.xmlrules;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openl.exception.OpenLCompilationException;
import org.openl.extension.xmlrules.model.*;
import org.openl.extension.xmlrules.model.lazy.LazyCells;
import org.openl.extension.xmlrules.model.lazy.LazyWorkbook;
import org.openl.extension.xmlrules.model.single.*;
import org.openl.extension.xmlrules.model.single.node.*;
import org.openl.extension.xmlrules.model.single.node.expression.CellInspector;
import org.openl.extension.xmlrules.model.single.node.expression.ExpressionContext;
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

// TODO Reduce complexity
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
                ProjectData.getCurrentInstance().addType(type);
                gridBuilder.addCell("Datatype " + type.getName(), 2).nextRow();

                for (Field field : type.getFields()) {
                    String typeName = field.getTypeName();
                    if (StringUtils.isBlank(typeName)) {
                        typeName = "String";
                    }

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

                    if (actualField != null && actualField.getTypeName() != null && actualField.getTypeName().endsWith(
                            "[]")) {
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
                table = prepareTable(table);
                boolean hasFunctionArguments = table.getParameters().size() > table.getHorizontalConditions().size() +
                        table.getVerticalConditions().size();
                if (hasFunctionArguments) {
                    createFunctionTable(gridBuilder, sheet, table);
                }
                boolean isSimpleRules = table.getHorizontalConditions().isEmpty();

                int tableWidth = getTableWidth(table, isSimpleRules);
                List<Attribute> attributes = table.getAttributes();
                if (!attributes.isEmpty()) {
                    tableWidth = Math.max(tableWidth, 3);
                }

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
                String returnType = "Object"; // Until it will be fixed on LE side
//                String returnType = table.getReturnType();
//                if (StringUtils.isBlank(returnType)) {
//                    returnType = "Object";
//                }
                header.append(tableType).append(" ").append(returnType).append(" ").append(table.getName()).append("(");
                boolean needComma = false;
                for (Parameter parameter : table.getParameters()) {
                    if (!isDimension(parameter)) {
                        continue;
                    }
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

                int attributesCount = attributes.size();
                headerHeight += attributesCount;
                addAttributes(gridBuilder, attributes);

                int startColumn = gridBuilder.getStartColumn();

                // HC expressions
                if (segment == null || !segment.isColumnSegment() || segment.getSegmentNumber() == 1) {
                    gridBuilder.setStartColumn(startColumn + table.getVerticalConditions().size());

                    for (Condition condition : table.getHorizontalConditions()) {
                        for (Expression expression : condition.getExpressions()) {
                            String value = expression.getValue();
                            if ("*".equals(value)) {
                                value = "";
                            }
                            gridBuilder.addCell(value, expression.getWidth());
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
                                    tableRow + 1 + attributesCount,
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
                        String value = expression.getValue();
                        if ("*".equals(value)) {
                            value = "";
                        }
                        gridBuilder.setCell(conditionColumn,
                                row,
                                expression.getWidth(),
                                expression.getHeight(),
                                value);
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
                        try {
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
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            OpenLMessagesUtils.addError(e);
                            gridBuilder.addCell("Error: " + e.getMessage());
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

    private void addAttributes(StringGridBuilder gridBuilder, List<Attribute> attributes) {
        if (!attributes.isEmpty()) {
            int height = attributes.size();

            int row = gridBuilder.getRow();
            int column = gridBuilder.getColumn();

            gridBuilder.setCell(column, row, 1, height, "properties");
            gridBuilder.setStartColumn(column + 1);

            for (Attribute attribute : attributes) {
                gridBuilder.addCell(attribute.getName());
                gridBuilder.addCell(attribute.getValue());
                gridBuilder.nextRow();
            }

            gridBuilder.setRow(row + height);
            gridBuilder.setStartColumn(column);
        }
    }

    private void createFunctionTable(StringGridBuilder gridBuilder, Sheet sheet, Table table) {
        StringBuilder headerBuilder = new StringBuilder();
        String returnType = "Object"; // Until it will be fixed on LE side
        //                String returnType = function.getReturnType();
        //                if (StringUtils.isBlank(returnType)) {
        //                    returnType = "Object";
        //                }

        headerBuilder.append("Method ")
                .append(returnType)
                .append(' ')
                .append(table.getName())
                .append('(');
        List<ParameterImpl> parameters = table.getParameters();
        String workbookName = sheet.getWorkbookName();
        String sheetName = sheet.getName();
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                headerBuilder.append(", ");
            }
            Parameter parameter = parameters.get(i);
            String type = parameter.getType();
            if (StringUtils.isBlank(type)) {
                type = "Object";
            }

            if (isDimension(parameter)) {
                headerBuilder.append(type).append(" ").append(parameter.getName());
            } else {
                CellReference cellReference = CellReference.parse(workbookName, sheetName, parameter.getName());
                headerBuilder.append(type)
                        .append(" ")
                        .append("R")
                        .append(cellReference.getRow())
                        .append("C")
                        .append(cellReference.getColumn());
            }
        }
        headerBuilder.append(')');

        List<Attribute> attributes = table.getAttributes();
        int width = attributes.isEmpty() ? 1 : 3;

        gridBuilder.addCell(headerBuilder.toString(), width).nextRow();

        addAttributes(gridBuilder, attributes);

        for (ParameterImpl parameter : parameters) {
            if (!isDimension(parameter)) {
                CellReference reference = CellReference.parse(workbookName, sheetName, parameter.getName());
                String cell = String.format("Push(\"%s\", R%sC%s);",
                        reference.getStringValue(),
                        reference.getRow(),
                        reference.getColumn());
                gridBuilder.addCell(cell).nextRow();
            }
        }

        StringBuilder tableInvokeString = new StringBuilder();
        tableInvokeString.append(returnType).append(" result = ").append(table.getName()).append("(");
        boolean needComma = false;
        for (ParameterImpl parameter : parameters) {
            if (isDimension(parameter)) {
                if (needComma) {
                    tableInvokeString.append(", ");
                }

                tableInvokeString.append(parameter.getName());

                needComma = true;
            }
        }
        tableInvokeString.append(");");
        gridBuilder.addCell(tableInvokeString.toString());
        gridBuilder.nextRow();

        for (ParameterImpl parameter : parameters) {
            if (!isDimension(parameter)) {
                CellReference reference = CellReference.parse(workbookName, sheetName, parameter.getName());
                String cell = String.format("Pop(\"%s\");", reference.getStringValue());
                gridBuilder.addCell(cell).nextRow();
            }
        }

        gridBuilder.addCell("return result;").nextRow();

        gridBuilder.nextRow();
    }

    private boolean isDimension(Parameter parameter) {
        return parameter.getName().startsWith("dim");
    }

    private Table prepareTable(Table source) {
        return sortReturnCells(sortConditionsOrder(removeGapsFromReturnRows(source)));
    }

    private Table removeGapsFromReturnRows(Table source) {
        List<ConditionImpl> horizontalConditions = source.getHorizontalConditions();
        List<ConditionImpl> verticalConditions = source.getVerticalConditions();
        if (horizontalConditions.isEmpty() || verticalConditions.isEmpty()) {
            return source;
        }

        int rowCount;
        int columnCount;

        int skipRows;
        int skipColumns;

        List<ReturnRow> returnValues = source.getReturnValues();

        TableRanges tableRanges = source.getTableRanges();
        if (tableRanges == null) {
            if ((verticalConditions.size() == 0 || returnValues.size() == verticalConditions.get(0).getExpressions().size())
                    && (horizontalConditions.size() == 0 ||
                    horizontalConditions.get(0).getExpressions().size() == returnValues.get(0).getList().size())) {
                return source;
            }

            rowCount = verticalConditions.size() > 0 ? verticalConditions.get(0).getExpressions().size() : 0;
            columnCount = horizontalConditions.size() > 0 ? horizontalConditions.get(0).getExpressions().size() : 0;
            skipRows = returnValues.size() - rowCount;
            skipColumns = returnValues.get(0).getList().size() - columnCount;
        } else {
            Range verticalRange = tableRanges.getVerticalConditionsRange();
            Range horizontalRange = tableRanges.getHorizontalConditionsRange();
            Range returnValuesRange = tableRanges.getReturnValuesRange();

            int rowStart = verticalRange.getRowNumber();
            rowCount = verticalRange.getRowCount();

            int columnStart = horizontalRange.getColumnNumber();
            columnCount = horizontalRange.getColCount();

            skipRows = returnValuesRange.getRowNumber() - rowStart;
            skipColumns = returnValuesRange.getColumnNumber() - columnStart;
        }

        List<ReturnRow> newReturnValues = new ArrayList<ReturnRow>();

        List<ReturnRow> subList = returnValues.subList(skipRows, skipRows + rowCount);
        for (ReturnRow returnValue : subList) {
            ReturnRow newReturnRow = new ReturnRow();
            newReturnRow.setList(returnValue.getList().subList(skipColumns, skipColumns + columnCount));
            newReturnValues.add(newReturnRow);
        }

        TableImpl table = new TableImpl();

        table.setSegment((SegmentImpl) source.getSegment());
        table.setName(source.getName());
        table.setAttributes(source.getAttributes());
        table.setReturnType(source.getReturnType());
        table.setParameters(new ArrayList<ParameterImpl>(source.getParameters()));
        table.setHorizontalConditions(new ArrayList<ConditionImpl>(horizontalConditions));
        table.setVerticalConditions(new ArrayList<ConditionImpl>(verticalConditions));
        table.setReturnValues(newReturnValues);

        return table;
    }

    private Table sortConditionsOrder(Table source) {
        try {
            boolean sortedConditions = true;

            int dimensionNumber = 0;
            List<ConditionImpl> verticalConditions = source.getVerticalConditions();
            List<ConditionImpl> horizontalConditions = source.getHorizontalConditions();
            int verticalSize = verticalConditions.size();
            int horizontalSize = horizontalConditions.size();

            for (ConditionImpl condition : verticalConditions) {
                if (condition.getParameterIndex() != dimensionNumber) {
                    sortedConditions = false;
                    break;
                }
                dimensionNumber++;
            }
            for (ConditionImpl condition : horizontalConditions) {
                if (condition.getParameterIndex() != dimensionNumber) {
                    sortedConditions = false;
                    break;
                }
                dimensionNumber++;
            }

            if (sortedConditions) {
                return source;
            }

            int parametersCount = source.getParameters().size();
            List<ConditionPath> conditionPaths = new ArrayList<ConditionPath>();

            for (int parameterIndex = 0; parameterIndex < parametersCount; parameterIndex++) {
                for (int i = 0; i < verticalSize; i++) {
                    ConditionImpl condition = verticalConditions.get(i);
                    if (parameterIndex == condition.getParameterIndex()) {
                        conditionPaths.add(new ConditionPath(true, i));
                        break;
                    }
                }
                for (int i = 0; i < horizontalSize; i++) {
                    ConditionImpl condition = horizontalConditions.get(i);
                    if (parameterIndex == condition.getParameterIndex()) {
                        conditionPaths.add(new ConditionPath(false, i));
                        break;
                    }
                }
            }

            ArrayList<ConditionImpl> newVerticalConditions = new ArrayList<ConditionImpl>();
            ArrayList<ReturnRow> newReturnValues = new ArrayList<ReturnRow>();
            for (int i = 0; i < conditionPaths.size(); i++) {
                ConditionImpl condition = new ConditionImpl();
                condition.setParameterIndex(i);
                condition.setExpressions(new ArrayList<ExpressionImpl>());
                newVerticalConditions.add(condition);
            }

            if (conditionPaths.get(0).isVertical()) {
                int rows = verticalConditions.get(0).getExpressions().size();
                for (int row = 0; row < rows; row++) {
                    if (horizontalSize == 0) {
                        for (int paramIndex = 0; paramIndex < conditionPaths.size(); paramIndex++) {
                            int conditionIndex = conditionPaths.get(paramIndex).getIndex();
                            ExpressionImpl expression = verticalConditions.get(conditionIndex).getExpressions().get(row);
                            newVerticalConditions.get(paramIndex).getExpressions().add(expression);
                        }

                        fillNewReturnValues(source, newReturnValues, row, 0);
                    } else {
                        int columns = horizontalConditions.get(0).getExpressions().size();
                        for (int column = 0; column < columns; column++) {
                            fillNewVerticalConditions(source, conditionPaths, newVerticalConditions, row, column);
                            fillNewReturnValues(source, newReturnValues, row, column);
                        }
                    }
                }
            } else {
                int columns = horizontalConditions.get(0).getExpressions().size();
                for (int column = 0; column < columns; column++) {
                    if (verticalSize == 0) {
                        for (int paramIndex = 0; paramIndex < conditionPaths.size(); paramIndex++) {
                            int conditionIndex = conditionPaths.get(paramIndex).getIndex();
                            ExpressionImpl expression = horizontalConditions.get(conditionIndex).getExpressions().get(
                                    column);
                            newVerticalConditions.get(paramIndex).getExpressions().add(expression);
                        }

                        fillNewReturnValues(source, newReturnValues, 0, column);
                    } else {
                        int rows = verticalConditions.get(0).getExpressions().size();
                        for (int row = 0; row < rows; row++) {
                            fillNewVerticalConditions(source, conditionPaths, newVerticalConditions, row, column);
                            fillNewReturnValues(source, newReturnValues, row, column);
                        }
                    }
                }
            }

            TableImpl table = new TableImpl();
            table.setSegment((SegmentImpl) source.getSegment());
            table.setName(source.getName());
            table.setAttributes(source.getAttributes());
            table.setReturnType(source.getReturnType());
            table.setParameters(new ArrayList<ParameterImpl>(source.getParameters()));
            table.setHorizontalConditions(new ArrayList<ConditionImpl>());
            table.setVerticalConditions(newVerticalConditions);
            table.setReturnValues(newReturnValues);

            return table;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            OpenLMessagesUtils.addError(e);
            return source;
        }
    }

    private void fillNewReturnValues(Table source, ArrayList<ReturnRow> newReturnValues, int row, int column) {
        ReturnRow newReturnRow = new ReturnRow();
        ExpressionImpl expression = source.getReturnValues().get(row).getList().get(column);
        newReturnRow.setList(Collections.singletonList(expression));

        newReturnValues.add(newReturnRow);
    }

    private void fillNewVerticalConditions(Table source,
            List<ConditionPath> conditionPaths,
            ArrayList<ConditionImpl> newVerticalConditions, int row, int column) {
        List<ConditionImpl> verticalConditions = source.getVerticalConditions();
        List<ConditionImpl> horizontalConditions = source.getHorizontalConditions();

        for (int paramIndex = 0; paramIndex < conditionPaths.size(); paramIndex++) {
            ConditionPath conditionPath = conditionPaths.get(paramIndex);
            int conditionIndex = conditionPath.getIndex();

            ExpressionImpl expression;
            if (conditionPath.isVertical()) {
                expression = verticalConditions.get(conditionIndex).getExpressions().get(row);
            } else {
                expression = horizontalConditions.get(conditionIndex).getExpressions().get(column);
            }

            newVerticalConditions.get(paramIndex).getExpressions().add(expression);
        }
    }

    private Table sortReturnCells(Table source) {
        try {
            TableImpl table = new TableImpl();
            table.setSegment((SegmentImpl) source.getSegment());
            table.setName(source.getName());
            table.setAttributes(source.getAttributes());
            table.setReturnType(source.getReturnType());
            table.setParameters(new ArrayList<ParameterImpl>(source.getParameters()));
            table.setHorizontalConditions(new ArrayList<ConditionImpl>(source.getHorizontalConditions()));
            table.setVerticalConditions(new ArrayList<ConditionImpl>(source.getVerticalConditions()));
            table.setReturnValues(new ArrayList<ReturnRow>(source.getReturnValues()));

            final List<ConditionImpl> verticalConditions = table.getVerticalConditions();
            final List<ConditionImpl> horizontalConditions = table.getHorizontalConditions();
            List<ReturnRow> returnValues = table.getReturnValues();

            List<Integer> rowNumbers = new ArrayList<Integer>();
            List<Integer> columnNumbers = new ArrayList<Integer>();
            for (int i = 0; i < returnValues.size(); i++) {
                rowNumbers.add(i);
            }
            for (int i = 0; i < returnValues.get(0).getList().size(); i++) {
                columnNumbers.add(i);
            }

            if (verticalConditions.size() > 0) {
                Collections.sort(rowNumbers, new ConditionsComparator(verticalConditions));
            }

            if (horizontalConditions.size() > 0) {
                Collections.sort(columnNumbers, new ConditionsComparator(horizontalConditions));
            }

            for (ConditionImpl condition : verticalConditions) {
                List<ExpressionImpl> oldExpressions = condition.getExpressions();
                List<ExpressionImpl> newExpressions = new ArrayList<ExpressionImpl>();
                for (Integer rowNumber : rowNumbers) {
                    newExpressions.add(oldExpressions.get(rowNumber));
                }
                condition.setExpressions(newExpressions);
            }

            for (ConditionImpl condition : horizontalConditions) {
                List<ExpressionImpl> oldExpressions = condition.getExpressions();
                List<ExpressionImpl> newExpressions = new ArrayList<ExpressionImpl>();
                for (Integer rowNumber : columnNumbers) {
                    newExpressions.add(oldExpressions.get(rowNumber));
                }
                condition.setExpressions(newExpressions);
            }

            List<ReturnRow> newReturnValues = new ArrayList<ReturnRow>();
            for (Integer rowNumber : rowNumbers) {
                newReturnValues.add(returnValues.get(rowNumber));
            }
            returnValues = newReturnValues;

            for (ReturnRow returnRow : returnValues) {
                List<ExpressionImpl> oldExpressions = returnRow.getList();
                List<ExpressionImpl> newExpressions = new ArrayList<ExpressionImpl>();
                for (Integer rowNumber : columnNumbers) {
                    newExpressions.add(oldExpressions.get(rowNumber));
                }
                returnRow.setList(newExpressions);
            }

            table.setReturnValues(returnValues);

            return table;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            OpenLMessagesUtils.addError(e);
            return source;
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
                String returnType = "Object"; // Until it will be fixed on LE side
//                String returnType = function.getReturnType();
//                if (StringUtils.isBlank(returnType)) {
//                    returnType = "Object";
//                }

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
                        type = "Object";
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

                List<Attribute> attributes = function.getAttributes();
                int width = attributes.isEmpty() ? 1 : 3;

                gridBuilder.addCell(headerBuilder.toString(), width).nextRow();

                addAttributes(gridBuilder, attributes);

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
                    if (cell.getHasArrayFormula()) {
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
                    if (cell.getHasArrayFormula()) {
                        // Array cells are handled differently
                        continue;
                    }

                    try {
                        CellReference reference = CellReference.parse(workbookName, sheetName, cell.getAddress());
                        List<String> currentRow = getCurrentRow(conditions, reference);
                        int currentColumnNumber = getCurrentColumnNumber(columnNumbers, reference);

                        ExpressionContext expressionContext = new ExpressionContext();
                        expressionContext.setCurrentRow(reference.getRowNumber());
                        expressionContext.setCurrentColumn(reference.getColumnNumber());
                        expressionContext.setCanHandleArrayOperators(false);
                        ExpressionContext.setInstance(expressionContext);

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
                            node.setRootNode(true);
                            if (node instanceof ValueHolder) {
                                expression = ((ValueHolder) node).asString();
                            } else {
                                expression = "= " + node.toOpenLString();
                            }
                        } catch (RuntimeException e) {
                            expression = "";
                            log.error(e.getMessage(), e);
                            addError(workbookName, sheetName, cell, e);
                        }
                        currentRow.set(currentColumnNumber, expression);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        addError(workbookName, sheetName, cell, e);
                    } finally {
                        ExpressionContext.removeInstance();
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

    private void addError(String workbookName, String sheetName, Cell cell, Exception e) {
        RangeNode address = cell.getAddress();
        String errorMessage = String.format("Error in cell [%s]%s!R%sC%s : %s",
                workbookName,
                sheetName,
                address.getRow(),
                address.getColumn(),
                e.getMessage());
        OpenLMessagesUtils.addError(errorMessage);
    }

    private void initNamedRanges(Sheet sheet) {
        try {
            if (sheet instanceof SheetHolder && ((SheetHolder) sheet).getInternalSheet() != null) {
                sheet = ((SheetHolder) sheet).getInternalSheet();
            }
            if (CollectionUtils.isEmpty(sheet.getCells())) {
                return;
            }
            ProjectData projectData = ProjectData.getCurrentInstance();

            for (LazyCells cells : sheet.getCells()) {
                for (NamedRange namedRange : cells.getNamedRanges()) {
                    projectData.addNamedRange(namedRange.getName(), namedRange.getRange());
                }
            }
        } catch (RuntimeException e) {
            log.error(e.getMessage(), e);
            OpenLMessagesUtils.addError(e);
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
                    if (!cell.getHasArrayFormula()) {
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
        try {
            final String workbookName = sheet.getWorkbookName();
            final String sheetName = sheet.getName();

            CellReference start = CellReference.parse(workbookName, sheetName, cell.getAddress());
            CellReference end = CellReference.parse(workbookName, sheetName, cell.getEndAddress());
            String tableName = new RulesTableReference(start, end).getTable();

            int startColumn = start.getColumnNumber();
            int startRow = start.getRowNumber();
            int endColumn = end.getColumnNumber();
            int endRow = end.getRowNumber();

            ExpressionContext expressionContext = new ExpressionContext(startRow, startColumn, endRow, endColumn);
            expressionContext.setCanHandleArrayOperators(true);
            ExpressionContext.setInstance(expressionContext);

            int columnsCount = endColumn - startColumn + 3;
            // TODO Add array operations parameters count

            Node node = cell.getNode();
            if (node == null) {
                throw new IllegalArgumentException("Cell [" + workbookName + "]" + sheetName + "!" + cell
                        .getAddress()
                        .toOpenLString() + " contains incorrect value. It will be skipped");
            }

            node.setRootNode(true);

            boolean isOutFunction = node instanceof FunctionNode && "Out".equals(((FunctionNode) node).getName());

            gridBuilder.addCell("Spreadsheet SpreadsheetResult " + tableName + "()", columnsCount).nextRow();

            gridBuilder.addCell("#");
            gridBuilder.addCell("Calculation");
            for (int i = startColumn; i <= endColumn; i++) {
                gridBuilder.addCell("C" + i + " : Object");
            }
            gridBuilder.nextRow();

            CellInspector.NodeSize nodeSize = CellInspector.inspect(cell.getNode(), true);
            int resultRows = nodeSize.getResultHeight();
            int resultColumns = nodeSize.getResultWidth();

            if (resultColumns == 1) {
                gridBuilder.addCell("Result : Object[]").addCell("= new Object[" + resultRows + "]");
            } else {
                gridBuilder.addCell("Result : Object[][]")
                        .addCell("= new Object[" + resultRows + "][" + resultColumns + "]");
            }
            gridBuilder.nextRow();

            for (int step = 0; step < resultRows; step++) {
                gridBuilder.addCell("Step" + step + " : Object");

                for (int column = startColumn; column < startColumn + resultColumns; column++) {
                    expressionContext.setCurrentRow(startRow + step);
                    expressionContext.setCurrentColumn(column);

                    String expression;
                    try {
                        if (node instanceof ValueHolder) {
                            expression = ((ValueHolder) node).asString();
                        } else {
                            String formula = isOutFunction ? ((FunctionNode) node).getArguments().get(0).toOpenLString() : node.toOpenLString();

                            if (resultColumns == 1) {
                                expression = "= $Calculation$Result[" + step + "] = " + formula;
                            } else {
                                expression = "= $Calculation$Result[" + step + "][" + (column - startColumn) + "] = " + formula;
                            }
                        }
                    } catch (RuntimeException e) {
                        expression = "";
                        log.error(e.getMessage(), e);
                        addError(workbookName, sheetName, cell, e);
                    }
                    gridBuilder.addCell(expression);
                }
                gridBuilder.nextRow();
            }

            expressionContext.setOutArray(isOutFunction);

            // FIXME Here is simple case only
            for (int row = startRow; row <= endRow; row++) {
                gridBuilder.addCell("R" + row);
                gridBuilder.addCell(null);

                for (int column = startColumn; column <= endColumn; column++) {
                    expressionContext.setCurrentRow(row);
                    expressionContext.setCurrentColumn(column);

                    String expression;
                    try {
                        if (node instanceof ValueHolder) {
                            expression = ((ValueHolder) node).asString();
                        } else {
                            if (isOutFunction) {
                                expression = "= " + node.toOpenLString();
                            } else {
                                int rowShift = row - startRow;
                                int columnShift = column - startColumn;
                                // TODO Replace Out() function with OutArray (the function that returns Object, not String)
                                expression = "= Out(" + rowShift + ", " + columnShift + ", $Calculation$Result)";
                            }
                        }
                    } catch (RuntimeException e) {
                        expression = "";
                        log.error(e.getMessage(), e);
                        addError(workbookName, sheetName, cell, e);
                    }
                    gridBuilder.addCell(expression);
                }
                gridBuilder.nextRow();
            }

            gridBuilder.nextRow();
        } finally {
            ExpressionContext.removeInstance();
        }
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
        } finally {
            ProjectData.clearUnmarshaller();
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
        ProjectData projectData = ProjectData.getCurrentInstance();
        for (LazyWorkbook workbook : module.getInternalWorkbooks()) {
            for (Sheet s : workbook.getSheets()) {
                for (Type type : s.getTypes()) {
                    projectData.addType(type);
                }
                for (Function function : s.getFunctions()) {
                    projectData.addFunction(function);
                }
                for (Table table : s.getTables()) {
                    projectData.addTable(table);
                }
            }
        }
    }

    protected List<String> getImports() {
        return Collections.singletonList("org.openl.rules.enumeration");
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

            for (Sheet sheet : sheets) {
                initNamedRanges(sheet);
            }

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
                tsn = new TableSyntaxNode(XlsNodeTypes.XLS_OTHER.toString(),
                        tsn.getGridLocation(),
                        source,
                        table,
                        tsn.getHeader());
                SyntaxNodeException sne = SyntaxNodeExceptionUtils.createError(t, tsn);
                tsn.addError(sne);
                OpenLMessagesUtils.addError(sne);
            }
        }
        return tsn;
    }

    private static class ConditionsComparator implements Comparator<Integer> {
        private final List<ConditionImpl> conditions;

        public ConditionsComparator(List<ConditionImpl> conditions) {
            this.conditions = conditions;
        }

        @Override
        public int compare(Integer o1, Integer o2) {
            for (ConditionImpl condition : conditions) {
                String v1 = condition.getExpressions().get(o1).getValue();
                String v2 = condition.getExpressions().get(o2).getValue();
                if (!v1.equals(v2)) {
                    if ("*".equals(v1)) {
                        return 1;
                    }
                    if ("*".equals(v2)) {
                        return -1;
                    }
                    return 0;
                }
            }
            throw new IllegalStateException("All conditions are equal");
        }
    }

    private static class ConditionPath {
        private final boolean vertical;
        private final int index;

        private ConditionPath(boolean vertical, int index) {
            this.vertical = vertical;
            this.index = index;
        }

        public boolean isVertical() {
            return vertical;
        }

        public int getIndex() {
            return index;
        }
    }
}
