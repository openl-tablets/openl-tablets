package org.openl.rules.cloner;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

class ClonerTest {

    @Test
    void testCloneImmutableObjects() {
        assertNotCloned("immutable string");
        assertNotCloned(1);
        assertNotCloned(2L);
        assertNotCloned(3.0);
        assertNotCloned(4.0f);
        assertNotCloned(false);
        assertNotCloned(null);
        assertNotCloned(new BigDecimal("123.45"));
        assertNotCloned(new BigInteger("12345"));
        assertNotCloned(LocalDate.now());
        assertNotCloned(LocalDateTime.now());
        assertNotCloned(LocalTime.now());
        assertNotCloned(ZonedDateTime.now());
        assertNotCloned(OffsetDateTime.now());
        assertNotCloned(OffsetTime.now());
        assertNotCloned(Instant.now());
        assertNotCloned(Duration.ofDays(2));
        assertNotCloned(Period.ofDays(3));
        assertNotCloned(UUID.randomUUID());
        assertNotCloned(Year.now());
        assertNotCloned(Month.of(7));
        assertNotCloned(YearMonth.now());
        assertNotCloned(MonthDay.now());
        assertNotCloned(DayOfWeek.of(4));
        assertNotCloned(Pattern.compile(".+"));
        assertNotCloned(File.class);
        assertNotCloned(new Object());

        assertNotCloned(Path.of("/some/path"));
        assertNotCloned(RoundingMode.FLOOR);
    }

    @Test
    void testCloneEnumMap() {
        EnumMap<DayOfWeek, String> enumMap = new EnumMap<>(DayOfWeek.class);
        enumMap.put(DayOfWeek.MONDAY, "Monday");
        assertCloned(enumMap);
    }

    @Test
    void testCloneCollections() {
        assertCloned(List.of());
        assertCloned(List.of("one"));
        assertCloned(List.of("one", "two"));
        assertCloned(List.of("one", "two", "three"));

        assertCloned(new ArrayList<>(List.of()));
        assertCloned(new ArrayList<>(List.of("one")));
        assertCloned(new ArrayList<>(List.of("one", "two")));
        assertCloned(new ArrayList<>(List.of("one", "two", "three")));

        assertCloned(new LinkedList<>(List.of()));
        assertCloned(new LinkedList<>(List.of("one")));
        assertCloned(new LinkedList<>(List.of("one", "two")));
        assertCloned(new LinkedList<>(List.of("one", "two", "three")));

        assertCloned(Set.of());
        assertCloned(Set.of(1));
        assertCloned(Set.of(1, 2));
        assertCloned(Set.of(1, 2, 3));

        assertCloned(new HashSet<>(Set.of()));
        assertCloned(new HashSet<>(Set.of(1)));
        assertCloned(new HashSet<>(Set.of(1, 2)));
        assertCloned(new HashSet<>(Set.of(1, 2, 3)));

        assertCloned(new LinkedHashSet<>(Set.of()));
        assertCloned(new LinkedHashSet<>(Set.of(1)));
        assertCloned(new LinkedHashSet<>(Set.of(1, 2)));
        assertCloned(new LinkedHashSet<>(Set.of(1, 2, 3)));

        assertCloned(new TreeSet<>(Set.of()));
        assertCloned(new TreeSet<>(Set.of(1)));
        assertCloned(new TreeSet<>(Set.of(1, 2)));
        assertCloned(new TreeSet<>(Set.of(1, 2, 3)));

        assertCloned(Map.of());
        assertCloned(Map.of("one", 1));
        assertCloned(Map.of("one", 1, "two", 2));
        assertCloned(Map.of("one", 1, "two", 2, "three", 3));

        assertCloned(new HashMap<>(Map.of()));
        assertCloned(new HashMap<>(Map.of("one", 1)));
        assertCloned(new HashMap<>(Map.of("one", 1, "two", 2)));
        assertCloned(new HashMap<>(Map.of("one", 1, "two", 2, "three", 3)));

        assertCloned(new LinkedHashMap<>(Map.of()));
        assertCloned(new LinkedHashMap<>(Map.of("one", 1)));
        assertCloned(new LinkedHashMap<>(Map.of("one", 1, "two", 2)));
        assertCloned(new LinkedHashMap<>(Map.of("one", 1, "two", 2, "three", 3)));

        assertCloned(new ConcurrentHashMap<>(Map.of()));
        assertCloned(new ConcurrentHashMap<>(Map.of("one", 1)));
        assertCloned(new ConcurrentHashMap<>(Map.of("one", 1, "two", 2)));
        assertCloned(new ConcurrentHashMap<>(Map.of("one", 1, "two", 2, "three", 3)));

        assertCloned(new TreeMap<>(Map.of()));
        assertCloned(new TreeMap<>(Map.of("one", 1)));
        assertCloned(new TreeMap<>(Map.of("one", 1, "two", 2)));
        assertCloned(new TreeMap<>(Map.of("one", 1, "two", 2, "three", 3)));
    }

    @Test
    void testCloneCustomObjects() {
        assertCloned(new GregorianCalendar());
        assertCloned(new Date());
        assertCloned(new Timestamp(300));
        assertCloned(new Time(200));
        assertCloned(new java.sql.Date(100));
    }

