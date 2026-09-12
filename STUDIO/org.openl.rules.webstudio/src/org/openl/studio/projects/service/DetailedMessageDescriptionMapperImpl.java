package org.openl.studio.projects.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.project.instantiation.IDependencyLoader;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.rest.compile.MessageDescription;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.project.status.DetailedMessageDescription;
import org.openl.studio.projects.model.project.status.MessageSource;
import org.openl.studio.projects.model.project.status.ModuleMessageSource;
import org.openl.studio.projects.model.project.status.TableMessageSource;

@Service
@RequiredArgsConstructor
public class DetailedMessageDescriptionMapperImpl implements DetailedMessageDescriptionMapper {

    private final ProjectIdentifierMapper projectIdentifierMapper;

    private static final Comparator<DetailedMessageDescription> BY_SEVERITY_AND_ID = Comparator
            .<DetailedMessageDescription, org.openl.message.Severity>comparing(m -> m.source().severity())
            .thenComparingLong(m -> m.source().id());

    @Override
    public List<DetailedMessageDescription> mapSorted(Collection<OpenLMessage> messages, ProjectModel model) {
        // A big project raises thousands of messages. Resolving each one against the model's tables and
        // modules independently rescanned every table twice per message; instead, index the tables and
        // modules once and look each message up against those indexes.
        var locator = new MessageLocator(model, projectIdentifierMapper);
        return messages.stream()
                .map(message -> map(message, locator))
                .sorted(BY_SEVERITY_AND_ID)
                .toList();
    }

    private static DetailedMessageDescription map(OpenLMessage message, MessageLocator locator) {
        var source = MessageDescription.builder()
                .id(message.getId())
                .summary(message.getSummary())
                .severity(message.getSeverity())
                .build();
        return DetailedMessageDescription.builder()
                .source(source)
                .location(locator.resolve(message))
                .stacktrace(hasStacktrace(message) ? Boolean.TRUE : null)
                .build();
    }

    private static boolean hasStacktrace(OpenLMessage message) {
        return message instanceof OpenLErrorMessage errorMessage && errorMessage.getError() != null;
    }

    /**
     * Resolves message locations against indexes built once per project. Two tables can only overlap
     * when they live on the same worksheet, so bucketing tables by workbook and sheet lets each message
     * intersect-test only its own sheet's tables instead of every table in the workspace. The matching
     * node is used directly, avoiding a second lookup by id.
     */
    private static final class MessageLocator {

        private record TableEntry(TableSyntaxNode node, XlsUrlParser location) {
        }

        /** Where a project is, as a screen addresses it, and what it is called. */
        private record ProjectAddress(String id, String name) {
        }

        private final Map<String, List<TableEntry>> tablesBySheet;
        private final List<Module> modules;
        /** The project each module belongs to — a message can come from a project this one depends on. */
        private final Map<String, ProjectAddress> projectsByModule;

        MessageLocator(ProjectModel model, ProjectIdentifierMapper projectIdentifierMapper) {
            tablesBySheet = indexTables(model);
            modules = indexModules(model);
            projectsByModule = indexProjects(model, projectIdentifierMapper, modules);
        }

        MessageSource resolve(OpenLMessage message) {
            var sourceLocation = message.getSourceLocation();
            if (sourceLocation == null) {
                return null;
            }
            var location = new XlsUrlParser(sourceLocation);
            var moduleName = resolveModuleName(sourceLocation);
            var project = moduleName == null ? null : projectsByModule.get(moduleName);
            var node = findNode(location);
            if (node != null) {
                var tableName = new TableSyntaxNodeAdapter(node).getDisplayName();
                return TableMessageSource.builder()
                        .id(node.getId())
                        .name(tableName)
                        .module(moduleName)
                        .projectId(project == null ? null : project.id())
                        .project(project == null ? null : project.name())
                        .cell(location.getCell())
                        .build();
            }
            return moduleName != null
                    ? ModuleMessageSource.builder()
                            .name(moduleName)
                            .projectId(project == null ? null : project.id())
                            .project(project == null ? null : project.name())
                            .build()
                    : null;
        }

