package org.openl.rules.indexer;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.openl.rules.indexer.Index.TokenBucket;

/**
 * During initialization, gets the elements for inclusion and exclusion in search.
 * Searches by the index using exclusion and inclusion search elements. see {@link IndexQuery#execute(Index)} 
 */
public class IndexQuery {
    
    private static final int ELEMENT_PRESENCE_WEIGHT = 20;
    private static final int CONTAINS_STR_WEIGHT = 100;

    private String[][] tokensInclude = null;
    // protected for tests
    protected String[][] tokensExclude = {};

    private IIndexElement[] indexExclude = {};

    private HashSet<IIndexElement> excludedIndexes;

    static  public <K,V> Map<K,V> intersect(Map<K,V> m1, Map<K,V> m2) {
        // TreeMap tm = new TreeMap(exclusions2.comparator());

        Map<K,V> tm = new HashMap<K,V>();

        for (Iterator<Map.Entry<K, V>> iter = m2.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<K,V> element = iter.next();
            if (m1.containsKey(element.getKey())) {
                tm.put(element.getKey(), element.getValue());
            }
        }

        return tm;
    }

    public IndexQuery(String[][] tokensInclude, String[][] tokensExclude, IIndexElement[] indexExclude) {
        this.tokensInclude = tokensInclude;
        if (tokensExclude != null) {
            this.tokensExclude = tokensExclude;
        }
        if (indexExclude != null) {
            this.indexExclude = indexExclude;
        }
    }
    
    /**
     * Initialize the indexes for exclusion elements, according with tokens that 
     * must be excluded from search result
     * @param idx Index of files.
     */
    private void makeExclusions(Index idx) {

        excludedIndexes = new HashSet<IIndexElement>(indexExclude.length);
        for (int i = 0; i < indexExclude.length; i++) {
            excludedIndexes.add(indexExclude[i]);
        }

        for (int i = 0; i < tokensExclude.length; i++) {

            Map<String, HitBucket> exclusions = null;
            for (int j = 0; j < tokensExclude[i].length; j++) {
                String tokenExclude = tokensExclude[i][j];
                TokenBucket tb = idx.findEqualsTokenBucket(tokenExclude);   
                if(tb != null) {
                    if (j == 0) {
                        exclusions = tb.getIndexElements();
                    } else {
                        exclusions = intersect(exclusions, tb.getIndexElements());
                    }
                }                
            }
            if(exclusions != null) {
                for (Iterator<HitBucket> iter = exclusions.values().iterator(); iter.hasNext();) {
                    HitBucket hb = (HitBucket) iter.next();
                    excludedIndexes.add(hb.getElement());
                }
            }
        }

    }    
    
    /**
     * 
     * @param idx
     * @return Map of search result, where there key is an uri and value is an element satisfying the search. 
     */
    private Map<String, HitBucket> makeInclusions(Index idx) {
        Map<String, HitBucket> allInclusions = new HashMap<String, HitBucket>();

        for (int i = 0; i < tokensInclude.length; i++) {
            Map<String, HitBucket> inclusions = makeInclusions(idx, tokensInclude[i]);

            for (Iterator<HitBucket> iter = inclusions.values().iterator(); iter.hasNext();) {
                HitBucket hb = (HitBucket) iter.next();
                String uri = hb.getElement().getUri();
                HitBucket hbInc = (HitBucket) allInclusions.get(uri);

                if (hbInc == null) {
                    allInclusions.put(uri, hb);
                } else {
                    hbInc.setWeight(hbInc.getWeight() * ELEMENT_PRESENCE_WEIGHT * hb.getWeight());
                }
            }
        }
        return allInclusions;
    }

    /**
     * At first we try to get the {@TokenBucket} by a strict equals condition. If there is no such element,
     * we try to find token buckets that contains current token.
     * @param idx Index contains indexed data.  
     * @param tokens Tokens to be found.
     * @return
     */
    private Map<String, HitBucket> makeInclusions(Index idx, String[] tokens) {
        
        String searchStr = "";
        for (String token : tokens) {
            searchStr += " " + token;
        }

        searchStr = searchStr.toLowerCase();

        Map<String, HitBucket> inclusions = new HashMap<String, HitBucket>();

        for (int i = 0; i < tokens.length; i++) {
            TokenBucket tb = idx.findEqualsTokenBucket(tokens[i]);
            if (tb == null) {
                List<TokenBucket> tbs = idx.findContainTokenBuckets(tokens[i]);
                for(TokenBucket tb1 : tbs) {
                    inclusions.putAll(include(tb1,i,searchStr));
                }
                continue;
            }
            inclusions.putAll(include(tb, i, searchStr));            
        }
        return inclusions;
    }
    
    
    
    private Map<String, HitBucket> include(TokenBucket tb, int i, String searchStr) {
        Map<String, HitBucket> inclusions = new HashMap<String, HitBucket>();
        if (i == 0) {
            for (Iterator<HitBucket> iter = tb.getIndexElements().values().iterator(); iter.hasNext();) {
                HitBucket hb = iter.next();
                if (!excludedIndexes.contains(hb.getElement())) {
                    HitBucket hbInc = new HitBucket(hb);
                    boolean contains = hbInc.getElement().getIndexedText().toLowerCase().indexOf(searchStr) >= 0;
                    if (contains) {
                        hbInc.setWeight(hbInc.getWeight() * CONTAINS_STR_WEIGHT);
                    }
                    inclusions.put(hb.getElement().getUri(), hbInc);
                }
            }
        } else {
            Map<String, HitBucket> myInclusions = new HashMap<String, HitBucket>();
            for (Iterator<HitBucket> iter = tb.getIndexElements().values().iterator(); iter.hasNext();) {

                HitBucket hb = (HitBucket) iter.next();
                String uri = hb.getElement().getUri();
                HitBucket hbInc = (HitBucket) inclusions.get(uri);

                if (hbInc != null) {
                    hbInc.setWeight(hb.getWeight() + hbInc.getWeight());
                    myInclusions.put(uri, hbInc);
                }
            }
            inclusions = myInclusions;
        }       
        return inclusions;        
    }

    public String[][] getTokensInclude() {
        return tokensInclude;
    }
    
    public void setTokensExclude(String[][] tokensExclude) {
        this.tokensExclude = tokensExclude;
    }
    
    public void setIndexExclude(IIndexElement[] indexExclude) {
        this.indexExclude = indexExclude;
    } 
    
    /**
     * Process the index with the search query.  
     * @param idx Indexed data.
     * @return 
     */
    public TreeSet<HitBucket> execute(Index idx) {
        TreeSet<HitBucket> result = new TreeSet<HitBucket>();        
        makeExclusions(idx);
        Map<String, HitBucket> allInc = makeInclusions(idx);

        for (Iterator<HitBucket> iter = allInc.values().iterator(); iter.hasNext();) {
            HitBucket hb = (HitBucket) iter.next();
            result.add(hb);
        }
        return result;
    }
    
}
