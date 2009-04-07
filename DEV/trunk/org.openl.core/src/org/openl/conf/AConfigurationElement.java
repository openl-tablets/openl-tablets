/*
 * Created on Jun 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

/**
 * @author snshor
 *
 */
public abstract class AConfigurationElement implements IConfigurationElement {
    protected String uri;

    /**
     * @return
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param string
     */
    public void setUri(String string) {
        uri = string;
    }

}
