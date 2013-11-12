package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;

/**
 * Title: Integer Expression to the power of Integer Expression Description:
 * class Copyright: 2002 Company: Exigen Group, Inc.
 *
 * @author Sergej Vanskov
 * @version 1.0
 */

public final class IntExpPowIntExp extends IntExpImpl {
    /**
     * Calculation for _base.max > 0 and _base.min < 0
     */
    final class CalcG extends IntExpPowExpCalc {
        private IntExp _base;
        private IntExp _power;

        public CalcG(IntExp base, IntExp power) {
            _base = base;
            _power = power;
        }

        @Override
        public int max() {
            if (_base.min() >= 0) {
                // positive base
                return (int) Math.pow(_base.max(), _power.max());
            } else {
                // _base.min() < 0
                if (_power.max() % 2 == 0) {
                    // power.max() is even value
                    return (int) Math.pow(Math.max(_base.max(), -_base.min()), _power.max());
                } else {
                    int max_even_power = max_even_power();
                    if (max_even_power < 0) {
                        // there are only odd values in power's domain
                        if (_base.max() == 0) {
                            return 0;
                        }
                        return (int) Math.pow(_base.max(), _power.max());
                    } else {
                        return (int) Math.max(Math.pow(_base.max(), _power.max()), Math
                                .pow(_base.min(), max_even_power));
                    }
                }
            }
        }

