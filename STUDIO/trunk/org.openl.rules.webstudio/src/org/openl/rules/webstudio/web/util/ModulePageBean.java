package org.openl.rules.webstudio.web.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.WebStudio;

@ManagedBean
@RequestScoped
public class ModulePageBean {
    private WebStudio studio = WebStudioUtils.getWebStudio();
/**
 * 
 * @return Map &lt;String, List&lt;String&gt;&gt; of the imports from Environment table
 */
    public Map<String, List<String>> getTableSyntaxNodes() {
        List<String> importValues;
        String key;
        String value;
        // Getting nodes from Model
        TableSyntaxNode[] nodes = studio.getModel().getTableSyntaxNodes();

        // Creating a list of environment tables. If tables more than 1
        List<TableSyntaxNode> envNodesTables = new LinkedList<TableSyntaxNode>();

        Map<String, List<String>> imports = new HashMap<String, List<String>>();

        // Filling the envNodesTables List by TableSyntaxNodes
        for (TableSyntaxNode node : nodes) {
            if ("xls.environment".equals(node.getType())) {
                envNodesTables.add(node);
            }
        }

        for (TableSyntaxNode node : envNodesTables) {
            for (int row = 1; row < node.getGridTable().getHeight(); row++) {
                key = node.getGridTable().getCell(0, row).getStringValue();
                value = node.getGridTable().getCell(1, row).getStringValue();

                if (imports.containsKey(key)) {
                    imports.get(key).add(value);
                } else {
                    importValues = new ArrayList<String>();
                    importValues.add(value);
                    imports.put(key, importValues);
                }
            }
        }

        return imports;
    }
/**
 * 
 * @return List of all imoirts
 */
    public List<String> getImports() {
        return getTableSyntaxNodes().get("import");
    }

/**
 * 
 * @return List of all includes
 */
    public List<String> getIncludes() {
        List<String> includeList = getTableSyntaxNodes().get("include");
        String[] includedFiles;
        List<String> includedModulesList = new ArrayList<String>();

        if (includeList != null) {
            for (String include : includeList) {
                includedFiles = include.split("/");

                includedModulesList.add(includedFiles[includedFiles.length - 1].split(".xls")[0]);
            }
        }

        return includedModulesList;
    }

    public List<String> getDependencies () {
        return getTableSyntaxNodes().get("dependency");
    }
}
