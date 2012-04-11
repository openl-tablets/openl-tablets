/*
 * Created on Sep 23, 2003 Developed by Intelligent ChoicePoint Inc. 2003
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
import org.openl.OpenL;
import org.openl.conf.IConfigurableResourceContext;
import org.openl.message.OpenLMessagesUtils;
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
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.impl.ParsedCode;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.SyntaxError;
import org.openl.syntax.impl.TokenizerParser;
import org.openl.util.Log;
import org.openl.util.PathTool;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;

/**
 * @author snshor
 * 
 */
public class XlsLoader {

    private static final String[][] headerMapping = {
            { IXlsTableNames.DECISION_TABLE, ITableNodeTypes.XLS_DT },
            { IXlsTableNames.DECISION_TABLE2, ITableNodeTypes.XLS_DT },
            { IXlsTableNames.SPREADSHEET_TABLE, ITableNodeTypes.XLS_SPREADSHEET },
            { IXlsTableNames.SPREADSHEET_TABLE2, ITableNodeTypes.XLS_SPREADSHEET },
            { IXlsTableNames.TBASIC_TABLE, ITableNodeTypes.XLS_TBASIC },
            { IXlsTableNames.TBASIC_TABLE2, ITableNodeTypes.XLS_TBASIC },
            { IXlsTableNames.COLUMN_MATCH, ITableNodeTypes.XLS_COLUMN_MATCH },
            { IXlsTableNames.DATA_TABLE, ITableNodeTypes.XLS_DATA },
            { IXlsTableNames.DATATYPE_TABLE, ITableNodeTypes.XLS_DATATYPE },
            { IXlsTableNames.METHOD_TABLE, ITableNodeTypes.XLS_METHOD },
            { IXlsTableNames.METHOD_TABLE2, ITableNodeTypes.XLS_METHOD },
            { IXlsTableNames.ENVIRONMENT_TABLE, ITableNodeTypes.XLS_ENVIRONMENT },
            { IXlsTableNames.TEST_METHOD_TABLE, ITableNodeTypes.XLS_TEST_METHOD },
            { IXlsTableNames.RUN_METHOD_TABLE, ITableNodeTypes.XLS_RUN_METHOD },
            { IXlsTableNames.PERSISTENCE_TABLE, ITableNodeTypes.XLS_PERSISTENT },
            { IXlsTableNames.PROPERTY_TABLE, ITableNodeTypes.XLS_PROPERTIES } };

    private static Map<String, String> tableHeaders;

    static {

        if (tableHeaders == null) {
            tableHeaders = new HashMap<String, String>();

            for (int i = 0; i < headerMapping.length; i++) {
                tableHeaders.put(headerMapping[i][0], headerMapping[i][1]);
            }
        }
    }

    private String allImportString;

    private String searchPath;

    private IConfigurableResourceContext ucxt;

    private OpenlSyntaxNode openl;

    private IdentifierNode vocabulary;

    private List<ISyntaxNode> nodesList = new ArrayList<ISyntaxNode>();

    private List<ISyntaxError> errors = new ArrayList<ISyntaxError>();

    private List<IdentifierNode> extensionNodes = new ArrayList<IdentifierNode>();

    private HashSet<String> preprocessedWorkBooks = new HashSet<String>();

    private List<WorkbookSyntaxNode> workbookNodes = new ArrayList<WorkbookSyntaxNode>();

    public XlsLoader(IConfigurableResourceContext ucxt, String searchPath) {

        this.ucxt = ucxt;
        this.searchPath = searchPath;
    }

