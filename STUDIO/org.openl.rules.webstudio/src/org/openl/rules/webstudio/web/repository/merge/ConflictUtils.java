package org.openl.rules.webstudio.web.repository.merge;

import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.openl.rules.webstudio.web.util.WebStudioUtils;

public class ConflictUtils {
    private static final String SESSION_PARAM_MERGE_CONFLICT = "mergeConflict";
    private static final String CONFLICT_RESOLUTIONS_PARAMETER = "conflictResolutions";

    public static void saveMergeConflict(MergeConflictInfo info) {
        WebStudioUtils.getExternalContext().getSessionMap().put(SESSION_PARAM_MERGE_CONFLICT, info);
    }

    public static MergeConflictInfo getMergeConflict() {
        return (MergeConflictInfo) WebStudioUtils.getExternalContext().getSessionMap().get(SESSION_PARAM_MERGE_CONFLICT);
    }

    public static void removeMergeConflict() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            facesContext.getExternalContext().getSessionMap().remove(SESSION_PARAM_MERGE_CONFLICT);
            HttpSession session = WebStudioUtils.getSession();
            if (session != null) {
                session.removeAttribute(CONFLICT_RESOLUTIONS_PARAMETER);
            }
        }
    }

    static void saveResolutionsToSession(Map<String, ConflictResolution> conflictResolutions) {
        HttpSession session = WebStudioUtils.getSession();
        session.setAttribute(CONFLICT_RESOLUTIONS_PARAMETER, conflictResolutions);
    }

    static Map<String, ConflictResolution> getResolutionsFromSession(HttpSession session) {
        @SuppressWarnings("unchecked")
        Map<String, ConflictResolution> suite = (Map<String, ConflictResolution>) session
            .getAttribute(CONFLICT_RESOLUTIONS_PARAMETER);
        return suite;
    }

}
