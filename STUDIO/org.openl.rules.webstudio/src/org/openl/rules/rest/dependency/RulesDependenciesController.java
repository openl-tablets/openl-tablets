package org.openl.rules.rest.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.jgrapht.traverse.TopologicalOrderIterator;
import org.openl.rules.dependency.graph.DependencyRulesGraph;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.impl.ExecutableMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping(value = "/project/tables", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Project Tables")
public class RulesDependenciesController {

    @GetMapping
    @Operation(summary = "project.tables.summary", description = "project.tables.desc")
    public List<Table> getTablesWithDependencies(HttpSession session) {
        WebStudio webStudio = WebStudioUtils.getWebStudio(session);
        if (webStudio == null || webStudio.getModel() == null) {
            return Collections.emptyList();
        }
        List<Table> tables = new ArrayList<>();
        DependencyRulesGraph graph = webStudio.getModel().getDependencyGraph();

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
