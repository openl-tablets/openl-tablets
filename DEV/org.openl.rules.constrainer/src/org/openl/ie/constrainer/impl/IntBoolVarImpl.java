package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalInstantiate;
import org.openl.ie.constrainer.IntBoolVar;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.Undo;
import org.openl.ie.constrainer.UndoImpl;
import org.openl.ie.constrainer.Undoable;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

/**
 * An implementation of the IntBoolVar interface. This implementation is optimized for [0..1] domain.
 */
public class IntBoolVarImpl extends IntBoolExpImpl implements IntBoolVar {
    static final class IntEventBool extends IntEvent {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new IntEventBool();
            }

        };

        int _int_value, _type;

        static IntEventBool getEvent(IntExp exp, boolean val) {
            IntEventBool ev = (IntEventBool) _factory.getElement();
            ev.init(exp, val);
            return ev;
        }

        public void init(IntExp exp, boolean val) {
            exp(exp);

            if (val) {
                _int_value = 1;
                _type = MIN | VALUE;
            } else {
                _int_value = 0;
                _type = MAX | VALUE;
            }

        }

        @Override
        public int max() {
            return _int_value;
        }

        @Override
        public int min() {
            return _int_value;
        }

        @Override
        public String name() {
            return "IntBoolEvent";
        }

        @Override
        public int numberOfRemoves() {
            return 0;
        }

        @Override
        public int oldmax() {
            return 1;
        }

        @Override
        public int oldmin() {
            return 0;
        }

        @Override
        public int removed(int i) {
            return -1;
        }

        @Override
        public int type() {
            return _type;
        }

    }

    /**
     * An implementation of the 'false' event.
     */
    static final class IntEventBoolFalse extends IntEvent {
        public static final IntEventBoolFalse the = new IntEventBoolFalse();

        @Override
        public void free() {
        }

        @Override
        public int max() {
            return 0;
        }

        @Override
        public int min() {
            return 0;
        }

        @Override
        public String name() {
            return "IntBoolEventFalse";
        }

        @Override
        public int numberOfRemoves() {
            return 0;
        }

        @Override
        public int oldmax() {
            return 1;
        }

        @Override
        public int oldmin() {
            return 0;
        }

        @Override
        public int removed(int i) {
            return -1;
        }

        @Override
        public int type() {
            return MAX | VALUE;
        }

    } // ~IntEventBoolFalse

    /**
     * An implementation of the 'true' event.
     */
    static final class IntEventBoolTrue extends IntEvent {
        public static final IntEventBoolTrue the = new IntEventBoolTrue();

        @Override
        public void free() {
        }

        @Override
        public int max() {
            return 1;
        }

        @Override
        public int min() {
            return 1;
        }

        @Override
        public String name() {
            return "IntBoolEventTrue";
        }

        @Override
        public int numberOfRemoves() {
            return 0;
        }

        @Override
        public int oldmax() {
            return 1;
        }

        @Override
        public int oldmin() {
            return 0;
        }

        @Override
        public int removed(int i) {
            return -1;
        }

        @Override
        public int type() {
            return MIN | VALUE;
        }

    } // ~IntEventBoolTrue

    /**
     * Undo Class for IntBoolVar.
     */
    static final class UndoIntBoolVar extends UndoSubject {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new UndoIntBoolVar();
            }

        };

        int _min, _max;

        static UndoIntBoolVar getBoolVarUndo() {
            return (UndoIntBoolVar) _factory.getElement();
        }

        /**
         * Use to display the UndoIntVar object
         */
        @Override
        public String toString() {
            return "UndoIntBoolVar " + undoable();
        }

        /**
         * Execute undo() operation for this UndoIntVar object
         */

        @Override
        public void undo() {
            IntBoolVar var = (IntBoolVar) undoable();
            var.forceMin(_min);
            var.forceMax(_max);
            super.undo();
        }

        @Override
        public void undoable(Undoable u) {
            super.undoable(u);
            IntBoolVarImpl var = (IntBoolVarImpl) u;
            _min = var.min();
            _max = var.max();

        }
    } // ~UndoIntBoolVar

    /**
     * Undo Class for IntBoolVar's "value only change".
     */
    static final class UndoIntBoolVarValue extends UndoImpl {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new UndoIntBoolVarValue();
            }

        };

        static UndoIntBoolVarValue getUndo(IntBoolVarImpl v) {
            UndoIntBoolVarValue undo = (UndoIntBoolVarValue) _factory.getElement();
            undo.undoable(v);
            return undo;
        }

        /**
         * Use to display the UndoIntVarValue object.
         */
        @Override
        public String toString() {
            return "UndoIntBoolVarValue " + undoable();
        }

        /**
         * Executes undo() operation for this UndoIntVarValue object.
         */
        @Override
        public void undo() {
            IntBoolVar var = (IntBoolVar) undoable();
            var.forceMin(0);
            var.forceMax(1);
            super.undo();
        }
    } // ~UndoIntBoolVarValue

    protected int _max;

    protected int _min;

    public IntBoolVarImpl(Constrainer constrainer) {
        this(constrainer, "");
    }

    public IntBoolVarImpl(Constrainer constrainer, String name) {
        super(constrainer, name);
        _min = 0;
        _max = 1;
    }

    @Override
    public Undo createUndo() {
        return UndoIntBoolVar.getBoolVarUndo();
    }

    @Override
    public int domainType() {
        return DOMAIN_BOOL;
    }

    @Override
    public void forceInsert(int val) {
        abort("Temporary method???");
    }

    @Override
    public void forceMax(int max) {
        _max = max;
    }

    @Override
    public void forceMin(int min) {
        _min = min;
    }

    @Override
    public void forceSize(int max) {
        abort("Temporary method???");
    }

    @Override
    public Goal instantiate() {
        return new GoalInstantiate(this);
    }

    @Override
    public int max() {
        return _max;
    }

    @Override
    public int min() {
        return _min;
    }

    @Override
    public void propagate() throws Failure {
        notifyObservers(IntEventBool.getEvent(this, _min == 1));
    }

    @Override
    public void setMax(int max) throws Failure {
        if (max < _min) {
            _constrainer.fail("set max bool var");
        }

        if (max < _max) {
            // addUndo();
            // if(!undone())
            constrainer().addUndo(UndoIntBoolVarValue.getUndo(this));

            _max = max;

            notifyObservers(IntEventBoolFalse.the);
            // notifyObservers(IntEventBool.getEvent(this, false));
            // addToPropagationQueue();
        }
    }

    @Override
    public void setMin(int min) throws Failure {
        if (min > _max) {
            _constrainer.fail("set min bool var");
        }

        if (min > _min) {
            // addUndo();
            // if(!undone())
            constrainer().addUndo(UndoIntBoolVarValue.getUndo(this));

            _min = min;

            notifyObservers(IntEventBoolTrue.the);
            // notifyObservers(IntEventBool.getEvent(this, true));
            // addToPropagationQueue();
        }
    }

    @Override
    public void removeValue(int value) throws Failure {
        if (value == 1)
            setMax(0);
        else
            setMin(1);
    }

} // ~IntBoolVarImpl
