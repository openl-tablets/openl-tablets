package org.openl.studio.projects.service.tables.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.studio.projects.model.tables.TableNodeView;
import org.openl.studio.projects.service.tables.read.SummaryTableReader;

class ProjectTablesGraphServiceTest {

    private static ProjectModel projectModel;
    private final ProjectTablesGraphService service = new ProjectTablesGraphService(new SummaryTableReader());

    @BeforeAll
    static void compileProject() throws Exception {
        WebStudio webStudio = mock(WebStudio.class);
        projectModel = new ProjectModel(webStudio, null);
        var module = ProjectResolver.getInstance()
                .resolve(Path.of("test-resources/org/openl/studio/projects/service/tables/graph/Project"))
                .getModules()
                .getFirst();
        projectModel.setModuleInfo(module);
    }

    private Map<String, TableNodeView> byName(List<TableNodeView> nodes) {
        return nodes.stream().collect(Collectors.toMap(node -> node.name, Function.identity()));
    }

    private String idOf(String name) {
        return byName(service.buildProjectGraph(projectModel, false)).get(name).id;
    }

    private Set<String> names(List<TableNodeView> nodes) {
        return nodes.stream().map(node -> node.name).collect(Collectors.toSet());
    }

    @Test
    void wholeProjectGraph() {
        var nodes = service.buildProjectGraph(projectModel, false);
        // the overloaded mySPR appears as its own dispatcher node alongside its two versions
        assertEquals(List.of("doSomething", "mySPR", "mySPR [state=AR]", "mySPR [state=AZ]", "theCall"),
                nodes.stream().map(node -> node.name).toList());
        var byName = byName(nodes);
        // theCall reaches the versions only through the dispatcher
        assertEquals(1, byName.get("theCall").dependencies.size());
        assertEquals(1, byName.get("mySPR [state=AR]").dependencies.size());
        assertTrue(byName.get("doSomething").dependencies.isEmpty());
        assertNotNull(byName.get("theCall").project);
        // the project graph exposes forward dependencies only
        assertNull(byName.get("doSomething").dependents);
    }

    @Test
    void reachableTableIdsFollowDependenciesThroughDispatchers() {
        var fromTheCall = service.reachableTableIds(projectModel, idOf("theCall"), GraphDirection.DEPENDENCIES, null);
        assertTrue(fromTheCall.contains(idOf("theCall")), "the root is included");
        // theCall reaches the overloaded versions through the dispatcher.
        assertTrue(fromTheCall.contains(idOf("mySPR [state=AR]")));
        assertTrue(fromTheCall.contains(idOf("mySPR [state=AZ]")));

        // doSomething is a leaf: it reaches only itself, never the tables that call it.
        var fromDoSomething = service.reachableTableIds(projectModel, idOf("doSomething"),
                GraphDirection.DEPENDENCIES, null);
        assertEquals(Set.of(idOf("doSomething")), fromDoSomething);
        assertFalse(fromDoSomething.contains(idOf("theCall")));

        // An unknown root yields nothing.
        assertTrue(service.reachableTableIds(projectModel, "missing", GraphDirection.DEPENDENCIES, null).isEmpty());
    }

    @Test
    void nodesCarrySummaryFields() {
        // every SummaryTableView field is mapped onto the graph node, not only id/name/kind
        var theCall = byName(service.buildProjectGraph(projectModel, false)).get("theCall");
        assertNotNull(theCall.tableType);
        assertNotNull(theCall.signature);
        assertNotNull(theCall.file);
        assertNotNull(theCall.pos);
    }

    @Test
    void candidatesCarryVersioningRules() {
        var byName = byName(service.buildProjectGraph(projectModel, false));
        // each dispatched version exposes the dimension properties that select it (here: state = AR)
        assertTrue(byName.get("mySPR [state=AR]").dimensionProperties.containsValue("AR"));
        assertTrue(byName.get("mySPR [state=AZ]").dimensionProperties.containsValue("AZ"));
        // the dispatcher is a synthetic selector — it has no versioning rules of its own
        assertTrue(byName.get("mySPR").dimensionProperties.isEmpty());
    }

