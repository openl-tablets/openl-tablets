package org.openl.rules.webstudio.web.repository;

import org.openl.rules.workspace.abstracts.ProjectArtefact;
import org.openl.rules.workspace.abstracts.ProjectVersion;

import java.util.Comparator;


/**
 * Repository Utilities
 *
 * @author Aleh Bykhavets
 */
public class RepositoryUtils {
    public static final Comparator<ProjectVersion> VERSIONS_REVERSE_COMPARATOR = new Comparator<ProjectVersion>() {
            public int compare(ProjectVersion o1, ProjectVersion o2) {
                return o2.compareTo(o1);
            }
        };
    public static final Comparator<ProjectArtefact> ARTEFACT_COMPARATOR = new Comparator<ProjectArtefact>() {
            public int compare(ProjectArtefact o1, ProjectArtefact o2) {
                if (o1.isFolder() == o2.isFolder()) {
                    return o1.getName().compareTo(o2.getName());
                } else {
                    return (o1.isFolder() ? (-1) : 1);
                }
            }
        };
}
