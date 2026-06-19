package org.openl.studio.projects.service.tables.graph;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.openl.base.INamedThing;
import org.openl.binding.MethodUtil;
import org.openl.rules.lang.xls.OverloadedMethodsDictionary;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.studio.projects.model.tables.SummaryTableView;
import org.openl.studio.projects.model.tables.TableNodeView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.studio.projects.service.tables.read.SummaryTableReader;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ExecutableMethod;
import org.openl.util.CollectionUtils;
import org.openl.util.StringUtils;

/**
 * Builds the dependency graph of project tables.
 *
 * <p>The graph can cover the whole project or only the opened module. It can be restricted to the neighbourhood of a
 * single table and can follow dependencies downstream, dependents upstream, or both directions.
 *
 * @author Vladyslav Pikus
 */
@Component
@RequiredArgsConstructor
public class ProjectTablesGraphService {

    /**
     * Kind assigned to the technical node that stands for an {@link OpenMethodDispatcher}. The dispatcher is a generated
     * table that selects one overloaded version at runtime, so it is highlighted apart from regular rules tables.
     */
    static final String DISPATCHER_KIND = "Dispatcher";

    private final SummaryTableReader summaryTableReader;

    /**
     * Builds the dependency graph of the whole project, or of the opened module only. Every table is returned together
     * with the tables it depends on.
     *
     * @param model             compiled project model
     * @param currentModuleOnly limit the graph to the opened module instead of the whole project
     * @return graph nodes sorted by table name
     */
    public List<TableNodeView> buildProjectGraph(ProjectModel model, boolean currentModuleOnly) {
        return build(model, currentModuleOnly, GraphDirection.DEPENDENCIES, null, null);
    }

    /**
     * Builds the dependency graph around a single table, following the given direction up to an optional depth.
     *
     * @param model       compiled project model
     * @param rootTableId table to build the graph around
     * @param direction   which relations to follow
     * @param maxDepth    maximum traversal depth from the root table, or {@code null} for unlimited
     * @return graph nodes sorted by table name
     */
    public List<TableNodeView> buildTableGraph(ProjectModel model,
                                               String rootTableId,
                                               GraphDirection direction,
                                               @Nullable Integer maxDepth) {
        return build(model, false, direction, rootTableId, maxDepth);
    }

