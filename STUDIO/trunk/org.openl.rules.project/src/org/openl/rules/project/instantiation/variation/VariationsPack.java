package org.openl.rules.project.instantiation.variation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Container of all variations for calculations.
 * 
 * Serves as the additional input parameter for special method that represents
 * "calculation with variations".
 * 
 * See {@link VariationsEnhancer}
 * 
 * @author PUdalau
 */
public class VariationsPack {
    private List<Variation> variations;

    public VariationsPack() {
        variations = new ArrayList<Variation>();
    }

    public VariationsPack(List<Variation> variations) {
        if (variations != null) {
            this.variations = variations;
        } else {
            variations = new ArrayList<Variation>();
        }
    }

    public VariationsPack(Variation... variations) {
        this.variations = Arrays.asList(variations);
    }

    public void addVariation(Variation variation) {
        variations.add(variation);
    }

    public boolean removeVariation(Variation variation) {
        return variations.remove(variation);
    }

    /**
     * @return All variations defined in this pack.
     */
    public List<Variation> getVariations() {
        return variations;
    }

    /**
     * @return IDs of all variation defined in this pack.
     */
    public List<String> getVariationIDs() {
        List<String> ids = new ArrayList<String>(variations.size());
        for (Variation variation : variations) {
            ids.add(variation.getVariationID());
        }
        return ids;
    }
}
