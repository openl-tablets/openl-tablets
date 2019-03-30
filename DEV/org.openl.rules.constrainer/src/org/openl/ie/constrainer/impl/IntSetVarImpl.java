package org.openl.ie.constrainer.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.IntBoolVar;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntSetVar;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.constrainer.UndoImpl;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

public class IntSetVarImpl extends SubjectImpl implements IntSetVar {

    class ElementsObserver extends Observer {
        private int _val;

        public ElementsObserver(int val) {
            _val = val;
        }

        @Override
        public Object master() {
            return IntSetVarImpl.this;
        }

        @Override
        public int subscriberMask() {
            return IntEvent.Constants.ALL;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            constrainer().addUndo(UndoPossibleSetReduction.getUndo(IntSetVarImpl.this));
            _unboundsCounter--;
            IntEvent e = (IntEvent) event;
            int valueMask = 0;
            if (_unboundsCounter == 0) {
                valueMask = IntSetEvent.IntSetEventConstants.VALUE;
            }
            if (e.max() == 1) {
                // TRUE
                notifyObservers(IntSetEvent
                    .getEvent(IntSetVarImpl.this, _val, IntSetEvent.IntSetEventConstants.REQUIRE | valueMask));
            } else {
                // FALSE
                notifyObservers(IntSetEvent
                    .getEvent(IntSetVarImpl.this, _val, IntSetEvent.IntSetEventConstants.REMOVE | valueMask));
            }
        } // ~update()
    }

    static public class UndoPossibleSetReduction extends UndoImpl {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new UndoPossibleSetReduction();
            }
        };

        static UndoPossibleSetReduction getUndo(IntSetVarImpl var) {
            UndoPossibleSetReduction undo = (UndoPossibleSetReduction) _factory.getElement();
            undo.undoable(var);
            return undo;
        }

        @Override
        public String toString() {
            return "UndoPossibleSetReduction " + undoable();
        }

        @Override
        public void undo() {
            IntSetVarImpl var = (IntSetVarImpl) undoable();
            var._unboundsCounter++;
            super.undo();
        }
    }

    private IntExpArray _set;

    private HashMap _values2index = new HashMap();

    private int _unboundsCounter;

    private IntSetVarImpl(Constrainer C) {
        super(C);
    }

    public IntSetVarImpl(Constrainer C, int[] array) {
        this(C, array, "");
    }

    public IntSetVarImpl(Constrainer C, int[] array, String name) {
        super(C, name);
        int size = array.length;
        _set = new IntExpArray(C, size);
        for (int i = 0; i < size; i++) {
            _set.set(C.addIntBoolVarInternal(name() + "[" + array[i] + "]"), i);
            _set.get(i).attachObserver(new ElementsObserver(array[i]));
            _values2index.put(new Integer(array[i]), new Integer(i));
        }
        _unboundsCounter = size;
    }

    public boolean bound() {
        for (int i = 0; i < _set.size(); i++) {
            if (!_set.get(i).bound()) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(Set anotherSet) {
        if (!_values2index.keySet().containsAll(anotherSet)) {
            return false;
        }
        Iterator iter = anotherSet.iterator();
        while (iter.hasNext()) {
            int val = ((Integer) iter.next()).intValue();
            if (!possible(val)) {
                return false;
            }
        }
        return true;
    }

    public Goal generate() {
        return new GoalGenerate(_set);
    }

    private IntBoolVar hasElem(int i) {
        int idx = ((Integer) _values2index.get(new Integer(i))).intValue();
        return (IntBoolVar) _set.get(idx);
    }

    public IntSetVar intersectionWith(IntSetVar anotherSet) {
        if (anotherSet instanceof IntSetVarImpl) {
            return intersectionWith((IntSetVarImpl) anotherSet);
        } else {
            return anotherSet.intersectionWith(this);
        }
    }

    public IntSetVar intersectionWith(IntSetVarImpl anotherSet) {

        Set values1 = anotherSet._values2index.keySet(), values2 = _values2index.keySet();

        int[] tmp = new int[values1.size()];

        Iterator iter = values1.iterator();
        int counter = 0;
        while (iter.hasNext()) {
            Integer curValue = (Integer) iter.next();
            if (values2.contains(curValue)) {
                tmp[counter++] = curValue.intValue();
            }
        }
        /** @todo add emptiness check */

        int[] intersection = new int[counter];
        System.arraycopy(tmp, 0, intersection, 0, counter);
        IntSetVarImpl result = (IntSetVarImpl) constrainer().addIntSetVar(intersection);
        for (int i = 0; i < intersection.length; i++) {
            int val = intersection[i];
            try {
                result.hasElem(val).equals(hasElem(val).and(anotherSet.hasElem(val))).execute();
            } catch (Failure f) {/* it would be never thrown */
            }
        }
        return result;
    }

    public boolean possible(int value) {
        return (hasElem(value).max() == 1);
    }

    @Override
    public void propagate() throws Failure {
    }

    public void remove(int val) throws Failure {
        hasElem(val).setFalse();
    }

    public void require(int val) throws Failure {
        hasElem(val).setTrue();
    }

    public boolean required(int value) {
        return (hasElem(value).min() == 1);
    }

    public Set requiredSet() {
        java.util.HashSet values = new java.util.HashSet();
        Iterator iter = _values2index.keySet().iterator();
        while (iter.hasNext()) {
            Integer curValue = (Integer) iter.next();
            if (hasElem(curValue.intValue()).min() == 1) {
                values.add(curValue);
            }
        }
        /** @todo add emptiness chrecking */
        return values;
    }

    public IntSetVar unionWith(IntSetVar anotherSet) {
        if (anotherSet instanceof IntSetVarImpl) {
            return unionWith((IntSetVarImpl) anotherSet);
        } else {
            return anotherSet.unionWith(this);
        }
    }

    public IntSetVar unionWith(IntSetVarImpl anotherSet) {
        Set values1 = _values2index.keySet(), values2 = anotherSet._values2index.keySet();

        int[] tmp = new int[values1.size() + values2.size()];
        int counter = 0;
        Iterator iter = values1.iterator();
        while (iter.hasNext()) {
            tmp[counter++] = ((Integer) iter.next()).intValue();
        }
        iter = values2.iterator();
        while (iter.hasNext()) {
            Integer curValue = (Integer) iter.next();
            if (!values1.contains(curValue)) {
                tmp[counter++] = (curValue).intValue();
            }
        }

        int[] union = new int[counter];
        System.arraycopy(tmp, 0, union, 0, counter);

        IntSetVarImpl result = (IntSetVarImpl) constrainer().addIntSetVar(union);

        for (int i = 0; i < union.length; i++) {
            int val = union[i];
            if (values1.contains(new Integer(val))) {
                if (values2.contains(new Integer(val))) {
                    try {
                        result.hasElem(val).equals(hasElem(val).or(anotherSet.hasElem(val))).execute();
                    } catch (Failure f) {/* it would be never thrown */
                    }
                } else {
                    try {
                        result.hasElem(val).equals(hasElem(val)).execute();
                    } catch (Failure f) {/* it would be never thrown */
                    }
                    ;
                }
            } else {
                try {
                    result.hasElem(val).equals(anotherSet.hasElem(val)).execute();
                } catch (Failure f) {/* it would be never thrown */
                }
                ;
            }
        }
        return result;
    }

    public Set value() throws Failure {
        if (!bound()) {
            constrainer().fail("Attempt to get value of the unbound variable " + this);
        }
        return requiredSet();
    }

}