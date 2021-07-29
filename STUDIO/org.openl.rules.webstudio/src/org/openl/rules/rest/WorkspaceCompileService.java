package org.openl.rules.rest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.tuple.Pair;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.model.Module;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.ui.ProjectCompilationStatus;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.tableeditor.TableBean;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenMethod;
import org.openl.util.CollectionUtils;
import org.springframework.stereotype.Service;

@Service
@Path("/compile/")
@Produces(MediaType.APPLICATION_JSON)
public class WorkspaceCompileService {

    private static final int MAX_PROBLEMS = 100;

    @GET
    @Path("progress/{messageId}/{messageIndex}")
    public Map<String, Object> getCompile(@PathParam("messageId") final Long messageId,
            @PathParam("messageIndex") final Integer messageIndex) {
        Map<String, Object> compileModuleInfo = new HashMap<>();
        WebStudio webStudio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        if (webStudio != null) {
            ProjectModel model = webStudio.getModel();
            ProjectCompilationStatus status = model.getCompilationStatus();
            List<MessageDescription> messages = status.getMessages()
                    .stream()
                    .map(message -> new MessageDescription(message.getId(), message.getSummary(), message.getSeverity()))
                    .collect(Collectors.toList());
            compileModuleInfo.put("dataType", "new");
            if (messageIndex != -1 && messageId != -1 && messageIndex < messages.size()) {
                MessageDescription messageDescription = messages.get(messageIndex);
                if (messageDescription.getId() == messageId) {
                    messages = messages.subList(messageIndex + 1, messages.size());
                    compileModuleInfo.put("dataType", "add");
                }
            }
            compileModuleInfo.put("modulesCount", status.getModulesCount());
            compileModuleInfo.put("modulesCompiled", status.getModulesCompiled());
            compileModuleInfo.put("messages", messages);
            compileModuleInfo.put("messageId", messages.isEmpty() ? -1 : messages.get(messages.size() - 1).getId());
            compileModuleInfo.put("messageIndex", messages.size() - 1);
            compileModuleInfo.put("errorsCount", status.getErrorsCount());
            compileModuleInfo.put("warningsCount", status.getWarningsCount());
            compileModuleInfo.put("compilationCompleted", model.isProjectCompilationCompleted());
        }
        return compileModuleInfo;
    }

    @GET
    @Path("tests/{tableId}")
    public Map<String, Object> getCompile(@PathParam("tableId") final String tableId) {
        Map<String, Object> tableTestsInfo = new HashMap<>();
        WebStudio webStudio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        List<TableBean.TableDescription> tableDescriptions = new ArrayList<>();
        if (webStudio != null) {
            ProjectModel model = webStudio.getModel();
            IOpenLTable table = model.getTableById(tableId);
            if (table != null) {
                IOpenMethod[] allTests = model.getTestAndRunMethods(table.getUri(), false);
                if (allTests != null) {
                    for (IOpenMethod test : allTests) {
                        TableSyntaxNode syntaxNode = (TableSyntaxNode) test.getInfo().getSyntaxNode();
                        tableDescriptions
                            .add(new TableBean.TableDescription(webStudio.url("table", syntaxNode.getUri()),
                                syntaxNode.getId(),
                                getTestName(test)));
                    }
                    tableDescriptions.sort(Comparator.comparing(TableBean.TableDescription::getName));
                }
                tableTestsInfo.put("allTests", tableDescriptions);
                tableTestsInfo.put("compiled", model.isProjectCompilationCompleted());
            }
        }
        return tableTestsInfo;
    }

    @GET
    @Path("tests")
    public Map<String, Object> tests() {
        Map<String, Object> moduleTestsInfo = new HashMap<>();
        WebStudio studio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        ProjectModel model = studio.getModel();
        Module moduleInfo = model.getModuleInfo();
        TestSuiteMethod[] allTestMethods = model.getAllTestMethods();
        moduleTestsInfo.put("count", CollectionUtils.isNotEmpty(allTestMethods) ? allTestMethods.length : 0);
        moduleTestsInfo.put("compiled", model.isProjectCompilationCompleted());
        moduleTestsInfo.put("tableRunState",
            (moduleInfo != null && moduleInfo.getOpenCurrentModuleOnly()) ? TableRunState.CAN_RUN_MODULE
                                                                          : TableRunState.CAN_RUN);
        return moduleTestsInfo;
    }

