package org.openl.rules.webstudio.web.repository;

import java.util.Comparator;

import org.openl.rules.workspace.abstracts.ProjectVersion;

/**
 * Repository Utilities
 * 
 * @author Aleh Bykhavets
 *
 */
public class RepositoryUtils {
    public static final Comparator<ProjectVersion> VERSIONS_REVERSE_COMPARATOR
    = new Comparator<ProjectVersion>() {
        public int compare(ProjectVersion o1, ProjectVersion o2) {
            return o2.compareTo(o1);
        }
    };
}
