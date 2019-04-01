package org.openl.ie.constrainer.impl;

import java.util.HashSet;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Observer;
import org.openl.ie.constrainer.Subject;
import org.openl.ie.tools.FastVector;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company: Exigen Group, Inc.
 * </p>
 *
 * @author Sergej Vanskov
 * @version 1.0
 */

public class TestUtils {
    public static boolean contains(int[] sub, int n) {
        for (int i = 0; i < sub.length; ++i) {
            if (sub[i] == n) {
                return true;
            }
        }
        return false;
    }

    static public class Finder {
        private int[] _array;
        private int _start, _end;
        private IntFindPredicate _predicate = null;

        public Finder(int[] array, int start, int end, IntFindPredicate predicate) {
            _array = array;
            _start = start;
            _end = end;
            _predicate = predicate;
        }

        public Finder(int[] array, int start, IntFindPredicate predicate) {
            _array = array;
            _start = start;
            _end = _array.length - 1;
            _predicate = predicate;
        }

        public Finder(int[] array, IntFindPredicate predicate) {
            _array = array;
            _start = 0;
            _end = _array.length - 1;
            _predicate = predicate;
        }

        public int[] findAll() {
            int[] temp = new int[_end - _start + 1];
            int counter = 0;
            for (int i = _start; i <= _end; i++) {
                if (_predicate.isTrue(_array[i])) {
                    temp[counter++] = i;
                }
            }
            if (counter > 0) {
                int[] out = new int[counter];
                System.arraycopy(temp, 0, out, 0, counter);
                return out;
            }
            return null;
        }

        public int findFirst() {
            for (int i = _start; i <= _end; i++) {
                if (_predicate.isTrue(_array[i])) {
                    return i;
                }
            }
            return -1;
        }

        public int findFirstIf(IntFindPredicate predicate) {
            for (int i = _start; i <= _end; i++) {
                if (predicate.isTrue(_array[i])) {
                    return i;
                }
            }
            return -1;
        }
    } // end of Fider

    static public class IntEqualsTo implements IntFindPredicate {
        private int _value;

        public IntEqualsTo(int value) {
            _value = value;
        }

        @Override
        public boolean isTrue(int i) {
            return (i == _value);
        }
    }

    public interface IntFindPredicate {
        boolean isTrue(int i);
    }

    static public class IntGreaterThan implements IntFindPredicate {
        private int _value;

        public IntGreaterThan(int value) {
            _value = value;
        }

        @Override
        public boolean isTrue(int i) {
            return i > _value;
        }
    }

    public static class TestObserver extends Observer {
        private int counter = 0;

        @Override
        public Object master() {
            return null;
        }

        @Override
        public int subscriberMask() {
            return MIN | MAX | VALUE;
        }

        @Override
        public void update(Subject exp, EventOfInterest event) throws Failure {
            counter++;
        }

        public int updtCounter() {
            return counter;
        }
    } // end of TestObserver

    static public boolean contains(FastVector vec, Object obj) {
        Object[] objs = vec.data();
        HashSet set = new HashSet(objs.length + 10);
        for (int i = 0; i < objs.length; i++) {
            set.add(objs[i]);
        }
        return set.contains(obj);
    }

    static public boolean contains(Object[] objs, Object obj) {
        HashSet set = new HashSet(objs.length + 10);
        for (int i = 0; i < objs.length; i++) {
            set.add(objs[i]);
        }
        return set.contains(obj);
    }

    static public TestObserver createTestObserver() {
        return new TestObserver();
    }

    static public boolean isAllDiff(int[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                if (arr[i] == arr[j]) {
                    return false;
                }
            }
        }
        return true;
    }

    static public int max(int[] arr) {
        if ((arr == null) || (arr.length == 0)) {
            throw new IllegalArgumentException();
        }
        int max = -Integer.MAX_VALUE;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > max) {
                max = arr[i];
            }
        }
        return max;
    }

    static public int min(int[] arr) {
        if ((arr == null) || (arr.length == 0)) {
            throw new IllegalArgumentException();
        }
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] < min) {
                min = arr[i];
            }
        }
        return min;
    }

    static public int minGreaterThan(int[] array, int val) {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < array.length; i++) {
            if ((array[i] < min) && (array[i] > val)) {
                min = array[i];
            }
        }
        return min;
    }

    static public int[] subArray(int[] array, int start, int end) {
        int[] subarray = new int[end - start + 1];
        System.arraycopy(array, start, subarray, 0, subarray.length);
        return subarray;
    }

    private TestUtils() {
    }
}