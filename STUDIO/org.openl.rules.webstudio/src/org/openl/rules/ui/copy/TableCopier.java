package org.openl.rules.ui.copy;

import org.hibernate.validator.constraints.NotBlank;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.constraints.Constraint;
import org.openl.rules.table.constraints.Constraints;
import org.openl.rules.table.constraints.LessThanConstraint;
import org.openl.rules.table.constraints.MoreThanConstraint;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.properties.inherit.PropertiesChecker;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.rules.tableeditor.model.TableEditorModel;
import org.openl.rules.tableeditor.renderkit.TableProperty;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tablewizard.PropertiesBean;
import org.openl.rules.ui.tablewizard.TableCreationWizard;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;
import org.openl.util.conf.Version;
import org.richfaces.component.UIRepeat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.Pattern;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Bean for table coping.
 *
 * @author Andrei Astrouski.
 */
public class TableCopier extends TableCreationWizard {

    private final Logger log = LoggerFactory.getLogger(TableCopier.class);

    public static final String INIT_VERSION = "0.0.1";

    private IOpenLTable table = null;

    /**
     * Table technical name
     */
    @NotBlank(message = "Can not be empty")
    @Pattern(regexp = "([a-zA-Z_][a-zA-Z_0-9]*)?", message = INVALID_NAME_MESSAGE)
    private String tableTechnicalName;

    /**
     * Bean - container of all properties for new copy of the original table.
     */
    private PropertiesBean propertiesManager;

    private UIRepeat propsTable;

    public PropertiesBean getPropertiesManager() {
        return propertiesManager;
    }

