package org.openl.rules.webstudio.web;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.faces.model.SelectItem;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * @author Andrei Astrouski
 */
public class RevertProjectChangesBean {

    public static String DATE_MODIFIED_PATTERN = "MM.dd.yyyy 'at' hh:mm:ss a";

    private long versionToRevert;

    public RevertProjectChangesBean() {
    }

    public long getVersionToRevert() {
        return versionToRevert;
    }

    public void setVersionToRevert(long versionToRevert) {
        this.versionToRevert = versionToRevert;
    }

    public List<SelectItem> getVersions() {
        List<SelectItem> versions = new ArrayList<SelectItem>();
        ProjectModel model = WebStudioUtils.getProjectModel();

        long[] modifyVersions = model.getHistoryManager().getVersions();
        for (long modifyVersion : modifyVersions) {
            String modified = new SimpleDateFormat(DATE_MODIFIED_PATTERN).format(
                    new Date(modifyVersion));
            versions.add(new SelectItem(modifyVersion, modified));
        }

        return versions;
    }

    public String revert() {
        ProjectModel model = WebStudioUtils.getProjectModel();
        if (model.getHistoryManager().revert(versionToRevert)) {
            return "mainPage";
        } else {
            FacesUtils.addErrorMessage("Error when reverting project");
            return null;
        }
    }

}
