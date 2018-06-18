/*
 * Created on Oct 2, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.xls;

import org.openl.IOpenParser;
import org.openl.OpenL;
import org.openl.conf.BaseOpenLBuilder;
import org.openl.conf.OpenConfigurationException;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.lang.xls.XlsParser;
import org.openl.rules.lang.xls.XlsVM;

/**
 * @author snshor
 * 
 */
public class OpenLBuilder extends BaseOpenLBuilder {

    public OpenL build(String category) throws OpenConfigurationException {

        OpenL openl = new OpenL();

        openl.setParser(createParser());
        openl.setBinder(new XlsBinder(getUserEnvironmentContext()));
        openl.setVm(new XlsVM());
        openl.setCompileContext(new RulesCompileContext());

        return openl;
    }

    protected IOpenParser createParser() {
        return new XlsParser(getUserEnvironmentContext());
    }
}
