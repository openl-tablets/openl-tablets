/*
 * Created on Sep 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.openl.IOpenSourceCodeModule;
import org.openl.conf.IConfigurableResourceContext;
import org.openl.rules.dt.DTLoader;
import org.openl.rules.extension.load.IExtensionLoader;
import org.openl.rules.extension.load.NameConventionLoaderFactory;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.OpenlSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.lang.xls.syntax.WorksheetSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.FileSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.ParsedCode;
import org.openl.syntax.impl.SyntaxError;
import org.openl.syntax.impl.TokenizerParser;
import org.openl.syntax.impl.URLSourceCodeModule;
import org.openl.util.Log;
import org.openl.util.PathTool;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;

/**
 * @author snshor
 * 
 */
public class XlsLoader implements IXlsTableNames, ITableNodeTypes {

    @SuppressWarnings("unused")
    private static class ParsedXls extends ParsedCode {
        /**
         * @param topnode
         * @param source
         * @param syntaxErrors
         */
        public ParsedXls(ISyntaxNode topnode, IOpenSourceCodeModule source, ISyntaxError[] syntaxErrors) {
            super(topnode, source, syntaxErrors);
        }
    }

    private static Map<String, String> tableHeaders;

    private static final String[][] headerMapping = { 
            { DECISION_TABLE, XLS_DT }, 
            { DECISION_TABLE2, XLS_DT },
            { SPREADSHEET_TABLE, XLS_SPREADSHEET }, 
            { SPREADSHEET_TABLE2, XLS_SPREADSHEET },
            { TBASIC_TABLE, XLS_TBASIC }, 
            { TBASIC_TABLE2, XLS_TBASIC }, 
            { COLUMN_MATCH, XLS_COLUMN_MATCH },
            { DATA_TABLE, XLS_DATA }, 
            { DATATYPE_TABLE, XLS_DATATYPE }, 
            { METHOD_TABLE, XLS_METHOD },
            { METHOD_TABLE2, XLS_METHOD }, 
            { ENVIRONMENT_TABLE, XLS_ENVIRONMENT },
            { TEST_METHOD_TABLE, XLS_TEST_METHOD }, 
            { RUN_METHOD_TABLE, XLS_RUN_METHOD },
            { PERSISTENCE_TABLE, XLS_PERSISTENT },
            { PROPERTY_TABLE, XLS_PROPERTIES }};

    private List<ISyntaxNode> nodesList = new ArrayList<ISyntaxNode>();

    private List<ISyntaxError> errors = new ArrayList<ISyntaxError>();

    private OpenlSyntaxNode openl;

    private String allImportString;

    private IConfigurableResourceContext ucxt;

    private String searchPath;

    private IdentifierNode vocabulary;

    private List<IdentifierNode> extensionNodes = new ArrayList<IdentifierNode>();

    private HashSet<String> preprocessedWorkBooks = new HashSet<String>();

    public static Map<String, String> tableHeaders() {
        if (tableHeaders == null) {
            tableHeaders = new HashMap<String, String>();
            for (int i = 0; i < headerMapping.length; i++) {
                tableHeaders.put(headerMapping[i][0], headerMapping[i][1]);
            }
        }
        return tableHeaders;
    }

    /**
     * @param ucxt
     * @param string
     */
    public XlsLoader(IConfigurableResourceContext ucxt, String searchPath) {
        this.ucxt = ucxt;
        this.searchPath = searchPath;
    }

    public void addError(ISyntaxError error) {
        errors.add(error);
    }

    public void addNode(ISyntaxNode node) {
        nodesList.add(node);
    }

    public void addExtensionNode(IdentifierNode node) {
        extensionNodes.add(node);
    }

    public Set<String> getPreprocessedWorkBooks() {
        return preprocessedWorkBooks;
    }