    public static Map<String, String> getTableHeaders() {

        return tableHeaders;
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
                OpenLMessagesUtils.addWarn(String.format("Cannot find '%s' ()", include, t.getMessage()));
            }
        }

        return null;
    }

    public IParsedCode parse(IOpenSourceCodeModule source) {

        preprocessWorkbook(source);

        return new ParsedCode(new XlsModuleSyntaxNode(workbookNodes.toArray(new WorkbookSyntaxNode[0]),
            source,
            openl,
            vocabulary,
            allImportString,
            extensionNodes), source, errors.toArray(new ISyntaxError[0]));
    }

    private void preprocessEnvironmentTable(TableSyntaxNode tableSyntaxNode, XlsSheetSourceCodeModule source) {

        IGridTable table = tableSyntaxNode.getTable().getGridTable();
        ILogicalTable logicalTable = LogicalTable.logicalTable(table);

        int height = logicalTable.getLogicalHeight();

        for (int i = 1; i < height; i++) {
            ILogicalTable row = logicalTable.getLogicalRow(i);

            String name = row.getLogicalColumn(0).getGridTable().getCell(0, 0).getStringValue();

            if (IXlsTableNames.LANG_PROPERTY.equals(name)) {
                preprocessOpenlTable(row.getGridTable(), source);
            } else if (IXlsTableNames.INCLUDE_TABLE.equals(name)) {
                preprocessIncludeTable(tableSyntaxNode, row.getGridTable(), source);
            } else if (IXlsTableNames.IMPORT_PROPERTY.equals(name)) {
                preprocessImportTable(row.getGridTable(), source);
            } else if (IXlsTableNames.VOCABULARY_PROPERTY.equals(name)) {
                preprocessVocabularyTable(row.getGridTable(), source);
            } else if (name == null || StringUtils.isEmpty(name) || DTLoader.isValidCommentHeader(name)) {
                ;// ignore comment
            } else {
                // TODO: why do we consider everything else an extension?
                IExtensionLoader loader = NameConventionLoaderFactory.INSTANCE.getLoader(name);

                if (loader != null) {
                    loader.process(this, tableSyntaxNode, table, source);
                }
            }
        }

    }

    private void preprocessImportTable(IGridTable table, XlsSheetSourceCodeModule sheetSource) {

        int height = table.getLogicalHeight();

        String concat = null;

        for (int i = 0; i < height; i++) {

            String imports = table.getCell(1, i).getStringValue();

            if (imports == null) {
                continue;
            }
            imports = imports.trim();

            if (imports.length() == 0) {
                continue;
            }

            if (concat == null) {
                concat = imports;
            } else {
                concat += ";" + imports;
            }
        }

        allImportString = concat;
    }

    private void preprocessIncludeTable(TableSyntaxNode tableSyntaxNode,
                                        IGridTable table,
                                        XlsSheetSourceCodeModule sheetSource) {

        int height = table.getLogicalHeight();

        for (int i = 0; i < height; i++) {

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

                    ISyntaxError se = new SyntaxError(null,
                        "Include " + include + " not found",
                        null,
                        new GridCellSourceCodeModule(table.getLogicalRegion(1, i, 1, 1).getGridTable()));

                    addError(se);
                    tableSyntaxNode.addError(se);

                    OpenLMessagesUtils.addError(se.getMessage());

                    continue;
                }
            } else {

                try {
                    String newURL = PathTool.mergePath(sheetSource.getWorkbookSource().getUri(0), include);
                    src = new URLSourceCodeModule(new URL(newURL));
                } catch (Throwable t) {
                    ISyntaxError se = new SyntaxError(null,
                        "Include " + include + " not found",
                        t,
                        new GridCellSourceCodeModule(table.getLogicalRegion(1, i, 1, 1).getGridTable()));
                    addError(se);
                    tableSyntaxNode.addError(se);
                    OpenLMessagesUtils.addError(se.getMessage());
                    continue;
                }
            }

            try {
                preprocessWorkbook(src);
            } catch (Throwable t) {
                ISyntaxError se = new SyntaxError(null,
                    "Include " + include + " not found",
                    t,
                    new GridCellSourceCodeModule(table.getLogicalRegion(1, i, 1, 1).getGridTable()));
                addError(se);
                tableSyntaxNode.addError(se);
                OpenLMessagesUtils.addError(se.getMessage());
                continue;
            }
        }
    }

    private void preprocessOpenlTable(IGridTable table, XlsSheetSourceCodeModule source) {

        String openlName = table.getCell(1, 0).getStringValue();

        setOpenl(new OpenlSyntaxNode(openlName, new GridLocation(table), source));
    }

    private TableSyntaxNode preprocessTable(IGridTable table, XlsSheetSourceCodeModule source) {

        GridCellSourceCodeModule src = new GridCellSourceCodeModule(table);

        IdentifierNode headerToken = TokenizerParser.firstToken(src, " \n\r");

        String header = headerToken.getIdentifier();

        HeaderSyntaxNode headerNode = new HeaderSyntaxNode(src, headerToken);

        String xls_type = getTableHeaders().get(header);

        if (xls_type == null) {
            xls_type = ITableNodeTypes.XLS_OTHER;
        }

        TableSyntaxNode tsn = new TableSyntaxNode(xls_type, new GridLocation(table), source, table, headerNode);

        if (header.equals(IXlsTableNames.ENVIRONMENT_TABLE)) {
            preprocessEnvironmentTable(tsn, source);
        }

        addNode(tsn);

        return tsn;
    }

    private void preprocessVocabularyTable(IGridTable table, XlsSheetSourceCodeModule source) {

        String vocabularyStr = table.getCell(1, 0).getStringValue();

        setVocabulary(new IdentifierNode(IXlsTableNames.VOCABULARY_PROPERTY,
            new GridLocation(table),
            vocabularyStr,
            source));
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

                sheetNodes[i] = new WorksheetSyntaxNode(tableNodes.toArray(new TableSyntaxNode[0]), sheetSource);
            }

            WorkbookSyntaxNode workbookNode = new WorkbookSyntaxNode(sheetNodes, workbookSourceModule);
            workbookNodes.add(workbookNode);

            return workbookNode;
        } catch (Exception e) {
            e.printStackTrace();
            OpenLMessagesUtils.addError(e.getMessage());
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
     * 
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

    private void setOpenl(OpenlSyntaxNode openl) {

        if (this.openl == null) {
            this.openl = openl;
        } else {
            if (!this.openl.getOpenlName().equals(openl.getOpenlName())) {
                SyntaxError error = new SyntaxError(openl, "Only one openl statement is allowed", null);
                OpenLMessagesUtils.addError(error.getMessage());
                addError(error);
            }
        }
    }

    private void setVocabulary(IdentifierNode vocabulary) {

        if (this.vocabulary == null) {
            this.vocabulary = vocabulary;
        } else {
            SyntaxError error = new SyntaxError(vocabulary, "Only one vocabulary is allowed", null);
            OpenLMessagesUtils.addError(error.getMessage());
            addError(error);
        }
    }

}