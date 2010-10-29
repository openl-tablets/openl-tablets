package org.openl.rules.workspace.repository;

import java.util.Date;
import java.util.Map;

import org.openl.rules.workspace.abstracts.ProjectException;

public interface RulesRepositoryArtefact {

    Date getEffectiveDate();

    Date getExpirationDate();

    String getLineOfBusiness();

    Map<String, Object> getProps();

    void setEffectiveDate(Date date) throws ProjectException;

    void setExpirationDate(Date date) throws ProjectException;

    void setLineOfBusiness(String lineOfBusiness) throws ProjectException;

    void setProps(Map<String, Object> props) throws ProjectException;

}
