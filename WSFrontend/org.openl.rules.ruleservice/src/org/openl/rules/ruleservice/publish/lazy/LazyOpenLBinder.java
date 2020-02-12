package org.openl.rules.ruleservice.publish.lazy;

import org.openl.IOpenBinder;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundCode;
import org.openl.binding.ICastFactory;
import org.openl.binding.INameSpacedMethodFactory;
import org.openl.binding.INameSpacedTypeFactory;
import org.openl.binding.INameSpacedVarFactory;
import org.openl.binding.INodeBinderFactory;
import org.openl.conf.IUserContext;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.lang.xls.prebind.XlsPreBinder;
import org.openl.syntax.code.IParsedCode;

/**
 * A wrapper for replacing Javassist proxy.
 *
 * @author Yury Molchan
 * @see LazyBinderMethodHandler
 */
class LazyOpenLBinder implements IOpenBinder {
    private final IOpenBinder originalBinder;
    private final IUserContext ucxt;

    LazyOpenLBinder(IOpenBinder originalBinder, IUserContext ucxt) {
        this.originalBinder = originalBinder;
        this.ucxt = ucxt;
    }

    private IOpenBinder getBinder() {
        IOpenBinder binder = originalBinder;
        IPrebindHandler prebindHandler = LazyBinderMethodHandler.getPrebindHandler();
        if (prebindHandler != null) {
            binder = new XlsPreBinder(ucxt, prebindHandler);
        }
        return binder;
    }

    @Override
    public IBoundCode bind(IParsedCode parsedCode) {
        return getBinder().bind(parsedCode);
    }

    @Override
    public IBoundCode bind(IParsedCode parsedCode, IBindingContext bindingContext) {
        return getBinder().bind(parsedCode, bindingContext);
    }

    @Override
    public ICastFactory getCastFactory() {
        return getBinder().getCastFactory();
    }

    @Override
    public INameSpacedMethodFactory getMethodFactory() {
        return getBinder().getMethodFactory();
    }

    @Override
    public INodeBinderFactory getNodeBinderFactory() {
        return getBinder().getNodeBinderFactory();
    }

    @Override
    public INameSpacedTypeFactory getTypeFactory() {
        return getBinder().getTypeFactory();
    }

    @Override
    public INameSpacedVarFactory getVarFactory() {
        return getBinder().getVarFactory();
    }

    @Override
    public IBindingContext makeBindingContext() {
        return getBinder().makeBindingContext();
    }
}
