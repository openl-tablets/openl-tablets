/**
 * Created Feb 16, 2007
 */
package org.openl.rules.table;

import java.net.URISyntaxException;
import java.net.URL;

import junit.framework.TestCase;

import org.openl.OpenL;
import org.openl.engine.OpenLManager;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 * 
 */
public class TestMetaInfo extends TestCase {

    public void testMeta() throws URISyntaxException {
        
        URL url = this.getClass().getClassLoader().getResource("org/openl/rules/table/TestMeta.xls");
        OpenL openl = OpenL.getInstance(OpenL.OPENL_JAVA_RULE_NAME);

        URLSourceCodeModule src = new URLSourceCodeModule(url);
        XlsModuleOpenClass module = (XlsModuleOpenClass) OpenLManager.compileModule(openl, src);

        TableSyntaxNode[] nodes = nodes(module);

        for (int i = 0; i < nodes.length; i++) {
            IGridTable table = nodes[i].getGridTable();
            int w = table.getWidth();
            int h = table.getHeight();

            for (int j = 0; j < w; j++) {
                for (int k = 0; k < h; k++) {
                    ICell cell = table.getCell(j, k);
                    String value = cell.getStringValue();
                    CellMetaInfo mi = cell.getMetaInfo();
                    if (mi == null)
                        continue;
                    String paramName = mi.getParamName();
                    IOpenClass ioc = mi.getDataType();
                    System.out.println("Value = " + value + ", paramName = " + paramName + " , type = "
                            + (ioc == null ? null : ioc.getDisplayName(0)));
                }
            }

        }
    }

    private TableSyntaxNode[] nodes(XlsModuleOpenClass xls) {
        return ((XlsMetaInfo) xls.getMetaInfo()).getXlsModuleNode().getXlsTableSyntaxNodes();
    }
}
