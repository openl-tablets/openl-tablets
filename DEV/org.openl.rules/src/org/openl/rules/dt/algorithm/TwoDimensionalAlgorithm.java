package org.openl.rules.dt.algorithm;

import java.util.ArrayList;
import java.util.Iterator;

import org.openl.domain.AIntIterator;
import org.openl.domain.IIntIterator;
import org.openl.vm.IRuntimeEnv;

public class TwoDimensionalAlgorithm implements IDecisionTableAlgorithm {

    private IDecisionTableAlgorithm va;
    private IDecisionTableAlgorithm ha;

    TwoDimensionalAlgorithm(IDecisionTableAlgorithm va, IDecisionTableAlgorithm ha) {
        super();
        this.va = va;
        this.ha = ha;
    }

    @Override
    public void cleanParamValuesForIndexedConditions() {
        va.cleanParamValuesForIndexedConditions();
        ha.cleanParamValuesForIndexedConditions();
    }

    @Override
    public IIntIterator checkedRules(Object target, Object[] params, IRuntimeEnv env) {
        IIntIterator iv = va.checkedRules(target, params, env);
        IIntIterator ih = ha.checkedRules(target, params, env);

        return ih.isResetable() ? new TwoDScaleIterator(iv, ih) : new TwoDScaleIteratorNotResetable(iv, ih);
    }

    class TwoDScaleIterator extends AIntIterator {
        IIntIterator iv;
        IIntIterator ih;
        int vValue = -1;

        TwoDScaleIterator(IIntIterator iv, IIntIterator ih) {
            this.iv = iv;
            this.ih = ih;
            nextV();
        }

        void nextV() {
            if (iv.hasNext()) {
                vValue = iv.next();
            } else {
                vValue = -1;
            }

        }

        @Override
        public int nextInt() {
            return vValue + nextH();
        }

        protected int nextH() {
            return ih.nextInt();
        }

        @Override
        public boolean hasNext() {
            while (vValue >= 0) {

                if (hasNextH()) {
                    return true;
                }

                resetH();
                nextV();
            }
            return false;
        }

        protected void resetH() {
            ih.reset();
        }

        protected boolean hasNextH() {
            return ih.hasNext();
        }

        @Override
        public boolean isResetable() {
            return false;
        }

        @Override
        public void reset() {
            throw new UnsupportedOperationException();
        }
    }

    class TwoDScaleIteratorNotResetable extends TwoDScaleIterator {

        ArrayList<Integer> storeIh = new ArrayList<>();
        Iterator<Integer> itH;

        TwoDScaleIteratorNotResetable(IIntIterator iv, IIntIterator ih) {
            super(iv, ih);
        }

        @Override
        protected int nextH() {
            if (itH != null) {
                return itH.next();
            }
            int i = ih.nextInt();
            storeIh.add(i);
            return i;
        }

        @Override
        protected void resetH() {
            itH = storeIh.iterator();
        }

        @Override
        protected boolean hasNextH() {
            return itH == null ? ih.hasNext() : itH.hasNext();
        }

    }

}
