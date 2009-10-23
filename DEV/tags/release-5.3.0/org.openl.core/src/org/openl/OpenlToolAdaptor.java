/*
 * Created on Jun 23, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl;

import org.openl.binding.IBindingContext;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;

/**
 * @author snshor
 */
public class OpenlToolAdaptor implements IOpenlAdaptor {
    OpenL openl;
    IBindingContext bindingContext;
    IOpenMethodHeader header;

    public OpenlToolAdaptor(OpenL openl, IBindingContext bindingContext) {
        this.openl = openl;
        this.bindingContext = bindingContext;
    }

    /**
     * @return
     */
    public IBindingContext getBindingContext() {
        return bindingContext;
    }

    /**
     * @return
     */
    public IOpenMethodHeader getHeader() {
        return header;
    }

    /**
     * @return
     */
    public OpenL getOpenl() {
        return openl;
    }

    /**
     *
     */

    public CompositeMethod makeMethod(IOpenSourceCodeModule src) {
        return OpenlTool.makeMethod(src, openl, header, bindingContext);
    }

    public CompositeMethod makeMethod(IOpenSourceCodeModule src, IOpenMethodHeader h2) {
        return OpenlTool.makeMethod(src, openl, h2, bindingContext);
    }

    /**
     * @param header
     */
    public void setHeader(IOpenMethodHeader header) {
        this.header = header;
    }

    /**
     * @param openL
     */
    public void setOpenl(OpenL openL) {
        openl = openL;
    }

}
