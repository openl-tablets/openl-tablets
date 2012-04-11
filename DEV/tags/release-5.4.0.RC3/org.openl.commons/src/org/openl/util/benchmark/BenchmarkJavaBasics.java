/**
 * Created Jul 7, 2007
 */
package org.openl.util.benchmark;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.openl.util.IdMap;
import org.openl.util.IdObject;

/**
 * @author snshor
 *
 */
public class BenchmarkJavaBasics {

    static class BSearch extends BenchmarkUnit {

        static int N = 100;

        int[] buf;

        int key = 51;;

        BSearch() throws Exception {
            buf = new int[N];
            for (int i = 0; i < N; ++i) {
                buf[i] = i * 4;
            }
        }

        @Override
        protected void run() throws Exception {
            Arrays.binarySearch(buf, key);
        }

    }

    static class Call extends BenchmarkUnit {
        BenchmarkJavaBasics bjb = new BenchmarkJavaBasics();

        Call() throws Exception {
        }

        @Override
        protected void run() throws Exception {
            bjb.add(10, 15);
        }
    }

    static class Empty extends BenchmarkUnit {
        @Override
        protected void run() throws Exception {
        }
    }

    static class IDMapGet extends BenchmarkUnit {
        IdMap map;

        int key = ("Xyz" + 37).hashCode();

        IDMapGet() throws Exception {
            map = new IdMap(107);
            for (int i = 0; i < 100; ++i) {
                map.add(new IdObject(("Xyz" + i).hashCode()));
            }
        }

        @Override
        protected void run() throws Exception {
            map.get(key);
        }
    }

    static class Invoke extends BenchmarkUnit {
        Method m = BenchmarkJavaBasics.class.getMethod("add", new Class[] { int.class, int.class });

        Object[] params = { new Integer(10), new Integer(15) };
        Object bjb = new BenchmarkJavaBasics();
        Invoke() throws Exception {
        }

        @Override
        protected void run() throws Exception {
            m.invoke(bjb, params);
        }
    }

    static class MapGet extends BenchmarkUnit {
        HashMap<String, String> map;

        String key = "Xyz" + 37;

        MapGet() throws Exception {
            map = new HashMap<String, String>();
            for (int i = 0; i < 100; ++i) {
                map.put("Xyz" + i, "Xyz" + i);
            }
        }

        @Override
        protected void run() throws Exception {
            map.get(key);
        }
    }

    static class MapInternGet extends BenchmarkUnit {
        HashMap<String, String> map;

        String key = ("Xyz" + 37).intern();

        MapInternGet() throws Exception {
            map = new HashMap<String, String>();
            for (int i = 0; i < 100; ++i) {
                map.put(("Xyz" + i).intern(), "Xyz" + i);
            }
        }

        @Override
        protected void run() throws Exception {
            map.get(key);
        }
    }

    static class ThreadG extends BenchmarkUnit {
        @Override
        protected void run() throws Exception {
            BenchmarkJavaBasics.isTracerOn();
        }
    }

    static class TreeMapGet extends BenchmarkUnit {
        TreeMap<Integer, String> map;

        Integer key = new Integer(50);

        TreeMapGet() throws Exception {
            map = new TreeMap<Integer, String>();
            for (int i = 0; i < 100; ++i) {
                map.put(new Integer(i * 2), "Xyz" + i);
            }
        }

        @Override
        protected void run() throws Exception {
            map.get(key);
        }
    }

    static class TreeMapGetFirstKey extends BenchmarkUnit {
        TreeMap<Integer, String> map;

        Integer key = new Integer(51);

        TreeMapGetFirstKey() throws Exception {
            map = new TreeMap<Integer, String>();
            for (int i = 0; i < 100; ++i) {
                map.put(new Integer(i * 2), "Xyz" + i);
            }
        }

        @Override
        protected void run() throws Exception {
            Iterator<String> it = map.tailMap(key).values().iterator();
            if (it.hasNext()) {
                it.next();
            }
        }
    }

    static ThreadLocal<Object> tracer = new ThreadLocal<Object>();

    static public boolean isTracerOn() {
        return tracer.get() != null;
    }

    public static void main(String[] args) throws Exception {
        BenchmarkUnit[] bu = { new Empty(), new Call(), new Invoke(), new MapGet(), new MapInternGet(), new IDMapGet(),
                new ThreadG(), new TreeMapGet(), new TreeMapGetFirstKey(), new BSearch() };

        List<BenchmarkInfo> res = new Benchmark(bu).measureAllInList(1000);

        for (BenchmarkInfo bi: res) {
            

            System.out.println(bi);

        }
    }

    public int add(int x, int y) {
        return x + y;
    }

}
