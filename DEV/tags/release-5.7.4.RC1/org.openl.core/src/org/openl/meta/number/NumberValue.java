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
    
    private ValueType valueType;
    
    /** Single value constructor **/
    public NumberValue() { 
        this.valueType = ValueType.SINGLE_VALUE;
    }
    
    /** Formula value constructor **/ 
    public NumberValue(NumberFormula<T> formula) {
        this.valueType = ValueType.FORMULA;
        this.formula = formula;
    }
    
    /** Function value constructor **/
    public NumberValue(NumberFunction<T> function) {
        this.valueType = ValueType.FUNCTION;
        this.function = function;
    }
    
    /**
     * 
     * @return formula for current value if {@link #getValueType()} returns {@link ValueType#FORMULA}.
     * in other case null.
     */
    public NumberFormula<T> getFormula() {
        return formula;
    }    
    
    /**
     * 
     * @return function for current value if {@link #getValueType()} returns {@link ValueType#FUNCTION}.
     * in other case null.
     */
    public NumberFunction<T> getFunction() {
        return function;
    }
    
    /**
     * 
     * @return type of current value.
     */
    public ValueType getValueType() {
        return valueType;
    }
    
    /**
     * Enum of possible number values types.
     * 
     * @author DLiauchuk
     *
     */
    public enum ValueType {
        SINGLE_VALUE("value"),
        FORMULA("formula"),
        FUNCTION("function");
        
        private String name;
        
        private ValueType(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {        
            return name;
        }
    }
}
