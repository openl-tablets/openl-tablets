package org.openl.rules.rest.dependency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.types.impl.ExecutableMethod;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Project Tables")
public class RulesDependenciesController {

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

    private List<Table> getTablesWithDependencies(ProjectModel projectModel) {
        List<Table> tables = new ArrayList<>();
        var methods = projectModel.getCompiledOpenClass().getOpenClassWithErrors().getMethods();
        if (methods == null || methods.isEmpty()) {
            return tables;
        }

        var webStudio = getWebStudio();
        var methodNodesDictionary = projectModel.getMethodNodesDictionary();
        var formats = WebStudioFormats.getInstance();

        var queue = new LinkedList<>(methods);
        while (!queue.isEmpty()) {
            var method = queue.poll();
            if (method instanceof ExecutableMethod rulesMethod) {
                var table = new Table();
                rulesMethod.getDisplayName(0);
                var displayNames = TableSyntaxNodeUtils.getTableDisplayValue((TableSyntaxNode) rulesMethod.getInfo().getSyntaxNode(),
                        0,
                        methodNodesDictionary,
                        formats);
                table.setName(displayNames[INamedThing.SHORT]);
                String tableUri = rulesMethod.getSourceUrl();
                table.setId(TableUtils.makeTableId(tableUri));
                table.setUrl(webStudio.url("table", tableUri));
                var dependencies = rulesMethod.getDependencies();
                if (dependencies != null) {
                    var dependentMethods = dependencies.getRulesMethods();
                    if (dependentMethods != null) {
                        for (var dependentMethod : dependentMethods) {
                            table.getDependencies().add(TableUtils.makeTableId(dependentMethod.getSourceUrl()));
                        }
                    }
                }

                tables.add(table);
            } else if (method instanceof OpenMethodDispatcher dispatcher) {
                queue.addAll(dispatcher.getCandidates());
            }
        }

        tables.sort(Comparator.comparing(Table::getName, String.CASE_INSENSITIVE_ORDER));
        return tables;
    }

}
