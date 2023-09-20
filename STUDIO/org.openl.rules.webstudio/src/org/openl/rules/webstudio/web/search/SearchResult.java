package org.openl.rules.webstudio.web.search;

import java.util.Collections;
import java.util.List;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class SearchResult {
    public static final SearchResult EMPTY = new SearchResult(Collections.emptyList(), 0, 0);
    private final int tableCountForIndexing;
    private final int expectedIndexingDuration;
    List<TableSyntaxNode> tableSyntaxNodes;

    public SearchResult(List<TableSyntaxNode> tableSyntaxNodes,
            int tableCountForIndexing,
            int expectedIndexingDuration) {
        this.tableCountForIndexing = tableCountForIndexing;
        this.expectedIndexingDuration = expectedIndexingDuration;
        this.tableSyntaxNodes = tableSyntaxNodes;
    }

    public int getTableCountForIndexing() {
        return tableCountForIndexing;
    }

    public int getExpectedIndexingDuration() {
        return expectedIndexingDuration;
    }

    public List<TableSyntaxNode> getTableSyntaxNodes() {
        return tableSyntaxNodes;
    }

}
