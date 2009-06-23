package org.openl.rules.liveexcel;

import java.util.List;

import org.openl.rules.extension.bind.IExtensionBinder;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.liveexcel.formula.ParsedDeclaredFunction;
import org.openl.rules.liveexcel.usermodel.LiveExcelWorkbook;
import org.openl.syntax.impl.IdentifierNode;

public class LiveexcelBinder implements IExtensionBinder {
    
    public void bind(XlsModuleOpenClass module, XlsModuleSyntaxNode moduleNode, IdentifierNode extension) {
        LiveExcelWorkbook wb = (LiveExcelWorkbook) ((XlsWorkbookSourceCodeModule) extension.getModule()).getWorkbook();
        List<String> names = wb.getUserDefinedFunctionNames();
        for (int j = 0; j < names.size(); j++) {
            String name = names.get(j);
            ParsedDeclaredFunction function = (ParsedDeclaredFunction) wb.getUserDefinedFunction(name);
            if (function != null) {
                LiveExcelMethod method = new LiveExcelMethod(new LiveExcelMethodHeader(function, null), function, wb);
                module.addMethod(method);
            }
        }
    }

    public String getNodeType() {
        return LiveexcelLoader.LIVEEXCEL_TYPE;
    }

}
