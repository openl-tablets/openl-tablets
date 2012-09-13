package org.openl.rules.common;

import java.util.Date;
import java.util.Map;

import org.openl.rules.common.impl.PropertyImpl;
import org.openl.rules.repository.api.ArtefactProperties;


public interface RulesRepositoryArtefact {
    Map<String, Object> getProps();

    Map<String, InheritedProperty> getInheritedProps();

    String getVersionComment();

    void setProps(Map<String, Object> props) throws PropertyException;

    void setVersionComment(String versionComment) throws PropertyException;

}
