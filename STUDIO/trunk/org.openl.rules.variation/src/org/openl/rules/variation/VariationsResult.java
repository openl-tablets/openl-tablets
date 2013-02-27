package org.openl.rules.variation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;

/**
 * Container of result from calculation with variations. Stores results for each
 * particular variation + original result(means without variations) that can be
 * retrieved by special ID, see {@link NoVariation#ORIGINAL_CALCULATION}.
 * 
 * Also stores exceptions for variation that was failed.
 * 
 * @param <T> return type of method calculated with variations.
 * 
 * @author PUdalau, Marat Kamalov
 */
public class VariationsResult<T> {
    private byte[] variationResultsData;
    private byte[] variationFailuresData;

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

    public byte[] getVariationResultsData() {
        return variationResultsData;
    }

    public void setVariationResultsData(byte[] variationResultsData) {
        this.variationResultsData = variationResultsData;
    }

    public byte[] getVariationFailuresData() {
        return variationFailuresData;
    }

    public void setVariationFailuresData(byte[] variationFailuresData) {
        this.variationFailuresData = variationFailuresData;
    }

    @SuppressWarnings("unchecked")
    public void unpack() throws IOException {
        if (variationFailuresData != null) {
            XStream xStream = new XStream(new Sun14ReflectionProvider());
            GZIPInputStream gzipInputStream = null;
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(variationFailuresData);
                gzipInputStream = new GZIPInputStream(bais);
                StringWriter writer = new StringWriter();
                IOUtils.copy(gzipInputStream, writer, "UTF-8");
                String xmlVariationFailures = writer.toString();
                variationFailures = (Map<String, String>) xStream.fromXML(xmlVariationFailures);
            } finally {
                if (gzipInputStream != null) {
                    try {
                        gzipInputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
        if (variationResultsData != null) {
            XStream xStream = new XStream(new Sun14ReflectionProvider());
            GZIPInputStream gzipInputStream = null;
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(variationResultsData);
                gzipInputStream = new GZIPInputStream(bais);
                StringWriter writer = new StringWriter();
                IOUtils.copy(gzipInputStream, writer, "UTF-8");
                String xmlVariationResults = writer.toString();
                variationResults = (Map<String, T>) xStream.fromXML(xmlVariationResults);
            } finally {
                if (gzipInputStream != null) {
                    try {
                        gzipInputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    public void pack() {
        ByteArrayOutputStream byteArrayOutputStream = null;
        XStream xStream = new XStream(new Sun14ReflectionProvider());
        String xmlVariationFailures = xStream.toXML(variationFailures);
        String xmlVariationResults = xStream.toXML(variationResults);
        GZIPOutputStream gzipOutputStream = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
            gzipOutputStream.write(xmlVariationFailures.getBytes("UTF-8"));
            gzipOutputStream.close();
            variationFailuresData = byteArrayOutputStream.toByteArray();
            variationFailures.clear();
        } catch (IOException e) {
            // Should never happen for ByteArrayOutputStream. If happen - something is broken.
            throw new IllegalStateException(e);
        } finally {
            if (gzipOutputStream != null) {
                try {
                    gzipOutputStream.close();
                } catch (IOException e) {

                }
            }
        }
        gzipOutputStream = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] data = xmlVariationResults.getBytes("UTF-8");
            gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream, data.length);
            gzipOutputStream.write(data);
            gzipOutputStream.close();
            variationResultsData = byteArrayOutputStream.toByteArray();
            variationResults.clear();
        } catch (IOException e) {
            // Should never happen for ByteArrayOutputStream. If happen - something is broken.
            throw new IllegalStateException(e);
        } finally {
            if (gzipOutputStream != null) {
                try {
                    gzipOutputStream.close();
                } catch (IOException e) {

                }
            }
        }
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
