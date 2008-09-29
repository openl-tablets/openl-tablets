package org.openl.rules.workspace.repository;

import java.util.Date;

import org.openl.rules.workspace.abstracts.ProjectException;

public interface RulesRepositoryArtefact {
    public Date getEffectiveDate();
    public Date getExpirationDate();
    public String getLineOfBusiness();
    
    public String getAttribute1();
    public String getAttribute2();
    public String getAttribute3();
    public String getAttribute4();
    public String getAttribute5();
    public Date getAttribute6();
    public Date getAttribute7();
    public Date getAttribute8();
    public Date getAttribute9();
    public Date getAttribute10();
    public Double getAttribute11();
    public Double getAttribute12();
    public Double getAttribute13();
    public Double getAttribute14();
    public Double getAttribute15();

    public void setEffectiveDate(Date date) throws ProjectException;
    public void setExpirationDate(Date date) throws ProjectException;
    public void setLineOfBusiness(String lineOfBusiness) throws ProjectException;
    
    public void setAttribute1(String attribute1) throws ProjectException;
    public void setAttribute2(String attribute2) throws ProjectException;
    public void setAttribute3(String attribute3) throws ProjectException;
    public void setAttribute4(String attribute4) throws ProjectException;
    public void setAttribute5(String attribute5) throws ProjectException;
    public void setAttribute6(Date attribute6) throws ProjectException;
    public void setAttribute7(Date attribute7) throws ProjectException;
    public void setAttribute8(Date attribute8) throws ProjectException;
    public void setAttribute9(Date attribute9) throws ProjectException;
    public void setAttribute10(Date attribute10) throws ProjectException;
    public void setAttribute11(Double attribute11) throws ProjectException;
    public void setAttribute12(Double attribute12) throws ProjectException;
    public void setAttribute13(Double attribute13) throws ProjectException;
    public void setAttribute14(Double attribute14) throws ProjectException;
    public void setAttribute15(Double attribute15) throws ProjectException;
}
