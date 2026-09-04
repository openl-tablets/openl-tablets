package org.openl.rules;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.engine.OpenLManager;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;

/**
 * The purpose of this class is to simplify compiling of OpenL objects in complex structured environments where context
 * is defined on top and must be propagated down without having to transfer many of the elements required to do the
 * validation and compilation.
 *
 * @author snshor
 */
@RequiredArgsConstructor
public class OpenlToolAdaptor {

    @Getter
    private final OpenL openl;
    @Getter
    @Setter
    private IOpenMethodHeader header;
    @Getter
    private final IBindingContext bindingContext;
    @Getter
    private final TableSyntaxNode tableSyntaxNode;

    public CompositeMethod makeMethod(IOpenSourceCodeModule src) {
        return OpenLManager.makeMethod(openl, src, header, bindingContext);
    }
}
