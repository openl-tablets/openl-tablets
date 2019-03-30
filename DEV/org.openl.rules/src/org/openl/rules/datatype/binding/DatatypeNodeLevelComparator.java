package org.openl.rules.datatype.binding;

import java.util.Comparator;
import java.util.Map;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * Compares datatype TableSyntaxNodes. This comparing is needed to build queue of datatypes for binding phase(We have to
 * bind parent datatype before all datatypes that inherits it)
 * 
 * @author PUdalau
 * 
 */
public class DatatypeNodeLevelComparator implements Comparator<TableSyntaxNode> {

    private Map<TableSyntaxNode, Integer> levelsMap;

    public DatatypeNodeLevelComparator(Map<TableSyntaxNode, Integer> levelsMap) {
        this.levelsMap = levelsMap;
    }

    @Override
    public int compare(TableSyntaxNode first, TableSyntaxNode second) {
        int firstLevel = levelsMap.get(first);
        int secondLevel = levelsMap.get(second);

        return firstLevel - secondLevel;
    }
}
