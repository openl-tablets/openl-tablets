package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;

/**
 * An implementation of the expression: <code>sqr(IntExp)</code>.
 * <p>
 * Examples:
 * <p>
 * <code>IntVar var = constrainer.addIntVar(min,max,name);
 *  <p>
 *  IntExp exp = var.sqr();
 *  </code>
 */
public final class IntExpSqr extends IntExpImpl {
    static final class IntEventSqr extends IntEvent {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new IntEventSqr();
            }

        };

        IntEvent _event;

        int _type = 0;

        static IntEventSqr getEvent(IntEvent event, IntExp exp) {
            IntEventSqr ev = (IntEventSqr) _factory.getElement();
            ev.init(event, exp);
            return ev;
        }

        void init(IntEvent event, IntExp exp_) {
            exp(exp_);
            _event = event;
            _type = 0;

            if (max() < oldmax()) {
                _type |= MAX;
            }

            if (min() > oldmin()) {
                _type |= MIN;
            }

            if (min() == max()) {
                _type |= VALUE;
            }
        }

        @Override
        public int max() {
            return IntCalc.sqrMax(_event.min(), _event.max());
        }

        @Override
        public int min() {
            return IntCalc.sqrMin(_event.min(), _event.max());
        }

        @Override
        public String name() {
            return "IntEventSqr";
        }

        @Override
        public int numberOfRemoves() {
            return 0;
        }

        @Override
        public int oldmax() {
            return IntCalc.sqrMax(_event.oldmin(), _event.oldmax());
        }

        @Override
        public int oldmin() {
            return IntCalc.sqrMin(_event.oldmin(), _event.oldmax());
        }

        @Override
        public int removed(int i) {
            return 0;
        }

        @Override
        public int type() {
            return _type;
        }

    }
    class IntExpSqrObserver extends ExpressionObserver {

        IntExpSqrObserver() {
            super(event_map);
        }

        @Override
        public Object master() {
            return IntExpSqr.this;
        }

        @Override
        public String toString() {
            return "IntExpSqrObserver: " + _exp;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            IntEvent e = (IntEvent) event;

            IntEventSqr ev = IntEventSqr.getEvent(e, IntExpSqr.this);

            notifyObservers(ev);
        }
    } // ~ IntExpSqrObserver

    static final private int[] event_map = { MIN | MAX, MIN, MIN | MAX, MAX, VALUE, VALUE, REMOVE, REMOVE };

    private IntExp _exp;

    private ExpressionObserver _observer;

    public IntExpSqr(IntExp exp) {
        super(exp.constrainer());
        _exp = exp;

        if (constrainer().showInternalNames()) {
            _name = "(" + exp.name() + "*" + exp.name() + ")";
        }

        _observer = new IntExpSqrObserver();
        _exp.attachObserver(_observer);
    }

    public int max() {
        return IntCalc.sqrMax(_exp.min(), _exp.max());
    }

    public int min() {
        return IntCalc.sqrMin(_exp.min(), _exp.max());
    }

    @Override
    public void onMaskChange() {
        _observer.publish(publisherMask(), _exp);
    }

    @Override
    public void removeValue(int value) throws Failure {
        if (value < 0) {
            return;
        }

        int sqrtValue = IntCalc.sqrtInt(value);
        if (sqrtValue < 0) {
            return;
        }

        _exp.removeValue(sqrtValue);
        _exp.removeValue(-sqrtValue);
    }

    public void setMax(int max) throws Failure {
        if (max < 0) {
            constrainer().fail("max < 0");
        }

        int expMinMax = (int) Math.sqrt(max);
        _exp.setMax(expMinMax);
        _exp.setMin(-expMinMax);
    }

    public void setMin(int min) throws Failure {
        if (min <= 0) {
            return;
        }

        int expMinMax = (int) Math.sqrt(min);

        if (expMinMax * expMinMax == min) {
            expMinMax--;
        }

        for (int i = -expMinMax; i <= expMinMax; i++) {
            _exp.removeValue(i);
        }
    }

    @Override
    public void setValue(int value) throws Failure {
        if (value < 0) {
            constrainer().fail("value < 0");
        }

        int sqrtValue = IntCalc.sqrtInt(value);
        if (sqrtValue < 0) {
            constrainer().fail("value is not a square");
        }

        _exp.setValue(sqrtValue);
    }

    @Override
    public String toString() {
        return "sqr(" + _exp + ")" + domainToString();
    }

} // ~IntExpSqr
