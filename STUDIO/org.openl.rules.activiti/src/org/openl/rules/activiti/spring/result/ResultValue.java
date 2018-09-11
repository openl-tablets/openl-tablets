package org.openl.rules.activiti.spring.result;

import org.activiti.engine.delegate.DelegateExecution;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.convertor.ObjectToDataOpenCastConvertor;

public class ResultValue {

    private Object value;
    private ObjectToDataOpenCastConvertor convertor = new ObjectToDataOpenCastConvertor();

    public ResultValue(Object value) {
        this.value = value;
    }
    
    @SuppressWarnings("unchecked")
    private <T> T convert(Class<T> to) {
        IOpenCast openCast = convertor.getConvertor(to, value.getClass());
        if (openCast != null) {
            return (T) openCast.convert(value);
        } else {
            throw new ResultValueConvertException(String.format("Can't convert from '%s' to '%s'!",
                value.getClass().getCanonicalName(),
                to.getCanonicalName()));
        }
    }
    
    public ResultValue asByte() {
        return new ResultValue(toByte());
    }

    public ResultValue asInt() {
        return new ResultValue(toInt());
    }

    public ResultValue asLong() {
        return new ResultValue(toLong());
    }

    public ResultValue asFloat() {
        return new ResultValue(toFloat());
    }

    public ResultValue asDouble() {
        return new ResultValue(toDouble());
    }

    public ResultValue asString() {
        return new ResultValue(toString());
    }

    public ResultValue asBoolean() {
        return new ResultValue(toBoolean());
    }

    public Byte toByte() {
        return convert(Byte.class);
    }

    public Integer toInt() {
        return convert(Integer.class);
    }

    public Long toLong() {
        return convert(Long.class);
    }

    public Float toFloat() {
        return convert(Float.class);
    }

    public Double toDouble() {
        return convert(Double.class);
    }

    public String toString() {
        return convert(String.class);
    }

    public Boolean toBoolean() {
        return convert(Boolean.class);
    }

    public Object value() {
        return value;
    }

    public void set(DelegateExecution execution, String variableName) {
        execution.setVariable(variableName, value);
    }

    public void set(DelegateExecution execution, String variableName, boolean fetchAllVariables) {
        execution.setVariable(variableName, value, fetchAllVariables);
    }

    public void setLocal(DelegateExecution execution, String variableName) {
        execution.setVariableLocal(variableName, value);
    }

    public void setLocal(DelegateExecution execution, String variableName, boolean fetchAllVariables) {
        execution.setVariableLocal(variableName, value, fetchAllVariables);
    }
}
