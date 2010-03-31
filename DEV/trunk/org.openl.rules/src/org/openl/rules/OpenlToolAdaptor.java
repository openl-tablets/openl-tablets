/*
 * Created on Jun 23, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.rules;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.engine.OpenLManager;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;

/**
 * The purpose of this class is to simplify compiling of OpenL objects in
 * complex structured environments where context is defined on top and must be
 * propagated down without having to transfer many of the elements required to
 * do the validation and compilation.
 * 
 * @author snshor
 */
public class OpenlToolAdaptor {

    private OpenL openl;
    private IOpenMethodHeader header;
    private IBindingContext bindingContext;

    public OpenlToolAdaptor(OpenL openl, IBindingContext bindingContext) {
        this.openl = openl;
        this.bindingContext = bindingContext;
    }

    public IBindingContext getBindingContext() {
        return bindingContext;
    }

    public IOpenMethodHeader getHeader() {
        return header;
    }

    public OpenL getOpenl() {
        return openl;
    }

    public void setHeader(IOpenMethodHeader header) {
        this.header = header;
    }

    public void setOpenl(OpenL openL) {
        openl = openL;
    }

    public CompositeMethod makeMethod(IOpenSourceCodeModule src) {
        return OpenLManager.makeMethod(openl, src, header, bindingContext);
    }

    public CompositeMethod makeMethod(IOpenSourceCodeModule src, IOpenMethodHeader h2) {
        return OpenLManager.makeMethod(openl, src, h2, bindingContext);
    }

}
