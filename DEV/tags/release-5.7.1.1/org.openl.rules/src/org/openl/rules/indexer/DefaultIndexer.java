package org.openl.rules.indexer;

public class DefaultIndexer implements IIndexer {

    public String getCategory() {
        return "All";
    }

    public String getType() {
        return "All";
    }

    public void index(IIndexElement element, Index index) {
        String indexedText = element.getIndexedText();
        if (indexedText != null) {
            String[] tokens = Tokenizer.parse(indexedText);

            for (String token : tokens) {
                index.add(token, element);
            }
        }
    }

}
