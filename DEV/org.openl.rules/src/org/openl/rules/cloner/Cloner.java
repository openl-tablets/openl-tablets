package org.openl.rules.cloner;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.EnumMap;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import groovy.lang.MetaObjectProtocol;
import org.slf4j.Logger;

import org.openl.rules.calc.AnySpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.StubSpreadSheetResult;
import org.openl.rules.helpers.CharRange;
import org.openl.rules.helpers.DateRange;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.helpers.StringRange;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;

/**
 * The cloner is used to deep cloning of Java Beans, Java collections and arrays. There was not a goal to do
 * the universal solution to cover all possible cases. It was inspired to create alternative which will work
 * on Java 17+ without illegal reflective access to the closed fields.
 *
 * @author Yury Molchan
 */
public class Cloner {
    private static final Set<Object> constants = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Class<?>> immutable = new HashSet<>();
    private static final Set<Class<?>> doNotClone = new HashSet<>();
    private static final Map<Class<?>, ICloner<?>> cloners = new HashMap<>();
    private static final Map<Class<?>, WeakReference<ICloner<?>>> cache = new WeakHashMap<>();

    static {
        constants.add(Collections.emptySet());
        constants.add(Collections.emptyNavigableSet());
        constants.add(Collections.emptySortedSet());

        constants.add(Collections.emptyMap());
        constants.add(Collections.emptyNavigableMap());
        constants.add(Collections.emptySortedMap());

        constants.add(Collections.emptyList());
        constants.add(Collections.emptyEnumeration());
        constants.add(Collections.emptyIterator());
        constants.add(Collections.emptyListIterator());

        immutable.add(Void.class);
        immutable.add(String.class);
        immutable.add(Double.class);
        immutable.add(Integer.class);
        immutable.add(Long.class);
        immutable.add(Boolean.class);
        immutable.add(BigDecimal.class);
        immutable.add(BigInteger.class);
        immutable.add(Character.class);
        immutable.add(Byte.class);
        immutable.add(Short.class);
        immutable.add(Float.class);
        immutable.add(LocalDate.class);
        immutable.add(LocalTime.class);
        immutable.add(LocalDateTime.class);
        immutable.add(ZonedDateTime.class);
        immutable.add(OffsetDateTime.class);
        immutable.add(OffsetTime.class);
        immutable.add(Duration.class);
        immutable.add(Instant.class);
        immutable.add(Period.class);
        immutable.add(Locale.class);
        immutable.add(UUID.class);
        immutable.add(URI.class);
        immutable.add(URL.class);
        immutable.add(Year.class);
        immutable.add(Month.class);
        immutable.add(YearMonth.class);
        immutable.add(MonthDay.class);
        immutable.add(DayOfWeek.class);
        immutable.add(Class.class);
        immutable.add(Pattern.class);

        immutable.add(CharRange.class);
        immutable.add(DateRange.class);
        immutable.add(IntRange.class);
        immutable.add(DoubleRange.class);
        immutable.add(StringRange.class);

        doNotClone.add(Path.class);
        doNotClone.add(Enum.class);
        doNotClone.add(MetaObjectProtocol.class);
        doNotClone.add(IOpenClass.class);
        doNotClone.add(IOpenMember.class);
        doNotClone.add(InvocationHandler.class);
        doNotClone.add(ILogicalTable.class);
        doNotClone.add(Logger.class);
    }

