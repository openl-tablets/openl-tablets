package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.*;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;
import org.openl.ie.tools.ReusableImpl;

public class IntExpArrayElement1 extends IntExpImpl {
    static class AdvancedMapping implements Mapping {
        java.util.HashMap valueToArrayIdx = new java.util.HashMap();
        IntExp _index;
        IntExp _element;
        IntExpArray _ary;
        IntExpArray _valuesUsed; // How many times a particular value could
        // be encountered within _ary[_index]

        IntExpArray copyOfAry;
        IntExp _copyOfIndex;
        AryElementsObserver[] _observers;

        static IntExpArray createCopyOfIntExpArray(IntExpArray array) {
            IntExpArray arrCopy = new IntExpArray(array.constrainer(), array.size());
            for (int i = 0; i < array.size(); i++) {
                IntVar varCopy;
                IntExp exp = array.get(i);
                if (exp instanceof IntVar) {
                    varCopy = createCopyOfIntVar((IntVar) exp);
                } else {
                    varCopy = array.constrainer().addIntVar(exp.min(), exp.max(), IntVar.DOMAIN_PLAIN);
                }
                arrCopy.set(varCopy, i);
            }
            return arrCopy;
        }

        static IntVar createCopyOfIntVar(IntVar var) {
            int type = var.domainType();
            boolean remove = false;
            switch (type) {
                case IntVar.DOMAIN_BIT_FAST:
                case IntVar.DOMAIN_BIT_SMALL:
                    remove = true;
                case IntVar.DOMAIN_PLAIN:
                default:
                    break;
            }
            IntVar copy = var.constrainer().addIntVar(var.min(), var.max(), type);
            if (remove) {
                for (int i = var.min(); i <= var.max(); i++) {
                    if (!var.contains(i)) {
                        try {
                            copy.removeValue(i);
                        } catch (Failure f) {
                        }
                    }
                }
            }
            return copy;
        }

        public AdvancedMapping(IntExp index, IntExp element, IntExpArray ary, AryElementsObserver[] observers) {
            _observers = observers;
            _index = index;
            _element = element;
            _ary = ary;

            createCountersArray();
            _copyOfIndex = createCopyOfIntVar((IntVar) _index);
            copyOfAry = createCopyOfIntExpArray(_ary);
        }

        @Override
        public void arrayElementMax(int oldmax, int max, int idx) throws Failure {
            if (max < _element.min()) {
                _index.removeValue(idx);
            } else {
                decreaseUsageCounter(max + 1, oldmax, idx);
            }
            IntVar varCopy = (IntVar) copyOfAry.get(idx);
            varCopy.setMax(max);
        }

        @Override
        public void arrayElementMin(int oldmin, int min, int idx) throws Failure {
            if (min > _element.max()) {
                _index.removeValue(idx);
            } else {
                decreaseUsageCounter(oldmin, min - 1, idx);
            }
            IntVar varCopy = (IntVar) copyOfAry.get(idx);
            varCopy.setMin(min);
        }

        @Override
        public void arrayElementRemove(int removedValue, int idx) throws Failure {
            decreaseUsageCounter(removedValue);
            IntVar varCopy = (IntVar) copyOfAry.get(idx);
            varCopy.removeValue(removedValue);
        }

        @Override
        public void arrayElementValue(int value, int idx) throws Failure {
            IntVar varCopy = (IntVar) copyOfAry.get(idx);
            if (value < _element.min() || value > _element.max()) {
                _index.removeValue(idx);
            } else {
                for (int i = varCopy.min(); i < value; i++) {
                    if (varCopy.contains(i)) {
                        decreaseUsageCounter(i);
                    }
                }
                for (int i = varCopy.max(); i > value; i--) {
                    if (varCopy.contains(i)) {
                        decreaseUsageCounter(i);
                    }
                }
            }
            varCopy.setValue(value);
        }

        void createCountersArray() {
            int cnt = 0;
            int[] usage = new int[_element.size()];
            for (int i = _element.min(); i <= _element.max(); i++) {
                if (_element.contains(i)) {
                    ValueCounterIterator iter = new ValueCounterIterator(_ary, i);
                    try {
                        _index.iterateDomain(iter);
                    } catch (Failure f) {
                    }
                    valueToArrayIdx.put(new Integer(i), new Integer(cnt));
                    usage[cnt] = iter.cnt;
                    cnt++;
                }
            }

            _valuesUsed = new IntExpArray(_index.constrainer(), cnt);
            for (int i = 0; i < cnt; i++) {
                _valuesUsed.set(_index.constrainer().addIntVar(0, usage[i]), i);
            }
        }

