package org.openl.rules.ui.tablewizard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedProperty;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.validator.ValidatorException;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.hibernate.validator.constraints.NotBlank;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.load.SimpleSheetLoader;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableUtils;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinition.SystemValuePolicy;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.ui.BaseWizard;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.properties.SystemValuesManager;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * @author Aliaksandr Antonik.
 *         <p/>
 *         TODO Rename Workbook and Worksheet to Module and Category correspondently
 */
public abstract class TableCreationWizard extends BaseWizard {
    protected static final String INVALID_NAME_MESSAGE = "Invalid name: only latin letters, numbers and _ are allowed, name cannot begin with a number";

    private final Logger log = LoggerFactory.getLogger(TableCreationWizard.class);

    private static final String SHEET_EXISTING = "existing";
    private static final String SHEET_NEW = "new";
    private String workbook;
    private Integer worksheetIndex;
    private Map<String, XlsWorkbookSourceCodeModule> workbooks;
    private boolean newWorksheet;
    private boolean wizardFinished;

    @NotBlank(message = "Cannot be empty")
    private String newWorksheetName;

    @ManagedProperty(value = "#{environment}")
    private Environment environment;

    /**
     * New table identifier
     */
    private String newTableId;

    private Set<XlsWorkbookSourceCodeModule> modifiedWorkbooks = new HashSet<>();

    protected XlsSheetSourceCodeModule getDestinationSheet() {
        XlsSheetSourceCodeModule sourceCodeModule;
        XlsWorkbookSourceCodeModule module = workbooks.get(workbook);
        if (newWorksheet) {
            Sheet sheet = module.getWorkbook().createSheet(getNewWorksheetName());
            sourceCodeModule = new XlsSheetSourceCodeModule(new SimpleSheetLoader(sheet), module);
        } else {
            Sheet sheet = module.getWorkbook().getSheetAt(getWorksheetIndex());
            sourceCodeModule = new XlsSheetSourceCodeModule(new SimpleSheetLoader(sheet), module);
        }
        return sourceCodeModule;
    }

    public String getNewWorksheet() {
        return newWorksheet ? SHEET_NEW : SHEET_EXISTING;
    }

    public void setNewWorksheet(String value) {
        newWorksheet = SHEET_NEW.equals(value);
    }

    public String getNewWorksheetName() {
        return newWorksheetName;
    }

    public void setNewWorksheetName(String newWorksheetName) {
        this.newWorksheetName = newWorksheetName;
    }

    public String getWorkbook() {
        return workbook;
    }

    public void setWorkbook(String workbook) {
        this.workbook = workbook;
    }

    public String getModuleName() {
        return FileUtils.getBaseName(workbook);
    }

    public void setWorksheetIndex(Integer worksheetIndex) {
        this.worksheetIndex = worksheetIndex;
    }

    public List<SelectItem> getWorkbooks() {
        List<SelectItem> items = new ArrayList<>(workbooks.size());
        for (String wbURI : workbooks.keySet()) {
            items.add(new SelectItem(wbURI, FileUtils.getBaseName(wbURI)));
        }

        return items;
    }

    public boolean isManyWorkbooks() {
        return workbooks.size() > 1;
    }

    public Integer getWorksheetIndex() {
        return worksheetIndex;
    }

    public String getWorksheetName() {
        Workbook currentWorkbook = workbooks.get(workbook).getWorkbook();
        return currentWorkbook.getSheetName(worksheetIndex);
    }

    public List<SelectItem> getWorksheets() {
        if (workbook == null || workbooks == null) {
            return Collections.emptyList();
        }

        XlsWorkbookSourceCodeModule currentSheet = workbooks.get(workbook);
        if (currentSheet == null) {
            return Collections.emptyList();
        }

        Workbook workbook = currentSheet.getWorkbook();
        List<SelectItem> items = new ArrayList<>(workbook.getNumberOfSheets());
        for (int i = 0; i < workbook.getNumberOfSheets(); ++i) {
            items.add(new SelectItem(i, workbook.getSheetName(i)));
        }

        Collections.sort(items, (item1, item2) -> item1.getLabel().compareToIgnoreCase(item2.getLabel()));

        return items;
    }

