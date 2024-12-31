package org.openl.j;

import org.openl.OpenL;
import org.openl.binding.impl.Binder;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IUserContext;
import org.openl.conf.LibrariesRegistry;
import org.openl.conf.TypeResolver;
import org.openl.rules.lang.xls.Parser;
import org.openl.rules.vm.SimpleRulesVM;

public class OpenLBuilder implements IOpenLBuilder {

    @Override
    public OpenL build(IUserContext ucxt) {
        var librariesRegistry = new LibrariesRegistry();
        var castFactory = new CastFactory();
        castFactory.setMethodFactory(librariesRegistry.asMethodFactory());
        var methodFactory = librariesRegistry.asMethodFactory2();
        var varFactory = librariesRegistry.asVarFactory();
        var typeFactory = new TypeResolver(ucxt.getUserClassLoader());

        OpenL op = new OpenL();
        op.setParser(new Parser());
        op.setBinder(new Binder(methodFactory, castFactory, varFactory, typeFactory, op));
        op.setVm(new SimpleRulesVM());
        return op;
    }

}
