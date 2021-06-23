package org.openl.rules.rest;

import java.util.ArrayList;
import java.util.List;

import org.openl.exception.OpenLException;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLWarnMessage;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.tableeditor.TableBean;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethod;
import org.openl.util.StringUtils;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInfo;

public class OpenLTableLogic {

    public static String[] getErrorCode(ILocation location, String sourceCode) {
        String code = StringUtils.isBlank(sourceCode) ? StringUtils.EMPTY : sourceCode;

        int pstart = 0;
        int pend = code.length();

        if (StringUtils.isNotBlank(code) && location != null && location.isTextLocation()) {
            TextInfo info = new TextInfo(code);
            pstart = location.getStart().getAbsolutePosition(info);
            pend = Math.min(location.getEnd().getAbsolutePosition(info) + 1, code.length());
        }

        if (pend != 0) {
            return new String[] { code.substring(0, pstart), code.substring(pstart, pend), code.substring(pend) };
        }

        return new String[0];
    }

    public static List<TableBean.TableDescription> getTargetTables(IOpenLTable table,
            ProjectModel model,
            WebStudio webStudio) {
        List<TableBean.TableDescription> targetTables = new ArrayList<>();
        String tableType = table.getType();
        if (tableType.equals(XlsNodeTypes.XLS_TEST_METHOD.toString()) || tableType
            .equals(XlsNodeTypes.XLS_RUN_METHOD.toString())) {
            IOpenMethod method = model.getMethod(table.getUri());
            if (method instanceof TestSuiteMethod) {
                List<IOpenMethod> targetMethods = new ArrayList<>();
                IOpenMethod testedMethod = ((TestSuiteMethod) method).getTestedMethod();

                // Overloaded methods
                if (testedMethod instanceof OpenMethodDispatcher) {
                    List<IOpenMethod> overloadedMethods = ((OpenMethodDispatcher) testedMethod).getCandidates();
                    targetMethods.addAll(overloadedMethods);
                } else {
                    targetMethods.add(testedMethod);
                }

                for (IOpenMethod targetMethod : targetMethods) {
                    IMemberMetaInfo methodInfo = targetMethod.getInfo();
                    if (methodInfo != null) {
                        TableSyntaxNode tsn = (TableSyntaxNode) methodInfo.getSyntaxNode();
                        IOpenLTable targetTable = new TableSyntaxNodeAdapter(tsn);
                        targetTables.add((new TableBean.TableDescription(webStudio.url(targetTable.getUri()),
                            targetTable.getId(),
                            getTableName(targetTable))));
                    }
                }
            }
        }
        return targetTables;
    }
    public static List<OpenlProblemMessage> processTableProblems(List<OpenLMessage> messages, ProjectModel model) {
        List<OpenlProblemMessage> problems = new ArrayList<>();
        for (OpenLMessage message : messages) {
            ILocation location = null;
            String sourceCode = null;
            boolean hasStackTrace = false;
            String errorUri = message.getSourceLocation();
            IOpenSourceCodeModule module = null;
            String code = null;
            String messageNodeId = model.getMessageNodeId(message);
            if (message instanceof OpenLErrorMessage) {
                OpenLErrorMessage errorMessage = (OpenLErrorMessage) message;
                hasStackTrace = errorMessage.getError() != null;
                OpenLException error = errorMessage.getError();
                location = error.getLocation();
                sourceCode = error.getSourceCode();
                code = error.getSourceCode();
            } else if (message instanceof OpenLWarnMessage) {
                OpenLWarnMessage warnMessage = (OpenLWarnMessage) message;
                ISyntaxNode source = warnMessage.getSource();
                location = source.getSourceLocation();
                sourceCode = source.getModule() == null ? null : source.getModule().getCode();
                module = source.getModule();
            }
            if (module != null) {
                code = module.getCode();
            }
            String[] errorCode = OpenLTableLogic.getErrorCode(location, sourceCode);
            boolean hasLinkToCell = errorUri != null && (code != null || module instanceof StringSourceCodeModule);
            String cell = errorUri != null ? new XlsUrlParser(errorUri).getCell() : null;
            problems.add(new OpenlProblemMessage(message
                .getSummary(), hasStackTrace, errorCode, hasLinkToCell, messageNodeId, cell, errorUri));
        }
        return problems;
    }

    private static String getTableName(IOpenLTable table) {
        String[] dimensionProps = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
        ITableProperties tableProps = table.getProperties();
        StringBuilder dimensionBuilder = new StringBuilder();
        String tableName = table.getDisplayName();
        if (tableProps != null) {
            for (String dimensionProp : dimensionProps) {
                String propValue = tableProps.getPropertyValueAsString(dimensionProp);

                if (propValue != null && !propValue.isEmpty()) {
                    dimensionBuilder.append(dimensionBuilder.length() == 0 ? "" : ", ")
                        .append(dimensionProp)
                        .append(" = ")
                        .append(propValue);
                }
            }
        }
        if (dimensionBuilder.length() > 0) {
            return tableName + " [" + dimensionBuilder + "]";
        } else {
            return tableName;
        }
    }
}
