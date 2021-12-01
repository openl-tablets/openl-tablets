package org.openl.util;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Set of utilities for Java Streams.
 *
 * @author Yury Molchan
 */
public class StreamUtils {

    /**
     * To use with Java Streams to collect in in a map preserving the order of elements.
     */
    public static <T, K, U> Collector<T, ?, Map<K, U>> toLinkedMap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends U> valueMapper) {
        return Collectors.toMap(keyMapper, valueMapper, (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        }, LinkedHashMap::new);
    }

    /**
     * To use with Java Streams to collect in tree set with custom order
     */
    public static <T> Collector<T, ?, Set<T>> toTreeSet(Comparator<? super T> comparator) {
        return Collectors.toCollection(() -> new TreeSet<>(comparator));
    }
}
