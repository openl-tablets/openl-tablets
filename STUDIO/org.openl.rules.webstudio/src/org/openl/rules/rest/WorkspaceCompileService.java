package org.openl.rules.rest;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.openl.CompiledOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.message.OpenLMessage;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.dependencies.WebStudioWorkspaceRelatedDependencyManager;
import org.openl.rules.webstudio.web.MessageHandler;
import org.openl.rules.webstudio.web.tableeditor.TableBean;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenMethod;
import org.openl.util.StringUtils;
import org.openl.validation.ValidatedCompiledOpenClass;
import org.springframework.stereotype.Service;

@Service
@Path("/compile/")
@Produces(MediaType.APPLICATION_JSON)
public class WorkspaceCompileService {

    private static final MessageHandler messageHandler = new MessageHandler();

    @GET
    @Path("progress/{messageId}/{messageIndex}")
    public Response getCompile(@PathParam("messageId") final Long messageId,
            @PathParam("messageIndex") final Integer messageIndex) {
        Map<String, Object> compileModuleInfo = new HashMap<>();
        WebStudio webStudio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        if (webStudio != null) {
            ProjectModel model = webStudio.getModel();
            Module moduleInfo = model.getModuleInfo();
            WebStudioWorkspaceRelatedDependencyManager webStudioWorkspaceDependencyManager = model
                .getWebStudioWorkspaceDependencyManager();
            if (webStudioWorkspaceDependencyManager != null) {
                int compiledCount = 0;
                int errorsCount = 0;
                int warningsCount = 0;
                int modulesCount = 0;
                List<MessageDescription> newMessages = new ArrayList<>();
                Deque<ProjectDescriptor> queue = new ArrayDeque<>();
                queue.add(moduleInfo.getProject());
                while (!queue.isEmpty()) {
                    ProjectDescriptor projectDescriptor = queue.poll();
                    for (Module module : projectDescriptor.getModules()) {
                        Collection<IDependencyLoader> dependencyLoadersForModule = webStudioWorkspaceDependencyManager
                            .findDependencyLoadersByName(module.getName());
                        for (IDependencyLoader dependencyLoader : dependencyLoadersForModule) {
                            CompiledDependency compiledDependency = dependencyLoader.getRefToCompiledDependency();
                            if (compiledDependency != null) {
                                for (OpenLMessage message : compiledDependency.getCompiledOpenClass().getCurrentMessages()) {
                                    switch (message.getSeverity()) {
                                        case WARN:
                                            warningsCount++;
                                            break;
                                        case ERROR:
                                            errorsCount++;
                                            break;
                                    }
                                    MessageDescription messageDescription = getMessageDescription(message, model);
                                    newMessages.add(messageDescription);
                                }
                                compiledCount++;
                            }
                            modulesCount++;
                        }
                    }
                    if (projectDescriptor.getDependencies() != null) {
                        for (ProjectDependencyDescriptor pd : projectDescriptor.getDependencies()) {
                            String projectDependencyName = ProjectExternalDependenciesHelper
                                .buildDependencyNameForProject(pd.getName());
                            Collection<IDependencyLoader> dependencyLoadersForProject = webStudioWorkspaceDependencyManager
                                .findDependencyLoadersByName(projectDependencyName);
                            if (dependencyLoadersForProject != null) {
                                for (IDependencyLoader dependencyLoader : dependencyLoadersForProject) {
                                    if (dependencyLoader != null && dependencyLoader.isProjectLoader()) {
                                        queue.add(dependencyLoader.getProject());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                CompiledOpenClass compiledOpenClass = webStudio.getModel().getCompiledOpenClass();
                if (compiledOpenClass instanceof ValidatedCompiledOpenClass) {
                    for (OpenLMessage message : ((ValidatedCompiledOpenClass) compiledOpenClass)
                        .getValidationMessages()) {
                        switch (message.getSeverity()) {
                            case WARN:
                                warningsCount++;
                                break;
                            case ERROR:
                                errorsCount++;
                                break;
                        }
                        MessageDescription messageDescription = getMessageDescription(message, model);
                        newMessages.add(messageDescription);
                    }
                }
                compileModuleInfo.put("dataType", "new");
                if (messageIndex != -1 && messageId != -1) {
                    MessageDescription messageDescription = newMessages.get(messageIndex);
                    if (messageDescription.getId() == messageId) {
                        newMessages = newMessages.subList(messageIndex + 1, newMessages.size());
                        compileModuleInfo.put("dataType", "add");
                    }
                }

                compileModuleInfo.put("modulesCount", modulesCount);
                compileModuleInfo.put("modulesCompiled", compiledCount);
                compileModuleInfo.put("messages", newMessages);
                compileModuleInfo.put("messageId",
                    newMessages.isEmpty() ? -1 : newMessages.get(newMessages.size() - 1).getId());
                compileModuleInfo.put("messageIndex", newMessages.size() - 1);
                compileModuleInfo.put("errorsCount", errorsCount);
                compileModuleInfo.put("warningsCount", warningsCount);
            }
        }
        return Response.ok(compileModuleInfo).build();
    }

    @GET
    @Path("tests/{tableId}")
    public Response getCompile(@PathParam("tableId") final String tableId) {
        Map<String, Object> tableTestsInfo = new HashMap<>();
        WebStudio webStudio = WebStudioUtils.getWebStudio(WebStudioUtils.getSession());
        List<TableBean.TableDescription> tableDescriptions = new ArrayList<>();
        if (webStudio != null) {
            ProjectModel model = webStudio.getModel();
            IOpenLTable table = model.getTableById(tableId);
            IOpenMethod[] allTests = model.getTestAndRunMethods(table.getUri());

            if (allTests != null) {
                for (IOpenMethod test : allTests) {
                    TableSyntaxNode syntaxNode = (TableSyntaxNode) test.getInfo().getSyntaxNode();
                    tableDescriptions.add(new TableBean.TableDescription(webStudio.url("table", syntaxNode.getUri()),
                        syntaxNode.getId(),
                        getTestName(test)));
                }
                tableDescriptions.sort(Comparator.comparing(TableBean.TableDescription::getName));
            }

            tableTestsInfo.put("allTests", tableDescriptions);
            tableTestsInfo.put("compiled", isModuleCompiled(model, webStudio));
        }
        return Response.ok(tableTestsInfo).build();
    }

    private boolean isModuleCompiled(ProjectModel model, WebStudio webStudio) {
        String currentProjectDependencyName = ProjectExternalDependenciesHelper
            .buildDependencyNameForProject(model.getModuleInfo().getProject().getName());
        WebStudioWorkspaceRelatedDependencyManager webStudioWorkspaceDependencyManager = webStudio.getModel()
            .getWebStudioWorkspaceDependencyManager();
        Collection<IDependencyLoader> dependencyLoadersForModule = webStudioWorkspaceDependencyManager
            .findDependencyLoadersByName(currentProjectDependencyName);
        for (IDependencyLoader dependencyLoader : dependencyLoadersForModule) {
            if (dependencyLoader.getRefToCompiledDependency() != null) {
                return true;
            }
        }
        return false;
    }

    private String getTestName(Object testMethod) {
        IOpenMethod method = (IOpenMethod) testMethod;
        String name = TableSyntaxNodeUtils.getTestName(method);
        String info = ProjectHelper.getTestInfo(method);
        return String.format("%s (%s)", name, info);
    }

    private MessageDescription getMessageDescription(OpenLMessage message, ProjectModel model) {
        String url = messageHandler.getSourceUrl(message, model);
        if (StringUtils.isBlank(url)) {
            url = messageHandler.getUrlForEmptySource(message);
        }
        return new MessageDescription(message.getId(), message.getSummary(), message.getSeverity(), url);
    }

}
