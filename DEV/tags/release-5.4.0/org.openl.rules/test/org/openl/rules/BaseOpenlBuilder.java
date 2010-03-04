package org.openl.rules;

import org.openl.conf.UserContext;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;

/**
 * 
 * @author DLiauchuk
 *
 */
public class BaseOpenlBuilder {
    
    private XlsModuleSyntaxNode xsn;
    
    public BaseOpenlBuilder() {        
    }
    
    protected void buildXlsModuleSyntaxNode(String fileToBuildWrapper) {        
        OpenClassJavaWrapper wrapper = getJavaWrapper(fileToBuildWrapper);
        XlsMetaInfo xmi = (XlsMetaInfo) wrapper.getOpenClassWithErrors().getMetaInfo();
        xsn = xmi.getXlsModuleNode();        
    }
    
    private OpenClassJavaWrapper getJavaWrapper(String fileToBuildWrapper) {
        UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), ".");
        OpenClassJavaWrapper wrapper = OpenClassJavaWrapper.createWrapper("org.openl.xls", ucxt, fileToBuildWrapper);
        return wrapper;
    }
    
    protected TableSyntaxNode findTable(String tableName, TableSyntaxNode[] tsns) {
        TableSyntaxNode result = null;
        for (TableSyntaxNode tsn : tsns) {
            if (tableName.equals(tsn.getDisplayName())) {
                result = tsn;   
            }
        }
        return result;
    }

    protected TableSyntaxNode[] getTableSyntaxNodes(String fileToBuildWrapper) {
        buildXlsModuleSyntaxNode(fileToBuildWrapper);
        XlsModuleSyntaxNode module = xsn;
        TableSyntaxNode[] tsns = module.getXlsTableSyntaxNodes();
        return tsns;
    }
    
    public XlsModuleSyntaxNode getModuleSuntaxNode() {
        return xsn;
    }

}