        void decreaseUsageCounter(int val) throws Failure {
            int idx = ((Integer) valueToArrayIdx.get(new Integer(val)));
            int oldMax = _valuesUsed.get(idx).max();
            if (oldMax == 1) {
                _element.removeValue(val);
                _valuesUsed.get(idx).setMax(0);
            } else {
                _valuesUsed.get(idx).setMax(oldMax - 1);
            }
        }

        void decreaseUsageCounter(int start, int end, int idx) throws Failure {
            IntVar var = (IntVar) copyOfAry.get(idx);
            for (int i = start; i <= end; i++) {
                if (var.contains(i)) {
                    decreaseUsageCounter(i);
                }
            }
        }

        void detachObservers(int start, int end) {
            for (int i = start; i <= end; i++) {
                if (_copyOfIndex.contains(i)) {
                    _ary.get(i).detachObserver(_observers[i]);
                }
            }
        }

        @Override
        public void indexMax(int oldmax, int max) throws Failure {
            FindMinMaxIterator iter = FindMinMaxIterator.getIterator(_index, _ary);
            _index.iterateDomain(iter);
            if (iter.max < _element.max() && iter.min > _element.min()) {
                _element.setMax(iter.max);
                _element.setMin(iter.min);
            } else {
                for (int i = oldmax; i > max; i--) {
                    if (_copyOfIndex.contains(i)) {
                        decreaseUsageCounter(copyOfAry.get(i).min(), copyOfAry.get(i).max(), i);
                    }
                }
            }

            detachObservers(max + 1, oldmax);
            _copyOfIndex.setMax(max);
        }/**/

        @Override
        public void indexMin(int oldmin, int min) throws Failure {
            FindMinMaxIterator iter = FindMinMaxIterator.getIterator(_index, _ary);
            _index.iterateDomain(iter);
            if (iter.max < _element.max() || iter.min > _element.min()) {
                _element.setMax(iter.max);
                _element.setMin(iter.min);
            } else {
                for (int i = oldmin; i < min; i++) {
                    if (_copyOfIndex.contains(i)) {
                        decreaseUsageCounter(copyOfAry.get(i).min(), copyOfAry.get(i).max(), i);
                    }
                }
            }
            updateResultDomainFromIndex();
            detachObservers(oldmin, min - 1);
            _copyOfIndex.setMin(min);
        }/**/

        @Override
        public void indexRemove(int removedValue) throws Failure {
            if (_copyOfIndex.contains(removedValue)) {
                decreaseUsageCounter(copyOfAry.get(removedValue).min(),
                    copyOfAry.get(removedValue).max(),
                    removedValue);
            }
            _ary.get(removedValue).detachObserver(_observers[removedValue]);
            _copyOfIndex.removeValue(removedValue);
        }/**/

        /**/
        @Override
        public void indexValue(int value) throws Failure {

            _element.setMin(_ary.get(value).min());
            _element.setMax(_ary.get(value).max());

            for (int i = 0; i < _ary.size(); i++) {
                if (_copyOfIndex.contains(i) && i != value) {
                    _ary.get(i).detachObserver(_observers[i]);
                }
            }

            _copyOfIndex.setValue(value);
        }

        @Override
        public void resultMax(int max) throws Failure {
            RemoveFromElementMaxIterator it = RemoveFromElementMaxIterator.getIterator(_index, _ary, max);
            _index.iterateDomain(it);
            it.free();
        }

        @Override
        public void resultMin(int min) throws Failure {
            RemoveFromElementMinIterator it = RemoveFromElementMinIterator.getIterator(_index, _ary, min);
            _index.iterateDomain(it);
            it.free();
        }

        @Override
        public void resultRemove(int value) throws Failure {

        }

        @Override
        public void resultValue(int value) throws Failure {
            SetValueFromElementIterator it = SetValueFromElementIterator.getIterator(_index, _ary, value);
            _index.iterateDomain(it);
            it.free();
        }

        void updateResultDomainFromIndex() throws Failure {
            FindMinMaxIterator it = FindMinMaxIterator.getIterator(_index, _ary);
            _index.iterateDomain(it);
            _element.setMin(it.min);
            _element.setMax(it.max);
            it.free();
        }

    }// ~AdvancedMapping

