package org.openl.rules.webstudio.web;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

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

        List<String> used = new ArrayList<String>();
        
        for (long modifiedOn : historyMap.keySet()) {
            String sourceName = historyMap.get(modifiedOn).getName();

            ProjectHistoryItem historyItem = new ProjectHistoryItem();
            String modifiedOnStr = new SimpleDateFormat(DATE_MODIFIED_PATTERN).format(
                    new Date(modifiedOn));
            historyItem.setVersion(modifiedOn);
            historyItem.setModifiedOn(modifiedOnStr);
            historyItem.setSourceName(sourceName);

            if (!used.contains(sourceName)) {
                used.add(sourceName);
                historyItem.setDisabled(true);
                // Add initial file to top
                history.add(0, historyItem);
                continue;
            }

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
        try {
            long[] versionsToCompare = getVersionsToCompare();

            ProjectModel model = WebStudioUtils.getProjectModel();
            SourceHistoryManager<File> historyManager = model.getHistoryManager();
            SortedMap<Long, File> sources = historyManager.get(versionsToCompare);

            if (sources.size() == 2) {
                Long source1Version = sources.firstKey();
                Long source2Version = sources.lastKey();
                File file1ToCompare = sources.get(source1Version);
                File file2ToCompare = sources.get(source2Version);
                String file1Name = file1ToCompare.getName();
                String file2Name = file2ToCompare.getName();

                if (!file2Name.equals(file1Name)) {
                    // Try to get a previous version
                    file1ToCompare = historyManager.getPrev(source2Version);
                    if (file1ToCompare == null) {
                        // Get initial source
                        sources = historyManager.get(file2Name);
                        file1ToCompare = sources.get(sources.firstKey());
                    }
                }

                UploadExcelDiffController diffController =
                    (UploadExcelDiffController) FacesUtils.getBackingBean("uploadExcelDiffController");
                diffController.compare(
                        Arrays.asList(file1ToCompare, file2ToCompare));
            }
        } catch (Exception e) {
            LOG.error(e);
            FacesUtils.addErrorMessage("Error when comparing projects");
        }

        return null;
    }

    private long[] getVersionsToCompare() {
        String versionsToCompareParam = FacesUtils.getRequestParameter("toCompare");
        String[] versionsToCompareStr = versionsToCompareParam.split(",");

        long[] versionsToCompare = new long[versionsToCompareStr.length];
        for (int i = 0; i < versionsToCompareStr.length; i++) {
            versionsToCompare[i] = Long.parseLong(versionsToCompareStr[i]);
        }

        return versionsToCompare;
    }

}
