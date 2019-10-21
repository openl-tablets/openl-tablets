package org.openl.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An util class for collections and arrays.
 *
 * @author Yury Molchan
 */
public class CollectionUtils {

    /**
     * Return {@code true} if a collection is null or is empty.
     *
     * @param col the checked collection.
     * @return return {@code true} if the collection does not contain any elements.
     * @see Collection#isEmpty()
     */
    public static boolean isEmpty(Collection<?> col) {
        return col == null || col.isEmpty();
    }

    /**
     * Return {@code true} if a collection contains at least one element. This method is inverse to
     * {@link #isEmpty(Collection)}.
     *
     * @param col the checked collection.
     * @return {@code true} if the collection contains at least one element.
     */
    public static boolean isNotEmpty(Collection<?> col) {
        return !isEmpty(col);
    }

    /**
     * Return {@code true} if a map is null or is empty.
     *
     * @param map the checked map.
     * @return return {@code true} if the map does not contain any elements.
     * @see Map#isEmpty()
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Return {@code true} if a map contains at least one element. This method is inverse to {@link #isEmpty(Map)}.
     *
     * @param map the checked map.
     * @return {@code true} if the map contains at least one element.
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * Return {@code true} if an array is null or is empty.
     *
     * @param array the checked array.
     * @return return {@code true} if the array does not contain any elements.
     * @see T[].length
     */
    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Return {@code true} if an array contains at least one element. This method is inverse to {@link #isEmpty(T[])}.
     *
     * @param array the checked array.
     * @return {@code true} if the array contains at least one element.
     */
    public static <T> boolean isNotEmpty(T[] array) {
        return !isEmpty(array);
    }

    /**
     * Return {@code true} if an array is null or is empty.
     *
     * @param array the checked array.
     * @return return {@code true} if the array does not contain any elements.
     * @see Array#getLength(Object)
     * @throws IllegalArgumentException if the argument is not an array.
     */
    public static <T> boolean isEmpty(T array) {
        return array == null || Array.getLength(array) == 0;
    }

    /**
     * Return {@code true} if an array contains at least one element. This method is inverse to {@link #isEmpty(T[])}.
     *
     * @param array the checked array.
     * @return {@code true} if the array contains at least one element.
     * @throws IllegalArgumentException if the argument is not an array.
     */
    public static <T> boolean isNotEmpty(T array) {
        return !isEmpty(array);
    }

    /**
     * Returns a new Collection consisting of the elements of the input collection transformed by the given transformer.
     * <p>
     *
     * @param <I> the type of object in the input collection.
     * @param <O> the type of object in the output collection.
     * @param col the collection to get the input from, may be null.
     * @param mapper the mapper to use.
     * @return the transformed result (new list).
     * @throws NullPointerException if the mapper is null.
     */
    public static <I, O> List<O> map(Iterable<I> col, Mapper<? super I, ? extends O> mapper) {
        if (col == null) {
            return null;
        }
        Objects.requireNonNull(mapper, "mapper cannot be null");
        int size = (col instanceof Collection) ? ((Collection) col).size() : 0;
        ArrayList<O> result = new ArrayList<>(size);
        for (I input : col) {
            O output = mapper.map(input);
            result.add(output);
        }
        return result;
    }

    /**
     * Finds the first element in the given collection which matches the given predicate.
     * <p>
     *
     * @param <T> the type of object the {@link Iterable} contains.
     * @param col the collection to search, may be null.
     * @param predicate the predicate to use.
     * @return the first element of the collection which matches the predicate or null if none could be found.
     * @throws NullPointerException if the predicate is null.
     */
    public static <T> T findFirst(Iterable<T> col, Predicate<? super T> predicate) {
        if (col == null) {
            return null;
        }
        Objects.requireNonNull(predicate, "predicate cannot be null");
        for (final T item : col) {
            if (predicate.evaluate(item)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Selects all elements from input collection which match the given predicate into an output collection.
     * <p>
     *
     * @param <T> the type of object the {@link Iterable} contains.
     * @param col the collection to search, may be null.
     * @param predicate the predicate to use.
     * @return the all the elements of the collection which matches the predicate or [] if none could be found or null
     *         if the input collection is null.
     * @throws NullPointerException if the predicate is null.
     */
    public static <T> List<T> findAll(Iterable<T> col, Predicate<? super T> predicate) {
        if (col == null) {
            return null;
        }
        Objects.requireNonNull(predicate, "predicate cannot be null");

        int size = (col instanceof Collection) ? ((Collection) col).size() : 0;
        ArrayList<T> result = new ArrayList<>(size);
        for (final T item : col) {
            if (predicate.evaluate(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Checks an array on {@code null} value.
     *
     * @param array the checked array.
     * @param <T> the element type of checked array.
     * @return {@code true} if the array contains {@code null}.
     */
    public static <T> boolean hasNull(T[] array) {
        for (T item : array) {
            if (item == null) {
                return true;
            }
        }
        return false;
    }

    public static Object toArray(Collection<?> list, Class<?> instanceClass) {
        if (list == null) {
            return null;
        }
        Object result = Array.newInstance(instanceClass, list.size());
        int i = 0;
        for (Object el : list) {
            Array.set(result, i, el);
            i++;
        }
        return result;
    }

    /**
     * Defines a functor interface implemented by classes that map one object into another.
     * <p>
     * A <code>Mapper</code> converts the input object to the output object. The input object should be left unchanged.
     *
     * @param <I> the input type to the mapper.
     * @param <O> the output type from the mapper.
     */
    public interface Mapper<I, O> {

        /**
         * Maps the input object (leaving it unchanged) into some output object.
         *
         * @param input the object to be mapped, should be left unchanged.
         * @return a mapped object.
         */
        O map(I input);

    }

    /**
     * Defines a functor interface implemented by classes that perform a predicate test on an object.
     * <p>
     * A <code>Predicate</code> is the object equivalent of an <code>if</code> statement. It uses the input object to
     * return a true or false value, and is often used in validation or filtering.
     * <p>
     *
     * @param <T> the type that the predicate queries.
     */
    public interface Predicate<T> {

        /**
         * Use the specified parameter to perform a test that returns true or false.
         *
         * @param object the object to evaluate, should not be changed.
         * @return true or false.
         */
        boolean evaluate(T object);

    }
}
