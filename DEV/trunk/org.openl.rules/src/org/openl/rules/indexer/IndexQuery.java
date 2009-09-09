package org.openl.rules.indexer;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.openl.rules.indexer.Index.TokenBucket;

/**
 * Contains parsed search request as elements that must be included in search and excluded from it.
 * Using method {@link IndexQuery#executeSearch(Index)} you can find all elements from indexed data that 
 * match to the parsed search request.  
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
     * Initialize elements that match to the tokens that must be excluded from search,
     * to exclude this elements in final search result. 
     * @param idx Indexed data.
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
     * Goes through the tokens parsed from the search query and gets the results from Index.
     * @param idx Indexed data.
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
     * Construct the result for array of tokens. If array contains only one token, 
     * we process it as a single word. Also if there are several elements in the 
     * array we process it as a query in curly brackets (e.g. "driver license"), 
     * it means we need results that match all the words in the query.
     * Both for single words and queries at first we try to get results that fully 
     * matches the search string (like equals()), if none we try to find results
     * that contain the search string from its beginning.  
     *
     * @param idx {@link Index} - contains indexed data.  
     * @param tokens Tokens to be found.
     * @return
     */
    private Map<String, HitBucket> makeInclusions(Index idx, String[] tokens) {
        
        String searchStr = tokens[0];
        for (int i = 1; i<tokens.length; i++) {
            searchStr += " " + tokens[i];
        }
        
        searchStr = searchStr.toLowerCase();

        Map<String, HitBucket> inclusions = new HashMap<String, HitBucket>();
        
        if(isTokenQuery(tokens)) {
            for (int i = 0; i < tokens.length; i++) {
                TokenBucket equalTokBuck = idx.findEqualsTokenBucket(tokens[i]);
                if (equalTokBuck == null) {
                    List<TokenBucket> containTokBucks = idx.findContainTokenBuckets(tokens[i]);
                    Map<String, HitBucket> containInclusions = new HashMap<String, HitBucket>();
                    for(TokenBucket containTokBuck : containTokBucks) {                            
                        containInclusions.putAll(include(containTokBuck,i,searchStr, inclusions));                        
                    }
                    inclusions = containInclusions; 
                    continue;
                } else {
                    inclusions = include(equalTokBuck, i, searchStr, inclusions);
                }                            
            }            
        } else {
            TokenBucket equalTokBuck = idx.findEqualsTokenBucket(searchStr);
            if (equalTokBuck == null) {
                List<TokenBucket> containTokBucks = idx.findContainTokenBuckets(searchStr);
                Map<String, HitBucket> containInclusions = new HashMap<String, HitBucket>();
                for(TokenBucket containTokBuck : containTokBucks) {
                    containInclusions = includeSingleToken(containTokBuck, searchStr);
                    inclusions.putAll(containInclusions);
                }
            } else {
                inclusions = includeSingleToken(equalTokBuck, searchStr);
            }                        
        }
        return inclusions;
    }
    
    /**
     * If string array contains more than one string, it means that the search request
     * was set as a query in curly brackets (e.g. "driver license").
     * And result of search must contain the whole this query.
     * @param tokens Tokens from search request.
     * @return
     */
    private boolean isTokenQuery(String[] tokens) {
        boolean result = false;
        if(tokens.length > 1) {
            result = true;
        }
        return result;        
    }
    
    /**
     * This method is called for every token in search request when it is a query. 
     * For the first element in the query we call {@link #includeSingleToken(TokenBucket, String, Map)} 
     * and get all results that match to this token. For every next token we call 
     * {@link #filterQuery(TokenBucket, Map)} and filter the existing map of results, excluding all
     * that don`t contain the current token. 
     * So we will get the results that fully match to our query.
     * 
     * @param tb {@link TokenBucket} that matches to the current token in array.
     * @param i The number of the current token in array.
     * @param searchStr The whole search request.
     * @param inclusions Map of search results.
     * @return
     */
    private Map<String, HitBucket> include(TokenBucket tb, int i, String searchStr, Map<String, HitBucket> inclusions) {        
        if (i == 0) {
            inclusions = includeSingleToken(tb, searchStr);
        } else {
            inclusions = filterQuery(tb, inclusions);
        }       
        return inclusions;        
    }
    
    /**
     * Filter the existing map of results, excluding all that don`t contain the current token. 
     * 
     * @param tb {@link TokenBucket} that matches to current token.
     * @param inclusions Map of search results that will be filtered. 
     * @return Map of filtered search results. 
     */
    private Map<String, HitBucket> filterQuery(TokenBucket tb, Map<String, HitBucket> inclusions) {
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
        return myInclusions;
    }
    
    /**
     * Collect the search results that match to the current token.
     * 
     * @param tb {@link TokenBucket} that matches to the string token.
     * @param searchStr String token.      
     * @return Map of search results that match to the current token.
     */
    private Map<String, HitBucket> includeSingleToken(TokenBucket tb, String searchStr) {
        Map<String, HitBucket> inclusions = new HashMap<String, HitBucket>();
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
     * Searches over elements from indexed data using parsed search request.
     *    
     * @param idx Indexed data to search for.
     * @return TreeSet<{@link HitBucket}> - result that matches to
     * the search request.  
     */
    public TreeSet<HitBucket> executeSearch(Index idx) {
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
