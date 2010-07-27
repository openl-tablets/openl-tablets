package org.openl.ie.constrainer.impl;

import java.util.Map;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;


//
//: IntExpAddExp.java
//
/**
 * An implementation of the expression: <code>(IntExp1 + IntExp2)</code>.
 */
public final class IntExpBitAndExp extends IntExpImpl {
    class ExpBitAndExpObserver extends Observer {
        ExpBitAndExpObserver() {
            // super(event_map);
        }

        @Override
        public Object master() {
            return IntExpBitAndExp.this;
        }

        @Override
        public int subscriberMask() {
            return VALUE;
        }

        @Override
        public String toString() {
            return "ExpAddExpObserver: ";
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            // IntEvent e = (IntEvent) event;

            // int type = e.type();
            // if ((type & MIN) != 0)

            _and.setMin(calc_min());

            // if ((type & MAX) != 0)
            _and.setMax(calc_max());

        }

    } // ~ ExpAddExpObserver
    private IntExp _exp1;
    private IntExp _exp2;

    private Observer _observer;

    // static final private int[] event_map = { MIN, MIN,
    // MAX, MAX,
    // MIN | MAX | VALUE, VALUE
    // };

    private IntVar _and;

    public IntExpBitAndExp(IntExp exp1, IntExp exp2) {
        super(exp1.constrainer());

        if (constrainer().showInternalNames()) {
            _name = "(" + exp1.name() + "+" + exp2.name() + ")";
        }

        _exp1 = exp1;
        _exp2 = exp2;

        // int trace = IntVarImplTrace.TRACE_ALL;
        int trace = 0;
        _and = constrainer().addIntVarTraceInternal(calc_min(), calc_max(), _name, IntVar.DOMAIN_PLAIN, trace);

        _exp1.attachObserver(_observer = new ExpBitAndExpObserver());
        _exp2.attachObserver(_observer);
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _and.attachObserver(observer);
    }

    public int calc_max() {

        if (_exp1.bound() && _exp2.bound()) {
            return _exp1.valueUnsafe() & _exp2.valueUnsafe();
        }

        return Math.min(_exp1.max(), _exp2.max());
    }

    public int calc_min() {

        if (_exp1.bound() && _exp2.bound()) {
            return _exp1.valueUnsafe() & _exp2.valueUnsafe();
        }

        return 0;
    }

    @Override
    public double calcCoeffs(Map map, double factor) throws NonLinearExpression {
        throw new NonLinearExpression(this);
    }

    @Override
    public void detachObserver(Observer observer) {
        super.detachObserver(observer);
        _and.detachObserver(observer);
    }

    @Override
    public boolean isLinear() {
        return false;
    }

    public int max() {
        return _and.max();
    }

    public int min() {
        return _and.min();
    }

    @Override
    public void onMaskChange() {
        // _observer.publish(publisherMask(), _exp1);
        // _observer.publish(publisherMask(), _exp2);
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _and.reattachObserver(observer);
    }

    @Override
    public void removeValue(int value) throws Failure {
        int Max = max();
        if (value > Max) {
            return;
        }
        int Min = min();
        if (value < Min) {
            return;
        }
        if (Min == Max) {
            constrainer().fail("remove for IntExpAddExp");
        }
        if (value == Max) {
            setMax(value - 1);
        }
        if (value == Min) {
            setMin(value + 1);
        }
    }

    public void setMax(int max) throws Failure {
        // if (max >= _sum.max())
        // return;
        //
        // int max1 = max - _exp2.min();
        // if (max1 < _exp1.max())
        // _exp1.setMax(max1);
        // int max2 = max - _exp1.min();
        // if (max2 < _exp2.max())
        // _exp2.setMax(max2);
    }

    public void setMin(int min) throws Failure {

        // if (min <= _sum.min())
        // return;
        //
        // int min1 = min - _exp2.max();
        // if (min1 > _exp1.min())
        // _exp1.setMin(min1);
        // int min2 = min - _exp1.max();
        // if (min2 > _exp2.min())
        // _exp2.setMin(min2);
    }

    @Override
    public void setValue(int value) throws Failure {
        setMin(value);
        setMax(value);
    }

    // static final class IntEventAddExp extends IntEvent
    // {
    //
    // static ReusableFactory _factory = new ReusableFactory()
    // {
    // protected Reusable createNewElement()
    // {
    // return new IntEventAddExp();
    // }
    //
    // };
    //
    // static IntEventAddExp getEvent(IntEvent event, IntExp exp)
    // {
    // IntEventAddExp ev = (IntEventAddExp) _factory.getElement();
    // ev.init(event, exp);
    // return ev;
    // }
    //
    //
    //
    //
    // int _type;
    // IntExp _second;
    // IntEvent _event;
    //
    // public String name()
    // {
    // return "Event AddExp";
    // }
    //
    //
    //
    // public void init(IntEvent e, IntExp second)
    // {
    // _event = e;
    // _second = second;
    // _type = e.type();
    // if (!_second.bound())
    // _type &= ~(REMOVE | VALUE);
    // }
    //
    // public int type()
    // {
    // return _type;
    // }
    //
    //
    //
    // public int removed(int i)
    // {
    // return _event.removed(i) + _second.min();
    // }
    //
    //
    // public int min()
    // {
    // return _event.min() + _second.min();
    // }
    //
    // public int max()
    // {
    // return _event.max() + _second.max();
    // }
    //
    // public int oldmin()
    // {
    // return _event.oldmin() + _second.min();
    // }
    //
    //
    // public int oldmax()
    // {
    // return _event.oldmax() + _second.max();
    // }
    //
    // public int numberOfRemoves()
    // {
    // if ((_type & REMOVE) != 0)
    // return _event.numberOfRemoves();
    // return 0;
    // }
    //
    // }

} // ~IntExpAddExp
