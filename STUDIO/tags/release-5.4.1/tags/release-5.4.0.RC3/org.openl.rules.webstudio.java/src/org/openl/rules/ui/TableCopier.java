package org.openl.rules.ui;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.rules.ui.tablewizard.WizardBase;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.properties.SystemValuesManager;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringTool;

/**
 * Backing bean for table coping.
 *
 * @author Andrei Astrouski.
 */
public abstract class TableCopier extends WizardBase {
    
    /** Logger */
    private static final Log LOG = LogFactory.getLog(TableCopier.class);   
    /** Table identifier */
    private String elementUri = null;
    /** New table identifier */
    private String newTableUri = null;
    /** Table technical name */
    private String tableTechnicalName;
    /** Table business name */
    private String tableBusinessName;

    /** <code>true</code> when copied table have to be displayed in edit mode
     * <code>false</code> when copied table have to be displayed in view mode*/
    private boolean edit = false;

    /**
     * Copies table.
     *
     * @throws CreateTableException
     */
    protected void doCopy() throws CreateTableException {
        WebStudio studio = WebStudioUtils.getWebStudio();
        ProjectModel model = studio.getModel();
        XlsSheetSourceCodeModule sourceCodeModule = getDestinationSheet();
        newTableUri = buildTable(sourceCodeModule, model);
    }

    /**
     * Creates new table.
     *
     * @param sourceCodeModule excel sheet to save in
     * @param model table model
     * @return URI of new table.
     * @throws CreateTableException
     */   
    protected String buildTable(XlsSheetSourceCodeModule sourceCodeModule, ProjectModel model)
        throws CreateTableException {
        IGridTable originalTable = model.getTable(elementUri);
        TableSyntaxNode baseNode = model.getNode(elementUri);
        String baseTableType = baseNode.getType();
        XlsSheetGridModel gridModel = new XlsSheetGridModel(sourceCodeModule);
        //validateTechnicalName(node);

        TableBuilder builder = new TableBuilder(gridModel);

        int baseTableWidth = originalTable.getGridWidth();
        int baseTableHeight = originalTable.getGridHeight();
        int logicBaseTableStartRow = 0;

        builder.beginTable(baseTableWidth, baseTableHeight);
        boolean envTable = ITableNodeTypes.XLS_ENVIRONMENT.equals(baseTableType);
        boolean otherTable = ITableNodeTypes.XLS_OTHER.equals(baseTableType); 
        if (!envTable && !otherTable) {
            String newHeader = buildHeader(baseNode.getHeaderLineValue().getValue(), baseTableType);
            ICellStyle headerStyle = originalTable.getCell(0, 0).getStyle();
            builder.writeHeader(newHeader, headerStyle);
            logicBaseTableStartRow++;

            ITableProperties tableProperties = baseNode.getTableProperties();   
            Map<String, Object> baseTablePhysicalProperties = null;
            ICellStyle propertiesStyle = null;
            if (tableProperties != null) {
                propertiesStyle = getPropertiesStyle(tableProperties);
                baseTablePhysicalProperties = tableProperties.getPropertiesDefinedInTable();
            }
            Map<String, Object> buildedPropForNewTable = buildProperties(); 
            if (buildedPropForNewTable.size() > 0) {
                builder.writeProperties(buildedPropForNewTable, propertiesStyle);
            }
            logicBaseTableStartRow += baseTablePhysicalProperties == null ? 0 : baseTablePhysicalProperties.size();
        }

        builder.writeGridTable(originalTable.getLogicalRegion(0, logicBaseTableStartRow, baseTableWidth,
                baseTableHeight - logicBaseTableStartRow).getGridTable());

        String uri = gridModel.getRangeUri(builder.getTableRegion());
        builder.endTable();
        builder.save();
        return uri;
    }

    /**
     * Creates new properties.
     *     
     * @return new properties
     */
    protected abstract Map<String, Object> buildProperties();
    
