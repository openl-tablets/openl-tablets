/*
 * Created on Oct 2, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.xls;

import org.openl.OpenConfigurationException;
import org.openl.OpenL;
import org.openl.conf.IConfigurableResourceContext;
import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IUserContext;
import org.openl.rules.lang.xls.XlsBinder;
import org.openl.rules.lang.xls.XlsParser;
import org.openl.rules.lang.xls.XlsVM;

/**
 * @author snshor
 *
 */
public class OpenLBuilder implements IOpenLBuilder {

    IUserContext ucxt;

    /**
     *
     */
    public OpenLBuilder() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.conf.IOpenLBuilder#build(java.lang.String)
     */
    public OpenL build(String category) throws OpenConfigurationException {
        OpenL openl = new OpenL();
        openl.setParser(new XlsParser(ucxt));
        openl.setBinder(new XlsBinder(ucxt));
        openl.setVm(new XlsVM());
        return openl;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.conf.IOpenLBuilder#setConfigurableResourceContext(org.openl.conf.IConfigurableResourceContext)
     */
    public void setConfigurableResourceContext(IConfigurableResourceContext cxt, IUserContext ucxt) {
        this.ucxt = ucxt;
    }

}
