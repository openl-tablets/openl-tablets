package org.openl.rules.ui;

import java.util.LinkedHashMap;
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
public class TableCopier extends WizardBase {

    /** Logger */
    private static final Log log = LogFactory.getLog(TableCopier.class);
    /** Table identifier */
    private String elementUri;
    /** Table technical name */
    private String tableTechnicalName;
    /** Table business name */
    private String tableBusinessName;

    public TableCopier() {
        start();
        init();
    }

    /**
     * Creates new header.
     *
     * @param header old header
     * @param tableType type of table
     * @return new header
     */
    private String buildHeader(String header, String tableType) {
        String tableOldTechnicalName = parseTechnicalName(header, tableType);
        String repl = "\\b" + tableOldTechnicalName + "(?=\\s*(\\(.*\\))?$)";
        return header.trim().replaceFirst(repl, tableTechnicalName.trim());
    }

    /**
     * Creates new properties.
     *
     * @param properties old properties
     * @return new properties
     */
    private Map<String, String> buildProperties(Property[] properties) {
        Map<String, String> newProperties = new LinkedHashMap<String, String>();
        if (properties != null) {
            for (int i = 0; i < properties.length; i++) {
                String key = properties[i].getKey().getValue();
                String value = properties[i].getValue().getValue();
                newProperties.put(key.trim(), value.trim());
            }
        }
        if (StringUtils.isBlank(tableBusinessName) && newProperties.containsKey(TableBuilder.TABLE_PROPERTIES_NAME)) {
            newProperties.remove(TableBuilder.TABLE_PROPERTIES_NAME);
        } else if (StringUtils.isNotBlank(tableBusinessName)) {
            newProperties.put(TableBuilder.TABLE_PROPERTIES_NAME, tableBusinessName);
        }
        return newProperties;
    }

    /**
     * Creates new table.
     *
     * @param sourceCodeModule excel sheet to save in
     * @param model table model
     * @throws CreateTableException
     */
    private void buildTable(XlsSheetSourceCodeModule sourceCodeModule, ProjectModel model) throws CreateTableException {
        IGridTable table = model.getTable(elementUri);
        TableSyntaxNode node = model.getNode(elementUri);
        String tableType = node.getType();

        TableBuilder builder = new TableBuilder(new XlsSheetGridModel(sourceCodeModule));

        int tableWidth = table.getGridWidth();
        int tableHeight = table.getGridHeight();
        int logicTableStartRow = 0;

        builder.beginTable(tableWidth, tableHeight);

        if (!tableType.equals(ITableNodeTypes.XLS_ENVIRONMENT) && !tableType.equals(ITableNodeTypes.XLS_OTHER)) {
            String newHeader = buildHeader(node.getHeaderLineValue().getValue(), tableType);
            ICellStyle headerStyle = table.getCellStyle(0, 0);
            builder.writeHeader(newHeader, headerStyle);
            logicTableStartRow++;

            TableProperties tableProperties = node.getTableProperties();
            Property[] properties = null;
            ICellStyle propertiesStyle = null;
            if (tableProperties != null) {
                IGridTable propertiesTable = tableProperties.getTable().getGridTable();
                propertiesStyle = propertiesTable.getCellStyle(0, 0) == null ? null : propertiesTable
                        .getCellStyle(0, 0);
                properties = tableProperties.getProperties();
            }
            builder.writeProperties(buildProperties(properties), propertiesStyle);
            logicTableStartRow += properties == null ? 0 : properties.length;
        }

        builder.writeGridTable(table.getLogicalRegion(0, logicTableStartRow, tableWidth,
                tableHeight - logicTableStartRow).getGridTable());

        builder.endTable();
    }

    /**
     * Copy table handler.
     */
    public String copy() {
        boolean success = false;
        try {
            doCopy();
            success = true;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Could not copy table", e.getMessage()));
            log.error("Could not copy table: ", e);
        }
        if (success) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Table was copied successful"));
        }
        return null;
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

    public String getTableBusinessName() {
        return tableBusinessName;
    }

    public String getTableTechnicalName() {
        return tableTechnicalName;
    }

    /**
     * Initializes table properties.
     */
    private void init() {
        String elementUri = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_URI);
        WebStudio studio = WebStudioUtils.getWebStudio();
        if (StringUtils.isNotBlank(elementUri)) {
            studio.setTableUri(elementUri);
            ProjectModel model = studio.getModel();
            TableSyntaxNode node = model.getNode(elementUri);
            tableTechnicalName = parseTechnicalName(node.getHeaderLineValue().getValue(), node.getType());
            tableBusinessName = node == null ? null : node.getProperty(TableBuilder.TABLE_PROPERTIES_NAME);
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
     * Parses table header for technical name
     *
     * @param header table header to parse
     * @param tableType type of table
     * @return technical name of table
     */
    private String parseTechnicalName(String header, String tableType) {
        if (tableType.equals(ITableNodeTypes.XLS_ENVIRONMENT) || tableType.equals(ITableNodeTypes.XLS_OTHER)) {
            return null;
        }
        header = header.replaceFirst("\\(.*\\)", "");
        String[] headerTokens = StringUtils.split(header);
        return headerTokens[headerTokens.length - 1];
    }

    /**
     * Cleans table properties.
     */
    @Override
    protected void reset() {
        super.reset();
        elementUri = null;
        tableTechnicalName = null;
        tableBusinessName = null;
    }

    public void setTableBusinessName(String tableBusinessName) {
        this.tableBusinessName = tableBusinessName;
    }

    public void setTableTechnicalName(String tableTechnicalName) {
        this.tableTechnicalName = tableTechnicalName;
    }

}
