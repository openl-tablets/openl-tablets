package org.openl.rules.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets the type of the array a rules function returns from the types of its arguments. A function applied to a
 * primitive array, such as {@code int[]}, returns a primitive array without a primitive overload.
 * <p>
 * The function takes the array as its first parameter. Separate values passed instead of the array count as an array
 * of their closest common type: {@code sort(3, 1, 2)} sorts an {@code int[]}.
 *
 * @author Yury Molchan
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ArrayResultType {

    Kind value();

    /**
     * How the type of the result follows from the arguments.
     */
    enum Kind {
        /**
         * The type of the array: {@code int[]} gives {@code int[]}.
         */
        SAME,

        /**
         * The closest common type of the array elements and the elements the last parameter adds to them:
         * {@code int[]} with an {@code int} gives {@code int[]}, with an {@code Integer} or an empty value
         * {@code Integer[]}, and with a {@code double} {@code double[]}. A boxed type among them keeps the result
         * boxed, so that an empty element keeps its place.
         */
        WIDENED
    }
}