    static class AdvancedMapping2 extends AdvancedMapping {
        public AdvancedMapping2(IntExp index, IntExp element, IntExpArray ary, AryElementsObserver[] observers) {
            super(index, element, ary, observers);
        }

        /*
         * public void indexValue(int value) throws Failure{ for (int i=0; i<this._ary.size(); i++){ if ( (_observers[i]
         * != null) && (i!=value)) this._ary.get(value).detachObserver(_observers[i]); }
         * this._element.setMin(this._ary.get(value).min()); this._element.setMax(this._ary.get(value).max()); }
         */

        @Override
        public void indexMax(int oldmax, int max) throws Failure {
            updateResultDomainFromIndex();
            detachObservers(max + 1, oldmax);
            _copyOfIndex.setMax(max);
        }

        @Override
        public void indexMin(int oldmin, int min) throws Failure {
            updateResultDomainFromIndex();
            detachObservers(oldmin, min - 1);
            _copyOfIndex.setMin(min);
        }

        @Override
        public void indexRemove(int removedValue) throws Failure {
            updateResultDomainFromIndex();
            detachObservers(removedValue, removedValue);
            _copyOfIndex.removeValue(removedValue);
        }
    } // ~AdvancedMapping2

    class AryElementsObserver extends Observer {
        private int idx;

        public AryElementsObserver(int id) {
            idx = id;
        }

        @Override
        public Object master() {
            return IntExpArrayElement1.this;
        }

        @Override
        public int subscriberMask() {
            return IntEvent.ALL;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            IntEvent e = (IntEvent) event;
            int type = e.type();

            if ((type & IntEvent.VALUE) != 0) {
                int value = e.min();
                _m.arrayElementValue(value, idx);
                /*
                 * if ( (value > _element.max()) || (value < _element.min()) ){ _index.removeValue(idx); }
                 */
            } else {
                if ((type & IntEvent.MIN) != 0) {
                    /*
                     * if (_element.max() < e.min()) _index.removeValue(idx);
                     */
                    _m.arrayElementMin(e.oldmin(), e.min(), idx);
                }
                if ((type & IntEvent.MAX) != 0) {
                    /*
                     * if (_element.min() > e.max()) _index.removeValue(idx);
                     */
                    _m.arrayElementMax(e.oldmax(), e.max(), idx);
                }
                if ((type & IntEvent.REMOVE) != 0) {
                    int nRemoves = e.numberOfRemoves();
                    int min = e.min();
                    int max = e.max();
                    for (int i = 0; i < nRemoves; ++i) {
                        int removedValue = e.removed(i);
                        if (min <= removedValue && removedValue <= max) {
                            _m.arrayElementRemove(removedValue, idx);
                        }
                    }
                }
            }
        } // ~update()
    } // ~ AryElementsObserver

