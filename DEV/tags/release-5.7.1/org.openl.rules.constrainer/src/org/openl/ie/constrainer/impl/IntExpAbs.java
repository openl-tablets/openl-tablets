package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;

//
//: IntExpAbs.java
//

/**
 * An implementation of the expression: <code>abs(IntExp)</code>.
 */
public final class IntExpAbs extends IntExpImpl {
    class ExpAbsObserver extends Observer {

        ExpAbsObserver() {
            // super(event_map);
        }

        @Override
        public Object master() {
            return IntExpAbs.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX | VALUE;
        }

        @Override
        public String toString() {
            return "ExpAbsObserver: " + _exp;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            IntEvent e = (IntEvent) event;

            // IntEventAbs ev = IntEventAbs.getEvent(e);
            // notifyObservers(ev);

            int min = e.min();
            int max = e.max();
            _abs.setMin(abs_min(min, max));
            _abs.setMax(abs_max(min, max));
        }

    } // ~ ExpAbsObserver
    private IntExp _exp;

    private Observer _observer;

    // static final private int[] event_map = { MIN | MAX, MIN,
    // MIN | MAX, MAX,
    // VALUE, VALUE,
    // REMOVE, REMOVE
    // };

    private IntVar _abs;

    static int abs_max(int min, int max) {
        if (min >= 0) {
            return max;
        }

        if (max < 0) {
            return -min;
        }

        return Math.max(-min, max);

    }

    static int abs_min(int min, int max) {
        if (min >= 0) {
            return min;
        }

        if (max >= 0) {
            return 0;
        }

        return -max;
    }

    public IntExpAbs(IntExp exp) {
        super(exp.constrainer());
        _exp = exp;
        _observer = new ExpAbsObserver();
        _exp.attachObserver(_observer);

        if (constrainer().showInternalNames()) {
            // _name = "IlcAbs("+exp.name()+")";
            _name = "|" + exp.name() + "|";
        }

        // int trace = IntVarImplTrace.TRACE_ALL;
        int trace = 0;
        int min = _exp.min();
        int max = _exp.max();
        _abs = constrainer().addIntVarTraceInternal(abs_min(min, max), abs_max(min, max), _name, IntVar.DOMAIN_PLAIN,
                trace);

    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _abs.attachObserver(observer);
    }

    @Override
    public boolean contains(int value) {
        // return value >= 0 && (_exp.contains(value) || _exp.contains(-value));
        return _abs.contains(value);
    }

    @Override
    public void detachObserver(Observer observer) {
        super.detachObserver(observer);
        _abs.detachObserver(observer);
    }

    public int max() {
        // return abs_max(_exp.min(), _exp.max());
        return _abs.max();
    }

    public int min() {
        // return abs_min( _exp.min(), _exp.max());
        return _abs.min();
    }

    @Override
    public void onMaskChange() {
        // _observer.publish(publisherMask(),_exp);
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _abs.reattachObserver(observer);
    }

    @Override
    public void removeValue(int value) throws Failure {
        if (value >= 0) {
            _abs.removeValue(value);
            _exp.removeValue(value);
            _exp.removeValue(-value);
        }
    }

    public void setMax(int max) throws Failure {
        _abs.setMax(max);

        _exp.setMax(max);
        _exp.setMin(-max);
    }

    public void setMin(int min) throws Failure {
        if (min <= 0) {
            return;
        }

        _abs.setMin(min);

        int removeMax = min - 1;

        _exp.removeRange(-removeMax, removeMax);

        // for(int i = -removeMax; i <= removeMax; ++i)
        // _exp.removeValue(i);
    }

    @Override
    public void setValue(int value) throws Failure {
        _abs.setValue(value);

        setMax(value);
        setMin(value);
    }

    // static final class IntEventAbs extends IntEvent
    // {
    // static ReusableFactory _factory = new ReusableFactory()
    // {
    // protected Reusable createNewElement()
    // {
    // return new IntEventAbs();
    // }
    //
    // };
    //
    // static IntEventAbs getEvent(IntEvent event)
    // {
    // IntEventAbs ev = (IntEventAbs) _factory.getElement();
    // ev.init(event);
    // return ev;
    // }
    //
    // IntEvent _event;
    //
    // int _type = 0;
    //
    // void init(IntEvent event)
    // {
    // _event = event;
    // _type = 0;
    //
    // if (max() < oldmax())
    // {
    // _type |= MAX;
    // }
    //
    // if (min() > oldmin())
    // {
    // _type |= MIN;
    // }
    //
    // if (min() == max())
    // _type |= VALUE;
    // }
    //
    //
    // public int type()
    // {
    // return _type;
    // }
    //
    //
    // public int oldmax()
    // {
    // return IntExpAbs.abs_max(_event.oldmin(), _event.oldmax());
    // }
    //
    // public int oldmin()
    // {
    // return IntExpAbs.abs_min(_event.oldmin(), _event.oldmax());
    // }
    //
    // public int max()
    // {
    // return IntExpAbs.abs_max(_event.min(), _event.max());
    // }
    //
    // public int min()
    // {
    // return IntExpAbs.abs_min(_event.min(), _event.max());
    // }
    //
    // public String name()
    // {
    // return "IntEventAbs";
    // }
    //
    // public int numberOfRemoves()
    // {
    // return 0;
    // }
    //
    // public int removed(int i)
    // {
    // return 0;
    // }
    // }

} // ~IntExpAbs
