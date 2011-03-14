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
    
    private String functionName;
    private T result;
    private T[] params;
    
    public NumberFunction(String functionName, T[] params, T result) {
        this.functionName = functionName;
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
        return functionName;
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

    public void setFunctionName(String functionName) {
        this.functionName = functionName;        
    }

    public void setParams(T[] params) {
        this.params = params;        
    }

    public void setResult(T result) {
        this.result = result;        
    }
}