    protected void initWorkbooks() {
        workbooks = new HashMap<>();

        WorkbookSyntaxNode[] syntaxNodes = WizardUtils.getWorkbookNodes();
        for (WorkbookSyntaxNode node : syntaxNodes) {
            XlsWorkbookSourceCodeModule module = node.getWorkbookSourceCodeModule();
            workbooks.put(module.getDisplayName(), module);
        }

        if (workbooks.size() > 0) {
            workbook = workbooks.keySet().iterator().next();
        }
    }

    public String getNewTableId() {
        return newTableId;
    }

    public void setNewTableId(String newTableId) {
        this.newTableId = TableUtils.makeTableId(newTableId);
        // TODO: It should be removed when the table can be resolved by the ID
        WebStudioUtils.getWebStudio().setTableUri(newTableId);
    }

    public Set<XlsWorkbookSourceCodeModule> getModifiedWorkbooks() {
        return modifiedWorkbooks;
    }

    protected void reset() {
        worksheetIndex = 0;
        workbooks = null;
        newWorksheet = false;
        wizardFinished = false;
        newWorksheetName = StringUtils.EMPTY;
        getModifiedWorkbooks().clear();
    }

    protected void doSave() throws IOException {
        for (XlsWorkbookSourceCodeModule workbook : modifiedWorkbooks) {
            workbook.save();
        }
    }

    @Override
    public String finish() throws Exception {
        boolean success = false;
        try {
            if (!wizardFinished) {
                onFinish();
                wizardFinished = true;
            }
            doSave();
            FacesUtils.removeSessionParam(org.openl.rules.tableeditor.util.Constants.TABLE_EDITOR_MODEL_NAME);
            success = true;
        } catch (Exception e) {
            log.error("Could not save table: ", e);
            throw e;
        }
        if (success) {
            WebStudioUtils.getWebStudio().compile();
            reset(); // After wizard is finished - no need to store references to tables etc: it will be a memory leak.
        }
        return null;
    }

    /**
     * Validation for technical name
     */
    public void validateTechnicalName(FacesContext context, UIComponent toValidate, Object value) {
        FacesMessage message = new FacesMessage();
        ValidatorException validEx = null;

        try {
            String name = ((String) value).toUpperCase();

            if (!this.checkNames(name)) {
                message.setDetail("Table with such name already exists");
                validEx = new ValidatorException(message);
                throw validEx;
            }

        } catch (Exception e) {
            throw new ValidatorException(message);
        }
    }

    private boolean checkNames(String techName) {
        WebStudio studio = WebStudioUtils.getWebStudio();
        ProjectModel model = studio.getModel();

        for (TableSyntaxNode node : model.getAllTableNodes().values()) {
            try {
                if (node.getMember().getName().equalsIgnoreCase(techName)) {
                    return false;
                }

            } catch (Exception e) {
            }
        }

        return true;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    protected Map<String, Object> buildSystemProperties() {
        String userMode = environment.getProperty("user.mode");
        Map<String, Object> result = new LinkedHashMap<>();

        List<TablePropertyDefinition> systemPropDefinitions = TablePropertyDefinitionUtils.getSystemProperties();
        for (TablePropertyDefinition systemPropDef : systemPropDefinitions) {
            String systemValueDescriptor = systemPropDef.getSystemValueDescriptor();
            if (userMode.equals("single") && systemValueDescriptor
                .equals(SystemValuesManager.CURRENT_USER_DESCRIPTOR)) {
                continue;
            }
            if (systemPropDef.getSystemValuePolicy().equals(SystemValuePolicy.IF_BLANK_ONLY)) {
                Object systemValue = SystemValuesManager.getInstance().getSystemValue(systemValueDescriptor);
                if (systemValue != null) {
                    result.put(systemPropDef.getName(), systemValue);
                }
            }
        }

        return result;
    }

    protected Map<String, Object> buildProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();

        // Put system properties.
        if (WebStudioUtils.getWebStudio().isUpdateSystemProperties()) {
            Map<String, Object> systemProperties = buildSystemProperties();
            properties.putAll(systemProperties);
        }

        return properties;
    }

}
