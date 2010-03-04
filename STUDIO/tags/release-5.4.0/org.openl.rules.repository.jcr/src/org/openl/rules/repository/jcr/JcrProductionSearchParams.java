package org.openl.rules.repository.jcr;

import org.openl.rules.repository.RProductionRepository;

import java.util.Date;

public class JcrProductionSearchParams implements RProductionRepository.SearchParams {
    private Date lowerEffectiveDate;
    private Date upperEffectiveDate;
    private Date lowerExpirationDate;
    private Date upperExpirationDate;
    private String lineOfBusiness;

    public String getLineOfBusiness() {
        return lineOfBusiness;
    }

    public Date getLowerEffectiveDate() {
        return lowerEffectiveDate;
    }

    public Date getLowerExpirationDate() {
        return lowerExpirationDate;
    }

    public Date getUpperEffectiveDate() {
        return upperEffectiveDate;
    }

    public Date getUpperExpirationDate() {
        return upperExpirationDate;
    }

    public void setLineOfBusiness(String lineOfBusiness) {
        this.lineOfBusiness = lineOfBusiness;
    }

    public void setLowerEffectiveDate(Date lowerEffectiveDate) {
        this.lowerEffectiveDate = lowerEffectiveDate;
    }

    public void setLowerExpirationDate(Date lowerExpirationDate) {
        this.lowerExpirationDate = lowerExpirationDate;
    }

    public void setUpperEffectiveDate(Date upperEffectiveDate) {
        this.upperEffectiveDate = upperEffectiveDate;
    }

    public void setUpperExpirationDate(Date upperExpirationDate) {
        this.upperExpirationDate = upperExpirationDate;
    }
}
