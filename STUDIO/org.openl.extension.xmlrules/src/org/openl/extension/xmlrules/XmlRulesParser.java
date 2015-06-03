package org.openl.extension.xmlrules;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openl.exception.OpenLCompilationException;
import org.openl.extension.ExtensionParser;
import org.openl.extension.Serializer;
import org.openl.extension.xmlrules.syntax.StringGridBuilder;
import org.openl.extension.xmlrules.model.*;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.IGridTable;
import org.openl.source.IOpenSourceCodeModule;

public class XmlRulesParser extends ExtensionParser {
    public XmlRulesParser() {
    }

    @Override
    protected ExtensionModule load(IOpenSourceCodeModule source) {
        Serializer<ExtensionModule> serializer = new SingleFileXmlSerializer();
        return serializer.deserialize(source.getByteStream());
    }

    /**
     * Wrap source to XlsWorkbookSourceCodeModule
     */
    @Override
    protected XlsWorkbookSourceCodeModule getWorkbookSourceCodeModule(ExtensionModule extensionModule, IOpenSourceCodeModule source) throws
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
    @Override
    protected IGridTable[] getAllGridTables(XlsSheetSourceCodeModule sheetSource, TableGroup tableGroup) {
        String uri = sheetSource.getUri();
        // TODO Improve LaunchFileServlet to support real ranges
        LazyXmlRulesWorkbookLoader workbookLoader = (LazyXmlRulesWorkbookLoader) sheetSource.getWorkbookSource().getWorkbookLoader();

        StringGridBuilder gridBuilder = new StringGridBuilder(uri, workbookLoader.getExtensionModule().getXlsFileName());

        createTypes(gridBuilder, tableGroup);
        createDataInstances(gridBuilder, tableGroup);
        createTables(gridBuilder, tableGroup);
        createFunctions(gridBuilder, tableGroup);

        return gridBuilder.build().getTables();
    }

