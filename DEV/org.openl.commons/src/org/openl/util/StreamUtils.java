package org.openl.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

    /**
     * Transform iterator to stream.
     *
     * @param iterator iterator to transform
     * @param <T>      type of elements
     * @return stream
     */
    public static <T> Stream<T> fromIterator(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    /**
     * Concatenates three {@link Stream}s into a single {@link Stream}.
     *
     * <p>This method takes three input streams and returns a single stream that combines
     * all elements from the three streams in the order they are provided. The resulting
     * stream will first contain all elements from the first stream, followed by all elements
     * from the second stream, and finally all elements from the third stream.</p>
     *
     * <p><strong>Note:</strong> The resulting stream is lazy and elements are not read
     * from the input streams until they are consumed from the resulting stream.</p>
     *
     * @param <T> the type of the elements in the streams
     * @param a the first stream
     * @param b the second stream
     * @param c the third stream
     * @return a new stream containing all elements from the three input streams in order
     * @throws NullPointerException if any of the input streams is {@code null}
     *
     * @see Stream#concat(Stream, Stream)
     */
    public static <T> Stream<T> concat(Stream<T> a, Stream<T> b, Stream<T> c) {
        return Stream.concat(Stream.concat(a, b), c);
    }
}
