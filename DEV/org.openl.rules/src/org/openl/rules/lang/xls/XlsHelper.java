package org.openl.rules.lang.xls;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.openl.binding.IBoundCode;
import org.openl.conf.UserContext;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.SpreadsheetHeaderNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.IOpenClass;
import org.openl.util.Log;
import org.openl.util.PropertiesLocator;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;

public abstract class XlsHelper {

    private static Map<String, String> tableHeaders;
    static {

        if (XlsHelper.tableHeaders == null) {
            XlsHelper.tableHeaders = new HashMap<String, String>();

            XlsHelper.tableHeaders.put(IXlsTableNames.DECISION_TABLE, XlsNodeTypes.XLS_DT.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.DECISION_TABLE2, XlsNodeTypes.XLS_DT.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.SIMPLE_DECISION_TABLE, XlsNodeTypes.XLS_DT.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.SIMPLE_DECISION_LOOKUP, XlsNodeTypes.XLS_DT.toString());

            // new dt2 implementation
            XlsHelper.tableHeaders.put(IXlsTableNames.DECISION_TABLE_2, XlsNodeTypes.XLS_DT2.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.DECISION_TABLE2_2, XlsNodeTypes.XLS_DT2.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.SIMPLE_DECISION_TABLE_2, XlsNodeTypes.XLS_DT2.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.SIMPLE_DECISION_LOOKUP_2, XlsNodeTypes.XLS_DT2.toString());

            XlsHelper.tableHeaders.put(IXlsTableNames.SPREADSHEET_TABLE, XlsNodeTypes.XLS_SPREADSHEET.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.SPREADSHEET_TABLE2, XlsNodeTypes.XLS_SPREADSHEET.toString());

            XlsHelper.tableHeaders.put(IXlsTableNames.TBASIC_TABLE, XlsNodeTypes.XLS_TBASIC.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.TBASIC_TABLE2, XlsNodeTypes.XLS_TBASIC.toString());

            XlsHelper.tableHeaders.put(IXlsTableNames.COLUMN_MATCH, XlsNodeTypes.XLS_COLUMN_MATCH.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.DATA_TABLE, XlsNodeTypes.XLS_DATA.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.DATATYPE_TABLE, XlsNodeTypes.XLS_DATATYPE.toString());

            XlsHelper.tableHeaders.put(IXlsTableNames.METHOD_TABLE, XlsNodeTypes.XLS_METHOD.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.METHOD_TABLE2, XlsNodeTypes.XLS_METHOD.toString());

            XlsHelper.tableHeaders.put(IXlsTableNames.ENVIRONMENT_TABLE, XlsNodeTypes.XLS_ENVIRONMENT.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.TEST_METHOD_TABLE, XlsNodeTypes.XLS_TEST_METHOD.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.TEST_TABLE, XlsNodeTypes.XLS_TEST_METHOD.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.RUN_METHOD_TABLE, XlsNodeTypes.XLS_RUN_METHOD.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.RUN_TABLE, XlsNodeTypes.XLS_RUN_METHOD.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.TABLE_PART, XlsNodeTypes.XLS_TABLEPART.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.PROPERTY_TABLE, XlsNodeTypes.XLS_PROPERTIES.toString());
        }
    }

    public static XlsMetaInfo getXlsMetaInfo(String srcFile) {

    	UserContext ucxt = new UserContext(Thread.currentThread().getContextClassLoader(), ".");
        String fileOrURL = PropertiesLocator.locateFileOrURL(srcFile, ucxt.getUserClassLoader(), new String[] { ucxt.getUserHome() });
        
        if (fileOrURL == null) {
            throw new RuntimeException("File " + srcFile + " is not found");
        }
        
        IOpenSourceCodeModule src = null;
        
        try {
            if (fileOrURL.indexOf(':') < 2) {
                src = new FileSourceCodeModule(fileOrURL, null);
            } else {
                src = new URLSourceCodeModule(new URL(fileOrURL));
            }
        } catch (MalformedURLException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
        
        IParsedCode pc = new XlsParser(ucxt).parseAsModule(src);
        IBoundCode bc = new XlsBinder(ucxt).bind(pc);
        IOpenClass ioc = bc.getTopNode().getType();
        
        return (XlsMetaInfo) ioc.getMetaInfo();
    }
    
    public static String getModuleName(XlsModuleSyntaxNode node) {

        String uri = node.getModule().getUri(0);

        try {
            URL url = new URL(uri);
            String file = url.getFile();
            int index = file.lastIndexOf('/');

            file = index < 0 ? file : file.substring(index + 1);

            index = file.lastIndexOf('.');

            if (index > 0) {
                file = file.substring(0, index);
            }

            return StringTool.makeJavaIdentifier(file);

        } catch (MalformedURLException e) {
            if(VirtualSourceCodeModule.SOURCE_URI.equals(uri)){
                return "VirtualModule";
            }else{
                Log.error("Error URI to name conversion", e);
                return "UndefinedXlsType";
            }
        }
    }


    public static TableSyntaxNode createTableSyntaxNode(IGridTable table, XlsSheetSourceCodeModule source) throws
                                                                                                           OpenLCompilationException {
        GridCellSourceCodeModule src = new GridCellSourceCodeModule(table);
        IdentifierNode headerToken = Tokenizer.firstToken(src, " \n\r");
        String header = headerToken.getIdentifier();
        String xls_type = tableHeaders.get(header);

        if (xls_type == null) {
            xls_type = XlsNodeTypes.XLS_OTHER.toString();
        }

        HeaderSyntaxNode headerNode;
        if (XlsNodeTypes.XLS_SPREADSHEET.toString().equals(xls_type)) {
            headerNode = new SpreadsheetHeaderNode(src, headerToken);
        } else {
            headerNode = new HeaderSyntaxNode(src, headerToken);
        }

        GridLocation pos = new GridLocation(table);
        return new TableSyntaxNode(xls_type, pos, source, table, headerNode);
    }
}
