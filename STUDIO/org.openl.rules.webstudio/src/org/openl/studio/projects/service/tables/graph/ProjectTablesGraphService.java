package org.openl.studio.projects.service.tables.graph;

import java.util.ArrayDeque;
import java.util.Collection;
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
import org.openl.rules.lang.xls.OverloadedMethodsDictionary;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.studio.projects.model.tables.DatatypeNodeFieldView;
import org.openl.studio.projects.model.tables.DatatypeNodeView;
import org.openl.studio.projects.model.tables.ExecutableNodeView;
import org.openl.studio.projects.model.tables.SummaryTableView;
import org.openl.studio.projects.model.tables.TableGraphNodeKind;
import org.openl.studio.projects.model.tables.TableNodeView;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.studio.projects.service.tables.read.SummaryTableReader;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.impl.ExecutableMethod;
import org.openl.types.impl.InternalDatatypeClass;
import org.openl.util.CollectionUtils;
import org.openl.util.OpenClassUtils;
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
     * @param layer             which layer of the graph to build
     * @return graph nodes sorted by table name
     */
    public List<TableNodeView> buildProjectGraph(ProjectModel model, boolean currentModuleOnly, GraphLayer layer) {
        return build(model, currentModuleOnly, GraphDirection.DEPENDENCIES, null, null, layer);
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
        // the root itself decides which layer the graph covers: neither layer links to the other
        return build(model, false, direction, rootTableId, maxDepth, GraphLayer.ALL);
    }

    private List<TableNodeView> build(ProjectModel model,
                                      boolean currentModuleOnly,
                                      GraphDirection direction,
                                      @Nullable String rootTableId,
                                      @Nullable Integer maxDepth,
                                      GraphLayer layer) {
        // reading the index once serves both passes: it is rebuilt over every table of every module on each call
        var projectByTable = model.getTableSyntaxNodeProjects();
        Map<String, RawNode> nodes = layer.includesExecutable()
                ? collectNodes(model, currentModuleOnly, true, projectByTable)
                : new LinkedHashMap<>();
        // A datatype is reached from another datatype only, so a graph rooted at a callable table never shows one.
        // Collecting them there would read every datatype table's source just to drop it — skip the pass instead.
        // Revisit this when the data model stops being a layer of its own and rules start linking to their types.
        if (layer.includesDatatypes() && (rootTableId == null || !nodes.containsKey(rootTableId))) {
            collectDatatypeNodes(model, currentModuleOnly, nodes, projectByTable);
        }
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

    private Map<String, RawNode> collectNodes(ProjectModel model,
                                              boolean currentModuleOnly,
                                              boolean withDetails,
                                              Map<TableSyntaxNode, String> projectByTable) {
        var nodes = new LinkedHashMap<String, RawNode>();
        var compiledOpenClass = currentModuleOnly
                ? model.getOpenedModuleCompiledOpenClass()
                : model.getCompiledOpenClass();
        if (compiledOpenClass == null) {
            return nodes;
        }
        var methods = compiledOpenClass.getOpenClassWithErrors().getMethods();
        if (CollectionUtils.isEmpty(methods)) {
            return nodes;
        }

        // The whole-project graph spans every module, so its overloaded-name disambiguation needs the all-module
        // dictionary; the opened-module dictionary would leave versions from other modules with identical names.
        var methodNodesDictionary = currentModuleOnly
                ? model.getMethodNodesDictionary()
                : model.getAllMethodNodesDictionary();
        var formats = WebStudioFormats.getInstance();

        var candidateToDispatcher = new LinkedHashMap<String, String>();
        var queue = new ArrayDeque<IOpenMethod>(methods);
        while (!queue.isEmpty()) {
            var method = queue.poll();
            if (method instanceof OpenMethodDispatcher dispatcher) {
                addDispatcherNode(nodes, candidateToDispatcher, dispatcher, projectByTable);
                queue.addAll(dispatcher.getCandidates());
            } else if (method instanceof ExecutableMethod rulesMethod) {
                addNode(nodes, rulesMethod, projectByTable, methodNodesDictionary, formats, withDetails);
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
            // the dispatcher carries the plain method name; the parameter signature is redundant beside its versions
            var node = new RawNode(key, dispatcher.getName(), DISPATCHER_KIND, dispatcherProject(dispatcher, projectByTable));
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
                         WebStudioFormats formats,
                         boolean withDetails) {
        var tableSyntaxNode = (TableSyntaxNode) rulesMethod.getInfo().getSyntaxNode();
        var id = TableUtils.makeTableId(rulesMethod.getSourceUrl());
        var node = nodes.computeIfAbsent(id, key -> withDetails
                ? detailedNode(id, tableSyntaxNode, rulesMethod, projectByTable, methodNodesDictionary, formats)
                : new RawNode(id, rulesMethod.getName(),
                        OpenLTableUtils.getTableTypeItems().get(tableSyntaxNode.getType()),
                        projectByTable.get(tableSyntaxNode)));
        var dependencies = rulesMethod.getDependencies();
        if (dependencies != null && dependencies.getRulesMethods() != null) {
            dependencies.getRulesMethods()
                    .forEach(dependency -> node.dependencies().add(TableUtils.makeTableId(dependency.getSourceUrl())));
        }
    }

    /** Builds a full node, reading the table's display name and summary — used for the graph views. */
    private RawNode detailedNode(String id,
                                 TableSyntaxNode tableSyntaxNode,
                                 ExecutableMethod rulesMethod,
                                 Map<TableSyntaxNode, String> projectByTable,
                                 OverloadedMethodsDictionary methodNodesDictionary,
                                 WebStudioFormats formats) {
        var displayNames = TableSyntaxNodeUtils.getTableDisplayValue(tableSyntaxNode, 0, methodNodesDictionary, formats);
        var kind = OpenLTableUtils.getTableTypeItems().get(tableSyntaxNode.getType());
        var summary = summaryTableReader.read(new TableSyntaxNodeAdapter(tableSyntaxNode));
        return new RawNode(id, displayNames[INamedThing.SHORT], kind, summary, dimensionProperties(rulesMethod),
                projectByTable.get(tableSyntaxNode), null);
    }

    /**
     * Adds a node for every datatype table and links the data model between the datatypes: the datatype each one
     * extends and the datatypes its own fields refer to.
     *
     * <p>Datatype tables define types, not methods, so they never arrive through the compiled methods; they are read
     * from the table syntax nodes instead. Only the relations between datatypes are followed — the rules tables that
     * use a datatype are not linked to it, so the data model reads as its own layer of the graph.
     */
    private void collectDatatypeNodes(ProjectModel model,
                                      boolean currentModuleOnly,
                                      Map<String, RawNode> nodes,
                                      Map<TableSyntaxNode, String> projectByTable) {
        Collection<TableSyntaxNode> tables = currentModuleOnly
                ? List.of(model.getTableSyntaxNodes())
                : model.getAllTableSyntaxNodes();
        tables.forEach(tableSyntaxNode -> {
            var datatype = datatypeOf(tableSyntaxNode);
            if (datatype != null) {
                nodes.computeIfAbsent(tableSyntaxNode.getId(),
                        id -> datatypeNode(id, tableSyntaxNode, datatype, projectByTable));
            }
        });
    }

    /** The type a table declares, or {@code null} when the table is not a datatype table. */
    @Nullable
    private static IOpenClass datatypeOf(TableSyntaxNode tableSyntaxNode) {
        if (!XlsNodeTypes.XLS_DATATYPE.toString().equals(tableSyntaxNode.getType())) {
            return null;
        }
        return tableSyntaxNode.getMember() instanceof InternalDatatypeClass datatype ? datatype.getType() : null;
    }

    /**
     * Builds the node of a datatype table together with the data model around it: the datatype it extends and the
     * fields it declares. Both become the node's dependencies, so the data model is walkable like any other part of
     * the graph.
     */
    private RawNode datatypeNode(String id,
                                 TableSyntaxNode tableSyntaxNode,
                                 IOpenClass datatype,
                                 Map<TableSyntaxNode, String> projectByTable) {
        var summary = summaryTableReader.read(new TableSyntaxNodeAdapter(tableSyntaxNode));
        var kind = OpenLTableUtils.getTableTypeItems().get(tableSyntaxNode.getType());
        var dataModel = dataModelOf(datatype);
        var node = new RawNode(id, summary.name, kind, summary, Map.of(), projectByTable.get(tableSyntaxNode),
                dataModel);
        if (dataModel.extendsId() != null) {
            node.dependencies().add(dataModel.extendsId());
        }
        dataModel.fields().stream()
                .map(DatatypeNodeFieldView::ref)
                .filter(Objects::nonNull)
                .forEach(node.dependencies()::add);
        return node;
    }

    /**
     * Reads the data model of one datatype: the datatype it extends and the fields it declares. Every field is
     * reported, and the ones whose type is a datatype table also name that table, a collection field through its
     * element type.
     *
     * <p>Only the fields the datatype declares itself are listed; the inherited ones belong to the parent's node.
     */
    private static DataModel dataModelOf(IOpenClass datatype) {
        // a datatype keeps its fields in declaration order, so the response lists them as the table itself reads
        var fields = datatype instanceof DatatypeOpenClass declaringType
                ? declaringType.getDeclaredFields().stream().map(ProjectTablesGraphService::fieldOf).toList()
                : List.<DatatypeNodeFieldView>of();
        return new DataModel(datatypeTableId(superTypeOf(datatype)), fields);
    }

    /** The datatype a type inherits from: the parent of a datatype, or the base type a vocabulary narrows. */
    @Nullable
    private static IOpenClass superTypeOf(IOpenClass datatype) {
        if (datatype instanceof DatatypeOpenClass declaringType) {
            return declaringType.getSuperClass();
        }
        return datatype instanceof DomainOpenClass vocabulary ? vocabulary.getBaseClass() : null;
    }

    private static DatatypeNodeFieldView fieldOf(IOpenField field) {
        var type = field.getType();
        var elementType = OpenClassUtils.getRootComponentClass(type);
        return new DatatypeNodeFieldView(field.getName(), type.getDisplayName(INamedThing.SHORT),
                datatypeTableId(elementType), elementType != type ? Boolean.TRUE : null);
    }

    /**
     * The id of the node a type is declared by, or {@code null} when the type is not declared by a datatype table. A
     * datatype knows the table it comes from, so the id is read from the type itself rather than looked up.
     *
     * <p>The table may be outside the graph — declared in another module of a module-scoped graph, or left out by the
     * traversal. Such an id simply matches no node and is dropped along with any other unreachable relation.
     */
    @Nullable
    private static String datatypeTableId(@Nullable IOpenClass type) {
        if (!(type instanceof DatatypeOpenClass || type instanceof DomainOpenClass)) {
            return null;
        }
        var metaInfo = type.getMetaInfo();
        return metaInfo != null ? TableUtils.makeTableId(metaInfo.getSourceUrl()) : null;
    }

    /**
     * Returns the ids of every table reachable from the root table in the given direction, including the root.
     *
     * <p>Lighter than {@link #buildTableGraph}: it builds only the dependency adjacency, not the per-table
     * summaries, so callers can filter by reachability without reading every table's source. Returns an empty set
     * when the root is not a table node (for example a test table, which is not part of the dependency graph).
     *
     * @param model       compiled project model
     * @param rootTableId table to traverse from
     * @param direction   which relations to follow
     * @param maxDepth    maximum traversal depth from the root, or {@code null} for unlimited
     * @return reachable table ids, including the root
     */
    public Set<String> reachableTableIds(ProjectModel model,
                                         String rootTableId,
                                         GraphDirection direction,
                                         @Nullable Integer maxDepth) {
        var nodes = collectNodes(model, false, false, model.getTableSyntaxNodeProjects());
        if (nodes.isEmpty()) {
            return Set.of();
        }
        linkDependents(nodes);
        return reachable(nodes, rootTableId, direction, maxDepth);
    }

    /**
     * Reads the dimension properties this table version is selected by — the rules the dispatcher uses to pick a
     * candidate. Values resolve from both the module name pattern and the table itself, exactly as OpenL resolves them
     * at compile time for dispatching. Keys are the human-readable property display names.
     */
    private static Map<String, String> dimensionProperties(ExecutableMethod rulesMethod) {
        var properties = PropertiesHelper.getTableProperties(rulesMethod);
        var dimensions = new LinkedHashMap<String, String>();
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
        var visited = new LinkedHashSet<String>();
        if (!nodes.containsKey(rootTableId)) {
            return visited;
        }
        visited.add(rootTableId);
        var frontier = new ArrayDeque<String>();
        frontier.add(rootTableId);
        for (var depth = 0; !frontier.isEmpty() && (maxDepth == null || depth < maxDepth); depth++) {
            expandFrontier(nodes, frontier, visited, direction);
        }
        return visited;
    }

    private void expandFrontier(Map<String, RawNode> nodes,
                                Deque<String> frontier,
                                Set<String> visited,
                                GraphDirection direction) {
        for (var size = frontier.size(); size > 0; size--) {
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
        var neighbours = new LinkedHashSet<String>();
        if (direction.includesDependencies()) {
            neighbours.addAll(node.dependencies());
        }
        if (direction.includesDependents()) {
            neighbours.addAll(node.dependents());
        }
        return neighbours;
    }

    private TableNodeView toView(RawNode node, GraphDirection direction, Set<String> included) {
        TableNodeView.Builder<?> builder = node.dataModel() != null
                ? datatypeBuilder(node.dataModel(), included)
                : ExecutableNodeView.builder().dimensionProperties(node.dimensionProperties());
        if (node.summary() != null) {
            // map every SummaryTableView field (signature, file, pos, properties…); id/name/kind below stay graph-owned
            builder.summary(node.summary());
        }
        builder.id(node.id())
                .name(node.name())
                .kind(TableGraphNodeKind.fromValue(node.kind()))
                .project(node.project());
        if (direction.includesDependencies()) {
            builder.dependencies(retain(node.dependencies(), included));
        }
        if (direction.includesDependents()) {
            builder.dependents(retain(node.dependents(), included));
        }
        return builder.build();
    }

    /**
     * Builds the datatype node, keeping only the references that stay inside the graph: a parent or a field type left
     * out by the traversal is not addressable, so the field is reported without its reference.
     */
    private static DatatypeNodeView.Builder datatypeBuilder(DataModel dataModel, Set<String> included) {
        var fields = dataModel.fields().stream()
                .map(field -> field.ref() == null || included.contains(field.ref())
                        ? field
                        : new DatatypeNodeFieldView(field.name(), field.type(), null, field.collection()))
                .toList();
        return DatatypeNodeView.builder()
                .extendz(included.contains(dataModel.extendsId()) ? dataModel.extendsId() : null)
                .fields(fields);
    }

    private static Set<String> retain(Set<String> ids, Set<String> included) {
        return ids.stream()
                .filter(included::contains)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private record RawNode(String id, String name, String kind, @Nullable SummaryTableView summary,
                           Map<String, String> dimensionProperties, String project,
                           Set<String> dependencies, Set<String> dependents, @Nullable DataModel dataModel) {
        private RawNode(String id, String name, String kind, String project) {
            this(id, name, kind, null, Map.of(), project, null);
        }

        private RawNode(String id, String name, String kind, @Nullable SummaryTableView summary,
                        Map<String, String> dimensionProperties, String project, @Nullable DataModel dataModel) {
            this(id, name, kind, summary, dimensionProperties, project,
                    new LinkedHashSet<>(), new LinkedHashSet<>(), dataModel);
        }
    }

    /** The data model a datatype node carries: the datatype it extends and the fields it declares. */
    private record DataModel(@Nullable String extendsId, List<DatatypeNodeFieldView> fields) {
    }
}
