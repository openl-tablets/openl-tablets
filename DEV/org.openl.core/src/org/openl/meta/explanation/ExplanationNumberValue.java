package org.openl.meta.explanation;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.meta.IMetaInfo;
import org.openl.meta.number.CastOperand;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.NumberCast;
import org.openl.meta.number.NumberFormula;
import org.openl.meta.number.NumberOperations;
import org.openl.meta.number.NumberValue;
import org.openl.util.tree.ITreeElement;

/**
 * Number value that supports explanation operations.
 * 
 * @author DLiauchuk
 *
 * @param <T> type that extends {@link ExplanationNumberValue}
 */
public abstract class ExplanationNumberValue<T extends ExplanationNumberValue<T>> extends NumberValue<T> implements
    ExplanationForNumber<T> {
    
    private static final long serialVersionUID = -5461468496220613277L;
    
    /** 
     * Explanator for current value.
     */
    private transient ExplanationForNumber<T> explanation;

    public ExplanationNumberValue() {        
        this.explanation = new SingleValueExplanation<T>();
    }
    
    public ExplanationNumberValue(IMetaInfo metaInfo) {
        this.explanation = new SingleValueExplanation<T>(metaInfo);
    }

    public ExplanationNumberValue(String name) {
        this.explanation = new SingleValueExplanation<T>(name);
    }
    
    /** Formula constructor */
    public ExplanationNumberValue(T dv1, T dv2, Formulas operand) {   
        this.formula = new NumberFormula<T>(dv1, dv2, operand);
        /** initialize explanation for formula value */
        this.explanation = new FormulaExplanationValue<T>(this.formula);
    }
    
    /** Function constructor */
    public ExplanationNumberValue(NumberOperations function, T[] params) {
        /** initialize explanation for function value */
        this.explanation = new FunctionExplanationValue<T>(function, params);
    }
    
    /** Casting constructor */
    @SuppressWarnings("unchecked")
    public ExplanationNumberValue(ExplanationNumberValue<?> previousValue, CastOperand operand) {   
        this.cast = new NumberCast(previousValue, operand);
        /** initialize explanation for cast value */
        this.explanation = new CastExplanationValue(this.cast);
    }

    private NumberFormula<T> formula;

    /**
     *
     * @return formula for current value.
     */
    public NumberFormula<T> getFormula() {
        return formula;
    }

    /**
     * @return explanation for a function value.
     */
    public FunctionExplanationValue<T> getFunction() {
        return (FunctionExplanationValue<T>) explanation;
    }

    private NumberCast cast;

    public NumberCast getCast() {
        return cast;
    }

    public boolean isFormula() {
        return explanation instanceof FormulaExplanationValue;
    }

    public boolean isFunction() {
        return explanation instanceof FunctionExplanationValue;
    }

    public boolean isCast() {
        return explanation instanceof CastExplanationValue;
    }

    public abstract T copy(String name);

    public IMetaInfo getMetaInfo() {
        return explanation.getMetaInfo();
    }
    
    public void setMetaInfo(IMetaInfo metaInfo) {
        explanation.setMetaInfo(metaInfo);
    }
    
    public String getName() {
        return explanation.getName();
    }
    
    public String getDisplayName(int mode) {
        return explanation.getDisplayName(mode);
    }

    public void setFullName(String name) {
        explanation.setFullName(name);
    }

    public void setName(String name) {
        explanation.setName(name);
    }

    public Iterable<? extends ITreeElement<T>> getChildren() {
        return explanation.getChildren();
    }

    public boolean isLeaf() {
        return explanation.isLeaf();
    }

    public String getType() {
        return explanation.getType();
    }

    @SuppressWarnings("unchecked")
    public T getObject() {    
        return (T) this;
    }
    
    @Override
    public String toString() {
        return printValue();
    }
    
    /**
     * Returns the equal element from collection
     * @param values
     * @param result
     * @return
     */
    protected static <T extends NumberValue<T>> NumberValue<T> getAppropriateValue(NumberValue<T>[] values, NumberValue<?> result) {
        for (NumberValue<T> value : values) {
            if (value == result || value != null && value.equals(result)) {
                return value;
            }
        }
        return null;
    }
    
    protected static OpenlNotCheckedException getTwoArgumentsException(String operation) {
        return new OpenlNotCheckedException(String.format("None of the arguments for '%s' operation can be null", 
            operation));
    }
    
    protected static OpenlNotCheckedException getOneArgumentException(NumberOperations operation) {
        return new OpenlNotCheckedException(String.format("Argument couldn`t be null for '%s' operation", 
            operation.toString()));
    }
    
    protected static void validate(ExplanationNumberValue<?> value1, ExplanationNumberValue<?> value2, 
            NumberOperations operation) {
        if (value1 == null || value2 == null) {
            throw getTwoArgumentsException(operation.toString());
        }
    }
    
    protected static void validate(ExplanationNumberValue<?> value, NumberOperations operation) {
        if (value == null) {
            throw getOneArgumentException(operation);
        }
    }
    
    protected static void validate(ExplanationNumberValue<?> value1, ExplanationNumberValue<?> value2, 
            String operation) {
        if (value1 == null || value2 == null) {
            throw getTwoArgumentsException(operation);
        }
    }
    
    
    
}
