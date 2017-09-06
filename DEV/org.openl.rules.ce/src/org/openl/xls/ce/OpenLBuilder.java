/*
 * Created on Oct 2, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.xls.ce;

import org.openl.OpenL;
import org.openl.conf.BaseOpenLBuilder;
import org.openl.conf.OpenConfigurationException;
import org.openl.rules.lang.xls.XlsParser;
import org.openl.rules.lang.xls.XlsVM;
import org.openl.rules.lang.xls.ce.XlsBinderCE;
import org.openl.xls.RulesCompileContext;

/**
 * @author snshor
 * 
 */
public class OpenLBuilder extends BaseOpenLBuilder {

    public OpenL build(String category) throws OpenConfigurationException {

        OpenL openl = new OpenL();

        openl.setParser(new XlsParser(getUserEnvironmentContext()));
        openl.setBinder(new XlsBinderCE(getUserEnvironmentContext()));
        openl.setVm(new XlsVM());
        openl.setCompileContext(new RulesCompileContext());

        return openl;
    }
}
