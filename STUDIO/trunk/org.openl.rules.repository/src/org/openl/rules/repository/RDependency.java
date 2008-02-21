package org.openl.rules.repository;


public interface RDependency {
    String getProjectName();

    boolean hasUpperLimit();

    CommonVersion getLowerLimit();
    CommonVersion getUpperLimit();
}
