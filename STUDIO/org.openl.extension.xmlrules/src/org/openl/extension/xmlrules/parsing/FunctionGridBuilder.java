package org.openl.extension.xmlrules.parsing;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.extension.xmlrules.model.ExtensionModule;
import org.openl.extension.xmlrules.model.Function;
import org.openl.extension.xmlrules.model.Parameter;
import org.openl.extension.xmlrules.model.Segment;
import org.openl.extension.xmlrules.model.Sheet;
import org.openl.extension.xmlrules.model.single.Attribute;
import org.openl.extension.xmlrules.model.single.ParameterImpl;
import org.openl.extension.xmlrules.model.single.SheetHolder;
import org.openl.extension.xmlrules.syntax.StringGridBuilder;
import org.openl.extension.xmlrules.utils.CellReference;
import org.openl.extension.xmlrules.utils.HelperFunctions;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FunctionGridBuilder {
    private FunctionGridBuilder() {
    }

    public static void build(StringGridBuilder gridBuilder, ExtensionModule module, Sheet sheet, Collection<OpenLMessage> messages) {
        Logger log = LoggerFactory.getLogger(FunctionGridBuilder.class);
        if (sheet instanceof SheetHolder && ((SheetHolder) sheet).getInternalSheet() != null) {
            sheet = ((SheetHolder) sheet).getInternalSheet();
        }
        if (sheet.getFunctions() == null) {
            return;
        }
        Set<String> functionNamesWithAttributes = new HashSet<String>();
        for (Function function : sheet.getFunctions()) {
            if (!function.getAttributes().isEmpty()) {
                functionNamesWithAttributes.add(function.getName());
            }
        }
        for (Function function : sheet.getFunctions()) {
            try {
                Segment segment = function.getSegment();
                if (segment != null && segment.getTotalSegments() == 1) {
                    segment = null;
                }
                if (segment != null && !functionNamesWithAttributes.contains(function.getName())) {
                    String message = "Function " + function.getName() + " with several segments but without attributes";
                    log.warn(message);
                    messages.add(OpenLMessagesUtils.newWarnMessage(message));
                }

                String cellAddress = function.getCellAddress();
                boolean isRange = cellAddress.contains(":");

                String returnType = getReturnType(function, isRange);

                List<ParameterImpl> parameters = function.getParameters();
                String workbookName = sheet.getWorkbookName();
                String sheetName = sheet.getName();

                writeHeader(gridBuilder, function, returnType, parameters, workbookName, sheetName);

                List<Attribute> attributes = function.getAttributes();
                GridBuilderUtils.addAttributes(gridBuilder, attributes);

                writeBody(gridBuilder, module, cellAddress, isRange, returnType, parameters, workbookName, sheetName, messages);

                gridBuilder.nextRow();
            } catch (RuntimeException e) {
                log.error(e.getMessage(), e);
                messages.addAll(OpenLMessagesUtils.newErrorMessages(e));
                gridBuilder.nextRow();
            }
        }
    }

    private static void writeHeader(StringGridBuilder gridBuilder,
            Function function,
            String returnType,
            List<ParameterImpl> parameters, String workbookName, String sheetName) {
        List<Attribute> attributes = function.getAttributes();
        int width = attributes.isEmpty() ? 1 : 3;

        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("Method ")
                .append(returnType)
                .append(' ')
                .append(function.getName())
                .append('(');
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                headerBuilder.append(", ");
            }
            Parameter parameter = parameters.get(i);
            String type = HelperFunctions.convertToOpenLType(parameter.getType());
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

        gridBuilder.addCell(headerBuilder.toString(), width).nextRow();
    }

    private static void writeBody(StringGridBuilder gridBuilder,
            ExtensionModule module,
            String cellAddress,
            boolean isRange,
            String returnType,
            List<ParameterImpl> parameters, String workbookName, String sheetName, Collection<OpenLMessage> messages) {
        for (ParameterImpl parameter : parameters) {
            CellReference reference = CellReference.parse(workbookName, sheetName, parameter.getName());
            String cell;
            if (isSameSheet(reference, workbookName, sheetName)) {
                cell = String.format("Push(%d, %d, R%sC%s);",
                        reference.getRowNumber(),
                        reference.getColumnNumber(),
                        reference.getRow(),
                        reference.getColumn());
            } else {
                cell = String.format("Push(\"%s\", R%sC%s);",
                        reference.getStringValue(),
                        reference.getRow(),
                        reference.getColumn());
            }
            gridBuilder.addCell(cell).nextRow();
        }

        String cellRetrieveString;
        if (isRange) {
            String[] addresses = cellAddress.split(":");
            CellReference left = CellReference.parse(workbookName, sheetName, addresses[0]);
            CellReference right = CellReference.parse(workbookName, sheetName, addresses[1]);
            cellRetrieveString = String.format("CellRange(%d, %d, %d, %d)",
                    left.getRowNumber(),
                    left.getColumnNumber(),
                    right.getRowNumber() - left.getRowNumber() + 1,
                    right.getColumnNumber() - left.getColumnNumber() + 1);
        } else {
            CellReference cellReference = CellReference.parse(workbookName, sheetName, cellAddress);
            try {
                cellRetrieveString = GridBuilderUtils.getCellExpression(module, workbookName, sheetName, cellReference);
            } catch (RuntimeException e) {
                Logger log = LoggerFactory.getLogger(FunctionGridBuilder.class);
                log.error(e.getMessage(), e);
                messages.addAll(OpenLMessagesUtils.newErrorMessages(e));
                cellRetrieveString = String.format("Cell(\"%s\", \"%s\", %d, %d)",
                        cellReference.getWorkbook(),
                        cellReference.getSheet(),
                        cellReference.getRowNumber(),
                        cellReference.getColumnNumber());
            }
        }

        String componentType = returnType.replace("[]", "");

        cellRetrieveString = GridBuilderUtils.wrapWithConvertFunctionIfNeeded(returnType,
                componentType,
                isRange,
                cellRetrieveString);
        String expression = String.format("%s result = %s;", returnType, cellRetrieveString);

        gridBuilder.addCell(expression);
        gridBuilder.nextRow();

        for (ParameterImpl parameter : parameters) {
            CellReference reference = CellReference.parse(workbookName, sheetName, parameter.getName());
            String cell;
            if (isSameSheet(reference, workbookName, sheetName)) {
                cell = String.format("Pop(%d, %d);", reference.getRowNumber(), reference.getColumnNumber());
            } else {
                cell = String.format("Pop(\"%s\");", reference.getStringValue());
            }
            gridBuilder.addCell(cell).nextRow();
        }

        gridBuilder.addCell("return result;").nextRow();
    }

    private static boolean isSameSheet(CellReference reference, String workbookName, String sheetName) {
        return reference.getWorkbook().equals(workbookName) && reference.getSheet().equals(sheetName);
    }

    private static String getReturnType(Function function, boolean isRange) {
        String returnType = HelperFunctions.convertToOpenLType(function.getReturnType());
        if (StringUtils.isBlank(returnType)) {
            returnType = "Object";
        }

        if (isRange && !returnType.endsWith("[][]")) {
            returnType += "[][]";
        } else if (!"Object".equals(returnType) && !returnType.endsWith("[]")){
            // TODO: Remove it when it will be possible to choose if the type is an array on LE side
            returnType += "[]";
        }
        return returnType;
    }
}
