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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.openl.conf.IConfigurableResourceContext;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.dt.DecisionTableHelper;
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
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.OffSetGridTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.code.impl.ParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.util.PathTool;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;

/**
 * @author snshor
 * 
 */
public class XlsLoader {
    
    private static Log LOG = LogFactory.getLog(XlsLoader.class);

    private static final String[][] headerMapping = { { IXlsTableNames.DECISION_TABLE, ITableNodeTypes.XLS_DT },
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

    private List<String> imports = new ArrayList<String>();

    private String searchPath;

    private IConfigurableResourceContext ucxt;

    private OpenlSyntaxNode openl;

    private IdentifierNode vocabulary;

    private List<ISyntaxNode> nodesList = new ArrayList<ISyntaxNode>();

    private List<SyntaxNodeException> errors = new ArrayList<SyntaxNodeException>();

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

    public void addError(SyntaxNodeException error) {

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
            imports,
            extensionNodes), source, errors.toArray(new SyntaxNodeException[0]));
    }

    private void preprocessEnvironmentTable(TableSyntaxNode tableSyntaxNode, XlsSheetSourceCodeModule source) {

        IGridTable table = tableSyntaxNode.getOriginalGridTable();
        IGridTable logicalTable = OffSetGridTableHelper.offSetTable(table);

        int height = logicalTable.getGridHeight();

        for (int i = 1; i < height; i++) {
            IGridTable row = logicalTable.getRow(i);

            String name = row.getColumn(0).getGridTable().getCell(0, 0).getStringValue();

            if (IXlsTableNames.LANG_PROPERTY.equals(name)) {
                preprocessOpenlTable(row.getGridTable(), source);
            } else if (IXlsTableNames.INCLUDE_TABLE.equals(name)) {
                preprocessIncludeTable(tableSyntaxNode, row.getGridTable(), source);
            } else if (IXlsTableNames.IMPORT_PROPERTY.equals(name)) {
                preprocessImportTable(row.getGridTable(), source);
            } else if (IXlsTableNames.VOCABULARY_PROPERTY.equals(name)) {
                preprocessVocabularyTable(row.getGridTable(), source);
            } else if (name == null || StringUtils.isEmpty(name) || DecisionTableHelper.isValidCommentHeader(name)) {
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
        int height = table.getGridHeight();
//        List<String> importsList = new ArrayList<String>();

        for (int i = 0; i < height; i++) {
            String singleImport = table.getCell(1, i).getStringValue();   
            if (StringUtils.isNotBlank(singleImport)) {
                singleImport = singleImport.trim();
            }            
            if (StringUtils.isNotEmpty(singleImport)) {
                addImport(singleImport);
            }
        }
        
        addInnerImports();
    }
    
    
    private void addImport(String singleImport)
    {
        if (!imports.contains(singleImport))
            imports.add(singleImport);
        
    }
    
    private void addInnerImports() {
        addImport("org.openl.rules.enumeration");
    }

    private void preprocessIncludeTable(TableSyntaxNode tableSyntaxNode,
            IGridTable table,
            XlsSheetSourceCodeModule sheetSource) {

        int height = table.getGridHeight();

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

                    SyntaxNodeException se = SyntaxNodeExceptionUtils.createError("Include " + include + " not found",
                        null,
                        null,
                        new GridCellSourceCodeModule(table.getRegion(1, i, 1, 1).getGridTable()));

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
                    SyntaxNodeException se = SyntaxNodeExceptionUtils.createError("Include " + include + " not found",
                        t,
                        null,
                        new GridCellSourceCodeModule(table.getRegion(1, i, 1, 1).getGridTable()));
                    addError(se);
                    tableSyntaxNode.addError(se);
                    OpenLMessagesUtils.addError(se.getMessage());
                    continue;
                }
            }

            try {
                preprocessWorkbook(src);
            } catch (Throwable t) {
                SyntaxNodeException se = SyntaxNodeExceptionUtils.createError("Include " + include + " not found",
                    t,
                    null,
                    new GridCellSourceCodeModule(table.getRegion(1, i, 1, 1).getGridTable()));
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

    private TableSyntaxNode preprocessTable(IGridTable table, XlsSheetSourceCodeModule source) throws OpenLCompilationException {

        GridCellSourceCodeModule src = new GridCellSourceCodeModule(table);

        IdentifierNode headerToken = Tokenizer.firstToken(src, " \n\r");
        HeaderSyntaxNode headerNode = new HeaderSyntaxNode(src, headerToken);

        String header = headerToken.getIdentifier();

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
            LOG.error("Error while preprocessing workbook", e);            
            OpenLMessagesUtils.addError(e);
            throw RuntimeExceptionWrapper.wrap(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }

            } catch (Throwable e) {
                LOG.error("Error trying close input stream:", e);
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
                SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Only one openl statement is allowed",
                    null,
                    openl);
                OpenLMessagesUtils.addError(error.getMessage());
                addError(error);
            }
        }
    }

    private void setVocabulary(IdentifierNode vocabulary) {

        if (this.vocabulary == null) {
            this.vocabulary = vocabulary;
        } else {
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError("Only one vocabulary is allowed",
                null,
                vocabulary);
            OpenLMessagesUtils.addError(error.getMessage());
            addError(error);
        }
    }

}