/**
 * Created Jul 26, 2007
 */
package org.openl.rules.ui.benchmark;

/**
 * @author snshor
 * 
 */
public class BenchmarkOrder implements Comparable<BenchmarkOrder> {

    int index;
    BenchmarkInfo info;
    int order;
    double ratio;

    /**
     * @param i
     * @param info
     */
    public BenchmarkOrder(int i, BenchmarkInfo info) {
        index = i;
        this.info = info;
    }

    @Override
    public int compareTo(BenchmarkOrder arg0) {
        double x = info.drunsunitsec() - arg0.info.drunsunitsec();
        return x > 0 ? -1 : x == 0 ? 0 : 1;
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 instanceof BenchmarkOrder) {
            BenchmarkOrder bo = (BenchmarkOrder) arg0;
            return bo.info.drunsunitsec() == info.drunsunitsec();
        }
        return false;
    }

    public int getIndex() {
        return index;
    }

    public BenchmarkInfo getInfo() {
        return info;
    }

    public int getOrder() {
        return order;
    }

    public double getRatio() {
        return ratio;
    }

    @Override
    public int hashCode() {
        return (int) info.drunsunitsec();
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setRatio(double ratio) {
        this.ratio = ratio;
    }

}
