package org.openl.studio.projects.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.project.status.DetailedMessageDescription;
import org.openl.studio.projects.model.project.status.MessageSource;
import org.openl.studio.projects.model.project.status.ModuleMessageSource;
import org.openl.studio.projects.model.project.status.TableMessageSource;

@Service
public class DetailedMessageDescriptionMapperImpl implements DetailedMessageDescriptionMapper {

    private static final Comparator<DetailedMessageDescription> BY_SEVERITY_AND_ID = Comparator
            .<DetailedMessageDescription, org.openl.message.Severity>comparing(m -> m.source().severity())
            .thenComparingLong(m -> m.source().id());

    @Override
    public List<DetailedMessageDescription> mapSorted(Collection<OpenLMessage> messages, ProjectModel model) {
        return messages.stream()
                .map(message -> map(message, model))
                .sorted(BY_SEVERITY_AND_ID)
                .toList();
    }

    private static DetailedMessageDescription map(OpenLMessage message, ProjectModel model) {
        var source = MessageDescription.builder()
                .id(message.getId())
                .summary(message.getSummary())
                .severity(message.getSeverity())
                .build();
        return DetailedMessageDescription.builder()
                .source(source)
                .location(resolveLocation(message, model))
                .stacktrace(hasStacktrace(message) ? Boolean.TRUE : null)
                .build();
    }

    private static boolean hasStacktrace(OpenLMessage message) {
        return message instanceof OpenLErrorMessage errorMessage && errorMessage.getError() != null;
    }

    private static MessageSource resolveLocation(OpenLMessage message, ProjectModel model) {
        var sourceLocation = message.getSourceLocation();
        if (sourceLocation == null) {
            return null;
        }
        var moduleName = resolveModuleName(sourceLocation, model);
        var nodeId = model.getMessageNodeId(sourceLocation);
        if (nodeId != null) {
            // getNodeById avoids the buildProjectTree() side effect that getTableById triggers;
            // the publisher may run on a worker thread without a bound HTTP session.
            var node = model.getNodeById(nodeId);
            var tableName = node != null ? new TableSyntaxNodeAdapter(node).getDisplayName() : null;
            return TableMessageSource.builder()
                    .id(nodeId)
                    .name(tableName)
                    .module(moduleName)
                    .cell(new XlsUrlParser(sourceLocation).getCell())
                    .build();
        }
        return moduleName != null
                ? ModuleMessageSource.builder().name(moduleName).build()
                : null;
    }

    /**
     * Walks every module dependency loader in the workspace (current project and any
     * projects it depends on) and returns the {@link Module#getName() module name}
     * whose rules root contains the supplied source location. Mirrors the lookup used by
     * {@code WebStudio} and {@code WorkspaceProjectService} so the result matches what
     * the rest of the UI shows for the same table.
     */
    private static String resolveModuleName(String sourceLocation, ProjectModel model) {
        var dependencyManager = model.getWebStudioWorkspaceDependencyManager();
        if (dependencyManager == null) {
            return null;
        }
        for (IDependencyLoader loader : dependencyManager.getDependencyLoaders()) {
            if (loader.isProjectLoader()) {
                continue;
            }
            var module = loader.getModule();
            if (module != null && module.containsTable(sourceLocation)) {
                return module.getName();
            }
        }
        return null;
    }
}
