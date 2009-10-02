package org.openl.rules.search;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * Class handles the result of business search
 * @author DLiauchuk
 *
 */
public class OpenLBussinessSearchResult {
    
    List<TableSyntaxNode> foundTables = new ArrayList<TableSyntaxNode>();
    
    public void add(TableSyntaxNode tsn) {
        foundTables.add(tsn);
    }
    
    public List<TableSyntaxNode> getFoundTables() {
        return foundTables;
    }
}
