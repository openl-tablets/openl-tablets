package org.openl.rules.webstudio.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.openl.rules.dependency.graph.DependencyRulesGraph;
import org.openl.rules.dependency.graph.DirectedEdge;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.impl.ExecutableMethod;
import org.openl.util.StringTool;

/**
 * @author Andrei Astrouski
 */
public class RulesDependenciesBean {

    public RulesDependenciesBean() {
    }

    public DependencyRulesGraph getDependencyGraph() {
        ProjectModel projectModel = WebStudioUtils.getProjectModel();
        return projectModel.getDependencyGraph();
    }

    public List<Table> getTablesWithDependencies() {
        List<Table> tables = new ArrayList<Table>();
        DependencyRulesGraph graph = getDependencyGraph();

        for (ExecutableMethod rulesMethod : graph.vertexSet()) {
            Table table = new Table();
            table.setName(rulesMethod.getName());
            String tableUri = StringTool.encodeURL(rulesMethod.getSourceUrl());
            table.setUri(tableUri);

            Set<DirectedEdge<ExecutableMethod>> outgoingEdges = graph.outgoingEdgesOf(rulesMethod);
            List<String> dependencies = table.getDependencies();
            for (DirectedEdge<ExecutableMethod> edge : outgoingEdges) {
                String depUri = StringTool.encodeURL(edge.getTargetVertex().getSourceUrl());
                if (!depUri.equals(tableUri)) {
                    dependencies.add(depUri);
                }
            }
            if (outgoingEdges.size() > 0) {
                // Tables with dependencies should be in the top of list
                tables.add(0, table);
            } else {
                tables.add(table);
            }
        }

        return tables;
    }

    public class Table {

        private String name;
        private String uri;

        private List<String> dependencies = new ArrayList<String>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
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
