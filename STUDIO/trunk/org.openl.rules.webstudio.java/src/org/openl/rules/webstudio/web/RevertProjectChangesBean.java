package org.openl.rules.webstudio.web;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * @author Andrei Astrouski
 */
public class RevertProjectChangesBean {

    public static String DATE_MODIFIED_PATTERN = "MM.dd.yyyy 'at' hh:mm:ss a";

    public RevertProjectChangesBean() {
    }

    public List<ProjectHistoryItem> getHistory() {
        List<ProjectHistoryItem> history = new ArrayList<ProjectHistoryItem>();
        ProjectModel model = WebStudioUtils.getProjectModel();

        long[] modifyVersions = model.getHistoryManager().getVersions();
        for (long modifyVersion : modifyVersions) {
            String modifiedOn = new SimpleDateFormat(DATE_MODIFIED_PATTERN).format(
                    new Date(modifyVersion));

            ProjectHistoryItem historyItem = new ProjectHistoryItem();
            historyItem.setVersion(modifyVersion);
            historyItem.setModifiedOn(modifiedOn);

            history.add(historyItem);
        }

        return history;
    }

    public String revert() {
        String versionToRevertParam = FacesUtils.getRequestParameter("version");
        long versionToRevert = Long.parseLong(versionToRevertParam);

        ProjectModel model = WebStudioUtils.getProjectModel();
        if (model.getHistoryManager().revert(versionToRevert)) {
            return "mainPage";
        } else {
            FacesUtils.addErrorMessage("Error when reverting project");
            return null;
        }
    }

}