        @Override
        public int min() {
            if (_base.min() == 0) {
                if (_power.max() == 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
            if (_base.min() > 0) {
                // positive base
                return (int) Math.pow(_base.min(), _power.min());
            } else {
                // _base.min() < 0
                if (_power.max() % 2 == 0) {
                    // power.max() is even value
                    int max_odd_power = max_odd_power();
                    if (max_odd_power < 0) {
                        // power's domain doesn't contain odd values
                        if (_base.contains(0)) {
                            if (_power.max() == 0) {
                                return 1;
                            } else {
                                return 0;
                            }
                        }
                        if (_base.max() > 0) {
                            int min_positive = 1;
                            while ((!_base.contains(min_positive)) && (min_positive <= _base.max())) {
                                min_positive++;
                            }
                            int max_negative = -1;
                            while ((!_base.contains(max_negative)) && (max_negative >= _base.min())) {
                                max_negative--;
                            }
                            return (int) Math.pow(Math.min(min_positive, -max_negative), _power.min());
                        } else {
                            return (int) Math.pow(_base.max(), _power.min());
                        }
                    } else {
                        return (int) Math.pow(_base.min(), max_odd_power);
                    }
                } else {
                    // power.max() is odd value
                    return (int) Math.pow(_base.min(), _power.max());
                }
            }
        }

        @Override
        public void setMax(int max) throws Failure {
            if (max <= 0) {
                remove_all_odd_power();
            }
        }

        @Override
        public void setMin(int min) throws Failure {
            if (min > 0) {
                remove_all_even_power();
            }
        }
    }
    /**
     * Calculation for _base <= 0.
     */
    final class CalcN extends IntExpPowExpCalc {
        private IntExp _base;
        private IntExp _power;

        public CalcN(IntExp base, IntExp power) {
            _base = base;
            _power = power;
        }

        @Override
        public int max() {
            int max_even_power = max_even_power();
            if (max_even_power > 0) {
                return (int) Math.pow(_base.min(), max_even_power);
            } else {
                if (_base.max() == 0) {
                    if (_power.max() == 0) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
                return (int) Math.pow(_base.max(), _power.min());
            }
        }

        @Override
        public int min() {
            int max_odd_power = max_odd_power();
            if (max_odd_power > 0) {
                return (int) Math.pow(_base.min(), max_odd_power);
            } else {
                // if there is no odd values in the power's domain
                if (_base.max() == 0) {
                    if (_power.max() == 0) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
                return (int) Math.pow(_base.max(), _power.min());
            }
        }

        @Override
        public void setMax(int max) throws Failure {
            if (max <= 0) {
                remove_all_even_power();

                // finding new max for power
                int base_max = -_base.max();
                if (base_max > 1 && ((int) Math.pow(base_max, _power.max())) > -max) {
                    _power.setMax((int) (Math.log(-max) / Math.log(base_max)));
                }

                // finding new max for base
                int power_min = _power.min();
                if (power_min > 0 && ((int) Math.pow(-_base.min(), power_min)) > -max) {
                    _base.setMin(-(int) (Math.pow(-max, 1.0 / (power_min))));
                }
            } else { /* max > 0 */
                int abs_max = Math.max(max, Math.abs(_result.min()));

                // finding new max for power
                int base_max = -_base.max();
                if (base_max > 1 && ((int) Math.pow(base_max, _power.max())) > abs_max) {
                    _power.setMax((int) (Math.log(abs_max) / Math.log(base_max)));
                }

                // finding new max for base
                int power_min = _power.min();
                if (power_min > 0 && ((int) Math.pow(-_base.min(), power_min)) > abs_max) {
                    _base.setMin(-(int) (Math.pow(abs_max, 1.0 / (power_min))));
                }
            }
        }

        @Override
        public void setMin(int min) throws Failure {
            if (min > 0) {
                remove_all_odd_power();

                // finding new min for power
                int base_max = -_base.max();
                if (base_max > 1 && ((int) Math.pow(base_max, _power.min())) < min) {
                    int min_power;
                    min_power = (int) (Math.log(min) / Math.log(base_max));
                    if ((int) Math.pow(base_max, min_power) < min) {
                        min_power++;
                    }
                    _power.setMax(min_power);
                }

                // finding new min for base
                int power_max = _power.max();
                if (power_max > 0 && ((int) Math.pow(_base.min(), power_max)) < min) {
                    int min_base;
                    min_base = (int) (Math.pow(min, 1.0 / (power_max)));
                    if ((int) Math.pow(min_base, power_max) < min) {
                        min_base++;
                    }
                    _base.setMax(-min_base);
                }
            } else {

                // finding new min for power
                int base_max = -_base.max();
                if (base_max > 1 && ((int) Math.pow(base_max, _power.min())) < min) {
                    int min_power;
                    min_power = (int) (Math.log(min) / Math.log(base_max));
                    if ((int) Math.pow(base_max, min_power) < min) {
                        min_power++;
                    }
                    _power.setMin(min_power);
                }

                // finding new min for base
                int power_max = _power.max();
                if (power_max > 0 && ((int) Math.pow(_base.min(), power_max)) < min) {
                    int max_base;
                    max_base = (int) (Math.pow(min, 1.0 / (power_max)));
                    if ((int) Math.pow(max_base, power_max) < min) {
                        max_base++;
                    }
                    _base.setMax(-max_base);
                }
            }
        }
    }
    /**
     * Calculation for _base >= 0.
     */
    final class CalcP extends IntExpPowExpCalc {
        private IntExp _base;
        private IntExp _power;

        public CalcP(IntExp base, IntExp power) {
            _base = base;
            _power = power;
        }

        @Override
        public int max() {
            return (int) Math.pow(_base.max(), _power.max());
        }

        @Override
        public int min() {
            if (_base.min() == 0) {
                if (_power.max() == 0) {
                    return 1;
                } else {
                    return 0;
                }
            }
            return (int) Math.pow(_base.min(), _power.min());
        }

        @Override
        public void setMax(int max) throws Failure {
            // finding new max for power
            int base_min = _base.min();
            if (base_min > 1 && ((int) Math.pow(base_min, _power.max())) > max) {
                _power.setMax((int) (Math.log(max) / Math.log(base_min)));
            }

            // finding new max for base
            int power_min = _power.min();
            if (power_min > 0 && ((int) Math.pow(_base.max(), power_min)) > max) {
                _base.setMin((int) (Math.pow(max, 1.0 / (power_min))));
            }
        }

        @Override
        public void setMin(int min) throws Failure {
            // finding new min for power
            int base_max = _base.max();
            if (base_max > 1 && ((int) Math.pow(base_max, _power.min())) < min) {
                int min_power;
                min_power = (int) (Math.log(min) / Math.log(base_max));
                if ((int) Math.pow(base_max, min_power) < min) {
                    min_power++;
                }
                _power.setMin(min_power);
            }

            // finding new min for base
            int power_max = _power.max();
            if (power_max > 0 && ((int) Math.pow(_base.min(), power_max)) < min) {
                int min_base;
                min_base = (int) (Math.pow(min, 1.0 / (power_max)));
                if ((int) Math.pow(min_base, power_max) < min) {
                    min_base++;
                }
                _base.setMin(min_base);
            }
        }
    }
    class ExpPowerExpBaseObserver extends Observer {
        @Override
        public Object master() {
            return IntExpPowIntExp.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX | VALUE;
        }

        @Override
        public String toString() {
            return "ExpPowerExpBaseObserver: " + _base + " ** " + _power;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            _calc.updateFromObserver();
        }
    }
    class ExpPowerExpPowerObserver extends Observer {
        @Override
        public Object master() {
            return IntExpPowIntExp.this;
        }

        @Override
        public int subscriberMask() {
            return ALL;
        }

        @Override
        public String toString() {
            return "ExpPowerExpPowerObserver: " + _base + " ** " + _power;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            _calc.updateFromObserver();
        }
    }
    abstract class IntExpPowExpCalc {
        // May be overriden where min/max more optimal to calculate
        // simultaneously.
        public void createResult() {
            createResultVar(min(), max());
        }

        abstract public int max();

        abstract public int min();

        abstract public void setMax(int max) throws Failure;

        abstract public void setMin(int min) throws Failure;

        // May be overriden where min/max more optimal to calculate
        // simultaneously.
        public void updateFromObserver() throws Failure {
            updateResultVar(min(), max());
        }
    }

    private IntExp _base;

    private IntExp _power;

    private IntVar _result;

    private IntExpPowExpCalc _calc;

    private ExpPowerExpBaseObserver _base_observer;

    private ExpPowerExpPowerObserver _power_observer;

    public IntExpPowIntExp(IntExp exp, IntExp pow_exp) {
        super(exp.constrainer());

        if (constrainer().showInternalNames()) {
            _name = "(" + exp.name() + "**" + pow_exp.name() + ")";
        }

        _base = exp;
        _power = pow_exp;

        _base.attachObserver(_base_observer = new ExpPowerExpBaseObserver());
        _power.attachObserver(_power_observer = new ExpPowerExpPowerObserver());

        createCalc();

        _calc.createResult();
    }

    @Override
    public void attachObserver(Observer observer) {
        super.attachObserver(observer);
        _result.attachObserver(observer);
    }

    private void createCalc() {
        if (_base.min() >= 0) {
            _calc = new CalcP(_base, _power);
        } else if (_base.max() <= 0) {
            _calc = new CalcN(_base, _power);
        } else {
            _calc = new CalcG(_base, _power);
        }

    }

    private void createResultVar(int min, int max) {
        int trace = 0;
        _result = constrainer().addIntVarTraceInternal(min, max, _name, IntVar.DOMAIN_PLAIN, trace);
    }

    @Override
    public void detachObserver(Observer observer) {
        super.detachObserver(observer);
        _result.detachObserver(observer);
    }

    public int max() {
        return _result.max();
    }

    private int max_even_power() {
        if (_power.max() % 2 == 0) {
            return _power.max();
        } else {
            int min_power = _power.min();
            int max_even_power = _power.max() - 1;
            while (!_power.contains(max_even_power) && max_even_power > min_power) {
                max_even_power -= 2;
            }
            if (max_even_power < min_power) {
                return -1;
            } else {
                return max_even_power;
            }
        }
    }

    private int max_odd_power() {
        if (_power.max() % 2 == 1) {
            return _power.max();
        } else {
            int min_power = _power.min();
            int max_even_power = _power.max() - 1;
            while (!_power.contains(max_even_power) && max_even_power > min_power) {
                max_even_power -= 2;
            }
            if (max_even_power < min_power) {
                return -1;
            } else {
                return max_even_power;
            }
        }
    }

    public int min() {
        return _result.min();
    }

    @Override
    public void name(String name) {
        super.name(name);
        _result.name(name);
    }

    @Override
    public void reattachObserver(Observer observer) {
        super.reattachObserver(observer);
        _result.reattachObserver(observer);
    }

    private void remove_all_even_power() throws Failure {
        int min_power = _power.min();
        int max_power = _power.max();

        for (int power = min_power; power <= max_power; power++) {
            if (power % 2 == 0) {
                _power.removeValue(power);
            }
        }
    }

    private void remove_all_odd_power() throws Failure {
        int min_power = _power.min();
        int max_power = _power.max();

        for (int power = min_power; power <= max_power; power++) {
            if (power % 2 == 1) {
                _power.removeValue(power);
            }
        }
    }

    public void setMax(int max) throws org.openl.ie.constrainer.Failure {
        if (max >= max()) {
            return;
        }
        _result.setMax(max);
        _calc.setMax(max);
    }

    public void setMin(int min) throws org.openl.ie.constrainer.Failure {
        if (min <= min()) {
            return;
        }
        _result.setMin(min);
        _calc.setMin(min);
    }

    @Override
    public void setValue(int value) throws Failure {
        setMin(value);
        setMax(value);
    }

    private void updateResultVar(int min, int max) throws Failure {
        _result.setMin(min);
        _result.setMax(max);
    }
}
/*
 * if (min < _result.min ()) { return; }
 *
 * if (min < 0) { int max_odd_power = max_odd_power ();
 *
 * if (((int) Math.pow (_base.min (), max_odd_power)) < min) { int min_base;
 * min_base = - (int) (Math.pow (-min, 1.0 /((double) max_odd_power)));
 * _base.removeRange (_base.min (), min_base); }
 *
 * if (((int) Math.pow (_base.max (), _power.min ())) < min) { int min_power;
 * min_power = (int) (Math.log (min) / Math.log (_base.max ())); if ((int)
 * Math.pow (_base.max (), min_power) < min) { min_power ++; } _power.setMin
 * (min_power); }
 *  } else { if (((int) Math.pow (_base.max (), _power.min ())) < min) { int
 * min_power; min_power = (int) (Math.log (min) / Math.log (_base.max ())); if
 * ((int) Math.pow (_base.max (), min_power) < min) { min_power ++; }
 * _power.setMin (min_power); } if (((int) Math.pow (_base.min (), _power.max
 * ())) < min) { int min_base; min_base = (int) (Math.pow (min, 1.0 /((double)
 * _power.max ()))); if ((int) Math.pow (min_base, _power.max ()) < min) {
 * min_base ++; } _base.setMin (min_base); } } }
 */