    static {
        cloners.put(GregorianCalendar.class, ICloner.<GregorianCalendar>create(x -> {
            var result = new GregorianCalendar((TimeZone) x.getTimeZone().clone());
            result.setTimeInMillis(x.getTimeInMillis());
            return result;
        }));

        cloners.put(Date.class, ICloner.<Date>create(x1 -> new Date(x1.getTime())));
        cloners.put(Time.class, ICloner.<Date>create(x1 -> new Time(x1.getTime())));
        cloners.put(Timestamp.class, ICloner.<Date>create(x1 -> new Timestamp(x1.getTime())));
        cloners.put(java.sql.Date.class, ICloner.<Date>create(x1 -> new java.sql.Date(x1.getTime())));

        var listCloner = CollectionCloner.<Collection<Object>>create(x -> new ArrayList<>(x.size()));
        var linkedListCloner = CollectionCloner.<Deque<Object>>create(x -> new LinkedList<>());
        var setCloner = CollectionCloner.<Set<Object>>create(x -> new HashSet<>(x.size()));
        var linkedHashSetCloner = CollectionCloner.<Set<Object>>create(x -> new LinkedHashSet<>(x.size()));
        var treeSetCloner = CollectionCloner.<SortedSet<Object>>create(x -> new TreeSet<>(x.comparator()));
        var mapCloner = MapCloner.<Map<Object, Object>, HashMap<Object, Object>>create(x -> new HashMap<>(x.size()));
        var linkedMapCloner = MapCloner.<Map<Object, Object>, LinkedHashMap<Object, Object>>create(x -> new LinkedHashMap<>(x.size()));
        var treeMapCloner = MapCloner.<SortedMap<Object, Object>, TreeMap<Object, Object>>create(x -> new TreeMap<>(x.comparator()));

        cloners.put(ArrayList.class, listCloner);
        cloners.put(LinkedList.class, linkedListCloner);
        cloners.put(HashSet.class, setCloner);
        cloners.put(HashMap.class, mapCloner);
        cloners.put(TreeMap.class, treeMapCloner);
        cloners.put(TreeSet.class, treeSetCloner);
        cloners.put(LinkedHashMap.class, linkedMapCloner);
        cloners.put(ConcurrentHashMap.class, MapCloner.create(x -> new ConcurrentHashMap<>(x.size())));
        cloners.put(ConcurrentLinkedQueue.class, CollectionCloner.create(x -> new ConcurrentLinkedQueue<>()));
        cloners.put(EnumMap.class, MapCloner.<EnumMap, EnumMap>create(x -> new EnumMap<>(x)));
        cloners.put(LinkedHashSet.class, linkedHashSetCloner);
        cloners.put(SpreadsheetResult.class, ICloner.<SpreadsheetResult>create(x -> {
            var result = new SpreadsheetResult(x);
            result.setResults(clone(x.getResults()));
            return result;
        }));
        cloners.put(StubSpreadSheetResult.class, ICloner.doNotClone);
        cloners.put(AnySpreadsheetResult.class, ICloner.doNotClone);

        // register private classes
        registerCloner("java.util.AbstractList$SubList", listCloner);
        registerCloner("java.util.ArrayList$SubList", listCloner);
        registerCloner("java.util.ImmutableCollections$List12", listCloner);
        registerCloner("java.util.ImmutableCollections$ListN", listCloner);
        registerCloner("java.util.ImmutableCollections$Set12", setCloner);
        registerCloner("java.util.ImmutableCollections$SetN", setCloner);
        registerCloner("java.util.ImmutableCollections$Map1", mapCloner);
        registerCloner("java.util.ImmutableCollections$MapN", mapCloner);

        registerCloner("java.util.Collections$UnmodifiableCollection", UnmodifiableCloner.create(Collections::unmodifiableCollection, listCloner));
        registerCloner("java.util.Collections$UnmodifiableRandomAccessList", UnmodifiableCloner.<List<Object>>create(Collections::unmodifiableList, listCloner));
        registerCloner("java.util.Collections$UnmodifiableSet", UnmodifiableCloner.<Set<Object>>create(Collections::unmodifiableSet, setCloner));
        registerCloner("java.util.Collections$UnmodifiableNavigableSet", UnmodifiableCloner.<NavigableSet<Object>>create(Collections::unmodifiableNavigableSet, treeSetCloner));
        registerCloner("java.util.Collections$UnmodifiableSortedSet", UnmodifiableCloner.<SortedSet<Object>>create(Collections::unmodifiableSortedSet, treeSetCloner));
        registerCloner("java.util.Collections$UnmodifiableMap", UnmodifiableCloner.create(Collections::unmodifiableMap, mapCloner));
        registerCloner("java.util.Collections$UnmodifiableNavigableMap", UnmodifiableCloner.<NavigableMap<Object, Object>>create(Collections::unmodifiableNavigableMap, treeMapCloner));
        registerCloner("java.util.Collections$UnmodifiableSortedMap", UnmodifiableCloner.<SortedMap<Object, Object>>create(Collections::unmodifiableSortedMap, treeMapCloner));
    }

    private static void registerCloner(String privateClass, ICloner<?> fastCloner) {
        try {
            ClassLoader classLoader = Cloner.class.getClassLoader();
            Class<?> subListClz = classLoader.loadClass(privateClass);
            cloners.put(subListClz, fastCloner);
        } catch (ClassNotFoundException ignore) {
            // ignore, seems a jdk does not have mentioned private class
        }
    }

    private static boolean skipClone(Class<?> clazz) {
        return clazz.isPrimitive() || immutable.contains(clazz) || skipCloneInstanceOf(clazz);
    }

    private static boolean skipCloneInstanceOf(Class<?> clazz) {
        for (Class<?> clz : doNotClone) {
            if (clz.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    public static <T> T clone(final T source) {
        if (source == null) {
            return null;
        }
        Map<Object, Object> clones = new IdentityHashMap<>();
        return clone(source, clones);
    }

    public static <T> T clone(T source, Map<Object, Object> clones) {
        if (source == null || constants.contains(source)) {
            return source;
        }

        Class<?> clazz = source.getClass();
        if (skipClone(clazz) || clazz == Object.class || clazz.isArray() && Array.getLength(source) == 0) {
            return source;
        }

        // do not clone the same instance again
        var clone = clones.get(source);
        if (clone != null) {
            return (T) clone;
        }

        var cloner = getCloner(clazz);
        Object instance = cloner.getInstance(source);
        Object target = instance;
        if (instance instanceof Wrapper) {
            var w = (Wrapper) instance;
            target = w.target;
            instance = w.unmodifiable;
        }
        clones.put(source, instance);
        cloner.clone(source, x -> clone(x, clones), target);
        return (T) instance;
    }

    private static <T> ICloner getCloner(Class<T> clazz) {
        ICloner<?> cloner;
        if (clazz.isArray()) {
            cloner = skipClone(clazz.getComponentType()) ? ArrayImmutableCloner.theInstance : ArrayCloner.theInstance;
        } else {
            cloner = cloners.get(clazz);
        }

        if (cloner != null) {
            return cloner;
        }

        var ref = cache.get(clazz);
        if (ref != null) {
            cloner = ref.get();
            if (cloner != null) {
                return cloner;
            }
        }

        cloner = new BeanCloner<>(clazz);
        synchronized (cache) {
            // there is no problem if multiple instances of the same cloner will be instantiated and placed at the same time.
            cache.put(clazz, new WeakReference<>(cloner));
        }
        return cloner;
    }
}
