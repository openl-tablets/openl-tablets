package org.openl.rules.indexer;

public class HitBucket implements Comparable {
    double weight = 0;

    IIndexElement element;

    public HitBucket(HitBucket hb) {
        element = hb.element;
        weight = hb.weight;
    }

    public HitBucket(IIndexElement element) {
        this.element = element;
    }

    public int compareTo(Object arg) {
        HitBucket hb = (HitBucket) arg;

        return weight == hb.weight ? element.getUri().compareTo(hb.getElement().getUri()) : (weight > hb.weight ? -1
                : 1);
    }

    public IIndexElement getElement() {
        return element;
    }

    public double getWeight() {
        return weight;
    }

    public void inc() {
        weight += 1;
    }

    public void setElement(IIndexElement element) {
        this.element = element;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

}
