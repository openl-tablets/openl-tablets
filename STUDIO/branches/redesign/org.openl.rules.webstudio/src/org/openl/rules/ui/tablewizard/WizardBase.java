package org.openl.rules.ui.tablewizard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.syntax.WorkbookSyntaxNode;

import static org.openl.rules.ui.tablewizard.WizardUtils.getMetaInfo;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.ui.tablewizard.jsf.BaseWizardBean;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringTool;

/**
 * @author Aliaksandr Antonik.
 */
public abstract class WizardBase extends BaseWizardBean {
    private static final String SHEET_EXSISTING = "existing";
    private static final String SHEET_NEW = "new";
    private String workbook;
    private Integer worksheetIndex;
    private Map<String, XlsWorkbookSourceCodeModule> workbooks;
    private boolean newWorksheet;
    private boolean wizardFinised;
    private String newWorksheetName;
    /** New table identifier */
    private String newTableUri;

    private static final Log LOG = LogFactory.getLog(WizardBase.class);

    protected XlsSheetSourceCodeModule getDestinationSheet() {
        XlsSheetSourceCodeModule sourceCodeModule;
        XlsWorkbookSourceCodeModule module = workbooks.get(workbook);
        if (newWorksheet) {
            Sheet sheet = module.getWorkbook().createSheet(getNewWorksheetName());
            sourceCodeModule = new XlsSheetSourceCodeModule(sheet, getNewWorksheetName(), module);
        } else {
            Sheet sheet = module.getWorkbook().getSheetAt(getWorksheetIndex());
            sourceCodeModule = new XlsSheetSourceCodeModule(sheet, module.getWorkbook().getSheetName(
                    getWorksheetIndex()), module);
        }
        return sourceCodeModule;
    }

    public String getNewWorksheet() {
        return newWorksheet ? SHEET_NEW : SHEET_EXSISTING;
    }

    public String getNewWorksheetName() {
        return newWorksheetName;
    }

    public String getWorkbook() {
        return workbook;
    }

    public String getWorkbookName() {
        String[] parts = workbook.split("/");
        return parts[parts.length - 1];
    }

    public List<SelectItem> getWorkbooks() {
        List<SelectItem> items = new ArrayList<SelectItem>(workbooks.size());
        for (String wbURI : workbooks.keySet()) {
            String[] parts = wbURI.split("/");
            items.add(new SelectItem(wbURI, parts[parts.length - 1]));
        }

        return items;
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
        List<SelectItem> items = new ArrayList<SelectItem>(workbook.getNumberOfSheets());
        for (int i = 0; i < workbook.getNumberOfSheets(); ++i) {
            items.add(new SelectItem(i, workbook.getSheetName(i)));
        }
        return items;
    }

    protected void initWorkbooks() {
        workbooks = new HashMap<String, XlsWorkbookSourceCodeModule>();

        WorkbookSyntaxNode[] syntaxNodes = getMetaInfo().getXlsModuleNode().getWorkbookSyntaxNodes();
        for (WorkbookSyntaxNode node : syntaxNodes) {
            XlsWorkbookSourceCodeModule module = node.getWorkbookSourceCodeModule();
            workbooks.put(module.getUri(), module);
        }

        if (workbooks.size() > 0) {
            workbook = workbooks.keySet().iterator().next();
        }
    }

    public String getNewTableUri() {
        return newTableUri;
    }

    public void setNewTableUri(String newTableUri) {
        this.newTableUri = newTableUri;
    }

    protected void reset() {
        worksheetIndex = 0;
        workbooks = null;
        newWorksheet = false;
        wizardFinised = false;
        newWorksheetName = StringUtils.EMPTY;
        getModifiedWorkbooks().clear();
    }

    public void setNewWorksheet(String value) {
        newWorksheet = SHEET_NEW.equals(value);
    }

    public void setNewWorksheetName(String newWorksheetName) {
        this.newWorksheetName = newWorksheetName;
    }

    public void setWorkbook(String workbook) {
        this.workbook = workbook;
    }

    public void setWorksheetIndex(Integer worksheetIndex) {
        this.worksheetIndex = worksheetIndex;
    }

    @Override
    public String finish() {
        boolean success = false;
        try {
            if (!wizardFinised) {
                wizardFinised = true;
                onFinish();
            }
            doSave();
            success = true;
        } catch (Exception e) {
            FacesUtils.addErrorMessage("Could not save table.", e.getMessage());
            LOG.error("Could not save table: ", e);
        }
        if (success) {
            resetStudio();
            try {
                FacesUtils.redirect(makeUrlForNewTable());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private void resetStudio() {
        final WebStudio studio = WebStudioUtils.getWebStudio();
        studio.rebuildModel();
    }

    protected String makeUrlForNewTable() {
        StringBuffer buffer = new StringBuffer(FacesUtils.getContextPath()
                + "/faces/pages/modules/rulesEditor/index.xhtml");
        buffer.append("?uri=" + StringTool.encodeURL(newTableUri));
        return buffer.toString();
    }

}