    @GET
    @Path("project")
    public boolean project() {
        WebStudio webStudio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        return webStudio.getModel().isProjectCompilationCompleted();
    }

    @GET
    @Path("table/{tableId}")
    public Map<String, Object> table(@PathParam("tableId") final String tableId) {
        Map<String, Object> tableInfo = new HashMap<>();
        WebStudio webStudio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        ProjectModel model = webStudio.getModel();
        IOpenLTable table = model.getTableById(tableId);
        Module moduleInfo = model.getModuleInfo();
        boolean projectCompiled = model.isProjectCompilationCompleted();
        TableRunState state = (model.getModuleInfo() != null && moduleInfo
            .getOpenCurrentModuleOnly()) || !projectCompiled ? TableRunState.CAN_RUN_MODULE : TableRunState.CAN_RUN;
        if (table != null) {
            String tableUri = table.getUri();
            List<OpenLMessage> errors = model.getOpenedModuleMessagesByTsn(tableUri, Severity.ERROR);
            List<OpenLMessage> warnings;
            if (!errors.isEmpty()) {
                state = TableRunState.CANNOT_RUN;
            }
            if (projectCompiled) {
                errors = model.getMessagesByTsn(tableUri, Severity.ERROR);
                warnings = model.getMessagesByTsn(tableUri, Severity.WARN);
                if (TableRunState.CAN_RUN.equals(state) && !errors.isEmpty()) {
                    state = TableRunState.CAN_RUN_MODULE;
                }
            } else {
                warnings = model.getOpenedModuleMessagesByTsn(tableUri, Severity.WARN);
            }

            if (warnings.size() >= MAX_PROBLEMS) {
                warnings = warnings.subList(0, MAX_PROBLEMS);
                warnings.add(OpenLMessagesUtils
                    .newErrorMessage("Only first " + MAX_PROBLEMS + " warnings are shown. Fix them first."));
            }
            if (errors.size() >= MAX_PROBLEMS) {
                errors = errors.subList(0, MAX_PROBLEMS);
                errors.add(OpenLMessagesUtils
                    .newErrorMessage("Only first " + MAX_PROBLEMS + " errors are shown. Fix them first."));
            }

            // if the current table is a test then check tested target tables on errors.
            List<Pair<String, TableBean.TableDescription>> targetTableUrlPairs = new ArrayList<>();
            for (TableBean.TableDescription targetTable : OpenLTableLogic.getTargetTables(table, model)) {
                targetTableUrlPairs.add(Pair.of(webStudio.url("table", targetTable.getUri()), targetTable));
                if (!model.getErrorsByUri(targetTable.getUri()).isEmpty()) {
                    warnings.add(new OpenLMessage("Tested rules have errors", Severity.WARN));
                    if (!TableRunState.CANNOT_RUN.equals(state) && model
                        .getOpenedModuleMessagesByTsn(targetTable.getUri(), Severity.ERROR)
                        .isEmpty()) {
                        state = TableRunState.CAN_RUN_MODULE;
                    } else {
                        state = TableRunState.CANNOT_RUN;
                        break;
                    }
                }
            }
            tableInfo.put("errors", OpenLTableLogic.processTableProblems(errors, model, webStudio));
            tableInfo.put("warnings", OpenLTableLogic.processTableProblems(warnings, model, webStudio));
            tableInfo.put("targetTables", targetTableUrlPairs);
            tableInfo.put("tableUrl", webStudio.url("table"));
            tableInfo.put("tableRunState", state);
        }
        return tableInfo;
    }

    private String getTestName(IOpenMethod method) {
        String name = TableSyntaxNodeUtils.getTestName(method);
        String info = ProjectHelper.getTestInfo(method);
        return String.format("%s (%s)", name, info);
    }

    private enum TableRunState {
        CAN_RUN,
        CAN_RUN_MODULE,
        CANNOT_RUN
    }
}
