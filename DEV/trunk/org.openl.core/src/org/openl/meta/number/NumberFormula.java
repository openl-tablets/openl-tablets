package org.openl.meta.number;

import java.util.ArrayList;
import java.util.List;


/**
 * Formula abstraction over 2 numbers.<br>
 * Handles the information about formula.
 * 
 * @author DLiauchuk
 *
 * @param <T> type that extends {@link NumberValue}
 */
public class NumberFormula<T extends NumberValue<T>> {
    
    private NumberOperations operand;
    
    private T v1, v2;
    
    private boolean isMultiplicative;
    
    public NumberFormula(T v1, T v2, NumberOperations operand, boolean isMultiplicative) {
        this.v1 = v1;
        this.v2 = v2;        
        this.operand = operand;
        this.isMultiplicative = isMultiplicative;
    }
    
    /**
     * 
     * @return array of two formula arguments.
     */    
    public List<T> getArguments() {   
        List<T> list = new ArrayList<T>();
        list.add(v1);
        list.add(v2);
        return list;
    }
    
    /**
     * 
     * @return the string representation of formula operand.
     */
    public String getOperand() {
        return operand.toString();        
    }
    
    /**
     * 
     * @return the first formula argument.
     */
    public T getV1() {        
        return v1;
    }
    
    /**
     * 
     * @return the second formula argument.
     */
    public T getV2() {        
        return v2;
    }
    
    /**
     * 
     * @return true if formula is multiplicative.
     */
    public boolean isMultiplicative() {        
        return isMultiplicative;
    }

    public void setMultiplicative(boolean isMultiplicative) {
        this.isMultiplicative = isMultiplicative;        
    }

    public void setOperand(NumberOperations operand) {
        this.operand = operand;        
    }

    public void setV1(T v1) {
        this.v1 = v1;        
    }

    public void setV2(T v2) {
        this.v2 = v2;
    }
    
}
