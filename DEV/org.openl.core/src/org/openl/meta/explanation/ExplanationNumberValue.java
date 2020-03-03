package org.openl.meta.explanation;

import java.util.Collection;

import javax.xml.bind.annotation.XmlTransient;

import org.openl.meta.IMetaInfo;
import org.openl.meta.number.CastOperand;
import org.openl.meta.number.Formulas;
import org.openl.meta.number.NumberOperations;
import org.openl.util.tree.ITreeElement;

/**
 * Number value that supports explanation operations.
 *
 * @author DLiauchuk
 *
 * @param <T> type that extends {@link ExplanationNumberValue}
 */
public abstract class ExplanationNumberValue<T extends ExplanationNumberValue<T>> extends Number implements ExplanationForNumber<T> {

    private static final long serialVersionUID = -5461468496220613277L;

    /**
     * Explanator for current value.
     */
    private transient ExplanationForNumber<T> explanation;

    public ExplanationNumberValue() {
    }

    /** Formula constructor */
    public ExplanationNumberValue(T dv1, T dv2, Formulas operand) {
        /** initialize explanation for formula value */
        this.explanation = new FormulaExplanationValue<>(dv1, dv2, operand);
    }

    /** Function constructor */
    public ExplanationNumberValue(NumberOperations function, T[] params) {
        /** initialize explanation for function value */
        this.explanation = new FunctionExplanationValue<>(function, params);
    }

    /** Casting constructor */
    @SuppressWarnings("unchecked")
    public ExplanationNumberValue(ExplanationNumberValue<?> previousValue, CastOperand operand) {
        /** initialize explanation for cast value */
        this.explanation = new CastExplanationValue(previousValue, operand);
    }

    /**
     * @return explanation for a formula value.
     */
    public FormulaExplanationValue<T> getFormula() {
        return (FormulaExplanationValue<T>) explanation;
    }

    /**
     * @return explanation for a function value.
     */
    public FunctionExplanationValue<T> getFunction() {
        return (FunctionExplanationValue<T>) explanation;
    }

    /**
     * @return explanation for a cast value.
     */
    public CastExplanationValue getCast() {
        return (CastExplanationValue) explanation;
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

    /**
     * Lazy initialization of explanation to reduce memory usage in executionMode=true
     */
    private ExplanationForNumber<T> getExplanation() {
        if (explanation == null) {
            explanation = new SingleValueExplanation<>();
        }
        return explanation;
    }

    @Override
    @XmlTransient
    public IMetaInfo getMetaInfo() {
        return getExplanation().getMetaInfo();
    }

    @Override
    public void setMetaInfo(IMetaInfo metaInfo) {
        getExplanation().setMetaInfo(metaInfo);
    }

    @Override
    public String getName() {
        return getExplanation().getName();
    }

    @Override
    public String getDisplayName(int mode) {
        return getExplanation().getDisplayName(mode);
    }

    @Override
    public void setFullName(String name) {
        getExplanation().setFullName(name);
    }

    @Override
    public void setName(String name) {
        getExplanation().setName(name);
    }

    @Override
    public Collection<? extends ITreeElement<T>> getChildren() {
        return getExplanation().getChildren();
    }

    @Override
    public boolean isLeaf() {
        return getExplanation().isLeaf();
    }

    @Override
    public String getType() {
        return getExplanation().getType();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {
        return (T) this;
    }

    @Override
    public String toString() {
        return printValue();
    }

}
