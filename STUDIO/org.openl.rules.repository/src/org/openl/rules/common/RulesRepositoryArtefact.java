package org.openl.rules.common;

import java.util.Map;

public interface RulesRepositoryArtefact {
    Map<String, Object> getProps();

    Map<String, InheritedProperty> getInheritedProps();

    String getVersionComment();

    void setProps(Map<String, Object> props) throws PropertyException;

    void setVersionComment(String versionComment) throws PropertyException;

}
