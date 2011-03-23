package org.openl.meta.number;

import java.util.ArrayList;
import java.util.List;


/**
 * Generic implementation for functions over {@link NumberValue} objects.
 * 
 * @author DLiauchuk
 *
 * @param <T> type that extends {@link NumberValue}
 */
public class NumberFunction<T extends NumberValue<T>> {
    
    private NumberOperations function;
    private T result;
    private T[] params;
    
    public NumberFunction(NumberOperations function, T[] params, T result) {
        this.function = function;
        if (params != null) {
            this.params = params.clone();
        }        
        this.result = result;
    }
    
    /**
     * Add new parameter to the function.
     * 
     * @param param
     */
    @SuppressWarnings("unchecked")
    public void addParam(T param) {
        
        List<T> list = new ArrayList<T>();
                
        for (int i = 0; i < params.length; i++) {
            list.add(params[i]);
        }
        list.add(param);
        params = (T[]) list.toArray();
        
    }
    
    /**
     * 
     * @return name of the function.
     */
    public String getFunctionName() {
        return function.toString();
    }
    
    /**
     * 
     * @return the array of function parameters.
     */
    public T[] getParams() {
        return params;
    }
    
    /**
     * 
     * @return result of the function.
     */
    public T getResult() {
        return result;
    }

    public void setFunction(NumberOperations function) {
        this.function = function;        
    }

    public void setParams(T[] params) {
        this.params = params;        
    }

    public void setResult(T result) {
        this.result = result;        
    }
}
