package org.openl.ie.constrainer.impl;

import java.util.Arrays;
import java.util.HashSet;

import lombok.RequiredArgsConstructor;

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
        for (int j : sub) {
            if (j == n) {
                return true;
            }
        }
        return false;
    }

    @RequiredArgsConstructor
    static public class Finder {
        private final int[] _array;
        private final int _start;
        private final int _end;
        private final IntFindPredicate _predicate;

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
            var counter = 0;
            for (var i = _start; i <= _end; i++) {
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
            for (var i = _start; i <= _end; i++) {
                if (_predicate.isTrue(_array[i])) {
                    return i;
                }
            }
            return -1;
        }

        public int findFirstIf(IntFindPredicate predicate) {
            for (var i = _start; i <= _end; i++) {
                if (predicate.isTrue(_array[i])) {
                    return i;
                }
            }
            return -1;
        }
    } // end of Fider

    @RequiredArgsConstructor
    static public class IntEqualsTo implements IntFindPredicate {
        private final int _value;

        @Override
        public boolean isTrue(int i) {
            return i == _value;
        }
    }

    public interface IntFindPredicate {
        boolean isTrue(int i);
    }

    @RequiredArgsConstructor
    static public class IntGreaterThan implements IntFindPredicate {
        private final int _value;

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
        var objs = vec.data();
        var set = new HashSet(objs.length + 10);
        set.addAll(Arrays.asList(objs));
        return set.contains(obj);
    }

    static public boolean contains(Object[] objs, Object obj) {
        var set = new HashSet(objs.length + 10);
        set.addAll(Arrays.asList(objs));
        return set.contains(obj);
    }

    static public TestObserver createTestObserver() {
        return new TestObserver();
    }

    static public boolean isAllDiff(int[] arr) {
        for (var i = 0; i < arr.length - 1; i++) {
            for (var j = i + 1; j < arr.length; j++) {
                if (arr[i] == arr[j]) {
                    return false;
                }
            }
        }
        return true;
    }

    static public int max(int[] arr) {
        if (arr == null || arr.length == 0) {
            throw new IllegalArgumentException();
        }
        var max = -Integer.MAX_VALUE;
        for (int j : arr) {
            if (j > max) {
                max = j;
            }
        }
        return max;
    }

    static public int min(int[] arr) {
        if (arr == null || arr.length == 0) {
            throw new IllegalArgumentException();
        }
        var min = Integer.MAX_VALUE;
        for (int j : arr) {
            if (j < min) {
                min = j;
            }
        }
        return min;
    }

    static public int minGreaterThan(int[] array, int val) {
        var min = Integer.MAX_VALUE;
        for (int j : array) {
            if (j < min && j > val) {
                min = j;
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