    /**
     * @param include
     * @return
     */
    private IOpenSourceCodeModule findInclude(String include) {
        if (searchPath == null) {
            searchPath = "include/";
        }
        String[] path = StringTool.tokenize(searchPath, ";");

        for (int i = 0; i < path.length; i++) {
            try {
                String p = PathTool.mergePath(path[i], include);
                URL url = ucxt.findClassPathResource(p);
                if (url != null) {
                    return new URLSourceCodeModule(url);
                }

                File f = ucxt.findFileSystemResource(p);
                if (f != null) {
                    return new FileSourceCodeModule(f, null);
                }

                // let's try simple concat and use url
                String u2 = path[i] + include;
                URL xurl = new URL(u2);

                // URLConnection uc;
                InputStream is = null;
                try {
                    is = xurl.openStream();
                } catch (IOException iox) {
                    return null;
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
                return new URLSourceCodeModule(xurl);
            } catch (Throwable t) {
            }
        }
        return null;
    }

    private List<WorkbookSyntaxNode> workbookNodes = new ArrayList<WorkbookSyntaxNode>();

    public IParsedCode parse(IOpenSourceCodeModule source) {

        preprocessWorkbook(source);

        // if (openl == null)
        // {
        // addError(new SyntaxError((ILocation) null, "No openl has been
        // defined",
        // null, source));
        // }

        // TableSyntaxNode[] nodes = nodesList.toArray(new
        // TableSyntaxNode[nodesList.size()]);
        return new ParsedCode(new XlsModuleSyntaxNode(workbookNodes.toArray(new WorkbookSyntaxNode[0]), source, openl,
                vocabulary, allImportString, extensionNodes), source, errors.toArray(new ISyntaxError[0]));
    }

    private void preprocessEnvironmentTable(TableSyntaxNode tsn, XlsSheetSourceCodeModule source) {

        IGridTable table = tsn.getTable().getGridTable();
        ILogicalTable lt = LogicalTable.logicalTable(table);

        int h = lt.getLogicalHeight();

        for (int i = 1; i < h; i++) {
            ILogicalTable row = lt.getLogicalRow(i);

            String name = row.getLogicalColumn(0).getGridTable().getCell(0, 0).getStringValue();

            if (LANG_PROPERTY.equals(name)) {
                preprocessOpenlTable(row.getGridTable(), source);
            } else if (INCLUDE_TABLE.equals(name)) {
                preprocessIncludeTable(tsn, row.getGridTable(), source);
            } else if (IMPORT_PROPERTY.equals(name)) {
                preprocessImportTable(row.getGridTable(), source);
            } else if (VOCABULARY_PROPERTY.equals(name)) {
                preprocessVocabularyTable(row.getGridTable(), source);
            } else if (name == null || StringUtils.isEmpty(name) || DTLoader.isValidCommentHeader(name)) {
                ;// ignore comment
            } else {
                // TODO: why do we consider everything else an extension?
                IExtensionLoader loader = NameConventionLoaderFactory.INSTANCE.getLoader(name);
                if (loader != null) {
                    loader.process(this, tsn, table, source);
                }
            }
        }

    }

    private void preprocessImportTable(IGridTable table, XlsSheetSourceCodeModule sheetSource) {

        int h = table.getLogicalHeight();

        String concat = null;

        for (int i = 0; i < h; i++) {
            String imports = table.getCell(1, i).getStringValue();
            if (imports == null) {
                continue;
            }
            imports = imports.trim();
            if (imports.length() == 0) {
                continue;
                // IOpenSourceCodeModule src = new
                // GridCellSourceCodeModule(table.getLogicalRegion(1, i, 1,
                // 1).getGridTable());
            }

            if (concat == null) {
                concat = imports;
            } else {
                concat += ";" + imports;
            }

        }

        allImportString = concat;

    }

    private void preprocessIncludeTable(TableSyntaxNode tsn, IGridTable table, XlsSheetSourceCodeModule sheetSource) {

        int h = table.getLogicalHeight();

        for (int i = 0; i < h; i++) {
            String include = table.getCell(1, i).getStringValue();
            if (include == null) {
                continue;
            }
            include = include.trim();
            if (include.length() == 0) {
                continue;
            }
            IOpenSourceCodeModule src = null;
            if (include.startsWith("<")) {
                src = findInclude(StringTool.openBrackets(include, '<', '>', "")[0]);

                if (src == null) {
                    ISyntaxError se = new SyntaxError(null, "Include " + include + " not found", null,
                            new GridCellSourceCodeModule(table.getLogicalRegion(1, i, 1, 1).getGridTable()));
                    addError(se);
                    tsn.addError(se);
                    continue;
                }
            } else {
                // if (source.workbook instanceof FileSourceCodeModule)
                // {
                // src = ((FileSourceCodeModule) source.workbook)
                // .getRelativeSourceCodeModule(include);
                // } else
                {
                    try {
                        String newURL = PathTool.mergePath(sheetSource.getWorkbookSource().getUri(0), include);
                        src = new URLSourceCodeModule(new URL(newURL));
                    } catch (Throwable t) {
                        ISyntaxError se = new SyntaxError(null, "Include " + include + " not found", t,
                                new GridCellSourceCodeModule(table.getLogicalRegion(1, i, 1, 1).getGridTable()));
                        addError(se);
                        tsn.addError(se);
                        continue;
                    }
                }

            }
            try {
                preprocessWorkbook(src);
            } catch (Throwable t) {
                ISyntaxError se = new SyntaxError(null, "Include " + include + " not found", t,
                        new GridCellSourceCodeModule(table.getLogicalRegion(1, i, 1, 1).getGridTable()));
                addError(se);
                tsn.addError(se);
                continue;
            }

        }

    }

    /**
     * @param table
     * @param source
     * @param parsedHeader
     */
    private void preprocessOpenlTable(IGridTable table, XlsSheetSourceCodeModule source) {

        String openlName = table.getCell(1, 0).getStringValue();

        setOpenl(new OpenlSyntaxNode(openlName, new GridLocation(table), source));

    }

    private TableSyntaxNode preprocessTable(IGridTable table, XlsSheetSourceCodeModule source) {
        // String header = table.getStringValue(0, 0);

        GridCellSourceCodeModule src = new GridCellSourceCodeModule(table);

        IdentifierNode headerToken = TokenizerParser.firstToken(src, " \n\r");

        // if (parsedHeader.length == 0)
        // return;

        String header = headerToken.getIdentifier();

        HeaderSyntaxNode headerNode = new HeaderSyntaxNode(src, headerToken);

        String xls_type = tableHeaders().get(header);

        if (xls_type == null) {
            xls_type = XLS_OTHER;
        }

        TableSyntaxNode tsn = new TableSyntaxNode(xls_type, new GridLocation(table), source, table, headerNode);

        if (header.equals(ENVIRONMENT_TABLE)) {
            preprocessEnvironmentTable(tsn, source);
        }

        addNode(tsn);

        return tsn;

    }

    private void preprocessVocabularyTable(IGridTable table, XlsSheetSourceCodeModule source) {
        String vocabularyStr = table.getCell(1, 0).getStringValue();
        setVocabulary(new IdentifierNode(VOCABULARY_PROPERTY, new GridLocation(table), vocabularyStr, source));
    }

    private WorkbookSyntaxNode preprocessWorkbook(IOpenSourceCodeModule source) {
        String uri = source.getUri(0);
        if (preprocessedWorkBooks.contains(uri)) {
            return null;
        }
        preprocessedWorkBooks.add(uri);

        InputStream is = null;
        try {
            is = source.getByteStream();
            Workbook workbook = WorkbookFactory.create(is);
            XlsWorkbookSourceCodeModule workbookSourceModule = new XlsWorkbookSourceCodeModule(source, workbook);

            int nsheets = workbook.getNumberOfSheets();

            WorksheetSyntaxNode[] sheetNodes = new WorksheetSyntaxNode[nsheets];

            for (int i = 0; i < nsheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);                          

                XlsSheetSourceCodeModule sheetSource = preprocessSheet(sheet, workbookSourceModule);

                IGridTable[] tables = getAllGridTables(sheetSource);
                
                List<TableSyntaxNode> tableNodes = new ArrayList<TableSyntaxNode>();
                
                for (int j = 0; j < tables.length; j++) {
                    TableSyntaxNode tsn = preprocessTable(tables[j], sheetSource);
                    tableNodes.add(tsn);
                }
                
                sheetNodes[i] = new WorksheetSyntaxNode(tableNodes.toArray(new TableSyntaxNode[0]) , sheetSource);

            }
            WorkbookSyntaxNode workbookNode = new WorkbookSyntaxNode(sheetNodes, workbookSourceModule);
            workbookNodes.add(workbookNode);
            return workbookNode;
        } catch (Exception e) {
            e.printStackTrace();
            throw RuntimeExceptionWrapper.wrap(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }

            } catch (Throwable e) {
                Log.error("Error trying close input stream:", e);
                return null;
            }
        }
    }
    
    /**
     * Gets all grid tables from the sheet.
     * @param sheetSource
     * @return
     */
    private IGridTable[] getAllGridTables(XlsSheetSourceCodeModule sheetSource) {
        XlsSheetGridModel xlsGrid = new XlsSheetGridModel(sheetSource);
        IGridTable[] tables = new GridSplitter(xlsGrid).split();
        return tables;
    }
    
    private XlsSheetSourceCodeModule preprocessSheet(Sheet sheet, XlsWorkbookSourceCodeModule workbookSourceModule) {
        String sheetName = sheet.getSheetName();
        return new XlsSheetSourceCodeModule(sheet, sheetName, workbookSourceModule);
    }

    void setOpenl(OpenlSyntaxNode openl) {
        if (this.openl == null) {
            this.openl = openl;
        } else {
            if (!this.openl.getOpenlName().equals(openl.getOpenlName()))
                addError(new SyntaxError(openl, "Only one openl statement is allowed", null));
        }
    }

    void setVocabulary(IdentifierNode vocabulary) {
        if (this.vocabulary == null) {
            this.vocabulary = vocabulary;
        } else {
            addError(new SyntaxError(vocabulary, "Only one vocabulary is allowed", null));
        }
    }

}