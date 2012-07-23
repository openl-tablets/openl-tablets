package org.openl.rules.common;

import java.util.Date;
import java.util.Map;

import org.openl.rules.common.impl.PropertyImpl;
import org.openl.rules.repository.api.ArtefactProperties;


public interface RulesRepositoryArtefact {

    Date getEffectiveDate();

    Date getExpirationDate();

    String getLineOfBusiness();

    Map<String, Object> getProps();

    String getVersionComment();

    void setEffectiveDate(Date date) throws PropertyException;

    void setExpirationDate(Date date) throws PropertyException;

    void setLineOfBusiness(String lineOfBusiness) throws PropertyException;

    void setProps(Map<String, Object> props) throws PropertyException;

    void setVersionComment(String versionComment) throws PropertyException;

}
