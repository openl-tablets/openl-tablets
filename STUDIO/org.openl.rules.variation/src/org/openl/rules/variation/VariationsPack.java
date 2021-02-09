package org.openl.rules.variation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Container of all variations for calculations.
 *
 * Serves as the additional input parameter for special method that represents "calculation with variations".
 *
 * See {@link VariationsEnhancer}
 *
 * @author Marat Kamalov
 */
@XmlRootElement
public final class VariationsPack {
    private List<Variation> variations = new ArrayList<>();

    public VariationsPack() {
    }

    public VariationsPack(Variation... variations) {
        for (int i = 0; i < variations.length - 1; i++) {
            for (int j = i + 1; j < variations.length; j++) {
                if (variations[i].getVariationID().equals(variations[j].getVariationID())) {
                    throw new IllegalArgumentException("variations contains two variations with the same variationID");
                }
            }
        }
        Collections.addAll(this.variations, variations);
    }

    public void addVariation(Variation variation) throws VariationException {
        if (variation == null || variation.getVariationID() == null || variation.getVariationID().isEmpty()) {
            throw new IllegalArgumentException("variation argument is invalid");
        }
        for (Variation v : variations) {
            if (variation.getVariationID().equals(v.getVariationID())) {
                throw new VariationException(
                    "Variation pack already contains variation with this variationID=" + variation.getVariationID());
            }
        }
        variations.add(variation);
    }

    /**
     * Removes variation by variationID
     *
     * @param variationID
     * @return
     */
    public boolean removeVariation(String variationID) {
        if (variationID == null || variationID.isEmpty()) {
            throw new IllegalArgumentException("variationID must not be empty");
        }
        int i;
        for (i = 0; i < variations.size(); i++) {
            if (variationID.equals(variations.get(i).getVariationID())) {
                break;
            }
        }
        if (i <= variations.size()) {
            variations.remove(i);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * @return All variations defined in this pack.
     */
    public List<Variation> getVariations() {
        return variations;
    }

    public void setVariations(List<Variation> variations) {
        this.variations = variations;
    }

    /**
     * @return IDs of all variation defined in this pack.
     */
    @XmlTransient
    public String[] getVariationIDs() {
        String[] ids = new String[variations.size()];
        int i = 0;
        for (Variation variation : variations) {
            ids[i++] = variation.getVariationID();
        }
        return ids;
    }
}
