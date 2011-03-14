package org.openl.rules.webstudio.web;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.diff.UploadExcelDiffController;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.source.SourceHistoryManager;

/**
 * @author Andrei Astrouski
 */
public class RevertProjectChangesBean {

    private static final Log LOG = LogFactory.getLog(RevertProjectChangesBean.class);

    public static final String DATE_MODIFIED_PATTERN = "MM.dd.yyyy 'at' hh:mm:ss a";

    public RevertProjectChangesBean() {
    }

    public List<ProjectHistoryItem> getHistory() {
        List<ProjectHistoryItem> history = new ArrayList<ProjectHistoryItem>();
        ProjectModel model = WebStudioUtils.getProjectModel();

        String[] sourceNames = getSources();
        Map<Long, File> historyMap = model.getHistoryManager().get(sourceNames);
        for (long modifiedOn : historyMap.keySet()) {
            ProjectHistoryItem historyItem = new ProjectHistoryItem();
            String modifiedOnStr = new SimpleDateFormat(DATE_MODIFIED_PATTERN).format(
                    new Date(modifiedOn));
            historyItem.setVersion(modifiedOn);
            historyItem.setModifiedOn(modifiedOnStr);
            historyItem.setSourceName(historyMap.get(modifiedOn).getName());

            history.add(historyItem);
        }

        return history;
    }

    public String[] getSources() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        return model.getModuleSourceNames();
    }

    public String revert() {
        String versionToRevertParam = FacesUtils.getRequestParameter("toRevert");
        long versionToRevert = Long.parseLong(versionToRevertParam);

        ProjectModel model = WebStudioUtils.getProjectModel();
        if (model.getHistoryManager().revert(versionToRevert)) {
            return "mainPage";
        } else {
            FacesUtils.addErrorMessage("Error when reverting project");
            return null;
        }
    }

    public String compare() {
        List<File> filesToCompare = new ArrayList<File>();
        
        try {
            String versionsToCompareParam = FacesUtils.getRequestParameter("toCompare");
            String[] versionsToCompareStr = versionsToCompareParam.split(",");
    
            long[] versionsToCompare = new long[versionsToCompareStr.length];
            for (int i = 0; i < versionsToCompareStr.length; i++) {
                versionsToCompare[i] = Long.parseLong(versionsToCompareStr[i]);
            }

            ProjectModel model = WebStudioUtils.getProjectModel();
            SourceHistoryManager<File> historyManager = model.getHistoryManager();
            for (long versionToCompare : versionsToCompare) {
                File fileToCompare = historyManager.get(versionToCompare);
                filesToCompare.add(fileToCompare);
            }

            UploadExcelDiffController diffController =
                (UploadExcelDiffController) FacesUtils.getBackingBean("uploadExcelDiffController");
            diffController.compare(filesToCompare);

        } catch (Exception e) {
            LOG.error(e);
            FacesUtils.addErrorMessage("Error when comparing projects");
        }

        return null;
    }

}
