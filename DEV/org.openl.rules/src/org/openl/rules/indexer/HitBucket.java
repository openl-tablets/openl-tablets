package org.openl.rules.indexer;

/**
 * Handles the index element and number of matches of it during indexing. 
 *
 */
public class HitBucket implements Comparable<HitBucket> {
    
    /*
     * number of matches of element during the indexing
     */
    private double weight = 0;
    private IIndexElement element;

    public HitBucket(HitBucket hb) {
        this.element = hb.element;
        this.weight = hb.weight;
    }

    public HitBucket(IIndexElement element) {
        this.element = element;
    }

    public int compareTo(HitBucket hb) {
        if (weight > hb.weight) {
            return -1;
        } else {
            if (weight < hb.weight){
                return 1;
            }else{
                return element.getUri().compareTo(hb.getElement().getUri()); 
            }
        }
    }

    public IIndexElement getElement() {
        return element;
    }

    public double getWeight() {
        return weight;
    }

    public void increment() {
        weight += 1;
    }

    public void setElement(IIndexElement element) {
        this.element = element;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

}
