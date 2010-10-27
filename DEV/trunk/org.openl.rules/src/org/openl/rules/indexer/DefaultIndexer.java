package org.openl.rules.indexer;

import org.apache.commons.lang.StringUtils;

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
                if (StringUtils.isNotBlank(token)) {
                    index.add(token, element);
                }
            }
        }
    }

}
