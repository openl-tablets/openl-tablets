package org.openl.rules.project.instantiation.variation;

import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.table.InputArgumentsCloner;

/**
 * Variation that clones all arguments before the modification by another
 * variation(that is delegated.)
 * 
 * @author PUdalau
 */
public class DeepCloninigVariaion extends Variation {
    private final Log log = LogFactory.getLog(DeepCloninigVariaion.class);

    /**
     * Suffix for generated variation ID if it have not been specified.
     */
    public static final String DEEP_CLONING_SUFFIX = "[Deep Cloning]";
    private Variation variation;

    /**
     * Constructs deep-cloning variation with the generated ID(ID of delegated
     * variation + {@link DeepCloninigVariaion.DEEP_CLONING_SUFFIX}).
     * 
     * @param variation Delegated variation.
     */
    public DeepCloninigVariaion(Variation variation) {
        this(variation + DEEP_CLONING_SUFFIX, variation);
    }

    /**
     * Constructs deep-cloning variation with the specified ID.
     * 
     * @param variationID Unique variation ID.
     * @param variation Delegated variation.
     */
    public DeepCloninigVariaion(String variationID, Variation variation) {
        super(variationID);
        this.variation = variation;
    }

    @Override
    public Object[] applyModification(Object[] originalArguments, Stack<Object> stack) {
        Object[] clonedParams = null;
        InputArgumentsCloner cloner = new InputArgumentsCloner();
        if (originalArguments != null) {

            try {
                clonedParams = cloner.deepClone(originalArguments);
            } catch (Exception ex) {
                log.error("Faield to clone arguments in variation \"" + getVariationID() + "\". Original arguments will be used.");
                clonedParams = originalArguments;
            }
        } else {
            clonedParams = new Object[0];
        }
        return variation.applyModification(clonedParams, stack);
    }

    @Override
    public void revertModifications(Object[] modifiedArguments, Stack<Object> stack) {
    }

}
