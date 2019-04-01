package org.openl.rules.variation;

/*
 * #%L
 * OpenL - Variation
 * %%
 * Copyright (C) 2013 OpenL Tablets
 * %%
 * See the file LICENSE.txt for copying permission.
 * #L%
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;

/**
 * Container of result from calculation with variations. Stores results for each particular variation + original
 * result(means without variations) that can be retrieved by special ID, see {@link NoVariation#ORIGINAL_CALCULATION}.
 * 
 * Also stores exceptions for variation that was failed.
 * 
 * @param <T> return type of method calculated with variations.
 * 
 * @author PUdalau, Marat Kamalov
 */
@XmlRootElement
public class VariationsResult<T> {
    private byte[] data;

    private Map<String, T> variationResults;
    private Map<String, String> variationFailures;

    public VariationsResult() {
        variationResults = new LinkedHashMap<>();
        variationFailures = new LinkedHashMap<>();
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

    public void setVariationResults(Map<String, T> variationResults) {
        this.variationResults = variationResults;
    }

    /**
     * @return All failed calculations of variations.
     */
    public Map<String, String> getVariationFailures() {
        return Collections.unmodifiableMap(variationFailures);
    }

    public void setVariationFailures(Map<String, String> variationFailures) {
        this.variationFailures = variationFailures;
    }

    /**
     * @return IDs of successfully calculated variations.
     */
    @XmlTransient
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
    @XmlTransient
    public String[] getFailedVariationIDs() {
        String[] ids = new String[variationFailures.size()];
        int i = 0;
        for (String variationID : variationFailures.keySet()) {
            ids[i++] = variationID;
        }
        return ids;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    public void unpack() throws IOException {
        if (data != null) {
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(data);
            Hessian2Input input = new Hessian2Input(byteInputStream);
            try {
                input.startMessage();
                variationFailures = (Map<String, String>) input.readObject();
                variationResults = (Map<String, T>) input.readObject();
                input.completeMessage();
                input.close();
            } catch (IOException e) {
                throw e;
            } finally {
                if (byteInputStream != null) {
                    try {
                        byteInputStream.close();
                    } catch (IOException e) {

                    }
                }
            }

        }
    }

    public void pack() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(byteArrayOutputStream);
        try {
            out.startMessage();
            out.writeObject(variationFailures);
            out.writeObject(variationResults);
            out.completeMessage();
            out.close();
            data = byteArrayOutputStream.toByteArray();
            variationFailures.clear();
            variationResults.clear();
        } catch (IOException e) {
            // Should never happen for ByteArrayOutputStream. If happen -
            // something is broken.
            throw new IllegalStateException(e);
        } finally {
            if (byteArrayOutputStream != null) {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e) {

                }
            }
        }
    }

    /**
     * @return IDs of all processed variations: successfully calculated and failed ones.
     */
    @XmlTransient
    public String[] getAllProcessedVariationIDs() {
        String[] failedIDs = getFailedVariationIDs();
        String[] calculatedIDs = getCalculatedVariationIDs();
        String[] ids = new String[failedIDs.length + calculatedIDs.length];
        System.arraycopy(calculatedIDs, 0, ids, 0, calculatedIDs.length);
        System.arraycopy(failedIDs, 0, ids, calculatedIDs.length, failedIDs.length);
        return ids;
    }
}
