package org.openl.rules.webstudio.web;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.diff.UploadExcelDiffController;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.source.SourceHistoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andrei Astrouski
 */
@ManagedBean
@RequestScoped
public class RevertProjectChangesBean {

    private final Logger log = LoggerFactory.getLogger(RevertProjectChangesBean.class);

    public String dateModifiedPattern = WebStudioUtils.getWebStudio()
        .getSystemConfigManager()
        .getStringProperty("data.format.date") + " 'at' hh:mm:ss a";

    public RevertProjectChangesBean() {
    }

    public List<ProjectHistoryItem> getHistory() {
        List<ProjectHistoryItem> history = new ArrayList<>();
        ProjectModel model = WebStudioUtils.getProjectModel();

        String[] sourceNames = getSources();
        Map<Long, File> historyMap = model.getHistoryManager().get(sourceNames);

        List<String> used = new ArrayList<>();

        for (long modifiedOn : historyMap.keySet()) {
            String sourceName = historyMap.get(modifiedOn).getName();

            ProjectHistoryItem historyItem = new ProjectHistoryItem();
            String modifiedOnStr = new SimpleDateFormat(dateModifiedPattern).format(new Date(modifiedOn));
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

    public String restore() throws Exception {
        String versionToRestoreParam = FacesUtils.getRequestParameter("toRestore");
        long versionToRestore = Long.parseLong(versionToRestoreParam);

        ProjectModel model = WebStudioUtils.getProjectModel();
        if (model != null) {
            model.getHistoryManager().restore(versionToRestore);
        }
        return null;
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

                UploadExcelDiffController diffController = (UploadExcelDiffController) FacesUtils
                    .getBackingBean("uploadExcelDiffController");
                diffController.compare(Arrays.asList(file1ToCompare, file2ToCompare));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Message("Error when comparing projects");
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
