package org.openl.rules.ruleservice.storelogdata;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

@NoJSR250Annotations
public class StoreLogDataFeature extends AbstractFeature {
    private static final int DEFAULT_LIMIT = Integer.MAX_VALUE;

    private boolean storeLogDataEnabled = true;

    private int limit = DEFAULT_LIMIT;

    private boolean prettyLogging;

    private StoreLogDataManager storeLogDataManager;

    public StoreLogDataManager getStoreLogDataManager() {
        return storeLogDataManager;
    }

    public void setStoreLogDataManager(StoreLogDataManager storeLogDataManager) {
        this.storeLogDataManager = storeLogDataManager;
    }

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        if (isStoreLogDataEnabled()) {
            CollectRequestMessageInInterceptor storeLogDataInInterceptor = new CollectRequestMessageInInterceptor(
                Integer.MAX_VALUE);
            storeLogDataInInterceptor.setPrettyLogging(false);
            provider.getInInterceptors().add(storeLogDataInInterceptor);
            provider.getInFaultInterceptors().add(storeLogDataInInterceptor);

            CollectResponseMessageOutInterceptor storeLogDataOutInterceptor = new CollectResponseMessageOutInterceptor(
                Integer.MAX_VALUE,
                getStoreLogDataManager());
            storeLogDataOutInterceptor.setPrettyLogging(false);
            provider.getOutInterceptors().add(storeLogDataOutInterceptor);
            provider.getOutFaultInterceptors().add(storeLogDataOutInterceptor);
        }
    }

    public boolean isStoreLogDataEnabled() {
        return storeLogDataEnabled;
    }

    public void setStoreLogDataEnabled(boolean storeLogDataEnabled) {
        this.storeLogDataEnabled = storeLogDataEnabled;
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
