package org.openl.rules.repository;

public interface RDependency {
    CommonVersion getLowerLimit();

    String getProjectName();

    CommonVersion getUpperLimit();

    boolean hasUpperLimit();
}
