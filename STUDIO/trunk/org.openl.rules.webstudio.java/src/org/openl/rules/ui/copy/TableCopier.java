package org.openl.rules.ui.copy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.Pattern;

import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.openl.commons.web.jsf.FacesUtils;
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
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tablewizard.WizardBase;
import org.openl.rules.webstudio.properties.SystemValuesManager;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * Backing bean for table coping.
 *
 * @author Andrei Astrouski.
 */
public abstract class TableCopier extends WizardBase {
    
    /** Table identifier */
    private String elementUri = null;

    /** Table technical name */
    @NotEmpty(message="Technical name can not be empty")
    @Pattern(regexp="([a-zA-Z_][a-zA-Z_0-9]*)?", message="Invalid technical name")
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
        XlsSheetSourceCodeModule sheetSourceModule = getDestinationSheet();
        String newTableUri = buildTable(sheetSourceModule, model);
        setNewTableUri(newTableUri);
        getModifiedWorkbooks().add(sheetSourceModule.getWorkbookSource());
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
        IGridTable originalTable = model.getGridTable(elementUri);
        TableSyntaxNode baseNode = model.getNode(elementUri);
        String baseTableType = baseNode.getType();
        XlsSheetGridModel gridModel = new XlsSheetGridModel(sourceCodeModule);

        TableBuilder builder = new TableBuilder(gridModel);

        int logicBaseTableStartRow = 0;

        boolean envTable = ITableNodeTypes.XLS_ENVIRONMENT.equals(baseTableType);
        boolean otherTable = ITableNodeTypes.XLS_OTHER.equals(baseTableType);

        String newHeader = null;
        ICellStyle headerStyle = null;
        Map<String, Object> buildedPropForNewTable = null;
        ICellStyle propertiesStyle = null;

        if (!envTable && !otherTable) {
            newHeader = buildHeader(baseNode.getHeaderLineValue().getValue(), baseTableType);
            headerStyle = originalTable.getCell(0, 0).getStyle();
            logicBaseTableStartRow++;

            ITableProperties tableProperties = baseNode.getTableProperties();   
            Map<String, Object> baseTablePhysicalProperties = null;

            if (tableProperties != null) {
                propertiesStyle = getPropertiesStyle(tableProperties);
                baseTablePhysicalProperties = tableProperties.getPropertiesDefinedInTable();
            }
            buildedPropForNewTable = buildProperties();

            logicBaseTableStartRow += baseTablePhysicalProperties == null ? 0 : baseTablePhysicalProperties.size();
        }

        IGridTable gridTable = originalTable.getSubtable(0, logicBaseTableStartRow, originalTable.getWidth(),
                originalTable.getHeight() - logicBaseTableStartRow);

        // calculate new table size
        int tableWidth = originalTable.getWidth();
        if (tableWidth < 3 && buildedPropForNewTable != null && !buildedPropForNewTable.isEmpty()) {
            tableWidth = 3;
        }

        int tableHeight = 0;
        if  (newHeader != null) {
            tableHeight += 1;
        }
        if (buildedPropForNewTable != null) {
            tableHeight += buildedPropForNewTable.size();
        }
        tableHeight += gridTable.getHeight();

        // build table
        builder.beginTable(tableWidth, tableHeight);
        if (newHeader != null) {
            builder.writeHeader(newHeader, headerStyle);
        }
        if (buildedPropForNewTable != null && !buildedPropForNewTable.isEmpty()) {
            builder.writeProperties(buildedPropForNewTable, propertiesStyle);
        }
        builder.writeGridTable(gridTable);

        String uri = gridModel.getRangeUri(builder.getTableRegion());

        builder.endTable();

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
                Object systemValue = SystemValuesManager.getInstance().getSystemValue(
                        systemPropDef.getSystemValueDescriptor());
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
    protected void onCancel() {
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
            propertiesStyle = propertiesSection.getSource().getCell(0, 0).getStyle();
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

    @Override
    protected void onFinish() throws Exception {
        doCopy();
        super.onFinish();
    }

    @Override
    protected String makeUrlForNewTable() {
        return super.makeUrlForNewTable() + "&mode=" + (edit ? "edit" : "view");
    }

}
