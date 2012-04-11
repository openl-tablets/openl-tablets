package org.openl.rules.indexer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.openl.rules.indexer.Index.TokenBucket;

public class IndexQuery {
    static final int ELEMENT_PRESENCE_WEIGHT = 20;
    static final int CONTAINS_STR_WEIGHT = 100;

    String[][] tokensInclude = null;
    String[][] tokensExclude = {};

    IIndexElement[] indexExclude = {};

    HashSet excludedIndexes;

    static public Map intersect(Map m1, Map m2) {
        // TreeMap tm = new TreeMap(exclusions2.comparator());

        Map tm = new HashMap();

        for (Iterator iter = m2.entrySet().iterator(); iter.hasNext();) {
            Map.Entry element = (Map.Entry) iter.next();
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

    public TreeMap execute(Index idx) {
        makeExclusions(idx);
        Map allInc = makeInclusions(idx);

        TreeMap tm = new TreeMap();

        for (Iterator iter = allInc.values().iterator(); iter.hasNext();) {
            HitBucket hb = (HitBucket) iter.next();
            tm.put(hb, hb);
        }
        return tm;
    }

    public String[][] getTokensInclude() {
        return tokensInclude;
    }

    void makeExclusions(Index idx) {

        excludedIndexes = new HashSet(indexExclude.length);
        for (int i = 0; i < indexExclude.length; i++) {
            excludedIndexes.add(indexExclude[i]);
        }

        for (int i = 0; i < tokensExclude.length; i++) {

            Map exclusions = null;
            for (int j = 0; j < tokensExclude[i].length; j++) {
                String tokenExclude = tokensExclude[i][j];
                TokenBucket tb = idx.findTokenBucket(tokenExclude);
                if (j == 0) {
                    exclusions = tb.getIndexElements();
                } else {
                    exclusions = intersect(exclusions, tb.getIndexElements());
                }
            }

            for (Iterator iter = exclusions.values().iterator(); iter.hasNext();) {
                HitBucket hb = (HitBucket) iter.next();
                excludedIndexes.add(hb.getElement());
            }
        }

    }

    Map makeInclusions(Index idx) {
        Map allInclusions = new HashMap();

        for (int i = 0; i < tokensInclude.length; i++) {
            Map inclusions = makeInclusions(idx, tokensInclude[i]);

            for (Iterator iter = inclusions.values().iterator(); iter.hasNext();) {
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

    // Var 1 - all have to be there
    Map makeInclusions(Index idx, String[] tokens) {

        String searchStr = tokens[0];
        for (int i = 1; i < tokens.length; i++) {
            searchStr += " " + tokens[i];
        }

        searchStr = searchStr.toLowerCase();

        Map inclusions = new HashMap();

        for (int i = 0; i < tokens.length; i++) {
            TokenBucket tb = idx.findTokenBucket(tokens[i]);
            if (tb == null) {
                continue;
            }
            if (i == 0) {
                for (Iterator iter = tb.getIndexElements().values().iterator(); iter.hasNext();) {
                    HitBucket hb = (HitBucket) iter.next();
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
                Map myInclusions = new HashMap();
                for (Iterator iter = tb.getIndexElements().values().iterator(); iter.hasNext();) {

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

        }

        return inclusions;
    }

    public void setIndexExclude(IIndexElement[] indexExclude) {
        this.indexExclude = indexExclude;
    }

    public void setTokensExclude(String[][] tokensExclude) {
        this.tokensExclude = tokensExclude;
    }

}