    /**
     * Creates system properties for new table.
     * 
     * @return
     */
    protected Map<String, Object> buildSystemProperties() {
        Map<String, Object> result = new HashMap<String, Object>();
        List<TablePropertyDefinition> systemPropDefinitions = TablePropertyDefinitionUtils.getSystemProperties();
        for (TablePropertyDefinition systemPropDef : systemPropDefinitions) {
            if (systemPropDef.getSystemValuePolicy().equals(SystemValuePolicy.IF_BLANK_ONLY)) {
                Object systemValue = SystemValuesManager.instance().getSystemValue(systemPropDef.getSystemValueDescriptor());
                if (systemValue != null){
                    result.put(systemPropDef.getName(), systemValue);                    
                }
            }
        }
        return result;
    }
    
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
    
    protected void initTableNames() {
        TableSyntaxNode node = getCopyingTable();                
        if (node != null) {
            tableTechnicalName = parseTechnicalName(node.getHeaderLineValue().getValue(), node.getType());
            ITableProperties tableProperties = node.getTableProperties();
            if (tableProperties != null) {
                tableBusinessName = node.getTableProperties().getName();
            }
        }        
    }
    
    protected TableSyntaxNode getCopyingTable() {
        WebStudio studio = WebStudioUtils.getWebStudio();
        studio.setTableUri(elementUri);
        ProjectModel model = studio.getModel();        
        return  model.getNode(elementUri);
    }
    
    /**
     * Parses table header for technical name
     *
     * @param header table header to parse
     * @param tableType type of table
     * @return technical name of table
     */
    protected String parseTechnicalName(String header, String tableType) {
        String result = null;
        String headerIntern = header;
        String[] headerTokens = null;
        if (!ITableNodeTypes.XLS_ENVIRONMENT.equals(tableType) && !ITableNodeTypes.XLS_OTHER.equals(tableType)) {
            headerIntern = header.replaceFirst("\\(.*\\)", "");
            headerTokens = StringUtils.split(headerIntern);
            result = headerTokens[headerTokens.length - 1]; 
        }              
        return result;
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
    
    protected String getElementUri() {
        return elementUri;
    }

    protected void setElementUri(String elementUri) {
        this.elementUri = elementUri;
    }

    public String getNewTableUri() {
        return newTableUri;
    }

    public boolean isEdit() {
        return edit;
    }

    protected void setEdit(boolean edit) {
        this.edit = edit;
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
     * 
     * @param tableProperties properties of the table that is going to be copied.  
     * @return style of the properties section in table if exists. If no <code>NULL</code>.
     */
    protected ICellStyle getPropertiesStyle(ITableProperties tableProperties) {
        ICellStyle propertiesStyle = null;
        ILogicalTable propertiesSection = tableProperties.getPropertiesSection();
        if (propertiesSection != null) {
            IGridTable propertiesTable = propertiesSection.getGridTable();
            propertiesStyle = propertiesTable.getCell(0, 0).getStyle();
        }        
        return propertiesStyle;
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
     * Copy table handler.
     */
    public String copy() {
        String result = null;
        boolean success = false;
        try {
            doCopy();
            success = true;
        } catch (CreateTableException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                            String.format("Could not copy table. %s", e.getMessage()), e.getMessage()));
            LOG.error("Could not copy table: ", e);
            result = "copyFailed";
        }
        if (success) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Table was copied successful"));
            result = "copySuccess";
            resetStudio();
            HttpServletResponse resp = (HttpServletResponse) FacesUtils.getExternalContext().getResponse();
            try {
                resp.sendRedirect(makeUrlForNewTable());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    private void resetStudio() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        studio.reset();
        studio.getModel().buildProjectTree();
    }
    
    private String makeUrlForNewTable(){
        StringBuffer buffer = new StringBuffer(FacesUtils.getExternalContext().getRequestContextPath()
                + "/faces/facelets/tableeditor/showTable.xhtml");
        buffer.append("?uri="+StringTool.encodeURL(newTableUri));
        buffer.append("&mode="+(edit?"edit":"view"));
        return buffer.toString();
    }
}
