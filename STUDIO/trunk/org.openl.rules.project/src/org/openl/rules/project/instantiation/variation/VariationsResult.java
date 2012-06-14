package org.openl.rules.project.instantiation.variation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Container of result from calculation with variations. Stores results for each
 * particular variation + original result(means without variations) that can be
 * retrieved by special ID, see {@link NoVariation.ORIGIANAL_CALCULATION}.
 * 
 * Also stores exceptions for variation that was failed.
 * 
 * @param <T> return type of method calculated with variations.
 * 
 * @author PUdalau
 */
public class VariationsResult<T> {
    private final Log log = LogFactory.getLog(VariationsResult.class);
    private LinkedHashMap<String, T> variationResults;
    private LinkedHashMap<String, Exception> variationFailures;

    public VariationsResult() {
        variationResults = new LinkedHashMap<String, T>();
        variationFailures = new LinkedHashMap<String, Exception>();
    }

    /**
     * Stores result of calculation with the specified variation.
     * 
     * @param variationID ID of variation.
     * @param result Result of the caculation with the corresponding variation.
     */
    public void registerResults(String variationID, T result) {
        if (variationResults.containsKey(variationID) || variationFailures.containsKey(variationID)) {
            log.warn("Variation result with id \"" + variationID + "\" has been already registered, make sure that all your input variations has unique ID.");
        }
        variationResults.put(variationID, result);
    }

    public void registerFailure(String variationID, Exception exception) {
        if (variationResults.containsKey(variationID) || variationFailures.containsKey(variationID)) {
            log.warn("Variation result with id \"" + variationID + "\" has been already registered, make sure that all your input variations has unique ID.");
        }
        variationFailures.put(variationID, exception);
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
     * @return Error that occurred during the calculation of variation.
     */
    public Exception getFailureErrorForVariation(String variationID) {
        return variationFailures.get(variationID);
    }

    /**
     * @return All stored results for calculated variations.
     */
    public Map<String, T> getVariationResults() {
        return variationResults;
    }

    /**
     * @return All failed calculations of variations.
     */
    public LinkedHashMap<String, Exception> getVariationFailures() {
        return variationFailures;
    }

    /**
     * @return IDs of successfully calculated variations.
     */
    public List<String> getCalculatedVariationIDs() {
        List<String> ids = new ArrayList<String>(variationResults.size());
        for (String variationID : variationResults.keySet()) {
            ids.add(variationID);
        }
        return ids;
    }

    /**
     * @return IDs of variations that have been failed and thrown an exception.
     */
    public List<String> getFailedVariationIDs() {
        List<String> ids = new ArrayList<String>(variationFailures.size());
        for (String variationID : variationFailures.keySet()) {
            ids.add(variationID);
        }
        return ids;
    }

    /**
     * @return IDs of all processed variations: successfully calculated and
     *         failed ones.
     */
    public List<String> getAllProcessedVariationIDs() {
        List<String> ids = new ArrayList<String>(getCalculatedVariationIDs());
        ids.addAll(getFailedVariationIDs());
        return ids;
    }
}
