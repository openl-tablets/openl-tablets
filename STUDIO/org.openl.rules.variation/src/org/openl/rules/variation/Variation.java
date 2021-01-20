package org.openl.rules.variation;

import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

/**
 * Common variations class. It should have unique ID and handle two phases: modifying arguments before the calculations
 * and rolling back changes after execution.
 *
 * To store previous values of changed fields there can be used stack passed as argument(if it is needed.)
 *
 * @author PUdalau, Marat Kamalov
 */

@XmlRootElement
@XmlSeeAlso({ ArgumentReplacementVariation.class,
        NoVariation.class,
        ComplexVariation.class,
        JXPathVariation.class,
        DeepCloningVariation.class })
public abstract class Variation {
    private String variationID;

    /**
     * No argument constructor. Required for WS data binding.
     */
    public Variation() {
        this.variationID = UUID.randomUUID().toString();
    }

    /**
     * Constructs variation with the ID.
     *
     * @param variationID Unique ID.
     */
    public Variation(String variationID) {
        this.variationID = variationID;
    }

    /**
     * @return Unique ID of this variation.
     */
    public String getVariationID() {
        return variationID;
    }

    public void setVariationID(String variationID) {
        this.variationID = variationID;
    }

    /**
     * Returns current value for this variation
     *
     * @param originalArguments
     * @return
     */
    public abstract Object currentValue(Object[] originalArguments);

    /**
     * Modifies original arguments before the calculation.
     *
     * @param originalArguments Original arguments for calculation.
     * @param stack The Stack instance to store previous values of changed fields.
     * @return Modified arguments.
     */
    public abstract Object[] applyModification(Object[] originalArguments);

    /**
     * Reverts changes of arguments after the calculation.
     *
     * @param modifiedArguments Modified arguments.
     * @param stack Stack where previous values of modified fields were stored.
     */
    public abstract void revertModifications(Object[] modifiedArguments, Object previousValue);

}
