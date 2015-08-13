/*
 * Created on Oct 2, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.extension.xmlrules;

import org.openl.*;
import org.openl.extension.ExtensionOpenLBuilder;

public class OpenLBuilder extends ExtensionOpenLBuilder {

    @Override
    protected IOpenParser getParser() {
        return new XmlRulesParser();
    }

    @Override
    protected IOpenBinder getBinder() {
        return new XmlRulesBinder(getUserEnvironmentContext());
    }
}
