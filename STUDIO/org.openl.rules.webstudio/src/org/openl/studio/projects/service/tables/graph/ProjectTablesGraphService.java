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
import jakarta.annotation.Nullable;

import org.springframework.stereotype.Component;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.OverloadedMethodsDictionary;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.studio.projects.model.tables.TableNodeView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ExecutableMethod;
import org.openl.util.CollectionUtils;

/**
 * Builds the dependency graph of project tables.
 *
 * <p>The graph can cover the whole project or only the opened module. It can be restricted to the neighbourhood of a
 * single table and can follow dependencies downstream, dependents upstream, or both directions.
 *
 * @author Vladyslav Pikus
 */
@Component
public class ProjectTablesGraphService {

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
        Deque<IOpenMethod> queue = new ArrayDeque<>(methods);
        while (!queue.isEmpty()) {
            var method = queue.poll();
            if (method instanceof OpenMethodDispatcher dispatcher) {
                queue.addAll(dispatcher.getCandidates());
            } else if (method instanceof ExecutableMethod rulesMethod) {
                addNode(nodes, rulesMethod, projectByTable, methodNodesDictionary, formats);
            }
        }
        return nodes;
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
            return new RawNode(id, displayNames[INamedThing.SHORT], kind, projectByTable.get(tableSyntaxNode));
        });
        var dependencies = rulesMethod.getDependencies();
        if (dependencies != null && dependencies.getRulesMethods() != null) {
            dependencies.getRulesMethods()
                    .forEach(dependency -> node.dependencies().add(TableUtils.makeTableId(dependency.getSourceUrl())));
        }
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
        var builder = new TableNodeView.Builder()
                .id(node.id())
                .name(node.name())
                .kind(node.kind())
                .project(node.project());
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

    private record RawNode(String id, String name, String kind, String project,
                           Set<String> dependencies, Set<String> dependents) {
        private RawNode(String id, String name, String kind, String project) {
            this(id, name, kind, project, new LinkedHashSet<>(), new LinkedHashSet<>());
        }
    }
}
