package org.openl.rules.rest.compile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(value = "/compile/", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Project Compilation")
public class WorkspaceCompileController {

    private static final int MAX_PROBLEMS = 100;

    @Operation(summary = "compile.get-compile.summary", description = "compile.get-compile.desc")
    @GetMapping("progress/{messageId}/{messageIndex}")
    public CompileModuleInfo getCompile(
            @Parameter(description = "compile.get-compile.field.messageId.desc") @PathVariable("messageId") final Long messageId,
            @Parameter(description = "compile.get-compile.field.messageIndex.desc") @PathVariable("messageIndex") final Integer messageIndex) {
        var response = CompileModuleInfo.builder();
        WebStudio webStudio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        if (webStudio != null) {
            ProjectModel model = webStudio.getModel();
            ProjectCompilationStatus status = model.getCompilationStatus();
            List<MessageDescription> messages = status.getAllMessage()
                .stream()
                .map(message -> new MessageDescription(message.getId(), message.getSummary(), message.getSeverity()))
                .collect(Collectors.toList());
            response.dataType("new");
            if (messageIndex != -1 && messageId != -1 && messageIndex < messages.size()) {
                MessageDescription messageDescription = messages.get(messageIndex);
                if (messageDescription.getId() == messageId) {
                    messages = messages.subList(messageIndex + 1, messages.size());
                    response.dataType("add");
                }
            }
            response.messages(messages);
            response.messageId(messages.isEmpty() ? -1 : messages.get(messages.size() - 1).getId());
            response.messageIndex(messages.size() - 1);
            response.errorsCount(status.getErrorsCount());
            response.warningsCount(status.getWarningsCount());
            response.modulesCount(status.getModulesCount());
            response.modulesCompiled(status.getModulesCompiled());
            response.compilationCompleted(
                model.isProjectCompilationCompleted() || model.getModuleInfo() != null && model.getModuleInfo()
                    .getWebstudioConfiguration() != null && model.getModuleInfo()
                        .getWebstudioConfiguration()
                        .isCompileThisModuleOnly());
        }
        return response.build();
    }

    @Operation(summary = "compile.get-compile-test.summary", description = "compile.get-compile-test.desc")
    @GetMapping("tests/{tableId}")
    public TableTestsInfo getCompile(
            @Parameter(description = "compile.get-compile-test.field.tableId.desc") @PathVariable("tableId") final String tableId) {
        var response = TableTestsInfo.builder();
        WebStudio webStudio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        if (webStudio != null) {
            List<TableBean.TableDescription> tableDescriptions = new ArrayList<>();
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
                response.allTests(tableDescriptions);
                response.compiled(!model.isCompilationInProgress());
            }
        }
        return response.build();
    }

    @Operation(summary = "compile.tests.summary", description = "compile.tests.desc")
    @GetMapping("tests")
    public ModuleTestsInfo tests() {
        var response = ModuleTestsInfo.builder();
        WebStudio webStudio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        if (webStudio == null) {
            return response.build();
        }
        ProjectModel model = webStudio.getModel();
        TestSuiteMethod[] allTestMethods = model.getAllTestMethods();
        response.count(CollectionUtils.isNotEmpty(allTestMethods) ? allTestMethods.length : 0);
        response.compiled(!model.isCompilationInProgress());
        response.tableRunState(
            !model.isProjectCompilationCompleted() ? TableRunState.CAN_RUN_MODULE : TableRunState.CAN_RUN);
        return response.build();
    }

    @Operation(summary = "compile.project.summary", description = "compile.project.desc")
    @GetMapping("project")
    public boolean project() {
        WebStudio webStudio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        if (webStudio == null) {
            return false;
        }
        return !webStudio.getModel().isCompilationInProgress();
    }

    @Operation(summary = "compile.table.summary", description = "compile.table.desc")
    @GetMapping("table/{tableId}")
    public TableInfo table(
            @Parameter(description = "compile.table.field.tableId.desc") @PathVariable("tableId") final String tableId) {
        var response = TableInfo.builder();
        WebStudio webStudio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        if (webStudio == null) {
            return response.build();
        }
        ProjectModel model = webStudio.getModel();
        IOpenLTable table = model.getTableById(tableId);
        final boolean projectCompilationCompleted = model.isProjectCompilationCompleted();
        TableRunState state = !projectCompilationCompleted ? TableRunState.CAN_RUN_MODULE : TableRunState.CAN_RUN;
        if (table != null) {
            String tableUri = table.getUri();
            List<OpenLMessage> errors = model.getOpenedModuleMessagesByTsn(tableUri, Severity.ERROR);
            List<OpenLMessage> warnings;
            if (!errors.isEmpty()) {
                state = TableRunState.CANNOT_RUN;
            }
            if (projectCompilationCompleted) {
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
            for (TableBean.TableDescription targetTable : OpenLTableLogic
                .getTargetTables(table, model, !projectCompilationCompleted)) {
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
            response.errors(OpenLTableLogic.processTableProblems(errors, model));
            response.warnings(OpenLTableLogic.processTableProblems(warnings, model));
            response.targetTables(targetTableUrlPairs);
            response.tableUrl(webStudio.url("table"));
            response.tableRunState(state);
        }
        return response.build();
    }

    private String getTestName(IOpenMethod method) {
        String name = TableSyntaxNodeUtils.getTestName(method);
        String info = ProjectHelper.getTestInfo(method);
        return String.format("%s (%s)", name, info);
    }

}
