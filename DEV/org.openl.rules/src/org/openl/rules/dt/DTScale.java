package org.openl.rules.dt;

public class DTScale {

    private int vScale, hScale;

    public DTScale(int vScale, int hScale) {
        super();
        this.vScale = vScale;
        this.hScale = hScale;
    }

    public interface RowScale {
        int getMultiplier();

        int getActualSize(int size);

        int getActualIndex(int logicalIndex);

        int getLogicalIndex(int actualIndex);
    }

    public RowScale getVScale() {
        return vScale == 0 ? STANDARD_SCALE : new VScale();
    }

    public RowScale getHScale() {
        return hScale == 0 ? STANDARD_SCALE : new HScale();
    }

    static RowScale getStandardScale() {
        return STANDARD_SCALE;
    }

    /**
     * Use this scale for standard decision tables and RET lookups
     */

    private static final RowScale STANDARD_SCALE = new RowScale() {

        @Override
        public int getActualSize(int size) {
            return size;
        }

        @Override
        public int getActualIndex(int logicalIndex) {
            return logicalIndex;
        }

        @Override
        public int getLogicalIndex(int actualIndex) {
            return actualIndex;
        }

        @Override
        public int getMultiplier() {
            return 1;
        }
    };

    static final DTScale STANDARD = new DTScale(0, 0);

    /**
     *
     * Use this scale for vertical conditions
     *
     */

    class VScale implements RowScale {

        @Override
        public int getActualSize(int size) {
            assert (size == vScale * hScale);
            return vScale;
        }

        @Override
        public int getActualIndex(int logicalIndex) {
            return logicalIndex % vScale;
        }

        @Override
        public int getLogicalIndex(int actualIndex) {
            return actualIndex;
        }

        @Override
        public int getMultiplier() {
            return hScale;
        }
    }

    /**
     *
     * Use this scale for horizontal conditions
     *
     */

    class HScale implements RowScale {

        @Override
        public int getActualSize(int size) {
            assert (size == vScale * hScale);
            return hScale;
        }

        @Override
        public int getActualIndex(int logicalIndex) {
            return logicalIndex / vScale;
        }

        @Override
        public int getLogicalIndex(int actualIndex) {
            return actualIndex * vScale;
        }

        @Override
        public int getMultiplier() {
            return vScale;
        }
    }

}
