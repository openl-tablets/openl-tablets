package org.openl.meta.number;


/**
 * Common type for all number values. There are 3 kinds of values:
 * <li>1) represented as a single value.</li>
 * <li>2) represented as a result of some formula. see {@link NumberFormula}</li>
 * <li>3) represented as a result of some function. see {@link NumberFunction}</li> 
 * 
 * @author DLiauchuk
 *
 * @param <T> type that extends {@link NumberValue}
 */
public abstract class NumberValue<T extends NumberValue<T>> extends Number implements Comparable<Number> {
    
    private static final long serialVersionUID = -1260393051446603330L;    
    
    private NumberFormula<T> formula;
    
    private NumberFunction<T> function;
    
    private NumberCast cast;

    /** Single value constructor **/
    public NumberValue() { 
    }
    
    /** Formula value constructor **/ 
    public NumberValue(NumberFormula<T> formula) {
        this.formula = formula;
    }
    
    /** Function value constructor **/
    public NumberValue(NumberFunction<T> function) {
        this.function = function;
    }
    
    /** Casting operation constructor **/ 
    public NumberValue(NumberCast cast) {
        this.cast = cast;
    }

    /**
     * 
     * @return formula for current value.
     */
    public NumberFormula<T> getFormula() {
        return formula;
    }    
    
    /**
     * 
     * @return function for current value.
     */
    public NumberFunction<T> getFunction() {
        return function;
    }
    
    public NumberCast getCast() {
        return cast;
    }
}
