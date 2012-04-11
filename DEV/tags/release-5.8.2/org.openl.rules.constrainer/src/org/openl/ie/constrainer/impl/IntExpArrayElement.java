package org.openl.ie.constrainer.impl;

import java.util.LinkedList;
import java.util.ListIterator;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;


/**
 * Title: Integer expression array element with index specified by integer
 * expression. Description: class Copyright: (c) 2002 Company: Exigen Group 2002
 *
 * @author Sergey Vanskov
 * @version 1.0
 */

public class IntExpArrayElement extends IntExpImpl {
    private class ArrayElementAndObserver {
        public int _index;

        public IntExp _exp;
        public Observer _observer;
        public ArrayElementAndObserver(IntExp exp, int index, Observer observer) {
            _index = index;
            _exp = exp;
            _observer = observer;
        }
    }
    private class ArrayElementObserver extends Observer {
        private IntExp _exp;

        public ArrayElementObserver(IntExp exp) {
            _exp = exp;
        }

        @Override
        public Object master() {
            return IntExpArrayElement.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX | VALUE | REMOVE;
        }

        @Override
        public void update(Subject subject, EventOfInterest event) throws Failure {
            IntEvent i_event = (IntEvent) event;

            if (i_event.min() > _result.min()) {
                _result.setMin(calc_min());
            }
            if (i_event.max() < _result.max()) {
                _result.setMax(calc_max());
            }
        }
    }
    private class IndexObserver extends Observer {
        IntExp _index;
        LinkedList _elements;

        public IndexObserver(LinkedList elements, IntExp index) {
            _elements = elements;
            _index = index;
        }

        @Override
        public Object master() {
            return IntExpArrayElement.this;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX | VALUE | REMOVE;
        }

        @Override
        public void update(Subject subject, EventOfInterest event) throws Failure {
            IntEvent i_event = (IntEvent) event;
            switch (event.type()) {
                case MIN: {
                    int min = i_event.min();
                    ArrayElementAndObserver element_and_observer;

                    while (!_elements.isEmpty()) {
                        element_and_observer = (ArrayElementAndObserver) _elements.getFirst();
                        if (element_and_observer._index < min) {
                            element_and_observer._exp.detachObserver(element_and_observer._observer);
                            _elements.removeFirst();
                        } else {
                            break;
                        }
                    }
                    updateMinMax();
                }
                    break;
                case MAX: {
                    int max = i_event.max();
                    ArrayElementAndObserver element_and_observer;

                    while (!_elements.isEmpty()) {
                        element_and_observer = (ArrayElementAndObserver) _elements.getLast();
                        if (element_and_observer._index > max) {
                            element_and_observer._exp.detachObserver(element_and_observer._observer);
                            _elements.removeLast();
                        } else {
                            break;
                        }
                    }
                    updateMinMax();
                }
                    break;
                case VALUE:
                case MINMAX: {
                    int min = i_event.min();
                    ArrayElementAndObserver element_and_observer;

                    while (!_elements.isEmpty()) {
                        element_and_observer = (ArrayElementAndObserver) _elements.getFirst();
                        if (element_and_observer._index < min) {
                            element_and_observer._exp.detachObserver(element_and_observer._observer);
                            _elements.removeFirst();
                        } else {
                            break;
                        }
                    }

                    int max = i_event.max();

                    while (!_elements.isEmpty()) {
                        element_and_observer = (ArrayElementAndObserver) _elements.getLast();
                        if (element_and_observer._index > max) {
                            element_and_observer._exp.detachObserver(element_and_observer._observer);
                            _elements.removeLast();
                        } else {
                            break;
                        }
                    }

                    updateMinMax();
                }
                    break;
                case REMOVE: {
                    for (int i = 0; i < i_event.numberOfRemoves(); i++) {
                        int rem = i_event.removed(i);
                        ArrayElementAndObserver element_and_observer;
                        ListIterator iterator = _elements.listIterator();
                        while (iterator.hasNext()) {
                            element_and_observer = (ArrayElementAndObserver) iterator.next();
                            if (element_and_observer._index == rem) {
                                element_and_observer._exp.detachObserver(element_and_observer._observer);
                                _elements.remove(element_and_observer);
                                break;
                            }
                        }
                    }
                    updateMinMax();
                }
                    break;
            }
            if (_elements.size() == 1) {
                ArrayElementAndObserver element_and_observer;
                element_and_observer = (ArrayElementAndObserver) _elements.getFirst();
                element_and_observer._exp.setMin(_result.min());
                element_and_observer._exp.setMax(_result.max());
            }
        }

        private void updateMinMax() throws Failure {
            _result.setMin(calc_min());
            _result.setMax(calc_max());
        }
    }
    private LinkedList _elements;

    private IntExp _index;

    private Observer _index_observer;

    private IntVar _result;

    public IntExpArrayElement(IntExpArray array, IntExp index) throws Failure {
        super(index.constrainer());

        if (constrainer().showInternalNames()) {
            name(array.name() + "[" + index.name() + "]");
        }
        _index = index;
        _index.setMin(0);
        _index.setMax(array.size() - 1);
        _elements = new LinkedList();

        int min = index.min();
        int max = index.max();
        IntExp element;
        ArrayElementObserver observer;

        for (int i = min; i <= max; i++) {
            element = array.get(i);
            observer = new ArrayElementObserver(element);
            element.attachObserver(observer);
            _elements.add(new ArrayElementAndObserver(element, i, observer));
        }

        _index_observer = new IndexObserver(_elements, _index);
        _index.attachObserver(_index_observer);

        int trace = 0;
        _result = constrainer().addIntVarTraceInternal(calc_min(), calc_max(),
                array.name() + "[" + index.name() + "](internal)", IntVar.DOMAIN_PLAIN, trace);
    }

    public int calc_max() {
        if (_elements.size() == 1) {
            return (((ArrayElementAndObserver) _elements.getFirst())._exp).max();
        }
        ListIterator iterator = _elements.listIterator();
        int l_max = Constrainer.INT_MIN;
        int e_max;

        while (iterator.hasNext()) {
            e_max = (((ArrayElementAndObserver) iterator.next())._exp).max();
            if (e_max > l_max) {
                l_max = e_max;
            }
        }
        return l_max;
    }

    public int calc_min() {
        if (_elements.size() == 1) {
            return (((ArrayElementAndObserver) _elements.getFirst())._exp).min();
        }
        ListIterator iterator = _elements.listIterator();
        int l_min = Constrainer.INT_MAX;
        int e_min;

        while (iterator.hasNext()) {
            e_min = (((ArrayElementAndObserver) iterator.next())._exp).min();
            if (e_min < l_min) {
                l_min = e_min;
            }
        }
        return l_min;
    }

    public int max() {
        return _result.max();
    }

    public int min() {
        return _result.min();
    }

    public void setMax(int max) throws Failure {
        _result.setMax(max);
        if (_elements.size() == 1) {
            ((IntExp) _elements.getFirst()).setMax(max);
        }
    }

    public void setMin(int min) throws Failure {
        _result.setMin(min);
        if (_elements.size() == 1) {
            ((IntExp) _elements.getFirst()).setMin(min);
        }
    }
}