    private void createTypes(StringGridBuilder gridBuilder, TableGroup tableGroup) {
        if (tableGroup.getTypes() == null) {
            return;
        }
        for (Type type : tableGroup.getTypes()) {
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
    }

    private void createDataInstances(StringGridBuilder gridBuilder, TableGroup tableGroup) {
        if (tableGroup.getDataInstances() == null) {
            return;
        }
        for (DataInstance dataInstance : tableGroup.getDataInstances()) {
            gridBuilder.addCell("Data " + dataInstance.getType() + " " + dataInstance.getName(),
                    dataInstance.getFields().size()).nextRow();
            // Fields
            boolean hasReferences = false;
            for (Field field : dataInstance.getFields()) {
                gridBuilder.addCell(field.getName());
                if (!StringUtils.isBlank(field.getReference())) {
                    hasReferences = true;
                }
            }
            gridBuilder.nextRow();

            // References
            if (hasReferences) {
                for (Field field : dataInstance.getFields()) {
                    String reference = field.getReference();

                    if (!StringUtils.isBlank(reference)) {
                        gridBuilder.addCell(">" + reference);
                    } else {
                        gridBuilder.addCell(null);
                    }
                }
                gridBuilder.nextRow();
            }

            // Business names
            for (Field field : dataInstance.getFields()) {
                gridBuilder.addCell(field.getName().toUpperCase());
            }
            gridBuilder.nextRow();

            for (List<String> row : dataInstance.getValues()) {
                for (String value : row) {
                    gridBuilder.addCell(value);
                }
                gridBuilder.nextRow();
            }

            gridBuilder.nextRow();
        }
    }

    private void createTables(StringGridBuilder gridBuilder, TableGroup tableGroup) {
        if (tableGroup.getTables() == null) {
            return;
        }
        for (Table table : tableGroup.getTables()) {
            int tableRow = gridBuilder.getRow();

            boolean isSimpleRules = table.getHorizontalConditions().isEmpty();
            String tableType = isSimpleRules ? "SimpleRules" : "SimpleLookup";
            StringBuilder header = new StringBuilder();
            String returnType = table.getReturnType();
            if (StringUtils.isBlank(returnType)) {
                returnType = "void";
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

            gridBuilder.addCell(header.toString(), table.getRegion().getWidth());
            gridBuilder.nextRow();

            int startColumn = gridBuilder.getStartColumn();

            // HC expressions
            gridBuilder.setStartColumn(startColumn + table.getVerticalConditions().size());

            for (Condition condition : table.getHorizontalConditions()) {
                for (ConditionExpression expression : condition.getExpressions()) {
                    XlsRegion region = expression.getRegion();
                    gridBuilder.addCell(expression.getExpression(), region == null ? 1 : region.getWidth());
                }
                gridBuilder.nextRow();
            }
            gridBuilder.setStartColumn(startColumn);

            // VC header
            if (isSimpleRules) {
                for (Parameter parameter : table.getParameters()) {
                    gridBuilder.addCell(parameter.getName());
                }
                gridBuilder.addCell("Return");
                gridBuilder.nextRow();
            } else {
                List<Parameter> parameters = table.getParameters();
                for (int i = 0; i < parameters.size(); i++) {
                    if (i >= table.getVerticalConditions().size()) {
                        break;
                    }
                    Parameter parameter = parameters.get(i);
                    gridBuilder.setCell(gridBuilder.getColumn(),
                            tableRow + 1,
                            1,
                            table.getHorizontalConditions().size(),
                            parameter.getName());
                }
            }

            // VC expressions
            int conditionRow = gridBuilder.getRow();
            int conditionColumn = gridBuilder.getColumn();
            for (Condition condition : table.getVerticalConditions()) {
                int row = conditionRow;
                for (ConditionExpression expression : condition.getExpressions()) {
                    XlsRegion region = expression.getRegion();
                    gridBuilder.setCell(conditionColumn,
                            row,
                            region == null ? 1 : region.getWidth(),
                            region == null ? 1 : region.getHeight(),
                            expression.getExpression());
                    row += region == null ? 1 : region.getHeight();
                }
                conditionColumn++;
            }

            // Return values
            if (isSimpleRules) {
                gridBuilder.setRow(tableRow + 2);
                gridBuilder.setStartColumn(conditionColumn);
                for (ReturnValue returnValue : table.getReturnValues().get(0)) {
                    gridBuilder.addCell(returnValue.getValue()).nextRow();
                }
            } else {
                gridBuilder.setRow(tableRow + 1 + table.getHorizontalConditions().size());
                gridBuilder.setStartColumn(startColumn + table.getVerticalConditions().size());
                for (List<ReturnValue> returnValues : table.getReturnValues()) {
                    for (ReturnValue returnValue : returnValues) {
                        gridBuilder.addCell(returnValue.getValue());
                    }
                    gridBuilder.nextRow();
                }
            }
            gridBuilder.setStartColumn(startColumn);
            gridBuilder.nextRow();
        }
    }

    private void createFunctions(StringGridBuilder gridBuilder, TableGroup tableGroup) {
        if (tableGroup.getFunctions() == null) {
            return;
        }
        for (Function function : tableGroup.getFunctions()) {
            StringBuilder headerBuilder = new StringBuilder();
            String returnType = function.getReturnType();
            if (StringUtils.isBlank(returnType)) {
                returnType = "void";
            }
            headerBuilder.append("Spreadsheet ")
                    .append(returnType)
                    .append(' ')
                    .append(function.getName())
                    .append('(');
            List<Parameter> parameters = function.getParameters();
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) {
                    headerBuilder.append(", ");
                }
                Parameter parameter = parameters.get(i);
                String type = parameter.getType();
                if (StringUtils.isBlank(type)) {
                    type = "String";
                }
                headerBuilder.append(type)
                        .append(" ")
                        .append(parameter.getName());
            }
            headerBuilder.append(')');
            gridBuilder.addCell(headerBuilder.toString(), 2).nextRow();

            gridBuilder.addCell("Step").addCell("Formula").nextRow();

            List<FunctionExpression> expressions = function.getExpressions();
            for (int i = 0; i < expressions.size(); i++) {
                FunctionExpression expression = expressions.get(i);
                String step = expression.getStepName();
                if (expression.getStepType() != null) {
                    step += " : " + expression.getStepType();
                } else if (StringUtils.isBlank(step) && i == expressions.size() - 1) {
                    step = "RETURN";
                }

                gridBuilder.addCell(step).addCell("=" + StringUtils.trim(expression.getExpression())).nextRow();
            }

            gridBuilder.nextRow();
        }
    }
}
