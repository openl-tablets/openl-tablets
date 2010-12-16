package org.openl.rules.common.impl;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectDependency;

public class ProjectDependencyImpl implements ProjectDependency {
    private static final long serialVersionUID = -1745471023092596849L;

    private String projectName;
    private CommonVersion lowerLimit;
    private CommonVersion upperLimit;

    public ProjectDependencyImpl(String projectName, CommonVersion lowerLimit) {
        this(projectName, lowerLimit, null);
    }

    public ProjectDependencyImpl(String projectName, CommonVersion lowerLimit, CommonVersion upperLimit) {
        if (projectName == null) {
            throw new NullPointerException("projectName is null");
        }
        if (lowerLimit == null) {
            throw new NullPointerException("lowerLimit is null");
        }

        if (upperLimit != null && lowerLimit.compareTo(upperLimit) > 0) {
            throw new IllegalArgumentException("upperLimit is less than lowerLimit");
        }

        this.projectName = projectName;
        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjectDependencyImpl)) {
            return false;
        }

        ProjectDependencyImpl that = (ProjectDependencyImpl) o;

        return projectName.equals(that.projectName) && lowerLimit.equals(that.lowerLimit)
                && (upperLimit == null ? that.upperLimit == null : upperLimit.equals(that.upperLimit));

    }

    public CommonVersion getLowerLimit() {
        return lowerLimit;
    }

    public String getProjectName() {
        return projectName;
    }

    public CommonVersion getUpperLimit() {
        return upperLimit;
    }

    @Override
    public int hashCode() {
        int result;
        result = projectName.hashCode();
        result = 31 * result + lowerLimit.hashCode();
        result = 31 * result + (upperLimit != null ? upperLimit.hashCode() : 0);
        return result;
    }

    public boolean hasUpperLimit() {
        return upperLimit != null;
    }
}
