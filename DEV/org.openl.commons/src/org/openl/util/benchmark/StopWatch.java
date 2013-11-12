package org.openl.util.benchmark;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class StopWatch {

    public StopWatch(String name) {
        this.name = name;
        this.start = System.nanoTime();
    }

    public void end() {
        this.end = System.nanoTime();
    }

    String name;
    long start, end;

    static ThreadLocal<Stack<StopWatch>> stack = new ThreadLocal<Stack<StopWatch>>() {

        @Override
        protected Stack<StopWatch> initialValue() {
            return new Stack<StopWatch>();
        }

    };

    static List<StopWatch> all = new ArrayList<StopWatch>();

    public static long start(String name) {
        stack.get().push(new StopWatch(name + '-' + Thread.currentThread().getName()));
        return stack.get().peek().start;
    }

    public static long end(String name, boolean print) {
        StopWatch last = stack.get().pop();
        if (!last.name.equals(name + '-' + Thread.currentThread().getName())) {
            throw new RuntimeException("StopWatch " + name + " != " + last.name);
        }

        last.end();

        if (print)
            System.out.println(last);

        synchronized (all) {
            all.add(last);
        }

        return last.end;
    }

    public static void printAll() {
        for (StopWatch sw : all) {
            System.out.println(sw + "\n");
        }
    }

    static DecimalFormat f = new DecimalFormat("#,###");

    @Override
    public String toString() {
        long diff = end - start;

        return name + " : " + f.format(diff);
    }

}
