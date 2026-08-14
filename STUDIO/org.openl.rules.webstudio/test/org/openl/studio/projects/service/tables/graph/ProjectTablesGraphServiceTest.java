package org.openl.studio.projects.service.tables.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import org.openl.studio.projects.model.tables.DatatypeNodeFieldView;
import org.openl.studio.projects.model.tables.DatatypeNodeView;
import org.openl.studio.projects.model.tables.ExecutableNodeView;
import org.openl.studio.projects.model.tables.TableGraphNodeKind;
import org.openl.studio.projects.model.tables.TableNodeView;
import org.openl.studio.projects.service.tables.read.SummaryTableReader;

class ProjectTablesGraphServiceTest {

    private static ProjectModel projectModel;
    private static ProjectModel dataModel;
    private final ProjectTablesGraphService service = new ProjectTablesGraphService(new SummaryTableReader());

    @BeforeAll
    static void compileProject() throws Exception {
        projectModel = compile("Project");
        dataModel = compile("DataModel");
    }

    static ProjectModel compile(String project) throws Exception {
        var model = new ProjectModel(mock(WebStudio.class), null);
        var module = ProjectResolver.getInstance()
                .resolve(Path.of("test-resources/org/openl/studio/projects/service/tables/graph/" + project))
                .getModules()
                .getFirst();
        model.setModuleInfo(module);
        return model;
    }

    private List<TableNodeView> projectGraph(ProjectModel model) {
        return service.buildProjectGraph(model, false, GraphLayer.ALL);
    }

    private Map<String, TableNodeView> byName(List<TableNodeView> nodes) {
        return nodes.stream().collect(Collectors.toMap(node -> node.name, Function.identity()));
    }

    private ExecutableNodeView executable(Map<String, TableNodeView> byName, String name) {
        return assertInstanceOf(ExecutableNodeView.class, byName.get(name));
    }

    private String idOf(String name) {
        return byName(projectGraph(projectModel)).get(name).id;
    }

    private Set<String> names(List<TableNodeView> nodes) {
        return nodes.stream().map(node -> node.name).collect(Collectors.toSet());
    }

    @Test
    void wholeProjectGraph() {
        var nodes = projectGraph(projectModel);
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
        var theCall = executable(byName(projectGraph(projectModel)), "theCall");
        assertNotNull(theCall.tableType);
        assertNotNull(theCall.signature);
        assertNotNull(theCall.file);
        assertNotNull(theCall.pos);
    }

    @Test
    void candidatesCarryVersioningRules() {
        var byName = byName(projectGraph(projectModel));
        // each dispatched version exposes the dimension properties that select it (here: state = AR)
        assertTrue(executable(byName, "mySPR [state=AR]").dimensionProperties.containsValue("AR"));
        assertTrue(executable(byName, "mySPR [state=AZ]").dimensionProperties.containsValue("AZ"));
        // the dispatcher is a synthetic selector — it has no versioning rules of its own
        assertTrue(executable(byName, "mySPR").dimensionProperties.isEmpty());
    }

    @Test
    void dispatcherBecomesATechnicalNode() {
        var byName = byName(projectGraph(projectModel));
        var dispatcher = byName.get("mySPR");
        assertEquals(TableGraphNodeKind.DISPATCHER, dispatcher.kind);
        assertNotNull(dispatcher.project);
        // the dispatcher fans out to the overloaded versions...
        assertEquals(Set.of(byName.get("mySPR [state=AR]").id, byName.get("mySPR [state=AZ]").id),
                dispatcher.dependencies);
        // ...and callers reach the versions only through it
        assertEquals(Set.of(dispatcher.id), byName.get("theCall").dependencies);
    }

    @Test
    void currentModuleGraph() {
        assertFalse(service.buildProjectGraph(projectModel, true, GraphLayer.ALL).isEmpty());
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
        var recursionModel = compile("Recursion");

        var selfReferencing = projectGraph(recursionModel)
                .stream()
                .filter(node -> node.dependencies != null && node.dependencies.contains(node.id))
                .toList();
        assertEquals(1, selfReferencing.size(), "the recursive table is linked to itself");
        assertTrue(selfReferencing.getFirst().name.contains("recCall"));
    }

    private DatatypeNodeView datatype(Map<String, TableNodeView> byName, String name) {
        return assertInstanceOf(DatatypeNodeView.class, byName.get(name));
    }

