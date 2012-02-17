package org.openl.ie.constrainer.impl;

import java.util.Map;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatEvent;
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.NonLinearExpression;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.tools.Reusable;
import org.openl.ie.tools.ReusableFactory;


/**
 * An implementation of the expression: <code>(FloatExp * value)</code> for
 * positive "value".
 */
public final class FloatExpMultiplyPositive extends FloatExpImpl {
    static class FloatEventMulPositiveValue extends FloatEvent {

        static ReusableFactory _factory = new ReusableFactory() {
            @Override
            protected Reusable createNewElement() {
                return new FloatEventMulPositiveValue();
            }

        };

        private double _value;

        private FloatEvent _event;
        static FloatEventMulPositiveValue getEvent(FloatEvent event, double value) {
            FloatEventMulPositiveValue ev = (FloatEventMulPositiveValue) _factory.getElement();
            ev.init(event, value);
            return ev;
        }

        public void init(FloatEvent e, double value) {
            _event = e;
            _value = value;
        }

        @Override
        public double max() {
            return _event.max() * _value;
        }

        @Override
        public double min() {
            return _event.min() * _value;
        }

        @Override
        public String name() {
            return "FloatEventMulPositiveValue";
        }

        @Override
        public double oldmax() {
            return _event.oldmax() * _value;
        }

        @Override
        public double oldmin() {
            return _event.oldmin() * _value;
        }

        @Override
        public int type() {
            return _event.type();
        }

    } // ~ FloatEventMulPositiveValue
    class FloatExpMultiplyPositiveObserver extends ExpressionObserver {

        @Override
        public Object master() {
            return FloatExpMultiplyPositive.this;
        }

        @Override
        public String toString() {
            return "FloatExpMultiplyPositiveObserver: " + _exp + "+" + _value;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            FloatEvent e = (FloatEvent) event;

            FloatEventMulPositiveValue ev = FloatEventMulPositiveValue.getEvent(e, _value);

            notifyObservers(ev);

        }

    } // ~ FloatExpMultiplyPositiveObserver
    private FloatExp _exp;

    private double _value;

    private ExpressionObserver _observer;

    public FloatExpMultiplyPositive(FloatExp exp, double value) {
        super(exp.constrainer(), "");// exp.name()+"+"+value);
        if (value <= 0) {
            abort("negative value in FloatExpMultiplyPositive");
        }
        _exp = exp;
        _value = value;
        _observer = new FloatExpMultiplyPositiveObserver();
        _exp.attachObserver(_observer);
    }

    @Override
    public double calcCoeffs(Map map, double factor) throws NonLinearExpression {
        return _exp.calcCoeffs(map, factor * _value);
    }

    @Override
    public boolean isLinear() {
        return _exp.isLinear();
    }

    public double max() {
        return _exp.max() * _value;
    }

    public double min() {
        return _exp.min() * _value;
    }

    @Override
    public FloatExp mul(double value) {
        return _exp.mul(_value * value);
    }

    @Override
    public void onMaskChange() {
        _observer.publish(publisherMask(), _exp);
    }

    public void setMax(double max) throws Failure {
        _exp.setMax(max / _value); // may fail
    }

    public void setMin(double min) throws Failure {
        _exp.setMin(min / _value); // may fail
    }

    public void setValue(double value) throws Failure {
        _exp.setValue(value / _value);
    }

    @Override
    public String toString() {
        domainToString();
        return name() + "(" + _exp + "x" + _value + domainToString() + ")";
    }

} // ~FloatExpMultiplyPositive
