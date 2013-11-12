package org.openl.rules.indexer;

public interface IIndexer {
    String getCategory();

    String getType();

    void index(IIndexElement element, Index index);

}
