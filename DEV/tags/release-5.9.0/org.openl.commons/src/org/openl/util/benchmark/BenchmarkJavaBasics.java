/**
 * Created Jul 7, 2007
 */
package org.openl.util.benchmark;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.openl.util.IdMap;
import org.openl.util.IdObject;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 * 
 */
public class BenchmarkJavaBasics {

    private static class BSearch extends BenchmarkUnit {

        private static int N = 100;

        private int[] buf;

        private int key = 51;;

        private BSearch() throws Exception {
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

    private static class Call extends BenchmarkUnit {
        private BenchmarkJavaBasics bjb = new BenchmarkJavaBasics();

        private Call() throws Exception {
        }

        @Override
        protected void run() throws Exception {
            bjb.add(10, 15);
        }
    }

    private static class Empty extends BenchmarkUnit {
        @Override
        protected void run() throws Exception {
        }
    }

    private static class IDMapGet extends BenchmarkUnit {
        private IdMap map;

        private int key = ("Xyz" + 37).hashCode();

        private IDMapGet() throws Exception {
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

    private static class Invoke extends BenchmarkUnit {
        private Method m = BenchmarkJavaBasics.class.getMethod("add", new Class[] { int.class, int.class });

        private Object[] params = { new Integer(10), new Integer(15) };
        private Object bjb = new BenchmarkJavaBasics();

        private Invoke() throws Exception {
        }

        @Override
        protected void run() throws Exception {
            m.invoke(bjb, params);
        }
    }

    private static class MapGet extends BenchmarkUnit {
        private HashMap<String, String> map;

        private String key = "Xyz" + 37;

        private MapGet() throws Exception {
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

    private static class MapInternGet extends BenchmarkUnit {
        private HashMap<String, String> map;

        private String key = ("Xyz" + 37).intern();

        private MapInternGet() throws Exception {
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
    
    private static class CurrentTimeMillis extends BenchmarkUnit {

        @Override
        protected void run() throws Exception {
            long abc = System.currentTimeMillis();
        }
        
    }
     
    private static class NanoTime extends BenchmarkUnit {

        @Override
        protected void run() throws Exception {
            long abc = System.nanoTime();
        }
        
    }

    private static class ThreadLocAccess extends BenchmarkUnit {
        @Override
        protected void run() throws Exception {
            BenchmarkJavaBasics.isTracerOn();
        }
    }

    private static class TreeMapGet extends BenchmarkUnit {
        private TreeMap<Integer, String> map;

        private Integer key = new Integer(50);

        private TreeMapGet() throws Exception {
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

    private static class TreeMapGetFirstKey extends BenchmarkUnit {
        private TreeMap<Integer, String> map;

        private Integer key = new Integer(51);

        private TreeMapGetFirstKey() throws Exception {
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

    
    private static class ConstructorDirect extends BenchmarkUnit {

        @Override
        protected void run() throws Exception {
            double ddd = 4.345;
            Double d = new Double(ddd);
        }
    }
    
    private static class ConstructorNewInstance extends BenchmarkUnit {

        Constructor<Double> ctr;

        ConstructorNewInstance()
        {
           try {
            ctr = Double.class.getConstructor(double.class);
        } catch (SecurityException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        } catch (NoSuchMethodException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        } 
        }
        
        @Override
        protected void run() throws Exception {
            double ddd = 4.345;
            Double d = ctr.newInstance(ddd);
        }
    }
    
    private static ThreadLocal<Object> tracer = new ThreadLocal<Object>();

    public static boolean isTracerOn() {
        return tracer.get() != null;
    }

    public static void main(String[] args) throws Exception {
        BenchmarkUnit[] bu = { new Empty(),
                new Call(),
                new Invoke(),
                new MapGet(),
                new MapInternGet(),
                new IDMapGet(),
                new ThreadLocAccess(),
                new TreeMapGet(),
                new TreeMapGetFirstKey(),
                new BSearch(), 
                new ConstructorDirect(),
                new ConstructorNewInstance(),
                new CurrentTimeMillis(),
                new NanoTime()
        
        };

        List<BenchmarkInfo> res = new Benchmark(bu).measureAllInList(1000);

        for (BenchmarkInfo bi : res) {

            System.out.println(bi);

        }
    }

    public int add(int x, int y) {
        return x + y;
    }

}
