package org.openl.meta.number;

/**
 * Common type for all number values. There are 3 kinds of values:
 * <li>1) represented as a single value.</li>
 * <li>2) represented as a result of some formula. see {@link NumberFormula}</li>
 * <li>3) represented as a result of some function. see {@link org.openl.meta.explanation.FunctionExplanationValue}</li>
 * 
 * @author DLiauchuk
 *
 * @param <T> type that extends {@link NumberValue}
 */
public abstract class NumberValue<T extends NumberValue<T>> extends Number implements Comparable<Number> {
}
