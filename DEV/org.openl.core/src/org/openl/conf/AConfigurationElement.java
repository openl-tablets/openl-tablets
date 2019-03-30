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
    private String uri;

    @Override
    public String getUri() {
        return uri;
    }

    public void setUri(String string) {
        uri = string;
    }

}
