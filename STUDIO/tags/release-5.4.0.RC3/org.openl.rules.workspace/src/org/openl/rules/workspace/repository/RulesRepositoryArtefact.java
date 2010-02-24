package org.openl.rules.workspace.repository;

import java.util.Date;
import java.util.Map;

import org.openl.rules.workspace.abstracts.ProjectException;

public interface RulesRepositoryArtefact {
    public Date getEffectiveDate();

    public Date getExpirationDate();

    public String getLineOfBusiness();

    public Map<String, Object> getProps();

    public void setEffectiveDate(Date date) throws ProjectException;

    public void setExpirationDate(Date date) throws ProjectException;

    public void setLineOfBusiness(String lineOfBusiness) throws ProjectException;

    public void setProps(Map<String, Object> props) throws ProjectException;
}
