package org.openl.rules.indexer;

/**
 * Index all the elements in file going down by its structure hierarchy. 
 */
public class IndexRunner {

    private IIndexer[] indexers;

    protected IIndexer defaultIndexer = new DefaultIndexer();

    private IIndexParser[] parsers;
    
    public IndexRunner(IIndexParser[] parsers, IIndexer[] indexers, IIndexer defaultIndexer) {
        this.parsers = parsers;
        this.indexers = indexers;
        this.defaultIndexer = defaultIndexer;
    }
    
    /**
     * Finds the appropriate indexer for element. At first it tries to find parser both by type and 
     * category of the element. If none, just only by category. If none returns {@link DefaultIndexer}.
     * @param element
     * @return
     */
    private IIndexer findIndexer(IIndexElement element) {
        for (int i = 0; i < indexers.length; i++) {
            if (indexers[i].getType().equals(element.getType())
                    && indexers[i].getCategory().equals(element.getCategory())) {
                return indexers[i];
            }
        }

        for (int i = 0; i < indexers.length; i++) {
            if (indexers[i].getCategory().equals(element.getCategory())) {
                return indexers[i];
            }
        }

        return defaultIndexer;
    }
    
   
    
    /**
     * Finds the appropriate parser for element. At first it tries to find parser both by type and 
     * category of the element. If none, just only by category. If none returns null.
     * @param element
     * @return
     */
    private IIndexParser findParser(IIndexElement element) {        
        for (int i = 0; i < parsers.length; i++) {
            if (parsers[i].getType().equals(element.getType())
                    && parsers[i].getCategory().equals(element.getCategory())) {
                return parsers[i];
            }
        }

        for (int i = 0; i < parsers.length; i++) {
            if (parsers[i].getCategory().equals(element.getCategory())) {
                return parsers[i];
            }
        }

        return null;
    }
    
    /**
     * At first it index the element, then parse it to sub elements and 
     * call index for every child in hierarchy.
     * @param element
     * @param index
     */
    public void index(IIndexElement element, Index index) {
        IIndexParser parser = findParser(element);
        IIndexer indexer = findIndexer(element);

        if (indexer != null) {
            indexer.index(element, index);
        }

        if (parser != null) {
            IIndexElement[] elements = parser.parse(element);
            for (int i = 0; i < elements.length; i++) {
                index(elements[i], index);
            }
        }
    }

}
