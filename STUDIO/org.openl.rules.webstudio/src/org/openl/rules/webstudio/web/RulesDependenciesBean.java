package org.openl.rules.webstudio.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openl.rules.dependency.graph.DependencyRulesGraph;
import org.openl.rules.dependency.graph.DirectedEdge;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.impl.ExecutableMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

/**
 * @author Andrei Astrouski
 */
@Service
@RequestScope
public class RulesDependenciesBean {

    public DependencyRulesGraph getDependencyGraph() {
        ProjectModel projectModel = WebStudioUtils.getProjectModel();
        return projectModel.getDependencyGraph();
    }

    public List<Table> getTablesWithDependencies() {
        List<Table> tables = new ArrayList<>();
        DependencyRulesGraph graph = getDependencyGraph();

        for (ExecutableMethod rulesMethod : graph.vertexSet()) {
            Table table = new Table();
            table.setName(rulesMethod.getName());
            String tableUri = rulesMethod.getSourceUrl();
            String tableId = TableUtils.makeTableId(tableUri);
            table.setId(tableId);

            Set<DirectedEdge<ExecutableMethod>> outgoingEdges = graph.outgoingEdgesOf(rulesMethod);
            List<String> dependencies = table.getDependencies();
            for (DirectedEdge<ExecutableMethod> edge : outgoingEdges) {
                String depId = TableUtils.makeTableId(edge.getTargetVertex().getSourceUrl());
                if (!depId.equals(tableId)) {
                    dependencies.add(depId);
                }
            }
            if (!outgoingEdges.isEmpty()) {
                // Tables with dependencies should be in the top of list
                tables.add(0, table);
            } else {
                tables.add(table);
            }
        }

        return tables;
    }

    public static class Table {

        private String name;
        private String id;

        private List<String> dependencies = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public List<String> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<String> dependencies) {
            this.dependencies = dependencies;
        }

        @Override
        public String toString() {
            return getName();
        }

    }

}
