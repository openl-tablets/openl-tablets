package org.openl.rules.webstudio.services;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.Serializable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ServiceParams
 *
 * @author Andrey Naumenko
 */
public class ServiceParams implements Serializable {
    private static final long serialVersionUID = 1L;
    private String fileName;
    private Locale locale;
    private Map<String, Object> extraParams = new HashMap<String, Object>();

    /**
     * Additional parameters that can be defined in runtime, key: parameter
     * name, value: parameter value. Generally passed to XSLT.
     *
     * @return additional parameters
     */
    public Map<String, Object> getExtraParams() {
        return extraParams;
    }

    /**
     * File name has different means in different services.
     *
     * @return file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Locale.
     *
     * @return locale
     */
    public Locale getLocale() {
        return locale;
    }

    public void setExtraParams(Map<String, Object> extraParams) {
        this.extraParams = extraParams;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE);
    }
}
