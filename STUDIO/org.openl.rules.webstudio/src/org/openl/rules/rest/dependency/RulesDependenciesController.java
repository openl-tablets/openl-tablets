package org.openl.rules.rest.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.openl.rules.dependency.graph.DependencyRulesGraph;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.rest.service.WorkspaceProjectService;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.types.impl.ExecutableMethod;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Project Tables")
public class RulesDependenciesController {

    private final WorkspaceProjectService projectService;

    public RulesDependenciesController(WorkspaceProjectService projectService) {
        this.projectService = projectService;
    }

    @Lookup
    public WebStudio getWebStudio() {
        return null;
    }

    @GetMapping("/project/tables")
    @Operation(summary = "project.tables.summary", description = "project.tables.desc")
    public List<Table> getTablesWithDependencies() {
        var webStudio = getWebStudio();
        if (webStudio == null || webStudio.getModel() == null) {
            return Collections.emptyList();
        }
        return getTablesWithDependencies(webStudio.getModel());
    }

    // for testing purposes only
    @Hidden
    @GetMapping("/projects/{projectId}/tables/graph")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public List<Table> getTablesWithDependencies(@PathVariable("projectId") RulesProject project) {
        return getTablesWithDependencies(projectService.getProjectModel(project));
    }

    private List<Table> getTablesWithDependencies(ProjectModel projectModel) {
        var webStudio = getWebStudio();
        List<Table> tables = new ArrayList<>();
        DependencyRulesGraph graph = projectModel.getDependencyGraph();

        var levelIterator = new TopologicalOrderIterator<>(graph);
        while (levelIterator.hasNext()) {
            ExecutableMethod rulesMethod = levelIterator.next();
            Table table = new Table();
            table.setName(rulesMethod.getName());
            String tableUri = rulesMethod.getSourceUrl();
            table.setId(TableUtils.makeTableId(tableUri));
            table.setUrl(webStudio.url("table", tableUri));

            var outgoingEdges = graph.outgoingEdgesOf(rulesMethod);
            outgoingEdges.stream()
                    .map(edge -> TableUtils.makeTableId(edge.getTargetVertex().getSourceUrl()))
                    .filter(depId -> !depId.equals(table.getId()))
                    .forEach(table.getDependencies()::add);

            tables.add(table);
        }

        return tables;
    }

}
