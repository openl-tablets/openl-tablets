package org.openl.rules.ui;

import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.lang.xls.binding.TableProperties.Property;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.rules.ui.tablewizard.WizardBase;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * Backing bean for table coping.
 *
 * @author Andrei Astrouski.
 */
public abstract class TableCopier extends WizardBase {
    
    /** Logger */
    private static final Log log = LogFactory.getLog(TableCopier.class);   
    /** Table identifier */
    protected String elementUri = null;
    /** Table technical name */
    protected String tableTechnicalName;
    /** Table business name */
    protected String tableBusinessName;
    /** Need to save body content during coping */
    protected boolean saveContent = true;
    
    public boolean isSaveContent() {
        return saveContent;
    }

    public void setSaveContent(boolean saveContent) {
        this.saveContent = saveContent;
    }

    /**
     * Creates new table.
     *
     * @param sourceCodeModule excel sheet to save in
     * @param model table model
     * @throws CreateTableException
     */   
    protected void buildTable(XlsSheetSourceCodeModule sourceCodeModule, ProjectModel model) throws CreateTableException {
        IGridTable table = model.getTable(elementUri);
        TableSyntaxNode node = model.getNode(elementUri);
        String tableType = node.getType();
        
        //validateTechnicalName(node);

        TableBuilder builder = new TableBuilder(new XlsSheetGridModel(sourceCodeModule));

        int tableWidth = table.getGridWidth();
        int tableHeight = table.getGridHeight();
        int logicTableStartRow = 0;

        builder.beginTable(tableWidth, tableHeight);

        if (!tableType.equals(ITableNodeTypes.XLS_ENVIRONMENT) && !tableType.equals(ITableNodeTypes.XLS_OTHER)) {
            String newHeader = buildHeader(node.getHeaderLineValue().getValue(), tableType);
            ICellStyle headerStyle = table.getCell(0, 0).getStyle();
            builder.writeHeader(newHeader, headerStyle);
            logicTableStartRow++;

            TableProperties tableProperties = node.getTableProperties();
            Property[] properties = null;
            ICellStyle propertiesStyle = null;
            if (tableProperties != null) {
                propertiesStyle = getPropertiesStyle(tableProperties);
                properties = tableProperties.getProperties();
            }
            builder.writeProperties(buildProperties(properties), propertiesStyle);
            logicTableStartRow += properties == null ? 0 : properties.length;
        }

        builder.writeGridTable(table.getLogicalRegion(0, logicTableStartRow, tableWidth,
                tableHeight - logicTableStartRow).getGridTable());

        builder.endTable();
        builder.save();
    }
    
    /**
     * Creates new properties.
     *
     * @param properties old properties
     * @return new properties
     */
    protected abstract Map<String, Object> buildProperties(Property[] properties);
    
    /**
     * Creates new header.
     *
     * @param header old header
     * @param tableType type of table
     * @return new header
     */
    protected String buildHeader(String header, String tableType) {
        String tableOldTechnicalName = parseTechnicalName(header, tableType);
        String repl = "\\b" + tableOldTechnicalName + "(?=\\s*(\\(.*\\))?$)";
        return header.trim().replaceFirst(repl, tableTechnicalName.trim());
    }
    
    protected void initTableNames () {
        TableSyntaxNode node = getCopyingTable();
        tableTechnicalName = parseTechnicalName(node.getHeaderLineValue().getValue(), node.getType());
        tableBusinessName = node == null ? null : node.getPropertValueAsString(TableBuilder.TABLE_PROPERTIES_NAME);
    }
    
    protected TableSyntaxNode getCopyingTable() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setTableUri(elementUri);
        ProjectModel model = studio.getModel();
        TableSyntaxNode node = model.getNode(elementUri);
        return node;
    }
    
    /**
     * Parses table header for technical name
     *
     * @param header table header to parse
     * @param tableType type of table
     * @return technical name of table
     */
    protected String parseTechnicalName(String header, String tableType) {
        if (tableType.equals(ITableNodeTypes.XLS_ENVIRONMENT) || tableType.equals(ITableNodeTypes.XLS_OTHER)) {
            return null;
        }
        header = header.replaceFirst("\\(.*\\)", "");
        String[] headerTokens = StringUtils.split(header);
        return headerTokens[headerTokens.length - 1];
    }
    
    /**
     * Cleans table information.
     */
    @Override
    protected void reset() {
        super.reset();
        elementUri = null;
        tableTechnicalName = null;
        tableBusinessName = null;
    }
    
    public String getTableBusinessName() {
        return tableBusinessName;
    }

    public String getTableTechnicalName() {
        return tableTechnicalName;
    }
    
    
    public void setTableBusinessName(String tableBusinessName) {
        this.tableBusinessName = tableBusinessName;
    }

    public void setTableTechnicalName(String tableTechnicalName) {
        this.tableTechnicalName = tableTechnicalName;
    }
    
    /**
     * Initializes table information.
     */
    protected void initUri() {
        elementUri = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_URI);
        WebStudio studio = WebStudioUtils.getWebStudio();
        if (StringUtils.isNotBlank(elementUri)) {
            initTableNames();
        } else {
            elementUri = studio.getTableUri();
        }
    }
    
    /**
     * Copies table.
     *
     * @throws CreateTableException
     */
    private void doCopy() throws CreateTableException {
        WebStudio studio = WebStudioUtils.getWebStudio();
        ProjectModel model = studio.getModel();
        XlsSheetSourceCodeModule sourceCodeModule = getDestinationSheet();
        buildTable(sourceCodeModule, model);
    }
    
    @Override
    protected void onFinish(boolean cancelled) {
        reset();
    }
    
    @Override
    protected void onStart() {
        reset();
        initWorkbooks();
    }
    
    /**
     * Copy table handler.
     */
    public String copy() {
        String result = null;
        boolean success = false;
        try {
            doCopy();
            success = true;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Could not copy table. "+e.getMessage(), e.getMessage()));
            log.error("Could not copy table: ", e);
            result = "copyFailed";
        }
        if (success) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Table was copied successful"));
            result = "copySuccess";
        }
        return result;
    }
    
    protected ICellStyle getPropertiesStyle(TableProperties tableProperties) {
        ICellStyle propertiesStyle;
        IGridTable propertiesTable = tableProperties.getTable().getGridTable();
        propertiesStyle = propertiesTable.getCell(0, 0).getStyle() == null ? null : propertiesTable
                .getCell(0, 0).getStyle();
        return propertiesStyle;
    }

}
