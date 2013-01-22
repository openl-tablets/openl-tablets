package org.openl.rules.project.instantiation.variation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Container of result from calculation with variations. Stores results for each
 * particular variation + original result(means without variations) that can be
 * retrieved by special ID, see {@link NoVariation.ORIGIANAL_CALCULATION}.
 * 
 * Also stores exceptions for variation that was failed.
 * 
 * @param <T> return type of method calculated with variations.
 * 
 * @author PUdalau, Marat Kamalov
 */
public class VariationsResult<T> {
    private Map<String, T> variationResults;
    private Map<String, String> variationFailures;

    public VariationsResult() {
        variationResults = new LinkedHashMap<String, T>();
        variationFailures = new LinkedHashMap<String, String>();
    }

    /**
     * Stores result of calculation with the specified variation.
     * 
     * @param variationID ID of variation.
     * @param result Result of the caculation with the corresponding variation.
     */
    public void registerResult(String variationID, T result) {
        variationResults.put(variationID, result);
    }

    public void registerFailure(String variationID, String errorMessage) {
        variationFailures.put(variationID, errorMessage);
    }

    /**
     * Return successfully calculated result for variation.
     * 
     * @param variationID ID of needed variation.
     * @return Result of calculation with the corresponding variation.
     */
    public T getResultForVariation(String variationID) {
        return variationResults.get(variationID);
    }

    /**
     * 
     * @param variationID ID of needed variation.
     * @return Error message that occurred during the calculation of variation.
     */
    public String getFailureErrorForVariation(String variationID) {
        return variationFailures.get(variationID);
    }

    /**
     * @return All stored results for calculated variations.
     */
    public Map<String, T> getVariationResults() {
        return Collections.unmodifiableMap(variationResults);
    }

    /**
     * @return All failed calculations of variations.
     */
    public Map<String, String> getVariationFailures() {
        return Collections.unmodifiableMap(variationFailures);
    }

    /**
     * @return IDs of successfully calculated variations.
     */
    public String[] getCalculatedVariationIDs() {
        String[] ids = new String[variationResults.size()];
        int i = 0;
        for (String variationID : variationResults.keySet()) {
            ids[i++] = variationID;
        }
        return ids;
    }

    /**
     * @return IDs of variations that have been failed and thrown an exception.
     */
    public String[] getFailedVariationIDs() {
        String[] ids = new String[variationFailures.size()];
        int i = 0;
        for (String variationID : variationFailures.keySet()) {
            ids[i++] = variationID;
        }
        return ids;
    }

    /**
     * @return IDs of all processed variations: successfully calculated and
     *         failed ones.
     */
    public String[] getAllProcessedVariationIDs() {
        String[] failedIDs = getFailedVariationIDs();
        String[] calculatedIDs = getCalculatedVariationIDs();
        String[] ids = new String[failedIDs.length + calculatedIDs.length];
        for (int i = 0; i < calculatedIDs.length; i++) {
            ids[i] = calculatedIDs[i];
        }
        for (int i = 0; i < failedIDs.length; i++) {
            ids[calculatedIDs.length + i] = failedIDs[i];
        }
        return ids;
    }
}