    public TableCopier(IOpenLTable table) {
        try {
            start();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        this.table = table;
        propertiesManager = new PropertiesBean(getAllPossibleProperties(table.getType()));
        initTableName();
        initProperties();
    }

    private void initProperties() {
        List<TableProperty> definedProperties = new ArrayList<TableProperty>();

        ITableProperties tableProperties = table.getProperties();
        for (String possiblePropertyName : propertiesManager.getPossibleToAddProperties()) {
            TablePropertyDefinition propDefinition = TablePropertyDefinitionUtils.getPropertyByName(possiblePropertyName);
            if (propDefinition != null && !propDefinition.isSystem() && propDefinition.getDeprecation() == null) {
                Object propertyValue = tableProperties.getPropertyValue(possiblePropertyName) != null ?
                        tableProperties.getPropertyValue(possiblePropertyName) : null;

                if (tableProperties.getTableProperties().containsKey(possiblePropertyName)) {
                    Class<?> propertyType = null;

                    if (propDefinition.getType() != null) {
                        propertyType = propDefinition.getType().getInstanceClass();
                    }

                    String displayName = propDefinition.getDisplayName();
                    String format = propDefinition.getFormat();
                    boolean dimensional = propDefinition.isDimensional();

                    TableProperty tableProperty = new TableProperty.TablePropertyBuilder(possiblePropertyName, propertyType).value(
                            propertyValue).displayName(displayName).format(format).dimensional(dimensional).build();

                    definedProperties.add(tableProperty);
                }
            }
        }
        propertiesManager.setProperties(definedProperties);
    }

    public String getValidationJS() {
        StringBuilder validation = new StringBuilder();
        TableProperty prop = getCurrentProp();
        Constraints constraints = prop.getConstraints();
        if (constraints != null) {
            String inputId = getInputIdJS(prop.getName());
            for (Constraint constraint : constraints.getAll()) {
                if (constraint instanceof LessThanConstraint || constraint instanceof MoreThanConstraint) {
                    String validator = constraint instanceof LessThanConstraint ? "lessThan" : "moreThan";
                    String compareToField = (String) constraint.getParams()[0];
                    String compareToFieldId = getInputIdJS(compareToField);
                    TableProperty compareToProperty = getProperty(prop.getName());
                    String compareToPropertyDisplayName = compareToProperty == null ? ""
                            : compareToProperty.getDisplayName();
                    validation.append("new Validation(")
                            .append(inputId)
                            .append(", '")
                            .append(validator)
                            .append("', '', {compareToFieldId:")
                            .append(compareToFieldId)
                            .append(",messageParams:'")
                            .append(compareToPropertyDisplayName)
                            .append("'})");
                }
            }
        }
        return validation.toString();
    }

    private List<String> getAllPossibleProperties(String tableType) {
        List<String> possibleProperties = new ArrayList<String>();
        TablePropertyDefinition[] propDefinitions = DefaultPropertyDefinitions.getDefaultDefinitions();
        for (TablePropertyDefinition propDefinition : propDefinitions) {
            if (!propDefinition.isSystem()) {
                String propertyName = propDefinition.getName();

                // check if the property can be defined in current type of table
                // and if property can be defined on TABLE level.
                if (PropertiesChecker.isPropertySuitableForTableType(propertyName, tableType)
                        && PropertiesChecker.isPropertySuitableForLevel(InheritanceLevel.TABLE, propertyName)) {
                    possibleProperties.add(propDefinition.getName());
                }
            }
        }
        return possibleProperties;
    }

    /**
     * Copies table.
     *
     * @throws CreateTableException if unable to create a table
     */
    protected void doCopy() throws CreateTableException {
        WebStudio studio = WebStudioUtils.getWebStudio();
        ProjectModel model = studio.getModel();
        XlsSheetSourceCodeModule sheetSourceModule = getDestinationSheet();
        String newTableUri = buildTable(sheetSourceModule, model);
        setNewTableId(newTableUri);
        getModifiedWorkbooks().add(sheetSourceModule.getWorkbookSource());
    }

    /**
     * Creates new table.
     *
     * @param sourceCodeModule excel sheet to save in
     * @param model            table model
     * @return URI of new table.
     * @throws CreateTableException if unable to create a table
     */
    protected String buildTable(XlsSheetSourceCodeModule sourceCodeModule, ProjectModel model)
            throws CreateTableException {
        IGridTable originalTable = model.getGridTable(table.getUri());
        originalTable.edit();
        TableSyntaxNode baseNode = model.getNode(table.getUri());
        String baseTableType = baseNode.getType();
        XlsSheetGridModel gridModel = new XlsSheetGridModel(sourceCodeModule);

        TableBuilder builder = new TableBuilder(gridModel);

        int logicBaseTableStartRow = 0;

        boolean envTable = XlsNodeTypes.XLS_ENVIRONMENT.toString().equals(baseTableType);
        boolean otherTable = XlsNodeTypes.XLS_OTHER.toString().equals(baseTableType);

        String newHeader = null;
        ICellStyle headerStyle = null;
        Map<String, Object> buildedPropForNewTable = null;
        ICellStyle propertiesStyle = null;

        if (!envTable && !otherTable) {
            newHeader = buildHeader(baseNode.getHeaderLineValue().getValue(), baseTableType);
            headerStyle = originalTable.getCell(0, 0).getStyle();
            logicBaseTableStartRow++;

            if (baseNode.hasPropertiesDefinedInTable()) {
                ITableProperties tableProperties = baseNode.getTableProperties();
                propertiesStyle = getPropertiesStyle(tableProperties);
                logicBaseTableStartRow += tableProperties.getPropertiesSection().getHeight();
            }
            buildedPropForNewTable = buildProperties();
        }

        IGridTable gridTable = originalTable.getSubtable(0, logicBaseTableStartRow, originalTable.getWidth(),
                originalTable.getHeight() - logicBaseTableStartRow);

        // calculate new table size
        int tableWidth = originalTable.getWidth();
        if (tableWidth < 3 && buildedPropForNewTable != null && !buildedPropForNewTable.isEmpty()) {
            tableWidth = 3;
        }

        int tableHeight = 0;
        if (newHeader != null) {
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
     * Creates new header.
     *
     * @param header    old header
     * @param tableType type of table
     * @return new header
     */
    protected String buildHeader(String header, String tableType) {
        String tableOldTechnicalName = parseTechnicalName(header, tableType);
        String repl = "\\b" + tableOldTechnicalName + "(?=\\s*(\\(.*\\))?$)";
        return header.trim().replaceFirst(repl, tableTechnicalName.trim());
    }

    private void initTableName() {
        if (table != null) {
            String name = table.getName();
            if (StringUtils.isEmpty(name)) {
                // If table contains errors, it will not contain compiled name. Try to parse table header instead.
                String methodHeader = table.getGridTable().getCell(0, 0).getStringValue();
                name = TableSyntaxNodeUtils.str2name(methodHeader, XlsNodeTypes.getEnumByValue(table.getType()));
            }
            tableTechnicalName = name;
        }
    }

    public IOpenLTable getCopyingTable() {
        return table;
    }

    /**
     * Parses table header for technical name
     *
     * @param header    table header to parse
     * @param tableType type of table
     * @return technical name of table
     */
    protected String parseTechnicalName(String header, String tableType) {
        String result = null;
        String headerIntern;
        String[] headerTokens;
        if (!XlsNodeTypes.XLS_ENVIRONMENT.toString().equals(tableType) && !XlsNodeTypes.XLS_OTHER.toString().equals(tableType)) {
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
        table = null;
        tableTechnicalName = null;
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

    protected IOpenLTable getTable() {
        return table;
    }

    public String getTableTechnicalName() {
        return tableTechnicalName;
    }

    public void setTableTechnicalName(String tableTechnicalName) {
        this.tableTechnicalName = tableTechnicalName;
    }

    @Override
    protected void onFinish() throws Exception {
        doCopy();
        super.onFinish();
    }

    private String getInputIdJS(String propName) {
        return "$j('#" + propsTable.getParent().getId() + "').find('input[type=hidden][name=id][value="
                + propName + "]').parent().find('input:first').id";
    }

    private TableProperty getCurrentProp() {
        return (TableProperty) propsTable.getRowData();
    }

    public TableProperty getProperty(String name) {
        for (TableProperty property : propertiesManager.getProperties()) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    /**
     * Creates new properties.
     *
     * @return new properties
     */
    protected Map<String, Object> buildProperties() {
        Map<String, Object> newProperties = new LinkedHashMap<String, Object>();
        if (WebStudioUtils.getWebStudio().isUpdateSystemProperties()) {
            newProperties.putAll(buildSystemProperties());
        }

        // TODO validateIfNecessaryPropertiesWereChanged(tableProperties);

        for (TableProperty property : propertiesManager.getProperties()) {
            String name = property.getName();
            Object value = property.getValue();
            if (!isEmpty(value)) {
                newProperties.put(name.trim(), value);
            }
        }
        return newProperties;
    }

    public static boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        } else if (value instanceof String && StringUtils.isEmpty((String) value)) {
            return true;
        } else if (value.getClass().isArray()) {
            return Array.getLength(value) <= 0;
        }
        return false;
    }

    public void setPropsTable(UIRepeat propsTable) {
        this.propsTable = propsTable;
    }

    public UIRepeat getPropsTable() {
        return propsTable;
    }

    /**
     * It is only necessary for version editor.
     *
     * @return "Version" TableProperty.
     */
    public TableProperty getVersion() {
        return getProperty("version");
    }

    /**
     * @return Min version that can be set into new copy of original table.
     */
    public Version getMinNextVersion() {
        TableProperty originalVersion = getVersion();
        if (originalVersion != null && StringUtils.isNotEmpty((String) originalVersion.getValue())) {
            return Version.parseVersion((String) originalVersion.getValue(), 0, "..");
        } else {
            return Version.parseVersion(INIT_VERSION, 0, "..");
        }
    }

    protected void updatePropertiesForOriginalTable(Map<String, Object> properties) {
        if (properties.size() > 0) {
            WebStudio studio = WebStudioUtils.getWebStudio();
            ProjectModel model = studio.getModel();
            TableEditorModel tableEditorModel = model.getTableEditorModel(table.getUri());

            try {
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    tableEditorModel.setProperty(entry.getKey(), entry.getValue());
                }
                getModifiedWorkbooks().add(tableEditorModel.getSheetSource().getWorkbookSource());
            } catch (Exception e) {
                log.error("Can not update table properties for original table", e);
            }
        }
    }

    public List<TableProperty> getPropertiesToDisplay() {
        return propertiesManager.getProperties();
    }

}
