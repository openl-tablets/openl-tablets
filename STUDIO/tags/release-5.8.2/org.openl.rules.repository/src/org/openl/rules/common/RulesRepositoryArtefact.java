package org.openl.rules.common;

import java.util.Date;
import java.util.Map;


public interface RulesRepositoryArtefact {

    Date getEffectiveDate();

    Date getExpirationDate();

    String getLineOfBusiness();

    Map<String, Object> getProps();

    void setEffectiveDate(Date date) throws PropertyException;

    void setExpirationDate(Date date) throws PropertyException;

    void setLineOfBusiness(String lineOfBusiness) throws PropertyException;

    void setProps(Map<String, Object> props) throws PropertyException;

}