        private TableSyntaxNode findNode(XlsUrlParser location) {
            var candidates = tablesBySheet.get(sheetKey(location));
            if (candidates == null) {
                return null;
            }
            for (TableEntry candidate : candidates) {
                if (location.intersects(candidate.location())) {
                    return candidate.node();
                }
            }
            return null;
        }

        /**
         * Walks every module dependency loader in the workspace (current project and any projects it
         * depends on) and returns the {@link Module#getName() module name} whose rules root contains the
         * supplied source location. Mirrors the lookup used by {@code WebStudio} and
         * {@code WorkspaceProjectService} so the result matches what the rest of the UI shows.
         */
        private String resolveModuleName(String sourceLocation) {
            for (Module module : modules) {
                if (module.containsTable(sourceLocation)) {
                    return module.getName();
                }
            }
            return null;
        }

        private static Map<String, List<TableEntry>> indexTables(ProjectModel model) {
            var index = new HashMap<String, List<TableEntry>>();
            for (TableSyntaxNode node : model.getAllTableSyntaxNodes()) {
                var location = node.getUriParser();
                if (location != null) {
                    index.computeIfAbsent(sheetKey(location), key -> new ArrayList<>()).add(new TableEntry(node, location));
                }
            }
            return index;
        }

        private static List<Module> indexModules(ProjectModel model) {
            var dependencyManager = model.getWebStudioWorkspaceDependencyManager();
            if (dependencyManager == null) {
                return List.of();
            }
            var modules = new ArrayList<Module>();
            for (IDependencyLoader loader : dependencyManager.getDependencyLoaders()) {
                if (loader.isProjectLoader()) {
                    continue;
                }
                var module = loader.getModule();
                if (module != null) {
                    modules.add(module);
                }
            }
            return modules;
        }

        /**
         * The project each module belongs to, resolved once per project rather than once per message.
         *
         * <p>The modules of the workspace include those of the projects this one depends on, and a message
         * raised in one of them belongs to that project — so the screen it sends the reader to is that
         * project's, not the one being compiled.
         */
        private static Map<String, ProjectAddress> indexProjects(ProjectModel model,
                                                                 ProjectIdentifierMapper projectIdentifierMapper,
                                                                 List<Module> modules) {
            var studio = model.getStudio();
            if (studio == null) {
                return Map.of();
            }
            var repositoryOfProject = new HashMap<String, String>();
            studio.getProjects()
                    .forEach((repositoryId, descriptors) -> descriptors
                            .forEach(descriptor -> repositoryOfProject.putIfAbsent(descriptor.getName(), repositoryId)));
            var addressOfProject = new HashMap<String, ProjectAddress>();
            var byModule = new HashMap<String, ProjectAddress>();
            for (Module module : modules) {
                var descriptor = module.getProject();
                if (descriptor == null || module.getName() == null) {
                    continue;
                }
                var address = addressOfProject.computeIfAbsent(descriptor.getName(),
                        name -> address(studio, projectIdentifierMapper, repositoryOfProject.get(name), descriptor));
                if (address != null) {
                    byModule.put(module.getName(), address);
                }
            }
            return byModule;
        }

        private static ProjectAddress address(WebStudio studio,
                                              ProjectIdentifierMapper projectIdentifierMapper,
                                              String repositoryId,
                                              ProjectDescriptor descriptor) {
            if (repositoryId == null || descriptor.getProjectFolder() == null) {
                return null;
            }
            var project = studio.getProject(repositoryId, descriptor.getProjectFolder().getFileName().toString());
            return project == null ? null
                    : new ProjectAddress(projectIdentifierMapper.map(project).encode(), descriptor.getName());
        }

        private static String sheetKey(XlsUrlParser location) {
            return location.getWbPath() + '\n' + location.getWbName() + '\n' + location.getWsName();
        }
    }
}
