package org.openl.rules.datatype.binding;

import java.util.*;

import org.openl.binding.IBindingContext;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.datatype.binding.TopologicalSort.TopoGraphNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * Created by dl on 6/13/14.
 */
public class DatatypesSorter {

    public TableSyntaxNode[] sort(Map<String, TableSyntaxNode> datatypes,
                                  IBindingContext bindingContext) {
        if (datatypes == null) {
            return null;
        }

        Set<TopoGraphNode<TableSyntaxNode>> sorted = new TopologicalSort<TableSyntaxNode>().sort(
                wrapAll(datatypes, bindingContext));

        return unwrapAll(sorted);
    }

    private List<TopoGraphNode<TableSyntaxNode>> wrapAll(Map<String, TableSyntaxNode> datatypes,
                                                         IBindingContext bindingContext) {
        List<TopoGraphNode<TableSyntaxNode>> toSort = new ArrayList<TopoGraphNode<TableSyntaxNode>>();
        for (TableSyntaxNode datatype : datatypes.values()) {
            toSort.add(wrap(datatype, datatypes, bindingContext, new HashMap<String, TopoGraphNode<TableSyntaxNode>>()));
        }
        return toSort;
    }

    private TableSyntaxNode[] unwrapAll(Set<TopoGraphNode<TableSyntaxNode>> sorted) {
        TableSyntaxNode[] result = new TableSyntaxNode[sorted.size()];
        Iterator<TopoGraphNode<TableSyntaxNode>> iterator = sorted.iterator();
        for (int i = 0; iterator.hasNext(); i++) {
            TopoGraphNode<TableSyntaxNode> element = iterator.next();
            if (element != null) {
                result[i] = element.getObj();
            }
        }
        return result;
    }

    private TopologicalSort.TopoGraphNode<TableSyntaxNode> wrap(
            TableSyntaxNode datatype, Map<String, TableSyntaxNode> datatypes,
            IBindingContext bindingContext, Map<String, TopoGraphNode<TableSyntaxNode>> dependentQueue) {

        DependentTypesExtractor dependeciesExtractor = new DependentTypesExtractor();
        Set<String> dependencies = dependeciesExtractor.extract(datatype, bindingContext);
        TopoGraphNode<TableSyntaxNode> forSort = new TopoGraphNode<TableSyntaxNode>(datatype);
        if (dependencies.isEmpty()) {
            return forSort;
        } else {
            String currentName = null;
            try {
                currentName = DatatypeHelper.getDatatypeName(datatype);
            } catch (OpenLCompilationException e) {
                // Suppress the exception
                //
            }
            for (String dependency : dependencies) {
                if (datatypes.containsKey(dependency)
                        // Avoid recursive dependencies
                        //
                        && !dependency.equals(currentName)) {
                    if (dependentQueue.containsKey(dependency)) {
                        // Avoid recursive dependencies
                        forSort.addDependency(dependentQueue.get(dependency));
                    } else {
                        dependentQueue.put(currentName, forSort);
                        forSort.addDependency(wrap(datatypes.get(dependency),
                                datatypes,
                                bindingContext,
                                dependentQueue));
                        dependentQueue.remove(currentName);
                    }
                }
            }
            return forSort;
        }

    }
}
