package org.openl.rules.lang.xls.prebind;

import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenSchema;

/**
 * ModuleOpenClass for prebinding that uses {@link IPrebindHandler} to convert
 * methods and fields to some invokable(after prebinding they are not invokable)
 * methods/fields before adding.
 * 
 * @author PUdalau
 */
public class XlsLazyModuleOpenClass extends XlsModuleOpenClass {
    private IPrebindHandler prebindHandler;

    public XlsLazyModuleOpenClass(IOpenSchema schema,
            String name,
            XlsMetaInfo metaInfo,
            OpenL openl,
            IPrebindHandler prebindHandler) {
        this(schema, name, metaInfo, openl, prebindHandler, null);
    }

    public XlsLazyModuleOpenClass(IOpenSchema schema,
            String name,
            XlsMetaInfo metaInfo,
            OpenL openl,
            IPrebindHandler prebindHandler,
            Set<CompiledOpenClass> usingModules) {
        super(schema, name, metaInfo, openl, usingModules);
        this.prebindHandler = prebindHandler;
    }

    @Override
    public void addMethod(IOpenMethod method) {
        if (prebindHandler != null) {
            super.addMethod(prebindHandler.processMethodAdded(method, this));
        } else {
            super.addMethod(method);
        }
    }

    @Override
    public void addField(IOpenField field) {
        if (prebindHandler != null) {
            super.addField(prebindHandler.processFieldAdded(field, this));
        } else {
            super.addField(field);
        }
    }
}
