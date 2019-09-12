package org.openl.rules.ruleservice.logging;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

@NoJSR250Annotations
public class StoreLoggingFeature extends AbstractFeature {
    private static final int DEFAULT_LIMIT = Integer.MAX_VALUE;

    private boolean storeLoggingEnabled = true;

    private int limit = DEFAULT_LIMIT;

    private boolean prettyLogging;

    private StoreLoggingService storeLoggingService;

    public StoreLoggingService getStoreLoggingService() {
        return storeLoggingService;
    }

    public void setStoreLoggingService(StoreLoggingService storeLoggingService) {
        this.storeLoggingService = storeLoggingService;
    }

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        if (isStoreLoggingEnabled()) {
            CollectRequestMessageInInterceptor storeLoggingInInterceptor = new CollectRequestMessageInInterceptor(
                Integer.MAX_VALUE);
            storeLoggingInInterceptor.setPrettyLogging(false);
            provider.getInInterceptors().add(storeLoggingInInterceptor);
            provider.getInFaultInterceptors().add(storeLoggingInInterceptor);

            CollectResponseMessageOutInterceptor storeLoggingOutInterceptor = new CollectResponseMessageOutInterceptor(
                Integer.MAX_VALUE,
                getStoreLoggingService());
            storeLoggingOutInterceptor.setPrettyLogging(false);
            provider.getOutInterceptors().add(storeLoggingOutInterceptor);
            provider.getOutFaultInterceptors().add(storeLoggingOutInterceptor);

            provider.getInInterceptors().add(new CollectInputDataInterceptor());
        }
    }

    public boolean isStoreLoggingEnabled() {
        return storeLoggingEnabled;
    }
    
    public void setStoreLoggingEnabled(boolean storeLoggingEnabled) {
        this.storeLoggingEnabled = storeLoggingEnabled;
    }
    
    /**
     * This function has no effect at this time.
     *
     * @param lim
     */
    public void setLimit(int lim) {
        limit = lim;
    }

    /**
     * Retrieve the value set with {@link #setLimit(int)}.
     *
     * @return the value set with {@link #setLimit(int)}.
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @return true if pretty logging of XML content is on
     */
    public boolean isPrettyLogging() {
        return prettyLogging;
    }

    /**
     * Turn pretty logging of XML content on/off
     *
     * @param prettyLogging
     */
    public void setPrettyLogging(boolean prettyLogging) {
        this.prettyLogging = prettyLogging;
    }
}
