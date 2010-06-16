package org.openl.rules.indexer;

public interface IIndexParser {
    String getCategory();

    String getType();

    IIndexElement[] parse(IIndexElement root);
}
