package org.openl.rules.lang.xls;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.SpreadsheetHeaderNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.util.Log;
import org.openl.util.StringTool;
import org.openl.util.text.ILocation;
import org.openl.util.text.TextInterval;

public abstract class XlsHelper {

    private static Map<String, String> tableHeaders;
    static {

        if (XlsHelper.tableHeaders == null) {
            XlsHelper.tableHeaders = new HashMap<String, String>();
            XlsHelper.tableHeaders.put(IXlsTableNames.CONSTANTS, XlsNodeTypes.XLS_CONSTANTS.toString());
            
            XlsHelper.tableHeaders.put(IXlsTableNames.DECISION_TABLE, XlsNodeTypes.XLS_DT.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.DECISION_TABLE2, XlsNodeTypes.XLS_DT.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.SIMPLE_DECISION_TABLE, XlsNodeTypes.XLS_DT.toString());
            
            XlsHelper.tableHeaders.put(IXlsTableNames.SMART_DECISION_TABLE, XlsNodeTypes.XLS_DT.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.SIMPLE_DECISION_LOOKUP, XlsNodeTypes.XLS_DT.toString());
            XlsHelper.tableHeaders.put(IXlsTableNames.SMART_DECISION_LOOKUP, XlsNodeTypes.XLS_DT.toString());

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

    public static String getModuleName(XlsModuleSyntaxNode node) {

        String uri = node.getModule().getUri();

        try {
            URL url = new URL(uri);
            String file = url.getFile();
            int index = file.lastIndexOf('/');

            file = index < 0 ? file : file.substring(index + 1);

            index = file.lastIndexOf('.');

            if (index > 0) {
                file = file.substring(0, index);
            }

            return makeJavaIdentifier(file);

        } catch (MalformedURLException e) {
            if(VirtualSourceCodeModule.SOURCE_URI.equals(uri)){
                return "VirtualModule";
            }else{
                Log.error("Error URI to name conversion", e);
                return "UndefinedXlsType";
            }
        }
    }


    private static String makeJavaIdentifier(String src) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (i == 0) {
                buf.append(Character.isJavaIdentifierStart(c) ? c : '_');
            } else {
                buf.append(Character.isJavaIdentifierPart(c) ? c : '_');
            }
        }

        return buf.toString();
    }

    public static TableSyntaxNode createTableSyntaxNode(IGridTable table, XlsSheetSourceCodeModule source) throws
                                                                                                           OpenLCompilationException {
        GridCellSourceCodeModule src = new GridCellSourceCodeModule(table);
        IdentifierNode[] headerTokens = Tokenizer.tokenize(src, " \n\r");
        if (headerTokens.length == 0){
            headerTokens = new IdentifierNode[]{Tokenizer.firstToken(src, " \n\r")};
        }
        IdentifierNode headerToken = headerTokens[0];
        String header = headerTokens[0].getIdentifier();
        String xls_type = tableHeaders.get(header);

        if (xls_type == null) {
            xls_type = XlsNodeTypes.XLS_OTHER.toString();
        }
        
        //Collect token concatenation
        List<String> collectParameters = new ArrayList<String>();
        boolean isCollect = false;
        if (header.equals(IXlsTableNames.SIMPLE_DECISION_TABLE) || header.equals(IXlsTableNames.SMART_DECISION_TABLE)
                || header.equals(IXlsTableNames.SIMPLE_DECISION_LOOKUP) || header.equals(IXlsTableNames.SMART_DECISION_LOOKUP)){
            if (headerTokens.length > 1 && headerTokens[1].getIdentifier().equals(IXlsTableNames.COLLECT)){
                isCollect = true;
                if (headerTokens.length > 2 && headerTokens[2].getIdentifier().equals(IXlsTableNames.COLLECT_AS)){
                    int i = 3;
                    collectParameters.add(headerTokens[i].getIdentifier());
                    if (i < headerTokens.length && headerTokens[i + 1].getIdentifier().equals(IXlsTableNames.COLLECT_AND)){
                        i = i + 2;
                        collectParameters.add(headerTokens[i].getIdentifier());
                    }
                    ILocation location = new TextInterval(headerToken.getLocation().getStart(), headerTokens[i].getLocation().getEnd());
                    headerToken = new IdentifierNode(headerToken.getType(), location, header, headerToken.getModule());
                }else{
                    ILocation location = new TextInterval(headerToken.getLocation().getStart(), headerTokens[1].getLocation().getEnd());
                    headerToken = new IdentifierNode(headerToken.getType(), location, header, headerToken.getModule());
                }
            }
        }

        HeaderSyntaxNode headerNode;
        if (XlsNodeTypes.XLS_SPREADSHEET.toString().equals(xls_type)) {
            headerNode = new SpreadsheetHeaderNode(src, headerToken);
        } else {
            headerNode = new HeaderSyntaxNode(src, headerToken, isCollect, collectParameters.toArray(new String[]{}));
        }

        GridLocation pos = new GridLocation(table);
        return new TableSyntaxNode(xls_type, pos, source, table, headerNode);
    }
}