    @Test
    void dispatcherBecomesATechnicalNode() {
        var byName = byName(service.buildProjectGraph(projectModel, false));
        var dispatcher = byName.get("mySPR");
        assertEquals("Dispatcher", dispatcher.kind);
        assertNotNull(dispatcher.project);
        // the dispatcher fans out to the overloaded versions...
        assertEquals(Set.of(byName.get("mySPR [state=AR]").id, byName.get("mySPR [state=AZ]").id),
                dispatcher.dependencies);
        // ...and callers reach the versions only through it
        assertEquals(Set.of(dispatcher.id), byName.get("theCall").dependencies);
    }

    @Test
    void currentModuleGraph() {
        assertFalse(service.buildProjectGraph(projectModel, true).isEmpty());
    }

    @Test
    void tableGraphBothDirections() {
        var byName = byName(service.buildTableGraph(projectModel, idOf("theCall"), GraphDirection.BOTH, null));
        assertEquals(Set.of("theCall", "mySPR", "mySPR [state=AR]", "mySPR [state=AZ]", "doSomething"),
                byName.keySet());
        // theCall → dispatcher → versions
        assertEquals(1, byName.get("theCall").dependencies.size());
        assertEquals(2, byName.get("mySPR").dependencies.size());
        // upstream relations are exposed too in the BOTH direction: the version is used by the dispatcher
        assertEquals(1, byName.get("mySPR [state=AR]").dependents.size());
    }

    @Test
    void tableGraphUpstreamOnly() {
        var byName = byName(service.buildTableGraph(projectModel, idOf("doSomething"), GraphDirection.DEPENDENTS, null));
        // doSomething is used by the AR version, reached through the dispatcher from theCall; AZ does not use doSomething
        assertEquals(Set.of("doSomething", "mySPR [state=AR]", "mySPR", "theCall"), byName.keySet());
        assertEquals(1, byName.get("doSomething").dependents.size());
        // the upstream-only direction does not expose forward dependencies
        assertNull(byName.get("theCall").dependencies);
    }

    @Test
    void tableGraphUpstreamOfTopLevelTable() {
        // theCall is a top-level rule that no other table calls, so its upstream graph is just itself — even though
        // its BOTH/DEPENDENCIES graphs reach the rest of the project.
        var topLevel = idOf("theCall");
        assertEquals(Set.of("theCall"), names(service.buildTableGraph(projectModel, topLevel, GraphDirection.DEPENDENTS, null)));
        assertTrue(service.buildTableGraph(projectModel, topLevel, GraphDirection.DEPENDENCIES, null).size() > 1);
        assertTrue(service.buildTableGraph(projectModel, topLevel, GraphDirection.BOTH, null).size() > 1);
    }

    @Test
    void tableGraphDepthLimit() {
        // depth 1 reaches the dispatcher; the overloaded versions are one hop deeper through it
        assertEquals(Set.of("theCall", "mySPR"),
                names(service.buildTableGraph(projectModel, idOf("theCall"), GraphDirection.DEPENDENCIES, 1)));
        assertEquals(Set.of("theCall", "mySPR", "mySPR [state=AR]", "mySPR [state=AZ]"),
                names(service.buildTableGraph(projectModel, idOf("theCall"), GraphDirection.DEPENDENCIES, 2)));
    }

    @Test
    void tableGraphUnknownRootIsEmpty() {
        assertTrue(service.buildTableGraph(projectModel, "missing", GraphDirection.BOTH, null).isEmpty());
    }

    @Test
    void recursiveTableKeepsSelfDependency() throws Exception {
        // a Spreadsheet whose cell calls itself: the binder records the self-reference, and the graph must keep it
        var recursionModel = new ProjectModel(mock(WebStudio.class), null);
        var module = ProjectResolver.getInstance()
                .resolve(Path.of("test-resources/org/openl/studio/projects/service/tables/graph/Recursion"))
                .getModules()
                .getFirst();
        recursionModel.setModuleInfo(module);

        var selfReferencing = service.buildProjectGraph(recursionModel, false)
                .stream()
                .filter(node -> node.dependencies != null && node.dependencies.contains(node.id))
                .toList();
        assertEquals(1, selfReferencing.size(), "the recursive table is linked to itself");
        assertTrue(selfReferencing.getFirst().name.contains("recCall"));
    }
}