    private DatatypeNodeFieldView field(DatatypeNodeView node, String name) {
        return node.fields.stream()
                .filter(field -> name.equals(field.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No field " + name + " on " + node.name));
    }

    @Test
    void dataModelJoinsTheGraph() {
        var byName = byName(projectGraph(dataModel));
        assertEquals(Set.of("calcPremium", "Car", "Driver", "Policy", "Region", "Vehicle"), byName.keySet());
        assertEquals(TableGraphNodeKind.DATATYPE, byName.get("Policy").kind);
        assertNotNull(byName.get("Policy").file);
        assertNotNull(byName.get("Policy").pos);
        // a vocabulary is a datatype table narrowing a simple type: same node kind, its own table type
        assertEquals(TableGraphNodeKind.DATATYPE, byName.get("Region").kind);
        assertEquals("Vocabulary", byName.get("Region").tableType);
        // the opened module sees the same data model
        assertEquals(byName.keySet(), byName(service.buildProjectGraph(dataModel, true, GraphLayer.ALL)).keySet());
    }

    @Test
    void datatypeExtendsItsParent() {
        var byName = byName(projectGraph(dataModel));
        var car = datatype(byName, "Car");
        assertEquals(byName.get("Vehicle").id, car.extendz);
        assertEquals(Set.of(byName.get("Vehicle").id), car.dependencies);
        // only the declared field is listed, the inherited ones belong to the parent node
        assertEquals(List.of("model"), car.fields.stream().map(DatatypeNodeFieldView::name).toList());
        assertEquals(List.of("make", "year"), datatype(byName, "Vehicle").fields.stream()
                .map(DatatypeNodeFieldView::name).toList());
    }

    @Test
    void datatypeFieldsReferToTheirDatatypes() {
        var byName = byName(projectGraph(dataModel));
        var policy = datatype(byName, "Policy");
        assertEquals(Set.of(byName.get("Car").id, byName.get("Driver").id, byName.get("Region").id),
                policy.dependencies);

        assertEquals(byName.get("Car").id, field(policy, "car").ref());
        assertNull(field(policy, "car").collection());

        // a collection field refers to the datatype it holds, and says that it holds many of them
        var drivers = field(policy, "drivers");
        assertEquals(byName.get("Driver").id, drivers.ref());
        assertEquals("Driver[]", drivers.type());
        assertTrue(drivers.collection());

        // a field of a simple type is still listed, but refers to no other table
        assertEquals("String", field(policy, "policyNumber").type());
        assertNull(field(policy, "policyNumber").ref());

        // the fields read in the order the table declares them, not sorted
        assertEquals(List.of("car", "drivers", "region", "policyNumber"),
                policy.fields.stream().map(DatatypeNodeFieldView::name).toList());
    }

    @Test
    void datatypeFieldOfItsOwnTypeIsASelfLoop() {
        var byName = byName(projectGraph(dataModel));
        var driver = datatype(byName, "Driver");
        assertEquals(driver.id, field(driver, "mentor").ref());
        assertEquals(Set.of(driver.id), driver.dependencies);
    }

    @Test
    void rulesTablesAreNotLinkedToTheDataModel() {
        var byName = byName(projectGraph(dataModel));
        // calcPremium takes a Policy, but the data model is its own layer of the graph
        assertTrue(executable(byName, "calcPremium").dependencies.isEmpty());
        assertTrue(byName(service.buildTableGraph(dataModel, byName.get("Policy").id, GraphDirection.DEPENDENTS, null))
                .keySet()
                .stream()
                .noneMatch("calcPremium"::equals));
        // and the other way round: a graph rooted at a rules table stays free of the data model, in either direction
        assertEquals(Set.of("calcPremium"),
                names(service.buildTableGraph(dataModel, byName.get("calcPremium").id, GraphDirection.BOTH, null)));
    }

    @Test
    void oneLayerOfTheGraphCanBeAskedForOnItsOwn() {
        // the data model alone — every datatype of the project, with the relations it has in the whole graph
        var datatypes = byName(service.buildProjectGraph(dataModel, false, GraphLayer.DATATYPE));
        assertEquals(Set.of("Car", "Driver", "Policy", "Region", "Vehicle"), datatypes.keySet());
        assertEquals(byName(projectGraph(dataModel)).get("Policy").dependencies, datatypes.get("Policy").dependencies);

        // the callable tables alone — the graph as it was before the data model joined it
        assertEquals(Set.of("calcPremium"),
                names(service.buildProjectGraph(dataModel, false, GraphLayer.EXECUTABLE)));
    }

    @Test
    void datatypeRootedGraphFollowsTheDataModel() {
        var byName = byName(projectGraph(dataModel));
        var policyId = byName.get("Policy").id;
        // everything Policy is built from, the parent of its Car field included
        assertEquals(Set.of("Policy", "Car", "Driver", "Region", "Vehicle"),
                names(service.buildTableGraph(dataModel, policyId, GraphDirection.DEPENDENCIES, null)));
        // upstream: what is built on a Vehicle — the Car that extends it, and the Policy that holds that Car
        assertEquals(Set.of("Vehicle", "Car", "Policy"),
                names(service.buildTableGraph(dataModel, byName.get("Vehicle").id, GraphDirection.DEPENDENTS, null)));
    }

    @Test
    void referencesOutsideTheGraphAreDropped() {
        var byName = byName(projectGraph(dataModel));
        var oneHop = byName(service.buildTableGraph(dataModel, byName.get("Policy").id, GraphDirection.DEPENDENCIES, 1));
        assertEquals(Set.of("Policy", "Car", "Driver", "Region"), oneHop.keySet());
        // Vehicle is one hop too far, so Car is reported without the parent it cannot address here
        assertNull(datatype(oneHop, "Car").extendz);
        assertTrue(datatype(oneHop, "Car").dependencies.isEmpty());
    }
}
