package org.openl.rules.liveexcel;

import java.util.List;

import org.openl.binding.impl.BindHelper;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.extension.bind.IExtensionBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.syntax.impl.IdentifierNode;

import com.exigen.le.LiveExcel;
import com.exigen.le.smodel.Function;

public class LiveexcelBinder implements IExtensionBinder {

    public void bind(XlsModuleOpenClass module, XlsModuleSyntaxNode moduleNode, IdentifierNode extension,
            RulesModuleBindingContext bindingContext) {
        LiveExcelIdentifierNode leIdentifierNode = (LiveExcelIdentifierNode) extension;
        LiveExcel liveExcel = leIdentifierNode.getLiveExcel();
        bindTypes(module, moduleNode, liveExcel, bindingContext);
        bindMethods(module, moduleNode, liveExcel, bindingContext);
    }

    public void bindTypes(XlsModuleOpenClass module, XlsModuleSyntaxNode moduleNode, LiveExcel liveExcel,
            RulesModuleBindingContext bindingContext) {
        LiveExcellDatatypeBinder datatypeBinder = new LiveExcellDatatypeBinder(liveExcel.getServiceModel().getTypes());
        datatypeBinder.preBind(module, bindingContext);
        datatypeBinder.bind(module, bindingContext);
    }

    public void bindMethods(XlsModuleOpenClass module, XlsModuleSyntaxNode moduleNode, LiveExcel liveExcel,
            RulesModuleBindingContext bindingContext) {
        List<Function> functions = liveExcel.getServiceModel().getFunctions();
        for (Function function : functions) {
            try {
                LiveExcelMethod method = new LiveExcelMethod(new LiveExcelMethodHeader(function, bindingContext),
                        function.getDeclaredName(), liveExcel);
                module.addMethod(method);
            } catch (Throwable t) {
                BindHelper.processError(String.format("Failed to bind LiveExcel method \"%s\"", function.getName()),
                        null, t, bindingContext);
            }
        }
    }

    public String getNodeType() {
        return LiveexcelLoader.LIVEEXCEL_TYPE;
    }

}
