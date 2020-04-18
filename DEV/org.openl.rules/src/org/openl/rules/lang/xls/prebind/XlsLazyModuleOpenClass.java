package org.openl.rules.lang.xls.prebind;

import java.util.Set;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.dependency.CompiledDependency;
import org.openl.rules.data.IDataBase;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

/**
 * ModuleOpenClass for prebinding that uses {@link IPrebindHandler} to convert methods and fields to some
 * invokable(after prebinding they are not invokable) methods/fields before adding.
 *
 * @author PUdalau
 */
public class XlsLazyModuleOpenClass extends XlsModuleOpenClass {
    private final IPrebindHandler prebindHandler;

    public XlsLazyModuleOpenClass(String name,
            XlsMetaInfo metaInfo,
            OpenL openl,
            IDataBase dbase,
            Set<CompiledDependency> usingModules,
            ClassLoader classLoader,
            IBindingContext bindingContext,
            IPrebindHandler prebindHandler) {
        super(name, metaInfo, openl, dbase, usingModules, classLoader, bindingContext);
        this.prebindHandler = prebindHandler;
    }

    @Override
    public void addMethod(IOpenMethod method) {
        if (prebindHandler != null) {
            // Add this module methods
            super.addMethod(prebindHandler.processMethodAdded(method, this));
        } else {
            // Add methods from dependencies
            super.addMethod(method);
        }
    }

    @Override
    public void addField(IOpenField field) {
        if (prebindHandler != null) {
            // Add this module fields
            super.addField(prebindHandler.processFieldAdded(field, this));
        } else {
            // Add fields from dependencies
            super.addField(field);
        }
    }

    /*@Override
    protected IOpenMethod decorateWrapper(IOpenMethod method) {
        if (method instanceof OpenMethodDispatcher) {
            return super.decorateWrapper(method);
        }
        if (method instanceof OpenMethodWrapper) {
            OpenMethodWrapper wrapper = (OpenMethodWrapper) method;
            if (wrapper.getDelegate() instanceof ILazyMember) {
                return super.decorateWrapper(method);
            }
        }
        return method;
    }*/
}
