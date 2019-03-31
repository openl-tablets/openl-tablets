package org.openl.rules.ruleservice.logging;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;

/**
 * This class is used to control message-on-the-wire logging. By attaching this feature to an endpoint, you can specify
 * logging. If this feature is present, an endpoint will log input and output of ordinary and log messages. <br/>
 * <br/>
 * If the <tt>loggingEnabled</tt> property is set to false then logging is disabled (default is true). By default
 * <tt>LoggingInInterceptor</tt> and <tt>LoggingOutInterceptor</tt> are used to log, you can redefine appropriate
 * properties to use another logging interceptors.
 * 
 * @author NSamatov
 * 
 */
@NoJSR250Annotations
public class LoggingFeature extends AbstractFeature {
    private static final int DEFAULT_LIMIT = Integer.MAX_VALUE;

    private boolean loggingEnabled = true;

    private LoggingInInterceptor inInterceptor;
    private LoggingOutInterceptor outInterceptor;
    private LoggingInInterceptor inFaultInterceptor;
    private LoggingOutInterceptor outFaultInterceptor;

    private String inLocation;
    private String outLocation;
    private String inFaultLocation;
    private String outFaultLocation;

    private int limit = DEFAULT_LIMIT;

    private boolean prettyLogging;

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        if (isLoggingEnabled()) {
            if (getInInterceptor() == null) {
                LoggingInInterceptor in = new LoggingInInterceptor(limit);
                in.setOutputLocation(inLocation);
                in.setPrettyLogging(prettyLogging);
                provider.getInInterceptors().add(in);
            } else {
                provider.getInInterceptors().add(getInInterceptor());
            }
            if (getOutInterceptor() == null) {
                LoggingOutInterceptor out = new LoggingOutInterceptor(limit);
                out.setOutputLocation(outLocation);
                out.setPrettyLogging(prettyLogging);
                provider.getOutInterceptors().add(out);
            } else {
                provider.getOutInterceptors().add(getOutInterceptor());
            }
            if (getInFaultInterceptor() == null) {
                LoggingInInterceptor in = new LoggingInInterceptor(limit);
                in.setOutputLocation(inFaultLocation);
                in.setPrettyLogging(prettyLogging);
                provider.getInFaultInterceptors().add(in);
            } else {
                provider.getInFaultInterceptors().add(getInFaultInterceptor());
            }
            if (getOutFaultInterceptor() == null) {
                LoggingOutInterceptor out = new LoggingOutInterceptor(limit);
                out.setOutputLocation(outFaultLocation);
                out.setPrettyLogging(prettyLogging);
                provider.getOutFaultInterceptors().add(out);
            } else {
                provider.getOutFaultInterceptors().add(getOutFaultInterceptor());
            }
        }
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public void setLoggingEnabled(boolean loggingEnabled) {
        this.loggingEnabled = loggingEnabled;
    }

    public LoggingInInterceptor getInInterceptor() {
        return inInterceptor;
    }

    public void setInInterceptor(LoggingInInterceptor inInterceptor) {
        this.inInterceptor = inInterceptor;
    }

    public LoggingOutInterceptor getOutInterceptor() {
        return outInterceptor;
    }

    public void setOutInterceptor(LoggingOutInterceptor outInterceptor) {
        this.outInterceptor = outInterceptor;
    }

    public LoggingInInterceptor getInFaultInterceptor() {
        return inFaultInterceptor;
    }

    public void setInFaultInterceptor(LoggingInInterceptor inFaultInterceptor) {
        this.inFaultInterceptor = inFaultInterceptor;
    }

    public LoggingOutInterceptor getOutFaultInterceptor() {
        return outFaultInterceptor;
    }

    public void setOutFaultInterceptor(LoggingOutInterceptor outFaultInterceptor) {
        this.outFaultInterceptor = outFaultInterceptor;
    }

    public String getInLocation() {
        return inLocation;
    }

    public void setInLocation(String inLocation) {
        this.inLocation = inLocation;
    }

    public String getOutLocation() {
        return outLocation;
    }

    public void setOutLocation(String outLocation) {
        this.outLocation = outLocation;
    }

    public String getInFaultLocation() {
        return inFaultLocation;
    }

    public void setInFaultLocation(String inFaultLocation) {
        this.inFaultLocation = inFaultLocation;
    }

    public String getOutFaultLocation() {
        return outFaultLocation;
    }

    public void setOutFaultLocation(String outFaultLocation) {
        this.outFaultLocation = outFaultLocation;
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
