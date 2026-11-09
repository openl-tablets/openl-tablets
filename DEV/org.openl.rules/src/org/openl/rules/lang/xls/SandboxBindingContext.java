package org.openl.rules.lang.xls;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.binding.impl.BindingContext;
import org.openl.binding.impl.module.ModuleSpecificType;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.data.IDataBase;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * A binding context that binds one table against a compiled module and leaves the module as it was.
 *
 * <p>It finds the types, methods and fields of the module, as the context the module was compiled in does. What the
 * bind adds is kept in this context: the types it defines, the keys of the tables it binds, the tables it registers,
 * and the errors and messages it reports. The calls of {@code msg} it binds are not kept in the module.
 *
 * <p>The property tables of the module and of its categories are found among the tables of the module, so the table
 * inherits the properties it inherits in the module.
 */
final class SandboxBindingContext extends RulesModuleBindingContext {

    private final RulesModuleBindingContext compiled;
    @Getter
    private final IDataBase dataBase;
    private final Map<String, IOpenClass> moduleTypes = new HashMap<>();

    /**
     * Opens a sandbox over a compiled module.
     *
     * @param compiled the context the module was compiled in
     * @param module   the compiled module
     */
    SandboxBindingContext(RulesModuleBindingContext compiled, XlsModuleOpenClass module) {
        super(rootOf(compiled), module, false);
        this.compiled = compiled;
        this.dataBase = new SandboxDataBase(module.getDataBase());
    }

    /** A root context of its own, so the local variables and the errors of the bind stay out of the module context. */
    private static BindingContext rootOf(RulesModuleBindingContext compiled) {
        var openl = compiled.getOpenL();
        var root = new BindingContext(openl.getBinder(), JavaOpenClass.VOID, openl);
        root.setExternalParams(compiled.getExternalParams());
        return root;
    }

    @Override
    public IOpenClass addType(IOpenClass type) {
        if (type instanceof ModuleSpecificType) {
            moduleTypes.put(type.getName(), type);
            return type;
        }
        return super.addType(type);
    }

    @Override
    public @Nullable IOpenClass findType(String typeName) {
        var type = moduleTypes.get(typeName);
        return type != null ? type : super.findType(typeName);
    }

    @Override
    public @Nullable TableSyntaxNode getTableSyntaxNode(String key) {
        var table = super.getTableSyntaxNode(key);
        return table != null ? table : compiled.getTableSyntaxNode(key);
    }
}