    /**
     * make an exctract from IntExpArray based on _index
     */
    static class CopyElementsIterator extends ReusableImpl implements IntExp.IntDomainIterator {
        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new CopyElementsIterator();
            }
        };

        IntExp _index;

        IntExpArray source;
        IntExpArray extract;
        int cnt = 0;

        static CopyElementsIterator getIterator(IntExp index, IntExpArray ary) {
            CopyElementsIterator iter = new CopyElementsIterator();
            iter._index = index;
            iter.source = ary;
            iter.extract = new IntExpArray(index.constrainer(), index.size());
            return iter;
        }

        @Override
        public boolean doSomethingOrStop(int idx) throws Failure {
            extract.set(source.get(idx), cnt++);
            return true;
        }
    }

    class ElementObserver extends Observer {
        @Override
        public Object master() {
            return IntExpArrayElement1.this;
        }

        @Override
        public int subscriberMask() {
            return IntEvent.ALL;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            IntEvent e = (IntEvent) event;
            int type = e.type();

            if ((type & IntEvent.VALUE) != 0) {
                _m.resultValue(e.min());
            } else {
                if ((type & IntEvent.MIN) != 0) {
                    _m.resultMin(e.min());
                }
                if ((type & IntEvent.MAX) != 0) {
                    _m.resultMax(e.max());
                }
                if ((type & IntEvent.REMOVE) != 0) {
                    int nRemoves = e.numberOfRemoves();
                    int min = e.min();
                    int max = e.max();
                    for (int i = 0; i < nRemoves; ++i) {
                        int removedValue = e.removed(i);
                        if (min <= removedValue && removedValue <= max) {
                            _m.resultRemove(removedValue);
                        }
                    }
                }
            }
        } // ~update()

    } // ~ ElementObserver

    /**
     * finds min(_ary[idx]) and max(_ary[idx]).
     */
    static class FindMinMaxIterator extends ReusableImpl implements IntExp.IntDomainIterator {
        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new FindMinMaxIterator();
            }
        };

        IntExpArray ary;
        int min = Integer.MAX_VALUE;
        int max = -Integer.MAX_VALUE;
        IntExp index;

        static FindMinMaxIterator getIterator(IntExp index, IntExpArray ary) {
            FindMinMaxIterator it = (FindMinMaxIterator) _factory.getElement();
            it.index = index;
            it.ary = ary;
            return it;
        }

        @Override
        public boolean doSomethingOrStop(int idx) throws Failure {
            if (ary.elementAt(idx).max() > max) {
                max = ary.elementAt(idx).max();
            }
            if (ary.elementAt(idx).min() < min) {
                min = ary.elementAt(idx).min();
            }
            return true;
        }
    }

    /*
     * ============================================================================== Functional iterators
     * ============================================================================
     */
    /**
     * Finds idx from the _index where _ary[idx] == value.
     */
    static class FindValueIterator extends ReusableImpl implements IntExp.IntDomainIterator {
        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new FindValueIterator();
            }
        };

        IntExp index;

        IntExpArray ary;
        int value;
        int foundIndex;

        static FindValueIterator getIterator(IntExp index, IntExpArray ary, int value) {
            FindValueIterator it = (FindValueIterator) _factory.getElement();
            it.index = index;
            it.ary = ary;
            it.value = value;
            it.foundIndex = -1;
            return it;
        }

        @Override
        public boolean doSomethingOrStop(int idx) throws Failure {
            if (ary.elementAt(idx).contains(value)) {
                foundIndex = idx;
                return false;
            }

            return true;
        }

    } // ~FindValueIterator

    class IndexObserver extends Observer {
        @Override
        public Object master() {
            return IntExpArrayElement1.this;
        }

        @Override
        public int subscriberMask() {
            return IntEvent.ALL;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            IntEvent e = (IntEvent) event;
            int type = e.type();

            if ((type & IntEvent.VALUE) != 0) {
                _m.indexValue(e.min());
            } else {
                if ((type & IntEvent.MIN) != 0) {
                    _m.indexMin(e.oldmin(), e.min());
                }
                if ((type & IntEvent.MAX) != 0) {
                    _m.indexMax(e.oldmax(), e.max());
                }
                if ((type & IntEvent.REMOVE) != 0) {
                    int nRemoves = e.numberOfRemoves();
                    int min = e.min();
                    int max = e.max();
                    for (int i = 0; i < nRemoves; ++i) {
                        int removedValue = e.removed(i);
                        if (min <= removedValue && removedValue <= max) {
                            _m.indexRemove(removedValue);
                        }
                    }
                }
            }
        } // ~update()

    } // ~ IndexObserver

    /**
     * A mapping for removing values from the index and element.
     */
    interface Mapping {
        void arrayElementMax(int oldmax, int max, int idx) throws Failure;

        void arrayElementMin(int oldmin, int min, int idx) throws Failure;

        void arrayElementRemove(int removedValue, int idx) throws Failure;

        void arrayElementValue(int value, int idx) throws Failure;

        void indexMax(int oldmax, int max) throws Failure;

        void indexMin(int oldmin, int min) throws Failure;

        void indexRemove(int removedValue) throws Failure;

        void indexValue(int value) throws Failure;

        void resultMax(int max) throws Failure;

        void resultMin(int min) throws Failure;

        void resultRemove(int removedValue) throws Failure;

        void resultValue(int value) throws Failure;
    }

    /**
     * Remove all indexes from the _index where max < min(_ary[idx]).
     */
    static class RemoveFromElementMaxIterator extends ReusableImpl implements IntExp.IntDomainIterator {
        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new RemoveFromElementMaxIterator();
            }
        };

        IntExp index;

        IntExpArray ary;
        int max;

        static RemoveFromElementMaxIterator getIterator(IntExp index, IntExpArray ary, int max) {
            RemoveFromElementMaxIterator it = (RemoveFromElementMaxIterator) _factory.getElement();
            it.index = index;
            it.ary = ary;
            it.max = max;
            return it;
        }

        @Override
        public boolean doSomethingOrStop(int idx) throws Failure {
            int arrayElementMin = ary.elementAt(idx).min();
            if (max < arrayElementMin) {
                index.removeValue(idx);
            }
            return true;
        }
    } // ~RemoveFromElementMaxIterator

    /**
     * Remove all indexes from the _index where min > max(_ary[idx]).
     */
    static class RemoveFromElementMinIterator extends ReusableImpl implements IntExp.IntDomainIterator {
        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new RemoveFromElementMinIterator();
            }
        };

        IntExp index;

        IntExpArray ary;
        int min;

        static RemoveFromElementMinIterator getIterator(IntExp index, IntExpArray ary, int min) {
            RemoveFromElementMinIterator it = (RemoveFromElementMinIterator) _factory.getElement();
            it.index = index;
            it.ary = ary;
            it.min = min;
            return it;
        }

        @Override
        public boolean doSomethingOrStop(int idx) throws Failure {
            int arrayElementMax = ary.elementAt(idx).max();
            if (min > arrayElementMax) {
                index.removeValue(idx);
            }
            return true;
        }
    } // ~RemoveFromElementMinIterator

    /**
     * Remove all indexes from the _index where _ary[idx] != value.
     */
    static class SetValueFromElementIterator extends ReusableImpl implements IntExp.IntDomainIterator {
        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new SetValueFromElementIterator();
            }
        };

        IntExp index;

        IntExpArray ary;
        int value;

        static SetValueFromElementIterator getIterator(IntExp index, IntExpArray ary, int value) {
            SetValueFromElementIterator it = (SetValueFromElementIterator) _factory.getElement();
            it.index = index;
            it.ary = ary;
            it.value = value;
            return it;
        }

        @Override
        public boolean doSomethingOrStop(int idx) throws Failure {
            if (!ary.elementAt(idx).contains(value)) {
                index.removeValue(idx);
            }
            return true;
        }

    } // ~SetValueFromElementIterator

    /**
     * An Mapping that scan index for a value.
     */
    static class SimpleMapping implements Mapping {
        IntExp _index;
        IntExp _element;
        IntExpArray _ary;
        AryElementsObserver[] _observers;

        public SimpleMapping(IntExp index, IntExp element, IntExpArray ary, AryElementsObserver[] observers) {
            _index = index;
            _element = element;
            _ary = ary;
            _observers = observers;
        }

        @Override
        public void arrayElementMax(int oldmax, int max, int idx) throws Failure {
            if (_element.min() > max) {
                _index.removeValue(idx);
            }
        }

        @Override
        public void arrayElementMin(int oldmin, int min, int idx) throws Failure {
            if (_element.max() < min) {
                _index.removeValue(idx);
            }
        }

        @Override
        public void arrayElementRemove(int value, int idx) throws Failure {

        }

        @Override
        public void arrayElementValue(int value, int idx) throws Failure {
            if (value > _element.max() || value < _element.min()) {
                _index.removeValue(idx);
            }
        }

        @Override
        public void indexMax(int oldmax, int max) throws Failure {
            for (int i = oldmax; i > max; i--) {
                if (_observers[i] != null) {
                    _ary.get(i).detachObserver(_observers[i]);
                }
            }
            updateResultDomainFromIndex();
        }

        @Override
        public void indexMin(int oldmin, int min) throws Failure {
            for (int i = oldmin; i < min; i++) {
                if (_observers[i] != null) {
                    _ary.get(i).detachObserver(_observers[i]);
                }
            }
            updateResultDomainFromIndex();
        }

        @Override
        public void indexRemove(int value) throws Failure {
            _ary.get(value).detachObserver(_observers[value]);
        }

        @Override
        public void indexValue(int value) throws Failure {
            for (int i = 0; i < _ary.size(); i++) {
                if (_observers[i] != null && i != value) {
                    _ary.get(value).detachObserver(_observers[i]);
                }
            }
            _element.setMin(_ary.get(value).min());
            _element.setMax(_ary.get(value).max());
        }

        @Override
        public void resultMax(int max) throws Failure {
            RemoveFromElementMaxIterator it = RemoveFromElementMaxIterator.getIterator(_index, _ary, max);
            _index.iterateDomain(it);
            it.free();
        }

        @Override
        public void resultMin(int min) throws Failure {
            RemoveFromElementMinIterator it = RemoveFromElementMinIterator.getIterator(_index, _ary, min);
            _index.iterateDomain(it);
            it.free();
        }

        @Override
        public void resultRemove(int value) throws Failure {

        }

        @Override
        public void resultValue(int value) throws Failure {
            SetValueFromElementIterator it = SetValueFromElementIterator.getIterator(_index, _ary, value);
            _index.iterateDomain(it);
            it.free();
        }

        public void updateResultDomainFromIndex() throws Failure {
            FindMinMaxIterator it = FindMinMaxIterator.getIterator(_index, _ary);
            _index.iterateDomain(it);
            _element.setMin(it.min);
            _element.setMax(it.max);
            it.free();
        }

    } // ~SimpleMapping

    // ~FindMinMaxIterator

    /*
     * ============================================================================== EOF Functional iterators
     * ============================================================================
     */

    private static class ValueCounterIterator implements IntExp.IntDomainIterator {
        IntExpArray _ary;
        int _val;
        int cnt = 0;

        public ValueCounterIterator(IntExpArray array, int value) {
            _ary = array;
            _val = value;
        }

        @Override
        public boolean doSomethingOrStop(int idx) throws Failure {
            if (_ary.get(idx).contains(_val)) {
                cnt++;
            }
            return true;
        }
    }

    private IntExpArray _ary;

    private AryElementsObserver[] _aryElementsObservers = null;

    private IntExp _indexExp;

    private IntVar _index;

    private IntExp _element;

    private Mapping _m;

    static int findIndex(IntExp index, IntExpArray ary, int value) {
        FindValueIterator it = FindValueIterator.getIterator(index, ary, value);
        try {
            index.iterateDomain(it);
        } catch (Failure f) {
        }

        int foundIndex = it.foundIndex;

        it.free();

        return foundIndex;
    }

    static boolean indexHasValue(IntExp index, IntExpArray ary, int value) {
        return findIndex(index, ary, value) >= 0;
    }

    static IntExpArray makeExtraction(IntExp index, IntExpArray ary) {
        CopyElementsIterator iter = CopyElementsIterator.getIterator(index, ary);
        try {
            index.iterateDomain(iter);
        } catch (Failure f) {
        }
        // iter.free();
        return iter.extract;
    }

    public IntExpArrayElement1(IntExpArray ary, IntExp indexExp) {
        super(ary.constrainer());
        _ary = ary;
        _indexExp = indexExp;

        _aryElementsObservers = new AryElementsObserver[ary.size()];

        if (constrainer().showInternalNames()) {
            _name = "" + _ary.name() + "[" + _indexExp.name() + "]";
        }

        try {
            createIndex();
            createElement();
            // Propagate events BEFORE attaching the observers.
            constrainer().propagate();
        } catch (Exception e) {
            throw new RuntimeException("Invalid elementAt-expression: " + ary + "[" + indexExp + "]. " + e.getClass()
                .getName() + ": " + e.getMessage());
        }

        createMapping();

        _index.attachObserver(new IndexObserver());
        _element.attachObserver(new ElementObserver());

        for (int i = 0; i < _ary.size(); i++) {
            if (_index.contains(i)) {
                _aryElementsObservers[i] = new AryElementsObserver(i);
                _ary.get(i).attachObserver(_aryElementsObservers[i]);
            }
        }
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _element.attachObserver(observer);
    }

    @Override
    public boolean contains(int value) {
        return _element.contains(value);
    }

    void createElement() throws Failure {
        int[] values = elementDomain();

        int min = values[0];
        int max = values[values.length - 1];
        int size = values.length;
        int nHoles = max - min + 1 - size;

        if (nHoles < 10) {
            createElement1(values);
        } else {
            createElement2(values);
        }
    }

    void createElement1(int[] values) throws Failure {
        // int trace = IntVar.TRACE_ALL;
        int trace = 0;
        int min = values[0];
        int max = values[values.length - 1];

        String name = "";
        if (constrainer().showInternalNames()) {
            name = "element_" + _ary.name() + "[" + _indexExp.name() + "]";
        }

        _element = constrainer().addIntVarTraceInternal(min, max, name, IntVar.DOMAIN_BIT_FAST, trace);

        // Remove NOT-index values from _element
        for (int i = 0; i + 1 < values.length; ++i) {
            for (int value = values[i] + 1; value < values[i + 1] - 1; ++value) {
                _element.removeValue(value);
            }
        }

    }

    void createElement2(int[] values) throws Failure {
        String name = "";
        if (constrainer().showInternalNames()) {
            name = "element_" + _ary.name() + "[" + _indexExp.name() + "]";
        }

        _element = new IntExpEnum(constrainer(), values, name);
    }

    void createIndex() throws Failure {
        boolean effectiveIndexExp = _indexExp instanceof IntVar && ((IntVar) _indexExp)
            .domainType() != IntVar.DOMAIN_PLAIN;
        int max = _ary.size() - 1;
        if (effectiveIndexExp) {
            // Use _indexExp as _index.
            _index = (IntVar) _indexExp;
            _index.setMin(0);
            _index.setMax(max);
        } else {
            // Create _index as a new effective index and post constraint
            // (_index == _indexExp).
            // int trace = IntVar.TRACE_ALL;
            int trace = 0;
            String name = "";
            if (constrainer().showInternalNames()) {
                name = "index_" + _ary.name() + "[" + _indexExp.name() + "]";
            }
            _index = constrainer().addIntVarTraceInternal(0, max, name, IntVar.DOMAIN_BIT_FAST, trace);

            // Sync _index and _indexExp
            for (int i = 0; i <= max; ++i) {
                if (!_indexExp.contains(i)) {
                    _index.removeValue(i);
                }
            }

            _index.equals(_indexExp).post();
        }
    }

    void createMapping() {
        // _m = new SimpleMapping(_index, _element, _ary,
        // _aryElementsObservers);
        _m = new AdvancedMapping2(_index, _element, _ary, _aryElementsObservers);
        /*
         * int nHoles = (_element.max() - _element.min() +1) - _element.size(); if(_index.size() == _element.size() &&
         * nHoles < 2000) { _m = new OneToOneMapping(_index, _element, _ary); } else { _m = new SimpleMapping(_index,
         * _element, _ary); }
         */
    }

    @Override
    public void detachObserver(Observer observer) {
        super.detachObserver(observer);
        _element.detachObserver(observer);
    }

    @Override
    public String domainToString() {
        return _element.domainToString();
    }

    /**
     * Returns element-domain as an array of different sorted values.
     */
    int[] elementDomain() {
        int arMin = _ary.min();
        int arMax = _ary.max();
        int[] values = new int[arMax - arMin + 1];
        int valCounter = 0;

        class IntExpComparator implements java.util.Comparator {
            @Override
            public int compare(Object a1, Object a2) {

                if (((IntExp) a1).min() < ((IntExp) a2).min()) {
                    return -1;
                }
                if (((IntExp) a1).min() == ((IntExp) a2).min()) {
                    return 0;
                }
                return 1;
            }
        }

        IntExpArray tmp = makeExtraction(_index, _ary);
        tmp.sort(new IntExpComparator());

        for (int i = tmp.get(0).min(); i <= tmp.get(0).max(); i++) {
            if (tmp.get(0).contains(i)) {
                values[valCounter++] = i;
            }
        }

        for (int i = 1; i < tmp.size(); i++) {
            IntExp curElem = tmp.get(i);
            int min = curElem.min();
            int max = curElem.max();
            if (min < values[valCounter - 1]) {
                min = values[valCounter - 1] + 1;
            }
            for (int j = min; j <= max; j++) {
                if (curElem.contains(j)) {
                    values[valCounter++] = j;
                }
            }
        }

        int[] valFinally = new int[valCounter];
        System.arraycopy(values, 0, valFinally, 0, valCounter);
        return valFinally;
    }

    @Override
    public int max() {
        return _element.max();
    }

    @Override
    public int min() {
        return _element.min();
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _element.reattachObserver(observer);
    }

    @Override
    public void removeValue(int value) throws Failure {
        _element.removeValue(value);
    }

    @Override
    public void setMax(int max) throws Failure {
        _element.setMax(max);
    }

    @Override
    public void setMin(int min) throws Failure {
        _element.setMin(min);
    }

    @Override
    public void setValue(int value) throws Failure {
        _element.setValue(value);
    }

    @Override
    public int size() {
        return _element.size();
    }
}
