package org.openl.rules.webstudio.web.search;

import java.util.List;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public interface AISearch {
    List<TableSyntaxNode> filter(String query, List<TableSyntaxNode> tableSyntaxNodes);
}
