package org.openl.rules.indexer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Handles indexed data by first char alphabet map.
 *
 */
public class Index {
    
    /**     
     * Handles the token and all elements that include it.
     *
     */
    public static class TokenBucket {
        static class TokenComparator implements Comparator<String> {

            public int compare(String s0, String s1) {
                return s0.length() == s1.length() ? s0.compareTo(s1) : s0.length() - s1.length();
            }

        }

        public static final TokenComparator TOKEN_COMPARATOR = new TokenComparator();
        
        /*
         * Base token is a root of the word.
         */
        private String baseToken;

        /*
         * Key: uri to element. value: element itself. 
         */
        private Map<String, HitBucket> indexElements = new HashMap<String, HitBucket>();        
        private Set<String> tokens = new TreeSet<String>();
        
        /*internal*/ TokenBucket(String token) {
            baseToken = token;
        }
        
        /**
         * Store the tokens in a set. Counts the number of iterates during indexing for given element. 
         * 
         * @param token Token to index.
         * @param element Element that contains given token.
         */
        public void addIndexElement(String token, IIndexElement element) {
            tokens.add(token);

            getHitBucket(element).increment();
        }
                
        public String getDisplayValue() {
            return tokens.iterator().next();
        }
        
        /**
         * Base token is a root of the word. See {@link Index#getRoot} .
         * @return base token.
         */
        public String getBaseToken() {
            return baseToken;
        }
        
        /**
         * Gets the {@link HitBucket} for given element. If none presents in map, creates new one and
         * put it to the map, where the key is an uri of element and value is its {@link HitBucket}. 
         * @param element index element
         * @return {@link HitBucket}
         */
        public synchronized HitBucket getHitBucket(IIndexElement element) {
            String uri = element.getUri();
            HitBucket hitBucket = indexElements.get(uri);
            if (hitBucket == null) {
                hitBucket = new HitBucket(element);
                indexElements.put(uri, hitBucket);
            }
            return hitBucket;

        }

        public Map<String, HitBucket> getIndexElements() {
            return indexElements;
        }

        public Set<String> getTokens() {
            return tokens;
        }

        public int size() {
            return indexElements.size();
        }

    }

    public static final String[] SUFFIXES = { "ies", "es", "s", "ied", "ed", "id", "y" };

    public static final String[][] EXCEPTIONS_ARRAY = { { "s", "was", "whereas", "us" }, { "d", "word" },
        { "es", "yes" }, { "id", "_id" } };
    
    /*
     * key: the upper letter of alphabet
     * value: map with tokens  
     */
    private Map<String, TreeMap<String, TokenBucket>> firstCharMap = new TreeMap<String, TreeMap<String, TokenBucket>>();

    /**
     * Get word without common suffixes. <br />
     * Processed suffixes: "ies", "es", "s", "ied", "ed", "id", "y" <br />
     * Turns all letters from upper case to lower. <br />
     * Algorithm: returns part of the word without common suffix or the whole word if the word doesn't 
     * contain common suffix.<br />
     *  
     * @param token
     * @return
     */
    public static String getRoot(String token) {
        String lc = token.toLowerCase();
        String result = lc;
        int len = token.length();
        for (int i = 0; i < SUFFIXES.length; i++) {
            if (len > SUFFIXES[i].length() && lc.endsWith(SUFFIXES[i])) {                
                result = lc.substring(0, len - SUFFIXES[i].length());                
            }
        }
        return result;
    }

    static boolean isException(String suffix, String lc) {
        boolean result = false;
        for (int i = 0; i < EXCEPTIONS_ARRAY.length; i++) {
            if (EXCEPTIONS_ARRAY[i][0].equals(suffix)) {
                for (int j = 1; j < EXCEPTIONS_ARRAY[i].length; j++) {
                    if (lc.endsWith(EXCEPTIONS_ARRAY[i][j])) {
                        result = true;
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Add token to an index element.
     * @param token
     * @param element
     */
    public void add(String token, IIndexElement element) {

        TokenBucket tb = findOrCreateTokenBucket(token);

        tb.addIndexElement(token, element);

    }
        
    public TokenBucket findOrCreateTokenBucket(String token) {
        String charStr = token.substring(0, 1).toUpperCase();

        Map<String, TokenBucket> charMap = getFirstCharMap(charStr);

        String tokenRoot = getRoot(token);
        TokenBucket tb = getTokenBucket(charMap, tokenRoot);
        return tb;
    }
    
    /**
     * Searches by strict compliance for token bucket in the first chat map by the token.      
     * @param token
     * @return
     */
    public TokenBucket findEqualsTokenBucket(String token) {
        TokenBucket tokenBucket = null;
        String charStr = token.substring(0, 1).toUpperCase();

        Map<String, TokenBucket> charMap = firstCharMap.get(charStr);

        if (charMap != null) {
            String tokenRoot = getRoot(token);
            tokenBucket = charMap.get(tokenRoot);            
        }        
        return tokenBucket;
    }
    
    /**
     * Searches all {@link TokenBucket} that matches to the token,
     * by contains for token bucket in the first chat map by the token.      
     * @param token
     * @return
     */
    public List<TokenBucket> findContainTokenBuckets(String token) {
        List<TokenBucket> tokBucks = new ArrayList<TokenBucket>();
        String charStr = token.substring(0, 1).toUpperCase();

        Map<String, TokenBucket> charMap = firstCharMap.get(charStr);

        if (charMap != null) {
            String tokenRoot = getRoot(token);
            for(Iterator<String> iter = charMap.keySet().iterator(); iter.hasNext();) {
                String keyValue = iter.next();
                TokenBucket tokenBucket = null;
                if(keyValue.contains(tokenRoot)) {
                    tokenBucket = charMap.get(keyValue);
                    tokBucks.add(tokenBucket);
                }
            }
        }
        return tokBucks;
    }

    public Map<String, TreeMap<String, TokenBucket>> getFirstCharMap() {
        return firstCharMap;
    }
    
    protected synchronized Map<String, TokenBucket> getFirstCharMap(String charStr) {
        TreeMap<String, TokenBucket> map = firstCharMap.get(charStr);
        if (map == null) {
            map = new TreeMap<String, TokenBucket>();
            firstCharMap.put(charStr, map);
        }
        return map;
    }

    protected synchronized TokenBucket getTokenBucket(Map<String, TokenBucket> tokenMap, String token) {
        TokenBucket tb = tokenMap.get(token);
        if (tb == null) {
            tb = new TokenBucket(token);
            tokenMap.put(token, tb);
        }
        return tb;
    }
    
}
