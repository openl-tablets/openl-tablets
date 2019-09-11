package org.openl.rules.webstudio.web.repository.merge;

import java.util.Map;

import javax.servlet.http.HttpSession;

class ConflictUtils {
    private static final String CONFLICT_RESOLUTIONS_PARAMETER = "conflictResolutions";

    static void saveConflictsToSession(HttpSession session, Map<String, ConflictResolution> conflictResolutions) {
        session.setAttribute(CONFLICT_RESOLUTIONS_PARAMETER, conflictResolutions);
    }

    static Map<String, ConflictResolution> getConflictsFromSession(HttpSession session) {
        @SuppressWarnings("unchecked")
        Map<String, ConflictResolution> suite = (Map<String, ConflictResolution>) session.getAttribute(
            CONFLICT_RESOLUTIONS_PARAMETER);
        return suite;
    }

    static void clear(HttpSession session) {
        session.removeAttribute(CONFLICT_RESOLUTIONS_PARAMETER);
    }
}
