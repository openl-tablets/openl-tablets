package org.openl.rules.extension.instantiation;

import java.util.List;

import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.project.model.Module;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.util.CollectionUtils;
import org.openl.util.tree.ITreeElement;

public interface IExtensionDescriptor {
    String getOpenLName();

    List<Module> getInternalModules(Module module);

    IOpenSourceCodeModule getSourceCode(Module module);

    String getUrlForModule(Module module);

    CollectionUtils.Predicate<ITreeElement> getUtilityTablePredicate(XlsModuleSyntaxNode moduleSyntaxNode);
}