    @Test
    void testCloneArrays() {
        assertNotCloned(new int[0]);
        assertCloned(new int[]{1});
        assertCloned(new int[]{1, 2});
        assertCloned(new int[]{1, 2, 3});
        assertCloned(new int[]{1, 2, 3, 4});

        assertNotCloned(new Integer[0]);
        assertCloned(new Integer[]{1});
        assertCloned(new Integer[]{1, 2});
        assertCloned(new Integer[]{1, 2, 3});
        assertCloned(new Integer[]{1, 2, 3, 4});

        assertNotCloned(new Integer[]{});
        assertCloned(new Integer[]{null});
        assertCloned(new Integer[]{null, null});
        assertCloned(new Integer[]{null, null, null});
        assertCloned(new Integer[]{null, 2, null, 4});

        assertNotCloned(new Date[]{});
        assertCloned(new Date[]{new Date(12)});
        assertCloned(new Date[]{new Date(23), new Date(44)});
        assertCloned(new Date[]{new Date(23), null, new Date(44)});
        assertCloned(new Date[]{null});

        assertNotCloned(new Object[0]);
        assertCloned(new Object[]{1});
        assertCloned(new Object[]{1, 2});
        assertCloned(new Object[]{1, 2, 3});
        assertCloned(new Object[]{1, 2, 3, 4});

        assertCloned(new Object[] {null});
        assertCloned(new Object[] {new Object()});
        assertCloned(new Object[] {new Object(), null, new Object()});
        assertCloned(new Object[] {new Object(), new Object(), new Object()});

        assertNotCloned(new Beans[0]);
        assertCloned(new Beans[1]);

        // Case 1
        Object[] arr1 = new Object[] {new Beans()};
        Object[] cloned1 = Cloner.clone(arr1);
        assertNotSame(arr1, cloned1);
        assertEquals(arr1.length, cloned1.length);
        assertNotSame(arr1[0], cloned1[0]); // Not immutable

        // Case 2
        Object[] arr2 = new Object[] {new Object()};
        Object[] cloned2 = Cloner.clone(arr2);
        assertNotSame(arr2, cloned2);
        assertEquals(arr2.length, cloned2.length);
        assertSame(arr2[0], cloned2[0]); // Immutable

    }

    @Test
    void testCloneWithNull() {
        assertNull(Cloner.clone(null));
    }

    @Test
    void testCloneWithCycles() {
        List<Object> list1 = new ArrayList<>();
        List<Object> list2 = new ArrayList<>();
        list1.add(list2);
        list2.add(list1);

        List<Object> clonedList1 = Cloner.clone(list1);
        List<Object> clonedList2 = (List<Object>) clonedList1.get(0);

        assertNotSame(list1, clonedList1);
        assertNotSame(list2, clonedList2);
        assertSame(clonedList1, clonedList2.get(0));
    }

    @Test
    public void testCloneOfSameObject() {
        var o1 = new Object();
        var o2 = new Object();

        var obj1 = new Fields();
        obj1.f1 = o1;
        obj1.f2 = o2;
        obj1.f3 = o1;
        obj1.f4 = o2;
        var clone = Cloner.clone(obj1);
        assertSame(clone.f1, clone.f3);
        assertSame(clone.f2, clone.f4);

        var h1 = Set.of(1);
        var h2 = Set.of(1);
        obj1.f1 = h1;
        obj1.f2 = h2;
        obj1.f3 = h1;
        obj1.f4 = h2;
        clone = Cloner.clone(obj1);
        assertSame(clone.f1, clone.f3);
        assertSame(clone.f2, clone.f4);
        assertNotSame(clone.f1, clone.f2);
        assertNotSame(clone.f3, clone.f4);
    }

    @Test
    public void testCloneBean() {

        var cache = new HashMap<>();
        var date = new Date(1234);
        var bean1 = new Beans();
        var bean2 = new Beans();
        bean1.setBean(bean2);
        bean2.setBean(bean1);

        bean1.setAge(10);
        bean2.setAge(20);
        bean1.setStr("100");
        bean2.setStr("200");
        bean1.setDate(date);
        bean2.setDate(date);

        var clone1 = Cloner.clone(bean1, cache);
        var clone2 = Cloner.clone(bean2, cache);
        assertNotSame(clone1, bean1);
        assertNotSame(clone2, bean2);

        assertSame(clone1, clone1.getBean().getBean());
        assertSame(clone1.getDate(), clone1.getBean().getDate());
        assertNotSame(bean1.getDate(), clone1.getBean().getDate());
        assertSame(bean1.getAge(), clone1.getAge());
        assertSame(bean1.getStr(), clone1.getStr());
        assertSame(bean2.getAge(), clone2.getAge());
        assertSame(bean2.getStr(), clone2.getStr());

        assertSame(clone1, clone2.getBean());
        assertSame(clone2, clone1.getBean());
    }

    private static void assertCloned(Object obj) {
        Object cloned = Cloner.clone(obj);
        assertNotSame(obj, cloned);
        assertEquals(obj, cloned);
    }

    private static void assertCloned(Object[] arr) {
        Object[] cloned = Cloner.clone(arr);
        assertNotSame(arr, cloned);
        assertArrayEquals(arr, cloned);
    }

    private static void assertCloned(int[] arr) {
        int[] cloned = Cloner.clone(arr);
        assertNotSame(arr, cloned);
        assertArrayEquals(arr, cloned);
    }

    private static void assertNotCloned(Object obj) {
        Object cloned = Cloner.clone(obj);
        assertSame(obj, cloned);
    }

    public static class Fields {
        public Object f1, f2, f3, f4;
    }

    public static class Beans {
        private String str;
        private Date date;
        private int age;
        private Beans bean;

        public String getStr() {
            return str;
        }

        public void setStr(String str) {
            this.str = str;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Beans getBean() {
            return bean;
        }

        public void setBean(Beans bean) {
            this.bean = bean;
        }
    }
}