    private List<TableNodeView> build(ProjectModel model,
                                      boolean currentModuleOnly,
                                      GraphDirection direction,
                                      @Nullable String rootTableId,
                                      @Nullable Integer maxDepth) {
        var nodes = collectNodes(model, currentModuleOnly);
        if (nodes.isEmpty()) {
            return List.of();
        }
        linkDependents(nodes);

        var included = rootTableId != null
                ? reachable(nodes, rootTableId, direction, maxDepth)
                : nodes.keySet();

        return included.stream()
                .map(nodes::get)
                .filter(Objects::nonNull)
                .map(node -> toView(node, direction, included))
                .sorted(Comparator.comparing(view -> view.name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Map<String, RawNode> collectNodes(ProjectModel model, boolean currentModuleOnly) {
        var compiledOpenClass = currentModuleOnly
                ? model.getOpenedModuleCompiledOpenClass()
                : model.getCompiledOpenClass();
        if (compiledOpenClass == null) {
            return Map.of();
        }
        var methods = compiledOpenClass.getOpenClassWithErrors().getMethods();
        if (CollectionUtils.isEmpty(methods)) {
            return Map.of();
        }

        var projectByTable = model.getTableSyntaxNodeProjects();
        var methodNodesDictionary = model.getMethodNodesDictionary();
        var formats = WebStudioFormats.getInstance();

        Map<String, RawNode> nodes = new LinkedHashMap<>();
        Map<String, String> candidateToDispatcher = new LinkedHashMap<>();
        Deque<IOpenMethod> queue = new ArrayDeque<>(methods);
        while (!queue.isEmpty()) {
            var method = queue.poll();
            if (method instanceof OpenMethodDispatcher dispatcher) {
                addDispatcherNode(nodes, candidateToDispatcher, dispatcher, projectByTable);
                queue.addAll(dispatcher.getCandidates());
            } else if (method instanceof ExecutableMethod rulesMethod) {
                addNode(nodes, rulesMethod, projectByTable, methodNodesDictionary, formats);
            }
        }
        rewireThroughDispatchers(nodes, candidateToDispatcher);
        return nodes;
    }

    /**
     * Adds a node for an {@link OpenMethodDispatcher} — the technical table that selects one overloaded version at
     * runtime. The dispatcher depends on its candidate versions and is mapped so that callers can later be rewired to
     * point at the dispatcher instead of the individual versions.
     *
     * <p>A dispatcher that wraps a single version is transparent: it gets no node, and callers keep pointing straight at
     * that version.
     */
    private void addDispatcherNode(Map<String, RawNode> nodes,
                                   Map<String, String> candidateToDispatcher,
                                   OpenMethodDispatcher dispatcher,
                                   Map<TableSyntaxNode, String> projectByTable) {
        var candidateIds = executableCandidates(dispatcher)
                .map(candidate -> TableUtils.makeTableId(candidate.getSourceUrl()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (candidateIds.size() <= 1) {
            return;
        }
        var id = dispatcherId(candidateIds);
        candidateIds.forEach(candidateId -> candidateToDispatcher.put(candidateId, id));
        nodes.computeIfAbsent(id, key -> {
            var name = MethodUtil.printSignature(dispatcher.getCandidates().getFirst(), INamedThing.SHORT);
            var node = new RawNode(key, name, DISPATCHER_KIND, dispatcherProject(dispatcher, projectByTable));
            node.dependencies().addAll(candidateIds);
            return node;
        });
    }

    private static String dispatcherProject(OpenMethodDispatcher dispatcher, Map<TableSyntaxNode, String> projectByTable) {
        return executableCandidates(dispatcher)
                .map(candidate -> candidate.getInfo().getSyntaxNode())
                .filter(TableSyntaxNode.class::isInstance)
                .map(projectByTable::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** The dispatched candidate versions that are real tables, narrowed to {@link ExecutableMethod}. */
    private static Stream<ExecutableMethod> executableCandidates(OpenMethodDispatcher dispatcher) {
        return dispatcher.getCandidates().stream()
                .filter(ExecutableMethod.class::isInstance)
                .map(ExecutableMethod.class::cast);
    }

    private static String dispatcherId(Set<String> candidateIds) {
        // the candidate sets of two dispatchers never overlap, so the smallest candidate id uniquely keys the dispatcher
        return "dispatcher:" + candidateIds.stream().min(Comparator.naturalOrder()).orElseThrow();
    }

    /**
     * Redirects every caller's dependency on a dispatched version to the dispatcher node, so that the graph reads
     * caller &#8594; dispatcher &#8594; versions. Dispatcher nodes keep their direct links to the versions.
     */
    private void rewireThroughDispatchers(Map<String, RawNode> nodes, Map<String, String> candidateToDispatcher) {
        if (candidateToDispatcher.isEmpty()) {
            return;
        }
        nodes.values().stream()
                .filter(node -> !DISPATCHER_KIND.equals(node.kind()))
                .forEach(node -> {
                    var rewired = node.dependencies().stream()
                            .map(dependencyId -> candidateToDispatcher.getOrDefault(dependencyId, dependencyId))
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                    node.dependencies().clear();
                    node.dependencies().addAll(rewired);
                });
    }

    private void addNode(Map<String, RawNode> nodes,
                         ExecutableMethod rulesMethod,
                         Map<TableSyntaxNode, String> projectByTable,
                         OverloadedMethodsDictionary methodNodesDictionary,
                         WebStudioFormats formats) {
        var tableSyntaxNode = (TableSyntaxNode) rulesMethod.getInfo().getSyntaxNode();
        var id = TableUtils.makeTableId(rulesMethod.getSourceUrl());
        var node = nodes.computeIfAbsent(id, key -> {
            var displayNames = TableSyntaxNodeUtils.getTableDisplayValue(tableSyntaxNode, 0, methodNodesDictionary, formats);
            var kind = OpenLTableUtils.getTableTypeItems().get(tableSyntaxNode.getType());
            var summary = summaryTableReader.read(new TableSyntaxNodeAdapter(tableSyntaxNode));
            return new RawNode(id, displayNames[INamedThing.SHORT], kind, summary, dimensionProperties(rulesMethod),
                    projectByTable.get(tableSyntaxNode));
        });
        var dependencies = rulesMethod.getDependencies();
        if (dependencies != null && dependencies.getRulesMethods() != null) {
            dependencies.getRulesMethods()
                    .forEach(dependency -> node.dependencies().add(TableUtils.makeTableId(dependency.getSourceUrl())));
        }
    }

    /**
     * Reads the dimension properties this table version is selected by — the rules the dispatcher uses to pick a
     * candidate. Values resolve from both the module name pattern and the table itself, exactly as OpenL resolves them
     * at compile time for dispatching. Keys are the human-readable property display names.
     */
    private static Map<String, String> dimensionProperties(ExecutableMethod rulesMethod) {
        var properties = PropertiesHelper.getTableProperties(rulesMethod);
        Map<String, String> dimensions = new LinkedHashMap<>();
        TablePropertyDefinitionUtils.getDimensionalTableProperties().forEach(definition -> {
            var value = properties.getPropertyValueAsString(definition.getName());
            if (StringUtils.isNotEmpty(value)) {
                dimensions.put(definition.getDisplayName(), value);
            }
        });
        return dimensions;
    }

    private void linkDependents(Map<String, RawNode> nodes) {
        nodes.values().forEach(node -> node.dependencies().forEach(dependencyId -> {
            var dependency = nodes.get(dependencyId);
            if (dependency != null) {
                dependency.dependents().add(node.id());
            }
        }));
    }

    private Set<String> reachable(Map<String, RawNode> nodes,
                                  String rootTableId,
                                  GraphDirection direction,
                                  @Nullable Integer maxDepth) {
        Set<String> visited = new LinkedHashSet<>();
        if (!nodes.containsKey(rootTableId)) {
            return visited;
        }
        visited.add(rootTableId);
        Deque<String> frontier = new ArrayDeque<>();
        frontier.add(rootTableId);
        for (int depth = 0; !frontier.isEmpty() && (maxDepth == null || depth < maxDepth); depth++) {
            expandFrontier(nodes, frontier, visited, direction);
        }
        return visited;
    }

    private void expandFrontier(Map<String, RawNode> nodes,
                                Deque<String> frontier,
                                Set<String> visited,
                                GraphDirection direction) {
        for (int size = frontier.size(); size > 0; size--) {
            var node = nodes.get(frontier.poll());
            if (node != null) {
                neighbours(node, direction).stream()
                        .filter(nodes::containsKey)
                        .filter(visited::add)
                        .forEach(frontier::add);
            }
        }
    }

    private Set<String> neighbours(RawNode node, GraphDirection direction) {
        Set<String> neighbours = new LinkedHashSet<>();
        if (direction.includesDependencies()) {
            neighbours.addAll(node.dependencies());
        }
        if (direction.includesDependents()) {
            neighbours.addAll(node.dependents());
        }
        return neighbours;
    }

    private TableNodeView toView(RawNode node, GraphDirection direction, Set<String> included) {
        var builder = new TableNodeView.Builder();
        if (node.summary() != null) {
            // map every SummaryTableView field (signature, file, pos, properties…); id/name/kind below stay graph-owned
            builder.summary(node.summary());
        }
        builder.id(node.id())
                .name(node.name())
                .kind(node.kind())
                .project(node.project())
                .dimensionProperties(node.dimensionProperties());
        if (direction.includesDependencies()) {
            builder.dependencies(retain(node.dependencies(), included));
        }
        if (direction.includesDependents()) {
            builder.dependents(retain(node.dependents(), included));
        }
        return builder.build();
    }

    private static Set<String> retain(Set<String> ids, Set<String> included) {
        return ids.stream()
                .filter(included::contains)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private record RawNode(String id, String name, String kind, @Nullable SummaryTableView summary,
                           Map<String, String> dimensionProperties, String project,
                           Set<String> dependencies, Set<String> dependents) {
        private RawNode(String id, String name, String kind, String project) {
            this(id, name, kind, null, Map.of(), project, new LinkedHashSet<>(), new LinkedHashSet<>());
        }

        private RawNode(String id, String name, String kind, SummaryTableView summary,
                        Map<String, String> dimensionProperties, String project) {
            this(id, name, kind, summary, dimensionProperties, project, new LinkedHashSet<>(), new LinkedHashSet<>());
        }
    }
}
