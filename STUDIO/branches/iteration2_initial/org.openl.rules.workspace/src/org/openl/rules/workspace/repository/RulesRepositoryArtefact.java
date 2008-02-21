package org.openl.rules.workspace.repository;

import java.util.Date;

import org.openl.rules.workspace.abstracts.ProjectException;

public interface RulesRepositoryArtefact {
    public Date getEffectiveDate();
    public Date getExpirationDate();
    public String getLineOfBusiness();

    public void setEffectiveDate(Date date) throws ProjectException;
    public void setExpirationDate(Date date) throws ProjectException;
    public void setLineOfBusiness(String lineOfBusiness) throws ProjectException;
}
